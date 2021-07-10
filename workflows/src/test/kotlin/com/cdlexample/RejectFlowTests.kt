package com.cdlexample

import com.cdlexample.contracts.AgreementContract
import com.cdlexample.flows.RejectFlow
import com.cdlexample.states.AgreementState
import com.cdlexample.states.AgreementStatus
import com.nhaarman.mockito_kotlin.*
import net.corda.systemflows.CollectSignaturesFlow
import net.corda.systemflows.FinalityFlow
import net.corda.testing.flow.utils.flowTest
import net.corda.v5.crypto.SecureHash
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.StateRef
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import java.util.*


class RejectFlowTests {

    @Test
    fun `propose flow test`() {
        val goods = "Some Sausages"
        val goodsValue = Amount(10, Currency.getInstance("GBP"))
        val message = "I don't like Sausages"

        val mockedStateRef = StateRef(SecureHash.randomSHA256(), 0)


        flowTest<RejectFlow>{
            createFlow {
                RejectFlow(mockedStateRef, message)
            }

            //simulating existing Agreement state in Vault:
            val mockedExistingAgreementState = AgreementState(
                status = AgreementStatus.PROPOSED,
                buyer = otherSide,
                seller = ourIdentity,
                goods = goods,
                price = goodsValue,
                proposer = otherSide,
                consenter = ourIdentity,
                linearId = UniqueIdentifier()
            )
            doReturn(listOf(mockedExistingAgreementState)).whenever(flow.persistenceService).find(AgreementState::class.java,listOf(mockedStateRef))

            //Mocking responder flow
            doReturn(signedTransactionMock)
                .whenever(flow.flowEngine)
                .subFlow(any<CollectSignaturesFlow>())
            doReturn(signedTransactionMock)
                .whenever(flow.flowEngine)
                .subFlow(any<FinalityFlow>())

            flow.call()

            argumentCaptor<AgreementState>().apply {
                verify(transactionBuilderMock).addOutputState(capture(), eq(AgreementContract.ID))
                SoftAssertions.assertSoftly {
                    it.assertThat(firstValue.status).isEqualTo(AgreementStatus.REJECTED)
                    it.assertThat(firstValue.rejectionReason).isEqualTo(message)
                }
            }

        }


//
//        // Create Proposal
//        val flow = ProposeFlow(partyA, partyB, "Some Sausages", Amount(10, Currency.getInstance("GBP")), partyA, partyB)
//
//        val future = a.startFlow(flow)
//        network.runNetwork()
//        val result = future.getOrThrow()
//
//
//        // Create Rejection
//        val proposedStateRef = StateRef(result.id, 0)
//        val flow2 = RejectFlow(proposedStateRef, "I don't like Sausages")
//
//        val future2 = b.startFlow(flow2)
//        network.runNetwork()
//        val result2 = future2.getOrThrow()
//
//
//        // check rejection in the other party's vault
//        val rejectedStateRef = StateRef(result2.id, 0)
//        val queryCriteria = QueryCriteria.VaultQueryCriteria(stateRefs = listOf(rejectedStateRef))
//        val outputState = b.services.vaultService.queryBy(AgreementState::class.java, queryCriteria).states.single().state.data
//
//        assert(outputState.status == AgreementStatus.REJECTED)
//        assert(outputState.rejectionReason == "I don't like Sausages")
    }
}