package com.cdlexample.flows

import com.cdlexample.contracts.AgreementContract
import com.cdlexample.states.AgreementState
import com.cdlexample.states.AgreementStatus
import net.corda.systemflows.FinalityFlow
import net.corda.systemflows.ReceiveFinalityFlow
import net.corda.v5.application.flows.*
import net.corda.v5.application.flows.flowservices.FlowEngine
import net.corda.v5.application.flows.flowservices.FlowIdentity
import net.corda.v5.application.flows.flowservices.FlowMessaging
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.application.services.persistence.PersistenceService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.contracts.StateRef
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.transactions.SignedTransaction
import net.corda.v5.ledger.transactions.TransactionBuilderFactory


@InitiatingFlow
@StartableByRPC
class RejectFlow(val proposedStateRef: StateRef, val rejectionReason: String): Flow<SignedTransaction>{


    @CordaInject
    lateinit var transactionBuilder: TransactionBuilderFactory
    @CordaInject
    lateinit var notaryService: NotaryLookupService
    @CordaInject
    lateinit var flowIdentity: FlowIdentity
    @CordaInject
    lateinit var flowMessaging: FlowMessaging
    @CordaInject
    lateinit var flowEngine: FlowEngine
    @CordaInject
    lateinit var persistenceService: PersistenceService

    @Suspendable
    override fun call(): SignedTransaction{

//        val queryCriteria = QueryCriteria.VaultQueryCriteria(stateRefs = listOf(proposedStateRef))
//        val results = serviceHub.vaultService.queryBy(AgreementState::class.java, queryCriteria).states
        val results = persistenceService.find(AgreementState::class.java,listOf(proposedStateRef))

        if (results.isEmpty()) throw FlowException("Proposal state not found for StateRef: $proposedStateRef.")
        if (results.size > 1 ) throw FlowException("Search for StateRef $proposedStateRef found more than one result.")

        val proposedState = results.single()

        val rejectedState = proposedState.copy(status = AgreementStatus.REJECTED, rejectionReason = rejectionReason, rejectedBy = flowIdentity.ourIdentity)
        val notary = notaryService.notaryIdentities.first()

        val command = Command(AgreementContract.Commands.Reject(), flowIdentity.ourIdentity.owningKey)

        val tx = transactionBuilder.create()
            .setNotary(notary)
            .addCommand(command)
            .addOutputState(rejectedState, AgreementContract.ID)

        tx.verify()

        val signedTx = tx.sign()

        val counterparty = if (proposedState.buyer == flowIdentity.ourIdentity) proposedState.seller else proposedState.buyer
        val session = flowMessaging.initiateFlow(counterparty)

        return flowEngine.subFlow(FinalityFlow(signedTx, listOf(session)))
    }
}

@InitiatedBy(RejectFlow::class)
class RejectResponderFlow(val counterpartySession: FlowSession): Flow<SignedTransaction>{

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(): SignedTransaction{
        return flowEngine.subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}