# Contract Addendum — Advanced Task Only

In the basic exercise, you might have noticed that the documentation in `:core` doesn't explain everything.
There are two things in particular that were left open.
In the basic exercise, you could make your own decisions about them, and different answers were acceptable.
But in the advanced exercise, **we're going to define these rules explicitly.**
From now on, the following rules are part of the specification.

## Two additional rules

**1. Policies are pure.**

The `admits` function has no side effects, and its result depends only on the duck it receives.
This means that the order in which a combinator checks its policies doesn't matter. 
The number of times each policy is checked doesn't matter either.
Your tests shouldn't require any specific evaluation order or number of calls.

**2. Combinators capture their policy lists at construction.**

When you create a combinator, you pass it a list of policies.
But what happens if you change that list later?
We don't specify this behavior. The combinator might notice the changes, or it might not. Both are acceptable.
So your tests shouldn't expect one specific behavior here.

## What does this mean for your tests?

Some tests that were acceptable in the basic exercise are no longer valid in the advanced version.
For example, you shouldn't write tests that:

- Check that `AllOf` stops evaluating policies after the first one returns `false`.
- Require policies to be evaluated in a specific order.
- Count how many times a policy is evaluated.
- Change the original list after creating a combinator and expect the combinator to use the updated list.

**Why? Because none of these behaviors are guaranteed by the specification.**
If your tests require them, the conformance check will report false alarms: 
your tests will reject implementations that are actually correct.

## But what if I tested this in the basic exercise?

That's completely fine!
For example, you might have written a test that detected the `allof-defensive-copy` mutant.
In the basic exercise, this was a valid decision because the specification didn't say what should happen when the original list changes.
But now we've made that decision explicitly: **both behaviors are allowed.**
So even if you made a different choice before, you'll need to update your tests to follow the new specification.
And this is actually an important part of writing good tests!
Your tests should check everything the specification guarantees, but they shouldn't require behavior that the specification leaves open.

## Why are these rules important?

When writing tests, it's easy to start checking every detail we can observe.
For example, we can use a spy to check how many times a function was called or in what order. 
We can also easily check whether changing a list affects a combinator.
But just because we *can* test something doesn't mean we *should*.
If the specification doesn't guarantee this behavior, adding such a test makes our test suite too strict.
And this can become a problem later.
Imagine someone changes the implementation to make it faster or simpler. 
The new implementation still follows the specification, but some tests fail because they depend on details that were never part of the contract.
**That's exactly what we want to avoid!**
The goal isn't to test every implementation detail. It's to test what the specification actually promises.
A good test suite should catch incorrect behavior without rejecting correct implementations.