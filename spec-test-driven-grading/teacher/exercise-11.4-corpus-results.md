# Exercise 11.4 — Measuring the Specification Corpus Four Ways

These notes are for teachers only.
We collected **13 specifications of the same pricing feature**, written by six AI models from the same business brief.
Then we evaluated them using every measurement available in this module.
This document explains the results, what they tell us, and where our first interpretations turned out to be wrong.
These are the measurements behind the course slides. The numbers come from reproducible runs in the repository, not from memory.

## 1. Why do we need four measurements?

A specification can look strong under one measurement and weak under another.
That's why we don't rely on a single score.

| Measurement                            | What it tells us                                                                   | What it cannot tell us                                                                         |
|----------------------------------------|------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| **Mutants killed**                     | How much incorrect behavior does the specification explicitly rule out?            | Whether the specification's requirements are actually correct.                                 |
| **Agreement with the reference**       | Did an agent implement the specification in a way that differs from our reference? | Whether the specification is incomplete. A strong agent may fill in missing details correctly. |
| **Open cases — two different readers** | How often do two agents make different decisions from the same specification?      | Whether those differences are intentional. Results can also be distorted by a weak reader.     |
| **Open cases — the same reader twice** | Can two independent runs of one model produce different behavior?                  | Most ambiguity. Agreement between two runs is especially weak evidence.                        |

Each measurement answers a different question.
**The useful picture comes from reading them together.**

---

## 2. The corpus results

The basic tier has **6 reachable mutants** and **8 properties**.
`BestOf` and `OnlyIf` aren't included in the basic brief, so they don't count toward its results.
The advanced tier has **8 mutants** and **10 properties**.

| Specification                         |     Words | Mutants killed | Reference agreement: 32b | Reference agreement: frontier | Open cases: frontier vs. 32b |
|---------------------------------------|----------:|---------------:|-------------------------:|------------------------------:|-----------------------------:|
| `written/claude-code`                 |     2,670 |        **6/6** |                      8/8 |                       **8/8** |                **0 / 4,021** |
| `compressed/claude-code`              |     1,312 |            6/6 |                      8/8 |                           8/8 |                **0 / 4,021** |
| `compressed/ollama-qwen2.5-coder-32b` |       639 |            6/6 |                      7/8 |                           8/8 |              **300 / 4,021** |
| `written/ollama-qwen2.5-coder-32b`    |       487 |            4/6 |                      6/8 |                           6/8 |                  276 / 4,021 |
| `written/ollama-qwen2.5-coder-7b`     |       247 |            3/6 |                      6/8 |                             — |                            — |
| `written/ollama-llama3.2-3b`          |       304 |            3/6 |                      6/8 |                             — |                            — |
| `written/ollama-qwen2.5-coder-1.5b`   |       397 |        **2/6** |                      7/8 |                       **8/8** |              **674 / 4,021** |
| `written/ollama-qwen2.5-coder-14b`    |       648 |            2/6 |                      5/8 |                           5/8 |                  504 / 4,021 |
| `compressed/*` — other four           | 746–2,644 |            6/6 |                      8/8 |                             — |                            — |
| `advanced/ollama-qwen2.5-coder-32b`   |       512 |            6/8 |                     9/10 |                             — |                            — |

The `1.5b` open-case result uses the 32b model as the second reader, just like the rest of that column.
**A dash means we haven't run that comparison. It doesn't mean zero.**
Each frontier implementation required a separate interactive session, so we selected the six comparisons that would answer specific questions rather than running all thirteen.

### How should we read this table?

Start by comparing **mutants killed** with **open cases**.
The `1.5b` specification is the clearest example of why that matters.

When a frontier agent implements it, the result gets **8/8 properties correct** and matches the reference byte-for-byte on all **4,021 probed inputs**.
By that measurement, it looks just as good as the strongest specification in the corpus.

But its claims kill only **2 of 6 mutants**, and two different readers disagree on **674 inputs**.
The specification is weak. The frontier implementer simply made good choices where the text left things open.
**A correct implementation doesn't prove that the specification communicated enough.**

There's another revealing result.
For `compressed/ollama-qwen2.5-coder-32b`, the 32b implementer scored **7/8**, while the frontier implementer scored **8/8**.
That is the only difference between those two reference-agreement columns.
It's a concrete example of a stronger agent making a weaker specification look more complete.

---

## 3. Finding 1 — A worked example doesn't always communicate a requirement

We compared two compressed versions of the same source specification.
Both retained all eight in-tier claims.
Both scored **8/8** on reference agreement.
But their implementations behaved differently on the differential probe.

### Claude's compression kept the general requirement

It explicitly said that multiplying `price * percent` using 32-bit arithmetic can overflow and is incorrect.
It required a `Long` intermediate.

Result:
**0 open cases out of 4,021.**

### The 32b compression kept only an example

It included this expected result:

```text
E14 — the largest shelf price

Int.MAX_VALUE, [Percentage(10)] → 1932735283
```

That's the exact correct answer for that input.
But the specification didn't explicitly require wider arithmetic.

Result:
**300 open cases out of 4,021.**

Every disagreement was near the upper end of the `Int` range.

### Why is this important?

The example wasn't vague.
It gave the correct answer for precisely the kind of input where overflow matters.
And it still didn't communicate the general rule as effectively as one sentence about using `Long`.

We held the reader constant: the same 32b implementer handled both specifications.
It overflowed when given the example-only version and avoided overflow when given the explicit requirement.
So the difference came from **how the specification was written**, not from changing the model.
This connects directly to the examples-versus-properties lesson in exercise 11.2.
An example tells us what should happen for one input.
A general requirement tells us what must happen across a whole class of inputs.

---

## 4. Finding 2 — A false requirement can be understood perfectly

The specification `written/ollama-qwen2.5-coder-14b` contains two incorrect requirements.
Both implementing agents followed them in the same way.

<text weight="medium">Example A — Bonus threshold</text>

```text
Input:
price = 100
[BigSpenderBonus(100, 20)]

Both readers: 100
Reference:     80
```

The specification says a price exactly at the threshold doesn't qualify for the bonus.
Our reference says it does.

<text weight="medium">Example B — Negative prices</text>

```text
Input:
price = 100
[Percentage(150)]

Both readers: -50
Reference:       0
```

The specification allows negative results. Its section 3 even contains an example that continues from −50 to −60.
Our reference clamps the result at zero.

### What happened?

On **1,008 of the 4,021 inputs**, the two readers agreed with each other but disagreed with the reference.
That's an important failure mode.
**A specification can be completely clear and completely wrong.**
Two agents agreeing doesn't prove that the requirement is correct.
They may simply be following the same incorrect instruction.

### Then the false requirement created another problem

The same specification had **504 open cases** where the two readers disagreed.

Almost all of them involved rounding negative prices.
The differences were usually just one unit:

- −1623 versus −1624.
- −125 versus −126.

Why?
The specification said to **round down**.
It also allowed prices to become negative.
Neither statement is necessarily ambiguous by itself.

But together they raise a question the author never addressed: what exactly does “round down” mean when the intermediate result is below zero?
The two readers made different choices.
This is a subtle but valuable lesson.

**One incorrect decision can create new unanswered questions elsewhere in the specification.**
The author didn't simply make a rounding error.
They allowed negative prices, and that choice exposed additional rounding behavior they hadn't considered.

---

## 5. Finding 3 — A guarantee stated for individual rules may leave a gap

Look at `written/ollama-qwen2.5-coder-32b`, section 2, lines 12–14.

It describes three discount rules.
In simplified form:

```text
Percentage:
Reduce the original price by percent, rounding down.

AmountOff:
Subtract the amount, without going below zero.

BigSpenderBonus:
Apply another discount, without going below zero.
```

Notice the difference.
The zero-price floor is explicitly stated for `AmountOff` and `BigSpenderBonus`.
But it isn't stated for `Percentage`.

What should happen if the percentage is greater than 100?
For example:

```text
price = 100
[Percentage(150)]
```

One reader returned **0**.
The other returned **−50**.
That gap accounted for **276 disagreements**.

The reference specification avoids the problem by stating the zero-price floor **once, as a guarantee of the whole `priceFor` function**.
That's property B9.

### The lesson

When a guarantee applies to every rule, it's often safer to state it at the function level.
Otherwise, readers may assume that a guarantee mentioned for two rule types doesn't apply to the third.
**Where you place a requirement changes how it can be interpreted.**

---

## 6. Finding 4 — The frontier implementer followed the text, not our intentions

The frontier model was strong, but it wasn't a magical safety net.
When a specification required `Long` arithmetic, it used `Long`.
When the specification didn't require it, the model could still produce overflow behavior.

For `written/ollama-qwen2.5-coder-32b`, it returned `Int.MAX_VALUE` unchanged on an overflow-sensitive case.
That matched the 32b implementer but differed from the reference.
This is useful for the experiment.

We don't want an agent that silently corrects every weak specification according to what it thinks we intended.
We want to observe what behavior the specification actually communicates.
**The frontier model's value here is that it can follow the text faithfully, including when the text is incomplete or wrong.**

---

## 7. Retraction 1 — Two runs of the same model are not enough

We originally considered measuring ambiguity by running the same frontier model twice.
That sounded reasonable: two independent sessions might make different choices if the specification was unclear.
But the results showed a serious limitation.

For `written/ollama-qwen2.5-coder-1.5b`:

| Comparison                                |      Open cases |
|-------------------------------------------|----------------:|
| Frontier session A vs. frontier session B |   **0 / 4,021** |
| Frontier vs. 32b                          | **674 / 4,021** |

The two frontier sessions produced genuinely different code.
But their behavior was identical on all probed inputs.
Both independently chose `coerceAtLeast(0)` and `Long` arithmetic, even though the specification didn't mention either.
They filled the same gaps with the same defaults.

### What did we get wrong?

We treated two independent sessions as if they were two independent readers.
They weren't.
They were two runs of the same model, with similar training and similar preferences.

The important concept is that **ambiguity is relative to a population of readers**.
If two runs disagree, that proves the specification allows different interpretations.
But if they agree, we haven't established that the text is unambiguous.
We may simply have sampled the same model's preferred interpretation twice.

So we're retracting the idea that same-model agreement can establish completeness.
`verifyDivergence` now warns when it detects that pairing.
**Disagreement is evidence of ambiguity. Agreement is not proof of clarity.**

---

## 8. Retraction 2 — Compression didn't necessarily increase ambiguity

We also thought we'd found evidence that compression made a strong specification less precise.
Using `qwen2.5-coder:14b` as the second reader, we measured:

- **0 open cases** for Claude's long specification.
- **391 open cases** for Claude's compressed specification.

That looked like a clear result.
But when we repeated the comparison with a frontier second reader, the compressed specification had:
**0 open cases.**

The original 391 disagreements came from the weaker reader's mistakes, not from a demonstrated loss of clarity in the compressed text.
This was the same `14b` model that, elsewhere in the corpus, imported `DiscountRule` from the wrong package and failed to compile.

### What did we learn?

It's not enough to use two different readers.
**Both readers need to be competent at the task.**

Otherwise, the divergence score mixes two things:

- Genuine ambiguity in the specification.
- Errors caused by an implementer that struggles with the task.

That makes the number difficult to interpret.
So we're retracting the conclusion that this compression increased ambiguity.
The result didn't survive a stronger comparison.

---

## 9. How to reproduce the measurements

Run these commands from `spec-test-driven-grading/`.

### Layer 1 — Check the specification deterministically

```bash
./gradlew verifySpec -Pspec=fixtures/11.4/written
```

### Layer 2 — Measure claim-extraction accuracy against the key

```bash
./gradlew scoreExtraction -Pagent=corpus32b
```

### Layer 3 — See which properties kill which mutants

```bash
./gradlew pricingPropertyMatrix
```

### Check the value of a declared claim set

```bash
./gradlew verifyClaims -Pclaims=<ids>
```

### Compare implementations with the reference

```bash
./gradlew verifyImplementations -Pagent=<run> --continue
```

This comparison respects the basic and advanced tiers.

### Compare two implementations of the same specification

```bash
./gradlew verifyDivergence -PagentA=<run> -PagentB=<run>
```

This measures where the two implementations behave differently.
Remember that it measures **disagreement between readers**, not correctness by itself.

### What if an implementation doesn't compile?

That's a result too.
Three of the thirteen implementations produced by `impl14b` failed to compile.

The report marks them:

`DID NOT BUILD OR NEVER RAN`

The other nine implementations still receive their scores.
Earlier, a compilation failure prevented the report from being produced, even with `--continue`.
That has been fixed.

The report is now reachable either way through:

`../../spec-test-driven/build-logic/reachable-reports.settings.gradle.kts`

It's still worth using `--continue`, because it lets other Gradle modules keep running.
But the report no longer depends on remembering that flag.

---

## The main takeaway

The corpus gives us four different views of specification quality.
A specification may:

- Kill many mutants but state incorrect requirements.
- Produce an implementation that matches the reference because a strong agent filled in missing details.
- Produce different implementations because it leaves decisions open.
- Produce identical implementations because two agents share the same assumptions, not because the text is complete.

And our own calibration produced two useful corrections.
We learned that two runs of the same model aren't enough to establish clarity, and that a weak second reader can make a good compression look worse than it is.

**The lesson is not to find one perfect score. It's to understand what each measurement proves, what it misses, and whether the conclusion survives a better experiment.**