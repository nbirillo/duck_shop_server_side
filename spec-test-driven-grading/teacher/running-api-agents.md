# Running API agents (`runAgent`) — teacher guide

Author-side tooling for evaluating the module against different agents over an OpenAI-compatible chat
API (Ollama / Mistral / Anthropic). The counterpart for strong interactive agents without an API key
is [`running-advanced-agents.md`](running-advanced-agents.md).

**This is not a student workflow.** `runAgent` and the `solutions/<agent>/` modules exist to test the
*course content across agents* — how each one implements the same task, and whether the tests catch
its mistakes. A learner never runs it: they work in `:starter` (or in the 11.2 exercise), and the AI
merely fills in the implementation.

Run the commands from **`spec-test-driven/`**.

## Modes

| `-Pmode=` | What the agent does | Written to |
| --- | --- | --- |
| `impl` (default) | Implements the `:starter` stubs | `solutions/<agent>/` |
| `tests` | Writes a test suite for the given `:core` algebra | `test-suites/<agent>/` |
| `verify-exercise` | Produces the flawed starter suite of 11.2 (a planted invalid `Not` test is injected afterwards) | `exercises/write-tests/…/PolicyTests.kt` |
| `verify-harden` | Does the 11.2 task: verifies and hardens that flawed suite | `hardened/<agent>/` |

The prompt is assembled from the `:core` contract and (for `impl`) the `:starter` stubs — never the
reference implementation, which is not even in that folder — so an API agent cannot copy answers. The
agent returns each file in a `// FILE: <path>` fenced block; weaker models that ignore this are merged
into one file as a fallback, and the output is normalised (single `package`, hoisted imports). The
normalisation touches wrapping only, never logic: we grade what the model *computed*, not how well it
followed a markdown contract.

For `tests`, `verify-exercise` and `verify-harden` the prompt is deliberately **generic** — it does
not enumerate the corner cases, so the result measures what the model finds on its own.

## Commands

```bash
# Ollama (local, no key):
./gradlew runAgent -Pprovider=ollama -Pmodel=qwen2.5-coder:7b

# Mistral (needs MISTRAL_API_KEY in the environment):
./gradlew runAgent -Pprovider=mistral -Pmodel=mistral-small-latest

# Anthropic / Claude (needs ANTHROPIC_API_KEY; OpenAI-compatible endpoint):
./gradlew runAgent -Pprovider=anthropic -Pmodel=claude-sonnet-4-5

# See the assembled prompt without calling the API or writing files:
./gradlew runAgent -Pprovider=ollama -Pmodel=qwen2.5-coder:7b -Pdry
```

`-Pagent=<name>` overrides the folder name (default `<provider>-<safe model>`). Then score it:

```bash
./gradlew checkPrimary -PprimaryAgent=<name>     # impl mode: the shared acceptance suite
./gradlew compareAgents --continue               # every solutions/<agent>/ at once
./gradlew ":test-suites:<name>:test"             # tests mode: the generated suite against :core
./gradlew ":hardened:<name>:test"                # verify-harden: validity on :core
```

For the mutation score of a hardened suite, see [`mutation-testing-notes.md`](mutation-testing-notes.md).

## Where the harness prompts live

`spec-test-driven/tools/agent-prompt*.md`, inside the student folder — deliberately **not** a root
`AGENTS.md`, so a learner's own agent does not auto-load them. They carry only the task statement and
the output contract, no answers, which is why they can stay there. The `// FILE:` contract is a
harness detail of a single API call and is irrelevant to an interactive agent, which edits files
directly.

The principle behind this: **student-facing requirements are the spec itself** — the stub KDoc and the
test suite, plus the module materials — never an agent-instruction file.

## Difficulty calibration

The task an agent gets has to be hard enough to make verification meaningful. The original in-memory
policy task was not: every tested model, down to `qwen2.5-coder:1.5b`, one-shot it. It was replaced by
the **opening-schedule** engine (wrap past midnight, special closures), which does separate the tiers:
weak models fail to compile, `7b` and `14b` get the wrap-around wrong in opposite directions, and only
`32b` and frontier agents get it right. The current model matrix and per-model quirks are recorded in
the project notes; the measured mutation scores are in `mutation-testing-notes.md`.
