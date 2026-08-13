# Specification — `priceFor`

## 1. What it does

`priceFor(duck, rules)` returns what one duck costs in a shop whose active promotions are `rules`: it
starts from the shelf price `duck.price` and applies every rule in list order, each rule discounting
the price the rules before it left behind. The result is a whole number in the same units as
`duck.price`, never negative and never above `duck.price`.

## 2. Behaviour

**Input domain.** This section defines the answer for `duck.price >= 0` and rules whose every field is
non-negative: `Percentage.percent >= 0` (it may exceed 100), `AmountOff.amount >= 0`,
`BigSpenderBonus.threshold >= 0`, `BigSpenderBonus.amount >= 0`. `rules` may be empty, and may contain
any number of rules of any kinds in any order, including duplicates. Outside this domain the behaviour
is free (N1).

**B1 — the result is a left fold over the rule list.** For `rules = [r₁, …, rₙ]`:

```
p₀ = duck.price
pᵢ = max(0, pᵢ₋₁ − discount(rᵢ, pᵢ₋₁))     for i = 1 … n
priceFor(duck, rules) = pₙ
```

`pᵢ₋₁` is *the price the rule is applied to*. *Chosen:* rules compound against the running price, not
each against the shelf price — "a **further** 20 off" reads as stacking, and it is the reading under
which the shop's rule order carries meaning.

**B2 — `Percentage(percent)`** discounts `percent` percent of the price it is applied to, with the
*discount* rounded down:

```
discount(Percentage(percent), p) = floor(p × percent / 100)
```

So `price = 95, [Percentage(10)] → 86`, not `85`: the fraction goes to the shop. `p × percent` must be
computed without 32-bit overflow (E7).

**B3 — `AmountOff(amount)`** discounts exactly `amount`, whatever the price it is applied to.

**B4 — `BigSpenderBonus(threshold, amount)`** discounts `amount` when the price it is applied to is
`>= threshold`, and 0 otherwise. The test is inclusive: `p == threshold` fires, `p == threshold − 1`
does not ("priced *from* 100" is `>= 100`). The rule's own fields are used; nothing hard-codes 100 or
20, and the bonus needs no other rule present — `price = 100, [BigSpenderBonus(100, 20)] → 80`.

**B5 — the bonus is tested against the price the rule is applied to, and fires at its own position in
the list.** An earlier rule can push the price below `threshold` and so suppress it:
`price = 100, [Percentage(10), BigSpenderBonus(100, 20)] → 90`. *Chosen:* running price over shelf
price, and list position over "bonus applied last" — every rule is then a function of one price, and
the whole calculation stays a plain fold.

**B6 — the price is clamped at 0 and never negative.** A discount larger than the price it is applied
to takes the price to exactly 0, and every later rule leaves it at 0.

**B7 — every list element is applied, once.** No deduplication, no "best rule wins", no cap on total
discount, no mutual exclusion between kinds. Equal rules compound:
`price = 100, [Percentage(10), Percentage(10)] → 81`, not `80` and not `90`.

**B8 — list order is significant.** `priceFor` is not invariant under permuting `rules`:
`price = 100, [Percentage(10), AmountOff(5)] → 85` while
`price = 100, [AmountOff(5), Percentage(10)] → 86`. The function never reorders, sorts, or groups
rules by kind; a caller wanting a particular stacking order must supply it.

**B9 — `duck` affects the result only through `price`.** `name`, `hasKotlinAttribute` and `accessories`
are never read: two ducks with equal `price` get equal answers for the same rules.

**B10 — `priceFor` is pure.** Equal arguments give equal results on every call. It mutates nothing —
not `duck`, not the `rules` list, not an accessory — and does not retain the list, so mutating the
caller's list afterwards cannot change an already-returned value.

**B11 — no exceptions for in-domain input.** `priceFor` returns normally for every input in the domain
above. `DiscountRule` is sealed with exactly these three cases; there is no fourth case to reject.

## 3. Edge cases

**E1 — empty rule list.** `priceFor(duck, emptyList()) == duck.price`, for every duck.

**E2 — shelf price 0.** Every in-domain rule list gives `0`.

**E3 — zero-valued discounts.** `Percentage(0)`, `AmountOff(0)` and `BigSpenderBonus(t, 0)` leave the
price unchanged, whatever it is.

**E4 — discount at or beyond the whole price.** `Percentage(100)` takes the price it is applied to
exactly to 0. `percent > 100` is legal and clamps, not an error: `price = 100, [Percentage(150)] → 0`.
`AmountOff` above the price gives 0: `price = 30, [AmountOff(75)] → 0`.

**E5 — rounding is down, not to nearest.** `price = 94, [Percentage(10)] → 85`;
`price = 95 → 86`; `price = 96 → 87`. Note 95 and 96 give different prices while 94 and 95 do not.

**E6 — a discount can round away to nothing.** `price = 5, [Percentage(10)] → 5`;
`price = 9, [Percentage(11)] → 9`. A rule may legally have no effect.

**E7 — exact arithmetic at the top of the range.** `price = Int.MAX_VALUE, [Percentage(10)] →
1932735283` (discount `floor(214748364.7)`). An implementation multiplying `price * percent` in 32-bit
arithmetic overflows here and is wrong; a `Long` intermediate is required.
`Percentage(Int.MAX_VALUE)` is in domain and gives 0 for any price. The *result* always fits in `Int`
because it lies in `0 .. duck.price`.

**E8 — threshold boundaries.** With `BigSpenderBonus(100, 20)`: `price = 99 → 99`, `price = 100 → 80`,
`price = 101 → 81`. `threshold == 0` fires at every price, including 0.

**E9 — several bonuses.** Each is tested independently at its own position against the price at that
point: `price = 100, [BigSpenderBonus(100, 20), BigSpenderBonus(80, 20), BigSpenderBonus(80, 20)] → 60`
(100 → 80 → 60 → 60, the third seeing 60 < 80).

## 4. Deliberately not specified

**N1 — behaviour outside the input domain of section 2**: a negative `duck.price`, `percent`, `amount`
or `threshold`. An implementation may apply the same formulas, clamp, or throw, and the exception type
and message are free. The business described discounts on shelf prices; negative inputs are not part of
that world, and with a negative `percent` on a large price no answer both follows the fold and fits
`Int`.

**N2 — how the value is computed.** B1 defines the value, not an algorithm: fold, loop, recursion, or
summing discounts where equivalent all conform. Whether the clamp is applied after each rule or only to
the final result is free — in the domain of section 2 both give the same answer everywhere — as is
stopping early once the price reaches 0. Rules are data, not callbacks, so there are no observable
intermediate values or call counts.

**N3 — performance and allocation.** No guarantee of linearity, of allocation behaviour, or of whether
`rules` is defensively copied.

**N4 — packaging.** Where the function lives (file, package, top-level versus member or extension of
`Shop`), so long as a callable with this signature and behaviour exists.

## 5. Open questions

**Q1 — is the shop's promotion configuration already validated?** *Assumed:* yes, so all fields and
`duck.price` are non-negative and behaviour outside that is free (N1). If unvalidated rules can arrive,
`priceFor` needs a defined answer — most likely rejecting the list — and N1 moves into section 2. This
also decides whether `percent > 100` is a legal clamp-to-free (E4) or a configuration error.

**Q2 — which way should money rounding go, and how often?** *Assumed:* the discount rounds down, once
per rule (B2). Rounding is usually an accounting or legal policy; rounding only the final price differs
from this whenever two percentage rules chain.

**Q3 — is there a minimum sale price or a cap on total discount?** The business text mentions neither,
so its absence is my inference. *Assumed:* no floor above 0 and no cap, so a rule list may make a duck
free and may stack past 100% off. A "never more than 50% off" or "promotions are mutually exclusive,
best one wins" policy is common enough to confirm explicitly.
