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

class AdditionalStates(val type: AdditionalStatesType, val clazz: Class<out ContractState>, val numberOfStates: Int)

enum class AdditionalStatesType {INPUT, OUTPUT, REFERENCE}

// todo: consider adding an exclusive flag, so that only the additional states specified in the PathConstraint are allowed to be in the transaction

class PathConstraint<T: StatusState>(val command: CommandData,
                     val outputStatus: Status?,
                     val inputMultiplicityConstraint: MultiplicityConstraint = MultiplicityConstraint(),
                     val outputMultiplicityConstraint: MultiplicityConstraint = MultiplicityConstraint(),
                     val additionalStatesConstraints: Set<AdditionalStatesConstraint> =  setOf()){    // todo: should this be a set, could it give different error messages depending on the order of evaluation

    infix fun allows(p: Path<T>): Boolean = when {
        (command::class.java != p.command::class.java) -> false
        (outputStatus != p.outputStatus)  -> false
        (inputMultiplicityConstraint doesNotAllow p.numberOfInputStates) -> false
        (outputMultiplicityConstraint doesNotAllow p.numberOfOutputStates) -> false
        (!additionalStatesCheck(additionalStatesConstraints, p.additionalStates)) -> false
        else -> true
    }

    infix fun doesNotAllow(p: Path<T>): Boolean = !this.allows(p)

    private fun additionalStatesCheck(constraints: Set<AdditionalStatesConstraint>, additionalStates: Set<AdditionalStates>) :Boolean =
            constraints.all { c -> additionalStates.any { s -> c isSatisfiedBy s} ||
                                                                            c.requiredNumberOfStates.from == 0 && additionalStates.none { it.clazz == c.clazz }
            }
}

class AdditionalStatesConstraint(val type: AdditionalStatesType ,val clazz: Class<out ContractState>, val requiredNumberOfStates: MultiplicityConstraint = MultiplicityConstraint()) {

    infix fun isSatisfiedBy(additionalStates: AdditionalStates ):Boolean = when {
        (type != additionalStates.type) -> false
        (clazz != additionalStates.clazz) -> false
        (requiredNumberOfStates.doesNotAllow(additionalStates.numberOfStates)) -> false
        else -> true
    }

    infix fun isNotSatisfiedBy (additionalStates: AdditionalStates): Boolean = !isSatisfiedBy(additionalStates)
}

class MultiplicityConstraint(val from: Int = 1, val bounded: Boolean = true, val upperBound: Int = from){

    infix fun allows(numberOfStates: Int): Boolean = when {
        (numberOfStates < from) -> false
        (bounded && numberOfStates > upperBound) -> false
        else -> true
    }

    infix fun doesNotAllow(numberOfStates: Int): Boolean = !this.allows(numberOfStates)
}

fun <T: StatusState> verifyPath(p: Path<T>, pathConstraintList: List<PathConstraint<T>>): Boolean =
        pathConstraintList.any { pc -> pc allows p }


inline fun <reified T: StatusState>requireSingleInputStatus(tx:LedgerTransaction): Status? = requireSingleInputStatus(tx, T::class.java)

fun <T: StatusState>requireSingleInputStatus(tx:LedgerTransaction, clazz: Class<T>): Status?{
    return requireSingleStatus(tx.inputsOfType(clazz),"All inputs of type ${clazz.simpleName} must have the same status.")
}

inline fun <reified T: StatusState>requireSingleOutputStatus(tx:LedgerTransaction): Status? = requireSingleOutputStatus(tx, T::class.java)

fun <T: StatusState>requireSingleOutputStatus(tx:LedgerTransaction, clazz: Class<T>): Status?{
    return requireSingleStatus(tx.outputsOfType(clazz), "All outputs of type ${clazz.simpleName} must have the same status.")
}

fun <T: StatusState>requireSingleStatus (states: List<T>, error: String): Status?{
    val statuses = states.map {it.status}.distinct()
    requireThat {
        error using ( statuses.size <= 1)}
    return if (states.isNotEmpty()) states.first().status else null
}

fun <T: StatusState>getPath(tx:LedgerTransaction, primaryStateClass: Class<T>, commandValue: CommandData): Path<T> {

    val outputStatus = requireSingleOutputStatus(tx, primaryStateClass)

    val primaryInputStates = tx.inputsOfType(primaryStateClass)
    val primaryOutputStates = tx.outputsOfType(primaryStateClass)

    // todo: consider what to do with reference states of primary type

    // get Additional Inputs
    val remainingInputs = tx.inputStates - primaryInputStates
    val distinctInputTypes = remainingInputs.map { it::class.java }.distinct()
    val mList = mutableListOf<AdditionalStates>()
    for (dt in distinctInputTypes){
        mList.add(AdditionalStates(AdditionalStatesType.INPUT, dt, tx.inputsOfType(dt).size))
    }

    // Get Additional Outputs
    val remainingOutputs = tx.outputStates - primaryOutputStates
    val distinctOutputTypes = remainingOutputs.map { it::class.java }.distinct()
    for (dt in distinctOutputTypes){
        mList.add(AdditionalStates(AdditionalStatesType.OUTPUT, dt, tx.outputsOfType(dt).size))
    }

    // Get Additional References
    val references = tx.referenceStates
    val distinctReferenceTypes = references.map { it::class.java }.distinct()
    for (dt in distinctReferenceTypes){
        mList.add(AdditionalStates(AdditionalStatesType.REFERENCE, dt, tx.referenceInputsOfType(dt).size))
    }

    val additionalStates = mList.toSet()

    return  Path<T>(commandValue, outputStatus, primaryInputStates.size, primaryOutputStates.size, additionalStates)
}