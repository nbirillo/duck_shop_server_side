# Teacher-Only Build

This folder contains the teacher materials for the Spec/Test-Driven Development module.
You'll find answer keys, grading tools, exercise notes, hints, and examples here.
**Important: Don't share this folder with students!**

## Why is this a separate project?

Students work in the `spec-test-driven/` folder using an IDE and an AI agent.
And here's the important part: **the AI agent can read everything inside that folder.**
If we put answer keys, hints, or grading details there, the agent could find them and use them to solve the exercises.
That's why we keep all teacher-only materials in a separate project.
The teacher build uses a Gradle composite build:

```kotlin
includeBuild("../spec-test-driven")
```

This lets us use `:core` from the student project when running teacher checks.
But the connection only goes one way: **the teacher build can access the student project, but the student project doesn't know about the teacher build.**

### What should you share with students?

Share only:

- `spec-test-driven/` for the regular exercises.
- `spec-test-driven-capstone/` when students reach the capstone.

These are separate folders, so you can share either one without accidentally including teacher materials.

## Where should I start?

If you're teaching this module, start with [`teacher/README.md`](teacher/README.md).
It's the main guide to the teacher materials, including answer keys, grading instructions, and notes for each exercise.
Here are a few other useful places:

| Start here                                                             | What you'll find                                                                                                                                              |
|------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [`teacher/README.md`](teacher/README.md)                               | **The teacher guide.** Answer keys, grading instructions, exercise notes, and checklists for working with AI agents. Start here!                              |
| [`fixtures/11.4/README.md`](fixtures/11.4/README.md)                   | The specifications written by different AI agents for exercise 11.4. Also used as the 11.4b handout and to check that our grading tools still work correctly. |
| [`teacher/capstone-11.6.md`](teacher/capstone-11.6.md)                 | How to grade the capstone, give feedback on individual results, and trace where the provided materials came from.                                             |
| [`teacher/capstone-11.6-debrief.md`](teacher/capstone-11.6-debrief.md) | A five-part capstone debrief, with evidence behind each result. We intentionally don't use slides for this one.                                               |

## What's inside this project?

There are quite a few folders here, but you probably won't need to open all of them.
Here's what each one is for:

| Directory                          | What it contains                                                                                                                                                                                  |
|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `teacher/`                         | All the written teaching materials: answer keys, instructions, notes, and checklists. See its README for the full index.                                                                          |
| `grading/`                         | The acceptance tests for the admission-policy implementation in `:core`. The test names reveal cases students are supposed to discover in exercise 11.2, so we keep them here.                    |
| `tests/kotlin/`                    | The source code for that test suite.                                                                                                                                                              |
| `reference/`                       | Reference implementations of `priceFor` for exercise 11.4 and `bestOffer` for the capstone. These are possible interpretations of the business descriptions, not necessarily the only valid ones. |
| `mutants/`                         | The grading mutant catalog for exercise 11.2. It includes more mutants than the student practice version, along with explanations and hints that students shouldn't see in advance.               |
| `pricing-mutants/`                 | Mutation testing for exercise 11.4. It checks the reference implementation against the property catalog.                                                                                          |
| `pricing-properties/`              | A collection of possible properties for exercise 11.4 specifications, written as executable checks.                                                                                               |
| `forks/`                           | Nine valid interpretations of four open questions in the capstone, used for grading.                                                                                                              |
| `forks-practice/`                  | The example about ties that you can demonstrate in class. It stays here because it contains a complete implementation of one interpretation.                                                      |
| `tests-11.6/`                      | Tests for important capstone edge cases. They provide a starting point, not a complete grade.                                                                                                     |
| `tests-11.6-validation/`           | A test suite used to check the grading tool itself. It resolves exactly one open question, so we can verify that `verifyForks` reports the expected result.                                       |
| `briefs/`                          | The business descriptions given to students in class. We keep them outside the student project so the AI agent can't simply read them instead of working from the student's specification.        |
| `prompts/`, `prompts-capstone/`    | Prompts used by the course authors for extracting claims, writing and shortening specifications, and combining capstone materials.                                                                |
| `tools/franchise-probe/`           | A collection of 3,010 test cases used to compare capstone implementations. It stays here because its comments reveal the open questions students should discover themselves.                      |
| `fixtures/`                        | Saved materials from our experiments: exercise 11.4 specifications, a hand-written answer key, and capstone franchise specifications.                                                             |
| `implementations/`, `extractions/` | Results produced by AI agents during our experiments. We keep them so we can trace reported results back to the actual files.                                                                     |

## How to run the teacher checks

**All commands in this section are for teachers only.**
Students have their own commands in the exercise READMEs inside `spec-test-driven/`.
Run the following commands from `spec-test-driven-grading/`:

```bash
# Check the given admission-policy implementation
./gradlew :grading:test

# Run the grading mutants against a student's tests (exercise 11.2)
./gradlew verifyMutants --continue

# Run the grading mutants against the teacher's test suite
./gradlew verifyMutants -PmutantTests=tests/kotlin

# Check the pricing mutants against the property catalog (exercise 11.4)
./gradlew verifyPricingMutants --continue

# Check which capstone interpretations a test suite rules out
./gradlew verifyForks --continue
```

For step-by-step grading instructions and additional command options, see the [teacher guide](teacher/README.md).

---

**One last reminder:** Keep the teacher build separate from the student project.
The goal isn't to hide information for its own sake. It's to give students a chance to think through the problems themselves, without their AI agent finding the answers before they do.