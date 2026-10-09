# Specification — `priceFor`

Copy this file to `SPEC.md` and fill in all the sections.
**Please keep the headings as they are!** 
We use them to check whether your specification is complete, and each section has its own purpose.

## 1. What it does

In one or two sentences, explain what `priceFor` does.
Imagine you're explaining it to someone who hasn't seen the code before. 
What does the function return?

## 2. Behaviour

This is the main part of your specification!
Here, you need to describe **how the price is calculated**. 
Try to be as clear as possible so that two different developers following your specification would get the same results.
Every rule you write should be something we can actually check.

For example:
- ❌ "Discounts should be applied correctly." — What does "correctly" mean?
- ✅ "A percentage discount reduces the current price by `percent` percent." — Now we know exactly what to check!

The business description is quite short, so there will probably be many things it doesn't explain.
**Your task is to find these unclear cases and decide how they should work.**
For each rule, ask yourself: could two developers understand this differently and calculate different prices?
If the answer is yes, you probably need to explain it better.

## 3. Edge cases

Now think about cases that are valid but might be easy to forget.
For example, what happens with empty lists, zero values, boundary values, very large numbers, or unusual combinations of discounts?
For each case, explain **what the function should actually return**.
Don't just write "this case should be handled." We need to know how!

## 4. Deliberately not specified

Sometimes you might decide that certain details don't need to be specified.
That's completely fine!
In this section, explain **what you're intentionally leaving open and why**.
This means that different implementations can behave differently in these cases, and both can still be correct. 
Your tests shouldn't require one specific behavior either.
But be careful!
If you leave too many things open, different implementations might return different results when they shouldn't.
On the other hand, you also don't need to define every tiny implementation detail if it doesn't matter for the feature.

## 5. Open questions

Sometimes the business description doesn't give you enough information, and you can't make the decision yourself.
In this section, write down the questions you would ask the business.
For each question, explain:

- What information is missing?
- What would you ask the business?
- What are you assuming for now so that implementation can continue?

**What's the difference between this section and section 4?**

It's quite simple:

- **Section 4:** We don't need to decide. Different behaviors are acceptable.
- **Section 5:** We need a decision, but we don't know the answer yet.

And that's it! The goal is to make your specification clear enough that someone else can implement the feature without having to guess what you meant.