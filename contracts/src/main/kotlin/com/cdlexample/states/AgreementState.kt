package com.cdlexample.states

import com.cdlexample.contracts.AgreementContract
import com.r3.corda.lib.contracts.contractsdk.verifiers.StandardState
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.lang.RuntimeException
import java.util.*

// *********
// * State *
// *********

@BelongsToContract(MyAwesomeContract::class)
data class MyAwesomeState(val status: Status,
                          val buyer: Party,
                          val seller: Party,
                          override val participants: List<AbstractParty> = listOf(buyer, seller),
                          override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, StandardState {

    override fun getParty(role: String) = when (role.toUpperCase()) {
        "BUYER" -> buyer
        "SELLER" -> seller
        else -> throw RuntimeException("The role '$role' is unknown in the MyAwesomeState")
    }

    override fun isInStatus(status: String) = Status.valueOf(status.toUpperCase()) == this.status
}

@CordaSerializable
enum class AgreementStatus {
    PROPOSED,
    REJECTED,
    AGREED
}
