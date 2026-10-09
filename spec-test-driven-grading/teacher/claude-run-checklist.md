# Checklist — Measuring Claude Code on The test-writing exercise

These notes are for teachers only.
This checklist walks you through measuring Claude Code on the test-writing exercise.
The actual prompts are in [`running-advanced-agents.md`](running-advanced-agents.md). This document explains **when to use each prompt, how to prepare the project, and how to collect trustworthy results**.

Two things can quietly invalidate the experiment:

1. Letting the agent see files from previous runs.
2. Letting the agent see mutation-testing feedback during the blind run.

Let's make sure neither happens.

---

## 0. Start with a clean student project

**Don't run this experiment in your normal `spec-test-driven/` working directory.**

That folder contains generated files from earlier experiments. They're git-ignored, but they're still visible to an AI agent working in the directory.
Some of them reveal exactly what we're trying to measure.

For example:

- `test-suites/claude-code/` contains an 86-test suite that already catches all 11 graded mutants.
- `test-suites/ollama-*` and `hardened/ollama-*` contain other models' test suites for the same admission-policy algebra.
- `attacks/*` and `implementations/*` contain results from previous experiments.

If Claude reads those files, a high score wouldn't tell us much about its ability to solve the exercise independently.

### Create a clean export

Instead, export only the files tracked by Git.
From the repository root, run:

```bash
cd <repo>

rm -rf ~/IdeaProjects/duck-shop-student-copy
mkdir -p ~/IdeaProjects/duck-shop-student-copy

git archive HEAD spec-test-driven | tar -x -C ~/IdeaProjects/duck-shop-student-copy
```

This gives us the same project a learner would receive, without the extra files from our experiments.
It also protects the working copy: Claude can edit the export without changing our original files.

### Check the starting state

The exported project is self-contained. It has its own Gradle wrapper and `build-logic`, so you can run it directly.
Before opening Claude Code, check that the exercise starts in the expected flawed state:

```bash
cd ~/IdeaProjects/duck-shop-student-copy/spec-test-driven

./gradlew verifyMutants --continue
```

The expected result is:

- **Baseline INVALID:** One test fails against the correct implementation.
- **Mutation score: 1/4.**

That's the starting point students are supposed to improve.
**This clean export is also exactly what we should hand out to learners.**

It contains no `spec-test-driven-grading/` directory, so the teacher-only answers and graded mutants aren't available inside the project.

---

## 1. Open the correct folder in Claude Code

Open this exact folder as the Claude Code project:

`~/IdeaProjects/duck-shop-student-copy/spec-test-driven`

Not the repository root, and not the parent directory of the export.
This matters because the exported student project doesn't contain:

- The reference implementation used for grading.
- The graded mutant catalog.
- The answer key.
- This teacher checklist.

Those files aren't merely hidden behind instructions. They're absent from the exported project.

### One more thing: Check your global Claude instructions

If you have a global file at:

`~/.claude/CLAUDE.md`

make sure it doesn't contain notes about this module that could reveal the answers.
Claude Code may load that file automatically.
Even a perfectly clean project export won't help if the agent receives the missing information through global instructions.

---

## 2. Run A — Blind, without mutation-testing feedback

**This is the run we can compare directly with the local-model results.**
Start a fresh Claude Code session in the exported student project.
Then paste **Prompt C** from [`running-advanced-agents.md`](running-advanced-agents.md).
The prompt asks Claude to:

- Find and fix the incorrect test.
- Add tests for missing behavior.
- Avoid reading `mutants/`.
- Avoid running `verifyMutants`.

We want to find out how much the agent can cover **without seeing the mutation report**.
That's the same kind of measurement we used for local models with:

`runAgent -Pmode=verify-harden`

### What if Claude follows the README instead?

There's an intentional conflict here.
The student README, `exercises/write-tests/README.md`, tells learners to run mutation testing as the third step.
But Prompt C explicitly forbids it.

For this experiment, **the prompt's restriction is part of what we're measuring**.
If Claude runs `verifyMutants` anyway, record that.
A frontier agent ignoring an explicit instruction is itself a useful finding. But its result should no longer be described as fully blind.

### Save the finished test suite

When Claude stops, record that Run A has finished.
Then copy its tests from the export into the working checkout, where we can score them without giving Claude access to the teacher tools.
From `<repo>/spec-test-driven`, run:

```bash
PKG=org/jetbrains/kotlin/course/duck/shop/admission
EXPORT=~/IdeaProjects/duck-shop-student-copy/spec-test-driven

mkdir -p hardened/claude-code-promptC/src/test/kotlin/$PKG

cp $EXPORT/exercises/write-tests/src/test/kotlin/$PKG/PolicyTests.kt \
   hardened/claude-code-promptC/src/test/kotlin/$PKG/

cp hardened/ollama-qwen2.5-coder-7b/build.gradle.kts \
   hardened/claude-code-promptC/
```

We save the suite as:

`hardened/claude-code-promptC/`

There's nothing to restore in the working checkout because Claude only edited the clean export.

---

## 3. Run B — With mutation-testing feedback

Now let's measure a different skill.
Instead of asking how many bugs Claude can anticipate on its own, we want to know:
**Can Claude use mutation-testing feedback to improve its test suite?**

### Reset the environment

Recreate the clean export using the commands from step 0.
Don't reuse the project from Run A.
That project already contains Claude's changes, so it no longer represents the original exercise.
Also start a **fresh Claude Code session**.
An existing session may remember the tests it wrote during Run A, even if you reset the files.

### Use Prompt D

Paste **Prompt D** from [`running-advanced-agents.md`](running-advanced-agents.md).
It gives Claude the same basic task, but this time the agent may run:

```bash
./gradlew verifyMutants --continue
```

It can inspect the report, improve its tests, and repeat the process until no mutants survive.
This is closer to the workflow students would actually follow.

Run A measures what Claude can do unaided.
**Run B measures how well Claude can use feedback.**

Save the resulting suite under:

`hardened/claude-code-promptD/`

Keep the two results separate so we can compare them fairly.

---

## 4. Score the results

Now we can evaluate the archived suites.
**Do this outside the Claude Code session.**
The teacher grading report reveals which graded mutants survive. If Claude sees that information, it gets access to the answers for the very test we're using to measure it.

### First, check that the suite is valid

From the student project in your working checkout:

```bash
cd <repo>/spec-test-driven

./gradlew ":hardened:claude-code-promptC:test"
```

This runs the archived test suite against the correct `:core` implementation.
The suite must pass before its mutation score is meaningful.
If it fails against correct code, we have a test-validity problem, not simply a low mutation score.

### Then run the teacher-only grading check

Switch to the grading project:

```bash
cd ../spec-test-driven-grading

./gradlew verifyMutants \
    -PmutantTests=../spec-test-driven/hardened/claude-code-promptC/src/test/kotlin \
    --continue
```

This evaluates the suite against the graded mutants that students and their agents cannot see.
Repeat the process for `claude-code-promptD` when Run B is ready.

### What should we record?

For each run, collect:

| Measurement                | What to look for                                                                |
|----------------------------|---------------------------------------------------------------------------------|
| **Validity**               | Does the suite pass against the correct `:core`?                                |
| **Test count**             | How many tests did Claude write?                                                |
| **Graded mutation score**  | How many of the 11 must-kill mutants did the suite catch?                       |
| **Spec-dependent mutants** | How did the suite handle the two behaviors the basic specification leaves open? |
| **Constraint compliance**  | Did Claude follow the rules of the blind or feedback run?                       |

Add the results to the table in [`mutation-testing-notes.md`](mutation-testing-notes.md).
Remember that the two spec-dependent mutants shouldn't be treated as ordinary must-kill defects.
They expose behavior that the basic contract doesn't fully specify.

---

## What are we trying to learn?

Before we separated the student and teacher materials, the project contained too many clues.
The policy acceptance tests had names that revealed expected behavior.
Some READMEs explicitly listed the missing cases and the two underspecified questions.
And generated suites from previous runs were available for an agent to inspect.
Under those conditions, a frontier agent getting a perfect score wouldn't prove very much.

**This experiment is designed to give us a trustworthy measurement instead.**
The clean export gives Claude the same materials a student receives.
The blind run shows what it can find independently.
The feedback run shows whether it can use mutation testing to improve.
And the teacher-only grading tools let us evaluate both results without revealing the answers to the agent.

The outcome helps us decide what the course needs next.
If Claude can already cover the admission-policy algebra completely, simply adding more mutants may not create a useful challenge for frontier agents.
In that case, the more meaningful escalation may come from the deeper API and OpenAPI work in exercise 11.7.

**The goal isn't to make the exercise artificially difficult. It's to understand what a capable agent can actually do, under conditions we can explain and reproduce.**