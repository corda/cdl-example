package com.cdlexample.flows

import co.paralleluniverse.fibers.Suspendable
import com.cdlexample.contracts.AgreementContract
import com.cdlexample.contracts.PathConstraint
import com.cdlexample.states.AgreementState
import net.corda.core.contracts.StateRef
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByRPC
class GetAllowedPathsFlow(val stateRef: StateRef): FlowLogic<List<PathConstraint>>() {

    @Suspendable
    override fun call(): List<PathConstraint>{

        val stateAndRef = serviceHub.toStateAndRef<AgreementState>(stateRef)

        val status = stateAndRef.state.data.status

        return AgreementContract.allowedPaths(status)

    }

}