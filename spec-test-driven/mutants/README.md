# Mutation testing

A green test run says nothing about the tests you did *not* write. Mutation testing measures that:
take the correct code, inject one small defect (a **mutant**), and run your suite against it. If some
test fails, your suite **killed** the mutant. If everything stays green, the mutant **survived** —
that behaviour is not pinned down by any test. The **mutation score** is the share you kill.

```bash
./gradlew verifyMutants --continue           # run your suite against every mutant
./gradlew verifyMutants -PmutantsStrict      # fail the build while any must-kill mutant survives
./gradlew generateMutants                    # only needed after editing catalog.json
```

`--continue` is there so one red mutant does not hide the rest. You get the report either way —
including when your suite does not compile at all, which it reports as exactly that rather than
burying it under compiler output.

## Reading the report

- **Baseline** comes first: your suite against the *unmutated* code. A test that fails there
  contradicts the correct behaviour, so it proves nothing about a mutant — such tests are excluded
  from the score. Fix them first.
- **KILLED** — some test of yours fails on that mutant. Good.
- **SURVIVED** — no test noticed the change. Work out which test would, and add it.
- **NOT RUN** — your suite did not compile against that mutant.

Each mutant is a module here: it compiles the real `:core` sources with one file replaced by a
mutated copy, so mutants can never drift out of sync with the algebra.

Mutants come in two buckets. **must-kill** mutants break behaviour the specification promises, and
the score counts only these. **spec-dependent** mutants change something the specification never
promised; they are listed separately and their survival can be perfectly correct. So the goal is not
"100% of everything" — it is: every must-kill mutant dead, and a deliberate decision about the rest.
