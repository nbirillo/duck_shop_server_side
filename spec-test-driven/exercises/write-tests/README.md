# Exercise 11.2 — Verify & harden an AI's tests

The admission-policy algebra (`KotlinOnly`, `MaxBudget`, `RequiresAccessory`, `MinAccessories`,
`AllOf`, `AnyOf`, `Not`) is **given and correct** in `:core`. You are NOT implementing it.

An AI wrote the test suite in `PolicyTests.kt`. Your job is to **verify and harden** it.

1. **Make it truthful.** At least one test contradicts the real behaviour. Find it and fix it.

   ```bash
   ./gradlew :exercises:write-tests:test
   ```

   Green means no test contradicts the code. It does not mean the suite is complete.

2. **Make it complete.** Add the cases the AI skipped, and pin down anything the specification
   leaves open — deciding what the contract *should* be is part of the job.

3. **Check it with mutation testing.** Your tests are run against copies of the algebra that each
   contain one defect. Every defect your suite fails to notice is reported as a surviving mutant.

   ```bash
   ./gradlew verifyMutants --continue
   ./gradlew verifyMutants -PmutantsStrict     # fails until every must-kill mutant is dead
   ```

   Work until nothing survives. See [`../../mutants/README.md`](../../mutants/README.md) for how to
   read the report.
