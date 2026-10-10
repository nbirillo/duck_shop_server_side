# Spec/Test-Driven Development with AI in Kotlin

Welcome to the **Spec- and Test-Driven Development with AI in Kotlin** module!
This module is based on the [Duck Shop server-side course](https://github.com/nbirillo/duck_shop_server_side).
If you want to build the Duck Shop project from scratch, you can start with that course. 
But you don't have to! This module is self-contained, so you can also start directly from here.

## What will you learn?

AI agents are getting better and better at writing code. But how do we know if the code they write is actually correct?
That's exactly what we'll focus on in this module!

**You'll learn how to write specifications and tests, let an AI agent implement the feature, and then check if the result is correct.**

The main goal isn't to practise writing Kotlin code. Instead, we'll focus on how to describe what we want, decide what correct behavior means, and verify that an implementation actually does what we expect.
You can use any AI agent you like, either local or hosted. We've tested the exercises with five local models and one frontier model, and designed the module to be **agent-agnostic**.
So you don't need to learn how to use one specific agent. The ideas you'll learn here should work with different tools!

## Before you start

First, make sure everything works on your machine. Run:

```bash
cd spec-test-driven
./gradlew :core:test
```

All tests should pass before you start the exercises.
If something fails, check your setup first. All exercises depend on `:core`, so it's important to make sure it works.

## Running the app

You don't actually need to run the server for this module. All exercises are checked using tests.
But if you want to start the Duck Shop server, you can use the **Duck Shop server (bootRun)** run configuration in your IDE (available in `.run/`).
Or run it manually:

```bash
cd ../server
./gradlew bootRun
```

Note that `server/` sits next to this folder in the main course repository. If you only have `spec-test-driven/`, it won't be there.

## One important rule

**Don't edit `core/`!**
This part of the project is already implemented and correct. You'll use it to check your tests and specifications.
All your work should be done in `exercises/`.

## Where should you start?

The module has several exercises. Each one has its own README with instructions, commands, and information about what you need to submit.
Here's a quick overview:

### Main exercises

| Exercise                                                                  | What you'll do                                                                                                                                                                             |
|---------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [Write Tests](exercises/write-tests/README.md)                            | An AI agent has written tests for a given specification. Your task is to check whether those tests are good enough and improve them if needed.                                             |
| [Write Tests — Advanced](exercises/write-tests/README-advanced.md)        | A harder version of the first exercise. You'll check whether your tests reject valid implementations, whether you can simplify them, and whether another agent can find a way around them. |
| [Write Specification](exercises/write-spec/README.md)                     | This time, you'll write the specification yourself! The goal is to make it clear enough that two different implementations following it will behave the same way.                          |
| [Write Specification — Advanced](exercises/write-spec/README-advanced.md) | A more difficult specification task, where you'll need to think about how different rules work together.                                                                                   |
| **Capstone project**                                                      | You'll get this task separately. Here, you'll put everything together and work with an existing project, specification, and tests.                                                         |

### Additional materials

You don't need to read everything right away, but these files might be useful when you're working on the exercises.

| File                                                            | What it's about                                                                                      |
|-----------------------------------------------------------------|------------------------------------------------------------------------------------------------------|
| [Mutation Testing](mutants/README.md)                           | Explains how mutation testing works, where the mutants come from, and how to understand the results. |
| [Variants](variants/README.md)                                  | Explains how we check that your tests also pass on other correct implementations.                    |
| [Contract Addendum](exercises/write-tests/contract-addendum.md) | Two additional rules for the advanced testing exercises.                                             |

## Project structure

Here's what you'll find in the repository:

| Directory         | What's inside                                                                                                                                                                                                                                            |
|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `core/`           | The given implementation and types. Includes `AdmissionPolicy` with its four basic rules and three combinators, `Duck`, `Accessory`, `Shop`, and the types used in later exercises: `DiscountRule`, `Franchise`, and `Offer`. Don't edit this directory! |
| `exercises/`      | Your main working directory. Contains the exercises for writing tests and specifications.                                                                                                                                                                |
| `mutants/`        | Copies of `:core`, each with one intentional bug. You'll use them to check whether your tests can catch incorrect behavior.                                                                                                                              |
| `variants/`       | Different implementations of `:core` that should behave exactly like the original. Your tests should pass on all of them.                                                                                                                                |
| `attacks/`        | Used in advanced exercises. Contains implementations that try to pass your tests while breaking the specification. `demo-prefix/` is an example, and `reference/` contains the original implementation used for comparison.                              |
| `tools/`          | Prompts for AI agents and tools used to compare implementations and check results.                                                                                                                                                                       |
| `sandbox/<name>/` | Created when you run the `sandbox` task. Contains your specification and the required types, but no implementation. This is where your AI agent will work. The directory is generated automatically and ignored by Git.                                  |
| `build-logic/`    | Gradle tasks used by the exercises. You don't need to change anything here.                                                                                                                                                                              |

That's it! You can now start with the [first exercise](exercises/write-tests/README.md).
Good luck, and remember: **the goal isn't just to make the tests pass, but to understand what you're testing and why!**

## Where Are the Answers?

You may have noticed that there are no answer keys in this folder.
**That's intentional.**
The reference implementations, graded mutants, answer keys, and other materials that reveal the solutions live in a separate folder alongside this one:
`spec-test-driven-grading/`

There's a simple reason for this.
You'll be working on these exercises with an AI agent. That agent can read files in its workspace. If we kept the answers alongside the exercises, the agent could find them — and you wouldn't know whether it solved the problem or simply found the solution.

The separation works in one direction:

- The teacher project can use code from the student project.
- The student project doesn't depend on or reference the teacher project.

### Can you look at the answers anyway?

Yes. Both folders are part of the same public repository, so nothing is actually locked away.
But there's an important trade-off.
**The checks are only meaningful if you and your agent haven't seen the answers beforehand.**
Otherwise, a passing result won't tell you whether your specification was clear, your tests were strong, or your agent genuinely figured something out.
We can't enforce that boundary for you.
**Keeping the experiment honest is part of the exercise.**