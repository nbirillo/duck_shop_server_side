# Exercise 11.6 — Capstone: Teacher Notes

These notes are for teachers only.
Students should receive `spec-test-driven-capstone/`, not this grading project.
This guide explains how the capstone was prepared, what we check, how to interpret the results, and how to give students useful feedback.

## Why is the capstone a separate project?

There's a practical reason for this.
The capstone includes a **working implementation of `priceFor`**.
Students already worked on pricing in exercise 11.4, and in a real software project, that feature would probably be finished by the time the next task begins.
But if we put the working implementation inside `spec-test-driven/`, students who haven't finished exercise 11.4 could find the answer.
And so could their AI agents.
That's why we keep the capstone separate, just like the teacher grading tools and business descriptions.
**Share `spec-test-driven-capstone/` only when students reach the capstone.**
The capstone uses a Gradle composite build to access `:core` from the student project. The dependency goes in that direction only.

### What about the inherited pricing code?

The capstone includes:

`inherited/.../Pricing.kt`

This is the reference pricing implementation, with its KDoc rewritten to look like documentation inherited from an existing project.
Outside comments, the code is checked to be identical to:

`reference/.../Pricing.kt`

That matters because the grading tools use the reference pricing implementation.
If the two versions calculated prices differently, students and teachers could get different results for reasons unrelated to the capstone.

---

## What do we grade?

The main grading tool is `verifyForks`.
It runs against the student's test suite and checks **nine possible interpretations of four open questions**.
The interpretations are defined in:

`forks/catalog.json`

For each open question, the report can say:

| Verdict           | Meaning                                                   |
|-------------------|-----------------------------------------------------------|
| **SETTLED**       | The tests accept exactly one interpretation.              |
| **LEFT OPEN**     | The tests accept multiple interpretations.                |
| **CONTRADICTORY** | The tests reject every interpretation in our catalog.     |
| **DID NOT BUILD** | The tests couldn't be compiled against an interpretation. |

**There is no score and no single correct answer.**
We're not asking students to reproduce our reference implementation.
We're asking them to make clear decisions about ambiguous behavior, document those decisions, and write tests that enforce them.

### What about the corner-case tests?

We also have:

`tests-11.6/FranchiseCornerCases.kt`

These tests cover six behaviors that the business description clearly specifies.
They are a **minimum correctness check, not a grading system**.
In our experiments, every implementation passed all six tests, including implementations that disagreed significantly on the open questions.
So run these tests, but don't use them to rank submissions.

### Be careful with CONTRADICTORY

A `CONTRADICTORY` verdict usually means something is wrong with the student's tests.
But there's another possibility: the student may have chosen a valid interpretation that isn't in our catalog.
The checker can't tell the difference.
**Read the student's specification before treating CONTRADICTORY as a mistake.**
A well-explained third interpretation can be a very good outcome.

---

## Why did we design the capstone this way?

Three experiments shaped the design.

### 1. Different specifications really produce different implementations

We gave two specifications of the same business description to the **same AI agent**.
The resulting implementations disagreed on **951 of 3,010 inputs**.
That's exactly the kind of problem we want students to experience: different interpretations of the same requirements can lead to different working software.
An earlier exercise, 11.5, didn't show this effect. There, a local model implementing a frontier model's specification produced exactly the same behavior as the reference.

### 2. Hidden corner-case tests don't help us grade this task

Every implementation in our corpus passed every settled corner case.
Even a specification that disagreed with the reference on **884 of 3,010 inputs** passed them all.
Why?
Because the differences were in the questions the business description left open.
So a traditional hidden-test score would tell us almost nothing.
That's why we check **which decisions the tests make**, rather than simply counting passing tests.

### 3. Weak specifications missed the same important questions

Two independently written weak specifications missed exactly the same three decisions:

- Whether the franchise's own admission policy matters.
- In what order promotions are applied.
- At what point the cheapest shop is chosen.

Other questions, such as ties and empty chains, tended to produce the same interpretation.
So the four graded questions weren't chosen randomly. They reflect places where readers actually disagreed or failed to make a decision.

---

## Where did the inherited materials come from?

This is important to explain accurately.
The course generally tries to **measure what agents actually produce**, rather than create artificial examples that fit our teaching goals.
But the capstone includes some deliberately prepared materials.
Here's the full story.

### `SPEC.md` — Combined by an AI agent

We started with six real franchise specifications saved in:

`fixtures/11.6/`

They came from a frontier model and the local models.
We collected them in `fixtures/11.6-parts/` and asked an agent to combine them using:

`prompts-capstone/agent-prompt-compress.md`

The instruction was deliberately different from the compression exercise in 11.4b.
Instead of making the document short, the agent was told to:

**Keep everything. Keep disagreements. Don't decide anything.**

The result shows a realistic way specifications become difficult to work with.
They don't always become long because someone writes badly. Sometimes they become long because multiple people keep adding requirements without resolving disagreements.
We kept the original parts so students can see how that happened.

**If you regenerate `SPEC.md`, check that the agent hasn't silently resolved any open questions.**

The prompt tells it not to, but that doesn't guarantee it will follow the instruction.

### `BestOffer.kt` — Agent-generated, then deliberately changed

This part needs an especially clear explanation.
We first asked `qwen2.5-coder:14b` to implement the trimmed specification.
Its implementation matched the reference on **all 3,010 tested inputs**.
It also passed all six settled cases and all seven open-fork checks.
So the partially correct implementation we wanted for the capstone didn't appear naturally.
The combined specification already contained enough precise rules from the frontier model for a capable agent to reproduce the reference behavior.
We saved that original correct implementation here:

`fixtures/11.6/inherited-correct-14b.kt.txt`

It's worth showing during the debrief.
We then **deliberately changed the implementation by hand**.
But we followed one important rule: every change had to be supported by a possible reading of the inherited specification.
That way, students can trace each surprising behavior back to something in the document they received.

Here are the four changes:

| Change                                            | Why the inherited specification allows that reading                                                                                    |
|---------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| Ignore `franchise.admissionPolicy`                | Section 4 leaves the effect of the franchise-level policy open, and another bullet describes eligibility using only the shop's policy. |
| Apply franchise promotions before shop promotions | Outside an assumed answer to Q2, the order isn't consistently fixed, and one bullet mentions franchise promotions first.               |
| Return the last shop when prices tie              | Section 4 says any shop may be returned when several have the same lowest price.                                                       |
| Return no offer for a duck priced `0`             | Section 3 explicitly contains statements saying a zero-priced duck should not be sold.                                                 |

We also left an unused `private fun applyRule` from the agent's original output.
Its pricing logic is wrong in two ways, but the function isn't called.
That's realistic inherited code: sometimes a project contains unused code with incorrect assumptions.
Students can decide whether to remove it.

### What does the resulting implementation do?

We measured it after making the changes.

| Check                       | Result                          |
|-----------------------------|---------------------------------|
| `./gradlew :inherited:test` | GREEN                           |
| Settled corner cases        | 6/6                             |
| Open-fork corner cases      | 2/7                             |
| Differential probe          | 976/3,010 disagreements (32.4%) |

Those 976 disagreements break down as follows:

| Difference                                                                   | Cases |
|------------------------------------------------------------------------------|------:|
| It sells a duck that the reference rejects because of franchise admission    |   680 |
| It refuses a duck priced `0`                                                 |   139 |
| It calculates a different price for the same shop because of promotion order |    60 |
| It chooses a different shop                                                  |    97 |

The important thing is that **the inherited tests are green, and all clearly specified corner cases pass**.
The disagreements are in the ambiguous behavior students are supposed to investigate.
We also found that the inherited suite already covers every settled fact it can meaningfully test. We couldn't introduce a defect in settled behavior while keeping that suite green.
So the shipped implementation is deliberately imperfect, but not randomly broken.

### `FranchiseTests.kt` — Written by us

The inherited test suite is also a prepared artifact.
We wrote it to satisfy two conditions:

- It passes against the inherited implementation.
- It doesn't settle any of the four graded questions.

We verified the second condition using `verifyForks`: it settles **zero forks**.
A better approach would have been to select a real agent-generated suite with those properties.
We chose the manually written version to reduce the cost of preparing the exercise.
If this artifact is regenerated later, prefer the measured approach.

---

## Why did we edit the combined specification?

The AI-generated combined specification was originally **185 lines and 2,988 words**.
It included detailed pricing rules from exercise 11.4.
That created a problem.
When we asked an agent to implement the capstone from that document, it reimplemented `priceFor` instead of calling the existing function.
And it introduced three pricing bugs:

- Integer overflow.
- An exclusive threshold for the big-spender bonus.
- A `Then` implementation that changed the duck's own price, so a nested `OnlyIf` evaluated a different duck.

These bugs caused differences on **154 of 3,010 inputs**, including 20 cases where a different shop won.
Those are realistic mistakes, but they belong to exercise 11.4.
We didn't want the capstone to turn into another pricing exercise.
So we removed only the sections that repeated the already-settled pricing rules.
The document became **165 lines and 2,552 words**.

### What exactly changed?

We made these edits:

- Removed B4's rule fixing the base price at `max(0, duck.price)`.
- Kept B6's decision about which promotions apply and in what order, but expressed the calculation as `priceFor(duck, s.promotions + franchise.promotions)`.
- Replaced B7–B9 with one B7 explaining that pricing was settled earlier and is documented on `priceFor`.
- Removed three appended bullets restating the behavior of `Percentage`, `AmountOff`, and `BigSpenderBonus`.
- Removed the pricing-specific edge cases in section 3: 6, 7, 8, 9, 10, 19, and 20.
- Removed the rounding question, Q4, from section 5.

The remaining edge cases were renumbered.
**We did not intentionally resolve any of the capstone's open questions.**
A script checks that all important fork statements and retained contradictions are still present.
It specifically checks B1, B5, B6, B11, B12, and B14, along with phrases such as “any one of them may be returned,” “left open,” “highest discount is applied first,” and “Return the first shop.”
This is checked automatically, not just by someone reading the document.

---

## What contradictions should teachers know about?

The inherited specification is intentionally difficult to read.
The original six specifications were only 31–35 lines each.
After consolidation, the document grew to around 3,000 words and contained real contradictions.
Here are the important ones:

| One part says…                                                         | Another part says…                                                                          |
|------------------------------------------------------------------------|---------------------------------------------------------------------------------------------|
| **B12:** When prices tie, choose the shop with the lowest index.       | Any shop with the lowest price may be returned, and section 4 calls the choice unspecified. |
| **B1:** A shop is eligible only if the franchise also admits the duck. | Section 4 leaves the effect of the franchise's admission policy open.                       |
| **B5/B6:** Apply all promotions in a fixed order.                      | Section 4 says the promotion giving the highest discount should go first.                   |
| **B6:** Each promotion uses the price left by the previous one.        | Appended bullets describe percentage discounts and bonuses using the original price.        |
| **Section 5:** Questions Q1–Q6 end with explicit assumptions.          | Section 4 describes some of those same questions as open.                                   |

The decisions are there, but they're mixed with other statements that contradict them.
**Students need to notice the disagreement and decide which interpretation their specification will follow.**
There's also a useful detail in Q5.
One agent explicitly said it couldn't access `:core` from its working folder and therefore couldn't verify the pricing rules.
That's a good example of an AI-generated specification honestly stating the limits of its evidence.
It's worth comparing with specifications that simply guessed.

---

## Demonstrating a fork in class

<text color="secondary" size="xs">Updated September 18, 2026</text>

We originally planned to give students a practice fork they could run themselves.
But we discovered a problem.
**A fork reading is a complete implementation of the feature.**
The example `forks/readings/Fork3Earliest.kt` didn't just answer the question about ties.
It also contained decisions about franchise admission, promotion order, choosing the cheapest shop, and empty chains.
So sharing its source code would accidentally reveal answers to all four graded questions.
We moved the practice example into the teacher project:

`forks-practice/`

**Teachers demonstrate this check in class. Students don't receive the practice implementation.**

### Commands for the demonstration

Run these from the teacher grading project:

```bash
./gradlew generateForks -PforksCatalog=forks-practice/catalog.json -PforksOut=forks-practice \
    -PforkTests=../spec-test-driven-capstone/inherited/src/test/kotlin

./gradlew verifyForks   -PforksCatalog=forks-practice/catalog.json -PforksOut=forks-practice --continue
```

When you run this against the inherited test suite, the report says:
**LEFT OPEN**

That's exactly what we want to demonstrate.
The inherited suite has eight passing tests, but none of them decides how ties should work.
You can also point `-PforkTests` at a student's own test directory to demonstrate the same check using their work.
If the test directory path changes, regenerate the forks. If you only edit the tests inside the same directory, running `verifyForks` again is enough.
The student README and slide 57 explain that this check exists and will be used on submissions, but **there is no student command for running it**.

---

## How to review a student's submission

The capstone is an assignment, not a guided lecture.
We deliberately kept the student-facing slides short.
Slides 52–56 introduce the situation, business description, deliverables, and commands.
They don't explain all the open questions or show how previous agents performed.
Students should discover those things through their own work.
Use the information below **after they've attempted the exercise**, when you're discussing their submission.

### Step 1: Run the fork checks

From `spec-test-driven-grading/`, point the tool at the student's test sources:

```bash
./gradlew generateForks -PforkTests=<path to their work/src/test/kotlin>
./gradlew verifyForks --continue
```

### Step 2: Check the implementation

Put the student's implementation in:

`implementations/<name>/`

Then run `verifyImplementations` and the corner-case tests in:

`tests-11.6/FranchiseCornerCases.kt`

Remember: the corner cases are a minimum check, **not a grade**.

### Step 3: Discuss the fork verdicts

The most useful part of grading is the conversation that follows.

| Verdict             | What it means                                                | What to ask                                                                                                                                     |
|---------------------|--------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| **SETTLED**         | The tests accept exactly one interpretation.                 | “Why did you choose this interpretation?” The answer should come from their specification, not just from what the existing code happened to do. |
| **LEFT OPEN**       | Several interpretations still pass.                          | “What does your specification say about this question?” Often the answer is nothing, which is the point of the exercise.                        |
| **CONTRADICTORY**   | Every catalog interpretation fails.                          | Read their specification first. They may have chosen a valid third interpretation that we didn't include.                                       |
| **DID NOT COMPILE** | The tests don't build against an alternative implementation. | Check whether the tests depend on implementation details instead of the public contract.                                                        |
| **NO TESTS YET**    | The selected directory has no tests.                         | Check whether the student hasn't started or the wrong directory was selected.                                                                   |

The important distinction is that **LEFT OPEN isn't necessarily a missed bug**.
It means the tests haven't made a decision between alternatives.
And **CONTRADICTORY isn't automatically wrong**.
It may mean the student's decision falls outside the options we prepared.

---

## What does a good submission look like?

There's no single correct structure.
But here are some things worth looking for.

### 1. Every important decision is written in the specification

It's not enough for a test to enforce one interpretation.
The specification should explain that interpretation too.
Otherwise, the next person who rewrites the tests may accidentally change the decision.

### 2. The student notices contradictions

The inherited specification contains statements that can't all be true at the same time.
A strong submission identifies those conflicts and explains which interpretation it chooses.

### 3. The student makes the specification easier to work with

For example, they might split one long document into smaller, focused specifications.
They might also have a good reason not to split it.
What matters is whether they can explain their choice.

### 4. The student compares agent results thoughtfully

They might run the same agent several times or use two different agents.
If the results differ, that's useful evidence about the clarity of the specification.
It's not automatically proof that one model is better.

### 5. The student finds a problem in the inherited implementation

This is especially valuable.
Ask them to explain whether the behavior is clearly wrong under the specification or whether the specification allows it.
That distinction is at the heart of the capstone.

### Things that deserve a conversation

Some results aren't necessarily bad, but are worth discussing:

- The tests spend a lot of effort checking pricing arithmetic that was already settled in exercise 11.4.
- The student's specification becomes longer instead of clearer.
- The tests copy the inherited implementation's behavior rather than enforcing the student's own decisions.

The last one is particularly important.
A suite can settle every fork simply by copying what the inherited code already does.
The checker can't tell whether those decisions were deliberate.
That's something you need to ask the student.

---

## Discussion points for the debrief

These ideas used to appear in slides.
We moved them into the teacher notes so they can come up naturally in response to students' work.

### A fork reading isn't a mutant

A mutant asks:

**Can your tests catch this bug?**

A fork reading asks:

**Have your tests made a decision about this ambiguous behavior?**

These are different questions.
In exercise 11.2, conformance testing teaches students **not to require behavior the contract leaves open**.
In the capstone, fork checking teaches students to **make and test the decisions that the specification needs**.
Knowing when to leave behavior flexible and when to make it precise is the real skill.

### One counterexample doesn't necessarily settle a fork

We tried a test that established the franchise could reject a duck even when every individual shop would sell it.
But the fork checker still reported LEFT OPEN.
Why?
Because two of the three interpretations in our catalog behaved the same way on that example.
To settle a fork, tests must reject every alternative except one.
This connects directly to the advanced exercise 11.2 lesson:
**Fixing one counterexample doesn't necessarily cover the whole category of behavior.**

### The options in a business description may not match what readers actually understand

Our original business description offered three possible answers to the franchise-versus-shop admission question.
But when we implemented them, we found that:

- Two options described the same behavior.
- One couldn't be expressed using the available `Shop` types.
- The interpretation that weak specifications actually used wasn't listed at all.

That's a useful lesson for anyone writing requirements.
**The alternatives we imagine when writing a brief aren't always the alternatives readers will actually choose.**

### A clean report has limits

Students may have seen a clean result for the practice fork.
But that doesn't mean the other forks are settled.
Those other interpretations weren't available in their project.
As in mutation and adversarial testing, **a checker can only report on the cases it knows how to examine**.

---

## What should we reveal at the end?

The full capstone debrief is in:

[`capstone-11.6-debrief.md`](capstone-11.6-debrief.md)

It contains five parts, with the evidence behind each number.
We intentionally don't use slides for this debrief.
There's one important point to handle accurately.
An earlier draft described the inherited implementation as an agent naturally following an ambiguous specification.
That was true of the original implementation we measured.
**It isn't true of the version students receive.**

The shipped version was deliberately modified by hand.
Every modification is supported by a possible reading of the inherited specification, but **we chose those modifications**.
How much of that preparation to explain during the debrief is a teaching decision. The teacher notes should always preserve the actual history.

---

## One final note about the business description

The original brief is in:

`briefs/11.6-franchise.md`

Its first open question wasn't written as well as we initially thought.
Two of the three options described the same behavior. The third wasn't expressible using `Shop`. And the interpretation readers actually chose wasn't included.
That's worth mentioning when teaching students how to write specifications.
Even the people preparing an exercise can misunderstand the possible interpretations of their own requirements!
**A good specification isn't just about writing down decisions. It's also about discovering which decisions really need to be made.**