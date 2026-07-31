# Checklist — measuring Claude Code on exercise 11.2

Step-by-step for the run that has to be done by hand, because there is no Anthropic API key. The
prompts themselves are in [`running-advanced-agents.md`](running-advanced-agents.md); this file is the
operational order of events, including the two things that silently invalidate the measurement.

## 0. Clear the folder of author-side answers (do this first)

`spec-test-driven/` contains, from our own harness runs:

- `test-suites/claude-code/` — an 86-test suite that already scores 11/11 on the graded mutants. A
  perfect answer, sitting in the folder.
- `test-suites/ollama-*` and `hardened/ollama-*` — other models' suites for the same algebra.

An agent that reads any of these is not measuring anything. Move them out of the folder before the
run and back afterwards:

```bash
cd <repo>/spec-test-driven
mkdir -p ../.author-artifacts
mv test-suites hardened ../.author-artifacts/          # before the run
# ... run Claude ...
mv ../.author-artifacts/test-suites ../.author-artifacts/hardened .   # after
```

`solutions/` can stay: those are opening-schedule implementations, unrelated to the policy tests.

Also confirm the exercise starts from its committed, flawed state — a previous run may have left it
hardened:

```bash
git status --short exercises        # must print nothing
git restore -- exercises            # if it printed something
```

## 1. Open the right folder

Open **`spec-test-driven/` itself** as the project in Claude Code — not the repository root. From that
root the agent cannot reach `spec-test-driven-grading/`, which holds the reference implementation, the
graded mutants, the answer key and this file.

One more thing to check once: if you keep a global `~/.claude/CLAUDE.md` with notes about this module,
the session will load it and may hand the agent exactly what we are trying to hide.

## 2. Run A — blind (this is the comparable number)

Paste Prompt C from `running-advanced-agents.md`. It asks the agent to fix the wrong test and add the
missing cases, and explicitly forbids `mutants/` and `verifyMutants`. This measures what the agent
covers **unaided** and is therefore comparable with the `runAgent -Pmode=verify-harden` numbers for the
local models.

Note the deliberate conflict: `exercises/write-tests/README.md` tells the learner to run mutation
testing as step 3, and the prompt overrides that. If the agent runs `verifyMutants` anyway, say so — a
frontier agent ignoring an explicit constraint is itself a finding worth recording.

When it stops, tell me it is done and which run it was; I archive, score and restore. Manually it is:

```bash
mkdir -p hardened/claude-code-promptC/src/test/kotlin/org/jetbrains/kotlin/course/duck/shop/admission
cp exercises/write-tests/src/test/kotlin/org/jetbrains/kotlin/course/duck/shop/admission/PolicyTests.kt \
   hardened/claude-code-promptC/src/test/kotlin/org/jetbrains/kotlin/course/duck/shop/admission/
git restore -- exercises
```

The archive folder also needs a `build.gradle.kts`; copy one from any `hardened/ollama-*`.

## 3. Run B — with mutation-testing feedback

Start again from the restored, flawed exercise, in a **fresh** Claude session (an existing session
already knows what it wrote). Paste Prompt D: same task, but the agent may run
`./gradlew verifyMutants --continue` and iterate until nothing survives.

This is the realistic learner flow, and it answers a different question: not "what does it cover
unaided" but "can it use the feedback". Archive it as `hardened/claude-code-promptD/`.

## 4. Scoring

```bash
cd <repo>/spec-test-driven
./gradlew ":hardened:claude-code-promptC:test"        # validity on the correct :core

cd ../spec-test-driven-grading
./gradlew verifyMutants -PmutantTests=../spec-test-driven/hardened/claude-code-promptC/src/test/kotlin --continue
```

**Do not run the graded score inside the Claude session** — the report names the surviving mutants, so
the agent would see the answers to the set it is being measured against.

Record: validity, test count, mutation score over the 11 must-kill mutants, and what it did with the
two spec-dependent ones. Then add a row to the table in
[`mutation-testing-notes.md`](mutation-testing-notes.md).

## What the run is meant to settle

Before the isolation work, the folder contained the policy acceptance suite whose test names spell out
the answers, plus READMEs that listed the gaps and both under-specifications. A frontier agent scoring
well then proved little. This run is the first honest frontier measurement — and it decides whether
mutation testing has to get harder for strong agents, or whether the escalation belongs entirely to the
API/OpenAPI depth of 11.7.
