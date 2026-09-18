# Teacher pack

Everything that explains, hints at or answers the exercises. It lives here, in the teacher-only
build, and **not** in `spec-test-driven/`, for the same reason the reference implementation does: a
learner opens that folder in an IDE with an AI agent, and anything inside it is readable by that
agent. The student folder therefore holds only the task statement and the commands to run.

| File | What it is |
| --- | --- |
| [`exercise-11.2-answer-key.md`](exercise-11.2-answer-key.md) | 11.2: the planted defect, the gaps, the two under-specifications, how to grade |
| [`mutation-testing-notes.md`](mutation-testing-notes.md) | How the mutant machinery works, the graded catalog, why some mutants must survive, measured scores |
| [`../fixtures/11.4/`](../fixtures/11.4/) | Every spec any agent wrote for 11.4 — committed on purpose. The 11.4b handout, the benchmark fixtures, and the regression corpus for the checker |
| [`exercise-11.4-calibration.md`](exercise-11.4-calibration.md) | 11.4: what the spec brief leaves open, how every agent did, why our consistency check ranks the best spec last, and the laws verified for the advanced tier |
| [`advanced-tier-notes.md`](advanced-tier-notes.md) | 11.2 advanced: the contract addendum, the conformant variants and what each one frees, the calibrated test budget, the adversary |
| [`claude-run-checklist.md`](claude-run-checklist.md) | Step-by-step for the by-hand Claude Code measurement of 11.2, incl. what to clear out first |
| [`claude-attack-checklist.md`](claude-attack-checklist.md) | Step-by-step for the by-hand frontier attack, with prompts E (blind) and F (with feedback) |
| [`exercise-11.4-answer-key.md`](exercise-11.4-answer-key.md) | 11.4: the property catalog and the hand-written key — and the appeal (`verifyClaims`) a learner can raise against them |
| [`exercise-11.4-corpus-results.md`](exercise-11.4-corpus-results.md) | 11.4 measured four ways over thirteen specifications — the evidence behind the slides, including two retractions |
| [`capstone-11.6.md`](capstone-11.6.md) | 11.6: why it is a separate build, what is graded, how to answer each verdict, and the provenance of every inherited artifact |
| [`capstone-11.6-debrief.md`](capstone-11.6-debrief.md) | The capstone debrief — five beats, every number with its provenance. No slides for it on purpose |
| [`claude-implement-checklist.md`](claude-implement-checklist.md) | Driving a frontier agent as the *implementer*: code from a specification alone (11.4, step 3e) |
| [`running-api-agents.md`](running-api-agents.md) | `runAgent` — every mode, the flags each one needs, and difficulty calibration |
| [`running-advanced-agents.md`](running-advanced-agents.md) | Driving a strong interactive agent (Claude Code, Junie) without an API key, and the prompts |

The narrative parts of these notes are what became slide content; the commands and the answer keys stay
here, next to the code they refer to. **The slides are the ground truth for what a learner is told** —
if a command here disagrees with a slide, the slide is right and this file is stale.

## Reference implementation and grading

```bash
# in spec-test-driven-grading/
./gradlew :grading:test        # the given-algebra acceptance suite against :core
./gradlew verifyMutants --continue                      # graded mutants vs the learner's 11.2 suite
./gradlew verifyMutants -PmutantTests=tests/kotlin      # graded mutants vs the authored policy suite
```

This build lives outside `spec-test-driven/` on purpose: a learner opens that folder, so an agent
working from it cannot reach the reference implementation, the graded catalogs or these notes. It
borrows `:core` through a composite build (`includeBuild("../spec-test-driven")`); the link points only
from here INTO the student project, never the other way, so the student build knows nothing about
grading.

To hand out the module, export the `spec-test-driven/` folder only — and, when the capstone starts,
`spec-test-driven-capstone/`. A clone of the whole repository has this build beside them, so the
separation is a matter of not looking rather than of a lock; the build-root [`../README.md`](../README.md)
says the same thing to whoever opens it.

## What stays in the student folder, and why

| In `spec-test-driven/` | Kept because |
| --- | --- |
| `exercises/write-tests/README.md` | The task statement and the commands — no gap list, no named ambiguities |
| `mutants/README.md` | How to read a mutation report; the learner runs the task themselves |
| `mutants/catalog.json` + generated mutants | The practice feedback loop. Entries carry **no** `what`/`hint` text; those exist only in the graded catalog here |
| `tools/agent-prompt*.md` | Harness prompts for `runAgent` (output contracts, no answers) |

Two known limits of this isolation, both accepted: a learner *can* read a practice mutant's source
and see the changed line (that is the price of a local feedback loop), and directory isolation only
holds if the learner is given the `spec-test-driven/` folder rather than the whole monorepo.
