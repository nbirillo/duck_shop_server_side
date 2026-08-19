# The 11.6 debrief

Teacher-only. Run this **after** they have handed in — everything here is comparison and reveal material,
and the general rule the whole module is built on is that comparison comes after the attempt.

There are **no slides for this**, on purpose ([[capstone-11.6.md]] explains why). It is a conversation you
run from these notes, so you can spend it on what your group actually did.

## The one decision that is yours

**How much of the staging you admit is at each teacher's discretion** — her call, 2026-08-19, and it is not
prescribed here because the right answer depends on the group.

What is true: **two of the four inherited artifacts were written by us** — the test suite, and the
implementation, which was degraded by hand from a correct one. The specification was not: it is six real
specifications merged by an agent. Full audit trail, including which sentence licenses which defect, is in
`capstone-11.6.md`.

- **Saying it** turns the exercise into a worked example of the module's own method: we calibrated, the
  profile did not arise on its own, so we staged it and wrote down exactly what we staged. Groups that
  will go on to build teaching material of their own get more from this than from the exercise.
- **Not saying it** keeps the artifacts feeling like a real inheritance, which is worth more with groups
  who are here to practise rather than to design curricula.
- **What not to do either way:** claim the implementation is untouched agent output. It is not, and the
  teacher pack says so.

## The five beats

**1. Who noticed the specification was stacked?**
It is six independent specifications, merged and smoothed — nobody ever wrote it as one document, which is
why it reads the way it does. 165 lines against 31–35 for each source. Ask who split it back up, and who
tried to fix it in place.

**2. It contradicts itself, and here is where.**
The table in `capstone-11.6.md` lists them: the tie-break is fixed in one section and called unspecified in
two others · the chain's rule is a filter in §2 and "left open" in §4 · §4 asserts a promotion-ordering
rule that is not a reading of the brief at all, merely false · two appended bullets contradict what 11.4
settled about compounding. Ask which ones they found, and what they did with them — a contradiction is not
a gap, and resolving it is a decision they now own.

**3. The implementation's defects were all licensed by that text.**
Every one of them can be quoted back out of the specification they were handed: the chain's own rule never
consulted, the chain's promotions applied first, a tie going to the last shop, a duck priced zero refused.
**That is the point** — not that somebody was careless, but that a document saying several things produces
code that picks some of them. Whether you also say the picking was ours is the decision above.

**4. Everything they inherited passed the settled facts.**
The floor — the facts the brief really does settle — is **6/6 green** on the inherited implementation, whose
answers differ from ours on **976 of 3010** probed inputs (32.4%). *Provenance: our own corner-case suite
and the franchise probe, 3010 fixed cases, against the reference reading.* And it is not special to the
staged artifact: an earlier specification in our corpus disagreed with the reference on **884 of 3010**
inputs and also passed every settled fact.
**This is the module's thesis arriving:** the machine-checkable part was fully satisfied by a bad artifact.
What was left was reading, deciding, and writing it down — which is what stayed human all along.

**5. Whichever forks they left open, they are in good company — and that is not the same as being right.**
Two independently written weak specifications, from different models, missed **exactly the same three**:
the chain's own rule, promotion order, and when "cheapest" is measured. Ties and empty chains converged
every time. *Provenance: six specifications of this brief, each implemented by qwen2.5-coder:32b at
temperature 0, compared on the same 3010 cases.*
Worth ending on: those three are not arbitrary. **They are where readers actually fail**, which is why the
graded set is built on them and not on a list of corner cases.

## If you want one artifact to show

`fixtures/11.6/inherited-correct-14b.kt.txt` — the **same specification**, implemented by the same model,
coming out byte-identical to the reference on all 3010 inputs. Put it beside the one they inherited. The
text did not change; what changed was which of its sentences got followed.

## Where the numbers come from

`[[module-11-agent-run-findings]]` in the assistant's memory holds every run and every measurement, and
`capstone-11.6.md` holds the design and the audit trail. Do not quote a number from here without the
provenance line that comes with it — the same rule the slides follow.
