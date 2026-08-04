# Running the frontier attack by hand

The local models cannot attack — three copy `:core`, one refuses, and 32b writes something every
suite catches (see [`advanced-tier-notes.md`](advanced-tier-notes.md)). So the adversary tier has
only ever been measured against a fixed hand-written attack. This is the procedure for the real
measurement, driven interactively because we have no Anthropic key.

**The question it answers:** a suite that kills every graded mutant — Claude's own blind 47-test suite,
11/11 — is by mutation testing's own measure complete. Can a frontier adversary still get past it? If
it can, mutation testing was never the ceiling and the adversary is the escalation the advanced tier
needs. If it cannot, after a real attempt, that is a result too: it says this algebra is small enough
to be pinned down completely, and the escalation has to come from depth (11.7) instead.

## 1. Set up a clean export

Measure in an export of tracked content, not in the working copy — the working copy holds other
agents' suites, generated solutions and previous attacks, and an attacker that reads them is not
measuring anything. This is the same method the blind mutation measurement used.

```bash
cd ~/IdeaProjects/duck_shop_server_side_slides/duck-shop
rm -rf ~/IdeaProjects/duck-shop-attack-run && mkdir -p ~/IdeaProjects/duck-shop-attack-run
git archive HEAD spec-test-driven | tar -x -C ~/IdeaProjects/duck-shop-attack-run

# the suite under attack is git-ignored, so copy it in explicitly
cp -R spec-test-driven/hardened/claude-code-blind \
      ~/IdeaProjects/duck-shop-attack-run/spec-test-driven/hardened/
```

The export has no grading build, so the graded mutant catalog is out of reach by construction. It
does contain `mutants/` (the five practice entries) and `attacks/demo-prefix/` — both are shipped
material a learner sees too, so leaving them in is right.

## 2. Round 1 — blind

Open **`~/IdeaProjects/duck-shop-attack-run/spec-test-driven`** as the project in a fresh Claude Code
session, so it cannot wander into the rest of the repository. Then give it prompt E, unchanged.

Blind means: no running the checker, no reading the mutant or variant catalogs. That is deliberate.
The feedback regime made the harden run score *worse* by reorganising its effort around a visible
target, and we want to know whether the same thing happens to an attacker before we let it see any
report.

### Prompt E — blind attack

> The admission-policy algebra in `core/` is the specification, and it is correct. The test suite in
> `hardened/claude-code-blind/src/test/kotlin` was written for it by another agent.
>
> Write an implementation of that algebra which passes every one of those tests and is still wrong —
> wrong meaning there is some duck, or some combination of policies, for which it answers differently
> from what the specification requires.
>
> Put your files in `attacks/claude-code/src/main/kotlin/org/jetbrains/kotlin/course/duck/shop/admission/`,
> keeping the exact package, class names and constructor signatures. Rewrite whole files: if you
> change one class in `Leaves.kt`, supply all four.
>
> Do not run any Gradle task, and do not open `mutants/`, `variants/` or `attacks/demo-prefix/`. Work
> it out by reading the specification and the suite.
>
> If you conclude no such implementation exists — that every implementation passing these tests is
> necessarily correct — say so instead of forcing it, and name the tests that close the door.
>
> When you are done, write two or three sentences about which behaviour you changed and which duck
> would expose it.

Then score it yourself:

```bash
cd ~/IdeaProjects/duck-shop-attack-run/spec-test-driven
./gradlew prepareAttack -Pagent=claude-code -PmutantTests=hardened/claude-code-blind/src/test/kotlin
./gradlew verifyAttack  -Pagent=claude-code -PmutantTests=hardened/claude-code-blind/src/test/kotlin --continue
```

`prepareAttack` renames anything colliding with a `:core` file name and works out the excludes from
the classes the sources declare, so the agent does not have to touch a build script.

Read the verdict as:

| Report | Meaning |
| --- | --- |
| suite FAILED — n tests caught it | the suite held; the attack failed |
| suite PASSED, probe IDENTICAL | not an attack at all — a correct implementation written differently |
| suite PASSED, probe DIFFERS | **the suite has a hole**, and the printed case is the counterexample |

Save the result: `cp -R attacks/claude-code ~/…/duck-shop/spec-test-driven/attacks/claude-code-blind`
(git-ignored, like the hardened suites).

## 3. Round 2 — with feedback

**Recreate the export from scratch — do not reuse round 1's.** Scoring round 1 ran the reference
module, so that export now holds `attacks/reference/build/probe.txt`: two thousand correct verdicts,
sitting in a file the agent can read. Handing an attacker the oracle's answers would end the
experiment before it starts. Recreating also picks up any change to the probe itself.

Fresh export, fresh session, same suite — same commands as step 1. This time the agent may iterate:

### Prompt F — attack with feedback

> Same task as before, but you may run the checker and iterate:
>
> ```
> ./gradlew prepareAttack -Pagent=claude-code -PmutantTests=hardened/claude-code-blind/src/test/kotlin
> ./gradlew verifyAttack  -Pagent=claude-code -PmutantTests=hardened/claude-code-blind/src/test/kotlin --continue
> ```
>
> Keep going until either the suite passes while the probe reports a disagreement — that is a
> successful attack — or you are convinced no such implementation exists. `mutants/` and `variants/`
> are still off limits.

What to compare afterwards: whether feedback helped or, as in the harden experiment, narrowed the
search. The probe reports the *number* of disagreeing cases, which is a target an agent can optimise
towards; a run that maximises that number rather than finding a subtle single case would be the same
Goodhart effect in a new place, and worth writing up.

## 4. Round 3 — against a suite that already knows

Rounds 1 and 2 both broke the suite on their first try, so neither told us anything about iteration.
Round 3 removes the easy answers first: `hardened/round3-target/` is the blind suite with five tests
added, closing all four holes the two rounds surfaced — the empty required name, prices below zero,
combinators past the third policy, and `MinAccessories` past the third accessory. It is 52 tests,
still 11/11 on the graded mutants, still 10/10 on the conformant variants, and it catches every
attack produced so far. Like the other hardened suites it is git-ignored, so it exists only in the
working checkout.

Now the outcome is genuinely open, and either answer is worth having: another hole means the loop has
not converged and the adversary keeps its teeth; nothing found, after a real attempt, means this
algebra can be pinned down completely and the escalation has to come from depth instead.

Run **both regimes on this same suite**, blind first, in separate exports and separate sessions —
that is the comparison rounds 1 and 2 could not give us.

```bash
cd ~/IdeaProjects/duck_shop_server_side_slides/duck-shop
rm -rf ~/IdeaProjects/duck-shop-attack-run && mkdir -p ~/IdeaProjects/duck-shop-attack-run
git archive HEAD spec-test-driven | tar -x -C ~/IdeaProjects/duck-shop-attack-run
cp -R spec-test-driven/hardened/round3-target \
      ~/IdeaProjects/duck-shop-attack-run/spec-test-driven/hardened/
```

Prompts E and F unchanged, with `hardened/claude-code-blind` replaced by `hardened/round3-target`
throughout. Score with:

```bash
./gradlew prepareAttack -Pagent=claude-code -PmutantTests=hardened/round3-target/src/test/kotlin
./gradlew verifyAttack  -Pagent=claude-code -PmutantTests=hardened/round3-target/src/test/kotlin --continue
```

What to compare afterwards: whether the feedback run needed more than one iteration, whether it found
the same hole as the blind one, and whether it started chasing the probe's disagreement count rather
than the subtlest case. That count is exactly the sort of number a capable agent optimises towards,
and it is the last untested corner of the Goodhart story.

## 5. What to record

In [`advanced-tier-notes.md`](advanced-tier-notes.md), next to the local-model table: which round, what
the attack changed, whether the suite caught it, how many of the 2000 probed cases disagreed, and the
counterexample. If the attack succeeded, add the case to the 11.2 answer key — it is a gap in a suite
we had been treating as complete, which is exactly the kind of thing the module is about.
