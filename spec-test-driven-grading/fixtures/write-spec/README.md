# 11.4 Fixtures — Specifications Written by AI Agents

This folder contains specifications written by different AI agents for exercise 11.4.
**Unlike most other AI-generated files in this repository, these files are committed to Git.**

Why?
Most generated test suites, implementations, and attacks are git-ignored because we can easily reproduce them using Ollama.
But some specifications here were written by frontier models in separate sessions. 
Reproducing them would require API access we don't currently have.
We don't want to lose those results, especially because they include some of the strongest specifications we've collected.

## Why do we keep these files?

We use them for three things.

### 1. Materials for exercise 11.4b

In exercise 11.4b, students are asked to take several specifications and combine them into one that is **shorter but doesn't lose any important information**.
For this exercise, we need examples of different quality, from weak specifications to very strong ones.
That's exactly what this folder provides!

It also means that students can do the exercise regardless of which AI model they have access to.
A student using a small local model can still work with a specification written by a frontier model. 
And a student using a frontier model still has to figure out what to do with a weak specification.
**Important: Only share `written/` when students reach exercise 11.4b!**

In exercise 11.4a, students should write their own specification using only the business description.
If they see these examples too early, they'll start reviewing existing specifications instead of learning to write one themselves.

### 2. Testing our specification checkers

We also use these files to evaluate the tools that check students' specifications.
In particular, we want to know two things:

- **Consistency:** If we check the same specification several times, do we get the same result?
- **Discrimination:** Can the checker tell the difference between a vague specification and a precise one?

These specifications are useful because they were created under the same conditions but vary a lot in quality.
We originally planned to write examples manually for this purpose, but our experiments gave us a better set of real AI-generated examples!

### 3. Checking for regressions

Once our specification checker is ready, we can run it on all these files.
Then we can compare its results with our manual evaluation in [`../../teacher/write-spec-calibration.md`](../../teacher/write-spec-calibration.md).
The checker should rank the specifications in the same order as our manual evaluation.
If it doesn't, we need to investigate why.
We've already seen how this can go wrong: our first checker simply looked for certain keywords, and it ranked the **best specification last**!
So these files help us make sure that improvements to the checker don't accidentally make it worse.

## What's inside?

### `written/` — Original specifications (exercise 11.4a)

These are specifications written by different AI agents using only the original business description.
There's one specification per agent.
The results vary quite a lot:

- **29 lines:** A specification from the 7b model. It looks reasonable at first, but some of its rules contradict each other.
- **269 lines:** A specification from a frontier model. It was the only one that identified the ambiguity about which price a threshold should use, two possible approaches to rounding, and the overflow problem.

One especially useful example is `ollama-qwen2.5-coder-1.5b.md`.
It's a good example of a weak specification because it mostly gives specific examples instead of general rules.
As a result, it never properly explains how rounding should work.

### `advanced/` — Advanced specifications

These are specifications for the advanced version of exercise 11.4, where discount rules can be combined and students also need to describe general properties (laws).
So far, we only have a result from the 32b model.
It proposed four laws, but:

- Three were trivial or repeated the same idea.
- One was actually true, but the model incorrectly said it was false.
- Its counterexample didn't demonstrate a failure because neither side changed the result.

We still need to run this version with a frontier model.

### `compressed/` — Combining specifications (exercise 11.4b)

In this exercise, agents were given three specifications from `written/`:

- A 269-line frontier specification.
- A 49-line specification.
- A 40-line specification.

Their task was to combine them into one specification that was **shorter without losing important information**.
None of the agents managed to do this successfully.
The resulting specifications ranged from 109 to 257 lines.
But the interesting part is *how* they failed.

**The 1.5b and 7b models mostly copied the frontier specification.**
Around 93–94% of their output was copied word for word, with only one original line each.
That's not really combining specifications. It's mostly copying one of them!

**The 14b and 32b models actually tried to rewrite and combine the information.**
Only 12–25% of their output was copied word for word, and they added 32–48 original lines.
But their final specifications were still 109 and 121 lines long, around four times the amount of substantive content they needed to preserve.
So they did more real rewriting, but they still struggled to make the result concise.

## What can we learn from this?

One result is especially interesting:
**Giving a small model a great specification doesn't mean it knows how to improve or combine specifications.**
In our experiments, the smallest models mostly copied the frontier model's work instead of deciding what was important and what could be removed.
And even the larger local models struggled to make the specification shorter without adding unnecessary text.
This is a useful lesson for students: **having a good example isn't the same as knowing how to use it.**
That's why this exercise isn't just about asking an AI agent to write or combine specifications.
It's about learning how to evaluate what the agent produces, recognize missing information, and decide which details really matter.