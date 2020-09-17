package com.cdlexample.contracts

import com.cdlexample.states.Status
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction
import org.junit.Test

class ContractUtilsTests(){

    @BelongsToContract(TestContract::class)
    class TestStateA(override val participants: List<AbstractParty> = listOf()) : ContractState{

        class TestStatus{
            class StatusA1: Status
            class StatusA2: Status
        }
    }

    @BelongsToContract(TestContract::class)
    class TestStateB(override val participants: List<AbstractParty> = listOf()) : ContractState

    @BelongsToContract(TestContract::class)
    class TestStateC(override val participants: List<AbstractParty> = listOf()) : ContractState



    class TestContract: Contract {
        override fun verify(tx: LedgerTransaction) {
        }

        interface Commands: CommandData {
            class Command1: Commands
            class Command2: Commands
        }
    }




    @Test
    fun `check Path equality method`() {

        // test Command equality

        val path1 = Path(TestContract.Commands.Command1(), TestStateA.TestStatus.StatusA1())
        val path2 = Path(TestContract.Commands.Command1(), TestStateA.TestStatus.StatusA1())
        val path3 = Path(TestContract.Commands.Command2(), TestStateA.TestStatus.StatusA1())

        assert(path1 == path2)
        assert(path1 !== path3)
        assert(path2 == path1)
        assert(path3 != path1)

        // test Status Equality

        val path4 = Path(TestContract.Commands.Command1(), TestStateA.TestStatus.StatusA1())
        val path5 = Path(TestContract.Commands.Command1(), TestStateA.TestStatus.StatusA1())
        val path6 = Path(TestContract.Commands.Command1(), TestStateA.TestStatus.StatusA2())
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

    @Test
    fun `check additional inputs and output in Path equality method`() {


        // Test additionalInput equality
        val path1 = Path(TestContract.Commands.Command1(), TestStateA.TestStatus.StatusA1(), setOf(TestStateB::class.java))
        val path2 = Path(TestContract.Commands.Command1(), TestStateA.TestStatus.StatusA1(), setOf(TestStateB::class.java))
        val path3 = Path(TestContract.Commands.Command1(), TestStateA.TestStatus.StatusA1(), setOf(TestStateC::class.java))

        assert(path1 == path2)
        assert(path1 !== path3)
        assert(path2 == path1)
        assert(path3 != path1)

        // Test additionalOutput equality
        val path4 = Path(TestContract.Commands.Command1(), TestStateA.TestStatus.StatusA1(), additionalOutputs = setOf(TestStateB::class.java))
        val path5 = Path(TestContract.Commands.Command1(), TestStateA.TestStatus.StatusA1(), additionalOutputs = setOf(TestStateB::class.java))
        val path6 = Path(TestContract.Commands.Command1(), TestStateA.TestStatus.StatusA1(), additionalOutputs = setOf(TestStateC::class.java))

        assert(path4 == path5)
        assert(path4 !== path6)
        assert(path5 == path4)
        assert(path6 != path4)

        // Test Both

        val path7 = Path(TestContract.Commands.Command1(), TestStateA.TestStatus.StatusA1(), setOf(TestStateB::class.java),setOf(TestStateB::class.java))
        val path8 = Path(TestContract.Commands.Command1(), TestStateA.TestStatus.StatusA1(), setOf(TestStateB::class.java),setOf(TestStateB::class.java))
        val path9 = Path(TestContract.Commands.Command1(), TestStateA.TestStatus.StatusA1(), setOf(TestStateC::class.java),setOf(TestStateC::class.java))

        assert(path7 == path8)
        assert(path7 !== path9)
        assert(path8 == path7)
        assert(path9 != path7)

    }

}