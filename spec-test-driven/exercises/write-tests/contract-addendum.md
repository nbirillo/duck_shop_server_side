# Contract addendum — advanced tier only

The class documentation in `:core` is silent on two points. In the basic tier that silence is the
lesson: you notice the question, decide it yourself, and either answer counts as long as it is a
decision rather than an accident.

This tier decides them for you. From here on the following is part of the contract:

> **Policies are pure.** `admits` has no side effects and depends only on the duck it is given. The
> order in which a combinator evaluates its policies, and how many times it evaluates each one, are
> therefore **not** part of the contract.
>
> **A combinator captures its policy list at construction.** Mutating the list you passed in
> afterwards is unspecified — a combinator may or may not notice, and neither behaviour is a defect.

## What changes because of it

A test that pins either of these is now wrong, and the conformance check will report it as a false
alarm. Concretely: no asserting that `AllOf` stops asking after the first refusal, no asserting the
order policies are consulted in, no counting how many times a policy was asked, and no test that
mutates the list after handing it over and expects the combinator to follow along.

If you took the basic tier, you may well have killed the `allof-defensive-copy` mutant there and been
right to. Nothing about that was a mistake — the question was open and you closed it. Here it has been
closed the other way, in writing, and the rules follow the addendum rather than your earlier choice.
That is the point of writing it down: an open question is answered by whoever is entitled to answer
it, and after that the suite's job is to respect the answer in **both** directions — pin what the
contract promises, and leave alone what it does not.

## Why these two

Both are the kind of thing a thorough suite reaches for on its own. Evaluation order and call counts
are observable with a spy, and a suite trying to be exhaustive will happily write that spy; list
aliasing is observable with three lines. Neither is promised anywhere, so pinning them costs a future
refactor its freedom while adding nothing a caller could ever rely on.

That is worth more attention than it usually gets. Over-specification does not show up as a failing
test — it shows up years later, as a suite that says no to a change that was allowed.
