# Write the Specification — Advanced

Welcome to the advanced version of the specification exercise!
If the first exercise felt too easy, this one is for you
You'll work with the same feature — promotions in the Duck Shop. But this time, things are a bit more complicated.
In the basic exercise, you worked with individual discount rules. Now, **discount rules can be combined with each other**, and you'll need to describe how these combinations should work.

## What is the task?

Just like in the basic exercise, your teacher will give you a short description of what the business wants.
The description isn't included in the repository on purpose. 
Otherwise, your AI agent could read it too, and we wouldn't be able to check how well it can implement the feature using only your specification.
**Your task is to write a clear specification based on this description.**

## What are we specifying?

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

You already know `AdmissionPolicy` from the test-writing exercise. It checks whether a duck satisfies a condition and returns a yes/no result.
Here, we have three basic discount rules and three ways to combine them.
The structure is similar to `AdmissionPolicy`, but there's one important difference: **we're working with prices instead of Boolean values.**
And that means we also need to think about calculations, rounding, and how different discounts affect each other.

## What should you do?

Use [`SPEC-template-advanced.md`](SPEC-template-advanced.md) to write your specification.
It's almost the same template as in the basic exercise, but we've added one new section.
This section is the main challenge of the advanced exercise!

## What's new in this exercise?

When we combine discount rules, we need to think about more than just the result of one operation.
For example, does applying discount A and then discount B always give the same result as applying B and then A?
Or can two different combinations of rules produce the same result for every possible duck?
**Your job is to decide which of these properties should hold and describe them in your specification.**

These are general rules that should work for all valid inputs, not just a few examples.
There are two important things to keep in mind:

1. **Don't trust just a few examples.** Some rules might work for most prices but fail for specific cases. You'll need to check more than two or three inputs before deciding that a property always holds.
2. **Don't assume that a property should hold just because it sounds reasonable.** Sometimes two rules can't work together in the way you'd expect. If you want to guarantee a certain behavior, you might need to change other parts of your specification.

And that's fine! There isn't always one correct design.
What's important is that you understand the decisions you're making and can explain why you made them.

## How will we check your specification?

We'll use the same checks as in the basic exercise, but with one additional step.
**The properties you describe will be turned into property-based tests.**
Instead of checking only a few examples, we'll generate different inputs and check whether your properties hold.
This helps us find cases where a rule works for some inputs but fails for others.
We'll also check whether your properties are actually useful. 
For example, a property that every possible implementation satisfies doesn't help us distinguish correct implementations from incorrect ones.
The goal is the same as before: **write a specification that clearly explains what correct behavior means, and then check whether an implementation follows it.**