# Write the specification

Until now the contract was handed to you. This time you write it.

The shop wants promotions, and your teacher will give you the whole of what the business said about
them — a few lines, no more.

> **The brief is not in this repository, on purpose.** It is a short piece of business text — a few
> If it were, the agent you later hand your specification to could read it — and then it
> would be answering the business, not you. Whatever it built would tell you nothing about
> your own text, which is the only thing this exercise measures.

That is a normal amount of detail to be given, and it is **not enough to implement from**. Your job
is to turn it into a contract precise enough that somebody — a colleague, or an agent — can implement
it without asking you anything, and precise enough that two correct implementations cannot disagree.

## What you are specifying

```kotlin
sealed interface DiscountRule {
    data class Percentage(val percent: Int) : DiscountRule
    data class AmountOff(val amount: Int) : DiscountRule
    data class BigSpenderBonus(val threshold: Int, val amount: Int) : DiscountRule
}

/** Returns what [duck] actually costs in a shop whose active promotions are [rules]. */
fun priceFor(duck: Duck, rules: List<DiscountRule>): Int
```

`Duck` is the type you already know: `name`, `price`, `hasKotlinAttribute`, `accessories`.

## How to write it

Copy [`SPEC-template.md`](SPEC-template.md) to `SPEC.md` and fill it in. The sections are not
decoration — each one is a different kind of statement, and a specification that skips one is missing
something a reader needs.

The section that will feel strangest is **"deliberately not specified"**. You met the idea while
hardening the test suite:
some behaviour is contract and some is an accident of how the code happens to be written, and pinning
an accident costs a future refactor its freedom. Now you are on the other side of it — you decide,
and you write the decision down.

## What makes this hard

Read the business text again and count how many different implementations satisfy it. They will not
all return the same number for the same duck. Every place where that is true is a decision waiting
for you: either you make it, or the person implementing makes it for you, quietly, and you find out
later.

You are not expected to have one right answer. You are expected to **have an answer, on purpose, and
to be able to say why** — and to be honest in the last two sections about what you left open.

## What happens to your spec

- It is read for **structure and completeness** — did you make statements of each kind, or only
  describe the happy path.
- The claims in it are turned into **executable properties** and run against a correct implementation
  and against deliberately broken ones. A claim that holds everywhere pins nothing; a claim that
  fails on correct code is simply wrong.
- Every operation in the signature is checked for **coverage** — if nothing in your spec says anything
  about some part of it, that shows up.
- And an agent implements the feature **from your text alone**. What it gets wrong is the most direct
  answer you will get to "was that enough?"

## Running the checks

Same order as above. Everything runs from `spec-test-driven/`, and the whole sequence uses **one**
specification path — if the two readers get different text, you are measuring the text, not the readers.

```bash
SPEC=exercises/write-spec/SPEC.md

# structure and coverage — is every name in the surface mentioned, is every section filled
./gradlew verifySpec -Pspec=$SPEC
```

Then your claims as code. Put any number of `.kt` files in `properties/`, named however you like, and
build a sandbox: your specification and the types, and nothing else.

```bash
./gradlew sandbox -Pname=a -Pspec=$SPEC
./gradlew -p sandbox/a test
```

A sandbox is a **separate Gradle build**. It has no `gradlew` of its own and this project's tasks do not
exist inside it, so every command stays here at the project root and reaches in with `-p`.

Finally, two readers of the same text. One interactive agent working in the sandbox, one over an API —
they have to be **two different agents**, and both competent, or what you measure is the weaker one's
incapacity rather than a gap in your text.

```bash
./gradlew sandbox -Pname=a -Pspec=$SPEC
#   ▸ open sandbox/a in your agent and let it implement. this step is yours, not a command
./gradlew prepareImplementation -Pagent=reader-a -Pspec=$SPEC -Pfrom=sandbox/a

./gradlew runAgent -Pmode=impl-from-spec -Pagent=reader-b \
    -Pprovider=ollama -Pmodel=qwen2.5-coder:32b -Pspec=$SPEC

./gradlew verifyDivergence -PagentA=reader-a -PagentB=reader-b
```

The last command records what each reader answers on a fixed corpus and reports how many inputs they
answer **differently**. It needs no reference and no answer key: every open input is a decision your text
did not make, so somebody else made it. Read the open lines, not the number — leaving something free on
purpose is a legitimate choice, and section 4 of the template is where you say so.

## The checker can be wrong, and you are allowed to say so

Checking a specification is hard — harder than checking code, and our tooling does not manage it
perfectly. One of those four steps, the one that turns your sentences into a list of claims, asks a
model to read your prose, and it makes mistakes. On the best specification we have, it reported a
claim as missing that the text states twice.

So if the report says something is absent and you are sure it is there, do not add a paragraph to
satisfy the tool. Treat it as a question rather than a verdict, and settle it:

1. **Say what you claim, and hand that in.** Write the claims yourself — one per line, by number or
   by name. From that list onward there is no model in the loop: those exact properties are run
   against a correct implementation and against deliberately broken ones, and the report comes back
   with both disagreements spelled out — claims you declared that the checker did not find, and
   claims it found that you never declared. The second list is usually the more interesting one.
2. **Let the later steps answer it.** The property run and the implement-from-your-spec run do not
   depend on the checker's reading. If the property holds, and an agent given only your text builds
   the thing you meant, then the claim was in there.
3. **Check by hand.** Find the sentence and point at it. If it is there, the checker was wrong. If
   what you find is vaguer than you remembered, then the checker read it the way a stranger would —
   and that is worth more to you than a clean report was.

Declaring a claim does not make it true, and the report will not pretend otherwise. A property you
declare still has to hold against correct code — one that fails there is a claim contradicting the
feature — and it still has to rule something out to be worth anything. You can win the argument with
the checker and discover in the same run that the claim you were defending constrains nobody.

Either outcome teaches you something, which is why this is written down rather than quietly fixed.
It is also the module's own point turned around and aimed at us: **a result from a tool is a claim,
and a claim gets verified.** That holds for the tool marking your work exactly as much as for the
agent writing your code.

Verification is what stays human — including verification of the thing doing the verifying.
