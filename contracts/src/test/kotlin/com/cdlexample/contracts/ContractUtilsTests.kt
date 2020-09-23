package com.cdlexample.contracts

import com.cdlexample.states.Status
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction
import org.junit.Test

class ContractUtilsTests() {

    @BelongsToContract(TestContract::class)
    class TestStateA(override val participants: List<AbstractParty> = listOf()) : ContractState {

        enum class TestStatus : Status {
            STATUSA1,
            STATUSA2
        }
    }

    @BelongsToContract(TestContract::class)
    class TestStateB(override val participants: List<AbstractParty> = listOf()) : ContractState

    @BelongsToContract(TestContract::class)
    class TestStateC(override val participants: List<AbstractParty> = listOf()) : ContractState


    class TestContract : Contract {
        override fun verify(tx: LedgerTransaction) {
        }

        interface Commands : CommandData {
            class Command1 : Commands
            class Command2 : Commands
        }
    }

    @Test
    fun `check PathConstraint with default input and output multiplicities`() {

        val pathConstraint = PathConstraint(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, Multiplicity(), Multiplicity())

        val path1 = Path(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, 1, 1)
        assert(pathConstraint allows path1)

        // Different command
        val path2 = Path(TestContract.Commands.Command2()::class.java, TestStateA.TestStatus.STATUSA1, 1, 1)
        assert(pathConstraint doesNotAllow path2)

        // Different Status
        val path3 = Path(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA2, 1, 1)
        assert(pathConstraint doesNotAllow path3)

        // Too many inputs
        val path4 = Path(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, 2, 1)
        assert(pathConstraint doesNotAllow path4)

        // To few inputs
        val path5 = Path(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, 0, 1)
        assert(pathConstraint doesNotAllow path5)

        // Too many outputs
        val path6 = Path(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, 1, 2)
        assert(pathConstraint doesNotAllow path6)

        // Too few output
        val path7 = Path(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, 1, 0)
        assert(pathConstraint doesNotAllow path7)

    }

    @Test
    fun `check Multiplicities`(){

        val m1 = Multiplicity()
        assert(m1 doesNotAllow 0)
        assert(m1 allows 1)
        assert(m1 doesNotAllow  2)

        val m2 = Multiplicity(0)
        assert(m2 allows 0)
        assert(m2 doesNotAllow  1)
        assert(m2 doesNotAllow  2)

        val m3 = Multiplicity(1, true, 3)
        assert(m3 doesNotAllow 0)
        assert(m3 allows 1)
        assert(m3 allows 2)
        assert(m3 allows 3)
        assert(m3 doesNotAllow 4)

        val m4 = Multiplicity(1, false, 3)
        assert(m4 doesNotAllow 0)
        assert(m4 allows 1)
        assert(m4 allows 2)
        assert(m4 allows 3)
        assert(m4 allows 4)
    }

    @Test
    fun `check PathConstraints with input and output Multiplicities`(){

        // test bounded input Multiplicity
        val pathConstraint1 = PathConstraint(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, Multiplicity(1,true, 2), Multiplicity())

        val path1 = Path(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, 0, 1)
        assert(pathConstraint1 doesNotAllow  path1)

        val path2 = Path(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, 1, 1)
        assert(pathConstraint1 allows path2)

        val path3 = Path(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, 2, 1)
        assert(pathConstraint1 allows path3)

        val path4 = Path(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, 3, 1)
        assert(pathConstraint1 doesNotAllow  path4)


        // test unbounded output Multiplicity
        val pathConstraint2 = PathConstraint(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, Multiplicity(), Multiplicity(1,false,0))

        val path5 = Path(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, 1, 0)
        assert(pathConstraint2 doesNotAllow  path5)

        val path6 = Path(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, 1, 1)
        assert(pathConstraint2 allows path6)

        val path7 = Path(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, 1, 2)
        assert(pathConstraint2 allows path7)

        val path8 = Path(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, 1, 3)
        assert(pathConstraint2 allows  path8)
    }
}