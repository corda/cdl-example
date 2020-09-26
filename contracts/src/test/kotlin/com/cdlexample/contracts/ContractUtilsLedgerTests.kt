package com.cdlexample.contracts

import com.cdlexample.states.Status
import com.cdlexample.states.StatusState
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.transactions.LedgerTransaction
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class ContractUtilsLedgerTests {


    @BelongsToContract(TestContract::class)
    class TestStateA(override val status: Status?, override val participants: List<AbstractParty> = listOf()) : ContractState, StatusState {

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
            const val ID = "com.cdlexample.contracts.ContractUtilsLedgerTests\$TestContract"
        }

        override fun verify(tx: LedgerTransaction) {

            val command = tx.commands.requireSingleCommand<TestContract.Commands>().value
            val txPath = getPath(tx, TestStateA::class.java, command)


            // todo: can test created paths against PathConstraints to check if Path builder is working

        }

        interface Commands : CommandData {
            class Command1 : Commands
            class Command2 : Commands
        }
    }

    private val ledgerServices = MockServices()

    val alice = TestIdentity(CordaX500Name("Alice Ltd", "London", "GB"))
    val bob = TestIdentity(CordaX500Name("Bob Inc", "New York", "US"))
    val charlie = TestIdentity(CordaX500Name("Charlie SA", "Paris", "FR"))


    @Test
    fun `contract test`() {

        val testStateA1 = TestStateA(TestStateA.TestStatus.STATUSA1)
        val testStateB1 = TestStateB()
        val testStateC1 = TestStateC()
        val testStateD1 = TestStateD()

        ledgerServices.ledger {
            transaction {

                input(TestContract.ID, testStateA1)
                input(TestContract.ID, testStateB1)
                input(TestContract.ID, testStateB1)
                input(TestContract.ID, testStateC1)
                input(TestContract.ID, testStateD1)
                command(alice.publicKey, TestContract.Commands.Command1())
                verifies()

            }


        }
    }
}