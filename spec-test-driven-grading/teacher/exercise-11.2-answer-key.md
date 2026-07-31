# Exercise 11.2 — answer key and grading

**Task as the learner sees it:** the admission-policy algebra is given and correct in `:core`; an
"AI-written" suite for it sits in `exercises/write-tests/src/test/kotlin/.../PolicyTests.kt`; verify
and harden it, then drive the mutation score to zero survivors. The student folder deliberately does
not list what is wrong or missing — that is this file.

## What is planted

**The invalid test.** `Not returns the decision of the wrapped policy` asserts that
`Not(KotlinOnly()).admits(kotlinDuck)` is `true`. `Not` inverts, so this fails on the correct code.
It is generated into the suite unconditionally by `runAgent -Pmode=verify-exercise`, so the exercise
always has at least one defect regardless of which model produced the rest. It is plausible-looking
on purpose: the name reads as if forwarding were the contract.

**The gaps.** The AI covers the happy path and skips:

- `MaxBudget` at exactly the limit (`price == maxPrice`).
- `MinAccessories` at exactly the minimum.
- `RequiresAccessory` on a duck with no accessories at all.
- `AllOf` / `AnyOf` over an **empty** list (vacuous truth: `true` and `false` respectively).
- `Not` inverting in both directions.
- Negative directions in general — e.g. that `KotlinOnly` *rejects* a duck without the attribute.

**The two under-specifications** (element A — deliberately not stated in the KDoc):

1. **`MaxBudget` boundary.** The prose never says whether the limit is inclusive. `:core` implements
   `<=`. A learner who decides "exclusive" and writes that test discovers the truth from the red run
   — which is the intended lesson: an ambiguity has to be resolved against something.
2. **`AllOf` / `AnyOf` aliasing.** They keep the caller's `List`, so mutating it after construction
   changes the policy's behaviour. Nothing says whether that is contract or accident. Both answers
   are defensible; the graded behaviour is *noticing* it and either pinning it with a test or
   flagging it for a human. Claude, unprompted, called it "an unspecified detail rather than intended
   contract" and recommended a defensive copy — a good exemplar to show.

## Grading

```bash
# validity: does the suite hold on the correct code?
cd ../spec-test-driven && ./gradlew :exercises:write-tests:test

# coverage: the graded mutant set (13 mutants, 11 must-kill), from this build
cd ../spec-test-driven-grading && ./gradlew verifyMutants --continue
```

`verifyMutants` scores `exercises/write-tests` by default; pass
`-PmutantTests=../spec-test-driven/hardened/<agent>/src/test/kotlin` for an archived agent run, or
`-PmutantTests=tests/kotlin` for this module's own authored policy suite.

Pass mark: valid on `:core`, every must-kill mutant dead, and a deliberate answer for both
spec-dependent mutants. See `mutation-testing-notes.md` for what "deliberate" means and for the
scores real agents reach.

## Regenerating the exercise

```bash
cd ../spec-test-driven
./gradlew runAgent -Pprovider=ollama -Pmodel=<model> -Pmode=verify-exercise
```

This asks the model for a suite with a deliberately generic prompt (it does **not** enumerate the
corner cases, so the result measures what the model finds on its own), then injects the invalid `Not`
test. Review the diff before committing: the run should be RED on the planted test and green
otherwise.
