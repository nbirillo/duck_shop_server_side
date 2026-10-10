# Mutation Testing

Your tests are passing. Great!
But does that mean you've tested everything important?
Not necessarily!
**Passing tests only tell you that the cases you've checked work correctly.** They don't tell you what you've missed.
That's where **mutation testing** comes in!

## How does mutation testing work?

The idea is simple:

1. Take the correct implementation.
2. Introduce one small bug. This modified version is called a **mutant**.
3. Run your tests against the mutant.
4. Check whether your tests catch the bug.

There are two possible results:

- **KILLED:** At least one test fails. Great! Your tests caught the bug.
- **SURVIVED:** All tests still pass. Your test suite didn't notice the change.

The **mutation score** tells you what percentage of the relevant mutants your tests caught.
It's a useful way to find missing tests, but remember: **a high mutation score doesn't automatically mean your test suite is complete.**

## How to run mutation testing

Use these commands:

```bash
./gradlew verifyMutants --continue           # Run your tests against every mutant
./gradlew verifyMutants -PmutantsStrict      # Fail if any must-kill mutant survives
./gradlew generateMutants                    # Only needed after editing catalog.json
```

We use `--continue` so that one failing check doesn't stop the others.
This way, you can see the full report instead of fixing one problem at a time.
You'll also get a clear message if your test suite doesn't compile, rather than having to figure that out from a long compiler error.

## How to read the report

The report starts with a **baseline check**.
This runs your tests against the original, correct implementation, without any mutations.
If a test fails here, it means the test expects behavior that doesn't match the correct implementation.
**Fix these tests first!** Otherwise, they can't tell us whether a mutant contains a bug. 
Tests that fail on the baseline are excluded from the mutation score.
After that, you'll see the results for each mutant:

| Result       | What it means                                                              |
|--------------|----------------------------------------------------------------------------|
| **KILLED**   | At least one of your tests caught the bug. Great!                          |
| **SURVIVED** | Your tests didn't notice the change. Think about what test could catch it. |
| **NOT RUN**  | Your test suite didn't compile against this mutant.                        |

Each mutant is built using the real `:core` source files, with just one file replaced by a modified version.
This helps keep the mutants consistent with the original implementation.

## Do you need to kill every mutant?

**No! And this is important.**
There are two types of mutants in this exercise:

**1. Must-kill mutants**

These mutants break behavior that the specification guarantees.
Your tests should catch all of them.
Only these mutants count toward your mutation score.

**2. Spec-dependent mutants**

These mutants change behavior that the specification doesn't clearly define.
Whether they are actually incorrect depends on the decisions you've made about the contract.
Sometimes a spec-dependent mutant should be caught. Sometimes it's completely fine for it to survive.
Your job is to understand the difference!

**The goal isn't to kill 100% of all mutants.**
The goal is to catch every bug that breaks the specification, while making deliberate decisions about behavior the specification leaves open.
And remember: mutation testing helps you find gaps, but it doesn't replace thinking about what your tests should actually check.