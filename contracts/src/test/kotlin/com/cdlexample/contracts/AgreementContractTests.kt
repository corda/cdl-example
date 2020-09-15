package com.cdlexample.contracts

import com.cdlexample.states.AgreementState
import com.cdlexample.states.AgreementStatus
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.dsl.LedgerDSL
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test
import java.util.*

class AgreementContractTests {
    private val ledgerServices = MockServices()

    val alice = TestIdentity(CordaX500Name("Alice Ltd", "London", "GB"))
    val bob = TestIdentity(CordaX500Name("Bob Inc", "New York", "US"))
    val charlie = TestIdentity(CordaX500Name("Charlie SA", "Paris", "FR"))

    @Test
    fun `check correctly formed Tx verifies`() {  // todo: modify as add more constraints

        val input = AgreementState(AgreementStatus.Proposed(),
                alice.party, bob.party, "Some grapes", Amount(10, Currency.getInstance("GBP")), alice.party, bob.party)
        val output = AgreementState(AgreementStatus.Agreed(),
                alice.party, bob.party, "Some more grapes", Amount(10, Currency.getInstance("GBP")), alice.party, bob.party)

        ledgerServices.ledger {
            transaction {
                input(AgreementContract.ID, input)
                command(alice.publicKey, AgreementContract.Commands.Agree())
                output(AgreementContract.ID, output)
                verifies()
            }
        }
    }

    @Test
    fun `check all inputs of type AgreementState have the same Status`() {

        val input1 = AgreementState(AgreementStatus.Proposed(),
                alice.party, bob.party, "Some grapes", Amount(10, Currency.getInstance("GBP")), alice.party, bob.party)
        val input2 = AgreementState(AgreementStatus.Proposed(), alice.party, bob.party, "Some more grapes", Amount(10, Currency.getInstance("GBP")), alice.party, bob.party)
        val input3 = AgreementState(AgreementStatus.Agreed(), alice.party, bob.party, "yet more grapes", Amount(10, Currency.getInstance("GBP")), alice.party, bob.party)

        ledgerServices.ledger {
            transaction {
                input(AgreementContract.ID, input1)
                input(AgreementContract.ID, input2)
                input(AgreementContract.ID, input3)
                command(alice.publicKey, AgreementContract.Commands.Propose())
                failsWith("All inputs of type AgreementState have the same Status.")
            }
        }
    }


    @Test
    fun `check all outputs of type AgreementState have the same Status`() {

        val output1 = AgreementState(AgreementStatus.Proposed(), alice.party, bob.party, "Some grapes", Amount(10, Currency.getInstance("GBP")), alice.party, bob.party)
        val output2 = AgreementState(AgreementStatus.Proposed(), alice.party, bob.party, "Some more grapes", Amount(10, Currency.getInstance("GBP")), alice.party, bob.party)
        val output3 = AgreementState(AgreementStatus.Agreed(), alice.party, bob.party, "yet more grapes", Amount(10, Currency.getInstance("GBP")), alice.party, bob.party)

        ledgerServices.ledger {
            transaction {
                output(AgreementContract.ID, output1)
                output(AgreementContract.ID, output2)
                output(AgreementContract.ID, output3)
                command(alice.publicKey, AgreementContract.Commands.Propose())
                failsWith("All outputs of type AgreementState have the same Status.")
            }
        }
    }


    @Test
    fun `check non happy transition paths`() {

        val proposedState = AgreementState(AgreementStatus.Proposed(),
                alice.party, bob.party, "Some grapes", Amount(10, Currency.getInstance("GBP")), alice.party, bob.party)
        val rejectedState = AgreementState(AgreementStatus.Rejected(),
                alice.party, bob.party, "Some more grapes", Amount(10, Currency.getInstance("GBP")), alice.party, bob.party)
        val agreedState = AgreementState(AgreementStatus.Agreed(),
                alice.party, bob.party, "Some more grapes", Amount(10, Currency.getInstance("GBP")), alice.party, bob.party)

        ledgerServices.ledger {

            // From null status
            // Incorrect Statuses
            transaction {
                command(alice.publicKey, AgreementContract.Commands.Propose())
                output(AgreementContract.ID, agreedState)
                `fails with`("When there is no input AgreementState the path must be Propose -> Proposed")
            }
            transaction {
                command(alice.publicKey, AgreementContract.Commands.Propose())
                output(AgreementContract.ID, rejectedState)
                `fails with`("When there is no input AgreementState the path must be Propose -> Proposed")
            }
            // Incorrect Commands
            transaction {
                command(alice.publicKey, AgreementContract.Commands.Agree())
                output(AgreementContract.ID, proposedState)
                `fails with`("When there is no input AgreementState the path must be Propose -> Proposed")
            }
            transaction {
                command(alice.publicKey, AgreementContract.Commands.Reject())
                output(AgreementContract.ID, proposedState)
                `fails with`("When there is no input AgreementState the path must be Propose -> Proposed")
            }
            transaction {
                command(alice.publicKey, AgreementContract.Commands.Repropose())
                output(AgreementContract.ID, proposedState)
                `fails with`("When there is no input AgreementState the path must be Propose -> Proposed")
            }
            transaction {
                command(alice.publicKey, AgreementContract.Commands.Complete())
                output(AgreementContract.ID, proposedState)
                `fails with`("When there is no input AgreementState the path must be Propose -> Proposed")
            }

            // from Proposed state
            // Incorrect Statuses
            transaction {
                input(AgreementContract.ID, proposedState)
                command(alice.publicKey, AgreementContract.Commands.Agree())
                `fails with`("When the input Status is Proposed, the path must be Reject -> Rejected, or Agree -> Agreed.")
            }
            transaction {
                input(AgreementContract.ID, proposedState)
                command(alice.publicKey, AgreementContract.Commands.Agree())
                output(AgreementContract.ID, proposedState)
                `fails with`("When the input Status is Proposed, the path must be Reject -> Rejected, or Agree -> Agreed.")
            }
            transaction {
                input(AgreementContract.ID, proposedState)
                command(alice.publicKey, AgreementContract.Commands.Agree())
                output(AgreementContract.ID, rejectedState)
                `fails with`("When the input Status is Proposed, the path must be Reject -> Rejected, or Agree -> Agreed.")
            }
            // Incorrect Commands
            transaction {
                input(AgreementContract.ID, proposedState)
                command(alice.publicKey, AgreementContract.Commands.Propose())
                output(AgreementContract.ID, agreedState)
                `fails with`("When the input Status is Proposed, the path must be Reject -> Rejected, or Agree -> Agreed.")
            }
            transaction {
                input(AgreementContract.ID, proposedState)
                command(alice.publicKey, AgreementContract.Commands.Repropose())
                output(AgreementContract.ID, agreedState)
                `fails with`("When the input Status is Proposed, the path must be Reject -> Rejected, or Agree -> Agreed.")
            }
            transaction {
                input(AgreementContract.ID, proposedState)
                command(alice.publicKey, AgreementContract.Commands.Complete())
                output(AgreementContract.ID, agreedState)
                `fails with`("When the input Status is Proposed, the path must be Reject -> Rejected, or Agree -> Agreed.")
            }

            // From Rejected Status
            // Incorrect Statuses
            transaction {
                input(AgreementContract.ID, rejectedState)
                command(alice.publicKey, AgreementContract.Commands.Repropose())
                `fails with`("When the input Status is Rejected, the path must be Repropose -> Proposed.")
            }
            transaction {
                input(AgreementContract.ID, rejectedState)
                command(alice.publicKey, AgreementContract.Commands.Repropose())
                output(AgreementContract.ID, rejectedState)
                `fails with`("When the input Status is Rejected, the path must be Repropose -> Proposed.")
            }
            transaction {
                input(AgreementContract.ID, rejectedState)
                command(alice.publicKey, AgreementContract.Commands.Repropose())
                output(AgreementContract.ID, agreedState)
                `fails with`("When the input Status is Rejected, the path must be Repropose -> Proposed.")
            }
            // Incorrect Commands
            transaction {
                input(AgreementContract.ID, rejectedState)
                command(alice.publicKey, AgreementContract.Commands.Propose())
                output(AgreementContract.ID, proposedState)
                `fails with`("When the input Status is Rejected, the path must be Repropose -> Proposed.")
            }
            transaction {
                input(AgreementContract.ID, rejectedState)
                command(alice.publicKey, AgreementContract.Commands.Reject())
                output(AgreementContract.ID, proposedState)
                `fails with`("When the input Status is Rejected, the path must be Repropose -> Proposed.")
            }
            transaction {
                input(AgreementContract.ID, rejectedState)
                command(alice.publicKey, AgreementContract.Commands.Agree())
                output(AgreementContract.ID, proposedState)
                `fails with`("When the input Status is Rejected, the path must be Repropose -> Proposed.")
            }
            transaction {
                input(AgreementContract.ID, rejectedState)
                command(alice.publicKey, AgreementContract.Commands.Complete())
                output(AgreementContract.ID, proposedState)
                `fails with`("When the input Status is Rejected, the path must be Repropose -> Proposed.")
            }

            // From Agreed
            // Incorrect Status
            transaction {
                input(AgreementContract.ID, agreedState)
                command(alice.publicKey, AgreementContract.Commands.Complete())
                output(AgreementContract.ID, proposedState)
                `fails with`("When the input Status is Agree, the path must be Complete -> null.")
            }
            transaction {
                input(AgreementContract.ID, agreedState)
                command(alice.publicKey, AgreementContract.Commands.Complete())
                output(AgreementContract.ID, rejectedState)
                `fails with`("When the input Status is Agree, the path must be Complete -> null.")
            }
            transaction {
                input(AgreementContract.ID, agreedState)
                command(alice.publicKey, AgreementContract.Commands.Complete())
                output(AgreementContract.ID, agreedState)
                `fails with`("When the input Status is Agree, the path must be Complete -> null.")
            }
            // Incorrect Commands
            transaction {
                input(AgreementContract.ID, agreedState)
                command(alice.publicKey, AgreementContract.Commands.Propose())
                `fails with`("When the input Status is Agree, the path must be Complete -> null.")
            }
            transaction {
                input(AgreementContract.ID, agreedState)
                command(alice.publicKey, AgreementContract.Commands.Reject())
                `fails with`("When the input Status is Agree, the path must be Complete -> null.")
            }
            transaction {
                input(AgreementContract.ID, agreedState)
                command(alice.publicKey, AgreementContract.Commands.Repropose())
                `fails with`("When the input Status is Agree, the path must be Complete -> null.")
            }
            transaction {
                input(AgreementContract.ID, agreedState)
                command(alice.publicKey, AgreementContract.Commands.Agree())
                `fails with`("When the input Status is Agree, the path must be Complete -> null.")
            }





        }

    }


    @Test
    fun `check Path equality method`() {

        // test Command equality

        val path1 = AgreementContract.Path(AgreementContract.Commands.Agree(), AgreementStatus.Proposed())
        val path2 = AgreementContract.Path(AgreementContract.Commands.Agree(), AgreementStatus.Proposed())
        val path3 = AgreementContract.Path(AgreementContract.Commands.Propose(), AgreementStatus.Proposed())

        assert(path1 == path2)
        assert(path1 !== path3)
        assert(path2 == path1)
        assert(path3 != path1)

        // test Status Equality
        val path4 = AgreementContract.Path(AgreementContract.Commands.Agree(), AgreementStatus.Proposed())
        val path5 = AgreementContract.Path(AgreementContract.Commands.Agree(), AgreementStatus.Proposed())
        val path6  = AgreementContract.Path(AgreementContract.Commands.Agree(), AgreementStatus.Agreed())
        val path7 = AgreementContract.Path(AgreementContract.Commands.Agree(), null)
        val path8 = AgreementContract.Path(AgreementContract.Commands.Agree(), null)

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
    fun `check universal constraints`(){

        val state1 = AgreementState(AgreementStatus.Proposed(), alice.party, alice.party, "Some grapes", Amount(10, Currency.getInstance("GBP")), alice.party, bob.party)
        val state2 = AgreementState(AgreementStatus.Proposed(), alice.party, bob.party, "Some grapes", Amount(10, Currency.getInstance("GBP")), charlie.party, bob.party)
        val state3 = AgreementState(AgreementStatus.Proposed(), alice.party, bob.party, "Some grapes", Amount(10, Currency.getInstance("GBP")), alice.party, charlie.party)
        val state4 = AgreementState(AgreementStatus.Proposed(), alice.party, bob.party, "Some grapes", Amount(10, Currency.getInstance("GBP")), alice.party, alice.party)



        ledgerServices.ledger {

            // As output states
            transaction {
                command(alice.publicKey, AgreementContract.Commands.Propose())
                output(AgreementContract.ID, state1)
                `fails with`("The buyer and seller must be different Parties.")
            }
            transaction {
                command(alice.publicKey, AgreementContract.Commands.Propose())
                output(AgreementContract.ID, state2)
                `fails with`("The proposer must be either the buyer or the seller.")
            }
            transaction {
                command(alice.publicKey, AgreementContract.Commands.Propose())
                output(AgreementContract.ID, state3)
                `fails with`("The consenter must be either the buyer or the seller.")
            }
            transaction {
                command(alice.publicKey, AgreementContract.Commands.Propose())
                output(AgreementContract.ID, state4)
                `fails with`("The consenter and proposer must be different Parties.")
            }
        }






    }



}