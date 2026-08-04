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

**Still owed:** a live `-Pmode=attack` run — the mode is dry-verified only — and a re-run of the local
Ollama matrix under the budget.
