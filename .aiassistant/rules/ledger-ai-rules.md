---
apply: always
---

# AI Coding Rules

Read `README.md` in the project root before making any changes. 

## Project context

- This is a live coding interview challenge.
- The main focus is concurrency.
- Favor a solution that is simple, correct, and efficient.
- Keep the implementation minimal and easy to explain.

## Implementation principles

- Prefer straightforward code over abstractions.
- Use the smallest concurrency primitive that solves the problem correctly.
- Avoid shared mutable state unless it is required.
- Do not introduce extra frameworks, dependencies, or architectural layers unless they are clearly needed.
- Write code that is easy to verify with tests.

## Production vs interview tradeoff

If a production-grade system would normally need something more elaborate, but that would be overengineered for an interview setting, do not implement the full production version.

Instead:

- implement the interview-appropriate version,
- add a concise comment explaining what would be added in production,
- keep the comment specific and practical.

Examples of things that may stay as comments in an interview solution:

- advanced observability,
- retry/backoff frameworks,
- distributed coordination,
- persistence hardening,
- bulkhead/circuit-breaker infrastructure,
- extensive validation layers,
- full resiliency and operational tooling.

## Concurrency rules

- Make thread-safety guarantees explicit.
- Prefer immutable data where possible.
- Avoid blocking unless it is necessary and justified.
- If using locks, keep the locked section small.
- If using atomics or concurrent collections, use them deliberately and document why.
- Consider race conditions, visibility, and ordering issues in tests.

## Code quality

- Keep methods short and names clear.
- Follow existing project conventions.
- Prefer readable control flow over clever code.
- Add focused tests for edge cases and concurrency behavior.
- Do not over-comment obvious code, but do comment tricky concurrency decisions.

## Workflow

- Inspect the existing codebase before editing.
- Reuse the current Spring Boot / Gradle setup.
- Make the smallest change that solves the problem.
- Verify with the project’s test/build commands when possible.
