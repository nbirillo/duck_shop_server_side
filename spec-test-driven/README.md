#  Spec/Test-Driven Development with AI in Kotlin

An AI-native module on **spec- and test-driven development in Kotlin**, built on top of the
[Duck Shop server-side course](https://github.com/nbirillo/duck_shop_server_side).

> **Want to build the project from scratch first?** Start with the server-side course above.
> This module is self-contained and can also be taken as the next step after it.

## Idea

The learner writes the **specification and the tests**; an AI agent implements the Kotlin code
against them; the tests are the acceptance criterion. The skill the module teaches is
**verification** — writing good specs/tests and checking that generated code satisfies them. 


TODO: describe about agent-agnostic and how to work with this later

## Feature

Duck shops with an **admission policy** — `AdmissionPolicy.admits(duck): Boolean`, modelled with
the **Specification pattern** (`KotlinOnly`, `MaxBudget`, `RequiresAccessory`, `MinAccessories`
+ combinators `AllOf`, `AnyOf`, `Not`). This algebra is **given** in `:core`.

The current implementation **task** (what an agent/student implements) is the harder
**opening-schedule** engine: a shop admits ducks only while open, per a weekly schedule.

- `DailyWindow.covers(at)` — open inclusive, close exclusive, wrap past midnight.
- `OpeningSchedule.isOpenAt(at)` — union of windows, `SpecialClosure` overrides, empty = closed.

(The earlier in-memory policy task turned out too easy — every tested model one-shot it — so the
schedule engine replaced it as the difficulty-calibrated task.)

## Run the app

The module builds on the Duck Shop server-side app (REST API + UI). From the repository root:

```bash
cd server
./gradlew bootRun
```

Then open <http://localhost:8080> for the UI, or `GET /api/ducks` for the API.
Dev credentials (HTTP Basic): `admin/admin` (ADMIN), `user/user` (USER).

A lightweight Ktor variant of the API lives in `ktor-server/` (`./gradlew run`, port 8081).

## Modules

One shared acceptance suite (`tests/kotlin`) is compiled and run against every implementation
module (Design B): a module supplies only its policy implementation under `src/main`, and the
`duck-shop.solution` convention plugin (in `build-logic/`) wires in `:core` and the shared tests.

- `:core` — contract + domain + **given** algebra & time types (`AdmissionPolicy`, `Duck`/`Accessory`/`Shop`, the policy leaves/combinators, `DailyWindow`/`SpecialClosure`).
- `:starter` — the stubs to implement (`schedule/WindowMatching.kt`, `schedule/OpeningSchedule.kt`), `TODO()`. Red until implemented.
- `spec-test-driven-grading/` — reference implementation + grading suite: a **separate teacher-only build outside this folder**, invisible to students (see "Reference grading suite" below).
- `solutions/<agent>/` — one implementation per AI agent (`src/main` + `agent.json`), auto-discovered.

## Run the tests

Check the primary implementation (`primaryAgent` in `gradle.properties`, default `starter`):

```bash
cd spec-test-driven
./gradlew checkPrimary                 # or override: -PprimaryAgent=<name>
```

Run the suite against every `solutions/<agent>/` and compare:

```bash
./gradlew compareAgents --continue
```

Reference grading suite (teacher-only) — a **separate build outside this folder**:

```bash
cd ../spec-test-driven-grading
./gradlew :grading:test
```

> `spec-test-driven-grading/` lives OUTSIDE the student's `spec-test-driven/` folder on purpose:
> a student opening `spec-test-driven/` — and any agent working from it — never sees the reference
> implementation, not even as readable files. That grading build links back to `:core` and the
> shared test suite via a composite build (`includeBuild("../spec-test-driven")`); the link only
> points from grading INTO the student project, never the other way.

## Generate a solution with an AI agent (Ollama / Mistral)

> **Author-side tool, not a student workflow.** `runAgent` and the `solutions/<agent>/` modules
> exist to test the *course content across different agents* — to see how each agent implements
> the same task and whether the tests catch its mistakes. A student does **not** use this to
> solve the exercise: they work in `:starter` (writing/checking specs and tests), and the AI
> merely fills the implementation.

The `runAgent` task calls an OpenAI-compatible chat API to fill in the stubs and writes the
result as a new `solutions/<agent>/`. It assembles the prompt from the `:core` contract and the
`:starter` stubs only — never `:grading` — so an API agent cannot copy the reference answers.
The agent is asked to return each file in a `// FILE: <path>` fenced block; weaker models that
ignore this are merged into a single file as a fallback.

> The harness system prompt lives in **`tools/agent-prompt.md`** — it is author-side only and is
> deliberately NOT a root `AGENTS.md`, so it is not auto-picked-up by a student's own agent. The
> `// FILE:` output contract is a harness detail (single API call) and is irrelevant to
> interactive agents. **Student-facing requirements are the spec itself: the stub KDoc and the
> test suite** (plus the module materials), not this prompt.

```bash
# Ollama (local, no key):
./gradlew runAgent -Pprovider=ollama -Pmodel=qwen2.5-coder

# Mistral (needs MISTRAL_API_KEY in the environment):
./gradlew runAgent -Pprovider=mistral -Pmodel=mistral-small-latest

# Anthropic / Claude (needs ANTHROPIC_API_KEY; OpenAI-compatible endpoint):
./gradlew runAgent -Pprovider=anthropic -Pmodel=claude-sonnet-4-5

# See the assembled prompt without calling the API or writing files:
./gradlew runAgent -Pprovider=ollama -Pmodel=qwen2.5-coder -Pdry
```

Options: `-Pagent=<name>` (defaults to `<provider>-<model>`). Then check how it did:

```bash
./gradlew checkPrimary -PprimaryAgent=<name>     # e.g. ollama-qwen2.5-coder
```

Interactive agents (Claude Code, Junie, Cursor) are driven in the IDE instead of via this task —
see `tools/running-advanced-agents.md` for the teacher instructions (setup, isolation, and the
two recommended prompts).