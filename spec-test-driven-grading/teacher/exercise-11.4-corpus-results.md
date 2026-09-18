# 11.4 — the corpus, measured four ways

Teacher-only. Thirteen specifications of the same feature, written by six models from the same brief,
put through every check the module has. This is the evidence behind the slides; the numbers here are
reproducible from the repository, not quoted from memory.

## The four axes, and why no single one is enough

| axis | question it answers | what it cannot see |
| --- | --- | --- |
| **mutants killed** | how much does the text *pin*? | nothing about whether the text is *right* |
| **agreement with the reference** | did an implementer get it *wrong*? | thinness — it collapses as the implementer gets stronger |
| **cases left open** (two readers) | how much did the text leave to *chance*? | whether that was on purpose; and it inherits the weaker reader's mistakes |
| **cases left open** (one reader twice) | a lower bound on ambiguity | almost everything — see below |

## The table

Basic tier: 6 mutants are reachable (`BestOf` and `OnlyIf` are not in that brief), and 8 properties
are in tier. Advanced tier: 8 and 10.

| specification | words | mutants killed | vs reference (32b) | vs reference (frontier) | open, frontier vs 32b |
| --- | --- | --- | --- | --- | --- |
| `written/claude-code` | 2670 | **6/6** | 8/8 | **8/8** | **0** / 4021 |
| `compressed/claude-code` | 1312 | 6/6 | 8/8 | 8/8 | **0** / 4021 |
| `compressed/ollama-qwen2.5-coder-32b` | 639 | 6/6 | 7/8 | 8/8 | **300** / 4021 |
| `written/ollama-qwen2.5-coder-32b` | 487 | 4/6 | 6/8 | 6/8 | 276 / 4021 |
| `written/ollama-qwen2.5-coder-7b` | 247 | 3/6 | 6/8 | — | — |
| `written/ollama-llama3.2-3b` | 304 | 3/6 | 6/8 | — | — |
| `written/ollama-qwen2.5-coder-1.5b` | 397 | **2/6** | 7/8 | **8/8** | **674** / 4021 * |
| `written/ollama-qwen2.5-coder-14b` | 648 | 2/6 | 5/8 | 5/8 | 504 / 4021 |
| `compressed/*` (the other four) | 746–2644 | 6/6 | 8/8 | — | — |
| `advanced/ollama-qwen2.5-coder-32b` | 512 | 6/8 | 9/10 | — | — |

\* measured against 32b as the second reader, like the rest of that column.

Blank cells are unrun, not zero. The frontier column needed a hand-driven session per specification,
so it covers the six that answer a question rather than all thirteen.

## What the table is for

**Read the first and the last column together.** `written/ollama-qwen2.5-coder-1.5b` scores **8/8**
against the reference under a frontier implementer — byte-identical to it on all 4021 probed inputs —
and its claims kill **2 of 6** mutants while leaving **674** cases open. It is the corpus's proof that
one number cannot describe a specification: by the middle column it is as good as the best text we
have, and it is not.

**The two Claude columns disagree exactly once**, on `compressed/ollama-qwen2.5-coder-32b`: 7/8 under
32b, 8/8 under the frontier. That single cell is what "a stronger implementer hides a weaker
specification" looks like in practice.

## Four findings, and two retractions

Each of these is measured. The retractions are here on purpose: they are the module's own thesis
applied to us, and they belong in the materials next to the findings.

### 1. A worked example does not transmit a requirement

Two compressions of the *same* source, both keeping all eight in-tier claims, both scoring 8/8:

- Claude's kept the **requirement** — *"an implementation multiplying `price * percent` in 32-bit
  arithmetic overflows here and is wrong; a `Long` intermediate is required"* → **0 open**.
- 32b's kept only the **expected value** — *"E14 — the largest shelf price: `Int.MAX_VALUE,
  [Percentage(10)] → 1932735283`"* → **300 open**, every one at the top of the `Int` range.

The example was not a hint; it was the exact answer for that input. It still did not transmit, while
one sentence naming the arithmetic did. The reader is held constant across both — 32b overflows on
one text and not on the other — so this is about the wording.

Same shape as the properties-versus-examples point in 11.2, from the other side: an example cannot
say *"for every input"*, and it cannot say *"do it differently"* either.

### 2. A false claim transmits perfectly, and manufactures new ambiguity downstream

`written/ollama-qwen2.5-coder-14b` states two things that are false. Both readers read them the same
way:

```
price=100, [BigSpenderBonus(100, 20)]  →  both 100, reference 80   (its §3: a price exactly at the
                                                                    threshold does not fire)
price=100, [Percentage(150)]           →  both -50, reference 0    (its §3 shows -50 → -60)
```

1008 of 4021 cases, agreed between readers and wrong. **Being wrong produces no confusion anywhere to
notice** — that is worse than being vague, and it is the case a learner will not expect.

Then the part nobody planned. Its 504 open cases are almost all an off-by-one on **negative** prices
(−1623/−1624, −125/−126). The specification says *"rounding down"* and, separately, permits negative
prices. Neither statement is ambiguous alone; together they create territory its author never saw —
what "down" means below zero — and two readers split on it.

**A wrong decision does not sit still. It manufactures undecided ground further down.** The author of
that text did not make a rounding mistake; they allowed a minus, and the rounding followed.

### 3. A guarantee stated per rule leaves a hole where you forgot to repeat it

`written/ollama-qwen2.5-coder-32b`, §2, lines 12–14 — the floor is attached to two rules of three:

```
- A percentage rule reduces the price by `percent` percent of the original price, rounding down…
- An amount off rule directly subtracts `amount`…, with the result not going below zero.
- A big-spender bonus rule applies an additional discount…, with the result not going below zero.
```

The two readers split precisely there: `Percentage(150)` on 100 gives 0 or −50, 276 cases. The
reference states the floor **once, for the function** (B9) and has no such hole.

### 4. The frontier implementer is faithful, not good

It added `Long` arithmetic where the text asked for it and overflowed where the text did not — on
`written/ollama-qwen2.5-coder-32b` it returned `Int.MAX_VALUE` unchanged, agreeing with 32b and
differing from the reference. It is not a safety net. That is what makes it a usable instrument for
measuring a text.

### Retracted: "same agent twice" as the measurement

On `written/ollama-qwen2.5-coder-1.5b`: two independent frontier sessions gave **0 open of 4021**,
the same clean zero the best specification in the corpus gets. The cross-agent pair gave 674. The two
sessions wrote genuinely different code and produced identical behaviour.

"Left open" is relative to a **population of readers**, and two sessions of one model are one reader
twice — both reached for `coerceAtLeast(0)` and for `Long` although the text mentions neither.

So **agreement between two runs of one agent proves nothing**; disagreement still proves ambiguity.
It is a lower bound and never a clean bill of health, and `verifyDivergence` says so when it detects
that pairing.

### Retracted: "compression narrows the set of readers"

With `qwen2.5-coder:14b` as the second reader, Claude's compression showed 391 open cases against 0
for the long version — which looked like a real result. Re-measured with a frontier second reader:
**0**. The 391 were the weak reader's incapacity; it is the same model that elsewhere in this corpus
imports `DiscountRule` from the wrong package and does not compile at all.

The rule is not "use two readers" but **use two readers competent at the task**. This number is only
as trustworthy as the weaker of the pair.

## Reproducing it

From `spec-test-driven-grading/`:

```
./gradlew verifySpec -Pspec=fixtures/11.4/written                 # layer 1, deterministic
./gradlew scoreExtraction -Pagent=corpus32b                       # layer 2 against the key
./gradlew pricingPropertyMatrix                                   # layer 3, property → mutants
./gradlew verifyClaims -Pclaims=<ids>                             # what a claim set is worth
./gradlew verifyImplementations -Pagent=<run> --continue           # vs the reference, tier-aware
./gradlew verifyDivergence -PagentA=<run> -PagentB=<run>           # what the text left open
```

An implementation that does not compile is a **result**, and three of `impl14b`'s thirteen do not —
they are reported as DID NOT BUILD OR NEVER RAN alongside the nine that scored. That used to require
`--continue` and did not actually work even with it; the report is reachable now either way (see
`../../spec-test-driven/build-logic/reachable-reports.settings.gradle.kts`). The flag is still worth
typing — it keeps the other modules going — but nothing depends on remembering it.
