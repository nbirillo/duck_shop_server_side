# The conformance check

Mutation testing asks whether your suite notices defects. It never asks the opposite question, and a
suite can score perfectly on it by nailing things down that were never nailed down to begin with —
pinning *how* the algebra is written rather than *what* it decides. Tests like that pass today and
break the first time somebody refactors, which is worse than useless: they punish a change that was
allowed.

This is the other half. Each variant here is a **legal rewrite** of `:core` — the behaviour is
unchanged, so a faithful suite stays green on all of them.

```bash
./gradlew verifyVariants --continue          # run your suite against every variant
./gradlew verifyVariants -PmutantsStrict     # fail the build while any false alarm remains
./gradlew generateVariants                   # only needed after editing catalog.json
```

## Reading the report

- **OK** — your suite is indifferent to that rewrite, which is what it should be.
- **FALSE ALARM** — a test of yours fails on code that behaves exactly like `:core`. The report names
  the failing tests, because those are the ones to relax.
- **NOT RUN** — your suite did not compile against that variant.

A false alarm is not a bug in the variant. It means a test asserts something the contract does not
promise, so the honest fix is almost always to weaken or delete that test — not to argue the variant
should have been forbidden.

## What counts as legal

Only the contract decides, and for this tier the contract includes the addendum in
[`../exercises/write-tests/contract-addendum.md`](../exercises/write-tests/contract-addendum.md).
Read it first: it settles two questions the class documentation leaves open, and both of them are
questions a thorough suite is tempted to answer on its own.

The catalog carries no descriptions, deliberately. Saying which behaviour each variant frees would
tell you which tests to drop before you had the chance to work out what the contract actually
promises — and that is the whole exercise.
