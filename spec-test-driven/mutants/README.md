# Mutation testing

A green test run tells you nothing about the tests you did *not* write. Mutation testing measures
that directly: take the correct code, inject one small defect (a **mutant**), and run your suite
against it. If some test fails, your suite **killed** the mutant. If everything stays green, the
mutant **survived** — that behaviour is not pinned down by any test.

The **mutation score** is the share of mutants your suite kills. It is the closest thing we have to
"how much would my tests actually catch", and it is exactly the question to ask about a test suite an
AI wrote for you.

## Running it

```bash
./gradlew generateMutants          # only after editing catalog.json (the modules are committed)
./gradlew verifyMutants --continue # runs your suite against every mutant and prints the score
```

Each mutant module compiles the real `:core` sources with one file swapped for a mutated copy, so a
mutant can never drift out of sync with the algebra. Open `mutants/<id>/src/main/...` to see the
injected defect — for this practice set it is meant to be readable.

Useful flags:

- `-PmutantTests=learner` (default) — run the suite in `exercises/write-tests`.
- `-PmutantTests=authored` — run the module's own acceptance suite instead.
- `-PmutantTests=<path>` — any test source dir relative to this folder (e.g. an archived
  `hardened/<agent>/src/test/kotlin`).
- `-PmutantsStrict` — fail the build unless every must-kill mutant dies.

Use `--continue` so that one mutant your suite cannot compile against does not hide the rest.

## Not every mutant should die

Two buckets appear in the report.

**Must-kill** mutants break behaviour the specification promises. A faithful suite kills all of them;
a survivor is a real gap in your tests.

**Spec-dependent** mutants change something the specification never promised. Their survival is
acceptable, and killing them may even mean your tests *over-specify* the code — pinning an
implementation detail that a future refactor is allowed to change. Two examples from this algebra:

- **Short-circuiting.** `AllOf` uses `all { … }`, which stops at the first policy that says no. A
  mutant that evaluates every policy first and only then combines the results behaves identically —
  the only way to tell them apart is a spy policy that counts its own calls. Nothing in the
  specification promises short-circuiting, so a suite that does not kill this mutant is fine, and one
  that does kill it has decided that short-circuiting is part of the contract. That is a legitimate
  choice — but it must be a *choice*, not an accident.
- **Aliasing.** `AllOf`/`AnyOf` keep the `List` you hand them. A mutant that stores a defensive copy
  behaves differently only if you mutate that list *after* building the policy. The specification is
  silent, so both are defensible; the exercise asks you to notice the ambiguity, decide, and either
  pin your decision with a test or flag it for a human.

So the target is not "100% of all mutants". It is: **every must-kill mutant dead, and a deliberate
answer for each spec-dependent one.**
