# Running a Frontier Agent as the Implementer (Exercise 11.4)

These notes are for teachers only.
This guide explains how to ask a strong AI agent to implement `priceFor` from a specification, then compare its result with implementations produced by other agents.

## Why are we running this experiment?

We've already tested our specification corpus using `qwen2.5-coder:32b` as the implementer.
That tells us how different specifications work when **the same mid-sized model reads them**.
But what happens if we give those specifications to a much stronger agent?
That's a different question.
A strong agent might notice that a specification is incomplete and fill in the missing details using reasonable defaults.
If those defaults happen to match our reference implementation, even a weak specification could produce correct code.
And that's exactly what we observed.

**One specification of just 397 words went from 7/8 to 8/8 correct behaviors when a stronger agent implemented it.**
The resulting implementation also matched the reference on **all 4,021 probed inputs**.
So the differences we saw with the mid-sized model didn't necessarily survive when we used a stronger reader.
This is why exercise 11.4 doesn't rely only on whether an implementation matches our reference.

**We also compare what two different agents produce from the same specification.**
That helps us see whether the specification communicates its decisions clearly or whether the agent is filling in missing information on its own.

---

## 1. Use the same workflow students use

An earlier version of this experiment involved copying a specially prepared prompt into a chat window without giving the agent access to project files.
**Don't use that approach.**
It doesn't reflect how students actually work.

In the exercise, a student opens an AI agent inside a project and asks it to implement code from `SPEC.md`.
We should measure that real workflow, not an artificial version of it.
So use the sandbox, just as the student slides describe.

### Create a sandbox

From `spec-test-driven/`, run:

```bash
cd spec-test-driven

./gradlew sandbox -Pname=frontier-a -Pspec=<the specification>
```

This prepares a separate working folder for the agent.

### Open the agent inside the sandbox

```bash
cd sandbox/frontier-a
claude
```

Then give the agent a simple instruction, such as:

> Implement `priceFor` in `Pricing.kt` using the specification in `SPEC.md`.

That's all it needs.
Don't give it a long explanation of the business rules. We want to find out what it understands from the specification itself.

## 2. Three things to watch while the agent works

### Don't answer questions about missing requirements

The agent may ask something like:

> Should this promotion use the original price or the current price?

Don't choose an answer for it.
Instead, tell it to make its own decision and continue.
**The fact that it needed to ask is already useful evidence about the specification.**
If you answer, the final implementation reflects both the specification and your additional instructions.
That would change what we're measuring.

### Don't give it the original business description

The original business text now lives in `briefs/` inside the teacher build.
It's no longer available inside the student project.
That's deliberate.

Previously, an agent could potentially find the original requirements and use them to fill gaps in `SPEC.md`.
Now the project layout prevents that particular information leak.
Keep the agent inside its sandbox and don't paste the missing rules into the conversation.

### Watch for changes to the properties

Sometimes an agent may try to change the properties or tests instead of writing code that satisfies them.
Don't silently ignore that.
The next sandbox run will report the changes.
**That's a finding about how the agent approached the task**, not just an inconvenience to clean up.

---

## 3. Collect the implementation

When the agent finishes, return to the project and run:

```bash
./gradlew prepareImplementation -Pagent=frontier-a \
    -Pspec=<the specification> -Pfrom=sandbox/frontier-a
```

**Use the same `-Pspec` value you used when creating the sandbox.**
The tool uses it to identify which specification the implementation belongs to.

Now switch to the teacher grading build and run:

```bash
./gradlew verifyImplementations -Pagent=frontier-a --continue

./gradlew verifyDivergence -PagentA=frontier-a -PagentB=impl32b
```

The first command checks the implementation.
The second compares the frontier agent's implementation with the one produced by the 32b model.

### What if the implementation doesn't compile?

**That's still a valid experimental result.**
For example, three of the thirteen implementations produced by `impl14b` didn't compile.

The report marks them as:

`DID NOT BUILD OR NEVER RAN`

It still reports results for the other nine implementations.
This wasn't always handled correctly. An earlier version of the checker stopped after a compilation failure and printed no useful results.
We fixed that because a failed build is something we want to measure, not something that should hide the rest of the experiment.

---

## 4. Using a local model instead

If you're working with an API-driven model, you don't need to create a sandbox or open an interactive agent.
One command handles the whole process:

```bash
./gradlew runAgent -Pmode=impl-from-spec -Pagent=reader-b \
    -Pprovider=ollama -Pmodel=qwen2.5-coder:32b -Pspec=<the specification>
```

You can run this from either build.
The command writes the implementation module directly.
**The sandbox is specifically for interactive agents**, such as Claude Code, that need a project folder where they can read and edit files.

---

## 5. How to interpret the results

A difference between two implementations doesn't automatically mean one is buggy.
It means they disagree.

That disagreement might happen because:

- The specification left a question unanswered, and the agents made different choices.
- The specification explicitly required behavior different from our reference, and an agent followed it correctly.
- An agent misunderstood or ignored something the specification did say.

**Always read the specification before calling a divergence a defect.**

### Basic and advanced behavior

When checking a basic-tier specification, the tools automatically exclude properties that belong only to the advanced tier.

For example:

- `BestOf`
- `OnlyIf`

Students working on the basic tier never received those requirements.
So it wouldn't be fair to penalize their specifications for not describing them.

### Example 1: A claim was present, but the implementation still got it wrong

The specification from `qwen1.5b` claimed to handle integer overflow safely.
Our specification checker counted that claim.

But the only example it gave was:

`AmountOff(1)` applied to `Int.MAX_VALUE`.

That example never multiplies anything, so it doesn't actually demonstrate the dangerous case.
The implementation then calculated:

```kotlin
price * percent
```

using `Int`, which can overflow.
So the specification contained the right claim, and our checker counted it, but **the claim wasn't clear or concrete enough to produce the intended implementation**.
This is a useful reminder that mentioning a requirement isn't the same as communicating it effectively.

### Example 2: The agent ignored an explicit decision

The `qwen7b` specification said that the bonus should use the duck's original *shelf price*.
But the implementing agent used the running price instead.
Here, the specification did make a decision.

The implementation simply didn't follow it.
So not every disagreement is caused by an incomplete specification.
Sometimes the reader gets something wrong even when the text is clear.

---

## 6. What should we compare the results against?

We have two different measurements for the basic-tier specifications.
Both are useful, but they answer different questions.

### Measurement A: How correctly did the agent implement each specification?

We gave each specification to `qwen2.5-coder:32b` and checked the resulting implementation.
Advanced-tier requirements were excluded.

| Specification | Correct behaviors |
|---------------|------------------:|
| Claude        |               8/8 |
| `qwen1.5b`    |               7/8 |
| `llama3b`     |               6/8 |
| `qwen32b`     |               6/8 |
| `qwen7b`      |               6/8 |
| `qwen14b`     |               5/8 |

By this measure, `qwen1.5b` looks surprisingly good.
Its specification is short and incomplete, but the implementing agent made reasonable decisions about most of the missing details.

### Measurement B: How many defects can the specification's claims catch?

Now look at a different question.
Instead of asking an agent to implement the specification, we check how many known defects its stated requirements can rule out.

| Specification | Defects caught |
|---------------|---------------:|
| Claude        |            6/6 |
| `qwen32b`     |            4/6 |
| `llama3b`     |            3/6 |
| `qwen7b`      |            3/6 |
| `qwen1.5b`    |            2/6 |
| `qwen14b`     |            2/6 |

Here, `qwen1.5b` is tied for last.
That's not a mistake in the measurements.
**The two checks measure different things.**

### Why do the rankings disagree?

When we check the implementation, an unanswered question doesn't necessarily cause a failure.
A capable agent can fill the gap with a sensible default.
If that default matches the reference, the implementation gets the right result.

So **silence can be almost free in implementation testing**.
But when we check what a specification explicitly guarantees, silence matters a lot.
If the specification never states a rule, we can't use it to reject an implementation that violates that rule.

So **silence is expensive when measuring how well a specification constrains behavior**.
This explains the surprising result for `qwen1.5b`.
Its specification produced an implementation that matched the reference on 7/8 behaviors, yet its claims caught only 2/6 known defects.

The implementation looked good partly because the agent supplied missing decisions.
We call this **faithful by vacuity**: the implementation can appear faithful because the specification says too little to constrain it.
Neither score describes the quality of a specification by itself.

For the complete comparison and measured results, see [`exercise-11.4-corpus-results.md`](exercise-11.4-corpus-results.md).

---

## The main takeaway

**A good implementation doesn't prove that the specification was good.**
A strong AI agent can sometimes compensate for missing requirements and produce code that looks exactly right.
But another agent might make different decisions from the same text.

That's why we need both kinds of evidence:

- Does the specification explicitly rule out incorrect behavior?
- Do different agents interpret and implement it consistently?

The goal of exercise 11.4 is to teach students to write specifications that communicate decisions clearly enough that the result doesn't depend on which agent happens to read them.