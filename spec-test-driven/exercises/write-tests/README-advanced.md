# Verify and Improve AI-Generated Tests — Advanced

Welcome to the advanced version of the testing exercise!
If the basic exercise felt too easy, this one is for you
Maybe your AI agent quickly fixed the incorrect test, added the missing cases, 
and passed all the mutation checks without much trouble.
That's completely fine! The basic exercise is relatively small, and a strong AI agent might already know how to handle most of its edge cases.
So now let's make things a bit more challenging!
**One important note:** These tasks work best with strong AI agents. Smaller local models might not be able to complete some of them, so a clean report doesn't always mean your test suite is good enough.

Before you start, please read [`contract-addendum.md`](contract-addendum.md).
It defines two additional rules that were left open in the basic exercise. 
These rules are important for everything you'll do next.
If you made different decisions about these cases in the basic exercise, that's fine! But from now on, your tests need to follow the updated specification.

---

## 1. Can your tests reject correct implementations?

In the basic exercise, we used mutation testing to check whether your tests could catch bugs.
But there's another important question: **can your tests fail even when the implementation is correct?**
This can happen when tests check implementation details that aren't actually required by the specification.
For example, you might write a test that expects policies to be evaluated in a specific order. 
But if the specification doesn't require that order, a different implementation should still be considered correct.
These tests can become a problem when someone tries to refactor the code.

### Let's check!

We prepared several alternative implementations of `:core`.
They work differently internally, but all of them follow the specification.
You can find them in `../../variants/catalog.json`.
**Your tests should pass on all of these implementations.**
Run:

```bash
./gradlew verifyVariants --continue
./gradlew verifyVariants -PmutantsStrict
```

The first command runs the checks and shows the results.
The second command fails if any false alarms remain.
If your tests fail on a valid implementation, the report will show a **FALSE ALARM** and tell you which tests caused it.
You can read more about the report in the [Variants README](../../variants/README.md).
**Important:** If a variant is correct but your test fails, you need to change the test, not the implementation.

### An extra experiment

Let's compare two ways of creating a test suite with an AI agent.

- **Run A:** Ask your agent to write tests from scratch using `../../core/…/Leaves.kt` and `Combinators.kt`.
- **Run B:** Give your agent the existing `src/test/kotlin/…/PolicyTests.kt` and ask it to fix incorrect tests and add missing ones, just like in the basic exercise.

Then run the conformance checks on both suites:

```bash
./gradlew verifyVariants --continue -PmutantTests=path/to/your/src/test/kotlin
```

Before running the checks, try to predict the results.
**Which approach do you think is more likely to produce tests that reject correct implementations? And why?**
Compare your prediction with the actual results. You might be surprised!

---

## 2. Can you write fewer tests?

In the basic exercise, our goal was to catch as many bugs as possible.
But mutation testing doesn't care how many tests you write.
You could have 10 tests or 50 tests, and both suites might catch exactly the same bugs.
In real projects, though, we need to read, review, and maintain these tests.
**So the goal here is to keep good coverage while reducing the number of tests.**

### Your task

Look at the results from the basic exercise and decide how many tests your suite should have.
You need to choose your own test budget and explain why you chose that number.

Try to find a balance:

- If your budget is too small, you might only cover the bugs you already know about.
- If your budget is too large, it won't really help you improve the test suite.

Then run:

```bash
./gradlew verifyMutants --continue -PtestBudget=<your number>
```

The command runs the mutation checks and shows the full report.
If your test suite has more tests than your budget allows, the check will fail.

### How can you reduce the number of tests?

One useful approach is **parameterized testing**.
Instead of writing a separate test for every input, you can use one test that runs on several different inputs.
For example, instead of testing each possible number of policies separately, you can create a set of inputs and check the same behavior for all of them.
You can also test different positions and combinations of policies in the same way.
The idea is simple: **try to cover different types of behavior, not just add more and more individual test cases.**
And remember, fewer tests aren't automatically better. We still want to catch important bugs!

---

## 3. Can another AI agent find a bug your tests missed?

Now let's try something different!
So far, you've been using an AI agent to help write and improve tests.
But what if we ask another agent to find problems in them?
**The idea is to let one AI agent check the work of another.**
Different agents might notice different problems, so this can be a useful way to get a second opinion.
For this exercise, you'll need a strong AI agent. 
Ideally, it should be at least as capable as the one that helped you write the tests.
Otherwise, if it doesn't find anything, we won't really know whether your tests are good or the agent simply wasn't able to find a problem.

### Your task

Ask another AI agent to write an implementation that passes your tests but doesn't follow the specification.
For example, you can use this prompt:

> Write an implementation that passes all tests in this test suite but breaks the specification. Try to find a realistic bug that the tests don't catch. If you can't find one, explain why and point to the tests that prevent it.

Save the implementation in a new folder under `attacks/`.
Use a different folder for each attempt.
Then run:

```bash
# Example: attacks/my-attack/src/main/kotlin/…/Leaves.kt

./gradlew prepareAttack -Pagent=my-attack
./gradlew verifyAttack -Pagent=my-attack --continue
```

### How does the check work?

We compare the agent's implementation with the correct implementation.
Both are run on the same **20,000 randomly generated inputs**, using a fixed seed so the results can be reproduced.
We can't run the two implementations together because they define the same classes in the same packages. 
Instead, we run them separately and compare their results.
Here's how to understand the report:

| Result                                | What it means                                                                                                  |
|---------------------------------------|----------------------------------------------------------------------------------------------------------------|
| Your tests fail                       | Great! Your tests caught the bug.                                                                              |
| Tests pass, no differences found      | The implementation passed your tests and behaved like the correct one on the checked inputs. No bug was found. |
| Tests pass, but results are different | Your tests missed a bug! The report will show the first input where the implementations disagree.              |

### Who should come up with the bug?

You have two options.

**Option 1: Let the agent decide.**

Ask it to find a problem in your test suite and write an implementation that demonstrates it.
This is a good way to see what kinds of problems the agent can find on its own.

**Option 2: Find the problem yourself.**

Think about a realistic bug your tests might miss, and ask the agent to implement it.
This is harder, but also a very useful exercise!
You'll need to understand your tests well enough to find a case they don't cover.

**One important rule:** Try to find realistic bugs.
For example, a mistake in how `AllOf` handles an empty list is interesting because someone could actually make that mistake.
But a special case like "return the wrong answer only when the price is exactly 7" isn't very useful.
You can always invent a random exception that a finite test suite won't catch. That's not what we're trying to learn here.

### Try several rounds!

Once you find a bug your tests missed:

1. Understand why your tests didn't catch it.
2. Improve the test suite.
3. Ask the agent to try again.
4. Keep track of how many rounds you complete.

After each round, think about whether your new tests cover a whole group of similar bugs or just the one example the agent found.
**And don't forget to stop at some point!**
A strong agent might always be able to invent another unusual case that your tests don't cover.
But our goal isn't to test every possible input.
The goal is to catch realistic mistakes and build a test suite that we can trust.
Once the agent starts finding only very artificial bugs that nobody would realistically make, it's probably a good time to stop.

---

## What should you learn from this exercise?

By the end, you should have a better understanding of three things:

- **Correctness:** Good tests should catch bugs without rejecting valid implementations.
- **Efficiency:** More tests don't always mean better tests.
- **Verification:** Even a test suite that looks complete can have problems, and another AI agent might help you find them.

The main idea is the same as in the basic exercise: **don't just trust AI-generated tests because they pass. Check what they actually test, what they miss, and whether they follow the specification.**