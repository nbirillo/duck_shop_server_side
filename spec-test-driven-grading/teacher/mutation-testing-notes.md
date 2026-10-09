# Mutation Testing — Teacher Notes

These notes are for teachers only.
This guide explains how the mutation-testing infrastructure works, what we've learned from running real AI agents against it, and how to use those findings in exercise 11.2.
The most important lesson is that **a perfect mutation score doesn't necessarily mean a complete test suite**. Our experiments show why.

## 1. How the mutation-testing infrastructure works

The mutation catalog lives in two places:

- `mutants/catalog.json` in the grading build contains the hidden graded mutants.
- `spec-test-driven/mutants/` contains the visible practice mutants.

Each catalog entry describes one textual replacement in a `:core` source file.
When you run:

```bash
./gradlew generateMutants
```

Gradle generates a module for each mutant.
That module compiles the real `:core` sources, except for the file being mutated. It replaces that one file with a modified copy.
This design has several important benefits.

### Mutants stay connected to the real implementation

We don't maintain separate hand-written copies of the admission-policy algebra.
Every unchanged source file comes directly from `:core`.
That means the mutant implementations can't silently drift away from the real code.

### All `:core` types remain available

The generated modules include every type from `:core`, including `Shop`.
This matters because students may write tests that exercise `Shop`, not just individual policies.
Earlier hand-written practice mutants didn't include `Shop`.

As a result, those tests failed to compile and were reported as `ERROR` instead of `CAUGHT`.
That unintentionally discouraged students from testing `Shop`.
The generated modules fix this.

### Mutation patterns must match exactly once

The generator checks that each `find` pattern matches exactly one location.
If it doesn't, generation fails with an error.

This is important because a code fragment such as:

```kotlin
duck.price <= maxPrice
```

can appear both in the implementation and in its KDoc.
A careless replacement might change only the documentation while leaving the behavior untouched.
That would create a mutant identical to the correct implementation — one no test could kill.
The generator therefore matches complete `override` lines instead of loose fragments.

### The baseline prevents false mutation scores

The generator also creates a **baseline module**.
It runs the same test suite against the original, unmutated `:core`.
Tests that already fail against correct code don't count toward killing mutants.
Without this check, the flawed starter suite in exercise 11.2 appeared to kill **4/4 mutants**.

Its deliberately incorrect test failed everywhere, so it looked as if every mutant had been caught.
With baseline validation, the honest starting score is **1/4**.
**A failing test only counts as useful evidence if it passes against the correct implementation.**

---

## 2. Why there are two mutation tiers

We deliberately separate practice mutants from graded mutants.

| Tier         | Location                                               | Contents                                                                                                                     |
|--------------|--------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| **Practice** | `spec-test-driven/mutants/` — visible and committed    | 5 mutants: 4 must-kill mutants covering boundaries and vacuous truth, plus `allof-defensive-copy` as a spec-dependent mutant |
| **Graded**   | `mutants/` in the grading build — hidden from learners | 13 mutants: 11 must-kill mutants, including three subtle accessory-matching defects, plus 2 spec-dependent mutants           |

The practice catalog intentionally omits the `what` and `hint` descriptions.
Otherwise, it would become a list of exactly which tests students should write.

The practice set is there to provide feedback, not to reveal the answer key.
We also include a spec-dependent mutant in the practice set.
That gives students a chance to encounter a mutant that may legitimately survive.
Not every surviving mutant represents a missing test.

---

## 3. Why some mutants are allowed to survive

Only the **must-kill mutants** count toward the mutation score.
The two spec-dependent mutants exist to teach a different lesson: sometimes the specification doesn't define enough behavior to say whether a change is wrong.

### `allof-no-short-circuit`

This mutant changes `AllOf` so it evaluates every policy, even after one has already rejected the duck.
The final Boolean answer is unchanged for every input.
To detect the difference, a test needs something like a spy policy that counts how often it's called.
But the specification doesn't promise short-circuit evaluation.

So both outcomes can be acceptable:

- **Leave the mutant alive:** Short-circuiting isn't part of the required contract.
- **Kill the mutant:** Explicitly decide that short-circuiting is required and test for it.

What matters is making that decision deliberately.
In one earlier run, Claude killed this mutant using a spy.

### `allof-defensive-copy`

This mutant makes `AllOf` copy the list of policies it receives.
In the original implementation, `AllOf` retains the caller's list.
If the caller later changes a mutable list, the policy's behavior can change too.
With a defensive copy, those later changes no longer affect the policy.

Only a test that modifies the list **after constructing `AllOf`** can distinguish the two implementations.
This is the second intentional ambiguity in exercise 11.2.

Again, there are two defensible choices:

- **Kill the mutant:** Decide that changes to the caller's list must remain visible.
- **Leave it alive:** Treat that aliasing behavior as an implementation detail, not a contractual guarantee.

In an earlier run, Claude deliberately left this mutant alive and recommended using a defensive copy.

### The teaching point

A mutation score isn't supposed to mean “100% of every possible mutant.”
Some mutants are equivalent to the original implementation from the perspective of the contract.
Others differ only in behavior the specification never promised.
Trying to kill all of them can lead to **over-specification**: tests that freeze implementation details a future refactor should be free to change.

---

## 4. Measured results — July 31, 2026

We evaluated the agents against the hidden graded set of **11 must-kill mutants**.
For the local-model experiments, each model received the same flawed starter suite and a general instruction to verify and improve it.
The command used:

`runAgent -Pmode=verify-harden`

The models didn't see the graded mutants.

### A note about isolation

We repeated these measurements after moving the answer keys out of the student folder.
The results were identical: same scores and same surviving mutants.
That makes sense because `runAgent` builds its prompt from `:core` and the flawed suite, not from the folder's documentation.
The isolation work matters for **interactive agents**, which can explore files in their working directory.
It doesn't change what the API-based harness sends to local models.

### Results

| Agent                                          | Valid on `:core`?                                 | Graded mutation score |
|------------------------------------------------|---------------------------------------------------|----------------------:|
| `qwen2.5-coder:1.5b`                           | Yes — 10 tests                                    |            4/11 (36%) |
| `llama3.2:3b`                                  | **No — duplicate test names prevent compilation** |                     — |
| `qwen2.5-coder:7b`                             | Yes — 13 tests                                    |            5/11 (45%) |
| `qwen2.5-coder:14b`                            | Yes — 15 tests                                    |            5/11 (45%) |
| `qwen2.5-coder:32b`                            | Yes — 15 tests                                    |            7/11 (63%) |
| This module's hand-written policy suite        | Yes — 14 tests                                    |        **8/11 (72%)** |
| **Claude Code — blind hardening**              | Yes — 47 tests                                    |      **11/11 (100%)** |
| Claude Code — hardening with practice feedback | Yes — 47 tests                                    |       **10/11 (90%)** |
| Claude Code — suite written from scratch       | Yes — 86 tests                                    |      **11/11 (100%)** |

The difference between the two 47-test Claude runs is particularly important.

---

## 5. Claude Code without feedback — the frontier measurement

For the blind run, we gave Claude a clean export of the tracked student project.
The export contained:

- No grading build.
- No answer key.
- No test suites from other agents.

The prompt also explicitly forbade reading `mutants/` or running `verifyMutants`.
Claude had to improve the tests by reasoning about the admission-policy algebra.

### What did it do?

Claude fixed the deliberately incorrect `Not` test.
It also renamed the test so its name accurately described the behavior being checked.
Then it added **37 tests**.
The finished suite caught **all 11 must-kill mutants** without seeing any mutation report.

### It found defects every local model missed

Three particularly interesting cases were covered without prompting.

**Exact accessory-name matching**

An accessory named `"hatband"` must not satisfy a requirement for `"hat"`.
Claude tested the distinction, including the reverse direction.

**Case-sensitive matching**

Accessory names with different capitalization must not be treated as identical.

**Checking accessories beyond the first item**

Claude deliberately placed the required accessory third in the list.
It reasoned that the original suite might pass an incorrect implementation that only checked `policies.first()`.
These are exactly the kinds of cases the local models consistently missed.

### How did it handle the underspecified behaviors?

Claude made a deliberate decision about short-circuiting.
It argued that the policies in this exercise are pure, so evaluation order isn't observable through the normal contract.
A test that freezes short-circuit behavior would unnecessarily restrict legitimate implementation changes.
Instead of writing such a test, Claude documented the decision in the KDoc.

**That's the intended approach.**
But it didn't address list aliasing at all in this run.
The `allof-defensive-copy` mutant survived, not because Claude explicitly chose that interpretation, but because it never raised the question.

In an earlier run where Claude wrote a suite from scratch, it had independently noticed the aliasing issue and recommended a defensive copy.
This is useful teaching material.
**Even a frontier agent doesn't identify every underspecification consistently across runs.**

### It also found additional cases

Claude voluntarily tested behavior the exercise hadn't specifically asked it to investigate:

- `MinAccessories(0)` and negative minimums admit every duck. It tested the implementation's existing behavior instead of assuming invalid inputs should throw.
- Duplicate accessory names count toward `MinAccessories`.
- Single-element combinators behave like identity operations.
- Both De Morgan laws hold.

### What does this tell us?

The clean-room measurement confirmed that Claude could solve the basic mutation-testing challenge.
The isolation work didn't make the task harder.
It made the result **trustworthy**.

Before isolation, the working folder contained acceptance tests whose names effectively revealed the answers.
Now we can say Claude reached 11/11 without seeing those clues.
For a frontier agent, mutation testing on this algebra is more of a confirmation than a challenge.

---

## 6. The surprising result: Feedback made Claude score worse

We ran the same task again.
This time we used:

- The same flawed starter suite.
- A fresh project export.
- A fresh Claude session.
- A prompt that allowed mutation testing.

Claude could run:

```bash
./gradlew verifyMutants --continue
```

and improve its suite until the practice report was clean.

It did exactly that.
The practice score reached **4/4**, and `-PmutantsStrict` passed.
Then Claude stopped.

But when we scored the suite against the hidden graded mutants, it caught only **10/11**.
That's one fewer than the blind run.

### Which defect did it miss?

The surviving mutant was:

`requires-accessory-prefix`

In the blind run, Claude had tested whether `"hatband"` incorrectly matched `"hat"`, and vice versa.
That directly caught the prefix-matching defect.

In the feedback run, Claude still wrote tests about accessory names.
But it focused on:

- Case sensitivity, such as `"Hat"`.
- Leading whitespace, such as `" hat"`.

Those are reasonable tests.
However, neither catches the prefix mutant.

The practice mutation report didn't point toward prefix matching, so Claude never added that case.

### This wasn't a tiny change

Both finished suites contained **47 tests**.
But they shared only **17 test names**.
Claude had substantially reorganized its testing strategy.

In the blind run, it reasoned broadly about the algebra.
In the feedback run, it focused on satisfying the visible mutation catalog.

This is a concrete example of **Goodhart's law**: once a measure becomes the target, optimizing it can undermine the broader goal.
The visible practice mutants effectively became Claude's definition of “done.”
Coverage outside that list got worse.

---

## 7. What should we change in the exercise?

The feedback experiment suggested three important changes.

### Change A: Don't treat 4/4 as proof of completeness

For a strong agent, the practice mutant set doesn't necessarily add coverage.
A green report can encourage it to stop.
That's why we keep the graded mutants hidden.

The student-facing instructions should make the distinction clear:
**A clean practice report means no known practice mutants survived. It doesn't mean the test suite is complete.**
The exercise README already says this.
Now we have experimental evidence supporting it.

### Change B: Include a legitimately surviving mutant in practice

Originally, the practice catalog contained only must-kill mutants.
That meant students never encountered a mutant that could reasonably survive.
It also meant the list-aliasing question might never come up unless an agent noticed it independently.

We added `allof-defensive-copy` as a spec-dependent practice mutant.
Now students can see that a surviving mutant may represent an unresolved contract decision rather than a missing test.

We verified the intended behavior:

- The flawed starter suite still scores **1/4** on must-kill mutants.
- The spec-dependent mutant is reported separately.
- Claude's blind suite scores **4/4** on the practice must-kill set while leaving the spec-dependent mutant alive.

That's the kind of result we want students to learn how to interpret.

### Change C: Make students reason before showing them the report

We also changed the exercise instructions.
Students must now:

1. Improve the test suite **without running mutation testing**.
2. Save that version.
3. Run `verifyMutants`.
4. Compare what they found independently with what the report revealed.

The README explains the reason in one line: a checklist can make it harder to think beyond the checklist.
This turns our calibration finding into part of the learning experience.
Instead of accidentally steering students toward the visible score, we ask them to notice how feedback changes their thinking.

### One more observation Claude made

Claude also noticed something we hadn't explicitly accounted for.
The mutant `min-accessories-strict` was already being killed by the flawed starter suite.
Why?
Because the original AI-generated test included:

```kotlin
MinAccessories(1).admits(hatDuck)
```

That happens to test the exact boundary:

`size == min`

So the starter suite's **1/4** score was partly accidental.
The test wasn't necessarily written to check the boundary.
It just happened to catch that mutant.

After hardening, explicit boundary tests covered the same behavior deliberately.
That's another useful distinction:
**A mutant can be killed by accident. A strong test suite should make important boundary decisions intentional and understandable.**

---

## 8. How should we interpret the model scores?

There are several conclusions worth sharing with students.

### Local models consistently miss the same kinds of defects

Every local model left these mutants alive:

- The inclusive `<=` budget boundary.
- Accessory-name prefix matching.
- An implementation that checks only the first accessory.
- Case-insensitive accessory matching.

These are meaningful gaps, not just low scores.

### Even our hand-written suite had gaps

The module's own authored policy suite scored **8/11**.
It missed exactly the three accessory-matching mutants.
That's worth showing students.
Mutation testing doesn't only expose weaknesses in AI-generated tests.
It can reveal missing cases in a hand-written, test-first suite too.

### The basic exercise separates weaker models, but not frontier agents

The mutation-testing task clearly distinguishes weaker and mid-tier models.
But it doesn't provide much of a challenge for Claude Code.

That's why the advanced tier doesn't simply add more mutants of the same kind.
Instead, it introduces three different directions:

- **Conformant variants.**
- **A calibrated test budget.**
- **An adversary.**

See [`advanced-tier-notes.md`](advanced-tier-notes.md) for how each one works and what it measures.
The deeper API/OpenAPI scenario in exercise 11.7 provides another way to increase difficulty.

For students using weaker agents, the basic exercise already offers plenty of work: verifying and improving the generated suite is challenging enough.
For students using stronger agents, the advanced tier should follow.

---

## 9. Running the frontier experiments yourself

The instructions for the two Claude Code runs are in:
`running-advanced-agents.md`

Use:

- **Prompt C** for blind hardening without mutation feedback.
- **Prompt D** for hardening with practice-mutant feedback.

Keep the runs separate, start from a clean export each time, and score the results using the teacher-only grading build.
The original experiment notes also identify the harden-regime comparison as an open follow-up. The measured blind and feedback results above provide that comparison.

---

## The main takeaway

This experiment produced two lessons that are easy to miss.

**First, a perfect mutation score is meaningful only relative to the mutants being tested.**
The visible practice catalog cannot tell us whether every important behavior is covered.
And spec-dependent mutants remind us that not every behavioral difference should be treated as a defect.

**Second, feedback can narrow an agent's attention.**
Claude found all 11 graded defects when it reasoned independently.
When it optimized against the visible practice report, it missed one.
That doesn't mean mutation testing is unhelpful.
It means we should use it **after** reasoning about the contract, not as a substitute for that reasoning.

The goal is to build a suite that expresses the important behavior of the system — not merely one that turns a particular report green.