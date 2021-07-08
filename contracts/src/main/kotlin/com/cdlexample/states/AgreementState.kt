package com.cdlexample.states

import com.cdlexample.contracts.AgreementContract
import com.cdlexample.contracts.Status
import com.cdlexample.contracts.StatusState
import net.corda.v5.application.identity.AbstractParty
import net.corda.v5.application.identity.Party
import net.corda.v5.ledger.UniqueIdentifier
import net.corda.v5.ledger.contracts.Amount
import net.corda.v5.ledger.contracts.BelongsToContract
import net.corda.v5.ledger.contracts.LinearState
import java.util.*

// *********
// * State *
// *********
@BelongsToContract(AgreementContract::class)
data class AgreementState(override val status: AgreementStatus?,
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

enum class AgreementStatus: Status {
    PROPOSED,
    REJECTED,
    AGREED
}
