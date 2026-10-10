# Running API Agents (`runAgent`) — Teacher Guide

These notes are for teachers.
This guide explains how to use `runAgent` to evaluate the course exercises with different AI models through an OpenAI-compatible chat API.

Supported providers include Ollama, Mistral, and Anthropic.
If you want to evaluate a strong interactive coding agent such as Claude Code, JetBrains Junie, or Cursor without an API key, see [`running-advanced-agents.md`](running-advanced-agents.md).

## What is `runAgent` for?

**Most of the time, `runAgent` is a teacher tool, not a student workflow.**
We use it to test the course itself:

- How do different agents handle the same exercise?
- Which mistakes do they make?
- Do our verification tools detect those mistakes?
- Is the exercise difficult enough to teach something useful?

There is one important exception.
**`impl-from-spec` is a student-facing step.**

In the spec-writing exercise and the capstone, students give an agent their own specification and examine the implementation it produces.
That is part of the learning process, not just our course calibration.

### Before you run anything

You must always provide `-Pmode`.
**There is no default mode.**
Unless noted otherwise, run the commands from:

`spec-test-driven/`

Some modes need teacher-only briefs or prompts and must be run from the grading build. Those are identified below.

---

## 1. Available modes

| `-Pmode=`         | What the agent does                                                                                 | Output directory                         | Run from                                           |
|-------------------|-----------------------------------------------------------------------------------------------------|------------------------------------------|----------------------------------------------------|
| `tests`           | Writes a test suite for the `:core` algebra from a generic prompt                                   | `test-suites/<agent>/`                   | Student project                                    |
| `verify-exercise` | Generates the flawed starter suite for the test-writing exercise; the incorrect `Not` test is injected afterward         | `exercises/write-tests/…/PolicyTests.kt` | Student project                                    |
| `verify-harden`   | Verifies and improves the flawed suite from the test-writing exercise                                                    | `hardened/<agent>/`                      | Student project                                    |
| `attack`          | Advanced test-writing: writes an implementation that passes a given suite but contradicts the specification | `attacks/<agent>/`                       | Student project                                    |
| `spec`            | Writes a basic-tier spec-writing specification from the business brief                                      | `specs/<agent>/`                         | **Teacher build**, using `briefs/write-spec-basic.md`    |
| `spec-advanced`   | Writes a specification for the composable advanced tier                                             | `specs-advanced/<agent>/`                | **Teacher build**, using `briefs/write-spec-advanced.md` |
| `spec-compress`   | The compression exercise: combines several specifications into one that should be shorter and no weaker       | `specs-short/<agent>/`                   | **Teacher build**, with `-PspecsFrom=<dir>`        |
| `spec-extract`    | Layer 2: identifies which claims from the property catalog appear in a specification                | `extractions/<agent>/`                   | **Teacher build**                                  |
| `impl-from-spec`  | Implements a feature using only its specification and the available API surface                     | `implementations/<agent>/<spec>/`        | Either project                                     |

The teacher-only modes use briefs and prompts that aren't included in the student project.
That separation is intentional.

---

## 2. What information does the agent receive?

The harness builds its prompt from:

- The contract in `:core`.
- The relevant specification.
- The task-specific agent prompt.

It does **not** include the reference implementation.
The reference isn't even present in the student folder.
So an API agent can't simply copy the expected solution from the grading materials.

### How does the agent return code?

The prompt asks the agent to return each generated file in a fenced block marked with:

```text
// FILE: <path>
```

Stronger models usually follow this format.
Smaller models sometimes ignore it and return everything as one block.
The harness has a fallback for that case: it can combine the output into one file and normalize the structure.
Normalization may:

- Keep a single `package` declaration.
- Move imports to the correct location.
- Remove redeclarations of types that already exist.
- Repair the file wrapping needed to make the generated output usable.

**Normalization never changes the agent's logic.**
We're trying to measure whether the model solved the programming task, not whether it followed every detail of a Markdown output format.
But we also don't want a broken output format to look like a completely clean run.

Every normalization step is:

1. Printed in the run output.
2. Recorded in `normalisation.txt` beside the generated module.

That way, you can always see when the harness had to rescue the model's output.

### Why are the prompts deliberately generic?

For these modes:

- `tests`
- `verify-exercise`
- `verify-harden`

the prompts don't list the corner cases students are expected to discover.
That's important for calibration.

If we tell an agent exactly which boundaries and negative cases to test, we're no longer measuring what it can find independently.
We're measuring whether it can follow a checklist.

---

## 3. Running agents with different providers

All the examples below use `-Pmode=tests`.
Run them from `spec-test-driven/`.

### Ollama — local, no API key

```bash
./gradlew runAgent \
    -Pmode=tests \
    -Pprovider=ollama \
    -Pmodel=qwen2.5-coder:7b
```

### Mistral — requires an API key

Set `MISTRAL_API_KEY` in your environment.
Then run:

```bash
./gradlew runAgent \
    -Pmode=tests \
    -Pprovider=mistral \
    -Pmodel=mistral-small-latest
```

### Anthropic / Claude — requires an API key

Set `ANTHROPIC_API_KEY` in your environment.
The harness uses an OpenAI-compatible endpoint.

```bash
./gradlew runAgent \
    -Pmode=tests \
    -Pprovider=anthropic \
    -Pmodel=claude-sonnet-4-5
```

### Preview the prompt without calling a model

Use `-Pdry`:

```bash
./gradlew runAgent \
    -Pmode=tests \
    -Pprovider=ollama \
    -Pmodel=qwen2.5-coder:7b \
    -Pdry
```

This prints the assembled prompt without making an API call or writing generated files.
It's useful when you're checking what information an agent will receive.

---

## 4. Naming and scoring the generated results

By default, the harness names the output folder using:

`<provider>-<safe model>`

You can override that name with:

`-Pagent=<name>`

This is useful when you want to keep separate runs of the same model.

### Test a generated suite against `:core`

For `tests` mode:

```bash
./gradlew ":test-suites:<name>:test"
```

### Check a hardened suite

For `verify-harden` mode:

```bash
./gradlew ":hardened:<name>:test"
```

This verifies that the tests pass against the correct `:core` implementation.

### Evaluate an attack

For `attack` mode:

```bash
./gradlew verifyAttack -Pagent=<name> --continue
```

This checks whether the target test suite catches the agent's adversarial implementation.

### Where are the other scoring instructions?

For mutation scoring of a hardened suite, see [`mutation-testing-notes.md`](mutation-testing-notes.md).
For specification evaluation, see [`write-spec-answer-key.md`](write-spec-answer-key.md).

---

## 5. Where do the agent prompts live?

The prompts are deliberately split between the student project and the teacher build.

### Student-side prompts

These live in:

`spec-test-driven/tools/agent-prompt*.md`

They cover:

- `-tests`
- `-verify`
- `-attack`
- `-impl-spec`

These files contain only the task description and output format.
They don't reveal answers.

That's why they can safely remain in the student project.
They are also deliberately **not** placed in a root-level `AGENTS.md`.
A student's own coding agent shouldn't automatically load our evaluation prompts.

### Teacher-side prompts

These live in the grading build:

- `prompts/agent-prompt-spec.md`
- `prompts/agent-prompt-extract.md`
- `prompts/agent-prompt-compress.md`
- `prompts-capstone/agent-prompt-compress.md`

The capstone compression prompt has a different goal from the compression exercise compression prompt.
In the compression exercise, the agent tries to make a specification **shorter without making it weaker**.
In the capstone, the instruction is effectively the opposite:

**Keep everything. Decide nothing.**
The capstone agent consolidates existing information without silently resolving open decisions.
These teacher-side prompts describe what the exercises are testing, so they don't belong in the student folder.

### The principle behind the separation

**Student-facing requirements belong in the specification itself.**
That means:

- The KDoc contract.
- The exercise README.
- The module's learning materials.

An agent-instruction file should never become the hidden source of requirements that students are expected to discover.

---

## 6. How we calibrated the difficulty

An exercise needs to be challenging enough that verification actually matters.
We didn't choose the difficulty by guessing.
We tested the tasks against different agents and adjusted the course based on the results.

### The original implementation task was too easy

Our first task used a simple in-memory admission-policy algebra.
Every model we tested could implement it in one attempt, including `qwen2.5-coder:1.5b`.
That told us something important:
**Implementing well-specified logic wasn't a useful challenge for these agents.**
We needed to test a different skill.

### The test-writing exercise focuses on verification

Instead of asking students to implement the algebra, the test-writing exercise asks them to **verify and harden an existing AI-written test suite**.
This separates model capabilities much more clearly.
Every tested model could fix the deliberately incorrect test.

But most failed to identify the missing cases.
Finding a visible error and discovering an absent requirement are different tasks.
The second is much harder.

### The advanced tier challenges frontier agents

A frontier agent can still solve the basic test-writing task very well.
So the advanced tier adds different challenges:

- **Conformant variants.**
- **A calibrated test budget.**
- **An adversary.**

All three were measured as challenges appropriate for frontier-level agents.
See [`advanced-tier-notes.md`](advanced-tier-notes.md) for the experiments and what each task measures.

### The spec-writing exercise moves from code to specifications

The spec-writing exercise shifts the focus.
Instead of asking whether the implementation is correct, it asks whether the **specification text communicates enough to produce the intended behavior**.
The capstone then brings the entire process together.
Students have to work with specifications, tests, implementations, and feedback — including artifacts left behind by someone else.

---

## 7. A removed experiment worth remembering

An earlier version of the course included an opening-schedule engine.
In calibration, the `7b` and `14b` models both mishandled a schedule that wrapped around a boundary.
Interestingly, they made mistakes in **opposite directions**.
That was a useful example of two models failing the same case differently.
But the exercise never reached the course slides.

We removed it on **September 18, 2026**, along with its `-Pmode=impl` mode.
The experiment is no longer part of the active course.
We keep the observation here because it clearly illustrates why comparing different agents can reveal more than repeatedly running the same one.

---

## 8. Where to find the current measurements

The current model matrix and individual model quirks are recorded in the project notes.
For measured mutation scores, including the differences between local and frontier agents, see [`mutation-testing-notes.md`](mutation-testing-notes.md).
Those results are the evidence behind the difficulty choices described above.

---

## The main takeaway

`runAgent` is primarily a way to **test the course against different AI agents**.
It helps us find out whether an exercise is too easy, whether our checks detect real mistakes, and whether different models fail in useful ways.

The key rules are straightforward:

- Keep student-facing requirements separate from teacher-only evaluation materials.
- Use generic prompts when measuring what agents discover independently.
- Record output normalization so rescued results remain visible.
- Score generated work with deterministic checks.
- Increase task difficulty when strong agents consistently succeed, rather than making the evaluation less strict.

And remember the one student-facing exception: **`impl-from-spec` lets learners test whether their own specification is clear enough for an agent to implement.**
That's where the tooling becomes part of the lesson itself.