# Exercise 11.4 — Calibration Notes

These notes are for teachers only.
This document records what happened when we tested exercise 11.4 with different AI models, what we learned about the difficulty of the task, and how those results shaped the advanced tier.
The main calibration run took place on **August 13, 2026**.

We used `runAgent -Pmode=spec` with the updated business brief, after removing hints from the template that made the answers too easy to guess.
We tested the full Ollama model set and also ran Claude Opus 5 interactively in a clean project export.

## 1. What does the business brief leave open?

The brief deliberately doesn't answer every pricing question.
Students and agents need to notice these decisions and make them explicit in their specifications.

The open questions include:

- **Rounding:** Do we round up or down? After every rule, or only once at the end?
- **Rule order:** Does the order of discounts matter?
- **Price basis:** Does each rule use the original shelf price or the running price after previous discounts?
- **Minimum price:** Can the final price go below zero?
- **Bonus threshold:** Does `BigSpenderBonus` check the shelf price or the discounted price?
- **Inclusive boundaries:** Does “from 100” include exactly 100?
- **Repeated rules:** What happens when several rules of the same kind appear together?

The goal is to see whether an agent recognizes these choices, not just whether it produces something that looks like a specification.

---

## 2. How did the models perform?

|   | Agent                |   Lines | What we observed                                                                                                        |
|:--|----------------------|--------:|-------------------------------------------------------------------------------------------------------------------------|
|   | `qwen2.5-coder:1.5b` |      49 | Used examples instead of general rules. Included an arithmetic error and copied explanatory text from the template.     |
|   | `llama3.2:3b`        |      30 | Shallow specification. Mentioned important decisions without actually resolving them.                                   |
|   | `qwen2.5-coder:7b`   |      29 | Mostly plausible rules, but one contradiction about which price to use.                                                 |
|   | `qwen2.5-coder:14b`  |      50 | Mostly plausible rules, but one contradiction about rule order.                                                         |
|   | `qwen2.5-coder:32b`  |      40 | Looked the most complete among the local models, but contained four contradictions.                                     |
|   | **Claude Opus 5**    | **269** | Found **two important decisions every other model missed**, plus another issue we hadn't included in our original list. |

### What went wrong in the smaller models?

The `1.5b` model often replaced general rules with examples.
For instance:

> 10% off: 100 → 90; 50 → 45.

Those examples are correct, but they don't tell us how to round when the result isn't an integer.
The specification also contradicted itself: it gave a bonus to a duck priced at 50 even though the bonus threshold was 100.
Its section 4 discussed algorithmic complexity, and section 5 asked about time complexity.
Those weren't the business decisions the template was asking it to address.

The `3b` model produced a shorter, shallower specification. It named some of the important questions without choosing an answer.
The `7b` and `14b` models wrote plausible rules but contradicted themselves about the price basis and application order, respectively.

The `32b` model was particularly interesting because its specification **looked thorough**.
But it contained four contradictions:

1. Section 2 said percentages use the original price and also the current price.
2. It said rules run in list order but also that the bonus always runs last.
3. Section 2 already required clamping the price, while section 5 still asked whether clamping was needed.
4. Section 2 fixed the bonus order, while section 5 treated that order as unresolved.

A polished-looking specification can still contain incompatible instructions.

---

## 3. What did Claude Opus 5 discover?

The frontier model went noticeably deeper than the local models.
Three findings are especially useful for teaching.

### Finding A: Which price determines the bonus threshold?

Claude noticed that the bonus threshold could be tested against:

- The original `duck.price`.
- The running price at the point where the bonus rule is applied.

It chose the running-price interpretation but explicitly marked that decision as an assumption linked to an open question.
That's good specification-writing practice.
The author makes a decision so implementation can proceed, while also showing which decision may need to change when the business owner responds.

### Finding B: Two rounding formulas are not equivalent

Claude went beyond saying “round down.”
It distinguished between these two calculations:

**Formula 1 — Round the discount amount down:**

\[
p-\left\lfloor\frac{p\times percent}{100}\right\rfloor
\]

**Formula 2 — Round the remaining price down:**

\[
\left\lfloor\frac{p\times(100-percent)}{100}\right\rfloor
\]

They look almost interchangeable.
But for a price of 95 and a 10% discount:

| Calculation                        | Result |
|------------------------------------|-------:|
| Subtract the rounded-down discount | **86** |
| Round down the remaining price     | **85** |

Claude explicitly rejected the first formula as non-conforming.
We verified the difference.
This is exactly the kind of detail that a few ordinary examples may fail to reveal.

### Finding C: Integer overflow matters

Claude also required a wider intermediate type for percentage calculations.
For example, applying a 10% discount to `Int.MAX_VALUE` should produce:

`1932735283`

But a naive calculation using 32-bit `Int` multiplication can overflow before division happens.
Claude required a `Long` intermediate to avoid that problem.

We verified this behavior too.

### A better use of section 4

Claude also used section 4 as intended.
Instead of discussing time complexity, it addressed behavior outside the normal input domain and what parts of the algorithm are observable.
It made an important observation: the rules are **data**, not independently observable operations.

That means there are no intermediate rule calls to spy on.
This was a much more useful analysis than simply describing algorithmic complexity.

---

## 4. Our consistency checker got the ranking backwards

This was the most important finding from calibration.
We tried a simple consistency check:

> Look for a topic that was decided in section 2 but appears again as an open question in section 4 or 5.

The checker flagged:

- Claude on **all four topics**.
- The `32b` model on **three topics**.
- The two weakest models on **none**.

That ranking was almost exactly the opposite of what a careful reader would conclude.

### Why did the weak specifications look clean?

Because they didn't discuss the important topics at all.
One talked about algorithmic complexity.
Another asked about performance.
There was no contradiction for the checker to find because the relevant business decisions were missing.
**Silence made weak work look consistent.**

### Why did Claude look inconsistent?

Because it deliberately connected its assumptions to its open questions.
For example, it would state a working assumption in section 2, then explain in section 5 that the assumption needed confirmation.
It also made clear that if the business owner chose a different answer, both the specification and its tests would need to change.
**That's correct practice.**

Our own template asks authors to do it.
The `32b` model did something different: it stated a rule as settled, then later asked whether that same rule should exist.
That's a genuine contradiction.
But a keyword-based checker couldn't distinguish the two patterns.

### What does this mean for the checking tools?

The checker must distinguish:

- **Acknowledged assumption:** “We're using this interpretation for now, but it needs confirmation.”
- **Unacknowledged contradiction:** “This behavior is required,” followed by “Should we require this behavior?”

Otherwise, it will penalize the best specifications and reward incomplete ones.
We caught this design problem during calibration, before using the checker to evaluate student work.

---

## 5. Is the basic tier difficult enough?

For the local models, yes.
The basic exercise clearly separates stronger and weaker outputs.
But for Claude Opus 5, the task was relatively easy.
That gives us a straightforward design decision.

**When strong agents can handle the task, make the task more challenging rather than weakening the evaluation tool.**
So the composable version of the pricing rules becomes the real advanced tier of exercise 11.4.
It introduces algebraic laws that require more careful reasoning than a list of individual examples.

---

## 6. Advanced-tier laws: What actually holds?

We checked these laws numerically on August 13, 2026.
One correction is important.
An earlier planning note claimed that rounding breaks associativity.

**That was wrong.**
`then` represents function composition, and function composition is associative regardless of rounding.
Here are the verified results.

| Law                                                     | Holds?          | Evidence                                                                          |
|---------------------------------------------------------|-----------------|-----------------------------------------------------------------------------------|
| `then` is associative                                   | **Yes, always** | `(a then b) then c` and `a then (b then c)` apply the same sequence of functions. |
| `then` is commutative                                   | **No**          | At price 100, `10% then −5` gives 85, while `−5 then 10%` gives 86.               |
| Applying discounts one by one equals summing them first | **No**          | Two 10% rules give 78 vs. 77 at price 95, and 117 vs. 115 at price 143.           |
| There is an identity element                            | **Yes**         | `Percentage(0)` leaves every price unchanged.                                     |
| Pricing is monotonic in the input price                 | **No**          | `BigSpenderBonus(100, 20)` maps 99 → 99 but 100 → 80.                             |

### Why are the failed laws especially interesting?

Because some failures only appear for particular inputs.

For commutativity:

- At price 100, the two rule orders produce different results.
- At prices 95 and 37, they produce the same result.

For applying discounts separately versus summing them:

- The results differ at prices 95 and 143.
- They happen to agree at price 37.

So a student could test only prices 95 and 37 for commutativity and conclude that rule order doesn't matter.
That conclusion would be wrong.

**A few passing examples don't establish a general law.**
This is the reason to state laws explicitly and test them across many inputs.
It's also one of the main motivations for property-based testing.

---

# Exercise 11.4b — Calibrating the Compression Task

<text color="secondary">Calibration date: August 13, 2026</text>

The compression task asks students or agents to combine three specifications of the same feature into one.
The goal is:

**Produce a specification that is shorter than the longest source and no weaker than the strongest source.**

For calibration, we used three specifications:

- A frontier-model specification: **269 lines**.
- A `1.5b` specification: **49 lines**.
- A `32b` specification: **40 lines**.

They were anonymized as A, B, and C.
We ran the local models with:

```bash
runAgent -Pmode=spec-compress -PspecsFrom=…
```

We ran the frontier model manually in a clean export.

## 7. Compression results

| Agent                                      | Output lines | Important claims kept (out of 5) | What happened                                                                   |
|--------------------------------------------|-------------:|---------------------------------:|---------------------------------------------------------------------------------|
| Source A — original frontier specification |          269 |                                5 | Starting point                                                                  |
| **Claude Opus 5**                          |      **141** |                            **4** | The only meaningful synthesis                                                   |
| `qwen2.5-coder:32b`                        |          121 |                                3 | Rewrote the text; only 12% verbatim, but removed important content              |
| `qwen2.5-coder:14b`                        |          109 |                                3 | Rewrote the text; 25% verbatim, but removed important content                   |
| `qwen2.5-coder:7b`                         |          188 |                                5 | Copied 93% verbatim from source A                                               |
| `qwen2.5-coder:1.5b`                       |          193 |                                5 | Copied 94% verbatim from source A, with one original line                       |
| `llama3.2:3b`                              |          257 |                                5 | Copied 67% verbatim and produced an output longer than two of its three sources |

### Which five claims were we tracking?

These were the important behavioral decisions the compressed specification needed to preserve:

1. **Rounding formulas differ.** Rounding down the discount amount and rounding down the remaining price give different results: 86 versus 85 at price 95 with a 10% discount.
2. **Large calculations need a wider intermediate type.** Otherwise, integer overflow can occur.
3. **The bonus checks the running price**, not the original shelf price.
4. **The final result is clamped at zero.**
5. **Rule order matters.**

These claims carry important meaning.
Removing one can make the compressed specification shorter while also making it less precise.

---

## 8. What did the compression experiment show?

### Shorter doesn't necessarily mean better

The `14b` and `32b` models produced shorter specifications than Claude.
But both lost the distinction between the two rounding formulas.
That was the subtle behavior only source A had identified.
The models removed important content, not just repetition.
So their outputs were shorter, but weaker.

**Compression is only successful if the important decisions survive.**

### Keeping everything doesn't necessarily mean successful synthesis

The `7b` and `1.5b` models preserved all five claims.
That sounds good until we look at how they did it.
Their outputs were **93% and 94% copied verbatim from source A**.

Each added only one original line.
They hadn't really merged three specifications.
They had mostly copied the longest one.
Neither carried over the two false claims from source B, but only because they effectively ignored B.

The `3b` model also preserved all five claims, with 67% copied verbatim, but its output remained 257 lines long.
That's longer than two of the three original specifications.

### Claude was the only model that really merged the sources

Claude reduced the longest specification from **269 lines to 141**.
It combined material from the sources and identified two incorrect claims in source B.
The first claimed that a duck priced at 50 should receive a `BigSpenderBonus(100, 20)` and end up at 30.
That's wrong.

The duck is below the threshold, so the correct result is **50**.
The second claimed that applying:

```kotlin
listOf(AmountOff(5), Percentage(10))
```

to a price of 100 should give 95.
That's also wrong.
The correct result is **86**.
Claude recognized and removed both errors.

### It also preserved an important example

Before deleting a worked-example table, Claude kept one especially useful result.
For:

```kotlin
listOf(Percentage(10), BigSpenderBonus(100, 20))
```

with a starting price of 100, the result is **90**.
Why does that matter?
Because it distinguishes two interpretations of the bonus threshold.

<box gap={2}>
| Threshold basis | Result |
|---|---:|
| Running price | **90** |
| Original shelf price | **70** |
</box>

After the 10% discount, the running price is 90.
If the bonus checks that running price, the threshold of 100 isn't reached, so the bonus doesn't apply.
If it checks the original shelf price of 100, the bonus does apply.
We verified this example.
It's a compact way to communicate a decision that could otherwise be easy to miss.

### But Claude still lost one important claim

The compressed specification no longer explicitly stated that **rule order matters**.
A reader could derive that fact from its definition of composition.

But there's a difference between:

- A behavior that can be inferred from other rules.
- A behavior the specification states directly.

That distinction is one of the main lessons of the advanced-tier laws exercise.
So even the strongest compression result wasn't perfect.

---

## 9. How should we measure compression quality?

Counting lines is easy.
But how do we measure whether the compressed specification is still strong?

The experiment gives us a useful definition:

> **A claim is load-bearing if the property derived from it kills at least one mutant.**

This connects the compression task to the property-testing tools we already have.
Each claim can be translated into a property.
That property runs against the correct `:core` implementation and the mutant catalog.
If it kills a mutant, it distinguishes correct behavior from at least one incorrect behavior.

We can then evaluate a compressed specification using two measurements:

| Measurement                      | What it tells us                                          |
|----------------------------------|-----------------------------------------------------------|
| **Mutants killed by its claims** | How much distinguishing power the specification preserves |
| **Number of lines**              | How much text it uses                                     |

Now “shorter and no weaker” becomes something we can compare directly rather than relying only on subjective judgment.
A good compressed specification should preserve the ability to rule out incorrect implementations while reducing unnecessary text.
This also connects exercise 11.4b to the test-budget challenge in exercise 11.2.

In both exercises, students have to find a useful balance.
Too little content leaves important behavior unconstrained.
Too much content can repeat information without adding value.

**The goal is not the smallest possible artifact. It's the smallest artifact that still communicates and protects the important decisions.**

---

## The main takeaway

Calibration taught us three things.

**First, strong agents can reveal weaknesses in our evaluation tools.** Claude's careful treatment of assumptions looked like inconsistency to a simple keyword checker. We needed to improve the checker, not penalize the specification.

**Second, examples are not the same as laws.** Two operations can appear equivalent on several inputs and still differ elsewhere. Advanced specifications need to state the general behavior clearly.

**Third, compression must preserve meaning.** A shorter document may lose important rules, while a document that preserves everything may simply be a copy.

These findings are why the advanced tier focuses on composable pricing rules, explicit laws, and measurable preservation of behavior.
The lesson for students is the same one we learned while building the exercise: **don't trust a score, a clean-looking document, or a shorter answer without checking what the measurement actually tells you.**