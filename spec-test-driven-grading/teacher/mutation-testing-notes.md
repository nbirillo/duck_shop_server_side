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

These numbers were re-measured from scratch after the answer keys were moved out of the student
folder, and they came out identical (same scores, same surviving mutants) — as expected, since
`runAgent` assembles its prompt from `:core` and the flawed suite rather than from the folder's
documentation. The isolation work changes what an *interactive* agent can see, not what the API
harness sends.

| Agent | Suite valid on `:core` | Mutation score |
| --- | --- | --- |
| qwen2.5-coder:1.5b | yes, 10 tests | 4/11 (36%) |
| llama3.2:3b | **does not compile** (duplicate test names) | — |
| qwen2.5-coder:7b | yes, 13 tests | 5/11 (45%) |
| qwen2.5-coder:14b | yes, 15 tests | 5/11 (45%) |
| qwen2.5-coder:32b | yes, 15 tests | 7/11 (63%) |
| **this module's authored policy suite** | yes, 14 tests | **8/11 (72%)** |
| Claude Code, blind harden of the flawed suite (47 tests) | yes | **11/11 (100%)** |
| Claude Code, harden **with** practice-mutant feedback (47 tests) | yes | **10/11 (90%)** |
| Claude Code (suite written from scratch, 86 tests) | yes | **11/11 (100%)** |

### Claude Code, blind regime — the honest frontier measurement

Run in a fresh export of the tracked student content (no grading build, no other agent's suites, no
answer key), with the prompt forbidding `mutants/` and `verifyMutants`. It fixed the planted `Not`
test — and renamed it to say what it actually checks — then added 37 tests and reached 100% of the
must-kill set without ever seeing a mutant. Notably it covered, unprompted, the three defects every
local model misses: exact-name matching (`"hatband"` does not satisfy `"hat"`), case sensitivity, and
"the accessory is deliberately the third in the list" — it explicitly reasoned that the original suite
would pass an implementation consulting only `policies.first()`.

On the two spec-dependent mutants: it left **short-circuiting** unpinned *deliberately*, arguing every
policy here is pure so evaluation order is unobservable and a test freezing it would block legitimate
reordering — it recorded that in the KDoc instead. That is exactly the intended answer. It did **not**
address **list aliasing** at all this time (an earlier from-scratch run had spontaneously flagged it and
recommended a defensive copy), so that mutant survives unaddressed rather than by decision. Useful
teaching material: even a frontier agent's coverage of under-specification is not stable run to run.

It also volunteered four decisions the exercise never asks for — `MinAccessories(0)` and negative
minimums admit everything (no input validation, pinned as-is rather than pretending it throws),
duplicate accessory names count towards `MinAccessories`, single-element combinators as identity, and
both De Morgan laws.

⇒ The isolation work did not change the verdict: mutation testing on this algebra confirms a frontier
agent rather than challenging it. What it *did* change is that the verdict is now trustworthy — before,
the folder contained the acceptance suite whose test names spell out the answers.

### The feedback regime scored *worse* — the practice set anchors the agent

Same task, same starting suite, fresh export, but this time the prompt let the agent run
`verifyMutants` and iterate until nothing survived. It reached 4/4 on the practice set, `-PmutantsStrict`
passing, and stopped there — and on the graded set it scored **10/11, one worse than the blind run**.

The mutant it lost is `requires-accessory-prefix`. Blind, it had written `RequiresAccessory("hat")` against
a duck wearing a `"hatband"` (and the reverse) precisely because nothing in the spec ruled prefix matching
out. With the practice report in front of it, it still pinned name matching — but as case sensitivity and
whitespace (`"Hat"`, `" hat"`), which no mutant tests, and dropped the prefix case.

The two suites are both 47 tests and share only 17 test names, so this is not a small perturbation: given a
visible target, the agent reorganised its effort around that target. It is Goodhart's law inside the
exercise — **a visible mutant set turns into the specification of "done"**, and coverage of everything not
in the set degrades. The blind run thought about the algebra; the feedback run optimised a metric.

Three consequences worth carrying into the materials:

1. For a strong agent the practice set is *not* additive: 4/4 green invites stopping. The graded set is
   what keeps the exercise honest, which is another reason it stays hidden.
2. The learner-facing framing should say that a clean practice report means "nothing known is missing",
   not "the suite is complete" — the exercise README already says a perfect practice score is not a
   thorough suite; this run is the evidence for that sentence.
3. The practice tier contains **only must-kill mutants**, so the agent never saw a spec-dependent survivor
   and the list-aliasing question never came up in either run. If we want the learner to meet the "some
   mutants are supposed to survive" case in practice rather than only in prose, the practice catalog needs
   a spec-dependent entry.

It also made an accurate observation we had not: `min-accessories-strict` was already dying *before* it
touched anything, because the AI's `MinAccessories(1).admits(hatDuck)` happens to sit exactly on the
`size == min` boundary. An incidental kill, not an intentional one — the flawed starter suite therefore
begins at 1/4 partly by luck, and its explicit boundary tests now carry that mutant on purpose.

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
