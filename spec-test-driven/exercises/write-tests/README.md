# Verify and harden an AI's tests

The admission-policy algebra (`KotlinOnly`, `MaxBudget`, `RequiresAccessory`, `MinAccessories`,
`AllOf`, `AnyOf`, `Not`) is **given and correct** in `:core`. You are NOT implementing it.

An AI wrote the test suite in `PolicyTests.kt`. Your job is to **verify and harden** it.

Do the steps in order. Step 3 gives you a checklist, and once you have seen a checklist it is very
hard to think past it — so steps 1 and 2 have to happen first, without it.

1. **Make it truthful.** At least one test contradicts the real behaviour. Find it and fix it.

   ```bash
   ./gradlew :exercises:write-tests:test
   ```

   Green means no test contradicts the code. It does not mean the suite is complete.

2. **Make it complete — on your own.** Add the cases the AI skipped, and pin down anything the
   specification leaves open; deciding what the contract *should* be is part of the job. Do not run
   the mutation testing from step 3 yet, and commit (or copy aside) the suite you end up with — you
   will want to compare.

3. **Only now, check it with mutation testing.** Your tests are run against copies of the algebra
   that each contain one defect. Every defect your suite fails to notice is reported as a surviving
   mutant.

   ```bash
   ./gradlew verifyMutants --continue
   ./gradlew verifyMutants -PmutantsStrict     # fails until every must-kill mutant is dead
   ```

   Two things to keep in mind while you close the gaps:

   - Not every mutant is supposed to die. The report separates **must-kill** mutants from
     **spec-dependent** ones, whose survival can be the right answer — decide, don't guess. See
     [`../../mutants/README.md`](../../mutants/README.md).
   - A clean report means "nothing the mutants know about is missing", not "the suite is complete".
     The mutants are a sample of the ways an implementation can be wrong, not the definition of a
     good suite. This is also why step 2 came first: it is worth noticing which gaps you found by
     thinking about the algebra, and which ones only the report pointed at.

---

Done, and it barely put up a fight? Your agent may simply be stronger than this task.
[`README-advanced.md`](README-advanced.md) asks three harder questions — whether your suite forbids
changes that were **allowed**, whether it could have said the same in **fewer** tests, and whether
another agent can get past it **on purpose**.
