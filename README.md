<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# CorDapp Design Language (CDL) - Example CorDapp

This CorDapp provides an example implementation of a CDL design for two Parties to come to an agreement over the purchase of some goods. 

The intention is to take a mid level complexity CDL design and show how it can be methodically translated into the States and Contract required to implement the design.


# CDL

## CDL Smart Contract view

The Smart Contract design can be articulated using the Smart Contract CDL view, this replaces and enhances the old State Machine view. 

The Agreement Smart Contract can be represented as follows:

<img src="resources/cdl-agreement-smart-contract.png" alt="cdl-agreement-smart-contract.png">

Note, the strikethrough command constraint on the Agree Command concerns Billing which has not been implemented yet. 

## CDL Ledger Evolution view

The Ledger Evolution view replaces the old State Evolution view. The Ledger Evolution view represents part of the ledger as a Directed Acyclic Graph (DAG). This is helpful for tracing privacy implications and should give a more intuitive view of how the ledger can evolve.

An example of how the Agreement Smart contract might evolve on the ledger can be represented as follows:


<img src="resources/cdl-agreement-ledger-evolution.png" alt="cdl-agreement-ledger-evolution.png">

Note, the greyed out states concerns Billing which has not been implemented yet. 

This particular evolution (Propose, Reject, Repropose, Agree, Complete) is used as the base happy path case in the unit tests and most of the test cases follow this order, see `/contracts/AgreementContractTests.kt`

# New concepts

As part of the implementation we introduce some new concepts: 
 
 
### StatusStates and Statue interfaces

A core element of the CDL Smart Contract view is that Corda States can be in different statuses. When in different statuses there are different restrictions on the form of the state and the transitions that state can make.

To Identify ContractStates as requiring a status property we introduce new interfaces `StatusState`, together with `Status`, which is an interface for the status property.

```kotlin
/**
 * The StatusState interface should be implemented for all Contract states that require a status field.
 */
interface StatusState: ContractState {
    val status: Status?
}

/**
 * Statuses are defined as enum classes in the StatusState which should implement this Status interface.
 */
@CordaSerializable
interface Status


```
Statuses are defined in the StatusState as an enum class implementing the Status interface. 
 
```kotlin
data class AgreementState(override val status: AgreementStatus,
                           //.../// 
                          override val participants: List<AbstractParty> = listOf(buyer, seller),
                          override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, StatusState

enum class AgreementStatus: Status{
    PROPOSED,
    REJECTED,
    AGREED
}

```
 
## Primary State (might want a better name)

For convenience in these explanations, we define the 'Primary' States as States of type `StatusState` which are the main concern of the CDL diagram. Ie, it is the statuses of the Primary State which are shown in the different boxes on the CDL Smart Contract Diagram. In this CorDapp it is the AgreementState

We also mandate that:

- if there are multiple Primary State inputs they must have the same status. 
- if there are multiple Primary State outputs, they must have the same status, which can be different from the Primary State Input's status.)

There can be other states in the transaction, we will refer to those as 'Additional' states. 

The state that is considered Primary depends on the perspective of the CDL diagrams and the implementation. If two Smart Contracts are interacting, for example, this Agreement Smart Contract state and a Billing Smart Contract, then each would have its own CDL diagram and Smart Contract implementation. For the Agreement Smart Contract the Agreement States would be considered Primary with the Billing States being 'Addtional', where as for the Billing Smart Contract the Billing States would be considered Primary with the Agreements states being 'Additional'

 
## Paths

A Path is defined as the transition that a state makes from a given input Status with in a specific transaction. They are defined using the following code

```kotlin
class Path<T: StatusState>(val command: CommandData,
                val outputStatus: Status?,
                val numberOfInputStates: Int,
                val numberOfOutputStates: Int,
                val additionalStates: Set<AdditionalStates> = setOf())

class AdditionalStates(val type: AdditionalStatesType, val clazz: Class<out ContractState>, val numberOfStates: Int)

enum class AdditionalStatesType {INPUT, OUTPUT, REFERENCE}
```

- `command` represents the command.value in the transaction relating to the Primary State's Contract (there could be other commands in the transaction to but they are not dealt with by Paths) 
- `outputStatus` represents the status of the output Primary State 
- `numberOfInputStates` represents the number of States of the Primary State type in the transaction. 
- `numberOfOutputStates` represents the number of States of the Primary State type in the transaction. 
- `additionalStates` represents States types other than the Primary States which are in the transaction, including whether they are Inputs, outputs or reference states and how many of each are in the transaction. 

In this CorDapp:

- `numberOfInputStates` and `numberOfOutputStates` are going to be set to 0 or 1, because for any given agreement we only want to have one AgreementState unconsummed at one point in time representing the latest state of this agreement. However, in other use cases there could be different Multiplicities involved)
- `additionalStates` will be used to represent the BillingChips required in the Agree Paths (Although this is not implemented yet)


## PathConstraints

PathConstraints are used to restrict the allowed Paths that can be in a transaction. 

A PathConstraint allows a subset of paths defined by: 

```kotlin
class PathConstraint<T: StatusState>(val command: CommandData,
                     val outputStatus: Status?,
                     val inputMultiplicityConstraint: MultiplicityConstraint = MultiplicityConstraint(),
                     val outputMultiplicityConstraint: MultiplicityConstraint = MultiplicityConstraint(),
                     val additionalStatesConstraints: Set<AdditionalStatesConstraint> =  setOf()){  

    infix fun allows(p: Path<T>): Boolean {
        ...
    }
    
    infix fun doesNotAllow(p: Path<T>): Boolean = !this.allows(p)

    private fun additionalStatesCheck(constraints: Set<AdditionalStatesConstraint>, additionalStates: Set<AdditionalStates>) :Boolean{
        ...    
    }
}


```
Where:
- `command` is the class of the command allowed
- `outputStatus` is the outputStatus of the Primary State that is allowed
- `inputMultiplicityConstraint` defines the range of number of inputs of Primary type that are allowed 
- `outputMultiplicityConstraint` defines the range of number of outputs of Primary type that are allowed 
- `additionalStatesConstraint` defines which additional states must be present in the transaction

The Contract defines a set of PathConstraints for each Primary State status, for example when in status X you can follow PathConstraint A or B, but when you are in state Y you can only follow PathConstraint C .

In order to pass the verify the Path in the transaction needs to comply to at least one of the allowed PathConstraints for the Status of the Primary Input State status. 

This can be represented diagrammatically as follows: 

<img src="resources/paths diagram.png" alt="paths diagram.png">



There is an open question as to whether there should be an 'exclusive' flag on the Path Constraint, meaning that the only the States defined in the AdditionalStateConstraints may be present. Currently Path constraints allow any number of other State types not mentioned in the constraint, it will pass verification as long as the specified AdditionalStates are present.

## ContractUtils

*** designed to be reuseable ***

 
 
 
 the idea of Paths. Paths represent the options that a State in a particular status 


# Approach to writing the Contract

## 'Sub' verify functions for each Constraint

As smart Contracts become more complicated, the risk of missing some important control grows. To reduce this risk the smart contract is split up into sub verify functions which each deal with one of the types of constraints defined in CDL Smart contract view. (With the exception of the blue Flow constraints which are not implemented in the Contract and are more notes on what the flows should be doing.) 

```kotlin
    override fun verify(tx: LedgerTransaction) {

        verifyPathConstraints(tx, AgreementState::class.java)
        verifyUniversalConstraints(tx)
        verifyStatusConstraints(tx)
        verifyLinearIDConstraints(tx, AgreementState::class.java)
        verifySigningConstraints(tx)
        verifyCommandConstraints(tx)
    }
```

## Principle: Clarity and structure over duplicate code

By spliting the verifications into the sub verify functions there is some duplication, eg multiple when statements on Command(), but the principle is that it is better to have some duplication if it allows better clarity and structure, because better clarity and structure leads to lower risk, easier to understand and implement Smart Contracts.

## Standardised mechanisms to implement each type of constraint 

For each sub verify function we aim for a standard structure to test that type of constraint. The idea being that any Cordapp should be able to follow the same structure and just change the details of the conditions. For example, the code for verifying Status constraints looks like this: 

```kotlin
    fun verifyStatusConstraints(tx: LedgerTransaction){

        val allStates = tx.inputsOfType<AgreementState>() + tx.outputsOfType<AgreementState>()

        for (s in allStates) {
            when(s.status){
                PROPOSED -> {
                    requireThat {
                        "When status is Proposed rejectionReason must be null" using (s.rejectionReason == null)
                        "When status is Rejected rejectedBy must be null" using (s.rejectedBy == null)
                    }
                }
                REJECTED -> {
                    requireThat {
                        "When status is Rejected rejectionReason must not be null" using (s.rejectionReason != null)
                        "When status is Rejected rejectedBy must not be null" using (s.rejectedBy != null)
                        "When the Status is Rejected rejectedBy must be the buyer or seller" using (listOf(s.buyer, s.seller).contains(s.rejectedBy))
                    }
                }
                AGREED -> {}
            }
        }
    }
```
Which can be generalised to this for any CorDapp: 

```kotlin
    fun verifyStatusConstraints(tx: LedgerTransaction){

        val allStates = tx.inputsOfType<MyState>() + tx.outputsOfType<MyState>()

        for (s in allStates) {
            when(s.status){
                MY_STATUS_1 -> {
                    requireThat {
                        // Checks on states in status MY_STATUS_1
                    }
                }
                MY_STATUS_2 -> {
                    requireThat {
                        // Checks on states in status MY_STATUS_2
                    }
                }
            }
        }
    }
```

## Testing

Each sub verify has a set of unit tests which follow the base happy path, but introduce errors which ensure the contract validations fail in the correct way. 


# Paths and PathConstraints

It turns out that once you introduce status and start constraining the paths that a Smart contract allows between those statuses things get a little complicated. 







# Build and deploy the Cordapp

The CorDapp is based of the Cordapp Template - Kotlin, so should behave in the normal way, eg `./gradlew deployNode` to build and start up the nodes.

Note, most of the testing of the smart contract is done using Contract Unit tests, the rest of the CorDapp is mostly not implemented yet:
- Only two flows are implemented (Propose and Reject) togther with basic flow tests for each, just to test that serialisation of the Classes works.
- The webserver is not implemented
- The RPC client is not implemented


