package com.cdlexample.contracts

import com.cdlexample.states.Status
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.ContractState



class Path(val command: CommandData, val outputStatus: Status?){

    override fun equals(other: Any?): Boolean {
        if(other == null) return false
        if (other::class.java != this::class.java) return false
        val castClass = other as Path
        if (castClass.command::class.java != this.command::class.java) return false
        if (castClass.outputStatus == null && this.outputStatus != null) return false
        if (castClass.outputStatus != null && this.outputStatus == null) return false
        if (castClass.outputStatus != null && this.outputStatus != null) {
            if (castClass.outputStatus::class.java != this.outputStatus::class.java) return false
        }
        return true
    }
}


class additionalPath(val clazz: Class<ContractState>){

    // todo: fill out equals + add in to Path with defaults as an empty list
    // todo: move into a Contract utils file
}