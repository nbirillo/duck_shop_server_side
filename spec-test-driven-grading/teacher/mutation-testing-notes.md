# Mutation testing — teacher notes

## How the machinery works

`mutants/catalog.json` (in this build for the graded set, in `spec-test-driven/mutants/` for the
practice set) lists one textual replacement per `:core` source file. `./gradlew generateMutants`
turns each entry into a module that compiles the **real** `:core` sources with that one file excluded
and replaced by a mutated copy. Consequences worth knowing:

- Mutants cannot drift from the algebra: the untouched code is `:core` itself, not a copy.
- Every `:core` type (including `Shop`) is on the classpath, so a learner's suite that tests `Shop`
  compiles. The earlier hand-written practice copies lacked `Shop`, which reported ERROR instead of
  CAUGHT and silently forced learners to avoid testing it.
- The generator fails loudly if a `find` pattern does not match exactly once. This is not
  bureaucracy: `duck.price <= maxPrice` also appears in the KDoc, so an unguarded pattern would have
  produced a mutant identical to `:core` — one nobody can kill, i.e. a false green. Patterns
  therefore match whole `override` lines.
- A generated **baseline** module runs the same suite against the *unmutated* `:core`. Tests that
  already fail there are excluded from the score. Without it, the flawed starter suite of 11.2 scored
  a perfect 4/4 (its planted failure "killed" every mutant); with it, an honest 1/4.

Two tiers, on purpose:

| Tier | Where | Content |
| --- | --- | --- |
| Practice | `spec-test-driven/mutants/` (visible, committed) | 4 mutants, boundary + vacuous truth. No prose: `what`/`hint` are omitted so the catalog is not a list of the tests to write |
| Graded | `mutants/` here (hidden) | 13 mutants: 11 must-kill incl. three subtle accessory-matching defects, plus 2 spec-dependent |

## Why some mutants must survive

The score counts only **must-kill** mutants. The two **spec-dependent** ones are the teaching
material:

- **`allof-no-short-circuit`** — `AllOf` evaluates every policy instead of stopping at the first
  rejection. Same answers for every input; the only way to detect it is a spy policy that counts its
  own calls. Nothing in the specification promises short-circuiting, so a suite that leaves it alive
  is fine — and a suite that kills it has *decided* short-circuiting is part of the contract. Both are
  acceptable; what is not acceptable is doing either by accident. (Claude killed it, with a Spy.)
- **`allof-defensive-copy`** — `AllOf` copies the list it is handed, so mutating the caller's list
  afterwards no longer changes behaviour. Only a test that mutates the list after construction can
  tell. This is exactly under-specification 2 of the exercise: killing it means the learner decided
  aliasing is contract; leaving it alive means they decided it is incidental. (Claude left it alive,
  deliberately, and recommended the defensive copy.)

Use this to make the general point: a mutation score is never "100% of everything". Some mutants are
equivalent to the original, and chasing them produces tests that over-specify — pinning
implementation details a future refactor is allowed to change.

## Measured scores (2026-07-31)

Graded set, 11 must-kill mutants. Blind regime: each model was given the flawed suite and asked to
verify and harden it with a generic prompt (`runAgent -Pmode=verify-harden`), then scored against a
mutant set it never saw.

| Agent | Suite valid on `:core` | Mutation score |
| --- | --- | --- |
| qwen2.5-coder:1.5b | yes, 10 tests | 4/11 (36%) |
| llama3.2:3b | **does not compile** (duplicate test names) | — |
| qwen2.5-coder:7b | yes, 13 tests | 5/11 (45%) |
| qwen2.5-coder:14b | yes, 15 tests | 5/11 (45%) |
| qwen2.5-coder:32b | yes, 15 tests | 7/11 (63%) |
| **this module's authored policy suite** | yes, 14 tests | **8/11 (72%)** |
| Claude Code (suite written from scratch, 86 tests) | yes | **11/11 (100%)** |

Reading of these numbers:

- Every local model leaves the `<=` boundary and all three accessory-matching mutants alive
  (name-prefix match, only-the-first-accessory, case-insensitive match).
- The authored suite of this module misses exactly those three. Worth showing to learners: mutation
  testing exposes gaps in a hand-written, test-first suite too.
- On this algebra, mutation testing **discriminates the weak and mid tier but does not challenge a
  frontier agent**. For strong agents the escalation still has to come from depth (11.7, the
  API/OpenAPI scenario) or from harder mutants. Hence the rule in the exercise: mandatory after a
  strong agent, optional after a weak one — the weak-agent learner is already fully occupied by
  verifying and hardening.
- Still open: Claude in the *harden* regime rather than write-from-scratch. See
  `running-advanced-agents.md`, prompts C (blind) and D (with feedback).
