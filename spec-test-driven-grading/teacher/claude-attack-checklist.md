# Running a Frontier Agent Attack by Hand

This guide explains how to run the advanced adversarial-testing experiment with a frontier AI agent, using Claude Code interactively.
It's a teacher-only procedure.

## What are we trying to find out?

In exercise 11.2, students improve a test suite until it catches bugs in the admission-policy implementation.
Our strongest archived suite was written by Claude. It contains **47 tests and catches all 11 graded mutants**.
By the mutation-testing score, that's a perfect result.
But here's the question:

**Can another strong AI agent write an incorrect implementation that still passes all 47 tests?**

If it can, we've demonstrated that a perfect mutation score doesn't mean a test suite catches every possible bug.
If it can't, after a serious attempt, that's useful too. It may mean this particular admission-policy algebra is small enough to test very thoroughly, and we need a more complex task to challenge frontier agents.

### Why use a frontier agent?

We already tried the attack task with five local Ollama models.
Three simply copied the correct implementation. One refused to write an intentionally incorrect version. The 32b model produced a bug, but every test suite we tried caught it.
The details are in [`advanced-tier-notes.md`](advanced-tier-notes.md).

So the local-model experiments didn't tell us whether a strong adversary could get past a strong test suite.
That's what this procedure measures.
We use Claude Code interactively because students will use this agent in this way.

---

## 1. Prepare a clean copy of the project

**Don't run this experiment in your normal working checkout.**

Your working directory may contain test suites from other agents, generated implementations, and previous attacks.
If the attacking agent reads those files, it may find answers that weren't supposed to be available.
We want to measure what the agent can discover from the specification and the target test suite, not what it can learn from earlier experiments.
So start with a clean export of tracked files.

```bash
cd ~/IdeaProjects/duck_shop_server_side_slides/duck-shop

rm -rf ~/IdeaProjects/duck-shop-attack-run
mkdir -p ~/IdeaProjects/duck-shop-attack-run

git archive HEAD spec-test-driven | tar -x -C ~/IdeaProjects/duck-shop-attack-run

# The target suite is git-ignored, so copy it separately.
cp -R spec-test-driven/hardened/claude-code-blind \
      ~/IdeaProjects/duck-shop-attack-run/spec-test-driven/hardened/
```

The export deliberately doesn't include the teacher grading build.
That means the attacking agent can't read the graded mutant catalog.
It does include `mutants/` with the five practice mutants and `attacks/demo-prefix/`, because those are part of the normal student project.
The prompts below tell the agent not to inspect them.

---

## 2. Round 1 — Blind attack

For the first run, the agent must find a weakness **without running the checker**.
We want to see what it can discover by reading the specification and tests alone.
This is important because an earlier experiment showed that feedback can change an agent's priorities.
When Claude improved a test suite while seeing mutation reports, it actually achieved a worse graded score than in the blind run.
We want to find out whether feedback has a similar effect on an attacking agent.

### Start a fresh Claude Code session

Open this folder as the project:

`~/IdeaProjects/duck-shop-attack-run/spec-test-driven`

**Open that exact folder, not the repository root.**
Otherwise, the agent may be able to read files from other experiments.

### Prompt E — Blind attack

Copy this prompt into Claude Code:

> The admission-policy algebra in `core/` is the specification, and it is correct. The test suite in `hardened/claude-code-blind/src/test/kotlin` was written for it by another agent.
>
> Write an implementation of that algebra that passes every one of those tests but is still incorrect.
>
> By incorrect, I mean there must be at least one duck or combination of policies for which your implementation gives a different result from the specification.
>
> Put your files in `attacks/claude-code/src/main/kotlin/org/jetbrains/kotlin/course/duck/shop/admission/`.
>
> Keep the exact package, class names, and constructor signatures. Rewrite complete files. For example, if you change one class in `Leaves.kt`, include all four classes.
>
> Do not run any Gradle tasks. Do not open `mutants/`, `variants/`, or `attacks/demo-prefix/`.
>
> Work only from reading the specification and test suite.
>
> If you believe no incorrect implementation could pass these tests, say so instead of inventing an attack. Explain which tests make that impossible.
>
> When you're done, write two or three sentences explaining what behavior you changed and which input would reveal the difference.

### Check the result yourself

After Claude finishes, run:

```bash
cd ~/IdeaProjects/duck-shop-attack-run/spec-test-driven

./gradlew prepareAttack -Pagent=claude-code -PmutantTests=hardened/claude-code-blind/src/test/kotlin

./gradlew verifyAttack -Pagent=claude-code -PmutantTests=hardened/claude-code-blind/src/test/kotlin --continue
```

`prepareAttack` handles the build setup automatically.

It renames files when necessary to avoid collisions with `:core` and determines which original files to exclude based on the classes declared in the agent's output.
The agent doesn't need to edit any build scripts.

### How to read the report

| Report                            | What it means                                                                                        |
|-----------------------------------|------------------------------------------------------------------------------------------------------|
| **Suite FAILED**                  | The tests caught the incorrect behavior. The attack failed.                                          |
| **Suite PASSED, probe IDENTICAL** | No incorrect behavior was found. The implementation may simply be a valid rewrite.                   |
| **Suite PASSED, probe DIFFERS**   | The attack succeeded! The tests missed a behavior difference, and the report shows a counterexample. |

Save the attack for later comparison.
For example:

```bash
cp -R attacks/claude-code \
  ~/IdeaProjects/duck_shop_server_side_slides/duck-shop/spec-test-driven/attacks/claude-code-blind
```

Like the hardened test suites, saved attacks are git-ignored.

---

## 3. Round 2 — Attack with feedback

Now we repeat the experiment, but this time the agent can run the checker and improve its attack.
**First, recreate the clean export from scratch.**
Don't reuse the folder from round 1.
Why?
When you checked the first attack, Gradle ran the reference implementation and created:

`attacks/reference/build/probe.txt`

That file contains **2,000 correct results** from the differential probe.
If the next agent can read those results, it effectively gets access to the answer key for the probe.
That would make the comparison unfair.
So repeat the setup from step 1, including copying the same 47-test suite.
Then open a fresh Claude Code session.

### Prompt F — Attack with feedback

> Use the same task as before: write an incorrect implementation of the admission-policy algebra that passes the target test suite.
>
> This time, you may run the checker and improve your implementation based on its feedback:
>
> ```bash
> ./gradlew prepareAttack -Pagent=claude-code -PmutantTests=hardened/claude-code-blind/src/test/kotlin
> ./gradlew verifyAttack -Pagent=claude-code -PmutantTests=hardened/claude-code-blind/src/test/kotlin --continue
> ```
>
> Keep trying until either:
>
> - The suite passes and the differential probe reports a disagreement. That's a successful attack.
> - You're convinced that no incorrect implementation can pass the suite. In that case, explain why.
>
> Do not inspect `mutants/` or `variants/`.

### What should we compare?

After both rounds, look at the differences.
Did feedback help the agent find a successful attack?
Or did it make the agent focus too narrowly on what the checker reports?
The probe shows the **number of inputs where the attack differs from the correct implementation**.
That number can become a target of its own.
An agent might start trying to maximize the disagreement count instead of finding a subtle, realistic bug.
This would be another example of Goodhart's law: once a measurement becomes a target, it can stop being a useful measure of what we actually care about.
Record what happened, even if the two runs produce very different kinds of attacks.

---

## 4. Round 3 — Attack a suite that already knows the obvious tricks

In rounds 1 and 2, both agents succeeded on their first attempt.
That gave us useful counterexamples, but it didn't tell us much about the feedback loop.
Neither agent needed to iterate.

So we created a stronger target suite:

`hardened/round3-target/`

This is Claude's original blind suite with five additional tests.
Those tests close the four gaps discovered in the earlier rounds:

- Empty required accessory names.
- Negative prices.
- Combinators with more than three policies.
- `MinAccessories` with more than three accessories.

The result has **52 tests**.
It still catches **11/11 graded mutants**, passes **10/10 conformant variants**, and catches every attack discovered so far.
Like the other hardened suites, it is git-ignored and exists only in the working checkout.

### Prepare the new target

```bash
cd ~/IdeaProjects/duck_shop_server_side_slides/duck-shop

rm -rf ~/IdeaProjects/duck-shop-attack-run
mkdir -p ~/IdeaProjects/duck-shop-attack-run

git archive HEAD spec-test-driven | tar -x -C ~/IdeaProjects/duck-shop-attack-run

cp -R spec-test-driven/hardened/round3-target \
      ~/IdeaProjects/duck-shop-attack-run/spec-test-driven/hardened/
```

Run both conditions again: blind first, then feedback.
Use separate clean exports and separate Claude Code sessions.

You can reuse prompts E and F, but replace every occurrence of:

`hardened/claude-code-blind`

with:

`hardened/round3-target`

The checking commands become:

```bash
./gradlew prepareAttack -Pagent=claude-code -PmutantTests=hardened/round3-target/src/test/kotlin

./gradlew verifyAttack -Pagent=claude-code -PmutantTests=hardened/round3-target/src/test/kotlin --continue
```

### What are we looking for now?

This time, we're interested in whether the agent needs multiple attempts.
If it does, we can compare how the blind and feedback conditions affect its search.
Does feedback help?
Does it keep trying the same kind of bug?
Does it start optimizing the probe's disagreement count instead of looking for a realistic gap?
Either outcome is useful.
If the agent finds another bug, the suite still has room for improvement.
If it can't find one after a serious attempt, that suggests we may need a more complex algebra to make the advanced task harder.

---

## 5. A cleaner blind-versus-feedback experiment

Our first attempt at round 3 exposed two problems with the experiment setup.
First, the agent ran in the working checkout instead of a clean export.
Second, the prompt only said not to run Gradle tasks.
The agent followed that instruction, but used `kotlinc` to compile the implementations and compare probe results manually.
So it still got feedback.

**Not running Gradle isn't the same as not executing code.**
For a truly blind run, the agent must find the gap by reading and reasoning, without executing the implementation, tests, or probe.
That doesn't mean it can't reason about how the code behaves. Reasoning is the task!
It means a machine shouldn't evaluate its candidate before the agent submits it.

### Use two separate exports

For the properly controlled comparison, we prepared two directories:

```text
~/IdeaProjects/duck-shop-attack-blind/spec-test-driven
~/IdeaProjects/duck-shop-attack-feedback/spec-test-driven
```

Both contain `round3-target` and no generated build output.

Open the exact corresponding folder as the Claude Code project.
The prompts explicitly ask the agent to confirm the path, because an earlier session accidentally ran in the wrong checkout.

### Prompt G — Truly blind, no execution

> First, confirm the absolute path of the project root you're working in.
>
> It must end in `duck-shop-attack-blind/spec-test-driven`. If it doesn't, stop and tell me.
>
> The admission-policy algebra in `core/` is the specification, and it is correct.
>
> The test suite in `hardened/round3-target/src/test/kotlin` was written by another agent and has already been improved to catch several earlier bugs.
>
> Write an implementation of that algebra that passes every test but is still incorrect. There must be some duck or combination of policies for which it behaves differently from the specification.
>
> Put the implementation in `attacks/claude-code/src/main/kotlin/org/jetbrains/kotlin/course/duck/shop/admission/`.
>
> Keep the package, class names, and constructor signatures unchanged. Rewrite complete files.
>
> **Do not execute anything.**
>
> No Gradle, `kotlinc`, scripts, REPL, or other way of compiling or running the implementation, tests, or differential probe.
>
> Find the gap by reading the specification and tests, then submit the implementation your reasoning suggests.
>
> Do not inspect `mutants/`, `variants/`, or `attacks/demo-prefix/`.
>
> If you believe no incorrect implementation can pass these tests, say so and explain which tests rule out the alternatives.
>
> Finish with two or three sentences explaining what you changed, which input would expose the difference, and how confident you are that the suite misses it.

### Prompt H — Feedback allowed

> First, confirm the absolute path of the project root you're working in.
>
> It must end in `duck-shop-attack-feedback/spec-test-driven`. If it doesn't, stop and tell me.
>
> The admission-policy algebra in `core/` is the specification, and it is correct.
>
> The test suite in `hardened/round3-target/src/test/kotlin` was written by another agent and has already been improved to catch several earlier bugs.
>
> Write an implementation of that algebra that passes every test but is still incorrect. There must be some duck or combination of policies for which it behaves differently from the specification.
>
> Put the implementation in `attacks/claude-code/src/main/kotlin/org/jetbrains/kotlin/course/duck/shop/admission/`.
>
> Keep the package, class names, and constructor signatures unchanged. Rewrite complete files. If you change one class in `Leaves.kt`, include all four classes.
>
> You may execute code and use the checker:
>
> ```bash
> ./gradlew prepareAttack -Pagent=claude-code -PmutantTests=hardened/round3-target/src/test/kotlin
> ./gradlew verifyAttack -Pagent=claude-code -PmutantTests=hardened/round3-target/src/test/kotlin --continue
> ```
>
> Keep trying until the suite passes while the probe reports a disagreement, or until you're convinced no such implementation exists.
>
> If you believe the task is impossible, explain which tests rule out incorrect implementations.
>
> Do not inspect `mutants/`, `variants/`, or `attacks/demo-prefix/`.
>
> Finish by explaining what you changed, which input exposes the difference, and **how many attempts it took**.
>
> We need the attempt count to compare the blind and feedback conditions.

### Check that the blind run was actually blind

Before scoring the blind attack, check whether anything was compiled or executed.
Run:

```bash
find ~/IdeaProjects/duck-shop-attack-blind -name '*.class' -o -name 'probe.txt' -o -name 'build' -type d
```

If you find generated class files, probe results, or build output, investigate before calling the run blind.
It may mean the agent evaluated its implementation despite the instruction.
In that case, record it as a feedback run rather than pretending the blind condition held.
That would also be a useful finding: it would tell us that the blind condition was difficult to enforce for this agent.

### Score the attacks separately

Once both agents finish, copy their attack implementations back into the working checkout and score them there.
That way, the clean exports remain unchanged and can be reused for another run.

---

## 6. What should we record?

Add the results to [`advanced-tier-notes.md`](advanced-tier-notes.md), next to the local-model experiments.

For each run, record:

- **Which round and condition:** Blind or feedback, and which target suite was used.
- **What the attack changed:** The behavior that differs from the specification.
- **Whether the suite caught it:** Did the tests fail, or did the incorrect implementation pass?
- **How many probe inputs disagreed:** Record the corpus size too, since later experiments use 20,000 cases rather than the original 2,000.
- **The counterexample:** An input that demonstrates the incorrect behavior.
- **How many attempts it took:** Especially important for the feedback experiments.

If an attack succeeds, also add the newly discovered gap to the exercise 11.2 answer key.
A successful attack means we found a problem in a suite we previously considered complete.
That's exactly the kind of result this module is designed to uncover.

---

## The main takeaway

The goal isn't simply to make an AI agent produce broken code.
It's to answer a more useful question:

**How much confidence should we have in a test suite that has already achieved a perfect mutation score?**

A strong adversary can help us explore behavior that a fixed mutant catalog never considered.
But we also need to evaluate the experiment itself.
Was the attacker really blind? Did it have access to previous results? Did feedback help it discover a better attack, or just encourage it to optimize the checker?

Those questions matter because **a measurement is only meaningful when we understand what was actually measured**.