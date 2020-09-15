package com.cdlexample.contracts

import com.cdlexample.states.AgreementState
import com.cdlexample.states.AgreementStatus.*
import com.cdlexample.states.Status
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

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {

        // Extract command, inputs and outputs

        val command = tx.commands.requireSingleCommand<Commands>()
        val inputAgreementStates = tx.inputsOfType<AgreementState>()
        val outputAgreementStates = tx.outputsOfType<AgreementState>()

        // Check Some assumed invariants and get Statuses

        val inputStatuses = inputAgreementStates.map {it.status::class.java}.distinct()
        val outputStatuses = outputAgreementStates.map {it.status::class.java}.distinct()

        requireThat {
            "All inputs of type AgreementState have the same Status." using ( inputStatuses.size <= 1)
            "All outputs of type AgreementState have the same Status." using ( outputStatuses.size <= 1)}

        var inputStatus: Status? = null
        if(inputAgreementStates.isNotEmpty()) inputStatus = inputAgreementStates.first().status

        var outputStatus: Status? = null
        if (outputAgreementStates.isNotEmpty()) outputStatus = outputAgreementStates.first().status


        // Transition Constraints - Commands

        val txPath =  Path(command.value, outputStatus)
        when (inputStatus) {
            null -> {
                val pathList = listOf(
                        Path(Commands.Propose(), Proposed()))
                requireThat {
                    "When there is no input AgreementState the path must be Propose -> Proposed." using (pathList.contains(txPath))
                }
            }
            is Proposed -> {
                val pathList = listOf(
                        Path(Commands.Reject(), Rejected()),
                        Path(Commands.Agree(), Agreed())
                )
                requireThat {
                    "When the input Status is Proposed, the path must be Reject -> Rejected, or Agree -> Agreed." using (pathList.contains(txPath))
                }
            }
            is Rejected -> {
                val pathList = listOf(
                        Path(Commands.Repropose(), Proposed()))
                requireThat {
                    "When the input Status is Rejected, the path must be Repropose -> Proposed." using (pathList.contains(txPath))
                }
            }
            is Agreed -> {
                val pathList = listOf(
                        Path(Commands.Complete(), null)
                )
                requireThat {
                    "When the input Status is Agree, the path must be Complete -> null." using (pathList.contains(txPath))
                }
            }
        }

    }

    class Path(val command: Commands, val outputStatus: Status?){



        override fun equals(other: Any?): Boolean {
            if(other == null) return false
            if (other::class.java != this::class.java) return false
            val castClass = other as Path
            if (castClass.command::class.java != this.command::class.java) return false
            if (castClass.outputStatus == null && this.outputStatus != null) return false
            if (castClass.outputStatus != null && this.outputStatus == null) return false
            if (castClass.outputStatus != null && this.outputStatus != null) {
                if (castClass.outputStatus!!::class.java != this.outputStatus!!::class.java) return false
            }
            return true
        }
    }



    // todo: consider adding archive Command - allows testing of no outputs

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Propose : Commands
        class Repropose: Commands
        class Reject: Commands
        class Agree: Commands
        class Complete: Commands
    }
}