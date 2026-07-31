# Checklist — measuring Claude Code on exercise 11.2

Step-by-step for the run that has to be done by hand, because there is no Anthropic API key. The
prompts themselves are in [`running-advanced-agents.md`](running-advanced-agents.md); this file is the
operational order of events, including the two things that silently invalidate the measurement.

## 0. Run in a fresh export, not in the working copy

Do **not** measure in `<repo>/spec-test-driven`. Our working copy accumulates git-ignored artifacts of
our own harness runs that a learner never receives — and one of them is fatal:
`test-suites/claude-code/` is an 86-test suite that already scores 11/11 on the graded mutants, sitting
right there for an agent to read. `test-suites/ollama-*` and `hardened/ollama-*` are other models'
suites for the same algebra, and `solutions/ollama-*` are generated schedule implementations.

Export exactly the tracked content instead — that *is* the learner's environment, and it also removes
any risk of the run mutating our copy:

```bash
cd <repo>
rm -rf ~/IdeaProjects/duck-shop-student-copy
mkdir -p ~/IdeaProjects/duck-shop-student-copy
git archive HEAD spec-test-driven | tar -x -C ~/IdeaProjects/duck-shop-student-copy
```

The export is a self-contained Gradle build (own wrapper, own `build-logic`). Sanity-check it before
handing it to the agent — the exercise must start from its flawed state:

```bash
cd ~/IdeaProjects/duck-shop-student-copy/spec-test-driven
./gradlew verifyMutants --continue     # baseline INVALID (1 test), mutation score 1/4
```

This is also the answer to "what exactly do we hand out?": the handout is this export, nothing more.
Directory isolation only holds because the export contains no `spec-test-driven-grading/`.

## 1. Open the right folder

Open **`~/IdeaProjects/duck-shop-student-copy/spec-test-driven`** as the project in Claude Code — the
export from step 0, and that folder itself rather than its parent. The export contains no grading build
at all, so the reference implementation, the graded mutants, the answer key and this file are not
merely hidden from the agent; they are not on disk anywhere near it.

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

When it stops, say so and which run it was; the suite is then copied from the export back into the
working copy for scoring. Manually, from `<repo>/spec-test-driven`:

```bash
PKG=org/jetbrains/kotlin/course/duck/shop/admission
EXPORT=~/IdeaProjects/duck-shop-student-copy/spec-test-driven
mkdir -p hardened/claude-code-promptC/src/test/kotlin/$PKG
cp $EXPORT/exercises/write-tests/src/test/kotlin/$PKG/PolicyTests.kt hardened/claude-code-promptC/src/test/kotlin/$PKG/
cp hardened/ollama-qwen2.5-coder-7b/build.gradle.kts hardened/claude-code-promptC/
```

Nothing to restore in the working copy — the agent only ever touched the export.

## 3. Run B — with mutation-testing feedback

Re-create the export (step 0) so the exercise is flawed again, and use a **fresh** Claude session — an
existing one already knows what it wrote. Paste Prompt D: same task, but the agent may run
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
