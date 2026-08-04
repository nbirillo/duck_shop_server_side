# Exercise 11.2, advanced tier — teacher notes

The basic tier of 11.2 is unchanged: fix the invalid test, harden the suite by reasoning, then check
it with mutation testing. It discriminates the local model tier well (36–63%) and a learner working
with a weak agent is fully occupied by it.

It does not challenge a frontier agent. Claude reaches 11/11 blind on the graded mutant set, and more
mutants of the same kind would not change that: the specification is complete and machine-readable,
the object is a pure boolean function of four fields, so the defect space is enumerable by a
checklist a strong agent already has. What is missing is not difficulty, it is a second and a third
direction. The advanced tier adds three constraints and a contract addendum that makes the first of
them well-founded.

## The contract addendum

The addendum belongs to this tier only, and lives in the exercise folder as its own file — **never as
an edit to the `:core` KDoc**, because the basic tier's lesson is precisely that the specification is
silent on these points and the learner has to decide. It states:

> Policies are pure: `admits` has no side effects and depends only on the duck. The order in which a
> combinator evaluates its policies, and how many times, are therefore not part of the contract. A
> combinator captures its policy list at construction; mutating the caller's list afterwards is
> unspecified.

So the same two questions change role between tiers. In the basic tier the learner decides them and
either answer counts, as long as it is a decision. In the advanced tier they have been decided, and
pinning them is now a defect. The advanced README says that in one line — without it a learner who
legitimately killed `allof-defensive-copy` in the basic tier gets penalised for it here and
reasonably concludes the rules changed at random.

## 1. Conformant variants — the suite must not over-specify

`spec-test-driven/variants/catalog.json`, run with `./gradlew verifyVariants --continue`. Same engine
as the mutants, opposite expectation: each entry is a legal rewrite of `:core`, the suite has to stay
GREEN, and a failure is reported as FALSE ALARM together with the tests to relax.

The catalog carries no prose, for the same reason the practice mutants carry none — naming the
behaviour each variant frees would say which tests to drop before the learner has worked it out. The
rationale therefore lives here:

| Variant | Rewrite | What a suite has to avoid to survive it |
| --- | --- | --- |
| `allof-no-short-circuit` | `map { }.all { }` | asserting that evaluation stops at the first rejection |
| `anyof-no-short-circuit` | `map { }.any { }` | the same for the first acceptance |
| `allof-reverse-order` | `policies.reversed().all { }` | asserting the order policies are consulted in |
| `allof-memoized` | caches the verdict per duck | asserting how many times a policy is consulted |
| `allof-defensive-copy` | `policies.toList()` | pinning that the caller's list stays live |
| `allof-fold` | `fold(true) { … && … }` | depending on `all {}` specifically |
| `not-explicit-comparison` | `== false` instead of `!` | — (syntactic; a sanity entry) |
| `max-budget-negated` | `!(price > maxPrice)` | — (syntactic) |
| `requires-accessory-contains` | `name in accessories.map { }` | asserting the accessories are scanned lazily |
| `min-accessories-count` | `count()` instead of `size` | — (syntactic) |

The first five have teeth; the rest are there so a suite that has fitted itself to the shape of the
code rather than to its behaviour shows up.

**Measured, 2026-08-04.** Every archived suite that *hardened a given suite* — both Claude runs and
all five local models — accepts all ten, zero false alarms. The only suite that trips it is Claude's
86-test suite written **from scratch**: three variants, four tests, all of them about evaluation
order and short-circuiting (`AllOf/AnyOf evaluates its policies in list order`, `AllOf/AnyOf
short-circuits after the first rejection/admission`).

Two conclusions worth carrying into the materials:

- Over-specification tracks **authoring freedom**, not model strength. The same agent hardening a
  given suite produces none of it; inventing a suite from nothing, it reaches for spies and order
  traces. So this check belongs to 11.3, where the learner's agent writes a suite from scratch, more
  than to 11.2.
- No local model over-specifies at any size — they never reach for a spy in the first place. The
  check is therefore free for a weak-agent learner and only bites a strong one, which is exactly what
  an advanced tier should do.

That eight independent suites stay green on all ten is also the evidence the rewrites really are
behaviour-preserving.

## 2. The test budget

`-PtestBudget=<n>` on `verifyMutants` or `verifyVariants`. The suite size is read off the baseline run
and always printed; with a budget it becomes a constraint, and the task fails after printing the full
report. Naming a budget is opting in to it, so it does not wait for `-PmutantsStrict`.

**The number is calibrated, not guessed.** Taking Claude's blind 47-test suite and asking which of
its tests kill which of the 11 graded must-kill mutants, a greedy cover shows **seven tests suffice**.
One of them, `a nested policy admits exactly the ducks it describes`, accounts for four mutants on its
own; four mutants are killed by exactly one test each (both empty-list cases, prefix matching, case
sensitivity) and are the fragile part of the set.

So the floor is 7, the suites that score 100% spend 47, and **the budget is 12** — real headroom, and
still a fourfold cut.

Say plainly in the materials that the budget is a teaching device rather than a style rule. It trades
the readability of one-assertion-per-test suites for the skill of choosing the discriminating case,
and it only works while the graded set stays hidden: a learner who knew the mutants would simply
write those seven.

## 3. The adversary

`runAgent -Pmode=attack` then `verifyAttack -Pagent=<name> --continue`. A second agent is given the
algebra and the learner's suite and asked for an implementation that passes every test and still
contradicts the specification. Unlike a catalog, this has no ceiling: each time the suite improves,
the next attack has to reach further out.

The oracle needs no answer key, so the whole loop runs in the student folder. The attack module and
`attacks/reference` both run `DifferentialProbe` over 2000 seeded random policy trees and ducks and
record their verdicts; `verifyAttack` diffs the two files. (Comparing the implementations directly is
impossible — same classes, same package, never the same classpath.) Outcomes:

- the suite fails against the attack → it caught it;
- the suite passes and the probe finds nothing → the attack is a correct implementation written
  differently, and says nothing about the suite;
- the suite passes and the probe disagrees → the suite has a hole, and the first disagreeing case is
  printed as the counterexample.

Failures the suite already has against the correct algebra are subtracted, or a suite with one broken
test would claim every attack as a catch.

`attacks/demo-prefix/` is a hand-written attack (accessory names matched by prefix) kept so the
machinery can be demonstrated without an API key: the flawed starter suite accepts it while the probe
disagrees in 36 of 2000 cases, and Claude's blind suite catches it.

### Measured: the local tier cannot attack (2026-08-04)

All five Ollama models were run live with `-Pmode=attack` against Claude's blind 47-test suite — the
strongest artifact we have, 11/11 on the graded mutants. None of them produced a usable attack:

| Attacker | What it returned | Verdict |
| --- | --- | --- |
| qwen2.5-coder:1.5b | `:core` verbatim | identical to `:core` — no attack |
| llama3.2:3b | `:core` verbatim | identical to `:core` — no attack |
| qwen2.5-coder:7b | `:core` verbatim | identical to `:core` — no attack |
| qwen2.5-coder:14b | **refused the task** | no code at all |
| qwen2.5-coder:32b | `RequiresAccessory` admits any duck wearing anything, commented `// Incorrect implementation` | differs in 222/2000, but caught by every suite tried |

Only 32b understood the task, and its attack is blunt rather than subtle: it was caught by Claude's
suite (3 tests), by its own hardened suite (1 test), and even by the flawed starter suite (3 tests).

So the adversary is a **frontier-versus-frontier** exercise. That is the same asymmetry the variant
check has, and it should be stated in the tier's framing: a learner with only a local model can do
the basic tier and the budget, but not this. Do not present the adversary as optional-but-equivalent
— without a capable attacker it produces a reassuring green that means nothing.

The 14b refusal is worth keeping as a per-agent note in its own right: asked to write code that
deliberately violates a specification, it answered *"I will not provide an implementation that
deliberately violates the specification or tests in a way that could cause harm or confusion."* An
adversarial-testing exercise can trip a model's refusal behaviour even though the task is ordinary
test engineering. Learners will hit this, and the materials should say what it looks like and that
rephrasing the request as "find an implementation this suite fails to distinguish from the correct
one" usually gets past it.

Two harness facts from the same runs: only 32b honoured the `// FILE:` contract, so the attack
builder infers which `:core` file was taken over from the **class names declared** in the reply, and
replaces a file only when every declaration in it was rewritten. Three of the five models returned
`:core` unchanged, which the probe reports as "identical" rather than as a pass — a copied reference
is a failed attack, not a clean suite.

### Measured: the frontier attack succeeded (2026-08-04)

Prompt E, blind, in a clean export, aimed at Claude's blind 47-test suite — the artifact that kills
all 11 graded mutants and is therefore *complete* by mutation testing's own measure. The attack got
past it: **suite PASSED, probe DIFFERS in 50 of 2000 cases.** It rewrote the leaves in two places the
suite never constructs:

- `RequiresAccessory("")` admits any duck. The suite checks case (`"Hat"`, `"HAT"`) and prefix
  (`"hatband"`), but never builds a policy with an empty required name. Specification says false for a
  bare duck; the attack says true.
- `MaxBudget` with `price.coerceAtLeast(0)`. Every boundary in the suite is at 0/1, 39/40, 99/100 — no
  duck costs less than nothing and no budget is negative. `MaxBudget(-1)` against a duck priced −2:
  specification admits, the attack refuses.

**This is the answer the tier was built to get.** Mutation testing was never the ceiling on this
algebra; a perfect mutation score means "nothing the mutants know about is missing", and an adversary
finds what they do not know about. Put the two numbers next to each other in the materials: 11/11 and
still broken.

### The probe's reach is itself a target — and it had a false negative

The attacker read `tools/probe/kotlin/…/DifferentialProbe.kt` — nothing forbade it, and it compiles
into the attacking module anyway — and picked its two defects *because they land inside the space the
generator samples*. It also reported a third hole it deliberately did **not** exploit: the suite pins
`AllOf`/`AnyOf` as boolean functions only for arities 0, 1 and 2, so an `AllOf` consulting
`policies.take(3)` would pass — and the probe, which built children of length 0..2, could never have
shown it.

Checked, and it was right: that implementation passed the suite while the probe reported **IDENTICAL**.
A real hole, reported as "not an attack". Fixed by raising the generator's width bound
(`MAX_WIDTH = 6`, covering combinator arity and accessory-list length); the same implementation now
disagrees in 17 of 2000 cases, and the frontier attack still shows at 44.

Two things to carry forward:

- **A "no disagreement" verdict is only as strong as the generator.** Whatever it cannot build, the
  check cannot see. If the algebra grows, widen the probe before trusting a clean report — and say so
  in the tier's framing, since the whole point of this exercise is not to mistake an instrument's
  silence for a property of the code.
- **This is Goodhart a third time**, after the mutation report and the practice tier: given a visible
  measurement, a capable agent optimises against the measurement. Here it did so *articulately* — it
  named the better attack and rejected it for being unmeasurable. That is worth quoting to learners
  verbatim; it is the clearest example in the whole module of a metric shaping the work rather than
  describing it.

**Still owed:** round 2 (prompt F, with feedback) to see whether feedback narrows the attacker the way
it narrowed the hardener, and a re-run of the local Ollama matrix under the budget.
