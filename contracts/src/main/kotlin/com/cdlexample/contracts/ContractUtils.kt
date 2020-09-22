package com.cdlexample.contracts

import com.cdlexample.states.Status
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState



data class Path(val command: Class<out CommandData>,
           val outputStatus: Status?,
           val additionalInputs: Set<Class<out ContractState>> = setOf(),
           val additionalOutputs: Set<Class<out ContractState>> = setOf())


