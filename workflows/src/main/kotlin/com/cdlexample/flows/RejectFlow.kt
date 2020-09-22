package com.cdlexample.flows

import co.paralleluniverse.fibers.Suspendable
import com.cdlexample.contracts.AgreementContract
import com.cdlexample.states.AgreementState
import com.cdlexample.states.AgreementStatus
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import org.hibernate.Transaction

@InitiatingFlow
@StartableByRPC
class RejectFlow(val proposedStateRef: StateRef, val rejectionReason: String): FlowLogic<SignedTransaction>(){


    @Suspendable
    override fun call(): SignedTransaction{

        val queryCriteria = QueryCriteria.VaultQueryCriteria(stateRefs = listOf(proposedStateRef))
        val results = serviceHub.vaultService.queryBy(AgreementState::class.java, queryCriteria).states

        if (results.isEmpty()) throw FlowException("Proposal state not found for StateRef: $proposedStateRef.")
        if (results.size > 1 ) throw FlowException("Search for StateRef $proposedStateRef found more than one result.")

        val proposedStateAndRef = results.single()
        val proposedState = proposedStateAndRef.state.data

        val rejectedState = proposedState.copy(status = AgreementStatus.REJECTED, rejectionReason = rejectionReason, rejectedBy = ourIdentity)
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val tx = TransactionBuilder(notary)
        val command = Command(AgreementContract.Commands.Reject(), ourIdentity.owningKey)

        tx.addInputState(proposedStateAndRef)
        tx.addCommand(command)
        tx.addOutputState(rejectedState, AgreementContract.ID)
        tx.verify(serviceHub)

        val signedTx = serviceHub.signInitialTransaction(tx)

        val counterparty = if (proposedState.buyer == ourIdentity) proposedState.seller else proposedState.buyer
        val session = initiateFlow(counterparty)

        return subFlow(FinalityFlow(signedTx, listOf(session)))
    }
}

@InitiatedBy(RejectFlow::class)
class RejectResponderFlow(val counterpartySession: FlowSession): FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction{
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}