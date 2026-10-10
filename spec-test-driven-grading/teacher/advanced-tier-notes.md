# The test-writing exercise — Advanced Tier: Teacher Notes

These notes explain the advanced version of the test-writing exercise, including the additional checks, 
how they work, and what we learned from running them with different AI agents.

## Why do we need an advanced version?

The basic exercise stays the same:

1. Find and fix the incorrect test.
2. Improve the test suite by thinking about missing cases.
3. Use mutation testing to find more gaps.

This works well with smaller local models. 
In our experiments, they caught around 36–63% of the graded mutants, 
so there's plenty for students to investigate and improve.
But for a strong frontier agent, the task can be too easy.

Claude caught all **11 out of 11 graded mutants** without seeing the mutation report.
This makes sense. The admission-policy implementation is small, the specification is complete, and the result is a Boolean value determined by four fields. A strong agent can work through most of the possible mistakes systematically.
Adding more mutants of the same kind probably wouldn't make the exercise much harder.
So the advanced version introduces three new challenges:

1. **Conformance testing:** Can the test suite accidentally reject a correct implementation?
2. **Test budget:** Can we cover the same behavior with fewer tests?
3. **Adversarial testing:** Can another AI agent find a realistic bug that the suite misses?

We also add two rules to the contract so the first challenge has a clear answer.

### Which frontier model did we use?

All frontier results in these notes come from **Claude Opus 5** (`claude-opus-5`), used interactively through Claude Code.
The model version was confirmed on October 9, 2026.
We originally recorded these runs only as `claude-code`. 
That wasn't precise enough to reproduce the experiments, so we later added the model information to the saved artifacts in `attacks/` and `hardened/`.

## The contract addendum

The advanced exercise includes a separate contract addendum.
**Don't add these rules to the `:core` KDoc!**
In the basic exercise, students are supposed to notice that the original specification doesn't answer two questions and make their own decisions.
The advanced version answers those questions explicitly:

> **Policies are pure.** `admits` has no side effects and depends only on the duck. The order in which policies are evaluated, and the number of times they're evaluated, are not part of the contract.
>
> **Policy lists are captured at construction.** If the caller changes the original list later, the combinator may or may not notice. Both behaviors are allowed.

This means the same tests can be valid in the basic exercise but invalid in the advanced one.
For example, a student might have decided that `AllOf` must use changes made to the original list and written a test that catches `allof-defensive-copy`.
That was a valid decision in the basic exercise.
But in the advanced version, **the contract explicitly allows both behaviors**, so that test would be too strict.
Make sure students understand why the rules are different. Otherwise, it can feel like we're suddenly penalizing them for something they previously did correctly.

---

## 1. Conformance testing: Does the suite reject correct code?

Mutation testing checks whether tests catch bugs.
Conformance testing checks the opposite: **do tests still pass when the implementation changes in a way the specification allows?**
The student project contains valid alternative implementations in:

`spec-test-driven/variants/catalog.json`

Run:

```bash
./gradlew verifyVariants --continue
```

Each variant is a correct rewrite of `:core`.
The test suite should stay green on every variant. 
If it fails, the report shows **FALSE ALARM** and names the failing tests.

### What do the variants change?

The student catalog intentionally doesn't explain each variant.
If we told students what behavior each variant changes, we'd also be telling them which tests to remove.

Here's the teacher reference:

| Variant                       | What changes                       | What tests must not require                             |
|-------------------------------|------------------------------------|---------------------------------------------------------|
| `allof-no-short-circuit`      | Uses `map { }.all { }`             | `AllOf` must stop after the first rejection             |
| `anyof-no-short-circuit`      | Uses `map { }.any { }`             | `AnyOf` must stop after the first acceptance            |
| `allof-reverse-order`         | Uses `policies.reversed().all { }` | Policies must be checked in list order                  |
| `allof-memoized`              | Caches the result for each duck    | A policy must be called a particular number of times    |
| `allof-defensive-copy`        | Uses `policies.toList()`           | Changes to the caller's list must affect the combinator |
| `allof-fold`                  | Uses `fold(true) { … && … }`       | The implementation must use `all { }`                   |
| `not-explicit-comparison`     | Uses `== false` instead of `!`     | No meaningful behavior difference                       |
| `max-budget-negated`          | Uses `!(price > maxPrice)`         | No meaningful behavior difference                       |
| `requires-accessory-contains` | Uses `name in accessories.map { }` | Accessory checks must be lazy                           |
| `min-accessories-count`       | Uses `count()` instead of `size`   | No meaningful behavior difference                       |

The first five are particularly useful because they expose tests that depend on implementation details.
The others help check whether a suite is too closely tied to how the code happens to be written.

### What happened in our experiments?

<text color="secondary" size="xs">Measured August 4, 2026</text>

We checked the archived test suites against all ten variants.
**Every suite that started from the provided tests passed all ten variants.**
That includes both Claude runs and all five local models.
But Claude's **86-test suite written from scratch** failed on three variants.
Four tests caused the failures. They checked evaluation order and short-circuit behavior in `AllOf` and `AnyOf`.

This gives us an interesting result:
**Over-specification depended more on how the tests were created than on how strong the model was.**
When Claude improved an existing suite, it didn't add these unnecessary requirements.
When it wrote a suite from scratch, it added tests that checked the order and number of policy evaluations.
None of the local models wrote these kinds of tests, so they didn't produce false alarms.
This also suggests that conformance testing may be especially useful in exercise 11.3, where students ask agents to create a test suite from scratch.
The fact that eight independent suites passed all ten variants also gives us evidence that the variants preserve the intended behavior.

### Which test suite should students check?

The slides show a command with a placeholder:

`-PmutantTests=<your suite>`

But students may need help knowing what value to use.
For **Run A**, where the agent writes a test suite from scratch, we recommend saving it under:

`test-suites/<name>/src/test/kotlin`

This is also where `runAgent -Pmode=tests` saves generated tests. The directory is git-ignored.
For **Run B**, where students improve the provided test suite, use the special value `learner`.
Here are the exact commands:

```bash
# Run A: tests written from scratch
./gradlew verifyVariants --continue -PmutantTests=test-suites/run-a/src/test/kotlin

# Run B: the provided suite, improved by the agent
./gradlew verifyVariants --continue -PmutantTests=learner
```

Both commands were checked in a clean export on September 18, 2026. They selected the intended suites and reported 10/10.
The slides focus on the difference between starting from scratch and improving existing tests. In practice, students also need to point the checker at the correct folder.

---

## 2. The test budget

Mutation testing rewards tests for catching bugs, but it doesn't care how many tests we write.
A suite with 47 tests might catch exactly the same bugs as a suite with 11.
Both could get a perfect mutation score.
But in a real project, tests are code we need to read, review, and maintain.
So the advanced exercise adds a **test budget**.

### How does it work?

Use `-PtestBudget=<n>` with `verifyMutants` or `verifyVariants`.
The tool counts the tests in the baseline suite and includes that number in the report.
If the suite exceeds the budget, the task fails after showing the full report.
Students don't need `-PmutantsStrict` to activate the budget. Specifying `-PtestBudget` is enough.

### Why did we choose 12 tests?

We didn't pick this number randomly.
Claude's original blind suite had 47 tests and caught all 11 graded must-kill mutants.
We analyzed which tests caught which mutants and found that **just seven tests were enough to catch all 11**.
One test, `a nested policy admits exactly the ducks it describes`, caught four mutants by itself.
Four mutants depended on a single test each: the two empty-list cases, prefix matching, and case sensitivity.
So we had two useful numbers:

- **7 tests:** The smallest set we found that catches all graded mutants.
- **47 tests:** The size of Claude's complete suite.

We chose a budget of **12 tests**.
That's enough room to write a useful suite without simply copying the seven tests that match the known mutants. But it's still a big reduction from 47.
**This budget is a teaching tool, not a general rule that every project should have fewer tests.**
Sometimes many small, readable tests are better than a few complicated ones.
Here, we deliberately introduce the constraint to practice choosing test cases carefully.

### What happened when local models knew the budget?

<text color="secondary" size="xs">Measured August 4, 2026</text>

We ran the Ollama models again with `-PtestBudget=12`.
This time, `runAgent -Pmode=verify-harden` included the budget in the prompt, so agents knew about it while writing tests.

| Model | Without a budget | With a 12-test budget |
|---|---|---|
| `qwen2.5-coder:1.5b` | 10 tests, 4/11 | 10 tests, 4/11 |
| `llama3.2:3b` | Did not compile | 12 tests, 4/11, but baseline invalid |
| `qwen2.5-coder:7b` | 13 tests, 5/11 | 10 tests, 4/11 |
| `qwen2.5-coder:14b` | 15 tests, 5/11 | 10 tests, 4/11 |
| `qwen2.5-coder:32b` | 15 tests, 7/11 | 12 tests, 5/11 |

Every model managed to stay within the budget.
But the models that previously caught more bugs now caught fewer.
**They responded by writing fewer tests, not by choosing better tests.**
For example, the 32b model went from catching 7 mutants with 15 tests to catching 5 mutants with 12 tests.
The budget didn't make it better at testing. It just made its suite smaller.
An interesting side effect: `llama3.2:3b` compiled successfully for the first time. Its previous suite had duplicate test names, and the shorter version avoided that problem. But its baseline was still incorrect, so its score wasn't evidence of a valid suite.
This is why the budget challenge is mainly useful with strong agents.

### How efficient were the original suites?

We also applied the 12-test budget to archived suites that had been written without knowing about it.

| Suite                                | Tests | Graded score | Must-kills per test |
|--------------------------------------|------:|-------------:|--------------------:|
| Smallest cover of the graded mutants |     7 |        11/11 |                1.57 |
| `qwen2.5-coder:1.5b`                 |    10 |         4/11 |                0.40 |
| `qwen2.5-coder:7b`                   |    13 |         5/11 |                0.38 |
| Teacher's test suite                 |    14 |         8/11 |                0.57 |
| `qwen2.5-coder:14b`                  |    15 |         5/11 |                0.33 |
| `qwen2.5-coder:32b`                  |    15 |         7/11 |                0.47 |
| Claude, blind hardening              |    47 |        11/11 |                0.23 |
| Claude, hardening with feedback      |    47 |        10/11 |                0.21 |

Among the archived agent suites, only the weakest one stayed within the budget, and it caught just 4/11 mutants.
None of those archived suites achieved both a perfect mutation score and the 12-test limit.
The frontier suites caught more bugs, but they also used many more tests.
That doesn't necessarily make them worse. A 47-test suite with clear, focused tests might be easier to maintain than a much shorter suite with complicated parameterization.
But it shows that **mutation score and test-suite efficiency measure different things**.

### Is the budget actually achievable?

Yes!
We later created a structurally improved suite for round 4 of the adversarial exercise.
It had **11 tests**, caught **11/11 graded mutants**, and passed **10/10 conformant variants**.
So the 12-test budget is achievable.
What we haven't yet tested is whether a frontier agent can reach that result when given the budget as a requirement from the beginning.

---

## 3. Adversarial testing

Now for the most interesting part of the advanced exercise!
Instead of checking a test suite against a fixed list of mutants, we ask another AI agent to find a bug the suite doesn't catch.
The agent receives the admission-policy specification and the learner's tests.
Its task is to write an implementation that:

- Passes the entire test suite.
- Breaks the specification in some realistic way.

The process uses:

```bash
# Generate an attack with an agent
./gradlew runAgent -Pmode=attack

# Check a saved attack
./gradlew verifyAttack -Pagent=<name> --continue
```

Unlike mutation testing, there's no fixed catalog of bugs.
Each time students improve their tests, the attacking agent can try a new approach.

### How do we know whether an attack is valid?

We don't need a separate answer key.
Instead, we run the attacking implementation and the correct implementation on the same randomly generated inputs.
The tool is called `DifferentialProbe`.
Originally, it used **2,000 seeded cases**. We later increased that to **20,000 cases** to catch subtler differences.
The two implementations can't share a classpath because they define the same classes in the same packages, so we run them separately and compare the recorded results.
There are three possible outcomes:

| Result                                              | Meaning                                                    |
|-----------------------------------------------------|------------------------------------------------------------|
| The test suite fails                                | The tests caught the attack.                               |
| The suite passes and the probe finds no differences | No incorrect behavior was found in the checked cases.      |
| The suite passes but the probe finds differences    | The tests missed a bug. The report shows a counterexample. |

The checker also accounts for tests that already fail on the correct implementation. Otherwise, a broken test could make every attack look successful.

### A demo attack

We keep a hand-written example in:

`attacks/demo-prefix/`

It incorrectly matches accessory names by prefix instead of exact name.
The original flawed test suite accepts it, but the differential probe found differences in **36 of 2,000 cases**.
Claude's blind test suite caught it.
This is a useful demonstration because it works without an API key.

### Can local models create attacks?

<text color="secondary" size="xs">Measured August 4, 2026</text>

We asked five Ollama models to attack Claude's 47-test suite, which already caught all 11 graded mutants.
None produced a successful attack.

| Attacker             | What it produced                                                     | Result                                  |
|----------------------|----------------------------------------------------------------------|-----------------------------------------|
| `qwen2.5-coder:1.5b` | A copy of `:core`                                                    | No attack                               |
| `llama3.2:3b`        | A copy of `:core`                                                    | No attack                               |
| `qwen2.5-coder:7b`   | A copy of `:core`                                                    | No attack                               |
| `qwen2.5-coder:14b`  | Refused the task                                                     | No code                                 |
| `qwen2.5-coder:32b`  | Made `RequiresAccessory` accept any duck with at least one accessory | Bug found, but existing tests caught it |

The 32b model understood the task, but its bug was too obvious.
The implementation disagreed with the correct one in 222 of 2,000 cases, but Claude's suite, the 32b model's own suite, and even the flawed starter suite all caught it.
So **this is really a frontier-versus-frontier exercise**.
Students with smaller local models can still complete the basic exercise, but an unsuccessful attack from a weak model doesn't tell us much about the quality of the test suite.

#### What if an agent refuses?

The 14b model refused because the prompt asked it to deliberately write code that violated a specification.
This can happen even though the task is ordinary software testing.
A useful alternative is to ask the agent to:

> Find an implementation that this test suite cannot distinguish from the correct one, even though their behavior differs.

The goal is still the same, but this wording makes the testing purpose clearer.
We also learned something about the tool integration: most local models didn't follow the expected `// FILE:` output format. The attack builder therefore identifies which source file to replace from the class declarations in the response.
And when a model simply copies `:core`, the checker reports it as identical, not as a successful attack.

---

## 4. What happened when a frontier agent attacked?

This is where the experiment became much more interesting.
We asked Claude to attack another Claude-generated suite.
The target was the **47-test suite that had already caught all 11 graded mutants**.

### Round 1: A perfect mutation score wasn't enough

<text color="secondary" size="xs">August 4, 2026 — blind attack, prompt E</text>

Claude found an implementation that passed the entire test suite but disagreed with the correct implementation in **50 of 2,000 cases**.
It exploited two missing inputs.

**First: an empty accessory name.**
The tests checked case sensitivity and prefix matching, but never created `RequiresAccessory("")`.
The attack made that policy admit every duck.

**Second: negative prices.**
The tests covered several price boundaries, but never used a negatively priced duck.
The attack changed `MaxBudget` to treat negative prices as zero:

```kotlin
price.coerceAtLeast(0)
```

That broke cases such as a duck priced at −2 with a budget of −1.
The original specification admits that duck, but the attack refuses it.
So we had a very clear result:

**11/11 mutants caught — and the test suite still missed real bugs.**

That's an important lesson for students. A perfect mutation score means the suite caught all the bugs in the mutant catalog, not every bug that could exist.

### The checker had its own blind spot

The attacking agent also noticed another possible bug.
The tests checked `AllOf` and `AnyOf` with zero, one, or two children, but didn't properly cover longer lists.
An implementation that checked only the first three policies could pass.
At first, the differential probe couldn't detect this either because it generated lists with only zero to two children.
We confirmed the problem: the incorrect implementation passed the tests, and the probe reported no differences.
After increasing `MAX_WIDTH` to 6, the same implementation disagreed in **17 of 2,000 cases**.

**The checker can only find problems in the inputs it generates.**
This is important because the checker itself is another piece of software with assumptions and limits.
A clean result isn't proof that the implementation is correct.

### Round 2: Feedback didn't help yet

<text color="secondary" size="xs">August 4, 2026 — prompt F</text>

We tried another attack, this time allowing feedback.
The agent succeeded immediately: all 47 tests passed, but the probe found **45 disagreements in 2,000 cases**.
The attack added a condition to `MaxBudget`:

```kotlin
duck.price >= 0 && duck.price <= maxPrice
```

Again, the suite never tested negative prices.
Because the attack succeeded on the first attempt, the agent didn't need to use feedback to improve it.
So we still couldn't answer whether feedback helps an attacking agent or causes it to focus too much on the checker.

### Four gaps with the same pattern

Across the first two rounds, we found four real gaps:

| Missing case                                             | How we found it  | Differences in 2,000 cases |
|----------------------------------------------------------|------------------|---------------------------:|
| `RequiresAccessory("")` accepts everyone                 | Round 1 attack   |                         45 |
| `MaxBudget` refuses negative prices                      | Rounds 1 and 2   |                         45 |
| `AllOf` checks only the first three policies             | Round 1 analysis |                         17 |
| `MinAccessories` counts only the first three accessories | Round 2 analysis |                         37 |

All four had something in common.
**The test suite never constructed the inputs needed to expose them.**
It didn't use empty required names, negative prices, or sufficiently long collections.
Mutation testing can tell us whether our tests distinguish different implementations on the inputs they use.
But it can't tell us which inputs we never thought to test.
That's one of the biggest lessons from this experiment.

### The probe missed another case

Even after increasing the width of generated lists, the probe still didn't generate `MinAccessories` arguments above 3.
So an implementation that counted only the first three accessories could still look correct to the probe.
We fixed this by adding checks for the generated corpus itself.
Now the probe verifies that its inputs include:

- Negative prices.
- Empty required accessory names.
- Ducks with at least four accessories.
- `MinAccessories` thresholds greater than three.

If those cases disappear from the generated corpus, the check fails instead of silently losing coverage.

---

## 5. Round 3: Missing combinations

We created `round3-target`, a test suite that fixed all four earlier gaps.
It caught all 11 graded mutants, passed all 10 variants, and detected every previous attack.
But the next attack succeeded immediately.
All **52 tests passed**, while the probe found **21 differences in 2,000 cases**.

### What was the bug?

The attack changed `MinAccessories` from counting all accessories to counting only those with nonblank names:

```kotlin
accessories.count { it.name.isNotBlank() } >= min
```

The test suite already included empty accessory names.
It also tested ducks with more than three accessories.
But it never combined those two cases.
Empty names appeared in `RequiresAccessory` tests, while `MinAccessories` tests used only named accessories.
**This was a new kind of gap: not a missing value, but a missing combination of values.**
That's much harder to fix by simply adding more boundary cases.

### An important limitation of this run

This experiment wasn't fully blind.
The agent worked in a checkout that still contained earlier attacks, other suites, and generated results.
It also found a way to run its own checks using `kotlinc`, even though the prompt told it not to run Gradle tasks.
So we can't claim that this was a clean blind experiment.
The new bug was real, but the experiment didn't prove that the agent found it without access to feedback.
The lesson for us was simple: **telling a capable agent not to run one tool doesn't necessarily prevent it from recreating the same check another way.**
We needed a cleaner experiment.

---

## 6. A proper blind-versus-feedback comparison

We ran two new experiments using prompts G and H, each in a clean export.
In the blind run, we confirmed that no compiled files, build directories, or previous probe results were available. The agent was not allowed to execute code, and it only wrote the attack implementation.
Both agents succeeded on their first attempt.

| Run           | Attack                                       | Differences in 2,000 cases | Attempts |
|---------------|----------------------------------------------|---------------------------:|---------:|
| Blind         | `AllOf` accidentally skips the fourth policy |                          7 |        1 |
| With feedback | `KotlinOnly` also requires `price >= 0`      |                         26 |        1 |

Feedback didn't provide any measurable advantage because neither agent needed a second attempt.
So the question about feedback influencing the agent's search is still open.

### Why did these attacks work?

Both problems came from how we had improved the suite after previous attacks.
We had added individual tests for specific counterexamples.

For example:

- To check that `AllOf` looks past the third policy, we used a five-policy list where the important policy was at index 4. But index 3 remained untested.
- To check negative prices, we added them to the `MaxBudget` tests. But no negatively priced duck appeared in the shared corpus used by `KotlinOnly` and other policies.
- We tested accessory lists of sizes 1 and 5, but not exactly 4.

So the fixes were too narrow.
**We fixed the exact examples the agents found, but not the larger groups of similar bugs.**

### What should we do instead?

Put important input values into a shared test corpus.
Then use parameterized tests to check different list lengths, positions, and combinations.
For example, instead of testing one list of five policies with a rejecting policy at a particular position, check different positions across different list lengths.
This helps us test a whole category of behavior rather than one example.

---

## 7. Making the probe more sensitive

The blind attack from the previous round disagreed with the correct implementation in only **7 of 2,000 cases**.
That means it behaved correctly on 99.65% of the generated inputs.
A slightly more subtle bug might have escaped the probe entirely.
So we increased the corpus to **20,000 cases**.
The probe still ran in about a second, so the extra coverage was inexpensive.
With the larger corpus, the two attacks produced 91 and 290 disagreements.
**Important:** Results recorded with 2,000 cases aren't directly comparable with results recorded using 20,000.

---

## 8. Round 4: Structural tests are better, but still not perfect

We created `round4-target` using the more systematic approach.

Instead of adding one test per discovered bug, we:

- Put important values into a shared corpus.
- Tested combinators with different numbers of policies.
- Checked every possible position of a deciding policy.
- Described leaf behavior using general rules rather than a few examples.

The result was much smaller:

**11 tests, 11/11 graded mutants, 10/10 variants.**

It also caught all six previous attacks and stayed within the 12-test budget.
This was a big improvement!
But the next attack still got through on its first attempt.

The suite passed, while the probe found **239 differences in 20,000 cases**.

### What did the attack change?

It changed how `RequiresAccessory` compares names.
The implementation trimmed whitespace from the required name but not from the accessory name.
For example, a duck wearing `Accessory("Scarf ")` would no longer satisfy `RequiresAccessory("Scarf ")`.

### Why didn't the tests catch it?

The test suite already included `"Scarf "` in its list of interesting names.
But that list was only used for the **required name**.
No duck in the shared corpus actually wore an accessory with a trailing space.
So we had covered the value, but only in one role.

**The important lesson: a value appearing somewhere in your tests doesn't mean you've tested every way that value can be used.**

The agent had also considered several other bugs, including case-insensitive matching, prefix matching, duplicate handling, negative-price behavior, and off-by-one errors.
The structural suite caught all of those.
The successful attack had to exploit a more subtle relationship between the inputs.

---

## 9. Round 5: The problem moved from values to structure

We improved the suite again.
In `round5-target`, both required accessory names and the names ducks actually wear came from the same shared list.
We added names with leading and trailing spaces, along with a few other missing values.
The suite still had **11 tests**, caught **11/11 graded mutants**, passed **10/10 variants**, and detected all seven earlier attacks.
We expected that only very artificial bugs might remain.
But the next agent proved us wrong!
All 11 tests passed, while the probe found **165 differences in 20,000 cases**.

### What was the new bug?

The attack tried to simplify nested policies by removing children that appeared to have no requirements.
It used this helper:

```kotlin
private fun AdmissionPolicy.statesNoRequirement(): Boolean = when (this) {
    is AllOf -> policies.isEmpty()
    is AnyOf -> policies.isEmpty()
    else -> false
}
```

The problem is that empty `AllOf` and empty `AnyOf` have different meanings.

- `AllOf(emptyList())` admits every duck.
- `AnyOf(emptyList())` admits no ducks.

Treating both as something that can be ignored changes the result.
For example:

```kotlin
AnyOf(listOf(AllOf(emptyList())))
```

This should admit every duck, but the attack makes it reject them.

### Why didn't the tests catch it?

The suite tested empty `AllOf` and empty `AnyOf` individually.
But it never used an empty combinator **inside another combinator**.
The shared corpus covered many interesting values, but we were still writing policy-tree structures manually.
So we had moved from missing values to missing combinations, then to missing roles, and finally to missing tree shapes.

Here's the progression:

| Round | What we improved                             | What the next attack found                  |
|-------|----------------------------------------------|---------------------------------------------|
| 3     | Added tests for individual counterexamples   | A missing combination of values             |
| 4     | Used a shared corpus and parameterized tests | A value covered in one role but not another |
| 5     | Used the same values in both roles           | A missing nested policy structure           |

**Whatever we still test using only a few hand-picked examples can become the next gap.**
The natural next improvement is to build a shared corpus of policy-tree shapes too.
For example, include empty `AllOf` and `AnyOf` as possible children, not just as top-level policies.
Then test them in different positions and combinations.

### What does this tell us about realistic bugs?

The round-5 agent also made an interesting observation.
This attack was realistic for an implementation that someone had tried to simplify or extend, but it wasn't a likely typo in the original `:core` code.
That's an important distinction.
As we improve the suite, finding realistic mistakes becomes harder. The agent may need to invent increasingly complicated changes to create a bug.
So the goal isn't necessarily to reach a point where no attack is possible.
**The goal is to understand how far we can improve the suite before the remaining attacks stop looking like mistakes developers would actually make.**

### One more limitation of the probe

The agent also found a real overflow problem that the probe couldn't detect.
An implementation of `MaxBudget` using:

```kotlin
maxPrice - duck.price >= 0
```

can overflow for extreme integer values.
The suite doesn't catch this, but the probe generates budget limits only in the range −1 to 11.
So the differential check would report no differences even though the bug is real.
This is another reminder that **a clean report tells us as much about the inputs our checker can generate as it does about the code we're checking**.

---

## What should teachers emphasize?

The most important lesson from these experiments isn't that one model is better than another.
It's that **test quality has several dimensions, and no single score captures all of them**.
Mutation testing helps us find bugs our tests miss. Conformance testing helps us find tests that are too strict. A test budget encourages us to choose cases more carefully. Adversarial testing helps us discover gaps that our existing tools never considered.
The adversarial rounds also show why simply adding a test for each discovered bug isn't enough.
We need to ask a bigger question:
**What whole category of mistakes does this test protect us against?**
And even when we improve the structure of our tests, we should keep looking at the relationships between values, their roles, and the shapes of the objects we're testing.
That's the real goal of the advanced tier: not just getting a green report, but learning how to evaluate the quality of the tests themselves.