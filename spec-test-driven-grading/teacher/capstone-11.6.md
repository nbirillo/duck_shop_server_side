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
- **`BestOffer.kt` — an agent's implementation, then DEGRADED BY HAND. Staged, and here is the whole
  audit trail.** First measured honestly: `qwen2.5-coder:14b` implementing the trimmed specification came
  out **byte-identical to the reference on all 3010 probed inputs**, 6/6 settled and 7/7 open. So the
  "partial implementation" the capstone wanted did not arise on its own, and could not — the merged
  specification is dominated by the frontier layer's B-rules, which decide every fork outright, so a
  competent implementer just reproduces the reference. That correct version is kept at
  `fixtures/11.6/inherited-correct-14b.kt.txt` and is worth showing in the debrief.

  Her call was then to degrade it deliberately. **The constraint I held: every defect is a legitimate
  reading of a contradiction the inherited document really contains**, so anything the learner finds can
  be quoted back to them out of the specification they were handed. Four edits, each with its licence:

  | edit | the sentence in `inherited/SPEC.md` that licenses it |
  | --- | --- |
  | `franchise.admissionPolicy` never consulted | §4: "the behavior when a shop's admission policy does not allow the duck to be sold is **left open**", and the appended bullet describing eligibility using only "the shop's `admissionPolicy`" |
  | chain promotions applied **before** the shop's | nothing outside Q2's "*Assumed:*" fixes the order, and the appended bullet names the franchise first |
  | a tie goes to the **last** shop (`<=`) | §4: "the order in which shops are considered when multiple have the same lowest price: **any shop can be returned**" |
  | a duck priced `0` gets **no offer** | §3: "**Duck with zero price**: Return `null`" and "**Zero Price Duck**: it should be considered as not being sold by any shop" |

  Also left in: the now-dead `private fun applyRule` from 14b's output, whose pricing is wrong in two
  ways. Dead code with wrong semantics is what inherited code looks like; deleting it is the learner's call.

  **The resulting profile, measured:**
  - `./gradlew :inherited:test` — **GREEN.** The first thing the learner sees says it works.
  - the settled corner cases — **6/6.** It breaks nothing the brief settles, so there is no obvious bug.
  - the open-fork corner cases — **2/7**, wrong on forks 1, 2, 3 and 4.
  - the probe — **976/3010 = 32.4%** disagreement with the reference: 680 inputs where it sells and the
    reference says nobody will (chain admission), 139 where it refuses a duck priced 0, 60 same-shop
    price differences (order), 97 where it names a different shop.

  **A structural fact found while choosing the edits, worth knowing:** no defect exists that breaks a
  *settled* fact while leaving the inherited suite green. Weak as that suite is, it pins every settled
  fact it is capable of pinning. So this profile — plausible, green, and wrong on three quarters of the
  open forks — is the honest maximum, not a compromise.
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

## Giving feedback on a submission

**Why this is here and not on a slide (her call, 2026-08-19).** The capstone is an assignment, not a
lecture. If the deck explains what a fork is, which forks exist and what we measured, it hands over the
answer and the learner is walked through instead of working. So the student deck is **five slides — 52–56:
the situation, the brief, the deliverable, and how to run things** — and everything below stays with you,
to use when you respond to their work. One consequence, deliberate: **the recurring "how the models did"
slide is not in this block.** Use the numbers in conversation if they help.

### Reading their submission

```bash
# in spec-test-driven-grading/, pointing at the learner's own test source
./gradlew generateForks -PforkTests=<path to their work/src/test/kotlin>
./gradlew verifyForks --continue
```

Then, separately, the floor: put their implementation in `implementations/<name>/` and run
`verifyImplementations`, plus `tests-11.6/FranchiseCornerCases.kt` against it. **Do not rank with the
corner cases** — see above, everything passes them.

### What to say to each verdict

| verdict | what it means | what to ask them |
| --- | --- | --- |
| **SETTLED** | their tests accept exactly one reading | Which one, and *why that one?* The answer is theirs to defend, and "it is what the code already did" is not a defence. |
| **LEFT OPEN** | their tests accept several readings that answer differently | Not a bug they missed — **a decision nobody made.** Show them the two readings and ask what their specification says about it. Usually: nothing. |
| **CONTRADICTORY** | their tests reject every reading we hold | **Read their specification before calling this a fault.** It looks identical to a learner who decided the fork a *third* way, which is a good outcome. The report cannot tell the difference; you can. |
| **DID NOT COMPILE** | their tests do not build against a reading | Often a test reaching into their own implementation's internals rather than the contract. Worth naming as its own lesson. |
| **NO TESTS YET** | nothing in the suite directory | They have not started, or ran the check in the wrong directory. |

### What a good submission looks like

There is no single shape, and saying so is part of the feedback. Signals worth crediting, in rough order
of how much they show:

1. **Every fork decided, and each decision stated in the specification** — not just pinned in a test.
   A test that pins something the text does not say is a decision that will be lost at the next rewrite.
2. **They noticed the inherited specification contradicts itself** and said which side they took. The
   table above lists what is there to find.
3. **They split the specification** rather than editing one long document, or explained why they did not.
4. **They ran the same agent more than once**, or two different agents, and treated a disagreement as
   evidence about their text rather than about the model.
5. **They found something wrong with the inherited implementation** and can say whether the specification
   licensed it. That is the highest-value observation in the whole exercise.

Signals that need a conversation, not a mark: a suite that pins arithmetic `priceFor` already settled
(scope) · a specification that grew rather than shrank · tests written against the inherited code's
behaviour instead of against their own text (this makes every fork SETTLED for the wrong reason — worth
catching, because the report cannot).

### Discussion points that used to be slides

Use these when they fit what a particular learner did. None of them should be delivered before the attempt.

- **A reading is not a mutant.** A mutant asks *does your suite catch a defect*; a reading asks *does your
  suite express a decision*. The two checks are mirrors: the conformance check from 11.2 says **do not pin
  what the contract leaves free**, this one says **do pin what is a decision**. Telling those apart is the
  whole skill, and no report will do it for them.
- **One counterexample closes one alternative, not a fork.** Our own self-test pinned "the chain can refuse
  a duck every shop would sell" and the check still said LEFT OPEN — because two of the three readings we
  held answer that case identically. Closing a fork means rejecting *every* reading but one. Same shape as
  the 11.2 lesson that hardening against a counterexample closes the instance and not the class.
- **The options a brief imagines are not the options readers take.** Our brief offered three answers for
  the chain-versus-shop question; building the checks showed two of them were the same reading and the
  third was not expressible against the types — while the reading every weak specification actually took
  was not listed at all. Good material for anyone who has to write a brief.
- **A clean report is worth exactly what the check can reach.** They practised on one fork. The others were
  never in their folder.

## The reveal, for the debrief

**Moved to `capstone-11.6-debrief.md`** — five beats, the numbers with their provenance, and the one
decision left to each teacher (how much of the staging to admit out loud). There are no slides for it.

One correction worth recording, because the earlier draft of this section had it wrong: it said the
inherited implementation was "nobody's mistake — an agent doing exactly what that text says". **That was
true of the version we measured and is NOT true of the one that ships.** The shipped one was degraded by
hand. The defects are still all licensed by the document, which is what makes the lesson work, but the
picking was ours.

## One thing to watch for in the brief

`briefs/11.6-franchise.md` records that its own fork 1 was badly posed — two of its three options were
the same reading, the third was not expressible against `Shop`, and the reading that readers *actually*
take was not listed. Worth showing when teaching brief-writing: **the options a brief imagines are not
the options readers take.**
