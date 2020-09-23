package com.cdlexample.contracts

import com.cdlexample.states.Status
import com.cdlexample.states.StatusState
import net.corda.core.contracts.*


data class Path(val commandClass: Class<out CommandData>,
                val outputStatus: Status?,
                val numberOfInputStates: Int,
                val numberOfOutputStates: Int,
                val additionalInputs: Set<AdditionalStates> = setOf(),
                val additionalOutputs: Set<AdditionalStates> = setOf()){
}

data class AdditionalStates(val type: Class<out ContractState>, val numberOfStates: Int)

class PathConstraint(val commandClass: Class<out CommandData>,
                     val outputStatus: Status?,
                     val multiplicityIn: Multiplicity = Multiplicity(),
                     val multiplicityOut: Multiplicity = Multiplicity(),
                     val additionalInputs: Set<AdditionalStates> = setOf(),
                     val additionalOutputs: Set<AdditionalStates> = setOf()){

    infix fun allows(p: Path): Boolean{
            var match = true
            if (commandClass != p.commandClass) match = false
            if (outputStatus != p.outputStatus) match = false
            if (multiplicityIn doesNotAllow p.numberOfInputStates) match = false
            if (multiplicityOut doesNotAllow p.numberOfOutputStates) match = false

            // todo: check all required additional states are present
            if (match) return true
            return false
    }

    infix fun doesNotAllow(p: Path): Boolean {
        return !this.allows(p)
    }


}

data class Multiplicity(val from: Int = 1,
                        val bounded: Boolean = true,
                        val to: Int = from){

    infix fun allows(numberOfStates: Int): Boolean{
        if (numberOfStates < from) return false
        if (bounded && numberOfStates > to) return false
        return true
    }
    infix fun doesNotAllow(numberOfStates: Int): Boolean {
        return !this.allows(numberOfStates)
    }
}


fun verifyPath(p: Path, pathConstraintList: List<PathConstraint>): Boolean{

    for (pc in pathConstraintList) {
        if (pc allows p) return true
    }
    return false
}





// todo, multiplicities type as an enum ??, with exact number as optional field


fun <T: StatusState>requireSingleStatus (states: List<T>, error: String): Status?{
    val statuses = states.map {it.status}.distinct()
    requireThat {
        error using ( statuses.size <= 1)}
    return if (states.isNotEmpty()) states.first().status else null
}