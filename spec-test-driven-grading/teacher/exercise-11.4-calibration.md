# Exercise 11.4 — calibration notes

Run 2026-08-13, `runAgent -Pmode=spec`, on the brief **after** the giveaway hints were removed from
the template. The whole Ollama matrix plus Claude Opus 5 driven interactively in a clean export.

## What the brief deliberately leaves undetermined

Rounding (direction, and per step or once at the end) · order of application · does each rule see the
shelf price or the running price · can the price go below zero · is the big-spender threshold tested
against the shelf price or the discounted one · is "from 100" inclusive · several rules of the same
kind.

## Results

| Agent | Lines | What happened |
| --- | --- | --- |
| qwen2.5-coder:1.5b | 49 | **examples instead of rules** — "10% off: 100 → 90; 50 → 45", so rounding never surfaces. Arithmetic error contradicting its own §3 (a 50 duck given the bonus at threshold 100). §4 holds "algorithmic complexity", §5 asks about time complexity. Copied the template's explanatory prose into the answer |
| llama3.2:3b | 30 | shallow; touches the forks by name without resolving them |
| qwen2.5-coder:7b | 29 | plausible rules, one contradiction (basis) |
| qwen2.5-coder:14b | 50 | plausible rules, one contradiction (order) |
| qwen2.5-coder:32b | 40 | most complete-looking, **four contradictions** — §2 says the percentage is of the *original* price and also of the *current* price; rules apply "in list order" and the bonus "applies last"; §5 asks whether to clamp after §2 clamped; §5 asks bonus ordering after §2 fixed it |
| **Claude Opus 5** | **269** | found **both** forks nobody else did, and one we had not listed |

## The frontier run, in detail

Found the **threshold basis** fork (bonus tested against the price the rule is applied to, not
`duck.price`) and flagged it as an assumption tied to an open question.

On rounding it went past "round down" to distinguish two formulations that look equivalent:
subtracting `floor(p × percent / 100)` versus `floor(p × (100 − percent) / 100)`. At price 95 and 10%
these give **86 and 85**. It declared the second non-conforming. Verified.

It also required a `Long` intermediate: `Int.MAX_VALUE` at 10% is 1932735283, and naive 32-bit
multiplication overflows. Verified.

Its §4 is what §4 is meant to be: behaviour outside the input domain, and the algorithm itself — with
the observation that there are no observable intermediate values because the rules are *data*, so
there is nothing to spy on.

## The finding that matters most: our consistency check ranks it backwards

A scan for "a topic decided in §2 and raised again in §4/§5" flags **Claude on all four topics**, 32b
on three, and gives the two weakest models a clean sheet.

That is exactly wrong, for two different reasons:

- The weak specs are "clean" because they never mention our topics at all — §4 discusses algorithmic
  complexity and §5 discusses performance, so there is nothing to collide.
- Claude cross-lists deliberately and says so: every open question carries the assumption written into
  §2, and if the answer comes back different, §2 changes and the tests with it. **That is the correct
  practice**, and our own template asks for it. 32b's version — state the clamp, then ask whether to
  clamp — is a contradiction. They look identical to a keyword scan.

**So the checker must distinguish an acknowledged assumption from an unacknowledged contradiction, or
it will reject the best work.** Design constraint for the checking machinery, caught during
calibration rather than on students.

## Verdict on difficulty

The basic tier discriminates the local matrix well and is **easy for the frontier**. Per the standing
rule — if strong agents cope, escalate the task rather than degrade the instrument — the composable
version becomes the real advanced tier of 11.4.

## Laws for the advanced tier — verified numerically, 2026-08-13

An earlier note in the plan claimed *rounding breaks associativity*. **That was wrong** and is
corrected here: `then` is function composition, which is associative whatever the rounding does.

| Law | Holds? | Evidence |
| --- | --- | --- |
| `then` associative | **yes**, always | composition; `(a then b) then c` and `a then (b then c)` are the same sequence |
| `then` commutative | **no** | `10% then −5` vs `−5 then 10%`: at 100 gives 85 vs 86 — **but equal at 95 and at 37** |
| one-by-one vs summing the discounts | **no** | two 10% rules: 95 → 78 vs 77, 143 → 117 vs 115 — **but equal at 37** |
| identity element | **yes** | `Percentage(0)` leaves every price unchanged |
| monotonic in price | **no** | `BigSpenderBonus(100, 20)`: 99 → 99 but 100 → 80 |

The best of these is that **two of the three broken laws are broken only sometimes**. Check
commutativity on 95 and 37 and you will conclude that order does not matter, and be wrong at 100.
That is the argument for stating a law instead of spot-checking it, and it is what property-based
testing exists for.

---

# 11.4b — the compression task, calibrated 2026-08-13

Task: given three specs of the same feature (269, 49 and 40 lines — the frontier's, 1.5b's and
32b's, anonymised as A/B/C), produce one that is **shorter than the longest and no weaker than the
best**. Run with `runAgent -Pmode=spec-compress -PspecsFrom=…` and, for the frontier, by hand in a
clean export.

## Results

| Agent | Lines | Load-bearing claims kept (of 5) | How it got there |
| --- | --- | --- | --- |
| source A (the frontier's original) | 269 | 5 | — |
| **Claude Opus 5** | **141** | **4** | the only real synthesis |
| qwen2.5-coder:32b | 121 | 3 | rewrote (12% verbatim), cut into the content |
| qwen2.5-coder:14b | 109 | 3 | rewrote (25% verbatim), cut into the content |
| qwen2.5-coder:7b | 188 | 5 | **93% verbatim from A** |
| qwen2.5-coder:1.5b | 193 | 5 | **94% verbatim from A**, one original line |
| llama3.2:3b | 257 | 5 | 67% verbatim, and longer than two of its three sources |

The five load-bearing claims: the two rounding formulations differ (`floor(p × percent / 100)` versus
`floor(p × (100 − percent) / 100)` — 86 versus 85 at price 95) · a wider intermediate is needed or
large prices overflow · the bonus tests the running price, not the shelf price · the result is clamped
at zero · rule order matters.

## What it shows

**Shorter is not better, and the difference is measurable.** The two models that came out shorter than
the frontier both dropped the two-rounding-formulations point — the single thing only source A had
found. They compressed by cutting content, not repetition.

**The three that kept everything did so by transcription.** 93–94% of their lines are A verbatim, one
original line apiece. They did not merge; they retyped the longest source. None of them propagated B's
two false claims — but only because they never read B.

**The frontier is the only one that actually merged**: 269 → 141, and it found and dropped both of B's
errors. Verified: "price 50 with `BigSpenderBonus(100, 20)` → 30" is wrong (50 is below the threshold,
the answer is 50), and "`[AmountOff(5), Percentage(10)]` on 100 → 95" is wrong (the answer is 86). It
also carried one number out of the worked-example table before deleting the table — `[Percentage(10),
BigSpenderBonus(100, 20)]` on 100 → **90**, which is the input that distinguishes testing the threshold
against the running price (90) from testing it against the shelf price (70). Verified.

It lost one thing: the explicit statement that **rule order matters**. Derivable from its own
definition of composition — but derivable and stated are not the same, which is the distinction the
laws section of the advanced tier exists to teach.

## The metric this hands us for the checker

Length is trivial to measure. The other half now has a definition that needs no model:

> **A claim is load-bearing if the property derived from it kills at least one mutant.**

That reuses the machinery already standing and matches the decision that properties run against
`:core` and against the mutants. A compressed spec is then scored on two numbers — **mutants its
claims still kill**, and **lines** — and "shorter and no weaker" becomes an ordering rather than a
judgement call. It also gives the exercise the same shape as the test budget in 11.2: derive your own
target, and defend it against both extremes.
