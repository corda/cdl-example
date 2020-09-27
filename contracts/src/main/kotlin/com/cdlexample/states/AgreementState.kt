package com.cdlexample.states

import com.cdlexample.contracts.AgreementContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
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
                          override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, StatusState

enum class AgreementStatus: Status{
    PROPOSED,
    REJECTED,
    AGREED
}
