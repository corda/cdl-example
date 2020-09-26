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


// todo: if leave these outside the test class need to change ContractutilsTest to use these + move them to a separate file

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
        const val ID = "com.cdlexample.contracts.TestContract"
    }

    override fun verify(tx: LedgerTransaction) {

        val command = tx.commands.requireSingleCommand<TestContract.Commands>().value
        val txPath = getPath(tx, TestStateA::class.java, command)

    }

    interface Commands : CommandData {
        class Command1 : Commands
        class Command2 : Commands
    }
}



class ContractUtilsLedgerTests {

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