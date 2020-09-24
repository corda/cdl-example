package com.cdlexample.contracts

import com.cdlexample.states.Status
import com.cdlexample.states.StatusState
import net.corda.core.contracts.*

// todo: do these need to be locked to State Type? -> could then lock allowed commands
// todo: show we revert back to Command rather than Command::class.java?

class Path(val command: CommandData,
                val outputStatus: Status?,
                val numberOfInputStates: Int,
                val numberOfOutputStates: Int,
                val additionalStates: Set<AdditionalStates> = setOf())


class AdditionalStates(val type: AdditionalStatesType, val clazz: Class<out ContractState>, val numberOfStates: Int)

enum class AdditionalStatesType {INPUT, OUTPUT, REFERENCE}

class PathConstraint(val command: CommandData,
                     val outputStatus: Status?,
                     val inputMultiplicityConstraint: MultiplicityConstraint = MultiplicityConstraint(),
                     val outputMultiplicityConstraint: MultiplicityConstraint = MultiplicityConstraint(),
                     val additionalStatesConstraints: Set<AdditionalStatesConstraint> =  setOf()){

    infix fun allows(p: Path): Boolean{
            var allows = true
            if (command::class.java != p.command::class.java) allows = false
            if (outputStatus != p.outputStatus) allows = false
            if (inputMultiplicityConstraint doesNotAllow p.numberOfInputStates) allows = false
            if (outputMultiplicityConstraint doesNotAllow p.numberOfOutputStates) allows = false
            if (!additionalStatesCheck(additionalStatesConstraints, p.additionalStates)) allows = false
            return allows
    }

    infix fun doesNotAllow(p: Path): Boolean {
        return !this.allows(p)
    }

    private fun additionalStatesCheck(constraints: Set<AdditionalStatesConstraint>, additionalStates: Set<AdditionalStates>) :Boolean{
        for (c in constraints){
            var satisfied = false
            for (s in additionalStates){
                if (c isSatisfiedBy s) satisfied = true
            }
            if (!satisfied) return false
        }
        return true
    }
}

class AdditionalStatesConstraint(val type: AdditionalStatesType ,val clazz: Class<out ContractState>, val requiredNumberOfStates: MultiplicityConstraint) {

    infix fun isSatisfiedBy(additionalStates: AdditionalStates ):Boolean {
        var match = true
        if (type != additionalStates.type) return false
        if (clazz != additionalStates.clazz) return false
        if (requiredNumberOfStates.doesNotAllow(additionalStates.numberOfStates)) return false
        return true
    }
    infix fun isNotSatisfiedBy (additionalStates: AdditionalStates): Boolean{
        return !isSatisfiedBy(additionalStates)
    }
}

class MultiplicityConstraint(val from: Int = 1,
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