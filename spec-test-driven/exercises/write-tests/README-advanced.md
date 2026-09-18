# Verify and harden, advanced — three harder questions

Take this tier if the basic exercise felt **too easy for the agent you were working with**: it fixed the
wrong test, filled the gaps, and the mutation report came back clean without much of a fight.

That is not a sign you did it wrong. It is a sign the task was small enough for your agent — the
specification is complete, the algebra is a boolean function of four fields, so the whole space of
defects is a checklist a strong agent already carries. This tier puts the difficulty back.

**Be warned about what that costs.** All three additions below only bite a **strong** agent. With a
small local model two of them come out trivially clean and the third it cannot perform at all — so a
green result here would tell you nothing. That is a fact about these three checks, not a ranking of
models.

First, read [`contract-addendum.md`](contract-addendum.md). It closes two questions the basic tier
deliberately left open, and everything below depends on the answers.

> If you pinned either of those questions down in the basic tier, **you were right to** — the question
> was open and you closed it. Here it has been closed the other way, in writing, and the rules follow
> the addendum. An open question is answered by whoever is entitled to answer it; after that, your job
> is to respect the answer in *both* directions — pin what the contract promises, leave alone what it
> does not.

---

## 1. Did you forbid a change that was allowed?

Mutation testing asks whether your tests notice a defect. Nothing so far has asked whether they
**fail on code that is also correct** — and a suite can reach a perfect score by pinning down things
that were never promised. Those tests pass today and break the first honest refactor, punishing a
change the contract permits.

So: the same engine, the opposite expectation. Each entry in `../../variants/catalog.json` is a
**legal rewrite** of `:core`, and your suite is supposed to stay **green** on all of them. A failure is
reported as a FALSE ALARM, with the names of the tests to relax. See
[`../../variants/README.md`](../../variants/README.md) for how to read it.

```bash
./gradlew verifyVariants --continue
./gradlew verifyVariants -PmutantsStrict     # fails while any false alarm remains
```

A false alarm is never the variant's fault. The honest fix is to weaken or delete **your test**.

### Worth running as an experiment

Get two suites for the same algebra and check both:

- **Run A — from nothing.** Ask your agent to write a suite for `../../core/…/Leaves.kt` and
  `Combinators.kt`, with no starting point.
- **Run B — from the given one.** Hand it `src/test/kotlin/…/PolicyTests.kt` and ask it to fix what is
  wrong and add what is missing — the basic exercise, in other words.

```bash
./gradlew verifyVariants --continue -PmutantTests=path/to/your/src/test/kotlin
```

Predict it first: which of the two do you expect to fail on a legal rewrite, and why? The answer says
something about where over-specification comes from, and it is not what most people guess.

---

## 2. Could you have said it in fewer tests?

Mutation scoring rewards every distinction you pin and charges **nothing** for the tests you spend, so
there is never a reason to choose between them. That is why suites drift to forty tests and nobody
notices.

It matters because you are still in the loop. A suite is code you will read, review and keep working
in — a dozen tests that each earn their place are a better thing to inherit than fifty that overlap,
and you are the one inheriting them.

**Derive your own cap** from what you have already measured — the reports from the basic tier, and
your own runs — and be ready to say why it is neither the floor nor forty-seven:

- aim at the floor and you are only writing the tests that kill the defects you were *shown*, which is
  the trap the basic tier warned about;
- aim too loose and the constraint does nothing at all.

```bash
./gradlew verifyMutants --continue -PtestBudget=<your number>
```

The report prints in full first, then fails if the suite is over. Naming a budget is opting in to it.

The technique that makes a small suite possible is **parameterising instead of enumerating**: put the
interesting values in one shared corpus that every test runs over, and sweep arity and position
instead of picking one of each. One test per *axis*, not one per case.

---

## 3. Can somebody get past you on purpose?

Nothing says you only get one agent. **Agents can check each other**, and a second opinion costs one
more prompt — a different model has different blind spots, which is the entire value of asking twice.

So when an agent hands you work that looks right and you still doubt it, reading it more carefully is
not the move. Give it an opponent:

> Write an implementation that passes every test in this suite and still contradicts the
> specification. If no such implementation exists, say so instead of forcing it — and name the tests
> that close the door.

**The opponent has to be at least as strong as the agent whose work you are checking.** A weaker one
comes back empty-handed and that means nothing at all.

Save its answer in a folder you name yourself, one per attempt, then:

```bash
# attacks/my-attack/src/main/kotlin/…/Leaves.kt
./gradlew prepareAttack -Pagent=my-attack
./gradlew verifyAttack  -Pagent=my-attack --continue
```

### Reading the verdict

The check needs no answer key. Your attack and the untouched algebra each answer the same 20 000
seeded random cases, and the two recordings are compared — the implementations cannot be compared
directly, since they declare the same classes in the same package and can never share a classpath.

| Report | What it means |
| --- | --- |
| your suite **fails** | it caught the attack |
| suite passes, **no divergence** | a correct implementation written differently — not an attack |
| suite passes, **they diverge** | your suite has a hole, and the first disagreeing case is printed |

### Who invents the attack

Either let the agent find its own angle — you learn what it reaches for first, and whether it will
admit it cannot find anything — or decide the angle yourself and ask only for the implementation. The
second is harder and the better exercise: you have to imagine a defect that is both plausible **and**
invisible to your suite, which is the same imagination the suite needed in the first place.

Either way, insist on a **plausible** defect — a mistake somebody could really make — rather than an
**arbitrary special case** on one value. A defect that singles out one value (*if the price is exactly
seven, answer the other way*) is always available, and it only tells you the suite is finite, which it
always is.

### Then do it again

Attack, close the hole, attack again, and **count the rounds**. That number says more about your suite
than any score does. After each one, ask whether the hole you closed was a **class** of mistake or
just that one case — closing the case tends to leave its neighbours open.

And know when to stop. You are not trying to exhaust the opponent: on an algebra this small, a capable
one will always find something, because a finite suite always has inputs nobody tested. **You stop
when the attacks stop being mistakes anyone would plausibly make.** That call is yours, and making it
is the skill this tier is for.
