# The capstone — Capstone Debrief

These notes are for teachers only.
**Use them after students have submitted their work, not before.**
The whole module follows one important principle: **first try, then compare**.
Students should have a chance to make their own decisions before seeing what other agents did, what we measured, or how we prepared the exercise.
There are deliberately **no slides for this debrief**. See [`capstone.md`](capstone.md) for the reasoning.
Instead, use these notes to guide a conversation. Spend more time on the things your students actually noticed, struggled with, or found surprising.

## First, one decision for you

**How much should you tell students about how we prepared the inherited materials?**
That's up to you.
We deliberately leave this decision to each teacher because different groups may benefit from different approaches.
Here's what actually happened.
Two of the four inherited artifacts were prepared by us:

- **The test suite** was written by us.
- **The implementation** started as a correct agent-generated implementation, which we then deliberately changed by hand.

The specification was different. It came from **six real AI-generated specifications combined by another agent**.
The full history, including which sentences in the specification support each deliberate defect, is documented in [`capstone.md`](capstone.md).
There are two reasonable ways to handle this.

**Option A: Explain how we prepared everything.**

This turns the capstone into an example of how we designed and calibrated the exercise itself.
We measured real agent outputs, discovered that the imperfect implementation we wanted didn't appear naturally, and deliberately prepared one. We also documented exactly what we changed.
This is especially useful for students who may go on to design exercises or teaching materials themselves.

**Option B: Keep the focus on the inherited project.**

You can let students experience the artifacts as an existing codebase they've been asked to understand and improve.
For groups focused on software engineering practice rather than course design, that experience may be more valuable.
Either approach is fine.

**But don't describe the shipped implementation as untouched AI-generated code. It isn't.**

---

## The five parts of the debrief

You don't need to deliver these as a lecture. Use them as five starting points for discussion.

### 1. Did anyone notice how the specification was assembled?

The inherited specification wasn't written as one document.
It started as **six independent specifications**, which an AI agent then merged into one.
Each original specification was only 31–35 lines long.
The combined version is **165 lines**.
That helps explain why it repeats itself, changes terminology, and sometimes gives different answers to the same question.

Ask students:

- Did you notice that the specification seemed to contain several different voices or sets of rules?
- Did you split it into smaller specifications?
- Or did you try to improve the existing document without changing its structure?
- Which approach worked better for you?

The point isn't that splitting is always the right answer.
It's that **the structure of a specification affects how easy it is to understand and maintain**.

### 2. The specification contradicts itself

Now show students some of the contradictions.
The full table is in [`capstone.md`](capstone.md), but here are the main examples:

| One part says…                                        | Another part says…                                                                                           |
|-------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| When prices tie, choose a particular shop.            | The choice is deliberately unspecified.                                                                      |
| The franchise's own admission policy must be checked. | The effect of that policy is left open.                                                                      |
| Promotions follow a particular order.                 | Section 4 gives a different ordering rule that isn't supported by the business description.                  |
| Promotions use the price left by previous promotions. | Appended notes describe calculations using the original price, contradicting what the spec-writing exercise established. |

Ask students which contradictions they found.
More importantly, ask **what they did about them**.

There's a useful distinction here:

- A **gap** means the specification doesn't answer a question.
- A **contradiction** means the specification gives incompatible answers.

You can't fix a contradiction just by adding another example.
You have to choose which rule should apply, remove or revise the conflicting statements, and make that decision explicit.
**Once students resolve a contradiction, they own that decision.**

### 3. The inherited implementation followed possible readings of the specification

The inherited implementation contains four important behavioral differences:

1. It doesn't check the franchise's own admission policy.
2. It applies franchise promotions before shop promotions.
3. When prices tie, it chooses the last shop.
4. It refuses to sell a duck whose price is zero.

Every one of these behaviors can be connected to a statement in the inherited specification.
That's the important part.
The implementation doesn't behave differently simply because someone forgot how to write Kotlin.
It behaves differently because **the document gives conflicting or incomplete instructions, and the implementation follows particular readings of them**.

Ask students:

- Which behaviors did you find?
- Did you initially think they were bugs?
- Could you find text in the specification that supported them?
- Did you change the code, the specification, or both?

This is also where you can explain that we deliberately chose those readings when preparing the inherited implementation, if you've decided to share that part of the exercise design.

### 4. Everything passed the clearly specified tests

This is probably the most important reveal.

The inherited implementation passes **all six settled corner-case tests**.

<box gap={1}>
  <row align="baseline" gap={3}>
    <title size="2xl" color="default" tabularNums>6/6</title>
    **Settled tests passed**
  </row>
  <row align="baseline" gap={3}>
    <title size="2xl" color="default" tabularNums>976/3,010</title>
    **Inputs where behavior differed from the reference**
  </row>
  <caption>32.4% of the fixed probe corpus produced different results.</caption>
</box>

These numbers come from our own corner-case suite and the franchise differential probe, run against the reference interpretation using 3,010 fixed inputs.
And this wasn't just a consequence of deliberately modifying the inherited implementation.
An earlier specification in our corpus also passed every settled corner case, while disagreeing with the reference on **884 of 3,010 inputs**.
So the problem wasn't that the tests were badly implemented.

**The tests covered what the business description clearly specified. The disagreements were about what it didn't settle.**
That's the central lesson of the module.
A tool can report that every machine-checkable requirement has been satisfied, while important decisions remain unresolved.
Someone still has to read the requirements, notice those decisions, make them, and document them clearly.
And that work doesn't disappear just because we use AI agents.

### 5. Other agents missed the same questions

Finish by showing students how the different models handled the original business description.
We took six specifications and asked the same model, `qwen2.5-coder:32b`, to implement each one at temperature 0.
Then we compared the resulting implementations on the same 3,010 inputs.
Two independently written weak specifications missed **exactly the same three questions**:

- Does the franchise's own admission policy apply?
- In what order are promotions applied?
- When do we decide which shop is cheapest?

The interpretations of ties and empty chains were consistent across the runs.
This is worth discussing because the three missed questions weren't random edge cases.
**They were the places where readers actually struggled to make the same decision.**

That's why the grading checks focus on those questions instead of using a longer list of ordinary corner cases.
Ask students whether they also missed any of these decisions.
If they did, they're in good company!
But that doesn't mean leaving the decisions open is correct. It means these are genuinely difficult parts of the specification, and exactly the parts worth learning to recognize.

---

## One useful artifact to show

If you want to compare just one additional file, use:

`fixtures/capstone/inherited-correct-14b.kt.txt`

This is the original implementation produced by `qwen2.5-coder:14b` from the same specification.
It matched the reference implementation on **all 3,010 probed inputs**.
Put it next to the implementation students inherited.
The specification was the same.

What changed was **which of its statements the implementations followed**.
That makes the problem concrete: two implementations can be connected to the same document and still make different decisions about its meaning.
Remember that the version students received was deliberately modified by us, not independently produced in its final form by the agent.

---

## Where do the numbers come from?

Please keep the evidence together with the results when discussing them.
The detailed run history and measurements are recorded in `[[module-11-agent-run-findings]]`.
The design decisions and full audit trail are documented in [`capstone.md`](capstone.md).
In particular, keep these distinctions clear:

| Result                                | How it was measured                                                                                                  |
|---------------------------------------|----------------------------------------------------------------------------------------------------------------------|
| **6/6 settled tests**                 | Our corner-case suite against the inherited implementation                                                           |
| **976/3,010 disagreements (32.4%)**   | The staged inherited implementation compared with the reference on the fixed franchise probe corpus                  |
| **884/3,010 disagreements**           | An earlier specification's implementation compared with the reference on the same corpus                             |
| **Three repeatedly missed questions** | Six specifications, each implemented by `qwen2.5-coder:32b` at temperature 0, then compared on the same 3,010 inputs |
| **3,010/3,010 matching results**      | The original correct 14b implementation compared with the reference                                                  |

Don't quote a number without explaining what was compared and how.
We apply the same rule to the course slides: **measurements need context, or they can easily tell the wrong story**.

---

## The main takeaway

If students remember one thing from the capstone, it should be this:
**Passing every test doesn't mean every important decision has been made.**
Tests can verify what a specification says.
But when the specification is incomplete or contradictory, someone still has to decide what the software should do.
AI agents can help us write specifications, tests, and implementations. They can also help us compare different interpretations.
But they don't remove the need to recognize ambiguity, make decisions, and take responsibility for those decisions.
That's the skill this whole module has been building toward.