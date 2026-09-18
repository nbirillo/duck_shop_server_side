# The capstone: a franchise

You have met every piece of this separately. This time you do the whole thing.

## What you are walking into

A chain of duck shops. Each shop already had its own door policy and its own promotions; the chain has
rules of its own as well. A customer with one duck wants to know **where in the chain to buy it and what
they will pay**.

Somebody has already started. In `inherited/` you will find:

- **`SPEC.md`** — the specification of the feature as it currently stands.
- **`src/main/kotlin/.../BestOffer.kt`** — an implementation an agent wrote from that specification.
- **`src/main/kotlin/.../Pricing.kt`** — `priceFor`, settled back when you wrote its specification.
  Not your problem here.
- **`src/test/kotlin/.../FranchiseTests.kt`** — the tests that came with the implementation.

None of it is yours to defend. It is a starting position, and the only thing you are told about it is
that it was produced the way things usually are: under time pressure, by more than one person, with an
agent doing the typing.

### Everything in this folder

| Path | What it is |
| --- | --- |
| `inherited/SPEC.md` | The specification you inherited. Read it before you change anything |
| `inherited/src/main/kotlin/…/BestOffer.kt` | The implementation written from it — **replace this file** when your agent produces a better one |
| `inherited/src/main/kotlin/…/Pricing.kt` | `priceFor`, settled when you specified it earlier. Use it; do not reopen it |
| `inherited/src/test/kotlin/…/FranchiseTests.kt` | The tests that came with the implementation |
| [`surface.md`](surface.md) | What you hand the agent together with **your** specification, and nothing else |
| `work/src/test/kotlin/` | **Your** tests. Empty until you put something in it |

`:core` comes from the `spec-test-driven/` folder next door, which you already have. There is nothing
else here — no reference, no answer key, no graded checks.

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

**1 — see where you are starting from.**

```bash
./gradlew :inherited:test    # the tests you inherited, against the code you inherited
```

**2 — write your specification.** A file in this folder; `my-spec.md` below, name it what you like.

**3 — have an agent implement it, from that text and nothing else.**

```bash
./gradlew runAgent -Pmode=impl-from-spec -Pagent=my-run -Pspec=my-spec.md \
    -Pprovider=ollama -Pmodel=qwen2.5-coder:14b
```

`-Pagent` names the run, so a second run does not overwrite the first. `-Pspec` is your text — the agent
receives it and `surface.md`, nothing else. `-Pprovider`/`-Pmodel` choose the agent.

It writes one file: `implementations/my-run/my-spec/src/main/kotlin/…/pricing/Pricing.kt`.

**4 — put it in place of what you inherited**, overwriting
`inherited/src/main/kotlin/…/pricing/BestOffer.kt`. That is the deliverable: you are replacing that file,
not adding next to it. Keep the earlier one somewhere if you want to compare.

**5 — run your own tests against it.**

```bash
./gradlew :work:test         # your tests, against whatever is in inherited/ right now
```

Run the same agent more than once before you conclude anything about it. Two different agents on the
same specification is more interesting still — where they disagree, your specification did not say.

## The check your tests will face

There is one more instrument, and **you do not run it** — it is run on your work, and you will see it
demonstrated in class rather than in this folder.

It takes **two complete implementations** of the feature. Both are sensible, both pass the tests that came
with this project, and they answer one question the business text never settled **differently**. Then it
runs *your* tests against each of them and asks a single thing: **do your tests tell them apart?**

- **SETTLED** — your tests accept only one of the two. You decided, and the decision is visible in code.
- **LEFT OPEN** — your tests accept both. That is not a bug you missed; it is a decision nobody made,
  not by whoever wrote what you inherited, and so far not by you.

Neither of the two is wrong, so nothing here tells you which to pick — only whether you picked. That is a
different kind of failure from a bug, and no mutant will ever find it for you.

**Why it is not in this folder:** each of those implementations is a complete, working answer to the
feature. Handing you the source would hand you our answer — and would hand it to any agent you point at
this directory, which is the one thing this exercise cannot survive.

## How this is judged

Not on matching our answer. Wherever the feature leaves a real choice, we do not have an answer — we have
a choice of our own, which we made and can defend. You are judged on whether **you** made yours, whether
your tests hold you to it, and whether you can say which of the things you inherited were decisions and
which were nobody's decision at all.
