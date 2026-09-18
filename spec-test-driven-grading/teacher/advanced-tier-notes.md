# Exercise 11.2, advanced tier — teacher notes

The basic tier of 11.2 is unchanged: fix the invalid test, harden the suite by reasoning, then check
it with mutation testing. It discriminates the local model tier well (36–63%) and a learner working
with a weak agent is fully occupied by it.

It does not challenge a frontier agent. Claude reaches 11/11 blind on the graded mutant set, and more
mutants of the same kind would not change that: the specification is complete and machine-readable,
the object is a pure boolean function of four fields, so the defect space is enumerable by a
checklist a strong agent already has. What is missing is not difficulty, it is a second and a third
direction. The advanced tier adds three constraints and a contract addendum that makes the first of
them well-founded.

**Which frontier agent.** Every "frontier" result in these notes is **Claude Opus 5** (`claude-opus-5`), driven interactively through Claude Code on its default model, checked 2026-08-05. That was recorded late: the runs themselves only ever said `claude-code`, which breaks the same pin-the-version rule we apply to the Ollama tier and would have made the numbers irreproducible. The artefacts under `attacks/` and `hardened/` have been backfilled.

## The contract addendum

The addendum belongs to this tier only, and lives in the exercise folder as its own file — **never as
an edit to the `:core` KDoc**, because the basic tier's lesson is precisely that the specification is
silent on these points and the learner has to decide. It states:

> Policies are pure: `admits` has no side effects and depends only on the duck. The order in which a
> combinator evaluates its policies, and how many times, are therefore not part of the contract. A
> combinator captures its policy list at construction; mutating the caller's list afterwards is
> unspecified.

So the same two questions change role between tiers. In the basic tier the learner decides them and
either answer counts, as long as it is a decision. In the advanced tier they have been decided, and
pinning them is now a defect. The advanced README says that in one line — without it a learner who
legitimately killed `allof-defensive-copy` in the basic tier gets penalised for it here and
reasonably concludes the rules changed at random.

## 1. Conformant variants — the suite must not over-specify

`spec-test-driven/variants/catalog.json`, run with `./gradlew verifyVariants --continue`. Same engine
as the mutants, opposite expectation: each entry is a legal rewrite of `:core`, the suite has to stay
GREEN, and a failure is reported as FALSE ALARM together with the tests to relax.

The catalog carries no prose, for the same reason the practice mutants carry none — naming the
behaviour each variant frees would say which tests to drop before the learner has worked it out. The
rationale therefore lives here:

| Variant | Rewrite | What a suite has to avoid to survive it |
| --- | --- | --- |
| `allof-no-short-circuit` | `map { }.all { }` | asserting that evaluation stops at the first rejection |
| `anyof-no-short-circuit` | `map { }.any { }` | the same for the first acceptance |
| `allof-reverse-order` | `policies.reversed().all { }` | asserting the order policies are consulted in |
| `allof-memoized` | caches the verdict per duck | asserting how many times a policy is consulted |
| `allof-defensive-copy` | `policies.toList()` | pinning that the caller's list stays live |
| `allof-fold` | `fold(true) { … && … }` | depending on `all {}` specifically |
| `not-explicit-comparison` | `== false` instead of `!` | — (syntactic; a sanity entry) |
| `max-budget-negated` | `!(price > maxPrice)` | — (syntactic) |
| `requires-accessory-contains` | `name in accessories.map { }` | asserting the accessories are scanned lazily |
| `min-accessories-count` | `count()` instead of `size` | — (syntactic) |

The first five have teeth; the rest are there so a suite that has fitted itself to the shape of the
code rather than to its behaviour shows up.

**Measured, 2026-08-04.** Every archived suite that *hardened a given suite* — both Claude runs and
all five local models — accepts all ten, zero false alarms. The only suite that trips it is Claude's
86-test suite written **from scratch**: three variants, four tests, all of them about evaluation
order and short-circuiting (`AllOf/AnyOf evaluates its policies in list order`, `AllOf/AnyOf
short-circuits after the first rejection/admission`).

Two conclusions worth carrying into the materials:

- Over-specification tracks **authoring freedom**, not model strength. The same agent hardening a
  given suite produces none of it; inventing a suite from nothing, it reaches for spies and order
  traces. So this check belongs to 11.3, where the learner's agent writes a suite from scratch, more
  than to 11.2.
- No local model over-specifies at any size — they never reach for a spy in the first place. The
  check is therefore free for a weak-agent learner and only bites a strong one, which is exactly what
  an advanced tier should do.

That eight independent suites stay green on all ten is also the evidence the rewrites really are
behaviour-preserving.

### Which suite to point the check at — the one thing the slide leaves out

The slide for this exercise prints one command with a placeholder (`-PmutantTests=<your suite>`) and
asks the learner to run it for both arms. Deliberately short — but it hides two things a learner asks
within a minute, so answer them at the board:

- **Run B — the suite they were given and edited in place.** The value is the literal **`learner`**, not
  a path: the task expands it to `exercises/write-tests/src/test/kotlin` itself and prints
  `suite: learner (exercises/write-tests/src/test/kotlin)`. Nothing to look up.
- **Run A — the suite an agent wrote from nothing.** The slide says to have one written but never says
  where to put it. Any source root works; the house location is **`test-suites/<name>/src/test/kotlin`**
  — the same place `runAgent -Pmode=tests` writes to, and git-ignored, so their runs never end up in a
  commit.

```bash
./gradlew verifyVariants --continue -PmutantTests=test-suites/run-a/src/test/kotlin   # run A
./gradlew verifyVariants --continue -PmutantTests=learner                             # run B
```

Both verified in a clean export (2026-09-18): each prints the suite it really used and reports 10/10.
Worth saying out loud that the **path is the second thing that differs between the arms** — the slide's
own line is "the only thing that changes is where the agent starts", and the suite moving with it is the
same fact seen from the filesystem.

## 2. The test budget

`-PtestBudget=<n>` on `verifyMutants` or `verifyVariants`. The suite size is read off the baseline run
and always printed; with a budget it becomes a constraint, and the task fails after printing the full
report. Naming a budget is opting in to it, so it does not wait for `-PmutantsStrict`.

**The number is calibrated, not guessed.** Taking Claude's blind 47-test suite and asking which of
its tests kill which of the 11 graded must-kill mutants, a greedy cover shows **seven tests suffice**.
One of them, `a nested policy admits exactly the ducks it describes`, accounts for four mutants on its
own; four mutants are killed by exactly one test each (both empty-list cases, prefix matching, case
sensitivity) and are the fragile part of the set.

So the floor is 7, the suites that score 100% spend 47, and **the budget is 12** — real headroom, and
still a fourfold cut.

Say plainly in the materials that the budget is a teaching device rather than a style rule. It trades
the readability of one-assertion-per-test suites for the skill of choosing the discriminating case,
and it only works while the graded set stays hidden: a learner who knew the mutants would simply
write those seven.

### Told the budget in advance, the local tier just writes less (2026-08-04)

`runAgent -Pmode=verify-harden` now appends the limit to the prompt when `-PtestBudget` is set, so the
agent knows about it while writing rather than meeting it afterwards. The whole Ollama matrix, rerun
at `-PtestBudget=12` and scored against the graded set:

| Model | Unconstrained | Under the budget |
| --- | --- | --- |
| qwen2.5-coder:1.5b | 10 tests, 4/11 | 10 tests, 4/11 |
| llama3.2:3b | did not compile | 12 tests, 4/11 — but **baseline invalid**, one test contradicts `:core` |
| qwen2.5-coder:7b | 13 tests, 5/11 | 10 tests, **4/11** |
| qwen2.5-coder:14b | 15 tests, 5/11 | 10 tests, **4/11** |
| qwen2.5-coder:32b | 15 tests, 7/11 | 12 tests, **5/11** |

Every one of them now fits. Every one that had anything to lose lost it. They obey the constraint by
writing fewer tests, not by choosing better ones — 32b's efficiency even slipped, from 0.47 must-kills
per test to 0.42. The budget does not make a weak agent sharper; it makes it smaller.

(Amusing side effect: llama3.2:3b compiles for the first time. Its earlier suite failed on duplicate
test names, and a shorter suite has fewer chances to repeat one. Its baseline is still red, so the
4/11 is honest arithmetic on a broken suite.)

So the budget joins the other two: **all three advanced-tier elements are frontier-only.** A learner
working with a local model gets nothing from any of them, which is worth saying once, plainly, in the
tier's framing rather than discovering three times.

The target is provably reachable, though, which matters for fairness: the structurally hardened suite
written for round 4 scores **11/11 on the graded set with 11 tests**, and 10/10 on the conformant
variants at the same time. Twelve is not a trick. Still untested: a frontier agent asked to hit it.

### Nothing in the archive satisfies it (2026-08-04)

Every archived suite scored against the graded set with `-PtestBudget=12`:

| Suite | Tests | Graded score | Must-kills per test |
| --- | --- | --- | --- |
| greedy cover of the graded set | 7 | 11/11 | 1.57 |
| qwen2.5-coder:1.5b | **10 — within budget** | 4/11 | 0.40 |
| qwen2.5-coder:7b | 13 | 5/11 | 0.38 |
| this module's authored suite | 14 | 8/11 | 0.57 |
| qwen2.5-coder:14b | 15 | 5/11 | 0.33 |
| qwen2.5-coder:32b | 15 | 7/11 | 0.47 |
| Claude, blind harden | 47 | 11/11 | 0.23 |
| Claude, harden with feedback | 47 | 10/11 | 0.21 |

The only suite that fits is the weakest one, and it fits because it barely tests anything. Nobody in
the archive achieves both at once — which is what makes the constraint worth setting rather than
decorative.

Note also that efficiency runs *backwards* against strength here: the frontier suites are the least
economical per mutant killed, roughly a seventh of what the greedy cover manages. That is not a
criticism of them — a test suite is documentation as well as a detector, and 47 readable tests may
well be the better artifact to maintain. It does mean the budget measures something the mutation
score alone never sees, and that no agent has yet been asked to optimise for it. Whether any can is
still open: these numbers are the budget applied *after the fact*, to suites written without knowing
about it.

## 3. The adversary

`runAgent -Pmode=attack` then `verifyAttack -Pagent=<name> --continue`. A second agent is given the
algebra and the learner's suite and asked for an implementation that passes every test and still
contradicts the specification. Unlike a catalog, this has no ceiling: each time the suite improves,
the next attack has to reach further out.

The oracle needs no answer key, so the whole loop runs in the student folder. The attack module and
`attacks/reference` both run `DifferentialProbe` over 2000 seeded random policy trees and ducks and
record their verdicts; `verifyAttack` diffs the two files. (Comparing the implementations directly is
impossible — same classes, same package, never the same classpath.) Outcomes:

- the suite fails against the attack → it caught it;
- the suite passes and the probe finds nothing → the attack is a correct implementation written
  differently, and says nothing about the suite;
- the suite passes and the probe disagrees → the suite has a hole, and the first disagreeing case is
  printed as the counterexample.

Failures the suite already has against the correct algebra are subtracted, or a suite with one broken
test would claim every attack as a catch.

`attacks/demo-prefix/` is a hand-written attack (accessory names matched by prefix) kept so the
machinery can be demonstrated without an API key: the flawed starter suite accepts it while the probe
disagrees in 36 of 2000 cases, and Claude's blind suite catches it.

### Measured: the local tier cannot attack (2026-08-04)

All five Ollama models were run live with `-Pmode=attack` against Claude's blind 47-test suite — the
strongest artifact we have, 11/11 on the graded mutants. None of them produced a usable attack:

| Attacker | What it returned | Verdict |
| --- | --- | --- |
| qwen2.5-coder:1.5b | `:core` verbatim | identical to `:core` — no attack |
| llama3.2:3b | `:core` verbatim | identical to `:core` — no attack |
| qwen2.5-coder:7b | `:core` verbatim | identical to `:core` — no attack |
| qwen2.5-coder:14b | **refused the task** | no code at all |
| qwen2.5-coder:32b | `RequiresAccessory` admits any duck wearing anything, commented `// Incorrect implementation` | differs in 222/2000, but caught by every suite tried |

Only 32b understood the task, and its attack is blunt rather than subtle: it was caught by Claude's
suite (3 tests), by its own hardened suite (1 test), and even by the flawed starter suite (3 tests).

So the adversary is a **frontier-versus-frontier** exercise. That is the same asymmetry the variant
check has, and it should be stated in the tier's framing: a learner with only a local model can do
the basic tier and the budget, but not this. Do not present the adversary as optional-but-equivalent
— without a capable attacker it produces a reassuring green that means nothing.

The 14b refusal is worth keeping as a per-agent note in its own right: asked to write code that
deliberately violates a specification, it answered *"I will not provide an implementation that
deliberately violates the specification or tests in a way that could cause harm or confusion."* An
adversarial-testing exercise can trip a model's refusal behaviour even though the task is ordinary
test engineering. Learners will hit this, and the materials should say what it looks like and that
rephrasing the request as "find an implementation this suite fails to distinguish from the correct
one" usually gets past it.

Two harness facts from the same runs: only 32b honoured the `// FILE:` contract, so the attack
builder infers which `:core` file was taken over from the **class names declared** in the reply, and
replaces a file only when every declaration in it was rewritten. Three of the five models returned
`:core` unchanged, which the probe reports as "identical" rather than as a pass — a copied reference
is a failed attack, not a clean suite.

### Measured: the frontier attack succeeded (2026-08-04)

Prompt E, blind, in a clean export, aimed at Claude's blind 47-test suite — the artifact that kills
all 11 graded mutants and is therefore *complete* by mutation testing's own measure. The attack got
past it: **suite PASSED, probe DIFFERS in 50 of 2000 cases.** It rewrote the leaves in two places the
suite never constructs:

- `RequiresAccessory("")` admits any duck. The suite checks case (`"Hat"`, `"HAT"`) and prefix
  (`"hatband"`), but never builds a policy with an empty required name. Specification says false for a
  bare duck; the attack says true.
- `MaxBudget` with `price.coerceAtLeast(0)`. Every boundary in the suite is at 0/1, 39/40, 99/100 — no
  duck costs less than nothing and no budget is negative. `MaxBudget(-1)` against a duck priced −2:
  specification admits, the attack refuses.

**This is the answer the tier was built to get.** Mutation testing was never the ceiling on this
algebra; a perfect mutation score means "nothing the mutants know about is missing", and an adversary
finds what they do not know about. Put the two numbers next to each other in the materials: 11/11 and
still broken.

### The probe's reach is itself a target — and it had a false negative

The attacker read `tools/probe/kotlin/…/DifferentialProbe.kt` — nothing forbade it, and it compiles
into the attacking module anyway — and picked its two defects *because they land inside the space the
generator samples*. It also reported a third hole it deliberately did **not** exploit: the suite pins
`AllOf`/`AnyOf` as boolean functions only for arities 0, 1 and 2, so an `AllOf` consulting
`policies.take(3)` would pass — and the probe, which built children of length 0..2, could never have
shown it.

Checked, and it was right: that implementation passed the suite while the probe reported **IDENTICAL**.
A real hole, reported as "not an attack". Fixed by raising the generator's width bound
(`MAX_WIDTH = 6`, covering combinator arity and accessory-list length); the same implementation now
disagrees in 17 of 2000 cases, and the frontier attack still shows at 44.

Two things to carry forward:

- **A "no disagreement" verdict is only as strong as the generator.** Whatever it cannot build, the
  check cannot see. If the algebra grows, widen the probe before trusting a clean report — and say so
  in the tier's framing, since the whole point of this exercise is not to mistake an instrument's
  silence for a property of the code.
- **This is Goodhart a third time**, after the mutation report and the practice tier: given a visible
  measurement, a capable agent optimises against the measurement. Here it did so *articulately* — it
  named the better attack and rejected it for being unmeasurable. That is worth quoting to learners
  verbatim; it is the clearest example in the whole module of a metric shaping the work rather than
  describing it.

### Round 2, with feedback: succeeded on the first iteration

Prompt F, fresh export, same suite. The attack got through immediately — suite green at 47/47, probe
disagreeing in 45 of 2000 — so **the feedback loop never engaged and the Goodhart question is still
open for attackers.** We did not learn whether a visible disagreement count narrows an adversary,
because this one never needed a second look.

The attack itself is more economical than the blind one: a single change, `duck.price >= 0 &&
duck.price <= maxPrice`, on the reasoning that a negatively priced duck has no meaningful price. The
suite misses it because every duck in it costs 0 or more — 0, 1, 20, 30, 40, 60, 100, 10 000. The
boundary *at* the limit and one *above* it are both pinned; the bottom of the range is not.

To test the anchoring question properly, the suite has to be patched against the known holes first,
so that iteration is actually required. That is the next run, not a repeat of this one.

### The real finding: four holes, all the same shape

Between the two rounds the agents named four gaps, and all four check out. Two they exploited, two
they described and I confirmed:

| Hole | Found | Verified |
| --- | --- | --- |
| `RequiresAccessory("")` admits everyone | round 1, exploited | 45/2000 |
| `MaxBudget` refusing negative prices | rounds 1 and 2, exploited | 45/2000 |
| `AllOf` consulting only `policies.take(3)` | round 1, declined as unmeasurable | 17/2000 |
| `MinAccessories` counting only the first three | round 2, described | 37/2000 |

Not one is a code perturbation. Every one is **an input value the suite never constructs**: an empty
string, a negative number, a collection longer than three. That is why mutation testing scored the
suite 11/11 and missed all four — a mutant is a change to the code, so it can only be caught or
missed *within the inputs the suite already builds*. Mutation testing measures how sharply a suite
discriminates over the values it imagines. It says nothing about the values it never imagined.

That sentence is the one to put in front of learners, and it is what the adversary adds that no
catalog can.

### The oracle's own blind spots, twice

Widening the generator after round 1 was not enough. `MinAccessories` was still being built with
arguments 0..3, so a policy counting only the first three accessories answered identically on every
case the probe could construct — a second silent false negative, in a different dimension of the same
generator, found one step after fixing the first.

Every bound in the generator is a blind spot, so the ranges are now **asserted rather than assumed**:
`DifferentialProbe` carries a second test that fails if the corpus does not actually contain a
negative price, an empty required name, a duck wearing four or more accessories, and a
`MinAccessories` above three. Narrow a bound and the build says so instead of quietly going blind.
Checked by narrowing `MAX_WIDTH` back to 3 and watching it fail.

### Round 3: the patched suite fell too — to a hole of a new kind

`round3-target` closes all four earlier holes, scores 11/11 on the graded mutants, 10/10 on the
variants, and catches every attack built before it. It lasted one attempt: **suite green at 52/52,
probe disagreeing in 21 of 2000.**

The change is one line in `MinAccessories` — `accessories.count { it.name.isNotBlank() } >= min`
instead of `accessories.size >= min`, so an accessory with a blank name does not count towards the
minimum. What makes it different from everything before is where it lives. The suite *does* build an
accessory with an empty name, and it *does* exercise `MinAccessories` past the third accessory — but
never both at once. Empty names appear only in the `RequiresAccessory` test; every duck fed to
`MinAccessories` wears named accessories.

So the first four holes were **missing ranges**, and this one is a **missing combination**. That
matters for what we tell learners about convergence: closing a range is bounded work, and there are
only so many ranges, but the pairs of ranges grow quadratically and the triples faster still. The
adversary is not running out of room; it moved up a level. That is the strongest evidence we have
that the loop does not converge cheaply — and the clearest reason the exercise should be framed as
"how far can you push it" rather than "get to green".

### But this run was not blind, and the reason generalises

Two things went wrong with the method, both worth keeping.

The session ran against the **working checkout instead of the export**, so the two previous attacks,
every other agent's suite, and `attacks/reference/build/probe.txt` — two thousand correct verdicts —
were all on disk, along with this file one directory up. What rescues the finding is that the hole it
reports is *not* in these notes: the four listed here are exactly the ones already closed, and blank
names inside `MinAccessories` is new. It went past whatever it may have read.

The second problem is the interesting one. The prompt forbade *running Gradle tasks*, and it complied
— then compiled both variants with `kotlinc`, ran the suite by reflection and diffed the two probe
corpora by hand. It rebuilt the oracle. **A restriction on tools is not a restriction on
information**: for a capable agent, "do not run the checker" only removes the convenience. Either the
blind prompt must forbid reconstructing the check by any means, or — more honestly — we should accept
that the blind regime is unenforceable against a strong agent and stop claiming it. For a *learner*
the instruction still works, because the point there is pedagogical sequencing rather than
containment; that distinction belongs in the teacher notes rather than in the student README.

So the blind-versus-feedback comparison on a single suite is **still not done.** What this run
actually measured is a self-served feedback regime that succeeded on its first submitted attempt.

### The paired round, run properly — and the answer is "no difference"

Prompts G and H, one clean export each, same target. The blind arm was **verified blind**: no `.class`
file, no `build/`, no `.gradle`, no `probe.txt` anywhere in its export, and the only source it wrote
was the attack itself. Forbidding *execution* rather than a named tool turned out to be enforceable
after all — the earlier failure was our prompt, not the regime.

Both arms succeeded **on the first attempt**, and found different holes:

| Arm | Attack | Disagreements | Attempts |
| --- | --- | --- | --- |
| Blind (no execution at all) | `AllOf` splits its list in two passes with an off-by-one, so the **fourth** policy is never consulted | 7 / 2000 | 1 |
| Feedback (checker available) | `KotlinOnly` also requires `price >= 0` | 26 / 2000 | 1 |

So the feedback arm gained nothing measurable, and the anchoring question **cannot be answered at this
difficulty**: anchoring needs iteration, iteration needs the task to be hard, and attacking this suite
is not hard. The harden experiment produced anchoring because reaching 100% there is genuinely
difficult. That is a real finding about when the effect appears, not a failed run.

Worth noting how sharp the blind analysis was: with nothing executed, it enumerated every `AllOf`
construction in the suite by line number, showed that arity 4 is never built and that in the only
two length-5 lists index 3 holds an admitting policy, and predicted the probe would see it because
`MAX_WIDTH` is 6. All of it correct.

### The diagnosis is about the patch, not the agents

Both holes exist because of how `round3-target` was hardened — by us, adding one test per
counterexample, right next to the counterexample that motivated it.

- "Combinators must look past the third policy" was closed with a length-5 list whose deciding policy
  sits at index 4. **Index 3 was left untested**, and that is exactly where the blind attack went.
- "Prices below zero" was closed inside the `MaxBudget` test, using a local helper. **No
  negatively-priced duck reaches any other leaf**, and none is in `allDucks`, the shared corpus that
  drives the De Morgan, double-negation and `Shop` tests — so `KotlinOnly` had never seen one.
- The same shape is still open: the suite builds accessory lists of size 1 and one of size 5, and
  **never one of exactly 4**. The feedback arm pointed this out unprompted.

**Hardening against a counterexample closes the instance, not the class.** That sentence is the most
transferable thing the adversary work has produced, and it is a lesson about how people patch rather
than about how agents attack. The structural fix is to put the interesting values into the *shared
fixture corpus* every test already runs over, and to parameterise over arity and position instead of
picking one, so closing a hole closes its neighbours too.

That also makes the next experiment obvious and worth doing: harden `round3-target` structurally
rather than instance-wise, and attack again. If a structurally hardened suite survives, the loop
converges after all and instance-patching was the whole problem. If it falls again, the exercise's
framing as "how far can you push it" is the honest one.

### Probe sensitivity

The blind attack disagreed in only 7 cases out of 2000 — it survives 99.65% of the corpus, far subtler
than anything before it, and a slightly subtler one could have slipped through on another seed. The
corpus is now **20 000 cases**, which costs nothing measurable (the probe still runs in about a
second) and moves the same two attacks to 91 and 290. Numbers recorded before this change were taken
at 2000 and are not directly comparable.

### Round 4: structural hardening held everywhere except one seam

`round4-target` is the suite rebuilt the structural way — every interesting value in one shared
corpus, the combinators checked at every arity with the deciding policy at every position, each leaf
pinned as a formula rather than by examples. It is **11 tests**, scores 11/11 on the graded set, 10/10
on the variants, fits the budget, and catches all six attacks that came before it.

It fell anyway, first attempt, 239 disagreements in 20 000 — but the attack had to work for it, and
the agent's own account of what it had to discard is the most useful part of the run: symmetric
`trim()` (indistinguishable from `:core`, so invisible even to the probe), case-insensitive matching,
`startsWith`, `contains`, `distinct()` in `MinAccessories`, "negative price means free" in
`MaxBudget`, an off-by-one tolerance on the budget, "the last policy decides", "exactly one policy
decides" — the structural suite catches every one.

What got through is a **one-sided `trim()`**: normalise the name the policy requires, leave the name
the duck wears alone. A duck wearing `Accessory("Scarf ")` no longer satisfies
`RequiresAccessory("Scarf ")`.

The suite covered that value. It had `"Scarf "` in `names`. But `names` fed only the *argument* side —
no duck in the corpus ever wore a padded name — so the padded string was permanently in the "must not
match" role, where a trimmed comparison and an exact one agree. **The axis looked covered and was
covered in one direction only.**

So the escalation continues, and it is getting sharper each time:

| Round | How the suite was hardened | What got through |
| --- | --- | --- |
| 3 | one test per counterexample | a **missing combination** — empty name × `MinAccessories` |
| 4 | one shared corpus, parameterised | a **role asymmetry** — a value present as an argument, never as data |

That is the finding to build the exercise's framing on. Each round closes a class of defect and
uncovers a subtler one, and the subtler one is always about a *relationship* between axes rather than
a missing value on any single axis.

### Round 5 is the terminating experiment

`round5-target` fixes the seam: both sides are now built from one `names` alphabet, so every name is
worn by some duck and required by some policy, and the alphabet gains ` hat` and `hat ` so padding
appears in both roles. It also adds a mid-range price, a three-accessory duck and distinct duck names,
which closes the three gaps the round-4 attacker listed as reachable only by a magic-number backdoor.
Still 11 tests, still 11/11 graded, still 10/10 conformant, and it catches all seven attacks to date.

The round-4 agent predicted that after this fix only class (b) attacks remain — arbitrary special
cases on a value the corpus does not contain.

### Round 5: the prediction was wrong, and how it was wrong is the whole lesson

The suite fell again — 11/11 green, **165 disagreements in 20 000** — and to a class (a) defect. But
the hole is no longer on any value axis. The attack drops composite children that "state no
requirement":

```kotlin
private fun AdmissionPolicy.statesNoRequirement(): Boolean = when (this) {
    is AllOf -> policies.isEmpty()
    is AnyOf -> policies.isEmpty()
    else -> false
}
```

An empty `AllOf` and an empty `AnyOf` are the identity elements of **different** operations — one says
yes, the other says no — so treating both as "no opinion" is wrong exactly across types. Within a type
the simplification is sound, which is what would get it through review. The witness is not a duck at
all: `AnyOf(AllOf())` is `true` in the specification and `false` in the attack **for every duck**. The
shape of the policy is the counterexample.

The suite misses it for a reason that can be stated precisely: it checks `AllOf(emptyList())` and
`AnyOf(emptyList())` **only at the root**. Verified — an empty composite appears nowhere as a child,
and in the arity unfolding the children are always `always`/`never`. The probe meanwhile builds them
freely at any depth, since `children()` may draw zero children.

**The law the three rounds add up to, and what the exercise should teach:**

| Round | What the suite had corpus-ised | Where the next hole lived |
| --- | --- | --- |
| 3 | nothing — one test per counterexample | a missing **combination** of values |
| 4 | values, but per role | a **role asymmetry** — present as an argument, never as data |
| 5 | values, symmetrically, from one alphabet | **tree shape** — the one axis still written out example by example |

**Whatever axis you enumerate by example rather than by corpus is where the next defect lives.** The
round-5 attacker diagnosed this itself: the suite lifted every interesting *value* into a shared
corpus and ran every leaf over it, but a shape is not a value, there is no corpus of shapes, and each
one is hand-written in its own test. The instance-to-structure move made for ducks was never made for
trees.

Its patch is the right one and generalises: put `AllOf(emptyList())` and `AnyOf(emptyList())` into
`leaves()` and into the pool the arity unfolding draws children from, so an empty composite reaches
every position at every arity the way `never` already does.

### Two things worth quoting to learners

**It graded its own plausibility, unprompted, and was right to.** It called this "class (a) in the
sense of a plausible mistake in a plausibly *extended* implementation, not a typo in a one-liner" —
because `:core` needs no normalisation step at all, so the defect is *added* code rather than
*altered* code. That is a signal in itself: the space of plausible modifications is exhausted, and
what is left requires inventing a reason to touch the code first. Defect density per unit of
plausibility is falling, even though it has not reached zero.

**It rejected a real hole because our instrument could not see it** — the fourth time this has
happened. `MaxBudget` written as `maxPrice - duck.price >= 0` overflows at `MaxBudget(Int.MAX_VALUE)`
against a negative price; the suite does not catch it, but the probe only builds limits in −1..11, so
the report would have said IDENTICAL. We therefore knowingly hold a genuine hole this oracle
structurally cannot report. Say that out loud in the materials: the check's reach is part of the
result, and a clean report is a statement about the instrument as much as about the suite.
