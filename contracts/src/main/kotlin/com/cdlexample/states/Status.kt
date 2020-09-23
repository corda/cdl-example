package com.cdlexample.states

import net.corda.core.contracts.ContractState
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
interface Status {
}

interface StatusState: ContractState {
    val status: Status?
}