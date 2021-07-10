package com.cdlexample.flows

import net.corda.v5.application.flows.*
import net.corda.v5.base.annotations.Suspendable


// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class Initiator : Flow<Unit> {
    //val progressTracker = CustomProgressTracker

    @Suspendable
    override fun call() {
        // Initiator flow logic goes here.
    }
}

@InitiatedBy(Initiator::class)
class Responder(val counterpartySession: FlowSession) : Flow<Unit> {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
    }
}
