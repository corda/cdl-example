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

        verifyTransitionConstraints(tx)
        verifyUniversalConstraints(tx)
        verifyStatusConstraints(tx)

    }


    fun verifyTransitionConstraints(tx: LedgerTransaction){

        val command = tx.commands.requireSingleCommand<Commands>()
        val inputStatus = requireSingleInputStatus(tx)
        val outputStatus = requireSingleOutputStatus(tx)

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

        // Note, in kotlin non-nullable properties must be populated, hence only need to check the nullable properties
        for (s in allStates) {

            when(s.status){
                is Proposed -> {
                    requireThat {
                        "When status is Proposed rejectionReason must be null" using (s.rejectionReason == null)
                        "When status is Rejected rejectedBy must be null" using (s.rejectedBy == null)
                    }
                }
                is Rejected -> {
                    requireThat {
                        "When status is Rejected rejectionReason must not be null" using (s.rejectionReason != null)
                        "When status is Rejected rejectedBy must not be null" using (s.rejectedBy != null)
                    }

                }
                is Agreed -> {}
            }
        }
    }


//    class Path(val command: Commands, val outputStatus: Status?){
//
//        override fun equals(other: Any?): Boolean {
//            if(other == null) return false
//            if (other::class.java != this::class.java) return false
//            val castClass = other as Path
//            if (castClass.command::class.java != this.command::class.java) return false
//            if (castClass.outputStatus == null && this.outputStatus != null) return false
//            if (castClass.outputStatus != null && this.outputStatus == null) return false
//            if (castClass.outputStatus != null && this.outputStatus != null) {
//                if (castClass.outputStatus::class.java != this.outputStatus::class.java) return false
//            }
//            return true
//        }
//    }
//
//
//
//    class additionalPath(val clazz: Class<ContractState>, val status: Status?){
//
//        // todo: fill out equals + add in to Path with defaults as an empty list
//        // todo: move into a Contract utils file
//    }







    fun requireSingleInputStatus(tx:LedgerTransaction): Status?{
        return requireSingleStatus(tx.inputsOfType<AgreementState>(),"All inputs of type AgreementState must have the same status.")
    }

    fun requireSingleOutputStatus(tx:LedgerTransaction): Status?{
        return requireSingleStatus(tx.outputsOfType<AgreementState>(), "All outputs of type AgreementState must have the same status.")
    }

    //todo: make generic + move in to a contract utils file
    fun requireSingleStatus(states: List<AgreementState>, error: String): Status?{
        val statuses = states.map {it.status::class.java}.distinct()
        requireThat {
            error using ( statuses.size <= 1)}
        var status: Status? = null
        if(states.isNotEmpty()) status = states.first().status
        return status

    }


}