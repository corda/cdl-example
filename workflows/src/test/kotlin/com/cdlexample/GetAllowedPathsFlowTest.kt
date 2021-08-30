package com.cdlexample

import com.cdlexample.contracts.AgreementContract
import com.cdlexample.flows.GetAllowedPathsFlow
import com.cdlexample.flows.ProposeFlow
import com.cdlexample.flows.ProposeResponderFlow
import com.cdlexample.flows.Responder
import com.cdlexample.states.AgreementState
import com.cdlexample.states.AgreementStatus
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

class GetAllowedPathsFlowTests {
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
    fun `GetAllowedPaths flow test`() {

        val flow = ProposeFlow(partyA, partyB, "Some Sausages", Amount(10, Currency.getInstance("GBP")), partyA, partyB)

        val future = a.startFlow(flow)
        network.runNetwork()
        val result = future.getOrThrow()

        // Check output state is returned
        val outputState = result.coreTransaction.outputStates.single() as AgreementState
        assert (outputState.goods == "Some Sausages")


        val bStateRef =  b.services.vaultService.queryBy<AgreementState>().states.single().ref

        val flow2 = GetAllowedPathsFlow(bStateRef)
        val future2 = b.startFlow(flow2)
        network.runNetwork()
        val result2 = future2.getOrThrow()

        println(result2)

        assert(result2.filter{it.command::class.java == AgreementContract.Commands.Reject()::class.java && it.outputStatus == AgreementStatus.REJECTED}.size == 1)
        assert(result2.filter{it.command::class.java == AgreementContract.Commands.Agree()::class.java && it.outputStatus == AgreementStatus.AGREED}.size == 1)
    }
}