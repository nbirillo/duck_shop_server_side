# Spec/Test-Driven Development with AI in Kotlin

An AI-native module on **spec- and test-driven development in Kotlin**, built on top of the
[Duck Shop server-side course](https://github.com/nbirillo/duck_shop_server_side).

> **Want to build the project from scratch first?** Start with the server-side course above.
> This module is self-contained and can also be taken as the next step after it.

**You write the specification and the tests. An AI agent writes the code. The tests decide whether to
accept it.** The skill the module teaches is the one that does not transfer to the agent:
**verification** — saying precisely what correct means, and checking that what came back satisfies it.

You can use any agent you like, local or hosted. Everything here was measured on real runs across five
local models and one frontier model, and the module is deliberately agent-agnostic: nothing in it
depends on which one you open.

## Before anything else

```bash
cd spec-test-driven
./gradlew :core:test
```

This must be green before you start. If it is not, fix the setup first — every exercise compiles
against `:core`.

## The one rule

**You never edit `core/`.** It is given, it is correct, and it is the thing your tests and your
specifications are *about*. Your work lives in `exercises/`, and that is the only thing here you are
expected to change.

## Where to go

Each exercise has its own README with the task, the commands and what to hand in. This file is only the
map.

| Start here | What it is |
| --- | --- |
| [`exercises/write-tests/README.md`](exercises/write-tests/README.md) | **Exercise 11.2** — an AI wrote a test suite for the given algebra. Decide whether it can be trusted, then make it complete. |
| [`exercises/write-tests/README-advanced.md`](exercises/write-tests/README-advanced.md) | The harder tier of the same exercise, for when your agent found the basic one easy: did you forbid something legal · could you have said it in fewer tests · can another agent get past you on purpose |
| [`exercises/write-spec/README.md`](exercises/write-spec/README.md) | **Exercise 11.4** — write the specification of a pricing function precise enough that two correct implementations cannot disagree |
| [`exercises/write-spec/README-advanced.md`](exercises/write-spec/README-advanced.md) | The harder tier: specify a thing that *composes*, and state the laws it obeys |
| the capstone | Handed out separately when you get there — the whole cycle alone, on inherited artifacts |

| Reference, when a report confuses you | What it explains |
| --- | --- |
| [`mutants/README.md`](mutants/README.md) | Mutation testing: where the mutants come from, and how to read the report |
| [`variants/README.md`](variants/README.md) | The conformance check — the same engine with the opposite expectation: your suite must stay **green** |
| [`exercises/write-tests/contract-addendum.md`](exercises/write-tests/contract-addendum.md) | The two questions the basic task left open, now written down as contract (advanced tier only) |

## What is in here

`:core` is the only Gradle module you compile against; everything else is either an exercise you edit
or a generated feedback loop you run.

| Directory | Role |
| --- | --- |
| `core/` | The **given** algebra — read-only. `AdmissionPolicy` with its four leaves and three combinators, `Duck`/`Accessory`/`Shop`, plus `DiscountRule` and `Franchise`/`Offer` for the later exercises |
| `exercises/` | Your work. `write-tests/` (11.2) and `write-spec/` (11.4) |
| `mutants/` | `:core` with one defect injected per copy. Feedback for 11.2: a good suite notices every one |
| `variants/` | `:core` rewritten **without** changing what it decides. A good suite stays green on all of them |
| `attacks/` | Advanced tier: implementations written to pass your suite and still contradict the specification. `demo-prefix/` is a worked example; `reference/` is the unmodified algebra the differential probe compares against |
| `tools/` | The prompts handed to agents, and the differential probes the checks record with |
| `sandbox/<name>/` | Appears when you run `sandbox`: your specification and the types, and nothing else — where an agent works. Derived, git-ignored |
| `build-logic/` | The Gradle tasks behind every check. Nothing to edit |

## Author-side material — not here on purpose

Everything that explains, hints at or answers an exercise lives in **`../spec-test-driven-grading/`**,
outside this folder: the reference implementation, the graded catalogs, the answer keys and the spec
corpus. The reason is mechanical rather than ceremonial — you open *this* folder in an IDE with an AI
agent, and anything inside it is readable by that agent.

That build reaches into this one through a composite build (`includeBuild("../spec-test-driven")`), and
the link points only **into** this project, never back out. So this folder knows nothing about grading,
and nothing here depends on it.

It is in the same public repository, which means the separation is a matter of not looking rather than
of a lock. Keeping it unread is on you, and it is the only way the checks tell you anything true.
