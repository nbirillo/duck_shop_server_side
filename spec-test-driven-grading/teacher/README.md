# Teacher Pack

Welcome to the teacher materials for the Spec/Test-Driven Development module!
This folder contains everything you need to teach and grade the exercises: answer keys, hints, grading instructions, experiment results, and checklists for working with AI agents.
**Please don't share this folder with students!**

## Why is this folder separate?

Students work in `spec-test-driven/` using an IDE and an AI agent.
The important thing is that **the AI agent can read files inside the project**.
If we put answer keys, hints, or reference implementations there, the agent could find them and solve the exercises without the student doing the intended work.
That's why the student project contains only the exercise instructions and commands, while the teacher materials live here.

## Where should I start?

Here's a guide to the files in this folder.

| File                                                                 | What you'll find                                                                                                                                                                     |
|----------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [`write-tests-answer-key.md`](write-tests-answer-key.md)         | **The test-writing exercise:** The intentionally incorrect test, missing cases, two unclear parts of the specification, and grading instructions.                                                |
| [`mutation-testing-notes.md`](mutation-testing-notes.md)             | How mutation testing works in this course, the grading mutant catalog, why some mutants are allowed to survive, and measured results.                                                |
| [`../fixtures/write-spec/`](../fixtures/write-spec/)                             | Specifications written by different AI agents for the spec-writing exercise. Used as the compression exercise handout, checker benchmarks, and regression tests.                                                |
| [`write-spec-calibration.md`](write-spec-calibration.md)       | **The spec-writing exercise:** Ambiguities in the business description, results from different agents, problems with our first consistency checker, and verified laws for the advanced exercise. |
| [`advanced-tier-notes.md`](advanced-tier-notes.md)                   | **The test-writing exercise advanced:** The additional contract rules, valid implementation variants, test budget, and adversarial testing.                                                      |
| [`claude-run-checklist.md`](claude-run-checklist.md)                 | Step-by-step instructions for manually running the Claude Code experiment for the test-writing exercise, including what to clean up before starting.                                             |
| [`claude-attack-checklist.md`](claude-attack-checklist.md)           | Step-by-step instructions for testing a frontier agent's ability to find weaknesses in a test suite, including prompts E and F.                                                      |
| [`write-spec-answer-key.md`](write-spec-answer-key.md)         | **The spec-writing exercise:** The property catalog, the manually written answer key, and how students can challenge a grading result using `verifyClaims`.                                      |
| [`write-spec-corpus-results.md`](write-spec-corpus-results.md) | Results from evaluating 13 specifications in four different ways, including the evidence used in the slides and two corrections to earlier conclusions.                              |
| [`capstone.md`](capstone.md)                               | **The capstone:** Why the capstone uses a separate project, what gets graded, how to explain each result, and where the provided materials came from.                               |
| [`capstone-debrief.md`](capstone-debrief.md)               | A five-part capstone debrief, with evidence behind every number. We intentionally don't use slides for this part.                                                                    |
| [`claude-implement-checklist.md`](claude-implement-checklist.md)     | Instructions for using a frontier agent to implement code from a specification alone (the spec-writing exercise, step 3e).                                                                       |
| [`running-api-agents.md`](running-api-agents.md)                     | How to use `runAgent`, including its different modes, command options, and difficulty calibration.                                                                                   |
| [`running-advanced-agents.md`](running-advanced-agents.md)           | How to work with strong interactive agents such as Claude Code or Junie without using an API key, including example prompts.                                                         |

Some explanations from these notes were also used to prepare the course slides.
**The slides are the main reference for what students are taught.**
If a command or explanation here disagrees with the slides, follow the slides. It means these notes need updating.
The detailed commands and answer keys stay here, close to the code they refer to.

## Reference implementation and grading

All commands in this section are for teachers.
Run them from `spec-test-driven-grading/`:

```bash
# Run the acceptance tests for the given admission-policy implementation
./gradlew :grading:test

# Run the grading mutants against a student's test suite (the test-writing exercise)
./gradlew verifyMutants --continue

# Run the grading mutants against the teacher's test suite
./gradlew verifyMutants -PmutantTests=tests/kotlin
```

### How does the teacher build access the student code?

The teacher project uses a Gradle composite build:

```kotlin
includeBuild("../spec-test-driven")
```

This allows the teacher build to use `:core` from the student project.
But the connection only goes one way: **the teacher build can access the student project, but the student project doesn't depend on the teacher build.**
So students can build and run their exercises without needing the grading project.

### What should I share with students?

Share only:

- `spec-test-driven/` for the main exercises.
- `spec-test-driven-capstone/` when students reach the capstone.

If students clone the entire repository, the teacher build will still be there.
So this separation isn't a security feature. It only keeps the answers out of the student's working project when you distribute the folders separately.
The [build-root README](../README.md) explains this as well.

## What stays in the student project?

Not everything related to grading is hidden from students.
Some tools need to be available so students can run checks and get feedback while working on the exercises.
Here's what we keep in `spec-test-driven/`:

| File or directory                            | Why it's included                                                                                                                                                    |
|----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `exercises/write-tests/README.md`            | The exercise instructions and commands, without revealing the missing test cases or unclear parts of the specification.                                              |
| `mutants/README.md`                          | Explains how to run mutation testing and understand the results.                                                                                                     |
| `mutants/catalog.json` and generated mutants | The practice mutants students use to check their tests. Unlike the teacher catalog, these don't include `what` or `hint` explanations that would reveal the answers. |
| `tools/agent-prompt*.md`                     | Prompts used by `runAgent` to tell agents what to produce and how to format the results. They don't contain exercise answers.                                        |

## What are the limitations?

There are two things worth keeping in mind.

**1. Students can inspect the practice mutants.**

Because mutation testing runs locally, students can open a mutant's source code and see exactly what changed.
That's fine! It's part of the trade-off we make to let students run their own checks.

**2. Separate folders only help if we distribute them separately.**

If students receive the entire repository, they can also access the teacher materials.
So when preparing the course, make sure you share only the student folders.

---

The goal of this setup is simple: **give students the tools they need to learn and check their work, without giving their AI agents the answers in advance.**