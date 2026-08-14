You are given several specifications of the same feature, written independently. Produce **one**.

The merged specification must be **shorter than the longest of them and no weaker than the best of
them**. That is the whole difficulty: length is easy to cut and easy to lose things by cutting.

What "no weaker" means, precisely:

- Every statement that **changes what an implementation must do** survives. If two implementations
  could differ on some input and the merged text no longer decides which is right, you removed
  something load-bearing.
- Every statement kept must be **checkable** — an input can be pointed at and the statement judged.
- Where the sources **disagree**, do not average them and do not keep both. Decide, say which you
  chose, and say it in one line.
- Where a source claims something **false**, drop it. Where a source claims something that nothing
  could violate, drop that too — it reads as rigour and pins nothing.

What to cut without mercy: restatement, worked examples that repeat a rule already stated in general
form, prose that describes the document rather than the feature, and anything in the "deliberately not
specified" or "open questions" sections that the behaviour section has already decided.

Keep the section headings. Output **Markdown only**, starting with the first heading, no preamble and
no commentary about what you merged.
