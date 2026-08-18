# Specification — `quote`

> Copy this file to `SPEC.md` and fill it in. Keep the headings: they are what gets checked for
> completeness, and each one holds a different kind of statement.

## 1. What it does

One or two sentences. What does `quote` return, in terms a reader who has not seen the code can
follow?

## 2. Behaviour

The rules of the calculation, stated so that two people implementing from this cannot disagree.

Write each statement so it is **checkable**: it should be possible to point at an input and say
whether the statement holds. "Discounts are applied sensibly" is not checkable. "A percentage rule
reduces the price by `percent` percent of the price it is applied to" is.

Finding what belongs here is most of the work. The business text is short, and a short text leaves
more undetermined than it looks — go through it and ask, at each sentence, whether two people could
read it and compute different numbers.

## 3. Edge cases

Inputs that are legal but unusual, and what the answer is for each. Empty things, zero, boundaries,
values at the far ends of what a type allows, and combinations that would not occur in a demo.

For each one, say the answer — not that it "should be handled".

## 4. Deliberately not specified

Behaviour you are **choosing** to leave free, and why. Anything here is something an implementation
may do either way without being wrong, and something a test must not pin down.

Be careful with this section in both directions. Leave too much open and the specification does not
determine the answer. Leave nothing open and you have frozen decisions that were never yours to
freeze.

## 5. Open questions

Things you could not decide on your own, and would ask the business. For each: what you need to know,
and what you assumed in the meantime so that implementation can continue.

The difference from section 4 matters. Section 4 is "any answer is fine". This section is "there is a
right answer and I do not know it".
