package com.cdlexample.states

import com.cdlexample.contracts.AgreementContract
import com.cdlexample.contracts.Status
import com.cdlexample.contracts.StatusState
import net.corda.contractsdk.verifiers.StandardState
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import java.lang.RuntimeException
import java.util.*

// *********
// * State *
// *********
@BelongsToContract(AgreementContract::class)
data class AgreementState(override val status: AgreementStatus,
                          val buyer: Party,
                          val seller: Party,
                          val goods: String,
                          val price: Amount<Currency>,
                          val proposer: Party,
                          val consenter: Party,
                          val rejectionReason: String? = null,
                          val rejectedBy: Party?= null,
                          override val participants: List<AbstractParty> = listOf(buyer, seller),
                          override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, StatusState, StandardState {

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

enum class AgreementStatus: Status {
    PROPOSED,
    REJECTED,
    AGREED
}
