package com.cdlexample

import com.cdlexample.flows.ProposeFlow
import com.cdlexample.flows.ProposeResponderFlow
import com.cdlexample.flows.Responder
import com.cdlexample.states.AgreementState
import net.corda.core.contracts.Amount
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.contextLogger
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

class ProposeFlowTests {
    private val network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
        TestCordapp.findCordapp("com.cdlexample.contracts"),
        TestCordapp.findCordapp("com.cdlexample.flows")
    )))
    private val a = network.createNode()
    private val b = network.createNode()

    init {
        listOf(a, b).forEach {
            it.registerInitiatedFlow(ProposeResponderFlow::class.java)
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

        // Check output state is returned
        val outputState = result.coreTransaction.outputStates.single() as AgreementState
        assert (outputState.goods == "Some Sausages")

        // Check a and b vaults for state
        assert( outputState.linearId == a.services.vaultService.queryBy<AgreementState>().states.single().state.data.linearId)
        assert( outputState.linearId == b.services.vaultService.queryBy<AgreementState>().states.single().state.data.linearId)
    }
}