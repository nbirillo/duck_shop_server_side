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
the **Specification pattern**:

- **Leaves:** `KotlinOnly`, `MaxBudget`, `RequiresAccessory`, `MinAccessories`
- **Combinators:** `All`, `Any`, `Not`

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

- `:core` — contract + domain: `AdmissionPolicy` + `Duck`/`Accessory`/`Shop`. No implementations.
- `:starter` — student-facing stubs (`TODO()`). Red until implemented.
- `:grading` — reference implementation, teacher-only (`-PincludeGrading`).
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

Reference grading suite (teacher-only):

```bash
./gradlew :grading:test -PincludeGrading
```

> To work on `:grading` inside the IDE (so it imports as a real module without passing the
> flag on every sync), add `includeGrading=true` to your **`~/.gradle/gradle.properties`**
> and Reload the Gradle project. This is a local, uncommitted teacher setting; students who
> don't set it get the clean `:core` + `:starter` view.

## Generate a solution with an AI agent (Ollama / Mistral)

> **Author-side tool, not a student workflow.** `runAgent` and the `solutions/<agent>/` modules
> exist to test the *course content across different agents* — to see how each agent implements
> the same task and whether the tests catch its mistakes. A student does **not** use this to
> solve the exercise: they work in `:starter` (writing/checking specs and tests), and the AI
> merely fills the implementation.

The `runAgent` task calls an OpenAI-compatible chat API to fill in the stubs and writes the
result as a new `solutions/<agent>/`. It assembles the prompt from the `:core` contract and the
`:starter` stubs only — never `:grading` — so an API agent cannot copy the reference answers.

```bash
# Ollama (local, no key):
./gradlew runAgent -Pprovider=ollama -Pmodel=qwen2.5-coder

# Mistral (needs MISTRAL_API_KEY in the environment):
./gradlew runAgent -Pprovider=mistral -Pmodel=mistral-small-latest

# See the assembled prompt without calling the API or writing files:
./gradlew runAgent -Pprovider=ollama -Pmodel=qwen2.5-coder -Pdry
```

Options: `-Pagent=<name>` (defaults to `<provider>-<model>`). Then check how it did:

```bash
./gradlew checkPrimary -PprimaryAgent=<name>     # e.g. ollama-qwen2.5-coder
```

Interactive agents (Claude Code, Junie, Cursor) are driven in the IDE instead of via this task.