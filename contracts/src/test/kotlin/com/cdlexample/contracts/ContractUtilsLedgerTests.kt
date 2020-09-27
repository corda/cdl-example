package com.cdlexample.contracts

import com.cdlexample.states.AgreementState
import com.cdlexample.states.Status
import com.cdlexample.states.StatusState
import net.bytebuddy.implementation.bytecode.Addition
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.LedgerTransaction
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class ContractUtilsLedgerTests {

    val alice = TestIdentity(CordaX500Name("Alice Ltd", "London", "GB"))
    val bob = TestIdentity(CordaX500Name("Bob Inc", "New York", "US"))
    val charlie = TestIdentity(CordaX500Name("Charlie SA", "Paris", "FR"))
    private val ledgerServices = MockServices(alice, networkParameters = testNetworkParameters(minimumPlatformVersion = 4))




    /*
    Test Set 1 - checking getPath generates the correct path, using the debugger to inspect the created object
     */

    @BelongsToContract(TestContract1::class)
    class TestState1A(override val status: Status?, override val participants: List<AbstractParty> = listOf()) : ContractState, StatusState {

        enum class TestStatus : Status {
            STATUSA1,
            STATUSA2
        }
    }

    @BelongsToContract(TestContract1::class)
    class TestState1B(override val participants: List<AbstractParty> = listOf()) : ContractState

    @BelongsToContract(TestContract1::class)
    class TestState1C(override val participants: List<AbstractParty> = listOf()) : ContractState

    @BelongsToContract(TestContract1::class)
    class TestState1D(override val participants: List<AbstractParty> = listOf()) : ContractState


    class TestContract1 : Contract {
        companion object {
            // Used to identify our contract when building a transaction.
            const val ID = "com.cdlexample.contracts.ContractUtilsLedgerTests\$TestContract1"
        }

        override fun verify(tx: LedgerTransaction) {

            val command = tx.commands.requireSingleCommand<TestContract1.Commands>().value
            val txPath = getPath(tx, TestState1A::class.java, command)
        }

        interface Commands : CommandData {
            class Command1 : Commands
            class Command2 : Commands
        }
    }

    @Test
    fun `check generation of Paths (via debugger)`() {

        val testState1A1 = TestState1A(TestState1A.TestStatus.STATUSA1)
        val testState1B1 = TestState1B()
        val testState1C1 = TestState1C()
        val testState1D1 = TestState1D()

        ledgerServices.ledger {
            transaction {

                input(TestContract1.ID, testState1A1)
                input(TestContract1.ID, testState1B1)
                input(TestContract1.ID, testState1B1)
                input(TestContract1.ID, testState1C1)
                input(TestContract1.ID, testState1D1)
                reference(TestContract1.ID, testState1B1)
                reference(TestContract1.ID, testState1C1)
                reference(TestContract1.ID, testState1C1)
                reference(TestContract1.ID, testState1D1)
                command(alice.publicKey, TestContract1.Commands.Command1())
                output(TestContract1.ID, testState1A1)
                output(TestContract1.ID, testState1A1)
                output(TestContract1.ID, testState1B1)
                output(TestContract1.ID, testState1C1)
                output(TestContract1.ID, testState1D1)
                output(TestContract1.ID, testState1D1)
                verifies()

            }
        }
    }

    /*
    Test Set 2 - Checking Path constraints
     */

    // todo: can test created paths against PathConstraints to check if Path builder is working
    // todo: what happens if primary state is specified in the additionalStateConstraints, would the primary state satisfy the requirement - is that a problem, or is it just duplication of the primary multiplicityConstraints

    // set up PathConstraints that require
    //    PC1:  Command1, StatusA1, 1 to unbounded Inputs of type B, 1 to 2 type C
    //    PC2:  Command1, StatusA1, one references of type B
    //    PC3:  Command2, StatusA2, 2 outputs of type C, one output of type D
    //    PC4:  Command2, StatusA2, 2 to unbounded inputs of B, one reference of C, 1 output of B

    @BelongsToContract(TestContract2::class)
    class TestState2A(override val status: Status?, override val participants: List<AbstractParty> = listOf()) : ContractState, StatusState {

        enum class TestStatus : Status {
            STATUSA1,
            STATUSA2
        }
    }

    @BelongsToContract(TestContract2::class)
    class TestState2B(override val participants: List<AbstractParty> = listOf()) : ContractState

    @BelongsToContract(TestContract2::class)
    class TestState2C(override val participants: List<AbstractParty> = listOf()) : ContractState

    @BelongsToContract(TestContract2::class)
    class TestState2D(override val participants: List<AbstractParty> = listOf()) : ContractState


    class TestContract2 : Contract {
        companion object {
            // Used to identify our contract when building a transaction.
            const val ID = "com.cdlexample.contracts.ContractUtilsLedgerTests\$TestContract2"
        }

        interface Commands : CommandData {
            class Command1 : Commands
            class Command2 : Commands
        }

        override fun verify(tx: LedgerTransaction) {

            verifyPathConstraints(tx, TestState2A::class.java)
        }

// todo: why do the statuses need qualifying
        fun <T: StatusState> verifyPathConstraints(tx: LedgerTransaction, clazz: Class<T>) {

            val commandValue = tx.commands.requireSingleCommand<Commands>().value
            val txPath = getPath(tx, clazz, commandValue)

            val pathConstraintsMap = mapOf<Status?, List<PathConstraint<T>>>(
                    TestState2A.TestStatus.STATUSA1 to listOf(
                            PathConstraint(Commands.Command1(), TestState2A.TestStatus.STATUSA1, additionalStatesConstraints = setOf(
                                    AdditionalStatesConstraint(AdditionalStatesType.INPUT, TestState2B::class.java, MultiplicityConstraint(1, false)),
                                    AdditionalStatesConstraint(AdditionalStatesType.INPUT, TestState2C::class.java, MultiplicityConstraint(1, true, 2))
                            )),
                            PathConstraint(Commands.Command1(), TestState2A.TestStatus.STATUSA1, additionalStatesConstraints = setOf(
                                    AdditionalStatesConstraint(AdditionalStatesType.REFERENCE, TestState2B::class.java, MultiplicityConstraint())
                            ))
                    ),
                    TestState2A.TestStatus.STATUSA2 to listOf(
                            PathConstraint(Commands.Command2(), TestState2A.TestStatus.STATUSA2, additionalStatesConstraints = setOf(
                                    AdditionalStatesConstraint(AdditionalStatesType.OUTPUT, TestState2C::class.java, MultiplicityConstraint(2,true)),
                                    AdditionalStatesConstraint(AdditionalStatesType.OUTPUT, TestState2D::class.java, MultiplicityConstraint())
                            )),
                            PathConstraint(Commands.Command2(), TestState2A.TestStatus.STATUSA2, additionalStatesConstraints = setOf(
                                    AdditionalStatesConstraint(AdditionalStatesType.INPUT, TestState2B::class.java, MultiplicityConstraint(2, false)),
                                    AdditionalStatesConstraint(AdditionalStatesType.REFERENCE, TestState2C::class.java, MultiplicityConstraint()),
                                    AdditionalStatesConstraint(AdditionalStatesType.OUTPUT, TestState2D::class.java, MultiplicityConstraint())
                            ))
                    )
            )

            val inputStatus = requireSingleInputStatus(tx, clazz)
            val allowedPaths = pathConstraintsMap[inputStatus]

            requireThat {
                "Input status must have a list of PathConstraints defined." using (allowedPaths != null)
                "txPath must be allowed by PathConstraints for inputStatus $inputStatus" using verifyPath(txPath, allowedPaths!!)
            }

}





    }

    @Test
    fun `check various PathConstraints with AdditionalStates`() {

        val testState2A1 = TestState2A(TestState2A.TestStatus.STATUSA1)
        val testState2B1 = TestState2B()
        val testState2C1 = TestState2C()
        val testState2D1 = TestState2D()

        ledgerServices.ledger {


            // happy Path for PC1
            transaction {

                command(alice.publicKey, TestContract2.Commands.Command1())

                input(TestContract2.ID, testState2A1)
                input(TestContract2.ID, testState2B1)
                input(TestContract2.ID, testState2B1)
                input(TestContract2.ID, testState2B1)
                input(TestContract2.ID, testState2C1)
                input(TestContract2.ID, testState2C1)

                output(TestContract2.ID, testState2A1)

                verifies()
            }

            // happy Path for PC1 fails because too many type C inputs
            transaction {

                command(alice.publicKey, TestContract2.Commands.Command1())

                input(TestContract2.ID, testState2A1)
                input(TestContract2.ID, testState2B1)
                input(TestContract2.ID, testState2B1)
                input(TestContract2.ID, testState2B1)
                input(TestContract2.ID, testState2C1)
                input(TestContract2.ID, testState2C1)
                input(TestContract2.ID, testState2C1)

                output(TestContract2.ID, testState2A1)

                failsWith("txPath must be allowed by PathConstraints for inputStatus STATUSA1")
            }




        }
    }


}