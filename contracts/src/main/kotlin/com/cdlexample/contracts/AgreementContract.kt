package com.cdlexample.contracts

import com.cdlexample.states.AgreementState
import com.cdlexample.states.AgreementStatus.*
import com.cdlexample.states.Status
import com.cdlexample.states.StatusState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class AgreementContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.cdlexample.contracts.AgreementContract"
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Propose : Commands
        class Repropose: Commands
        class Reject: Commands
        class Agree: Commands
        class Complete: Commands
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {

        verifyPathConstraints<AgreementState>(tx)
        verifyPathConstraints(tx, AgreementState::class.java)
        verifyUniversalConstraints(tx)
        verifyStatusConstraints(tx)
        verifyLinearIDConstraints(tx)
        verifySigningConstraints(tx)
        verifyCommandConstraints(tx)
    }

    // Kotlin version
    inline fun <reified T: StatusState> verifyPathConstraints(tx: LedgerTransaction) = verifyPathConstraints(tx, T::class.java)

    // Java version
    fun <T: StatusState> verifyPathConstraints(tx: LedgerTransaction, clazz: Class<T>){

        val commandValue = tx.commands.requireSingleCommand<AgreementContract.Commands>().value

        val txPath = getPath(tx, clazz, commandValue)

        val pathMap = mapOf<Status?, List<PathConstraint<T>>>(
            null to listOf(
                    PathConstraint(Commands.Propose(), PROPOSED, MultiplicityConstraint(0))
            ),
            PROPOSED to listOf(
                    PathConstraint(Commands.Reject(), REJECTED),
                    PathConstraint(Commands.Agree(), AGREED)
            ),
            REJECTED to listOf(
                    PathConstraint(Commands.Repropose(), PROPOSED)
            ),
            AGREED to listOf(
                    PathConstraint(Commands.Complete(), null, outputMultiplicityConstraint = MultiplicityConstraint(0))
            )
        )
        val inputStatus = requireSingleInputStatus(tx, clazz)
        val allowedPaths = pathMap[inputStatus]

        requireThat {
            "Input status must have a list of PathConstraints defined." using (allowedPaths != null)
            "txPath must be allowed by PathConstraints for inputStatus $inputStatus" using verifyPath(txPath, allowedPaths!!)
        }
    }

    fun verifyUniversalConstraints(tx: LedgerTransaction){

        val allStates = tx.inputsOfType<AgreementState>() + tx.outputsOfType<AgreementState>()

        for (s in allStates) {
            requireThat {
                "The buyer and seller must be different Parties." using (s.buyer != s.seller)
                "The proposer must be either the buyer or the seller." using (listOf(s.buyer, s.seller).contains(s.proposer))
                "The consenter must be either the buyer or the seller." using (listOf(s.buyer, s.seller).contains(s.consenter))
                "The consenter and proposer must be different Parties." using (s.consenter != s.proposer)
            }
        }
    }
    // todo: add full stops into error messages
    fun verifyStatusConstraints(tx: LedgerTransaction){
        val allStates = tx.inputsOfType<AgreementState>() + tx.outputsOfType<AgreementState>()

        // Note, in kotlin non-nullable properties must be populated, hence only need to check the nullable properties of the AgreementState
        for (s in allStates) {

            when(s.status){
                PROPOSED -> {
                    requireThat {
                        "When status is Proposed rejectionReason must be null" using (s.rejectionReason == null)
                        "When status is Rejected rejectedBy must be null" using (s.rejectedBy == null)
                    }
                }
                REJECTED -> {
                    requireThat {
                        "When status is Rejected rejectionReason must not be null" using (s.rejectionReason != null)
                        "When status is Rejected rejectedBy must not be null" using (s.rejectedBy != null)
                        "When the Status is Rejected rejectedBy must be the buyer or seller" using (listOf(s.buyer, s.seller).contains(s.rejectedBy))
                    }
                }
                AGREED -> {}
            }
        }
    }

    fun verifyLinearIDConstraints(tx: LedgerTransaction){}

    fun verifySigningConstraints(tx: LedgerTransaction){

        val command = tx.commands.requireSingleCommand<AgreementContract.Commands>()
        val inputStates = tx.inputsOfType<AgreementState>()
        val inputState = if (inputStates.isNotEmpty()) inputStates.single() else null
        val outputStates = tx.outputsOfType<AgreementState>()
        val outputState = if (outputStates.isNotEmpty()) outputStates.single() else null

        when (command.value){
            is Commands.Propose -> {
                requireThat { "When Command is Propose the output.proposer should sign." using(command.signers.contains(outputState?.proposer?.owningKey)) }
            }
            is Commands.Reject -> {
                requireThat {"When the Command is Reject the output.rejectedBy Party must sign." using(command.signers.contains(outputState?.rejectedBy?.owningKey) )}
            }
            is Commands.Repropose -> {
                requireThat {"When the Command is Repropose the output.proposer must sign." using (command.signers.contains(outputState?.proposer?.owningKey))}
            }
            is Commands.Agree -> {
                requireThat {"When the Command is Agree the input.consenter must sign." using (command.signers.contains(inputState?.consenter?.owningKey))}
            }
            is Commands.Complete -> {
                requireThat {"When the command is Complete the input.seller must sign." using (command.signers.contains(inputState?.seller?.owningKey))}
            }
        }
    }

    fun verifyCommandConstraints(tx: LedgerTransaction){}

}