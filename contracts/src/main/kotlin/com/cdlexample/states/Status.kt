package com.cdlexample.states

import net.corda.core.contracts.ContractState
import net.corda.core.serialization.CordaSerializable

/**
 * The StatusState interface should be implemented for all Contract states that require a status field.
 */
interface StatusState: ContractState {
    val status: Status?
}

/**
 * Statuses are defined as enum classes in the StatusState which should implement this Status interface.
 */
@CordaSerializable
interface Status

