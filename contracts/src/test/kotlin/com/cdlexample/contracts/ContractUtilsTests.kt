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

    @BelongsToContract(TestContract::class)
    class TestStateD(override val participants: List<AbstractParty> = listOf()) : ContractState


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

    @Test
    fun `check AdditionalStateConstraint`(){

        val asc = AdditionalStatesConstraint(TestStateB::class.java, Multiplicity(1, false))

        // Check state match
        val as1 = AdditionalStateType(TestStateA::class.java, 1)
        assert (asc isNotSatisfiedBy as1)

        val as2 = AdditionalStateType(TestStateB::class.java, 1)
        assert (asc isSatisfiedBy as2)

        val as3 = AdditionalStateType(TestStateC::class.java, 1)
        assert (asc isNotSatisfiedBy as3)

        // Check Multiplicity match
        val as4 = AdditionalStateType(TestStateB::class.java, 0)
        assert (asc isNotSatisfiedBy as4)

        val as5 = AdditionalStateType(TestStateB::class.java, 1)
        assert (asc isSatisfiedBy as5)

        val as6 = AdditionalStateType(TestStateB::class.java, 2)
        assert (asc isSatisfiedBy as6)

    }

    @Test
    fun `check AdditionalStateConstraints`(){


        val asc1 = AdditionalStatesConstraint(TestStateB::class.java, Multiplicity(1, true, 2))
        val asc2 = AdditionalStatesConstraint(TestStateC::class.java, Multiplicity(1, false))

        val ascs = AdditionalStatesConstraints(setOf(asc1, asc2))


        // check state type
        val as1 = AdditionalStateType(TestStateB::class.java, 1)
        val as2 = AdditionalStateType(TestStateC::class.java, 1)
        val as3 = AdditionalStateType(TestStateD::class.java, 1)

        val set1 = setOf(as1,as2)
        assert( ascs isSatisfiedBy set1)

        val set2 = setOf(as1,as2, as3)
        assert( ascs isSatisfiedBy set2)

        val set3 = setOf(as2, as3)
        assert( ascs isNotSatisfiedBy set3)

        // Check multiplicity
        val as4 = AdditionalStateType(TestStateB::class.java, 1)
        val as5 = AdditionalStateType(TestStateB::class.java, 3)
        val as6 = AdditionalStateType(TestStateC::class.java, 1)

        val set4 = setOf(as4,as6)
        assert( ascs isSatisfiedBy set4)

        val set5 = setOf(as4,as5)
        assert( ascs isNotSatisfiedBy set5)
    }

    @Test
    fun `check PathConstraints with additional input and output constraints`(){


        val pathConstraint1 = PathConstraint(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, additionalInputsConstraints = AdditionalStatesConstraints(setOf(AdditionalStatesConstraint(TestStateB::class.java, Multiplicity(1, true, 2)))))

        val as1 = AdditionalStateType(TestStateB::class.java, 1)
        val path1 = Path(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, 1, 1, setOf(as1))
        assert(pathConstraint1 allows  path1)

        val as2 = AdditionalStateType(TestStateC::class.java, 1)
        val path2 = Path(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, 1, 1, setOf(as2))
        assert(pathConstraint1 doesNotAllow  path2)

        val path3 = Path(TestContract.Commands.Command1()::class.java, TestStateA.TestStatus.STATUSA1, 1, 1, setOf(as1,as2))
        assert(pathConstraint1 allows  path3)

    }


}