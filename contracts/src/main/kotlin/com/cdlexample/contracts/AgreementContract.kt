package com.cdlexample.contracts

import com.cdlexample.states.AgreementState
import com.r3.corda.lib.contracts.contractsdk.StandardContract
import com.r3.corda.lib.contracts.contractsdk.annotations.*
import net.corda.core.contracts.*

// ************
// * Contract *
// ************
@RequireDistinctPartiesWithinEachOutputStateList(
    RequireDistinctPartiesWithinEachOutputState("Buyer","Seller"),
    RequireDistinctPartiesWithinEachOutputState("Consenter","Proposer")
)
@RequirePartyToAssumeAtLeastOneOtherRoleWithinEachOutputStateList(
    RequirePartyToAssumeAtLeastOneOtherRoleWithinEachOutputState("Proposer", "Buyer", "Seller"),
    RequirePartyToAssumeAtLeastOneOtherRoleWithinEachOutputState("Consenter", "Buyer", "Seller")
)
class AgreementContract : StandardContract(Commands::class.java), Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.cdlexample.contracts.AgreementContract"
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        @RequireNumberOfStatesOnInput(0, targetClasses = [AgreementState::class])
        @RequireNumberOfStatesOnOutputAtLeast(1)
        @PermitStatusOnOutput("Proposed")
        @RequireSignersFromEachOutputState("Proposer")
        @RequirePropertiesNotSetOnOutput("rejectionReason", "rejectedBy")
        class Propose : Commands

        @RequireStatusChangeInCoupledLinearStates("Rejected", "Proposed")
        @RequireSignersFromEachOutputState("Proposer")
        class Repropose: Commands

        @RequireStatusChangeInCoupledLinearStates("Proposed", "Rejected")
        @ForbidChangeInCoupledLinearStatesExcept("status", "rejectionReason", "rejectedBy")
        @RequirePropertiesSetOnOutput("rejectedBy", "rejectionReason")
        @RequireSignersFromEachOutputState("Rejector")
        class Reject: Commands

        @RequireStatusChangeInCoupledLinearStates("Proposed", "Agreed")
        @ForbidChangeInCoupledLinearStatesExcept("status")
        @RequireSignersFromEachInputState("Consenter")
        class Agree: Commands

        @RequireNumberOfStatesOnInputAtLeast(1)
        @RequireNumberOfStatesOnOutput(0)
        @PermitStatusOnInput("Agreed")
        @RequireSignersFromEachInputState("Seller")
        class Complete: Commands
    }

}