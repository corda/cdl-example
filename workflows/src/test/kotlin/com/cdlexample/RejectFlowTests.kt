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
import net.corda.core.node.services.vault.QueryCriteria
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

        // Create Proposal
        val flow = ProposeFlow(partyA, partyB, "Some Sausages", Amount(10, Currency.getInstance("GBP")), partyA, partyB)

        val future = a.startFlow(flow)
        network.runNetwork()
        val result = future.getOrThrow()


        // Create Rejection
        val proposedStateRef = StateRef(result.id, 0)
        val flow2 = RejectFlow(proposedStateRef, "I don't like Sausages")

        val future2 = b.startFlow(flow2)
        network.runNetwork()
        val result2 = future2.getOrThrow()


        // check rejection in the other party's vault
        val rejectedStateRef = StateRef(result2.id, 0)
        val queryCriteria = QueryCriteria.VaultQueryCriteria(stateRefs = listOf(rejectedStateRef))
        val outputState = b.services.vaultService.queryBy(AgreementState::class.java, queryCriteria).states.single().state.data

        assert(outputState.status == AgreementStatus.REJECTED)
        assert(outputState.rejectionReason == "I don't like Sausages")
    }
}