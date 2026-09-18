# Teacher-only build

Everything that explains, hints at or answers an exercise of the Spec/Test-Driven module. It lives
here, outside `spec-test-driven/`, for a mechanical reason rather than a ceremonial one: a learner
opens that folder in an IDE with an AI agent, and **anything inside it is readable by that agent**.

This build reaches into the student one through a composite build
(`includeBuild("../spec-test-driven")`) and borrows `:core` from it. The link points only **into** the
student project, never back out — so the student build knows nothing about this one.

**Hand out the `spec-test-driven/` folder only** (and, at the capstone, `spec-test-driven-capstone/`).
Both are separate directories precisely so that handing one over hands over nothing else.

## Where to go

| Start here | What it is |
| --- | --- |
| [`teacher/README.md`](teacher/README.md) | **The teacher pack** — answer keys, grading procedures, per-exercise notes, the checklists for driving an agent by hand. Start there; it is the index for everything narrative. |
| [`fixtures/11.4/README.md`](fixtures/11.4/README.md) | The spec corpus: every specification any agent wrote for 11.4, committed on purpose. Doubles as the 11.4b handout and as the regression corpus for the checkers. |
| [`teacher/capstone-11.6.md`](teacher/capstone-11.6.md) | The capstone: how it is graded, how to give feedback verdict by verdict, and the provenance of every inherited artifact |
| [`teacher/capstone-11.6-debrief.md`](teacher/capstone-11.6-debrief.md) | The capstone debrief — five beats, each number with its provenance. There are no slides for it on purpose. |

## What is in here

| Directory | Role |
| --- | --- |
| `teacher/` | All the prose: answer keys, notes, checklists. Indexed by its own README |
| `grading/` | The acceptance suite for the **given** policy algebra, run against `:core`. Its test names spell out the cases exercise 11.2 asks the learner to find, which is why it is not in the student folder |
| `tests/kotlin/` | That suite's source |
| `reference/` | The 11.4 reference implementation of `priceFor`, and `bestOffer` for the capstone. One reading of each brief, not the only defensible one |
| `mutants/` | The **graded** mutant catalog for 11.2 — larger than the practice set, and carrying the `what`/`hint` prose the visible one deliberately omits |
| `pricing-mutants/` | The same engine aimed at 11.4: mutants of `:reference`, shot at by the property catalog |
| `pricing-properties/` | The property catalog — the claims a 11.4 specification might make, as runnable code |
| `forks/` | The graded fork readings for the capstone: 9 readings of 4 open questions |
| `forks-practice/` | The one fork (ties) you demonstrate in class. It is here, not in the handout, because a reading is a complete implementation of the feature |
| `tests-11.6/` | The capstone's corner-case suite — a floor, not a grade |
| `tests-11.6-validation/` | A harness self-test: a suite that decides exactly one fork, so `verifyForks` can be checked against a known answer |
| `briefs/` | The business texts handed out in class. Not in the student folder, or the agent answers the brief instead of the specification |
| `prompts/`, `prompts-capstone/` | Author-side agent prompts: claim extraction, spec writing, compression, and the capstone's consolidation prompt |
| `tools/franchise-probe/` | The capstone's differential corpus, 3010 cases. Here rather than in the handout because its own comments name the open questions the capstone is about |
| `fixtures/` | Measured corpora — 11.4 specifications, the hand-written answer key, 11.6 franchise specs |
| `implementations/`, `extractions/` | What agents produced in our measured runs, kept so a number can be traced back to the artifact it came from |

## Running the checks

Every command below is a **teacher** command. The learner's own commands are in the student build's
exercise READMEs, and the two sets are deliberately disjoint.

```bash
# in spec-test-driven-grading/
./gradlew :grading:test                                  # the given-algebra suite against :core
./gradlew verifyMutants --continue                       # graded 11.2 mutants vs the learner's suite
./gradlew verifyMutants -PmutantTests=tests/kotlin       # graded mutants vs the authored suite
./gradlew verifyPricingMutants --continue                # graded 11.4 mutants vs the property catalog
./gradlew verifyForks --continue                         # the capstone: which forks a suite settles
```

Per-exercise procedures, and the flags each one takes, are in [`teacher/README.md`](teacher/README.md).
