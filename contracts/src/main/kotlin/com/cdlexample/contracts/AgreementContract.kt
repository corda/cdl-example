package com.cdlexample.contracts

import com.cdlexample.states.AgreementState
import com.cdlexample.states.AgreementStatus.*
import net.corda.core.contracts.*
import net.corda.core.identity.Party
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

        // todo: be clear which sub functions need to pass the class

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
    fun <T: StatusState> verifyPathConstraints(tx: LedgerTransaction, primaryStateClass: Class<T>){

        val commandValue = tx.commands.requireSingleCommand<AgreementContract.Commands>().value

        val txPath = getPath(tx, primaryStateClass, commandValue)

        val inputStatus = requireSingleInputStatus(tx, primaryStateClass)

        val allowedPaths: List<PathConstraint<T>> = when (inputStatus){
            null -> listOf(
                    PathConstraint(Commands.Propose(), PROPOSED, MultiplicityConstraint(0))
            )
            PROPOSED -> listOf(
                    PathConstraint(Commands.Reject(), REJECTED),
                    PathConstraint(Commands.Agree(), AGREED)
            )
            REJECTED -> listOf(
                    PathConstraint(Commands.Repropose(), PROPOSED)
            )
            AGREED -> listOf(
                    PathConstraint(Commands.Complete(), null, outputMultiplicityConstraint = MultiplicityConstraint(0))
            )
            else -> listOf()
        }

        requireThat {
            "txPath must be allowed by PathConstraints for inputStatus $inputStatus." using verifyPath(txPath, allowedPaths)
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

    fun verifyStatusConstraints(tx: LedgerTransaction){
        val allStates = tx.inputsOfType<AgreementState>() + tx.outputsOfType<AgreementState>()

        // Note, in kotlin non-nullable properties must be populated, hence only need to check the nullable properties of the AgreementState
        for (s in allStates) {

            when(s.status){
                PROPOSED -> {
                    requireThat {
                        "When status is Proposed rejectionReason must be null." using (s.rejectionReason == null)
                        "When status is Rejected rejectedBy must be null." using (s.rejectedBy == null)
                    }
                }
                REJECTED -> {
                    requireThat {
                        "When status is Rejected rejectionReason must not be null." using (s.rejectionReason != null)
                        "When status is Rejected rejectedBy must not be null." using (s.rejectedBy != null)
                        "When the Status is Rejected rejectedBy must be the buyer or seller." using (listOf(s.buyer, s.seller).contains(s.rejectedBy))
                    }
                }
                AGREED -> {}
            }
        }
    }

    fun verifyLinearIDConstraints(tx: LedgerTransaction){

        val command = tx.commands.requireSingleCommand<AgreementContract.Commands>()
        val inputStates = tx.inputsOfType<AgreementState>()
        val outputStates = tx.outputsOfType<AgreementState>()

        // Assume that if using LinearID we want a maximum of one Primary input state and a maximum one Primary output state
        // This is a guard which shouldn't be triggered because the Path constraints should have already ensured there is
        // a maximum of one Primary input state and a maximum one Primary output state
        requireThat{
            "When using LinearStates there should be a maximum of one Primary input state." using (inputStates.size <= 1)
            "When using LinearStates there should be a maximum of one Primary output state." using (outputStates.size <= 1)
        }

        val inputState = inputStates.singleOrNull()
        val outputState = outputStates.singleOrNull()

        val commandName = command.value::class.java.simpleName
        when (command.value){
            is Commands.Reject,
            is Commands.Repropose,
            is Commands.Agree-> {
                requireThat {"When the Command is $commandName the LinearID must not change." using(inputState?.linearId == outputState?.linearId)}
            }
        }
    }

    fun verifySigningConstraints(tx: LedgerTransaction){

        // This implementation assumes there is a maximum of one Primary input state and a maximum one Primary output state.
        // For Contracts which assume multiple Primary inputs or Output a different approach will be required

        val command = tx.commands.requireSingleCommand<AgreementContract.Commands>()
        val inputStates = tx.inputsOfType<AgreementState>()
        val outputStates = tx.outputsOfType<AgreementState>()

        // This is a guard which shouldn't be triggered because the Path constraints should have already ensured there is
        // a maximum of one Primary input state and a maximum one Primary output state
        requireThat{
            "when checking signing constraints there should be a maximum of one Primary input state." using (inputStates.size <= 1)
            "When checking signing constraints there should be a maximum of one Primary output state." using (outputStates.size <= 1)
        }

        val inputState = inputStates.singleOrNull()
        val outputState = outputStates.singleOrNull()

        val commandName = command.value::class.java.simpleName

        fun checkSigner(signerDescription: String, signer: Party?){
            requireThat { "When the Command is $commandName the $signerDescription must sign." using (command.signers.contains(signer?.owningKey))}

        }

        when (command.value){
            is Commands.Propose -> {
                checkSigner("output.proposer", outputState?.proposer)   // Can add multiple signing check against each Command
            }
            is Commands.Reject -> {
                checkSigner("output.rejectedBy", outputState?.rejectedBy)
            }
            is Commands.Repropose -> {
                checkSigner("output.proposer", outputState?.proposer)
            }
            is Commands.Agree -> {
                checkSigner("input.consenter", inputState?.consenter )
            }
            is Commands.Complete -> {
                checkSigner("input.seller", inputState?.seller)
            }
        }
    }

    fun verifyCommandConstraints(tx: LedgerTransaction){

        val command = tx.commands.requireSingleCommand<AgreementContract.Commands>()

        when (command.value){
            is Commands.Propose -> {
            }
            is Commands.Reject -> {
                // Path Constraints have already checked there is only one input and one output
                val inputState = tx.inputsOfType<AgreementState>().single()
                val outputState = tx.outputsOfType<AgreementState>().single()

                // Note, to check the majority of properties haven't change the code copies the outputstate but sets the changing properties to that of the input state. if all the other properties are the same, the copy should match the input state.
                requireThat {"When the command is Reject no properties can change except status, rejectionReason and rejectedBy." using (outputState.copy(
                        status = inputState.status,
                        rejectionReason = inputState.rejectionReason,
                        rejectedBy = inputState.rejectedBy) == inputState)}
            }
            is Commands.Repropose -> {
            }
            is Commands.Agree -> {
                requireThat {
                    // Path Constraints have already checked there is only one input and one output
                    val inputState = tx.inputsOfType<AgreementState>().single()
                    val outputState = tx.outputsOfType<AgreementState>().single()
                    requireThat {"When the command is Agree no properties can change except status." using (outputState.copy(status = inputState.status) == inputState)}
                }
            }
            is Commands.Complete -> {}
        }
    }
}