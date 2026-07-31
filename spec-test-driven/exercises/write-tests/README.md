# Exercise 11.2 — Verify & harden an AI's tests

The admission-policy algebra (`KotlinOnly`, `MaxBudget`, `RequiresAccessory`, `MinAccessories`,
`AllOf`, `AnyOf`, `Not`) is **given and correct** in `:core`. You are NOT implementing it.

An AI wrote the test suite in `PolicyTests.kt`. Your job is to **verify and harden** it — the
skill this whole module teaches.

## 1. Fix the wrong test

One test asserts the wrong thing and fails against the correct code. Run it, find it, fix it:

```bash
./gradlew :exercises:write-tests:test
```

A green run means no test contradicts the real behaviour. (It does NOT yet mean your suite is
*complete* — see below.)

## 2. Add the missing cases

The AI covered the happy path but skipped the cases where implementations actually slip. Add
tests for at least:

- `MaxBudget` **exactly at the limit** (a duck priced == the limit).
- `RequiresAccessory` on a duck with **no accessories at all** (empty list).
- `MinAccessories` **exactly at the minimum**.
- `AllOf` and `AnyOf` over an **empty list** — what should each return?
- `Not` truly inverting (beyond the one test above).

Check your suite actually catches broken implementations:

```bash
./gradlew verifyMutants --continue
```

This is **mutation testing**: your tests are run against copies of the algebra with one defect each,
and every defect your suite does not notice is reported as a surviving mutant. Read
[`../../mutants/README.md`](../../mutants/README.md) for what the report means.

## 3. The spec is silent on two things — decide and pin them

The behaviour below is **not stated** anywhere. That is on purpose: part of verification is
noticing what a spec leaves open, deciding the intended contract, and pinning it with a test (or
flagging the ambiguity so a human resolves it).

- **`MaxBudget` boundary.** Is the limit inclusive (`price <= limit`) or exclusive (`price <
  limit`)? The prose never says. Decide, and write a test that pins your choice.
- **`AllOf` / `AnyOf` aliasing.** They keep the `List` you pass in. If you mutate that list *after*
  building the policy, should the policy's behaviour change? Is that part of the contract, or an
  accident of implementation? Decide, and either pin it with a test or flag it.

## 4. Mutation testing — required if a strong agent wrote your suite

Steps 1–3 leave an uncomfortable question open: your suite is green and catches the practice
defects, but *what else* would it miss? Mutation testing answers that.

```bash
./gradlew verifyMutants --continue          # the practice set, with hints for survivors
./gradlew verifyMutants -PmutantsStrict     # same, but fails until every must-kill mutant dies
```

Work until the report says every must-kill mutant is dead, and you have a deliberate answer for each
spec-dependent one (see [`../../mutants/README.md`](../../mutants/README.md) — some mutants are
*supposed* to survive).

**Who has to do this step.** If you hardened the suite with a frontier agent (Claude Code, Junie, a
large hosted model), treat this step as **mandatory**: such an agent usually clears steps 1–3 on the
first try, and mutation testing is what keeps the exercise honest for you. If you worked with a
small local model, steps 1–3 are already the hard part — mutation testing is then an **optional**
extra, and a good way to see how much your own additions improved the suite.

Two things worth knowing before you read your score:

- The report first checks your suite against the **unmutated** code. A test that fails there
  contradicts the correct behaviour, so it proves nothing about a mutant — those tests are excluded
  from the score, which is why step 1 comes first.
- A perfect score on the practice set is not the same as a thorough suite. The graded set your
  teacher runs contains defects this one does not, and even the authored acceptance suite of this
  module does not kill all of them.
