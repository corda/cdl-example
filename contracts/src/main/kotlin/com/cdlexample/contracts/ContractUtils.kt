package com.cdlexample.contracts

import com.cdlexample.states.Status
import com.cdlexample.states.StatusState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction



class Path<T: StatusState>(val command: CommandData,
                val outputStatus: Status?,
                val numberOfInputStates: Int,
                val numberOfOutputStates: Int,
                val additionalStates: Set<AdditionalStates> = setOf())

class AdditionalStates(val type: AdditionalStatesType, val statesClass: Class<out ContractState>, val numberOfStates: Int)

enum class AdditionalStatesType {INPUT, OUTPUT, REFERENCE}

// todo: consider adding an exclusive flag, so that only the additional states specified in the PathConstraint are allowed to be in the transaction

class PathConstraint<T: StatusState>(val command: CommandData,
                     val outputStatus: Status?,
                     val inputMultiplicityConstraint: MultiplicityConstraint = MultiplicityConstraint(),
                     val outputMultiplicityConstraint: MultiplicityConstraint = MultiplicityConstraint(),
                     val additionalStatesConstraints: Set<AdditionalStatesConstraint> =  setOf()){

    infix fun allows(p: Path<T>): Boolean =
        (command::class.java == p.command::class.java) &&
        (outputStatus == p.outputStatus) &&
        (inputMultiplicityConstraint allows p.numberOfInputStates) &&
        (outputMultiplicityConstraint allows p.numberOfOutputStates) &&
        additionalStatesCheck(additionalStatesConstraints, p.additionalStates)

    infix fun doesNotAllow(p: Path<T>): Boolean = !this.allows(p)

    // For all AdditionalStatesConstraints there must be an AdditionalState which satisfies it.
    // Note, where the required number of states can be 0, we need to check the case where there are no states of that type, which will satisfy the constraint.
    private fun additionalStatesCheck(constraints: Set<AdditionalStatesConstraint>, additionalStates: Set<AdditionalStates>) :Boolean =
            constraints.all { c->
                additionalStates.any { s->
                    c isSatisfiedBy s} || c.requiredNumberOfStates.from == 0 && additionalStates.none { it.statesClass == c.statesClass }
            }
}

class AdditionalStatesConstraint(val type: AdditionalStatesType, val statesClass: Class<out ContractState>, val requiredNumberOfStates: MultiplicityConstraint = MultiplicityConstraint()) {

    infix fun isSatisfiedBy(additionalStates: AdditionalStates ):Boolean =
        (type == additionalStates.type) &&
        (statesClass == additionalStates.statesClass) &&
        (requiredNumberOfStates allows additionalStates.numberOfStates)

    infix fun isNotSatisfiedBy (additionalStates: AdditionalStates): Boolean = !isSatisfiedBy(additionalStates)
}

class MultiplicityConstraint(val from: Int = 1, val bounded: Boolean = true, val upperBound: Int = from){

    infix fun allows(numberOfStates: Int): Boolean =
        (numberOfStates >= from) &&
        (!bounded || numberOfStates <= upperBound)

    infix fun doesNotAllow(numberOfStates: Int): Boolean = !this.allows(numberOfStates)
}

fun <T: StatusState> verifyPath(p: Path<T>, pathConstraintList: List<PathConstraint<T>>): Boolean =
        pathConstraintList.any { pc -> pc allows p }


inline fun <reified T: StatusState>requireSingleInputStatus(tx:LedgerTransaction): Status? = requireSingleInputStatus(tx, T::class.java)

fun <T: StatusState>requireSingleInputStatus(tx:LedgerTransaction, statesClass: Class<T>): Status?{
    return requireSingleStatus(tx.inputsOfType(statesClass),"All inputs of type ${statesClass.simpleName} must have the same status.")
}

inline fun <reified T: StatusState>requireSingleOutputStatus(tx:LedgerTransaction): Status? = requireSingleOutputStatus(tx, T::class.java)

fun <T: StatusState>requireSingleOutputStatus(tx:LedgerTransaction, statesClass: Class<T>): Status?{
    return requireSingleStatus(tx.outputsOfType(statesClass), "All outputs of type ${statesClass.simpleName} must have the same status.")
}

fun <T: StatusState>requireSingleStatus (states: List<T>, error: String): Status?{
    val distinctStatuses = states.map {it.status}.distinct()
    requireThat {
        error using ( distinctStatuses.size <= 1)}
    return distinctStatuses.firstOrNull()
}

fun <T: StatusState>getPath(tx:LedgerTransaction, primaryStateClass: Class<T>, commandValue: CommandData): Path<T> {

    val outputStatus = requireSingleOutputStatus(tx, primaryStateClass)


    fun getStatesForType(type: AdditionalStatesType, statesClass: Class<out ContractState> ): List<ContractState> = when (type){
                AdditionalStatesType.INPUT -> tx.inputsOfType(statesClass)
                AdditionalStatesType.OUTPUT -> tx.outputsOfType(statesClass)
                AdditionalStatesType.REFERENCE -> tx.referenceInputsOfType(statesClass)
            }

    fun List<ContractState>.getAdditional(type: AdditionalStatesType) = asSequence()
            .map { it::class.java }
            .filter { it != primaryStateClass }
            .distinct()
            .map { AdditionalStates(type, it, getStatesForType(type, it).size) }

    // todo: consider what to do with reference states of primary type

    val additionalInputs = tx.inputStates.getAdditional(AdditionalStatesType.INPUT)
    val additionalOutputs = tx.outputStates.getAdditional(AdditionalStatesType.OUTPUT)
    val additionalReferences = tx.referenceStates.getAdditional(AdditionalStatesType.REFERENCE)

    val additionalStates = (additionalInputs + additionalOutputs + additionalReferences).toSet()

    return  Path(commandValue, outputStatus, tx.inputsOfType(primaryStateClass).size, tx.outputsOfType(primaryStateClass).size, additionalStates)
}