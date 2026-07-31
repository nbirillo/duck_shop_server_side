# Teacher pack

Everything that explains, hints at or answers the exercises. It lives here, in the teacher-only
build, and **not** in `spec-test-driven/`, for the same reason the reference implementation does: a
learner opens that folder in an IDE with an AI agent, and anything inside it is readable by that
agent. The student folder therefore holds only the task statement and the commands to run.

| File | What it is |
| --- | --- |
| [`exercise-11.2-answer-key.md`](exercise-11.2-answer-key.md) | 11.2: the planted defect, the gaps, the two under-specifications, how to grade |
| [`mutation-testing-notes.md`](mutation-testing-notes.md) | How the mutant machinery works, the graded catalog, why some mutants must survive, measured scores |
| [`running-advanced-agents.md`](running-advanced-agents.md) | Driving a strong interactive agent (Claude Code, Junie) without an API key, and the prompts |

When the slides exist, the narrative parts of these notes become slide content; the commands and the
answer keys stay here, next to the code they refer to.

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
