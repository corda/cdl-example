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
@BelongsToContract(AgreementContract::class)
data class AgreementState(val status: AgreementStatus,
                          val buyer: Party,
                          val seller: Party,
                          val goods: String,
                          val price: Amount<Currency>,
                          val proposer: Party,
                          val consenter: Party,
                          val rejectionReason: String? = null,
                          val rejectedBy: Party?= null,
                          override val participants: List<AbstractParty> = listOf(buyer, seller),
                          override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, StandardState {

    override fun getParty(role: String) = when (role.toUpperCase()) {
        "BUYER" -> buyer
        "SELLER" -> seller
        "PROPOSER" -> proposer
        "CONSENTER" -> consenter
        "REJECTOR" -> rejectedBy ?: throw RuntimeException("The role '$role' has undefined value in this state")
        else -> throw RuntimeException("The role '$role' is unknown in the AgreementState")
    }

    override fun isInStatus(status: String) = AgreementStatus.valueOf(status.toUpperCase()) == this.status
}

@CordaSerializable
enum class AgreementStatus {
    PROPOSED,
    REJECTED,
    AGREED
}
