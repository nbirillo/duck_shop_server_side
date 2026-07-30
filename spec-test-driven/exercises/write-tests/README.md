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
./gradlew practiceCatch
```

## 3. The spec is silent on two things — decide and pin them

The behaviour below is **not stated** anywhere. That is on purpose: part of verification is
noticing what a spec leaves open, deciding the intended contract, and pinning it with a test (or
flagging the ambiguity so a human resolves it).

- **`MaxBudget` boundary.** Is the limit inclusive (`price <= limit`) or exclusive (`price <
  limit`)? The prose never says. Decide, and write a test that pins your choice.
- **`AllOf` / `AnyOf` aliasing.** They keep the `List` you pass in. If you mutate that list *after*
  building the policy, should the policy's behaviour change? Is that part of the contract, or an
  accident of implementation? Decide, and either pin it with a test or flag it.
