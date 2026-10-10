# Conformance Testing

In mutation testing, we check whether your tests can catch bugs.
But there's another important question:
**Can your tests fail even when the implementation is correct?**

This might sound strange, but it happens!
Sometimes tests check details that aren't actually required by the specification.
For example, a test might expect policies to be evaluated in a particular order, even though the specification doesn't require that.
Such a test might pass today but fail after someone refactors the code, even if the new implementation is still correct.
**We don't want our tests to reject valid implementations!**
That's what conformance testing helps us check.

## How does conformance testing work?

The idea is similar to mutation testing, but this time we're looking for the opposite result.
Instead of introducing bugs, we create different implementations that still follow the specification.
We call these implementations **variants**.
Each variant is a valid alternative implementation of `:core`.
Your tests should pass on all of them!
If a test fails, it means your test suite might be checking something that the specification doesn't actually guarantee.

## How to run conformance testing

Use these commands:

```bash
./gradlew verifyVariants --continue          # Run your tests against every variant
./gradlew verifyVariants -PmutantsStrict     # Fail if any false alarm remains
./gradlew generateVariants                   # Only needed after editing catalog.json
```

We use `--continue` so that one failing check doesn't stop the others.
This way, you can see the full report.

## How to read the report

You'll see one of these results for each variant:

| Result          | What it means                                                     |
|-----------------|-------------------------------------------------------------------|
| **OK**          | Your tests pass on this valid implementation. Great!              |
| **FALSE ALARM** | One or more tests fail even though the implementation is correct. |
| **NOT RUN**     | Your test suite didn't compile against this variant.              |

If you get a **FALSE ALARM**, the report will show which tests failed.
These are the tests you need to look at!
Usually, the problem is that a test requires behavior that the specification doesn't promise.
For example, it might check the order of function calls or some other implementation detail.
**The solution is usually to change or remove the test, not to change the variant.**
Remember, the variant is already a valid implementation. Our goal is to make sure the test suite accepts it.

## What makes a variant correct?

The specification decides which implementations are valid.
For this advanced exercise, the specification also includes the additional rules in [`contract-addendum.md`](../exercises/write-tests/contract-addendum.md).
**Please read that file before running these checks!**
It explains two behaviors that were left open in the basic exercise.
These are exactly the kinds of details that are easy to test by accident, even when the specification doesn't guarantee them.

### Why doesn't the catalog explain each variant?

You might notice that the variant catalog doesn't describe what each variant changes.
That's intentional!
If we told you exactly what was different, you'd immediately know which tests to change.
But the goal of this exercise is to **figure out what the specification actually guarantees**.
So when you see a false alarm, read the failing test, compare it with the contract, and ask yourself:
**Does the specification really promise this behavior, or did my test assume too much?**
That's the skill we're practicing here.

---

Mutation testing and conformance testing work together:

- **Mutation testing** checks whether your tests catch incorrect implementations.
- **Conformance testing** checks whether your tests accept correct implementations.

A good test suite should do both!