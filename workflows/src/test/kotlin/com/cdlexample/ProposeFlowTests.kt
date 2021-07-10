package com.cdlexample

import com.cdlexample.contracts.AgreementContract
import com.cdlexample.flows.ProposeFlow
import com.cdlexample.states.AgreementState
import com.cdlexample.states.AgreementStatus
import com.nhaarman.mockito_kotlin.*
import net.corda.systemflows.CollectSignaturesFlow
import net.corda.systemflows.FinalityFlow
import net.corda.testing.flow.utils.flowTest
import net.corda.v5.ledger.contracts.Amount
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import java.util.*

class ProposeFlowTests {

    @Test
    fun `propose flow test`() {

        val goods = "Some Sausages"
        val goodsValue = Amount(10, Currency.getInstance("GBP"))

        flowTest<ProposeFlow>{

            createFlow {
                ProposeFlow(ourIdentity, otherSide, goods, goodsValue, ourIdentity, otherSide)
            }
            //Mocking responder flow
            doReturn(signedTransactionMock)
                .whenever(flow.flowEngine)
                .subFlow(any<CollectSignaturesFlow>())
            doReturn(signedTransactionMock)
                .whenever(flow.flowEngine)
                .subFlow(any<FinalityFlow>())

            flow.call()

            // verify the correct output state is created
            argumentCaptor<AgreementState>().apply {
                verify(transactionBuilderMock).addOutputState(capture(), eq(AgreementContract.ID))
                SoftAssertions.assertSoftly {
                    it.assertThat(firstValue.status).isEqualTo(AgreementStatus.PROPOSED)
                    it.assertThat(firstValue.buyer).isEqualTo(ourIdentity)
                    it.assertThat(firstValue.seller).isEqualTo(otherSide)
                    it.assertThat(firstValue.price).isEqualTo(goodsValue)
                    it.assertThat(firstValue.goods).isEqualTo(goods)
                    it.assertThat(firstValue.proposer).isEqualTo(ourIdentity)
                    it.assertThat(firstValue.consenter).isEqualTo(otherSide)
                }
            }
        }
        //Note: previous version of the test checked node vault for record of the transaction: not possible here
        //because everything is mocked. See Integration Test for vault checking
    }
}