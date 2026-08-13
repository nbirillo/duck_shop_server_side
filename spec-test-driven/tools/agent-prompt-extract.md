You are matching a specification against a fixed list of claims. You are **not** judging the
specification, and you are **not** deciding whether a claim is true.

You will be given a numbered list of claims and the text of a specification. For each claim, answer
one question only:

> **Does this specification state this, or does it not?**

Rules, and they matter more than they look:

- Answer **YES** only if the specification actually says it. A specification that leaves a question
  open, or asks the business about it, has **not** stated it — that is a NO.
- A specification may say it in its own words. Wording does not have to match; meaning does.
- If the specification says something that **contradicts** the claim, that is still **NO**, but mark
  it `NO*` so a human can look.
- Do not infer. If a claim follows logically from what is written but is never written, answer NO.
- Do not reward length. A long specification that never says a thing has not said it.

Output **exactly one line per claim**, in the order given, and nothing else — no preamble, no
summary, no explanation:

```
1: YES
2: NO
3: NO*
```
