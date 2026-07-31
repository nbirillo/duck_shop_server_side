# Running advanced (interactive) agents — teacher guide

Author-side notes for evaluating a **strong, interactive** agent (Claude Code, JetBrains Junie,
Cursor, …) on the module task **without an API key** — the counterpart to `runAgent`, which
covers the OpenAI-compatible API providers (Ollama / Mistral / Anthropic).

## Setup (this also enforces the reference isolation)

Open the **`spec-test-driven/` folder itself** as the project in the agent's CLI/IDE — **not** the
repository root. The reference implementation lives OUTSIDE this folder
(`../spec-test-driven-grading/`), so an agent working from `spec-test-driven/` physically cannot
read the answers. (Verified in practice: Claude Code read `core/.../schedule/TimeTypes.kt` — the
given contract — but had no access to the grading build.)

The agent implements the `TODO()` stubs in
`starter/src/main/kotlin/.../admission/schedule/` (`WindowMatching.kt`, `OpeningSchedule.kt`).
Afterwards:

```bash
./gradlew checkPrimary                 # runs the shared suite against :starter
# optional: keep the result for comparison (git-ignored):
#   cp -r starter/src/main/kotlin/.../schedule solutions/<agent>/src/main/kotlin/.../schedule
./gradlew resetStarter                 # restore the pristine :starter stubs (git-based)
```

## Two prompts (pick per goal)

### Prompt A — spec-only (recommended default / exemplar)

```
Implement the two TODO() functions in
starter/src/main/kotlin/org/jetbrains/kotlin/course/duck/shop/admission/schedule/ —
DailyWindow.covers (WindowMatching.kt) and OpeningSchedule.isOpenAt (OpeningSchedule.kt),
strictly from their KDoc specification. Implement from the KDoc only.
Do NOT read or run the tests; do NOT look inside tests/ or solutions/.
Do not modify any other files or modules.
```

**Why this one.** It mirrors the single-shot, spec-only regime used for the API/local models
(`runAgent`) on the simpler module tasks, so results are directly comparable across the module.
It measures whether the agent **intuits the corner cases from the spec alone** — exactly where
mid agents slip: on the opening-schedule task `qwen2.5-coder:7b` over-matched the wrap-around
(opened on the wrong day) and `qwen2.5-coder:14b` under-matched it (never opened the day-after
early-morning part), while Claude Code got it right. Use this as the standard evaluation.

### Prompt B — full access, iterate to green (realistic / honest usage)

```
Implement the two TODO() functions in
starter/.../admission/schedule/ so that `./gradlew checkPrimary` passes.
You may read the tests and run Gradle to iterate.
Do not modify the tests, :core, or any other module — only the two stub files.
```

**Why this one.** It mirrors how a learner actually works with an agent — full repo access,
iterating against the tests. A strong agent usually reaches green; the lesson it demonstrates is
that a **good test suite forces even a strong agent to correctness** (the verification payoff),
whereas a weak agent may still fail to compile. Use this to show the realistic end-to-end flow.

## Comparability

Prompt A results sit in the same regime as the `runAgent` runs (single pass, no test feedback),
so a strong interactive agent and the API/local models can be compared on the same footing.
Prompt B is a different regime (feedback loop) and is not directly comparable — it evaluates the
workflow, not raw corner-case intuition.

## Exercise 11.2 — verify & harden a suite, then score it by mutation testing

The same setup applies (open `spec-test-driven/` as the project). The task lives in
`exercises/write-tests/`: a flawed "AI-written" suite the agent must verify and harden. `runAgent
-Pmode=verify-harden` covers the API models; for an interactive agent use one of the prompts below,
then archive and score its suite exactly the same way.

### Prompt C — blind harden (comparable with the `runAgent` results)

```
In exercises/write-tests/src/test/kotlin/.../PolicyTests.kt an AI wrote a test suite for the
admission-policy algebra given in :core. Verify and harden it: fix any test that contradicts the
correct behaviour, and add the cases it is missing. Do not change :core or any other module.
Do NOT run ./gradlew verifyMutants and do not open mutants/ — I want to see what you cover on
your own.
```

### Prompt D — with mutation-testing feedback (the realistic learner flow)

```
… same task, and then: run `./gradlew verifyMutants --continue` and keep strengthening the suite
until every must-kill mutant is dead. Do not edit anything under mutants/ or :core.
```

Prompt C measures what the agent covers unaided; Prompt D measures whether it can *use* the
feedback. Keep them as separate runs — they are different regimes.

### Archiving and scoring

```bash
# archive the agent's suite the way runAgent does (hardened/ is git-ignored)
mkdir -p hardened/<agent>/src/test/kotlin/org/jetbrains/kotlin/course/duck/shop/admission
cp exercises/write-tests/src/test/kotlin/.../PolicyTests.kt hardened/<agent>/src/test/kotlin/.../
cp hardened/ollama-qwen2.5-coder-7b/build.gradle.kts hardened/<agent>/   # same consumer script
git restore -- exercises/write-tests                                     # put the flawed suite back

./gradlew :hardened:<agent>:test                                         # validity on :core
cd ../spec-test-driven-grading
./gradlew verifyMutants -PmutantTests=../spec-test-driven/hardened/<agent>/src/test/kotlin --continue
```

The last command is the honest score: the graded mutant set lives in the teacher-only build, so the
agent never saw which defects it would be measured against.
