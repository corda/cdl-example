package com.cdlexample.flows

import co.paralleluniverse.fibers.Suspendable
import com.cdlexample.contracts.AgreementContract
import com.cdlexample.states.AgreementState
import com.cdlexample.states.AgreementStatus
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

// todo:  write the rest of the flows

@InitiatingFlow
@StartableByRPC
class ProposeFlow(val buyer: Party,
                  val seller: Party,
                  val goods: String,
                  val price: Amount<Currency>,
                  val proposer: Party,
                  val consenter: Party): FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction {

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val tx = TransactionBuilder(notary)

        val proposeState = AgreementState(AgreementStatus.PROPOSED, buyer, seller, goods, price, proposer, consenter)
        val command = Command(AgreementContract.Commands.Propose(), proposer.owningKey)

        tx.addCommand(command)
        tx.addOutputState(proposeState, AgreementContract.ID)

        tx.verify(serviceHub)

        val signedTx = serviceHub.signInitialTransaction(tx)

        val counterparty = if (buyer == ourIdentity) seller else buyer
        val session = initiateFlow(counterparty)

        return subFlow(FinalityFlow(signedTx, listOf(session)))
    }
}

@InitiatedBy(ProposeFlow::class)
class ProposeResponderFlow(val counterpartySession: FlowSession): FlowLogic<SignedTransaction>(){

    @Suspendable
    override fun call(): SignedTransaction{
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}


