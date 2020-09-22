package com.cdlexample.states

import net.corda.core.contracts.ContractState
import net.corda.core.serialization.CordaSerializable

// todo: make sure these States can be serialised

@CordaSerializable
interface Status {
}

interface StatusState: ContractState {
    val status: Status?
}