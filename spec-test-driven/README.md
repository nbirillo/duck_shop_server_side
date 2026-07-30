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

## Run the tests

The student-facing spec/tests (`:starter`) — red until the policy stubs are implemented:

```bash
cd spec-test-driven
./gradlew :starter:test
```

Teachers can run the reference grading suite (`:grading`, hidden unless the flag is passed):

```bash
./gradlew :grading:test -PincludeGrading
```

> To work on `:grading` inside the IDE (so it imports as a real module without passing the
> flag on every sync), add `includeGrading=true` to your **`~/.gradle/gradle.properties`**
> and Reload the Gradle project. This is a local, uncommitted teacher setting; students who
> don't set it get the clean `:core` + `:starter` view.

Modules: `:core` (contract + `Duck`/`Accessory`/`Shop` domain) · `:starter` (stubs + tests) ·
`:grading` (reference impl + tests, teacher-only).