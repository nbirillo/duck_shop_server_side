# Running Advanced Interactive Agents — Teacher Guide

These notes are for teachers only.
This guide explains how to evaluate a **strong interactive coding agent**, such as Claude Code, JetBrains Junie, or Cursor, without needing an API key.

It's the interactive-agent counterpart to `runAgent`, which supports OpenAI-compatible providers such as Ollama, Mistral, and Anthropic.
The goal is to give interactive agents the same exercises, preserve a fair testing environment, and score their work using the same tools as the API-based runs.
**Unless a section says otherwise, run the commands from `spec-test-driven/`.**

### Why is this guide teacher-only?

This file describes the deliberately planted defects and includes the prompts we use to evaluate agents.
An agent working in the student project shouldn't be able to read those details.
That's why the guide lives in the teacher pack rather than in `spec-test-driven/tools/`.

### What happened to Prompts A and B?

Prompts A and B were part of this guide until September 18, 2026.
They covered an earlier exercise about implementing opening-schedule stubs.
That exercise never made it into the course slides, so we removed it, along with the `checkPrimary` and `resetStarter` tasks.
The prompts below are the ones still used in the course.

---

## 1. Set up the agent's workspace correctly

**Open `spec-test-driven/` itself as the project in the agent's CLI or IDE.**
Do **not** open the repository root.
This is important because the teacher-only grading build lives outside the student project:

`../spec-test-driven-grading/`

The reference answers and hidden evaluation materials are kept there.
When the agent is working inside `spec-test-driven/`, it can read the files it needs for the exercise without being given the grading materials.

### We verified this setup

In our Claude Code experiment, the agent read the contract in `core/`.
That's expected and allowed: the contract is part of the task.
The agent didn't have access to the grading build.

But remember that this isolation depends on the directory you give the agent.
**If you open the repository root instead of `spec-test-driven/`, you lose that separation.**
For a controlled experiment, use a clean student export and keep the teacher grading build outside the agent's workspace.

---

## 2. Exercise 11.2 — Verify and harden an AI-written test suite

The exercise lives in:
`exercises/write-tests/`

Students receive a flawed test suite that was supposedly written by an AI agent.
Their task is to:

1. Check whether the existing tests describe the correct behavior.
2. Fix tests that contradict the contract.
3. Add missing test cases.
4. Evaluate the improved suite using mutation testing.

For API-based models, we use:
`runAgent -Pmode=verify-harden`

For interactive agents, use one of the two prompts below.
Then archive the resulting suite and evaluate it in the same way.

### Prompt C — Blind hardening

Use this prompt when you want to measure what the agent can discover **without mutation-testing feedback**.

```text
In exercises/write-tests/src/test/kotlin/.../PolicyTests.kt an AI wrote a test suite for the
admission-policy algebra given in :core. Verify and harden it: fix any test that contradicts the
correct behaviour, and add the cases it is missing. Do not change :core or any other module.
Do NOT run ./gradlew verifyMutants and do not open mutants/ — I want to see what you cover on
your own.
```

This is the closest comparison with the blind `runAgent` experiments.
The agent has to reason from the contract and the existing tests.
It can't use the mutation catalog as a checklist.

### Prompt D — Hardening with mutation-testing feedback

Use this prompt to simulate the more realistic learner workflow, where the agent can run mutation tests and improve its suite based on the report.
Start with the same task as Prompt C, but replace the restriction on mutation testing with this instruction:

```text
Run ./gradlew verifyMutants --continue and keep strengthening the suite
until every must-kill mutant is dead. Do not edit anything under mutants/ or :core.
```

This gives the agent access to the practice mutation report.
It can inspect which practice mutants survive, add tests, and repeat the process.

### Why run both?

The two prompts measure different things.

| Prompt                | What it measures                                                                      |
|-----------------------|---------------------------------------------------------------------------------------|
| **C — Blind**         | What the agent can discover by reasoning about the specification and tests on its own |
| **D — With feedback** | How well the agent uses mutation-testing results to improve its suite                 |

**Run them separately, starting from the same flawed suite each time.**
Don't run Prompt D in a workspace that already contains Prompt C's improvements.
Otherwise, you won't be comparing independent approaches.

The difference between the two results is part of the experiment.
In our measured runs, Prompt D actually scored **one graded mutant worse** than Prompt C.

The agent optimized against the visible practice report and missed a defect it had found when working blind.
See [`mutation-testing-notes.md`](mutation-testing-notes.md) for the full analysis.

---

## 3. Archive the agent's test suite

After the agent finishes, save its tests before restoring the exercise to its original state.
The `hardened/` directory is Git-ignored.
This is the same general layout used by `runAgent`.

From `spec-test-driven/`:

```bash
# Create a directory for this agent's test suite.
mkdir -p hardened/<agent>/src/test/kotlin/org/jetbrains/kotlin/course/duck/shop/admission

# Copy the agent's finished tests into the archive.
cp exercises/write-tests/src/test/kotlin/.../PolicyTests.kt \
   hardened/<agent>/src/test/kotlin/.../

# Reuse the Gradle consumer script from an existing archived run.
cp hardened/<any-existing-run>/build.gradle.kts hardened/<agent>/

# Restore the original flawed exercise suite.
git restore -- exercises/write-tests
```

Use a different `<agent>` name for each run.
For example, keep the blind and feedback runs in separate directories so you can compare them later.
The archived suite should contain the agent's finished work, while `exercises/write-tests/` returns to its original state.

---

## 4. Check whether the archived suite is valid

Before scoring mutants, make sure the tests pass against the correct implementation in `:core`.
From `spec-test-driven/`:

```bash
./gradlew :hardened:<agent>:test
```

This is the validity check.
If the suite fails against correct code, it isn't ready for a meaningful mutation score.
A test that fails everywhere isn't evidence that it catches a defect.

---

## 5. Score the suite against the hidden mutants

Now switch to the teacher-only grading build:

`spec-test-driven-grading/`

Run:

```bash
./gradlew verifyMutants \
    -PmutantTests=../spec-test-driven/hardened/<agent>/src/test/kotlin \
    --continue
```

This is the important score.
The graded mutants live in the teacher-only build.
The agent never saw which defects it would be evaluated against.
That makes the result more useful than the practice score alone.

The practice report tells the agent which known practice mutants it caught.
The hidden graded report tells **you** how well its tests generalize beyond that visible set.
For expected scores and guidance on interpreting survivors, see [`mutation-testing-notes.md`](mutation-testing-notes.md).

---

## 6. Where to find the other interactive-agent guides

The rest of the module has its own teacher notes.

| Exercise                                                                      | Guide                                                                                                             |
|-------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| **11.2 advanced tier** — conformant variants, test budgets, and the adversary | [`advanced-tier-notes.md`](advanced-tier-notes.md) and [`claude-attack-checklist.md`](claude-attack-checklist.md) |
| **11.4** — using a frontier agent as the implementer                          | [`claude-implement-checklist.md`](claude-implement-checklist.md)                                                  |
| **11.6** — the capstone, feedback, and debrief                                | [`capstone-11.6.md`](capstone-11.6.md)                                                                            |

---

## The main takeaway

When evaluating an interactive coding agent, **the environment matters as much as the prompt**.
Keep the agent inside the student project, away from the hidden grading materials.
Start each run from the same clean state.
Save the resulting test suites before resetting the exercise.
And distinguish between two different abilities:

- Finding missing tests independently.
- Improving tests using mutation-testing feedback.

Both are useful, but they aren't the same skill.
Our experiments show why the distinction matters: **an agent that gets a perfect visible practice score can still perform worse on unseen defects than one that never saw the report.**