# Running the frontier as the *implementer* (11.4, step 3e)

Teacher-only. Everything here runs from `spec-test-driven-grading/`.

The API path (`runAgent -Pmode=impl-from-spec`) needs `ANTHROPIC_API_KEY`. Without one, drive the
frontier by hand in a second window — this is the checklist for that, and it exists so the two routes
send **exactly the same input**. Retyping the specification is how a brief or a reference leaks in,
and then the run measures the leak instead of the specification.

## Why this run is separate from the one already done

The corpus has been implemented once, by `qwen2.5-coder:32b`. That measures the specifications
*through one mid-size agent*. The open question is whether the spread survives a stronger
implementer: a frontier model may fill a gap with the same default our reference chose and make a
thin specification look complete. If the spread collapses, the exercise's signal comes largely from
the implementer, and that is worth knowing before it is taught.

## The one rule that matters

**The session must not be able to read this repository.**

Not "must not be asked to" — must not be *able* to. We have already been caught by the weaker
version of this: an agent told not to run Gradle rebuilt the oracle with `kotlinc` instead and did
exactly what the restriction was meant to prevent. A restriction on tools is not a restriction on
information. Use a plain chat window with no file access, no MCP filesystem server, and no working
directory inside the checkout.

If the model can reach `reference/.../Pricing.kt`, the run is worthless and will not look it.

## Steps

1. **Print the exact prompt.** The `-Pdry` run makes no API call and writes nothing:

   ```
   ./gradlew runAgent -Pmode=impl-from-spec -Pdry \
       -Pprovider=ollama -Pmodel=none -Pagent=claude \
       -Pspec=fixtures/11.4/written/claude-code.md \
       -PpromptDir=../spec-test-driven/tools
   ```

   For the advanced fixture add
   `-PimplSurface=../spec-test-driven/exercises/write-spec/README-advanced.md`, or the agent is shown
   three rule kinds for a specification written against six.

2. **Copy `===== SYSTEM =====` and `===== USER =====` into the session**, verbatim, nothing added.
   Do not answer questions it asks about the specification — if it asks, that is itself a finding
   about the specification. Tell it to decide and continue.

3. **Save the Kotlin block** it returns to

   ```
   implementations/claude/<written-claude-code>/src/main/kotlin/org/jetbrains/kotlin/course/duck/shop/pricing/Pricing.kt
   ```

   The folder name is the fixture's directory and file name joined with `-`.

4. **Scaffold and score:**

   ```
   ./gradlew prepareImplementation -Pagent=claude -Pspec=fixtures/11.4/written/claude-code.md
   ./gradlew verifyImplementations -Pagent=claude
   ```

   `prepareImplementation` with no source file in place prints step 1 for you and stops.

## Reading the result

Out-of-tier properties (`#9 BestOf`, `#10 OnlyIf` for a basic-tier specification) are excluded
automatically — a basic brief never showed them.

A divergence is **not** a defect until read. It means the agent and our reference disagree, which
happens when a specification failed to say something *and equally* when it said something different
on purpose and the agent obeyed. Both were observed in the 32b run:

- `qwen1.5b`'s text claims overflow safety, the key counts the claim, and the implementation still
  wrote `price * percent` in `Int` — its only example was `AmountOff(1)`, which never multiplies. A
  claim can be present, counted, and still not transmit.
- `qwen7b`'s text says the bonus reads the *shelf* price, and the agent used the running price
  anyway. Being wrong in writing does not guarantee being obeyed.

## What to compare against

The 32b implementer's basic tier, out-of-tier excluded:

```
claude 8/8 · qwen1.5b 7/8 · llama3b 6/8 · qwen32b 6/8 · qwen7b 6/8 · qwen14b 5/8
```

And keep the other axis beside it — the mutants each specification's claims kill:

```
claude 6 · qwen32b 4 · llama3b 3 · qwen7b 3 · qwen1.5b 2 · qwen14b 2
```

They disagree on purpose. Killing mutants measures how much a specification **pins**, where silence
is expensive; implementing measures how much it gets **wrong**, where silence is free because a
capable agent fills the gap with a sensible default. `qwen1.5b` is second on one and last-equal on
the other: faithful by vacuity. Neither number describes a specification on its own.
