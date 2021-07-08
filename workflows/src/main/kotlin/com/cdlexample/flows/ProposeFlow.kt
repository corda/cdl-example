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
import net.corda.v5.application.identity.Party
import net.corda.v5.application.injection.CordaInject
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.Command
import net.corda.v5.ledger.services.NotaryLookupService
import net.corda.v5.ledger.transactions.SignedTransaction
import net.corda.v5.ledger.transactions.TransactionBuilderFactory
import java.util.*

// todo:  write the rest of the flows

@InitiatingFlow
@StartableByRPC
class ProposeFlow(val buyer: Party,
                  val seller: Party,
                  val goods: String,
                  val price: Amount<Currency>,
                  val proposer: Party,
                  val consenter: Party): Flow<SignedTransaction>{

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

    @Suspendable
    override fun call(): SignedTransaction {

        val notary = notaryService.notaryIdentities.first()

        val proposeState = AgreementState(AgreementStatus.PROPOSED, buyer, seller, goods, price, proposer, consenter)
        val command = Command(AgreementContract.Commands.Propose(), proposer.owningKey)

        val tx = transactionBuilder.create()
            .setNotary(notary).addOutputState(proposeState, AgreementContract.ID).addCommand(command)

        tx.verify()

        val signedTx = tx.sign()

        val counterparty = if (buyer == flowIdentity.ourIdentity) seller else buyer
        val session = flowMessaging.initiateFlow(counterparty)

        return flowEngine.subFlow(FinalityFlow(signedTx, listOf(session)))
    }
}

@InitiatedBy(ProposeFlow::class)
class ProposeResponderFlow(val counterpartySession: FlowSession): Flow<SignedTransaction>{

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(): SignedTransaction{
        return flowEngine.subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}


