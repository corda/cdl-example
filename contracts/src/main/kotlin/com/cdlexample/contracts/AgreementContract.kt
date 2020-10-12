package com.cdlexample.contracts

import com.cdlexample.states.AgreementState
import com.cdlexample.states.AgreementStatus.*
import net.corda.contractsdk.StandardContract
import net.corda.contractsdk.annotations.*
import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
@MustBeDistinctPartiesWithinEachOutputStateList(
    MustBeDistinctPartiesWithinEachOutputState("Buyer","Seller"),
    MustBeDistinctPartiesWithinEachOutputState("Consenter","Proposer")
)
@SamePartyMustAssumeAtLeastOneOtherRoleWithinEachOutputStateList(
    SamePartyMustAssumeAtLeastOneOtherRoleWithinEachOutputState("Proposer", "Buyer", "Seller"),
    SamePartyMustAssumeAtLeastOneOtherRoleWithinEachOutputState("Consenter", "Buyer", "Seller")
)
class AgreementContract : StandardContract(Commands::class.java), Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.cdlexample.contracts.AgreementContract"
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        @NumberOfInputStates(0)
        @NumberOfOutputStatesAtLeast(1)
        @AllowedStatusOnOutput("Proposed")
        @RequiredSignersFromOutput("Proposer")
        @PropertiesOnOutputMustNotBeSet("rejectionReason", "rejectedBy")
        class Propose : Commands

        @AllowedStatusChangeInCoupledLinearStates("Rejected", "Proposed")
        @RequiredSignersFromOutput("Proposer")
        class Repropose: Commands

        @AllowedStatusChangeInCoupledLinearStates("Proposed", "Rejected")
        @LimitedChangeInCoupledLinearStates("status", "rejectionReason", "rejectedBy")
        @PropertiesOnOutputMustBeSet("rejectedBy", "rejectionReason")
        @RequiredSignersFromOutput("Rejector")
        class Reject: Commands

        @AllowedStatusChangeInCoupledLinearStates("Proposed", "Agreed")
        @LimitedChangeInCoupledLinearStates("status")
        @RequiredSignersFromInput("Consenter")
        class Agree: Commands

        @NumberOfInputStatesAtLeast(1)
        @NumberOfOutputStates(0)
        @AllowedStatusOnInput("Agreed")
        @RequiredSignersFromInput("Seller")
        class Complete: Commands
    }

}