# Exercise 11.4 — Property Catalog and Answer Key

These notes are for teachers only.
This guide explains how we check the claims made by students' specifications, 
how we measure the accuracy of the AI claim extractor, and how to prepare the materials for class.

## First, two things that are easy to confuse

Exercise 11.4 uses two separate artifacts.

**1. The property catalog**

`pricing-properties/.../PricingProperties.kt`

This is Kotlin test code. Each test represents a claim that a specification might make about `priceFor`.
We run these properties against the reference implementation and every mutant.
**This is the part that actually checks behavior.** It's deterministic, and no AI model changes it during evaluation.

**2. The answer key**

`fixtures/11.4/key.json`

This is a hand-written record of which claims each example specification actually makes.
We use it to measure whether the AI claim extractor reads specifications correctly.
**The key is not a grading rubric.** Students aren't graded against it, and no mark is calculated from it.
That distinction matters throughout this guide.

---

## 1. Why do we need an answer key?

The claim extractor reads a specification and answers a question for each property in the catalog:
**Does this specification explicitly make this claim?**

We ran the extractor five times with identical settings.
All five runs produced exactly the same answers.
That's good for reproducibility, and it's what we'd expect at temperature 0.
But reproducibility doesn't guarantee correctness.

### A real example

On the strongest specification in our corpus, the extractor answered **NO** to the claim:

> No rule ever increases the price.

But the specification states this twice.
First, as a bound:

```text
0 <= priceFor(duck, rules) <= duck.price
```

And again through a monotonicity requirement.
So the claim really is present.
The extractor simply missed it.

Without a hand-written key, we couldn't tell the difference between:

- A specification that forgot to state the rule.
- An extractor that failed to recognize the rule.

And that would mean our tool could report a problem in a specification that was actually correct.
To see the current extractor accuracy against the shipped catalog, run:

```bash
./gradlew scoreExtraction -Pagent=<run>
```

---

## 2. How to read the key

The key uses three possible verdicts for each claim.

| Verdict  | Meaning                                                       | Why it matters                                                                |
|----------|---------------------------------------------------------------|-------------------------------------------------------------------------------|
| `yes`    | The specification explicitly makes the claim.                 | The behavior is stated.                                                       |
| `no`     | The specification doesn't address the claim.                  | An implementer is free to choose. That may be acceptable or may reveal a gap. |
| `contra` | The specification says something incompatible with the claim. | An implementer may be directed toward the wrong behavior.                     |

It's important to distinguish **silence from contradiction**.
A specification that doesn't mention a behavior is different from one that explicitly requires the opposite behavior.

### What does `outOfScope` mean?

The key also has an `outOfScope` list.
These are claims the author wasn't expected to make because they weren't included in that specification's business description.
For example, a basic-tier specification doesn't need to describe `BestOf`, because `BestOf` wasn't part of the basic-tier brief.
The extractor should still answer `NO` for those claims.
But when a teacher reads the report, `outOfScope` explains why that absence isn't a problem.

### A limitation of the extractor

The extractor only answers YES or NO.
It cannot say:

> This specification explicitly requires the opposite behavior.

So when the correct key verdict is `contra`, the extractor answers `NO`.
Technically, that's correct: the expected claim isn't present.
But it misses the more serious issue: **the text actively contradicts the claim**.

`scoreExtraction` counts these cells as agreement but reports them separately.
That's a limitation of the extractor's design, not something the score can fix.
A human reader can recognize a contradiction that a yes/no extractor cannot express.

---

## 3. What if a student disagrees with the extractor?

That's what `verifyClaims` is for.
Students can challenge the extractor by declaring which claims their specification makes.
For example:

```bash
./gradlew verifyClaims -Pclaims=1,3,4,6
```

Or, if they have a `claims.txt` file next to their specification:

```bash
./gradlew verifyClaims -Pspec=<SPEC.md> -Pagent=<run>
```

They can identify claims using:

- Claim numbers.
- Full claim names.
- Any unambiguous fragment of a claim name.

Once the claims are declared, the rest of the process is deterministic.
The tool runs those properties against the reference implementation and mutants, then reports which mutants each claim rules out.

**No AI model is involved in that step, and the answer key isn't needed.**
That's why this process also works with a property catalog you've written yourself.

### What should we look for in the report?

The interesting part isn't just the score.
It's where the student's declared claims and the extractor's results disagree.

#### CONTESTED — The student declared a claim, but the extractor didn't find it

Ask the student:
**Where does your specification say this?**

There are two possibilities.
The extractor may have missed a real statement.
Or the student may believe the rule is obvious without having written it down.

Both are worth discussing.
The report deliberately doesn't decide which explanation is correct.

#### NOT DECLARED — The extractor found a claim the student didn't declare

This is often even more interesting.
It means the student wrote something that constrains the implementation without realizing it.
They may have accidentally made a decision they didn't intend to make.
Ask them to find the sentence and explain whether that behavior should really be part of the contract.

### Winning an appeal doesn't guarantee a better score

Here's a useful example from our corpus.
In the strongest specification, claim 2 is genuinely present.
The extractor missed it.
So the student would be right to challenge the result.

But the corresponding property **doesn't kill any mutant**.
That means the claim, although correctly stated, doesn't rule out any of the defects in our catalog.
Both facts can be true:

- The extractor was wrong.
- The claim adds no distinguishing power against the available mutants.

The report shows both results together.
**A correct appeal fixes the reading of the specification. It doesn't automatically make the specification stronger.**

---

## 4. Preparing the specification corpus for students

The repository is public, and that's intentional.
Someone studying independently can choose to read the teacher grading build. At that point, the exercise depends on their willingness to work through it honestly.
But when you're teaching a group, you control which materials you hand out.

**When you distribute `fixtures/11.4/`, rename the specification files.**
The original filenames reveal which AI model wrote each specification.
That matters because students are supposed to compare the texts without knowing their authors.

A model name can influence their judgment before they've read a single sentence.
It can also affect the compression exercise: students may automatically prefer whichever model they believe is strongest.

### Make an anonymous handout

For example:

```bash
mkdir -p handout

cp fixtures/11.4/written/claude-code.md \
   handout/A.md

cp fixtures/11.4/written/ollama-qwen2.5-coder-1.5b.md \
   handout/B.md

cp fixtures/11.4/written/ollama-qwen2.5-coder-32b.md \
   handout/C.md
```

These are the three specifications we used in the compression experiment, so the results in [`exercise-11.4-corpus-results.md`](exercise-11.4-corpus-results.md) correspond to this set.
You can choose other specifications too.
Just make sure the set includes **one longer specification and two shorter ones**.
Otherwise, there may be very little for students to combine or compress.

Shuffle the order yourself, rather than always using the same mapping.
And keep a private record of which file came from which model.
Students may want to know the authors afterward.
That's a good discussion to have **after they've finished comparing the specifications**, not before.

---

## 5. Should students get the property catalog?

By default, the claim list and property checks stay with the teacher.
Students write their specifications first.
Then they receive feedback about the claims their text appears to make and can challenge that feedback in writing.
Slide 41 also asks students to assess their own claim lists manually before using the tool.

That step doesn't require any extra materials from you.
But you can choose to give students the property catalog and `verifyClaims` directly.
The task already exists in the shared Gradle plugin. The student build only lacks `implTests`, so enabling this workflow is a small change.
There is a real trade-off, though.

### What students gain

They get immediate feedback.
They can write a claim, run the property check, and find out:

- Does the claim hold against the correct implementation?
- Does it rule out any known incorrect implementations?

They don't have to wait for you to run the checks.
For a strong group, or a second attempt at the exercise, this can be extremely useful.

### What it costs

The catalog becomes a **visible target**.
Once students see the list of properties, they can write specifications specifically to satisfy those checks.
We've seen this effect in our own experiments more than once.
As soon as a score becomes visible, agents and people may start organizing their effort around improving that number.

A student with the catalog can simply write a specification that asserts the ten listed claims.
But that's a different task from reading a business description and deciding which behaviors need to be specified.
It also weakens the final comparisons in the exercise, because the resulting specifications were written with the evaluation criteria already visible.

### A useful compromise

**Give students the catalog after they submit their first specification.**
Then let them use it for a second attempt.
This preserves two different learning experiences:

1. **First attempt:** Decide what the specification should say without seeing the evaluation criteria.
2. **Second attempt:** Use the property catalog to check, refine, and strengthen those decisions.

Students get the benefits of feedback without losing the independent reasoning exercise.
The order matters more than whether they eventually receive the tool.

### What should remain teacher-only?

Regardless of when you share the catalog, don't give students:

- The reference implementation.
- The mutant catalog.

Those are answers, not just evaluation criteria.
If students can read the mutants, they already know which defects the exercise expects them to discover.
If they can read the reference implementation, the task changes from interpreting the business requirements to reproducing our chosen implementation.

---

## 6. Creating your own property catalog

You don't have to use our catalog.
In fact, creating your own is a good way to adapt this exercise to your course.
Our catalog is a starting point, **not a universal definition of a good specification**.

### Step 1: Write the properties

Create your own `PricingProperties.kt`.
You can write it yourself or ask an AI model to generate a first draft.
If you use an agent, read every line before accepting it.

There are two important checks.
**A property that fails against the reference is incorrect.**
It cannot be used as a valid requirement for that implementation.

**A property that kills no mutant doesn't distinguish correct code from the incorrect implementations in the current catalog.**
It may be true, but it doesn't help catch any of those defects.
Run:

```bash
./gradlew pricingPropertyMatrix
```

This shows which mutants each property kills.
Two properties in our own catalog kill no mutants.
We keep them deliberately because they're useful examples of the distinction between a true claim and a claim that detects a defect.
So don't treat the matrix as a checklist of properties you must delete.
Use it to understand what each property contributes.

### Step 2: Keep claim names stable

The test names in `PricingProperties.kt` are also the claim identifiers.
The extractor reads them directly from the source file.
That prevents the claim list and property implementation from drifting apart.
But it also means **renaming a test changes the identifiers used by downstream tools**.
If you rename or reorder properties, remember to update anything that depends on those identifiers, including the answer key.

---

## 7. Creating your own answer key

**An answer key is only valid for the property catalog it was written for.**
If you change the properties, the existing `key.json` may refer to claims that no longer exist.
We manually update our key whenever the catalog changes.
And `scoreExtraction` checks the lengths: if the number of key entries doesn't match the catalog, it fails rather than silently comparing the wrong claims.

### What happens if you don't write a new key?

The tools still work.
The extractor can still read specifications and produce reports.
The deterministic property checks still run.
But you no longer know how accurate the extractor is on your new catalog.

Here's the difference:

| Setup                              | What you can say about the extractor                                |
|------------------------------------|---------------------------------------------------------------------|
| Shipped catalog and key            | Its accuracy has been measured.                                     |
| Custom catalog without a new key   | It produces results, but its accuracy is unknown.                   |
| Custom catalog with a reviewed key | You can measure its accuracy on your own claims and specifications. |

This doesn't break grading because **grades don't depend on the extractor's answers**.
The deterministic property-testing layer does the actual behavioral checking, and it doesn't need the key.
If you use an extractor that hasn't been measured against your catalog, describe its results as advisory.
Tell students that the tool may misread their specifications.

### How to write a new key

If you want to measure the extractor's accuracy, create a small reviewed dataset.

**1. Choose five or six specifications.**

Include examples of different quality levels.
You can reuse the specifications in `fixtures/11.4/` or create your own.

**2. Read every specification against every claim.**

For each pair, record:

- `yes` if the specification makes the claim.
- `no` if it doesn't address the claim.
- `contra` if it asserts incompatible behavior.

Also record a `where` field pointing to the relevant part of the specification.
That reference makes the key auditable.
It also helps when you revisit the key later and discover that you disagree with your earlier judgment.

**3. Run the extractor evaluation.**

Use `scoreExtraction` and inspect the mismatches.
A mismatch may mean the extractor has misread a specification that a human correctly classified.

### How much time should you budget?

This takes careful reading.
You need to evaluate every selected specification against the claims in your catalog.
It's not work that becomes faster simply by running more models in parallel, because the whole point is to establish a human-reviewed reference.
That's the cost of knowing whether your checker is accurate.
And it's why the shipped catalog includes a key we've already written.

---

## 8. When should you reveal the reference implementation?

The reference implementation lives in:

`reference/.../Pricing.kt`

Its KDoc documents all seven decisions used by the reference.
We hide it by default.
You can show it during the debrief if that helps your group understand the differences between specifications.

But **show it only after students have written their own specifications**.
Once the reference is visible, the exercise changes.
Instead of deciding what the business description means, students can simply copy the decisions we've already made.
And that's not the skill exercise 11.4 is designed to teach.

---

## The main takeaway

This exercise has three separate questions:

1. **What does the specification actually say?** The claim extractor helps answer this, but it can be wrong.
2. **Which behaviors do those claims constrain?** The deterministic property catalog checks this against the reference and mutants.
3. **Which decisions should the specification make in the first place?** That's the student's responsibility, based on the business description.

The answer key helps us measure the first tool.
The property catalog provides evidence for the second question.
Neither should replace the student's work on the third.

**The goal isn't to write a specification that scores well against a visible checklist. It's to make the important decisions explicit, then use tools to check whether the text really communicates them.**