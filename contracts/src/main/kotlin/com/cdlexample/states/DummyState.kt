package com.cdlexample.states

import com.cdlexample.contracts.AgreementContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.Party

@BelongsToContract(AgreementContract::class)
class DummyState(override val participants: List<Party> = listOf()): ContractState {
}