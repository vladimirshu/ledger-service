
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
  - status[COMPLETED]
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
- balances are updated in deterministic account-ID order to reduce deadlock risk
- balance of the second account is updated
- an immutable completed transfer is recorded; rejected or rolled-back attempts are not ledger entries

## Concurrency and retries
- account updates use optimistic locking
- transient optimistic-lock and database-integrity conflicts are retried up to three times
- each retry runs in a new database transaction and reads the latest account and transfer state
- concurrent requests with the same idempotency key return the already committed transfer after retrying
