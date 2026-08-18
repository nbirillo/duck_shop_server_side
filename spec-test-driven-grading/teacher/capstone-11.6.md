# Teacher notes — 11.6, the capstone

Teacher-only. The learner gets `spec-test-driven-capstone/` and nothing in this build.

## Why the capstone is a separate build

Not tidiness. What the capstone hands over includes a **working `priceFor`** — pricing was settled in
11.4, and any real codebase would already have it. Committing that file inside `spec-test-driven/` would
give a learner still on 11.4 the answer to its implement-from-specification step, and give it to any
agent working in that folder. Same reason the grading build and the business briefs sit outside it.

So: hand out `spec-test-driven-capstone/` when the capstone starts. It borrows `:core` from the student
build through a composite build, and the link points only inwards.

`inherited/.../Pricing.kt` is the reference pricing with its KDoc rewritten in-universe — **the code is
asserted identical to `reference/.../Pricing.kt` outside comments**, because the graded fork readings
price with the reference and any drift would put the learner and the grading on different arithmetic.

## What is graded, and what is not

`verifyForks` in THIS build, against the learner's test suite: **9 readings over 4 forks** in
`forks/catalog.json`. Per fork the verdict is SETTLED (and which reading), LEFT OPEN, CONTRADICTORY, or
DID NOT BUILD. **There is no score and no right answer.** The learner is judged on whether a decision was
made and held, not on matching our reference.

`tests-11.6/FranchiseCornerCases.kt` is a **floor, not a grade** — 6 settled facts, and we measured that
everything passes them (see below). Run it, but do not rank with it.

**CONTRADICTORY needs a human.** It means the suite rejects every reading we hold, which is usually a
broken test — but it is also what a learner who decided the fork some *third* way looks like. Read their
specification before calling it a fault. The report says as much, and it cannot tell the difference.

## The three measurements the design rests on

Do not redesign this without re-reading them; two of them killed earlier plans.

1. **The feature genuinely diverges.** Two specifications of the same brief, implemented by the *same*
   agent, answer differently on 951 of 3010 probed inputs. This is what 11.5 failed: there, a local model
   implementing the frontier's spec matched the reference byte for byte.
2. **A hidden corner-case set grades nothing.** Every artifact in the corpus passes every settled corner
   case — including a specification that disagrees with the reference on 884 of 3010 inputs. All the
   divergence lives in the open forks, so "did it pass the hidden tests" measures nothing at all. That is
   why the grading layer is fork discrimination.
3. **Two independently written weak specifications missed exactly the same three forks** — the chain's own
   rule, promotion order, and when cheapest is measured. Ties and empty chains converge every time. So
   the forks are not arbitrary: they are where readers actually fail.

## How the inherited artifacts were made — and which one is staged

Honesty matters here, because the module's method is *calibrate, do not stage*.

- **`SPEC.md` — not authored.** The six real franchise specifications in `fixtures/11.6/` (frontier plus
  the whole local tier) were concatenated as `fixtures/11.6-parts/`, then consolidated into one document
  by an agent under `prompts-capstone/agent-prompt-compress.md`, whose instruction is the opposite of the
  11.4b compression prompt: **keep everything, keep the disagreements, decide nothing.** That is how
  specifications actually get bloated — accretion, not bad writing. The parts are kept so the accretion
  can be shown.
  **Check after any regeneration:** that the consolidation did not quietly settle a fork. The prompt
  forbids it; the model is not bound by the prompt.
- **`BestOffer.kt` — not authored.** An agent implementing that specification and nothing else. Its
  wrongness is therefore *caused by the specification's silence*, which is the lesson. Nothing was broken
  on purpose.
- **`FranchiseTests.kt` — AUTHORED, and the only staged artifact.** Written to a profile: green on the
  inherited implementation, and settling **no fork** (verified with `verifyForks`, expect 0 settled).
  Selecting a real agent suite by that criterion was the alternative and is the better provenance; it was
  passed over for cost. If it is ever regenerated, prefer the measured route.

## The one edit made to the merged specification, and why

The consolidation is the agent's; **the scoping is mine**, and the boundary matters, so here it is
exactly. The merge came out **re-specifying pricing** — unsurprising, since all six sources specify a
feature whose price computation they each restate. An agent implementing that document therefore
reimplemented `priceFor` instead of calling it, and got it wrong three ways (Int overflow, an exclusive
big-spender threshold, and a `Then` that rewrote the duck's own price so a nested `OnlyIf` saw a
different duck) — **154 of 3010 probed inputs, and in 20 of them a different shop wins.** Realistic, but
it puts 11.4's settled arithmetic back on the table and drags the capstone's scope with it.

So the pricing restatements were removed, and nothing else: **185 → 165 lines, 2988 → 2552 words.**

- B4 no longer fixes the base at `max(0, duck.price)`
- B6 keeps **which** rules apply and **in what order** — that is fork 2 — but states the price as
  `priceFor(duck, s.promotions + franchise.promotions)` instead of spelling out the arithmetic
- B7–B9 (applying one rule, clamping at zero, exact arithmetic) collapse into one **B7** saying pricing
  was settled earlier, is documented on `priceFor`, and is not restated here
- three appended bullets restating what Percentage, AmountOff and BigSpenderBonus do
- §3 edge cases 6, 7, 8, 9, 10, 19, 20 — zero, negative and `Int.MAX_VALUE` bases, clamping, overflow,
  and the two rounding worked examples. The other 17 stay and are renumbered.
- §5 **Q4**, the rounding question

**Asserted, not eyeballed:** the trimming script fails unless every fork statement (B1, B5, B6, B11,
B12, B14) and every retained contradiction ("any one of them may be returned", "left open", "highest
discount is applied first", "Return the first shop") is still in the file.

## What the inherited specification actually contains

185 lines, ~3000 words, against 31–35 lines for each of the six sources. It came out better than hoped,
because the contradictions are **real** — the frontier's precise rules sit next to the local models'
vague restatements of the same thing, and neither side was edited. The ones worth having ready:

| §2 says | and elsewhere the same document says |
| --- | --- |
| **B12** a tie goes to the lowest index in `franchise.shops` | "If multiple shops have the same lowest price, **any one of them may be returned**" — and §4 repeats it as *deliberately not specified* |
| **B1** a shop is eligible only if the chain admits the duck too | §4: "the behavior when a shop's admission policy does not allow the duck to be sold is **left open**" |
| **B5/B6** every rule in both lists applies, in a fixed order | §4: "**the promotion with the highest discount is applied first**" — which is not a reading of the brief at all, it is simply false |
| **B6** each rule sees the price left by the one before it | the appended bullets: a percentage "reduces the price by percent percent of the **original** price", and the bonus fires "if the **original** price exceeds the threshold" — both contradict what 11.4 settled |
| **§5 Q1–Q6** each end in an explicit *"Assumed:"* line | §4 lists several of those same questions as open |

So the decisions **are** in there — buried, restated wrongly, and contradicted. That is the reading work.
Note also **Q5**: one source recorded that it could not read `:core` from its folder and so could not verify
the rule semantics at all. That is a specification honestly reporting the limit of its own evidence, and
it is worth showing next to the sources that simply guessed.

## The reveal, for the debrief

Say all of it, after they have handed in:

- The specification they inherited was **six independent specifications stacked and smoothed**. Nobody
  ever wrote it as one document, which is why it reads the way it does. Ask who noticed, and who split it
  back up.
- The implementation was **nobody's mistake** — an agent doing exactly what that text says.
- Everything they inherited **passes the settled facts**, and a specification can disagree with ours on
  a quarter of all inputs and still pass them. This is the module's thesis arriving: the
  machine-checkable part was fully satisfied by a bad artifact.
- Whichever forks they left open: two independent weak specifications missed the same three. Being in
  good company is not the same as being right.

## One thing to watch for in the brief

`briefs/11.6-franchise.md` records that its own fork 1 was badly posed — two of its three options were
the same reading, the third was not expressible against `Shop`, and the reading that readers *actually*
take was not listed. Worth showing when teaching brief-writing: **the options a brief imagines are not
the options readers take.**
