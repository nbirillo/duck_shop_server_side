# Exercise 11.4, advanced — specify a thing that composes

Take this tier when the first brief did not fight back: you wrote the contract, the agent implemented
it, and there was nothing much to argue about.

The feature is the same one. What changes is that promotions stop being a flat list and start
combining, and a contract for something that composes has to say things a contract for a flat list
never has to.

Here is the whole of what the business said this time:

> **Your teacher gives you the brief.** It is a short piece of business text — a few
> lines about what the shop wants — and it is deliberately not in this repository: if it
> were, the agent you hand your specification to could read it, and you would learn
> nothing from what it builds.

## What you are specifying

```kotlin
sealed interface DiscountRule {
    data class Percentage(val percent: Int) : DiscountRule
    data class AmountOff(val amount: Int) : DiscountRule
    data class BigSpenderBonus(val threshold: Int, val amount: Int) : DiscountRule

    data class Then(val first: DiscountRule, val second: DiscountRule) : DiscountRule
    data class BestOf(val options: List<DiscountRule>) : DiscountRule
    data class OnlyIf(val condition: AdmissionPolicy, val rule: DiscountRule) : DiscountRule
}

/** Returns what [duck] actually costs under the promotion [rule]. */
fun priceFor(duck: Duck, rule: DiscountRule): Int

/** Returns what [duck] actually costs in a shop running all of [rules]. */
fun priceFor(duck: Duck, rules: List<DiscountRule>): Int
```

`AdmissionPolicy` is the one you already know from 11.2 — a yes/no question about a duck.

Note the shape: three leaves and three combinators, the same skeleton as the admission algebra. What
is new is that these combine *values* rather than yes/no answers, and values are where arithmetic can
bite you.

## The new section

Use [`SPEC-template-advanced.md`](SPEC-template-advanced.md). It is the same template with one section
added, and that section is the point of this tier.

Once a thing composes, some ways of writing the same promotion ought to mean the same thing — and some
of them will not. Your contract has to say which. That is a different kind of statement from anything
you wrote in the basic tier: not "for this input, this output", but "for **every** input, these two
expressions agree".

Finding them is your job, and so is checking them. Beware of two traps:

- **A law that holds on the numbers you tried.** Some of these agree on most prices and disagree on a
  few. Two or three examples will tell you a law holds when it does not.
- **A law you would like to be true.** Guaranteeing one is not free: it rules out implementations, and
  sometimes the design you already described is one of them. Then you either drop the guarantee or
  change the design — and either is a legitimate answer, as long as it is deliberate.

## What happens to your spec

Everything from the basic tier, plus: the laws you claim are turned into **property-based tests** and
run over generated inputs — a law that is false will be found, and a law that is true but vacuous
(nothing could have violated it) shows up as pinning nothing.
