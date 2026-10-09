# The test-writing exercise — Answer Key and Grading Guide

These notes are for teachers only.
This guide explains what we've deliberately put into the test-writing exercise, what students should discover, and how to grade their work.

## What students receive

Students start with two things:

- A correct implementation of the admission-policy algebra in `:core`.
- An AI-generated test suite in `exercises/write-tests/src/test/kotlin/.../PolicyTests.kt`.

Their task is to **check the existing tests, fix anything incorrect, add missing cases, and use mutation testing to find any remaining weaknesses**.
The student instructions don't reveal which tests are wrong or which cases are missing.
That's intentional. Finding those problems is the exercise.
This document contains the answer key.

---

## 1. What have we deliberately planted?

### The incorrect test

One test is guaranteed to be wrong.
Its name is:

`Not returns the decision of the wrapped policy`

It asserts that:

```kotlin
Not(KotlinOnly()).admits(kotlinDuck)
```

returns `true`.
But `Not` must **invert** the wrapped policy's decision.

Since `KotlinOnly()` accepts `kotlinDuck`, wrapping it in `Not` should produce `false`.
So this test fails against the correct implementation.
The test is deliberately written to look plausible. Its name suggests that `Not` forwards the wrapped policy's result, rather than reversing it.
The test generator inserts this incorrect test every time we run:

`runAgent -Pmode=verify-exercise`

That means the exercise always contains at least one known defect, regardless of which model generated the rest of the suite.

**What students should do:** Notice that the assertion contradicts the contract, then fix the test rather than changing the correct implementation to satisfy it.

### The missing test cases

The AI-generated suite usually covers straightforward examples but misses important boundaries and negative cases.
Here are the gaps students should look for:

| Policy                          | Missing behavior                                                           |
|---------------------------------|----------------------------------------------------------------------------|
| `MaxBudget`                     | A duck priced exactly at the budget limit: `price == maxPrice`             |
| `MinAccessories`                | A duck with exactly the required number of accessories                     |
| `RequiresAccessory`             | A duck with no accessories at all                                          |
| `AllOf`                         | An empty list of policies should return `true`                             |
| `AnyOf`                         | An empty list of policies should return `false`                            |
| `Not`                           | Inversion in both directions: `true` to `false`, and `false` to `true`     |
| `KotlinOnly` and other policies | Negative cases, such as rejecting a duck that lacks the required attribute |

The empty-list cases are especially useful.
They follow the usual rules of Boolean logic:

- `AllOf(emptyList())` is `true`: there is no policy that fails.
- `AnyOf(emptyList())` is `false`: there is no policy that succeeds.

These are examples of *vacuous truth*.
Students don't need to memorize the term, but they should understand why the results make sense.

---

## 2. Two behaviors are intentionally underspecified

Not every unanswered question in this exercise has a single correct answer.
We've deliberately left two details out of the KDoc.
Students should notice them and decide how to handle the uncertainty.

### Ambiguity A: Is the `MaxBudget` limit inclusive?

The description doesn't explicitly say whether a duck priced exactly at the maximum should be accepted.
The correct `:core` implementation uses:

```kotlin
price <= maxPrice
```

So the boundary is inclusive.
But a student could reasonably read the original wording as an exclusive limit and write a test expecting rejection.
That test would fail against `:core`.

**This is the intended learning moment.**
The failure doesn't automatically mean the student wrote a bad test or that the implementation is wrong.
It means the specification didn't settle the question.
Someone needs to decide what the contract should say.

For this exercise, `:core` uses the inclusive interpretation, but students should recognize that the original prose left room for disagreement.

### Ambiguity B: Do `AllOf` and `AnyOf` keep a reference to the input list?

Both combinators store the caller's `List` rather than making a defensive copy.
As a result, if the caller passes a mutable list and changes it after constructing the policy, the policy's behavior can change too.
But the KDoc doesn't say whether that's intended.

There are two defensible interpretations:

- **Live list:** The combinator should reflect changes to the original list.
- **Snapshot:** The combinator should preserve the policies it received at construction time.

Neither behavior is explicitly required by the specification.
So we're not grading students on choosing one particular answer.
We're grading whether they **notice the issue and handle it deliberately**.

A student might write a test that documents the current behavior.
Or they might flag the ambiguity and ask for a contract decision before writing the test.
Both are useful responses.

Claude Code independently noticed this issue in an earlier run. It described the aliasing as an unspecified implementation detail rather than an intended contract and recommended making a defensive copy.
That's a good example to show during the debrief.

---

## 3. Why the order of the exercise matters

The student instructions ask learners to **improve the test suite before running mutation testing**.
They should also save that pre-mutation version.
This isn't just about keeping their work organized.
It's part of what we're trying to teach.

In our experiments, a frontier agent that saw the practice mutation report early actually performed **worse on the hidden graded mutants** than the same agent working without feedback.
The agent focused its effort on the visible mutants instead of exploring the full behavior of the algebra.

The details are in [`mutation-testing-notes.md`](mutation-testing-notes.md).

Students may do the same thing.
So when reviewing their work, ask:

- Which missing cases did you find by reading the specification?
- Which cases did you add only after seeing the mutation report?
- Did the report change how you approached the problem?

Both reasoning and feedback are useful.

The important lesson is that **mutation testing should help you evaluate your tests, not replace thinking about the contract**.

---

## 4. How to grade the exercise

There are two main checks.

### Check 1: Does the test suite pass against correct code?

From the grading project, run:

```bash
cd ../spec-test-driven
./gradlew :exercises:write-tests:test
```

This checks whether the student's tests are valid against the correct implementation in `:core`.
A suite that fails against correct code isn't ready for mutation scoring.

### Check 2: Does it catch the graded mutants?

Switch to the teacher grading build:

```bash
cd ../spec-test-driven-grading
./gradlew verifyMutants --continue
```

The graded catalog contains **13 mutants**:

- **11 must-kill mutants** representing behavior the contract does specify.
- **2 spec-dependent mutants** involving behavior that isn't fully specified.

By default, `verifyMutants` uses the tests in `exercises/write-tests`.
You can also point it at a different suite.
For an archived agent run:

```bash
./gradlew verifyMutants \
    -PmutantTests=../spec-test-driven/hardened/<agent>/src/test/kotlin \
    --continue
```

For the grading module's own authored policy suite:

```bash
./gradlew verifyMutants -PmutantTests=tests/kotlin --continue
```

### What counts as a passing submission?

A successful submission should meet three conditions:

1. **Valid tests:** The suite passes against the correct `:core` implementation.
2. **Complete required mutation coverage:** All 11 must-kill mutants are caught.
3. **Deliberate handling of ambiguity:** The student recognizes and addresses both spec-dependent behaviors.

That last condition doesn't mean they must kill both spec-dependent mutants.
It means they should explain what the specification leaves open and either document a chosen interpretation or flag the decision for a human.

See [`mutation-testing-notes.md`](mutation-testing-notes.md) for more detail on what counts as a deliberate decision, along with the scores achieved by real agents.

---

## 5. How to regenerate the exercise

If you want to generate a fresh AI-written test suite, run:

```bash
cd ../spec-test-driven

./gradlew runAgent \
    -Pprovider=ollama \
    -Pmodel=<model> \
    -Pmode=verify-exercise
```

This does two things.
First, it asks the model to write tests using a deliberately general prompt.

**The prompt doesn't list the missing boundary cases.**
That's important because we want to see what the model discovers independently, not whether it can follow a checklist of known gaps.

Second, the generator inserts the incorrect `Not` test.
This ensures that students always receive a suite with at least one defect to find.

### Check the generated result before committing

Review the Git diff after regeneration.

The expected starting state is:

- **RED** on the deliberately incorrect `Not` test.
- **GREEN** on the rest of the generated suite.

If other tests fail, investigate them before publishing the exercise.

The goal is to give students a realistic, imperfect AI-generated suite with a known starting problem, not an unpredictable collection of unrelated failures.

---

## The main takeaway

The test-writing exercise is about more than increasing a mutation score.

Students should learn to distinguish three different situations:

- **An incorrect test:** It contradicts behavior the specification clearly requires.
- **A missing test:** It fails to check behavior the specification already defines.
- **An underspecified contract:** The requirements don't say enough to determine which behavior is correct.

Those situations need different responses.
Fix the incorrect test. Add the missing test. Clarify the contract when the requirements leave a decision open.

**A strong test suite doesn't just catch bugs. It makes clear which behaviors are actually part of the contract.**