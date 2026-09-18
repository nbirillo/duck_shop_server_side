# Running API agents (`runAgent`) — teacher guide

Author-side tooling for evaluating the module against different agents over an OpenAI-compatible chat
API (Ollama / Mistral / Anthropic). The counterpart for strong interactive agents without an API key
is [`running-advanced-agents.md`](running-advanced-agents.md).

**Mostly not a student workflow.** `runAgent` exists to test the *course content across agents* — how
each one does the same task, and whether the checks catch its mistakes. The one exception is
`impl-from-spec`, which **is** a learner step: in 11.4 and in the capstone the learner hands their own
specification to an agent and looks at the code that comes back.

`-Pmode` is **required** — there is no default. Run the commands from **`spec-test-driven/`** unless a
mode needs a brief or an author-side prompt, which live in this build (noted per mode below).

## Modes

| `-Pmode=` | What the agent does | Written to | Run from |
| --- | --- | --- | --- |
| `tests` | Writes a test suite for the given `:core` algebra, from a generic prompt | `test-suites/<agent>/` | student |
| `verify-exercise` | Produces the flawed starter suite of 11.2 (the invalid `Not` test is injected afterwards) | `exercises/write-tests/…/PolicyTests.kt` | student |
| `verify-harden` | Does the 11.2 task: verifies and hardens that flawed suite | `hardened/<agent>/` | student |
| `attack` | The 11.2 advanced tier: an implementation that passes a given suite and still contradicts the spec | `attacks/<agent>/` | student |
| `spec` | Writes a 11.4 specification from the bare brief | `specs/<agent>/` | **teacher** — needs `briefs/11.4-basic.md` |
| `spec-advanced` | The same for the composing tier | `specs-advanced/<agent>/` | **teacher** — needs `briefs/11.4-advanced.md` |
| `spec-compress` | 11.4b: merges several specifications into one, shorter and no weaker | `specs-short/<agent>/` | **teacher** — needs `-PspecsFrom=<dir>` |
| `spec-extract` | Layer 2: lists which of the catalogued claims a specification asserts | `extractions/<agent>/` | **teacher** |
| `impl-from-spec` | Implements from a specification and the surface alone — **the learner's own step** | `implementations/<agent>/<spec>/` | either |

The prompt is assembled from the `:core` contract and the named specification — never the reference
implementation, which is not even in the student folder — so an API agent cannot copy answers. The
agent returns each file in a `// FILE: <path>` fenced block; weaker models that ignore this are merged
into one file as a fallback, and the output is normalised (single `package`, hoisted imports,
redeclarations of existing types stripped). **The normalisation touches wrapping only, never logic** —
we grade what the model computed, not how well it followed a markdown contract. Every rescue is printed
and written to `normalisation.txt` beside the module, so a broken output contract can never read as a
clean run.

For `tests`, `verify-exercise` and `verify-harden` the prompt is deliberately **generic** — it does not
enumerate the corner cases, so the result measures what the model finds on its own.

## Commands

```bash
# Ollama (local, no key):
./gradlew runAgent -Pmode=tests -Pprovider=ollama -Pmodel=qwen2.5-coder:7b

# Mistral (needs MISTRAL_API_KEY in the environment):
./gradlew runAgent -Pmode=tests -Pprovider=mistral -Pmodel=mistral-small-latest

# Anthropic / Claude (needs ANTHROPIC_API_KEY; OpenAI-compatible endpoint):
./gradlew runAgent -Pmode=tests -Pprovider=anthropic -Pmodel=claude-sonnet-4-5

# See the assembled prompt without calling the API or writing files:
./gradlew runAgent -Pmode=tests -Pprovider=ollama -Pmodel=qwen2.5-coder:7b -Pdry
```

`-Pagent=<name>` overrides the folder name (default `<provider>-<safe model>`). Then score the result:

```bash
./gradlew ":test-suites:<name>:test"             # tests mode: the generated suite against :core
./gradlew ":hardened:<name>:test"                # verify-harden: validity on :core
./gradlew verifyAttack -Pagent=<name> --continue # attack mode: did the suite catch it
```

For the mutation score of a hardened suite see [`mutation-testing-notes.md`](mutation-testing-notes.md);
for scoring a specification, [`exercise-11.4-answer-key.md`](exercise-11.4-answer-key.md).

## Where the harness prompts live

Student-side, in `spec-test-driven/tools/agent-prompt*.md`: `-tests`, `-verify`, `-attack` and
`-impl-spec`. Deliberately **not** a root `AGENTS.md`, so a learner's own agent does not auto-load them.
They carry only the task statement and the output contract, no answers, which is why they can stay there.

Author-side, in this build: `prompts/agent-prompt-spec.md`, `-extract.md`, `-compress.md`, and
`prompts-capstone/agent-prompt-compress.md` (the capstone's consolidation prompt, whose instruction is
the *inverse* of the 11.4b one — keep everything, decide nothing). These name what the exercise is
about, so they are not in the student folder.

The principle behind the split: **student-facing requirements are the specification itself** — the KDoc
contract, the exercise README and the module materials — never an agent-instruction file.

## Difficulty calibration

The task an agent gets has to be hard enough to make verification meaningful, and the module found its
level by measurement rather than by guessing:

- The plain in-memory policy task was **too easy** — every tested model, down to `qwen2.5-coder:1.5b`,
  one-shot it. Writing well-specified logic is not where agents fail.
- So 11.2 is not "implement it" but **verify and harden a given suite**, which does separate the tiers:
  every model fixes the planted wrong test, almost none finds what is missing.
- Where a frontier agent still saturates 11.2, the **advanced tier** is the answer — conformant
  variants, the test budget, the adversary. All three were measured as frontier-only; see
  [`advanced-tier-notes.md`](advanced-tier-notes.md).
- 11.4 moves the object up a level (the text, not the code), and the capstone gives the whole cycle at
  once on artifacts somebody else left behind.

> An earlier calibration used an opening-schedule engine, where `7b` and `14b` got the wrap-around wrong
> in opposite directions. That exercise was cut on 2026-09-18 — it never reached the slides — and its
> `-Pmode=impl` went with it. The finding is kept here because it is the cleanest example of two models
> failing the same case in opposite directions.

The current model matrix and per-model quirks are in the project notes; the measured mutation scores are
in [`mutation-testing-notes.md`](mutation-testing-notes.md).
