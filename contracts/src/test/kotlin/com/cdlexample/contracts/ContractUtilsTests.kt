package com.cdlexample.contracts

import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.LedgerTransaction
import org.junit.Test

class ContractUtilsTests() {

    @BelongsToContract(TestContract::class)
    class TestStateA(override val status: Status?, override val participants: List<AbstractParty> = listOf()) : StatusState {

        enum class TestStatus : Status {
            STATUSA1,
            STATUSA2
        }
    }

    @BelongsToContract(TestContract::class)
    class TestStateB(override val participants: List<AbstractParty> = listOf()) : ContractState

    @BelongsToContract(TestContract::class)
    class TestStateC(override val participants: List<AbstractParty> = listOf()) : ContractState

    @BelongsToContract(TestContract::class)
    class TestStateD(override val participants: List<AbstractParty> = listOf()) : ContractState


    class TestContract : Contract {
        companion object {
            // Used to identify our contract when building a transaction.
            const val ID = "com.cdlexample.contracts.TestContract"
        }

        override fun verify(tx: LedgerTransaction) {
        }

        interface Commands : CommandData {
            class Command1 : Commands
            class Command2 : Commands
        }
    }

    @Test
    fun `check PathConstraint with default input and output multiplicities`() {

        val pathConstraint = PathConstraint<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, MultiplicityConstraint(), MultiplicityConstraint())

        val path1 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 1, 1)
        assert(pathConstraint allows path1)

        // Different command
        val path2 = Path<TestStateA>(TestContract.Commands.Command2(), TestStateA.TestStatus.STATUSA1, 1, 1)
        assert(pathConstraint doesNotAllow path2)

        // Different Status
        val path3 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA2, 1, 1)
        assert(pathConstraint doesNotAllow path3)

        // Too many inputs
        val path4 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 2, 1)
        assert(pathConstraint doesNotAllow path4)

        // To few inputs
        val path5 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 0, 1)
        assert(pathConstraint doesNotAllow path5)

        // Too many outputs
        val path6 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 1, 2)
        assert(pathConstraint doesNotAllow path6)

        // Too few output
        val path7 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 1, 0)
        assert(pathConstraint doesNotAllow path7)

    }

    @Test
    fun `check Multiplicities`(){

        val m1 = MultiplicityConstraint()
        assert(m1 doesNotAllow 0)
        assert(m1 allows 1)
        assert(m1 doesNotAllow  2)

        val m2 = MultiplicityConstraint(0)
        assert(m2 allows 0)
        assert(m2 doesNotAllow  1)
        assert(m2 doesNotAllow  2)

        val m3 = MultiplicityConstraint(1, true, 3)
        assert(m3 doesNotAllow 0)
        assert(m3 allows 1)
        assert(m3 allows 2)
        assert(m3 allows 3)
        assert(m3 doesNotAllow 4)

        val m4 = MultiplicityConstraint(1, false, 3)
        assert(m4 doesNotAllow 0)
        assert(m4 allows 1)
        assert(m4 allows 2)
        assert(m4 allows 3)
        assert(m4 allows 4)
    }

    @Test
    fun `check PathConstraints with input and output Multiplicities`(){

        // test bounded input MultiplicityConstraint
        val pathConstraint1 = PathConstraint<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, MultiplicityConstraint(1,true, 2), MultiplicityConstraint())

        val path1 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 0, 1)
        assert(pathConstraint1 doesNotAllow  path1)

        val path2 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 1, 1)
        assert(pathConstraint1 allows path2)

        val path3 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 2, 1)
        assert(pathConstraint1 allows path3)

        val path4 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 3, 1)
        assert(pathConstraint1 doesNotAllow  path4)


        // test unbounded output MultiplicityConstraint
        val pathConstraint2 = PathConstraint<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, MultiplicityConstraint(), MultiplicityConstraint(1,false,0))

        val path5 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 1, 0)
        assert(pathConstraint2 doesNotAllow  path5)

        val path6 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 1, 1)
        assert(pathConstraint2 allows path6)

        val path7 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 1, 2)
        assert(pathConstraint2 allows path7)

        val path8 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 1, 3)
        assert(pathConstraint2 allows  path8)
    }

    @Test
    fun `check AdditionalStateConstraint`(){

        val asc = AdditionalStatesConstraint(AdditionalStatesType.OUTPUT,TestStateB::class.java, MultiplicityConstraint(1, false))

        // Check type match
        val as1 = AdditionalStates(AdditionalStatesType.OUTPUT, TestStateB::class.java, 1)
        assert( asc isSatisfiedBy as1)

        val as2 = AdditionalStates(AdditionalStatesType.INPUT, TestStateB::class.java, 1)
        assert( asc isNotSatisfiedBy as2)

        val as3 = AdditionalStates(AdditionalStatesType.REFERENCE, TestStateB::class.java, 1)
        assert( asc isNotSatisfiedBy as3)

        // Check state match
        val as10 = AdditionalStates(AdditionalStatesType.OUTPUT, TestStateA::class.java, 1)
        assert (asc isNotSatisfiedBy as10)

        val as11 = AdditionalStates(AdditionalStatesType.OUTPUT, TestStateB::class.java, 1)
        assert (asc isSatisfiedBy as11)

        val as12 = AdditionalStates(AdditionalStatesType.OUTPUT, TestStateC::class.java, 1)
        assert (asc isNotSatisfiedBy as12)

        // Check MultiplicityConstraint match
        val as20 = AdditionalStates(AdditionalStatesType.OUTPUT, TestStateB::class.java, 0)
        assert (asc isNotSatisfiedBy as20)

        val as21 = AdditionalStates(AdditionalStatesType.OUTPUT, TestStateB::class.java, 1)
        assert (asc isSatisfiedBy as21)

        val as22 = AdditionalStates(AdditionalStatesType.OUTPUT, TestStateB::class.java, 2)
        assert (asc isSatisfiedBy as22)

    }

    @Test
    fun `check PathConstraints with additional input and output constraints`(){


        val as1 = AdditionalStates(AdditionalStatesType.OUTPUT, TestStateB::class.java, 1)
        val as2 = AdditionalStates(AdditionalStatesType.OUTPUT, TestStateC::class.java, 1)
        val as3 = AdditionalStates(AdditionalStatesType.OUTPUT, TestStateD::class.java, 1)

        val asc1 = AdditionalStatesConstraint(AdditionalStatesType.OUTPUT, TestStateB::class.java, MultiplicityConstraint(1, true, 2))
        val asc2 = AdditionalStatesConstraint(AdditionalStatesType.OUTPUT, TestStateC::class.java, MultiplicityConstraint(1, true, 2))

        // Check PathConstraint with single AdditionalStatesConstraint
        val pathConstraint1 = PathConstraint<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, additionalStatesConstraints = setOf(asc1))

        val path1 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 1, 1, setOf(as1))
        assert(pathConstraint1 allows path1)

        val path2 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 1, 1, setOf(as2))
        assert(pathConstraint1 doesNotAllow  path2)

        val path3 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 1, 1, setOf(as1, as2))
        assert(pathConstraint1 allows  path3)


        // Check PathConstraint with multiple AdditionalStatesConstraint
        val pathConstraint2 = PathConstraint<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, additionalStatesConstraints = setOf(asc1, asc2))

        val path21 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 1, 1, setOf(as1))
        assert(pathConstraint2 doesNotAllow  path21)

        val path22 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 1, 1, setOf(as2))
        assert(pathConstraint2 doesNotAllow  path22)

        val path23 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 1, 1, setOf(as1, as2))
        assert(pathConstraint2 allows  path23)

        val path24 = Path<TestStateA>(TestContract.Commands.Command1(), TestStateA.TestStatus.STATUSA1, 1, 1, setOf(as1, as2, as3))
        assert(pathConstraint2 allows  path24)
    }
}