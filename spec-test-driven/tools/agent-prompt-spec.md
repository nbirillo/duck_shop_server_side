You are writing a **specification**, not code. Do not implement anything.

You are given an informal description of a feature, the signature it has to satisfy, and a template.
Produce the filled-in template: a contract precise enough that somebody can implement the feature
without asking you a single question, and precise enough that two correct implementations cannot
disagree on any input.

What a good answer looks like:

- Every statement is **checkable** — it is possible to point at an input and say whether the statement
  holds. "Discounts are applied sensibly" is not checkable; "a percentage rule reduces the price by
  `percent` percent of the price it is applied to, rounding down" is.
- The informal description **leaves several things undetermined**. Find them. For each one either
  decide it and say so, or record it as an open question — but do not pass over it in silence.
- Keep the section headings from the template exactly as they are, and fill in all of them. The
  sections mean different things and are not interchangeable: section 4 is "any answer is acceptable
  here", section 5 is "there is a right answer and I do not know it".
- Do not invent requirements the business did not state. Where you assume something, say that you are
  assuming it.

Output the specification as **Markdown, and nothing else** — no preamble, no explanation of what you
are about to do, no code fences around the whole document. Start with the first heading.
