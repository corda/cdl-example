package com.cdlexample.contracts

import com.cdlexample.states.Status
import com.cdlexample.states.StatusState
import net.corda.core.contracts.*

// todo: consider if these classes should be data classes or not
// todo: should the constraints be passing error messages back rather than false?
// todo: do these need to be locked to State Type?

data class Path(val commandClass: Class<out CommandData>,
                val outputStatus: Status?,
                val numberOfInputStates: Int,
                val numberOfOutputStates: Int,
                val additionalInputs: Set<AdditionalStateType> = setOf(),
                val additionalOutputs: Set<AdditionalStateType> = setOf())


data class AdditionalStateType(val type: Class<out ContractState>, val numberOfStates: Int)


class PathConstraint(val commandClass: Class<out CommandData>,
                     val outputStatus: Status?,
                     val multiplicityIn: Multiplicity = Multiplicity(),
                     val multiplicityOut: Multiplicity = Multiplicity(),
                     val additionalInputsConstraints: AdditionalStatesConstraints = AdditionalStatesConstraints(),
                     val additionalOutputsConstraints: AdditionalStatesConstraints = AdditionalStatesConstraints()){

    infix fun allows(p: Path): Boolean{
            var allows = true
            if (commandClass != p.commandClass) allows = false
            if (outputStatus != p.outputStatus) allows = false
            if (multiplicityIn doesNotAllow p.numberOfInputStates) allows = false
            if (multiplicityOut doesNotAllow p.numberOfOutputStates) allows = false
            if (additionalInputsConstraints isNotSatisfiedBy p.additionalInputs) allows = false
            if (additionalOutputsConstraints isNotSatisfiedBy p.additionalOutputs) allows = false
            return allows
    }

    infix fun doesNotAllow(p: Path): Boolean {
        return !this.allows(p)
    }
}

data class AdditionalStatesConstraints(val constraints: Set<AdditionalStatesConstraint> = setOf()) {

    infix fun isSatisfiedBy(additionalStatesType: Set<AdditionalStateType>): Boolean {
        for (c in constraints){
            var satisfied = false
            for (s in additionalStatesType){
                if (c isSatisfiedBy s) satisfied = true
            }
            if (!satisfied) return false
        }
        return true
    }
    infix fun isNotSatisfiedBy(additionalStatesType: Set<AdditionalStateType>): Boolean {
        return !isSatisfiedBy(additionalStatesType)
    }
}

data class AdditionalStatesConstraint(val type: Class<out ContractState>, val requiredNumberOfStates: Multiplicity) {

    infix fun isSatisfiedBy(additionalStates: AdditionalStateType ):Boolean {
        var match = true
        if (type != additionalStates.type) return false
        if (requiredNumberOfStates.doesNotAllow(additionalStates.numberOfStates)) return false
        return true
    }
    infix fun isNotSatisfiedBy (additionalStates: AdditionalStateType): Boolean{
        return !isSatisfiedBy(additionalStates)
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

fun <T: StatusState>requireSingleStatus (states: List<T>, error: String): Status?{
    val statuses = states.map {it.status}.distinct()
    requireThat {
        error using ( statuses.size <= 1)}
    return if (states.isNotEmpty()) states.first().status else null
}