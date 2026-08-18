# The capstone: a franchise

You have met every piece of this separately. This time you do the whole thing.

## What you are walking into

A chain of duck shops. Each shop already had its own door policy and its own promotions; the chain has
rules of its own as well. A customer with one duck wants to know **where in the chain to buy it and what
they will pay**.

Somebody has already started. In `inherited/` you will find:

- **`SPEC.md`** — the specification of the feature as it currently stands.
- **`src/main/kotlin/.../BestOffer.kt`** — an implementation an agent wrote from that specification.
- **`src/main/kotlin/.../Pricing.kt`** — `priceFor`, settled back in exercise 11.4. Not your problem.
- **`src/test/kotlin/.../FranchiseTests.kt`** — the tests that came with the implementation.

None of it is yours to defend. It is a starting position, and the only thing you are told about it is
that it was produced the way things usually are: under time pressure, by more than one person, with an
agent doing the typing.

## What you deliver

1. **A specification you would be willing to hand to a stranger.** It may be a rewrite of the one you
   inherited, or several documents if that reads better — long is not a virtue.
2. **A test suite that pins what your specification says.** This is the deliverable that carries the
   weight, for the reason the whole module has been about: it is the part a machine cannot check for you.
3. **An implementation an agent writes from your specification alone** — not from your tests, not from
   the code you inherited, not from a conversation. `surface.md` says what to hand it.

Then the obvious question, and it is the graded one: **does the implementation your agent wrote do what
you meant?** If it does not, decide whether the agent was wrong or your specification was, and say which.

## Running it

```bash
./gradlew :inherited:test        # the tests you inherited, against the implementation you inherited
./gradlew :work:test            # your own tests

# an agent implements from your specification and nothing else
./gradlew runAgent -Pmode=impl-from-spec -Pagent=<name> -Pspec=<your-spec.md> \
    -Pprovider=ollama -Pmodel=<model>

# record what two implementations answer on a fixed corpus, and diff them
./gradlew :implementations:<name>:test
```

Run the same agent more than once before you conclude anything about it. Two different agents on the
same specification is more interesting still — where they disagree, your specification did not say.

## The one check that is new

```bash
./gradlew generateForks -PforkTests=work/src/test/kotlin
./gradlew verifyForks
```

`forks/` holds two **readings** of one thing the feature leaves open: they are both complete, both
sensible, and they answer that one question differently. The check runs your tests against each and tells
you whether your tests **accept only one of them**.

Neither reading is wrong, and the report will never tell you which to pick. What it can tell you is
whether you picked at all — because a suite that passes on both has not said anything about that
question, and neither has your specification. That is a different failure from a bug, and no mutant will
ever find it for you.

There is one fork in this folder so that you can see how the check behaves. **There are others, and they
are not in this folder.** A clean report here means "the fork you practised on is decided" — never "my
specification is complete". You have met that distinction before.

## How this is judged

Not on matching our answer. Wherever the feature leaves a real choice, we do not have an answer — we have
a choice of our own, which we made and can defend. You are judged on whether **you** made yours, whether
your tests hold you to it, and whether you can say which of the things you inherited were decisions and
which were nobody's decision at all.
