package com.cdlexample.contracts

import net.corda.core.contracts.*
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction

/**
 * ContractUtils.kt provides a set of classes, interfaces and helper functions which can be used in Corda Contracts
 * to simplify the implementation of the SMart Contract designs described in CorDapp Design Language (CDL) Smart Contract Diagrams
 *
 */

/**
 * The StatusState interface should be implemented for all [ContractState]s that require a status field.
 *
 * [status] is nullable so that when there is no input or output state in a transaction, the status can be represented as [null]
 *
 */
interface StatusState: ContractState {
    val status: Status?
}

/**
 * Statuses are defined as enum classes in the StatusState which should implement this Status interface.
 */
@CordaSerializable
interface Status

/**
 * A [Path] represents the transition that a [StatusState] makes from a given input status within a specific transaction
 *
 * @property command represents the command.value in the transaction which relates to the Primary State's Contract
 * (there could be other commands in the transaction but they are not dealt with by Paths).
 * @property outputStatus represents the status of the output Primary State. it will be null if there is no output state.
 * @property numberOfInputStates represents the number of States of the Primary State type in the transaction.
 * @property numberOfOutputStates represents the number of States of the Primary State type in the transaction.
 * @property additionalStates represents States types other than the Primary States type which are in the transaction,
 * including whether they are inputs, outputs or reference states and how many of each are in the transaction.
 */

class Path<T: StatusState>(val command: CommandData,
                           val outputStatus: Status?,
                           val numberOfInputStates: Int,
                           val numberOfOutputStates: Int,
                           val additionalStates: Set<AdditionalStates> = setOf())


/**
 * [AdditionalStates] represents States types other than the Primary States type which are in the transaction,
 * including whether they are inputs, outputs or reference states and how many of each are in the transaction.
 *
 * @property type where the AdditionalStates are INPUT, OUTPUT or REFERENCE.
 * @property statesClass the Class of the states.
 * @property numberOfStates the number of states that match the [type] and [statesClass] in the transaction.
 */
class AdditionalStates(val type: AdditionalStatesType, val statesClass: Class<out ContractState>, val numberOfStates: Int)

/**
 * [AdditionalStatesType] defines whether the additionalStates are INPUT, OUTPU or REFERENCE states in the transaction.
 */
enum class AdditionalStatesType {INPUT, OUTPUT, REFERENCE}

// todo: consider adding an exclusive flag, so that only the additional states specified in the PathConstraint are allowed to be in the transaction


/**
 * [PathConstraints]s are used to restrict Paths that are allowed in a transaction. The Contract defines a set of
 * [PathConstraint]s for each Primary State status, for example when in status X you can follow PathConstraint A or B,
 * but when you are in state Y you can only follow PathConstraint C.
 *
 * In order to pass the verify, the [Path] in the transaction needs to comply to at least one of the
 * allowed [PathConstraints] for the Status of the Primary Input State.
 *
 * @property command is the class of the command required.
 * @property outputStatus is the outputStatus of the Primary State that is required.
 * @property inputMultiplicityConstraint defines the range of number of inputs of Primary type that is required.
 * @property outputMultiplicityConstraint defines the range of number of outputs of Primary type that is required.
 * @property additionalStatesConstraint defines which additional states must be present in the transaction.
 *
 * A [Path] will only be allowed by a [PathConstraint] if it complies to all of the above requirements.
 */
class PathConstraint<T: StatusState>(val command: CommandData,
                                     val outputStatus: Status?,
                                     val inputMultiplicityConstraint: MultiplicityConstraint = MultiplicityConstraint(),
                                     val outputMultiplicityConstraint: MultiplicityConstraint = MultiplicityConstraint(),
                                     val additionalStatesConstraints: Set<AdditionalStatesConstraint> =  setOf()){
    /**
     * Returns a [Boolean] indiacting whether [Path p] is allowed by the [PathConstraint].
     *
     * @property p the [Path] that is checked against the [PathConstraint].
     */
    infix fun allows(p: Path<T>): Boolean =
        (command::class.java == p.command::class.java) &&
        (outputStatus == p.outputStatus) &&
        (inputMultiplicityConstraint allows p.numberOfInputStates) &&
        (outputMultiplicityConstraint allows p.numberOfOutputStates) &&
        additionalStatesCheck(additionalStatesConstraints, p.additionalStates)

    /**
     * Negated version of [allows].
     */
    infix fun doesNotAllow(p: Path<T>): Boolean = !this.allows(p)

    /**
     * Returns a [Boolean] indicating whether all of additionalStateContraints are satisfied by the set of AdditionaState.
     *
     * @property constraints represent the set of [AdditionalStatesConstraints] that must be satisfied.
     * @property additionalStates represents the set of [AdditionalStates] in the [Path] that is being evaluated.
     */
    private fun additionalStatesCheck(constraints: Set<AdditionalStatesConstraint>, additionalStates: Set<AdditionalStates>) :Boolean =
            // For all AdditionalStatesConstraints there must be an AdditionalState which satisfies it.
            // Note, where the required number of states can be 0, we need to check the case where there are no states of that type, which will satisfy the constraint.
            constraints.all { c->
                additionalStates.any { s->
                    c isSatisfiedBy s} || c.requiredNumberOfStates.from == 0 && additionalStates.none { it.statesClass == c.statesClass }
            }
}

/**
 * [AdditionalStatesConstraint] represent a set of additional States which must be present in a transaction.
 *
 * @property type is type is INPUT, OUTPUT or REFERENCE.
 * @property statesClass is is the required type of the additional states.
 * @property requiredNumberOfStates defines how many AdditionalStates of this type are allowed using a MultiplicityConstraint.
 */
class AdditionalStatesConstraint(val type: AdditionalStatesType, val statesClass: Class<out ContractState>, val requiredNumberOfStates: MultiplicityConstraint = MultiplicityConstraint()) {

    /**
     * returns a [Boolean] indicating if the [AdditionalStates] satisfy the [AdditionalStatesConstraint].
     */
    infix fun isSatisfiedBy(additionalStates: AdditionalStates ):Boolean =
        (type == additionalStates.type) &&
        (statesClass == additionalStates.statesClass) &&
        (requiredNumberOfStates allows additionalStates.numberOfStates)

    /**
     * Negated version of [isSatisfiedBy].
     */
    infix fun isNotSatisfiedBy (additionalStates: AdditionalStates): Boolean = !isSatisfiedBy(additionalStates)
}

/**
 * [MultiplicityConstraint]s define how many AdditionalStates of this type are allowed
 *
 * @property from specifies the minimum number of states (inclusive).
 * @property bounded specifies if there is an upper limit.
 * @property upperBound specifies the upperbound on the number of states (inclusive), which is only applied if bounded is true.
 */
class MultiplicityConstraint(val from: Int = 1, val bounded: Boolean = true, val upperBound: Int = from){

    /**
     * Returns a [Boolean] indicating if a Integers (representing he number of states of a particular type and class in
     * a transaction) conforms to the [MultiplicityConstraint].
     */
    infix fun allows(numberOfStates: Int): Boolean =
        (numberOfStates >= from) &&
        (!bounded || numberOfStates <= upperBound)

    /**
     * Negated version of [allows].
     */
    infix fun doesNotAllow(numberOfStates: Int): Boolean = !this.allows(numberOfStates)
}

/**
 * [VerifyPath] takes a [Path] and verifies if it meets any of the set of [PathConstraint]s.
 *
 * @property p the [Path being verified].
 * @property pathConstraintList the set of [PathConstraint]s of which one or more must be satisfied by [p].
 */
fun <T: StatusState> verifyPath(p: Path<T>, pathConstraintList: List<PathConstraint<T>>): Boolean =
        pathConstraintList.any { pc -> pc allows p }

/**
 * Helper function which checks there is only one status for all input states of a particular class in the transaction.
 */
fun <T: StatusState>requireSingleInputStatus(tx:LedgerTransaction, statesClass: Class<T>): Status?{
    return requireSingleStatus(tx.inputsOfType(statesClass),"All inputs of type ${statesClass.simpleName} must have the same status.")
}

/**
 * Reified version of [requireSingleInputStatus] for Kotlin users.
 */
inline fun <reified T: StatusState>requireSingleInputStatus(tx:LedgerTransaction): Status? = requireSingleInputStatus(tx, T::class.java)


/**
 * Helper function which checks there is only one status for all output states of a particular class in the transaction.
 */
fun <T: StatusState>requireSingleOutputStatus(tx:LedgerTransaction, statesClass: Class<T>): Status?{
    return requireSingleStatus(tx.outputsOfType(statesClass), "All outputs of type ${statesClass.simpleName} must have the same status.")
}

/**
 * Reified version of [requireSingleOutputStatus] for Kotlin users.
 */
inline fun <reified T: StatusState>requireSingleOutputStatus(tx:LedgerTransaction): Status? = requireSingleOutputStatus(tx, T::class.java)


private fun <T: StatusState>requireSingleStatus (states: List<T>, error: String): Status?{
    val distinctStatuses = states.map {it.status}.distinct()
    requireThat {
        error using ( distinctStatuses.size <= 1)}
    return distinctStatuses.firstOrNull()
}


/**
 * Returns the [Path] of a given transaction from the point of view of the Primary State.
 *
 * @property tx the ledgerTransaction from which the [Path] needs to be extracted.
 * @property primaryStateClass the class of he Primary State, ie the state who statuses drive the transitions on the CDL diagram.
 * @property commandValue the Command value needs to be passed to getPath as ContractUtlis oes not have visibility of
 * the definitions of the contract Commands.
 */
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