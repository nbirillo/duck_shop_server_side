# Frontier implementer run — 11.4 step 3e

Six ready-to-paste prompts, one per specification. Generated, not hand-written: regenerate any of
them with the `-Pdry` command at the bottom.

**The question this run answers.** The corpus has been implemented once, by `qwen2.5-coder:32b`, and
the specifications spread from 5/8 to 8/8. That spread may be a property of the specifications or a
property of that one implementer. If a frontier model fills every gap with the same default our
reference chose, the spread collapses and a thin specification looks complete — which would mean the
exercise's signal comes largely from the implementer, and that is worth knowing before it is taught.

## The one rule

**The session must not be *able* to read this repository.** Not "must be asked not to" — must not be
able to. Use a plain chat window: no file access, no MCP filesystem server, no working directory
inside the checkout.

We have already been caught by the weaker version of this. An agent told not to run Gradle rebuilt
the oracle with `kotlinc` instead and did exactly what the restriction existed to prevent. A
restriction on tools is not a restriction on information. If the model can reach
`reference/.../Pricing.kt`, the run is worthless and will not look it.

## Order — stop early if the answer is already clear

| # | folder | 32b scored | why this one |
| --- | --- | --- | --- |
| 1 | `written-claude-code` | 8/8 | control. If this is not 8/8, something is wrong with the run, not the spec. |
| 2 | `written-ollama-qwen2.5-coder-1.5b` | 7/8 | the vacuity case. Its spec pins almost nothing yet scored well, because gaps get filled with sensible defaults. A frontier should score it *higher*, and that is the collapse we are looking for. |
| 3 | `written-ollama-qwen2.5-coder-14b` | 5/8 | the worst. Its spec states two things that are false, so a stronger implementer should **not** rescue it — being faithful to a wrong specification is the correct behaviour. |
| 4 | `written-ollama-qwen2.5-coder-32b` | 6/8 | |
| 5 | `written-ollama-qwen2.5-coder-7b` | 6/8 | |
| 6 | `written-ollama-llama3.2-3b` | 6/8 | |

After three you already know the shape: if #2 rises and #3 does not, the spread is real and it is
measuring the specifications. If both rise to 8/8, it was measuring the implementer.

## Steps, per folder

1. Open `<folder>/prompt.txt`. It has two parts.
   - Everything under `===== SYSTEM =====` is the system prompt.
   - Everything under `===== USER =====` is the message.

   Paste both, verbatim. Add nothing, and do not summarise the specification.

2. If it asks a question about the specification — **do not answer it.** Say "decide and continue".
   The question itself is a finding about that specification; answering it puts you in the loop and
   the run stops measuring the text.

3. Save the Kotlin block it returns to:

   ```
   implementations/claude/<folder>/src/main/kotlin/org/jetbrains/kotlin/course/duck/shop/pricing/Pricing.kt
   ```

   Just the code, no fences.

4. Scaffold it:

   ```
   ./gradlew prepareImplementation -Pagent=claude -Pspec=fixtures/11.4/<written/name>.md
   ```

## When all the ones you want are in

```
./gradlew verifyImplementations -Pagent=claude
```

Reload Gradle first if the IDE has not noticed the new modules.

## Reading it

`#9 BestOf` and `#10 OnlyIf` are excluded automatically — a basic brief never showed them.

A divergence is **not** a defect until read. It means the agent and our reference disagree, which
happens when a specification failed to say something *and equally* when it said something different
on purpose and the agent obeyed. Both occurred in the 32b run:

- `1.5b`'s text claims overflow safety and the key counts the claim, but its only example is
  `AmountOff(1)` at `Int.MAX_VALUE`, which never multiplies — and the implementation went on to write
  `price * percent` in `Int`. A claim can be present, counted, and still not transmit.
- `7b`'s text says the bonus reads the *shelf* price and the agent used the running price anyway.

Watch `#8 overflow` in particular: it is the most common divergence in the corpus, 7 of 13, and it
transmits from the frontier specification and copies of it and from nothing else. Whether a frontier
*implementer* now supplies it unprompted is most of this experiment.

## Regenerating a prompt

```
./gradlew runAgent -Pmode=impl-from-spec -Pdry \
    -Pprovider=ollama -Pmodel=none -Pagent=claude \
    -Pspec=fixtures/11.4/written/<name>.md \
    -PpromptDir=../spec-test-driven/tools
```

`-Pdry` makes no API call and writes nothing. For an advanced-tier specification add
`-PimplSurface=../spec-test-driven/exercises/write-spec/README-advanced.md`, or the agent is shown
three rule kinds for a specification written against six.
