package com.cdlexample.contracts

import com.cdlexample.states.Status
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState



class Path(val command: CommandData,
           val outputStatus: Status?,
           val additionalInputs: Set<Class<out ContractState>> = setOf(),
           val additionalOutputs: Set<Class<out ContractState>> = setOf()){

        override fun equals(other: Any?): Boolean {

        if (other !is Path) return false
        if (other.command::class.java != this.command::class.java) return false // note class comparison
        if (other.outputStatus != this.outputStatus) return false
        if (other.additionalInputs != this.additionalInputs) return false
        if (other.additionalOutputs != this.additionalOutputs) return false
        return true
    }
}

