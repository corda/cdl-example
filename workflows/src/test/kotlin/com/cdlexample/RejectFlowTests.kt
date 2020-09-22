package com.cdlexample

import com.cdlexample.flows.ProposeFlow
import com.cdlexample.flows.ProposeResponderFlow
import com.cdlexample.flows.RejectFlow
import com.cdlexample.flows.RejectResponderFlow
import com.cdlexample.states.AgreementState
import com.cdlexample.states.AgreementStatus
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateRef
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*


class RejectFlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.cdlexample.contracts"),
            TestCordapp.findCordapp("com.cdlexample.flows")
    )))
    private val a = network.createNode()
    private val b = network.createNode()

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(ProposeResponderFlow::class.java)
            it.registerInitiatedFlow(RejectResponderFlow::class.java)
        }
    }

    val partyA = a.info.legalIdentities.first()
    val partyB = b.info.legalIdentities.first()


    @Before
    fun setup() = network.runNetwork()

    @After
    fun tearDown() = network.stopNodes()

    @Test
    fun `propose flow test`() {

        val flow = ProposeFlow(partyA, partyB, "Some Sausages", Amount(10, Currency.getInstance("GBP")), partyA, partyB)

        val future = a.startFlow(flow)
        network.runNetwork()
        val result = future.getOrThrow()

        val proposedStateRef = StateRef(result.tx.id, 0)
        val flow2 = RejectFlow(proposedStateRef, "I don't like Sausages")

        val future2 = b.startFlow(flow2)
        network.runNetwork()
        val result2 = future2.getOrThrow()

        val outputState = result2.coreTransaction.outputStates.single() as AgreementState

        assert(outputState.status == AgreementStatus.REJECTED)
        assert(outputState.rejectionReason == "I don't like Sausages")


    }
}