package com.cdlexample.contracts

import com.cdlexample.states.Status
import com.cdlexample.states.StatusState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction


// todo: how to protect against substituting a different state type with the same stauts. need to lock the Path and Path constraint to a particular state type

// todo: make types inherit from StatusState, rather than ContractState


class Path<T: ContractState>(val command: CommandData,
                val outputStatus: Status?,
                val numberOfInputStates: Int,
                val numberOfOutputStates: Int,
                val additionalStates: Set<AdditionalStates> = setOf())

class AdditionalStates(val type: AdditionalStatesType, val clazz: Class<out ContractState>, val numberOfStates: Int)

enum class AdditionalStatesType {INPUT, OUTPUT, REFERENCE}



class PathConstraint<T: ContractState>(val command: CommandData,
                     val outputStatus: Status?,
                     val inputMultiplicityConstraint: MultiplicityConstraint = MultiplicityConstraint(),
                     val outputMultiplicityConstraint: MultiplicityConstraint = MultiplicityConstraint(),
                     val additionalStatesConstraints: Set<AdditionalStatesConstraint> =  setOf()){

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
             additionalStatesConstraints.all { c -> additionalStates.any { s -> c isSatisfiedBy s }}
}

class AdditionalStatesConstraint(val type: AdditionalStatesType ,val clazz: Class<out ContractState>, val requiredNumberOfStates: MultiplicityConstraint) {

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

fun <T: ContractState> verifyPath(p: Path<T>, pathConstraintList: List<PathConstraint<T>>): Boolean =
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

fun <T: StatusState>getPath(tx:LedgerTransaction, clazz: Class<T>, commandValue: CommandData): Path<T> {

    val outputStatus = requireSingleOutputStatus(tx, clazz)

    val primaryInputStates = tx.inputsOfType(clazz)
    val primaryOutputStates = tx.outputsOfType(clazz)

    val remainingInputs = tx.inputStates - primaryInputStates

    val inputTypes = remainingInputs.map { it::class.java }
    val distinctTypes = inputTypes.distinct()

    val list = mutableListOf<AdditionalStates>()
    for (dt in distinctTypes){
        list. add(AdditionalStates(AdditionalStatesType.INPUT, dt, tx.inputsOfType(dt).size))
    }

    // todo: write test to try out - need to work out how to test with LedgerTransactions - can we use mock services


    return  Path<T>(commandValue, outputStatus, primaryInputStates.size, primaryOutputStates.size)
}