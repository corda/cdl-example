package com.cdlexample.contracts

import com.cdlexample.states.Status
import com.cdlexample.states.StatusState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireThat


data class Path(val command: Class<out CommandData>,
           val outputStatus: Status?,
           val additionalInputs: Set<Class<out ContractState>> = setOf(),
           val additionalOutputs: Set<Class<out ContractState>> = setOf())


fun <T: StatusState>requireSingleStatus (states: List<T>, error: String): Status?{
    val statuses = states.map {it.status}.distinct()
    requireThat {
        error using ( statuses.size <= 1)}
    return if (states.isNotEmpty()) states.first().status else null
}