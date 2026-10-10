# Verify and Improve AI-Generated Tests

In this exercise, you'll work with tests written by an AI agent.
The admission policies (`KotlinOnly`, `MaxBudget`, `RequiresAccessory`, `MinAccessories`, `AllOf`, `AnyOf`, `Not`) are already implemented in `:core`, and the implementation is correct.
**You don't need to implement anything!**
Instead, an AI agent has already written a test suite in `PolicyTests.kt`.
**Your task is to check whether these tests are correct, find what's missing, and improve them.**
There are three steps in this exercise, and **it's important to follow them in order!**
In the last step, you'll get a report that helps you find missing tests. 
But we want you to try finding these problems yourself first, without any hints.

## Step 1. Check if the tests are correct

First, run the existing tests:

```bash
./gradlew :exercises:write-tests:test
```

At least one test is wrong: it expects behavior that doesn't match the actual implementation.
Your first task is to **find the incorrect test and fix it**.
Once all tests pass, you can move to the next step.
But remember: **passing tests don't mean that the test suite is complete!**
They only mean that the current tests don't contradict the implementation.

## Step 2. Find what's missing

Now it's time to improve the test suite yourself!
Read the specification carefully and think about what the AI agent might have missed.
Are all the policies covered? What about their combinations? Are there any edge cases that should be tested?
You might also notice that the specification doesn't explain some behavior clearly. 
In this exercise, you can make your own decisions about these cases and define how they should work.
**The goal is to make the test suite as complete as possible without any additional tools or hints.**
Don't run mutation testing yet! We'll do that in the next step.
Before moving on, commit your changes or save a copy of your test suite. You'll want to compare your results later.

## Step 3. Check your tests with mutation testing

Now let's see how good your test suite actually is!
We'll use **mutation testing** to check whether your tests can catch bugs.
The idea is simple: we take the correct implementation, create several copies, and introduce one bug into each copy.
Then we run your tests against these modified implementations.
If your tests catch the bug, great! If not, the mutant survives, which means your test suite might be missing something.
Run:

```bash
./gradlew verifyMutants --continue

./gradlew verifyMutants -PmutantsStrict
```

The first command runs the mutation checks and reports the results.
The second command uses strict mode and will fail until all required mutants are caught.

### A few important things to remember

**Not every mutant needs to be caught!**
The report separates mutants into two groups:

- **Must-kill mutants:** These contain bugs that your tests should definitely catch.
- **Spec-dependent mutants:** Whether these are bugs depends on how you interpret the specification.

For the second group, you'll need to decide what behavior should be guaranteed.
You can read more about this in the [Mutation Testing README](../../mutants/README.md).
**Also, catching all mutants doesn't mean your test suite is perfect.**
Mutation testing only checks the bugs we've included. There might still be other cases your tests don't cover.
That's why we asked you to improve the tests yourself in step 2, before running mutation testing.

Once you're done, compare your results:

- Which missing tests did you find yourself?
- Which problems did mutation testing help you discover?
- Did the AI agent miss anything you didn't expect?

The goal isn't just to get a green report, but to understand **how to check whether AI-generated tests are actually good enough**.

---

## Want an extra challenge?

If this exercise felt too easy, try the [advanced version](README-advanced.md)!
There, you'll explore three more questions:

- Can your tests reject implementations that are actually correct?
- Could you check the same behavior with fewer tests?
- Can another AI agent intentionally find a way to pass your tests while breaking the specification?

Good luck! 