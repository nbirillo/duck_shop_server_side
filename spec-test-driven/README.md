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

Everything a learner does is built on that one algebra: verify and harden a suite for it (11.2),
specify a pricing function over it (11.4), then do the whole cycle alone on a franchise (11.6).

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

`:core` is the one module you never edit; every exercise module supplies only its own sources and
compiles against it.

- `:core` — contract + domain + the **given** algebra (`AdmissionPolicy`, `Duck`/`Accessory`/`Shop`, the policy leaves/combinators, `DiscountRule`, `Franchise`/`Offer`).
- `exercises/write-tests/` — exercise 11.2: verify and harden an AI-written test suite. Its
  `README-advanced.md` is the harder tier, for when your agent found the basic one easy.
- `mutants/` — the practice mutants that exercise 11.2 scores against: `:core` with one defect injected, and a good suite notices.
- `variants/` — the same idea inverted, for the advanced tier: `:core` rewritten **without** changing what it decides, and a good suite stays green. See `variants/README.md`.
- `attacks/` — also advanced: implementations written to pass your suite while still contradicting the specification. `reference/` is the unmodified algebra the differential probe compares against, and `demo-prefix/` is a worked example.

## Run the tests

Check the setup before anything else — this must be green:

```bash
cd spec-test-driven
./gradlew :core:test
```

## Exercise 11.2 and mutation testing

`exercises/write-tests/` is the first learner-facing exercise: an "AI-written" test suite for the
given `:core` algebra that the learner has to **verify and harden**. See its README.

Feedback comes from **mutation testing** — the suite is run against copies of the algebra with one
injected defect each, and every defect it fails to notice is reported:

```bash
./gradlew verifyMutants --continue           # mutation score
./gradlew verifyMutants -PmutantsStrict      # fail until every must-kill mutant is dead
./gradlew generateMutants                    # only after editing mutants/catalog.json
```

`mutants/catalog.json` is the source of truth: each entry replaces one snippet of a `:core` file, and
`generateMutants` turns it into a module that compiles the real `:core` sources with that one file
swapped. `mutants/README.md` explains how to read the report.

## Author-side material

Everything a learner should not read lives in **`../spec-test-driven-grading/`**, outside this folder,
because anything inside it is readable by the learner's own AI agent:

- `grading/` — the reference implementation, and `tests/kotlin` the acceptance suite for the given
  policy algebra (its test names spell out the cases exercise 11.2 asks the learner to find).
- `mutants/` — the graded mutant set, larger than the practice one here.
- `teacher/` — answer keys, grading commands, mutation-testing notes, and the guides for running
  agents against the module (`runAgent`, interactive agents).

That build links back to `:core` and the shared test suite via a composite build
(`includeBuild("../spec-test-driven")`); the link only points from grading INTO this project, never
the other way, so this folder knows nothing about it.
