# Specification — `priceFor` (composing promotions)

> Copy this file to `SPEC.md` and fill it in. Keep the headings: they are what gets checked for
> completeness, and each one holds a different kind of statement.

## 1. What it does

One or two sentences. What does `priceFor` return, in terms a reader who has not seen the code can
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

## 6. Laws

Statements of the form *these two ways of writing a promotion mean the same thing, for every duck and
every price*. Not "for this input, this output" — that belongs in section 2 — but an identity that
either holds everywhere or does not hold at all.

For each one you write down:

- the identity, in a form somebody could test;
- whether the contract **guarantees** it;
- if it does not hold, one input where it fails.

A guarantee is a cost, not a decoration: it forbids implementations, possibly including the one
section 2 describes. If you find yourself wanting a law that your own rules break, say so — then
either drop the guarantee or change section 2, deliberately, and record which you did.

Two failure modes to avoid, and they are opposites:

- **claiming a law you checked on a handful of numbers** — some of these agree almost everywhere and
  disagree in a corner;
- **claiming a law nothing could ever violate** — it reads as rigour and pins nothing.
