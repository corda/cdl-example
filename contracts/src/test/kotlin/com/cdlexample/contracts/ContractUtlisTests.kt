package com.cdlexample.contracts

import com.cdlexample.states.AgreementStatus
import com.cdlexample.contracts.Path
import com.cdlexample.states.Status
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction
import org.junit.Test

class ContractUtlisTests(){

    class TestState(override val participants: List<AbstractParty> = listOf()) : ContractState{

        class TestStatus{
            class Status1: Status
            class Status2: Status
            class Status3: Status
        }
    }

    class TestContract: Contract {
        override fun verify(tx: LedgerTransaction) {
        }

        interface Commands: CommandData {
            class Command1: Commands
            class Command2: Commands
            class Command3: Commands
        }
    }




    @Test
    fun `check Path equality method`() {

        // test Command equality

        val path1 = Path(TestContract.Commands.Command1(), TestState.TestStatus.Status1())
        val path2 = Path(TestContract.Commands.Command1(), TestState.TestStatus.Status1())
        val path3 = Path(TestContract.Commands.Command2(), TestState.TestStatus.Status1())

        assert(path1 == path2)
        assert(path1 !== path3)
        assert(path2 == path1)
        assert(path3 != path1)

        // test Status Equality

        val path4 = Path(TestContract.Commands.Command1(), TestState.TestStatus.Status1())
        val path5 = Path(TestContract.Commands.Command1(), TestState.TestStatus.Status1())
        val path6 = Path(TestContract.Commands.Command1(), TestState.TestStatus.Status2())
        val path7 = Path(TestContract.Commands.Command1(), null)
        val path8 = Path(TestContract.Commands.Command1(), null)

        assert(path4 == path5)
        assert(path5 == path4)
        assert(path4 != path6)
        assert(path6 != path4)
        assert(path4 != path7)
        assert(path7 !== path4)
        assert(path7 == path8)
        assert(path8 == path7)

        // check both Command and Status mismatched

        assert(path3 != path7)
        assert(path7 != path3)

    }
}