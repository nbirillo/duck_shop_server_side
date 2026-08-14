# Running a frontier agent as the *implementer* (11.4)

Teacher-only.

## Why this run exists

The corpus has been implemented once by `qwen2.5-coder:32b`, which measures the specifications
*through one mid-size reader*. A stronger reader answers a different question: does the spread survive,
or does a capable agent fill every gap with the default the reference chose and make a thin text look
complete? Measured answer so far: **it does not survive** — one 397-word specification went from 7/8
to 8/8 and became byte-identical to the reference on all 4021 probed inputs. That single fact is why
the exercise measures disagreement between two readers rather than agreement with one.

## The route: the same one a learner uses

An earlier version of this checklist had you paste a hand-assembled prompt into a chat window with no
file access. **Do not do that.** It measures a situation that never occurs — a learner opens an agent
inside their project and says "implement this from my spec" — so the sterile version answers a question
nobody asked. Use the sandbox, exactly as the slides tell a learner to.

```
cd spec-test-driven
./gradlew sandbox -Pname=frontier-a -Pspec=<the specification>
```

Then open the agent **in that folder** — `cd sandbox/frontier-a && claude` — and ask it, in ordinary
words, to implement `priceFor` in `Pricing.kt` from `SPEC.md`.

Three things while it works:

- **Do not answer its questions about the specification.** Tell it to decide and continue. The
  question is itself a finding about the text, and answering it puts you in the loop.
- **Do not paste the rules in.** The business text now lives in `briefs/` in this build and is not in
  the student folder at all, so an agent working in `spec-test-driven` cannot reach it — the leak that
  used to matter most is closed by layout rather than by discipline. Keep it that way.
- If it edits your properties instead of satisfying them, `sandbox` will tell you on the next run.
  That is a finding, not a nuisance.

Collect and score:

```
./gradlew prepareImplementation -Pagent=frontier-a \
    -Pspec=<the specification> -Pfrom=sandbox/frontier-a
```

`-Pspec` must be the same in both commands — it is how the result is filed. Then, from this build:

```
./gradlew verifyImplementations -Pagent=frontier-a --continue
./gradlew verifyDivergence -PagentA=frontier-a -PagentB=impl32b
```

`--continue` matters: an implementation that does not compile is a result, and three of `impl14b`'s
thirteen do not.

## A local model needs no sandbox

For anything driven by an API, one command replaces the whole sequence — it writes the implementation
module directly:

```
./gradlew runAgent -Pmode=impl-from-spec -Pagent=reader-b \
    -Pprovider=ollama -Pmodel=qwen2.5-coder:32b -Pspec=<the specification>
```

Works from either build. The sandbox exists for *interactive* agents, which need somewhere to work.

## Reading the result

Out-of-tier properties (`BestOf`, `OnlyIf` for a basic-tier specification) are excluded automatically —
a basic brief never showed them.

A divergence is **not** a defect until read. It means the agent and our reference disagree, which
happens when a specification failed to say something *and equally* when it said something different on
purpose and the agent obeyed. Both occurred:

- `qwen1.5b`'s text claims overflow safety and the key counts the claim, but its only example is
  `AmountOff(1)` at `Int.MAX_VALUE`, which never multiplies — and the implementation wrote
  `price * percent` in `Int`. A claim can be present, counted, and still not transmit.
- `qwen7b`'s text says the bonus reads the *shelf* price, and the agent used the running price anyway.

## What to compare against

Basic tier, out-of-tier excluded, `qwen2.5-coder:32b` as the implementer:

```
claude 8/8 · qwen1.5b 7/8 · llama3b 6/8 · qwen32b 6/8 · qwen7b 6/8 · qwen14b 5/8
```

And keep the other axis beside it — the defects each specification's claims catch:

```
claude 6/6 · qwen32b 4/6 · llama3b 3/6 · qwen7b 3/6 · qwen1.5b 2/6 · qwen14b 2/6
```

They disagree on purpose. Catching defects measures how much a specification **pins**, where silence
is expensive; implementing measures how much it gets **wrong**, where silence is free because a capable
agent fills the gap sensibly. `qwen1.5b` is second on one axis and last-equal on the other: faithful by
vacuity. Neither number describes a specification alone — see `exercise-11.4-corpus-results.md`.
