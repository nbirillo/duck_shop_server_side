# Write the specification

In the previous exercises, you were given a specification. Now it's your turn to write one!
The shop wants to add promotions. Your teacher will give you a short description of what the business wants, 
probably just a few lines. In real projects, this is pretty common. You get a short description, 
but it's not enough to start implementing the feature.

**Your task is to turn this description into a clear specification** that another developer 
or an AI agent can use to implement the feature without asking you more questions.
The important part is that two different developers following your specification should get the same results.

## What are we specifying?

```kotlin
sealed interface DiscountRule {
    data class Percentage(val percent: Int) : DiscountRule
    data class AmountOff(val amount: Int) : DiscountRule
    data class BigSpenderBonus(val threshold: Int, val amount: Int) : DiscountRule
}

/** Returns what [duck] actually costs in a shop whose active promotions are [rules]. */
fun priceFor(duck: Duck, rules: List<DiscountRule>): Int
```

You already know the `Duck` type from the previous exercises. 
It has `name`, `price`, `hasKotlinAttribute`, and `accessories`.

## What should you do?

Copy [`SPEC-template.md`](SPEC-template.md) to `SPEC.md` and fill in all the sections.
Pay special attention to the **"deliberately not specified"** section. We'll come back to why it's important.

## What makes this exercise difficult?

Read the business description carefully. You'll probably notice that some things are not explained.
For example, what happens if several discounts apply to the same duck? Can they be combined? In what order?
Different developers might make different decisions here, and their implementations might return different prices.

**Your job is to find these unclear cases and decide how they should work.**

There isn't always one correct answer.
What's important is that you make these decisions on purpose, explain them, and write them down clearly.

## How will we check your specification?

We'll check your work in several ways:

1. **Structure and completeness.** Did you fill in all the sections? Did you cover all the important parts of the feature, including edge cases?
2. **Properties.** We'll turn the rules from your specification into executable properties and test them against a correct implementation and some intentionally broken ones.
3. **Coverage.** We'll check whether your specification covers all the parts of the function and its types.
4. **Implementation by AI agents.** We'll give your specification to an agent and ask it to implement the feature using only your text.

The last part is especially interesting. If the agent misunderstands something, 
it might mean that your specification wasn't clear enough.

## How to run the checks

Run all commands from the `spec-test-driven/` directory.
Make sure you use the same specification file for all the checks.

### Step 1. Check structure and coverage

```bash
SPEC=exercises/write-spec/SPEC.md

./gradlew verifySpec -Pspec=$SPEC
```

This checks whether you've filled in the required sections and mentioned all the important parts of the feature.

### Step 2. Test your properties

You can add any number of Kotlin files (`.kt`) to the `properties/` directory. You can name them however you like.
Then create a sandbox and run the tests:

```bash
./gradlew sandbox -Pname=a -Pspec=$SPEC
./gradlew -p sandbox/a test
```

The sandbox is a separate Gradle project. 
It contains your specification and the required types, but not the original implementation.
It doesn't have its own `gradlew`, so you should run all commands from the main project directory and use `-p` to point to the sandbox.

### Step 3. Let two agents implement your specification

Now let's see if two different agents understand your specification in the same way.
You'll need two different AI agents. 
Both should be capable of completing the task. 
Otherwise, differences might come from the agents' abilities rather than your specification.
The first agent works in the sandbox, and the second one runs through an API.

```bash
./gradlew sandbox -Pname=a -Pspec=$SPEC

# Open sandbox/a in your agent and ask it to implement the feature.
# This is a manual step.

./gradlew prepareImplementation -Pagent=reader-a -Pspec=$SPEC -Pfrom=sandbox/a

./gradlew runAgent -Pmode=impl-from-spec -Pagent=reader-b \
  -Pprovider=ollama -Pmodel=qwen2.5-coder:32b -Pspec=$SPEC

./gradlew verifyDivergence -PagentA=reader-a -PagentB=reader-b
```

The last command compares the results of both implementations on the same set of inputs.
It shows where the agents returned different answers.

**Don't focus only on the number of differences. Look at the actual cases!**

If two agents made different decisions, check whether your specification explains what should happen in that case.
Sometimes you might intentionally leave something unspecified. That's fine too! Just explain it in section 4 of the template.

## What if the checker is wrong?

One more important thing: **our checker isn't perfect.**
Checking a specification is actually quite difficult. 
One part of our tooling uses an AI model to read your text and extract the rules you've written.
And yes, it can make mistakes!
For example, we've seen cases where the checker reported a missing rule even though it was clearly written in the specification.
So if the checker says something is missing, but you think it's there, don't immediately change your specification just to make the checker happy.
Instead, you can try a few things:

1. **Write down your claims yourself.** Make a list of the rules your specification defines, one per line. 
You can then compare your list with what the checker found. The properties will still be tested against correct and intentionally broken implementations.
2. **Check the results of the other steps.** If your properties pass and an agent can implement the feature correctly using your specification, that's useful evidence that your text was clear enough.
3. **Read your specification again.** Find the exact sentence that explains the rule. Is it really clear? Or could someone understand it differently?

Also, remember that writing a rule doesn't automatically make it correct. It still needs to work with the correct implementation and help distinguish correct behavior from incorrect behavior.
Sometimes the checker will be wrong. Sometimes it will help you find something you missed. Both are useful!
And this is actually one of the main ideas of the course: **don't blindly trust the output of a tool, even when that tool is checking your work.**
Always check the results, ask questions, and make sure they make sense.