
# Project description
It's a ledger service for a bank.

# Business requirements
- service must allow to transfer money between different accounts

## Out of Scope
- currency conversion
- auth
- cross-bank transfers

# business invariants
- balance can not go negative

# Service boundaries
- service will be used directly by customers
- no other services in the system  
- one instance of the service

# Non-functional requirements
- p95 latency = 300 ms

# Service design
one instance of sql DB (H2) is sufficient 

## Domain entities
- Account
  - id(pk)
  - balance
  - version -- for optimistic locking
  - modifiedAt
- MoneyTransfer -- for auditing
  - fromAccount(fk)
  - toAccount(fk)
  - amount
  - idempotencyKey -- for avoiding repeated transfers
  - status[PENDING, FINISHED, FAILED]
  - createdAt

## API
sync
POST /api/v1/transfer
    ?fromAccount 
    ?toAccount
    ?amount
    ?idempotencyKey

# Feature workflows
## transfer money workflow
- API Request validated
- transaction is created
- account states are validated
- balance of the account with the lowest ID is updated (to avoid deadlocks)
- balance of the second account is updated
