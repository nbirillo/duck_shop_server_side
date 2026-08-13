# 11.4 fixtures — every spec any agent has written for us

**These are committed, unlike every other agent artifact in this repository.** The hardened suites,
the generated solutions and the attacks are all git-ignored because anyone with Ollama can reproduce
them. These cannot be: the frontier ones need an API key we do not have, and were produced by hand in
a separate session. Losing them would cost us the only high-quality end of the range.

They serve three purposes.

**1. The handout for exercise 11.4b.** The compression task ("take these and produce one that is
shorter and no weaker") needs a corpus spanning the range, and this is it. It also makes the exercise
independent of which model the learner owns — a student with only a small local model still gets to
work with a frontier spec, and a student with a frontier model still has to deal with a bad one.
Hand out `written/` at 11.4b, **not before**: 11.4a is writing your own from the bare brief, and
seeing these first would replace that with critiquing.

**2. Benchmark fixtures for the checking machinery.** Layers 1 and 2 are to be benchmarked for
*consistency* and *discrimination* — same spec, N runs, same verdict, and does it separate the vague
from the precise. These are specs of genuinely graded quality, produced under identical conditions,
which is exactly the fixture set that benchmark needs. We had planned to write such fixtures by hand;
the calibration produced better ones for free.

**3. A regression corpus.** When the checker exists, run it over all of these and compare its ranking
with the by-hand judgement recorded in `../../teacher/exercise-11.4-calibration.md`. Any checker that
does not reproduce that ordering is wrong — and we already know one way to get it wrong, because our
first keyword scan ranked the best spec last.

## What is here

`written/` — exercise 11.4a, the bare brief, one spec per agent. Range: 29 lines (7b, plausible and
self-contradicting) to 269 (frontier, the only one that found the threshold-basis fork, the two
rounding formulations and the overflow). `ollama-qwen2.5-coder-1.5b.md` is the useful bad example —
it gives worked examples instead of rules, so rounding never surfaces at all.

`advanced/` — exercise 11.4 advanced, the composing version with the laws section. Only 32b so far;
its four laws are three trivial or duplicated and one true-but-declared-false with a counterexample
where both sides do nothing. The frontier run of this tier is still to be done.

`compressed/` — exercise 11.4b, the compression task, given `written/{1.5b, 32b, claude-code}`.
Nobody managed it: sources were 269, 49 and 40 lines, outputs are 109 to 257. The failures split in
two, and the split is worth showing to learners. 1.5b and 7b are **93–94% verbatim from the frontier
spec with one original line each** — transcription, not merging. 14b and 32b genuinely rewrote (12–25%
verbatim, 32–48 original lines) and still came out at 109 and 121, four times the substantive content.

That second finding is the strongest argument in the whole sub-module against reaching for a better
model: **handed a frontier spec, the small models copied it.** A good example is not a substitute for
knowing what to do with one.
