# Running advanced (interactive) agents — teacher guide

Author-side notes for evaluating a **strong, interactive** agent (Claude Code, JetBrains Junie,
Cursor, …) **without an API key** — the counterpart to `runAgent`, which covers the
OpenAI-compatible providers (Ollama / Mistral / Anthropic).

This guide lives in the teacher pack rather than in `spec-test-driven/tools/` because it names the
planted defects and the prompts: an agent working inside the student folder must not be able to read
it. Unless stated otherwise, run the commands below from **`spec-test-driven/`**.

> Prompts A and B lived here until 2026-09-18. They implemented the opening-schedule stubs, and that
> exercise was cut because it never reached the slides — so they went with it, along with
> `checkPrimary` and `resetStarter`. What is below is the part that is still live.

## Setup — this also enforces the reference isolation

Open the **`spec-test-driven/` folder itself** as the project in the agent's CLI or IDE — **not** the
repository root. Everything that answers an exercise lives outside that folder
(`../spec-test-driven-grading/`), so an agent working from it cannot read the answers.

Verified in practice rather than assumed: Claude Code read the given contract in `core/` — which it is
welcome to — and had no access to the grading build. The isolation is directory-shaped, so it only
holds while the agent is pointed at `spec-test-driven/` and not at the repository root.

## Exercise 11.2 — verify and harden a suite, then score it by mutation testing

The task lives in `exercises/write-tests/`: a flawed "AI-written" suite the agent must verify and
harden. `runAgent -Pmode=verify-harden` covers the API models; for an interactive agent use one of the
prompts below, then archive and score its suite exactly the same way.

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

Prompt C measures what the agent covers unaided; Prompt D measures whether it can *use* the feedback.
Keep them as separate runs — they are different regimes, and the difference between them is the
result worth having: measured, D scored **one mutant worse** than C.

### Archiving and scoring

```bash
# in spec-test-driven/ — archive the agent's suite the way runAgent does (hardened/ is git-ignored)
mkdir -p hardened/<agent>/src/test/kotlin/org/jetbrains/kotlin/course/duck/shop/admission
cp exercises/write-tests/src/test/kotlin/.../PolicyTests.kt hardened/<agent>/src/test/kotlin/.../
cp hardened/<any-existing-run>/build.gradle.kts hardened/<agent>/   # the same consumer script
git restore -- exercises/write-tests                               # put the flawed suite back

./gradlew :hardened:<agent>:test                                   # validity on :core

# then, in spec-test-driven-grading/ — the graded score
./gradlew verifyMutants -PmutantTests=../spec-test-driven/hardened/<agent>/src/test/kotlin --continue
```

The last command is the honest score: the graded mutant set lives in the teacher-only build, so the
agent never saw which defects it would be measured against. Expected results and how to read them are
in [`mutation-testing-notes.md`](mutation-testing-notes.md).

## The rest of the module

| For | See |
| --- | --- |
| 11.2 advanced tier — variants, budget, the adversary | [`advanced-tier-notes.md`](advanced-tier-notes.md) and [`claude-attack-checklist.md`](claude-attack-checklist.md) |
| 11.4 — a frontier agent as the *implementer* | [`claude-implement-checklist.md`](claude-implement-checklist.md) |
| 11.6 — the capstone, feedback and the debrief | [`capstone-11.6.md`](capstone-11.6.md) |
