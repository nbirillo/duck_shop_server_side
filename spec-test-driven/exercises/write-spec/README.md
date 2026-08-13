# Exercise 11.4 — write the specification

Until now the contract was handed to you. This time you write it.

The shop wants promotions. Here is the whole of what the business told us:

> Each shop keeps a list of discount rules, and they apply to a duck's shelf price.
>
> - **Percentage off** — for example, 10% off.
> - **Amount off** — for example, 5 off.
> - **Big-spender bonus** — a duck priced from 100 gets a further 20 off.
>
> Prices are whole units. The shop does not deal in fractions.

That is a normal amount of detail to be given, and it is **not enough to implement from**. Your job
is to turn it into a contract precise enough that somebody — a colleague, or an agent — can implement
it without asking you anything, and precise enough that two correct implementations cannot disagree.

## What you are specifying

```kotlin
sealed interface DiscountRule {
    data class Percentage(val percent: Int) : DiscountRule
    data class AmountOff(val amount: Int) : DiscountRule
    data class BigSpenderBonus(val threshold: Int, val amount: Int) : DiscountRule
}

/** Returns what [duck] actually costs in a shop whose active promotions are [rules]. */
fun priceFor(duck: Duck, rules: List<DiscountRule>): Int
```

`Duck` is the type you already know: `name`, `price`, `hasKotlinAttribute`, `accessories`.

## How to write it

Copy [`SPEC-template.md`](SPEC-template.md) to `SPEC.md` and fill it in. The sections are not
decoration — each one is a different kind of statement, and a specification that skips one is missing
something a reader needs.

The section that will feel strangest is **"deliberately not specified"**. You met the idea in 11.2:
some behaviour is contract and some is an accident of how the code happens to be written, and pinning
an accident costs a future refactor its freedom. Now you are on the other side of it — you decide,
and you write the decision down.

## What makes this hard

Read the business text again and count how many different implementations satisfy it. They will not
all return the same number for the same duck. Every place where that is true is a decision waiting
for you: either you make it, or the person implementing makes it for you, quietly, and you find out
later.

You are not expected to have one right answer. You are expected to **have an answer, on purpose, and
to be able to say why** — and to be honest in the last two sections about what you left open.

## What happens to your spec

- It is read for **structure and completeness** — did you make statements of each kind, or only
  describe the happy path.
- The claims in it are turned into **executable properties** and run against a correct implementation
  and against deliberately broken ones. A claim that holds everywhere pins nothing; a claim that
  fails on correct code is simply wrong.
- Every operation in the signature is checked for **coverage** — if nothing in your spec says anything
  about some part of it, that shows up.
- And an agent implements the feature **from your text alone**. What it gets wrong is the most direct
  answer you will get to "was that enough?"
