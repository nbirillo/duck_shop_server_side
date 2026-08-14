# 11.4 — the property catalog and the key

Teacher-only. Two artifacts are described here, and they are easy to confuse:

- **the property catalog** — `pricing-properties/.../PricingProperties.kt`, one test per claim a
  specification might make. It runs against the reference and against every mutant. This is the
  load-bearing layer: it is code, it is deterministic, and no model touches it.
- **the key** — `fixtures/11.4/key.json`, a hand-written record of which claims each fixture
  specification *actually makes*. It measures the **claim extractor**, and nothing else.

The key is not a rubric. It is never shown to a learner as one, and no mark is computed from it.

## Why the key exists

The extractor asks a model, for each property in the catalog, whether a given specification asserts
that claim. Five identical runs of it agreed verbatim. That is reproducibility, and it is what you
would expect at temperature 0 — it says nothing about whether the answers are right.

They are not always right. On the strongest specification in the corpus the extractor answered NO to
"no rule ever raises the price", a claim that spec makes twice (`0 <= priceFor(duck, rules) <=
duck.price`, and again as monotonicity). Without a key that miss is indistinguishable from a genuine
gap, and the tool reports a fault in work that has none.

Current measurement against the shipped catalog: see `./gradlew scoreExtraction -Pagent=<run>`.

## Reading the key

Three verdicts per claim, because silence and error are different facts:

| verdict | meaning | what it costs downstream |
| --- | --- | --- |
| `yes` | the text asserts it | — |
| `no` | the text is silent | an implementer is left free; usually fine, sometimes a gap |
| `contra` | the text asserts something incompatible | an implementer is sent the wrong way |

`outOfScope` lists claims the fixture's own brief never showed its author — a basic-tier spec is
silent about `BestOf` because the basic brief has no `BestOf`. That is correct work. The extractor
should still answer NO; the distinction matters only to a person reading the report.

Note what the third verdict exposes: the extractor cannot express "the specification says the
opposite". It answers a yes/no question, so on a `contra` cell it says NO, which is *right about the
absence and silent about the more serious fault*. `scoreExtraction` counts those as agreement and
lists them separately. A reader catches what the extractor structurally cannot.

## The appeal — `verifyClaims`

A learner who thinks the extractor misread them can declare their claims directly:

```
./gradlew verifyClaims -Pclaims=1,3,4,6
./gradlew verifyClaims -Pspec=<SPEC.md> -Pagent=<run>   # reads claims.txt beside the spec
```

Numbers, full claim names, or any unambiguous fragment. Everything after the declaration is
deterministic: those properties run against the reference and the mutants, and the report says which
mutants each claim kills. No model, and no key needed — which is why this path stays trustworthy on
a catalog of your own.

What to look at in the output is the **disagreement**, not the score:

- **CONTESTED** — declared, not extracted. Ask them to show you the sentence. Either the checker
  misread it, or the claim lives in the learner's head and not in the text. Both are worth the
  conversation, and the report deliberately does not decide which it is.
- **NOT DECLARED** — extracted, not declared. They wrote something down that pins an implementation
  and did not realise they had. Usually the more instructive of the two.

Note the honest failure mode this preserves: a learner can win the appeal and still lose the point.
On our best fixture, claim 2 is genuinely stated, the extractor genuinely missed it — and the
property kills no mutant, so the claim they were defending constrains nobody. The report says both
things in the same run.

## Handing out the corpus — rename the files

The repository is public, deliberately. Someone studying alone may read the grading build; the
exercise then rests on their honesty, which is their business. A teacher running the course downloads
everything and hands the group only what the step needs.

**When you hand out `fixtures/11.4/`, rename the files.** They are named by the model that wrote them,
and the slides promise the learner an anonymous set — a name on the file does the reading for them, and
in the compression step it makes everyone defer to whichever one they believe is best. Use `A.md`,
`B.md`, `C.md`, in an order you shuffle yourself:

```
mkdir -p handout
cp fixtures/11.4/written/claude-code.md                 handout/A.md
cp fixtures/11.4/written/ollama-qwen2.5-coder-1.5b.md   handout/B.md
cp fixtures/11.4/written/ollama-qwen2.5-coder-32b.md    handout/C.md
```

Those three are the set we used for the compression run, so the results in
`exercise-11.4-corpus-results.md` line up with them. Any three work; keep one long and two short, or
there is nothing to merge.

Keep your own mapping. You will want it when a learner asks which was which — after they have finished,
not before.

## Handing the learner the tool and the list, if you want to

The default is that the claim list and the property run stay on your side: the learner writes the
specification, gets the list back with their feedback, and argues with it in writing. Slide 39 has
them grade their own list by hand first, and that step needs nothing from you.

You can go further and give them the catalog and `verifyClaims` outright. It is one copy and one
gradle property — the task is already in the shared plugin, and the student build only lacks
`implTests`. Before you do, the trade, because it is not small:

**What they gain.** A tight loop. Write a claim, run it, see whether it holds against correct code and
whether it rules anything out — without waiting for you. For a strong group, or a second pass through
the exercise, that is worth a lot.

**What it costs.** The list becomes a **visible target**, and a visible target is one people write to
fit. We have measured that in this very module more than once, on ourselves: the moment a number was
in view, effort reorganised around it. A learner holding the catalog will produce a specification that
asserts those ten claims — which is a different and much easier task than deciding what the feature
should do. It also spoils the two comparisons the exercise ends with, since their text was written
against the same list it is scored on.

**A middle setting that keeps most of both.** Give them the tool and the list **after** their first
specification is handed in — as the second attempt. The first pass measures what they decided
unprompted; the second teaches the loop. The order matters more than the access.

**What must not travel either way.** The reference implementation and the mutant catalog. Those are
answers, not targets: with the mutants in hand there is nothing left to discover, and with the
reference the exercise is "guess what we wrote".

## Making your own property catalog

Supported, and it is the honest way to use this material — our catalog is a default, not a
definition of what a good specification says.

1. Write your own `PricingProperties.kt`, or generate one with a model of your choosing and read
   every line of it. Two rules decide whether a property earns its place:
   - a property that **fails on the reference** is simply wrong;
   - a property that **kills no mutant** pins nothing, however true it is.
   `./gradlew pricingPropertyMatrix` reports the second. Two of ours kill nothing and are kept
   deliberately, as the live example of that distinction — do not read the matrix as a to-do list.
2. Test names are the claim ids. The extractor reads them straight out of the file, so they cannot
   drift; renaming a test renumbers everything downstream, including the key.

## Making your own key — and the trade if you do not

**A key is valid only for the catalog it was written against.** Change the properties and
`key.json` describes claims that no longer exist. Ours is renumbered by hand whenever the catalog
changes, and `scoreExtraction` fails loudly on a length mismatch rather than scoring the wrong cells.

So, plainly:

- **With the shipped catalog**, the extractor's accuracy is measured and you can quote it.
- **With your own catalog**, it is unmeasured. The layer still runs and still produces a report;
  you just have no idea how often that report is wrong.

Nothing breaks either way, because no grade rests on the extractor — layer 3 does the load-bearing
work, and layer 3 needs no key. Treat an unmeasured extractor as advisory and say so to the group.

If you want the measurement back, author a key over a handful of specifications:

1. Take 5–6 specs of varying quality. Ours are in `fixtures/11.4/` and you may reuse them.
2. Read each one against each of your claims and record `yes` / `no` / `contra`, with a pointer to
   where in the text — the `where` field. The pointer is what makes the key auditable later, and it
   is what catches you when you disagree with your past self.
3. Run `scoreExtraction` and read the misses. A miss is your tooling misreading a correct spec.

Budget honestly: it is one careful reading per specification, and it does not parallelise. This is
the price of knowing whether your checker works, and it is why the default catalog ships with one
already written.

## Revealing the reference

`reference/.../Pricing.kt` carries all seven decisions in its KDoc and is hidden by default. Show it
if you want to — after the specs are written, never before. The moment it is visible the exercise
stops being "decide what the business meant" and becomes "guess what we wrote".
