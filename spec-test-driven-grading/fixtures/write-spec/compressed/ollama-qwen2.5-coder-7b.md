# Specification — `priceFor`

## 1. What it does

`priceFor(duck, rules)` returns what one duck actually costs in a shop whose active promotions are `rules`: it starts from the duck's shelf price (`duck.price`) and applies every rule in the list, in list order, each rule reducing the price that the rules before it left behind. The answer is a whole number of the same currency units as `duck.price`, and never less than 0.

## 2. Behaviour

**Input domain (assumed — see Q4, Q5).** This section defines the answer for `duck.price >= 0` and
for rules whose every field is non-negative: `Percentage.percent >= 0` (it may exceed 100),
`AmountOff.amount >= 0`, `BigSpenderBonus.threshold >= 0`, `BigSpenderBonus.amount >= 0`. `rules` may
be empty, may contain any number of rules of any kinds in any order, and may contain duplicates.
Behaviour outside this domain is left free (see section 4, N1).

**B1 — the value is a left fold over the rule list.** Write `rules = [r₁, r₂, …, rₙ]`. Define

```
p₀ = duck.price
pᵢ = max(0, pᵢ₋₁ − discount(rᵢ, pᵢ₋₁))     for i = 1 … n
priceFor(duck, rules) = pₙ
```

`pᵢ₋₁` is called *the price the rule is applied to*. The three rule kinds define `discount` as
follows.

**B2 — `Percentage(percent)`** discounts `percent` percent of the price it is applied to, with the
*discount* rounded down to a whole unit:

```
discount(Percentage(percent), p) = floor(p × percent / 100)
```

The product `p × percent` is computed exactly, without intermediate overflow of the 32-bit range; a
`Long` (or equivalent) intermediate is required (see E14). Rounding down the discount means the
rounding goes to the shop, not the customer: `priceFor(price = 95, [Percentage(10)]) == 86`, not `85`.

**B3 — `AmountOff(amount)`** discounts exactly `amount`, independent of the price it is applied to:

```
discount(AmountOff(amount), p) = amount
```

**B4 — `BigSpenderBonus(threshold, amount)`** discounts `amount` when the price it is applied to
reaches `threshold`, and nothing otherwise:

```
discount(BigSpenderBonus(threshold, amount), p) = if (p >= threshold) amount else 0
```

**B5 — the threshold test is inclusive.** `p == threshold` fires the bonus; `p == threshold − 1` does
not. ("Priced *from* 100" is read as `>= 100`.)

**B6 — the bonus is tested against the price the rule is applied to, not the shelf price** (assumed —
see Q2). So an earlier rule can push the price below `threshold` and thereby suppress the bonus, and
no rule ever consults `duck.price` except the first one in the list.

**B7 — the bonus's own fields are the thresholds and amount used.** The business's "from 100 … 20 off"
is one example; a `BigSpenderBonus(500, 50)` behaves per B4 with 500 and 50. Nothing in the
calculation hard-codes 100 or 20.

**B8 — the bonus does not require any other rule to be present.** `priceFor(price = 100,
[BigSpenderBonus(100, 20)]) == 80`. "A *further* 20 off" describes how the bonus stacks, not a
precondition that something else discounted first.

**B9 — the price is clamped at 0 and never goes negative.** For every input in the domain,
`0 <= priceFor(duck, rules) <= duck.price`. A discount larger than the price it is applied to takes the
price to exactly 0, and every subsequent rule leaves it at 0.

**B10 — every rule in the list is applied, once per list element.** There is no deduplication, no
"best rule wins", no cap on total discount, and no mutual exclusion between kinds. Two equal rules
compound: `priceFor(price = 100, [Percentage(10), Percentage(10)]) == 81`, not `80` and not `90`.

**B11 — list order is significant.** `priceFor` is *not* invariant under permuting `rules`:
`priceFor(price = 100, [Percentage(10), AmountOff(5)]) == 85` while
`priceFor(price = 100, [AmountOff(5), Percentage(10)]) == 86`. Callers that want a particular stacking
order must supply it; the function does not reorder, sort, or group rules by kind.

**B12 — a rule affects the result only through its kind and its field values.** Object identity and
whether the same instance appears twice are irrelevant; two `Percentage(10)` values are
interchangeable.

**B13 — `duck` affects the result only through `price`.** `name`, `hasKotlinAttribute` and
`accessories` are never read. Two ducks with equal `price` get equal answers for the same rules.

**B14 — `priceFor` is pure.** It has no side effects; it does not modify `duck` (which is immutable
anyway) nor the `rules` list nor any accessory; it does not retain the list, so mutating the caller's
list after the call cannot change an already-returned value. Repeated calls with equal arguments return
equal results, in any order, from any thread. It accepts any `List` implementation — mutable,
immutable, or a view.

**B15 — no exceptions for in-domain input.** For every input in the stated domain `priceFor` returns
normally. `DiscountRule` is sealed and has exactly the three cases above; there is no fourth case to
reject.

**B16 — monotonicity.** Appending a rule to `rules` never increases the result:
`priceFor(duck, rules + r) <= priceFor(duck, rules)`. (This follows from B1–B4 in the stated domain; it
is written out because it is the property most worth checking.)

### Worked examples (all with `duck.price` as shown; other `Duck` fields arbitrary)

| `price` | `rules` | result | why |
| --- | --- | --- | --- |
| 100 | `[]` | 100 | no rules, shelf price |
| 100 | `[Percentage(10)]` | 90 | discount 10 |
| 100 | `[AmountOff(5)]` | 95 | |
| 100 | `[BigSpenderBonus(100, 20)]` | 80 | 100 >= 100, B5/B8 |
| 99 | `[BigSpenderBonus(100, 20)]` | 99 | 99 < 100 |
| 95 | `[Percentage(10)]` | 86 | discount `floor(9.5) = 9`, B2 |
| 5 | `[Percentage(10)]` | 5 | discount `floor(0.5) = 0` — the discount rounds away entirely |
| 100 | `[Percentage(10), AmountOff(5)]` | 85 | 100 → 90 → 85 |
| 100 | `[AmountOff(5), Percentage(10)]` | 86 | 100 → 95 → 86; order matters (B11) |
| 100 | `[Percentage(10), Percentage(10)]` | 81 | compounding (B10) |
| 100 | `[Percentage(10), BigSpenderBonus(100, 20)]` | 90 | 90 < 100, bonus suppressed (B6) |
| 100 | `[BigSpenderBonus(100, 20), Percentage(10)]` | 72 | 100 → 80 → 72 |
| 100 | `[BigSpenderBonus(100, 20), BigSpenderBonus(100, 20)]` | 80 | the second sees 80 < 100 (B6) |
| 100 | `[Percentage(150)]` | 0 | discount 150, clamped (B9) |
| 100 | `[AmountOff(250), AmountOff(10)]` | 0 | clamped at 0, then stays 0 |

## 3. Edge cases

**E1 — empty rule list.** `priceFor(duck, emptyList()) == duck.price`, for every duck.

**E2 — shelf price 0.** `duck.price == 0` gives `0` for every list of in-domain rules. (A percentage of
0 is 0; any amount takes it below 0 and it clamps back; `BigSpenderBonus(0, a)` fires and still clamps
to 0.)

**E3 — zero-valued discounts.** `Percentage(0)`, `AmountOff(0)` and `BigSpenderBonus(t, 0)` all leave
the price unchanged, whatever it is.

**E4 — `Percentage(100)`** takes the price it is applied to exactly to 0.

**E5 — `percent` above 100** is legal and clamps: `price = 100, [Percentage(150)] → 0`. It is not an
error and does not produce a negative price.

**E6 — `AmountOff` larger than the price** gives 0, not a negative number: `price = 30,
[AmountOff(75)] → 0`.

**E7 — rounding, on both sides of the half.** `price = 94, [Percentage(10)] → 85` (9.4 → 9);
`price = 95, [Percentage(10)] → 86` (9.5 → 9); `price = 96, [Percentage(10)] → 87` (9.6 → 9). The
discount is always rounded *down*, never to nearest — note that 95 and 96 give different prices while
95 and 94 do not.

**E8 — a discount that rounds away to nothing.** `price = 5, [Percentage(10)] → 5`. `price = 9,
[Percentage(11)] → 9` (`floor(0.99) = 0`). A rule may legally have no effect at all.

**E9 — threshold boundary.** `price = 100, [BigSpenderBonus(100, 20)] → 80`;
`price = 99, [BigSpenderBonus(100, 20)] → 99`; `price = 101, [BigSpenderBonus(100, 20)] → 81`.

**E10 — `threshold == 0`** fires for every price, including 0, because the price is never negative
(B9): `price = 100, [BigSpenderBonus(0, 20)] → 80`.

**E11 — `threshold == Int.MAX_VALUE`** fires only for `price == Int.MAX_VALUE`.

**E12 — several bonuses.** Each is tested independently, at its own position, against the price at that
point: `price = 100, [BigSpenderBonus(100, 20), BigSpenderBonus(80, 20), BigSpenderBonus(80, 20)] → 60`
(100 → 80 → 60 → 60, since the third sees 60 < 80).

**E13 — many rules, price already at 0.** Once the running price is 0 it stays 0 for every remaining
in-domain rule, so a long list is well defined and terminates at 0:
`price = 10, [AmountOff(10)] + List(1000) { Percentage(50) } → 0`.

**E14 — the largest shelf price.** `price = Int.MAX_VALUE (2147483647), [Percentage(10)] → 1932735283`
(discount `floor(214748364.7) = 214748364`). An implementation that multiplies `price * percent` in
32-bit arithmetic overflows here and is wrong. The *result* always fits in `Int`, because it lies in
`0 .. duck.price` (B9).

**E15 — `Percentage(Int.MAX_VALUE)`** is in domain and yields 0 for any price (the exact discount is
far above the price). The exact-arithmetic requirement of B2 covers this case too.

**E16 — duck fields that are not the price.** `priceFor(Duck("a", 100, false, emptyList()), rules)` and
`priceFor(Duck("zzz", 100, true, listOf(Accessory("hat"), Accessory("hat"))), rules)` are equal for
every `rules`. An empty accessory list is not a special case.

**E17 — the price the customer pays can be 0.** Nothing in this specification prevents a duck from
being free (E2, E4, E6). See Q6 if the business wants a floor above 0.

## 4. Deliberately not specified

- **Order of promotions**: The order in which promotions are applied should be respected.
  - `priceFor(Duck("Duck", 100), listOf(AmountOff(5), Percentage(10))) == 95`
- **Complexity**: The function should handle complex scenarios efficiently, such as applying multiple discounts in sequence.

## 5. Open questions

- **Implementation details**: How are the discount rules applied? Should they be applied sequentially or simultaneously?
- **Performance**: What is the time complexity of `priceFor`? Can it be optimized for large lists of promotions?

The difference from section 4 matters. Section 4 is "any answer is fine". This section is "there is a right answer and I do not know it".
