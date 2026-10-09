# Specification — `priceFor`

## 1. What it does

`priceFor(duck, rules)` returns what one duck actually costs in a shop whose active promotions are `rules`: it starts from the duck's shelf price (`duck.price`) and applies every rule in the list, in list order, each rule reducing the price that the rules before it left behind. The answer is a whole number of the same currency units as `duck.price`, and never less than 0.

## 2. Behaviour

**Input domain (assumed).** This section defines the answer for `duck.price >= 0` and for rules whose every field is non-negative: `Percentage.percent >= 0`, `AmountOff.amount >= 0`, `BigSpenderBonus.threshold >= 0`, `BigSpenderBonus.amount >= 0`. `rules` may be empty, may contain any number of rules of any kinds in any order, and may contain duplicates. Behaviour outside this domain is left free.

**B1 — the value is a left fold over the rule list.** Write `rules = [r₁, r₂, …, rₙ]`. Define

```
p₀ = duck.price
pᵢ = max(0, pᵢ₋₁ − discount(rᵢ, pᵢ₋₁))     for i = 1 … n
priceFor(duck, rules) = pₙ
```

`pᵢ₋₁` is called *the price the rule is applied to*. The three rule kinds define `discount` as follows.

**B2 — `Percentage(percent)`** discounts `percent` percent of the price it is applied to, with the *discount* rounded down to a whole unit:

```
discount(Percentage(percent), p) = floor(p × percent / 100)
```

The product `p × percent` is computed exactly, without intermediate overflow of the 32-bit range; a `Long` (or equivalent) intermediate is required. Rounding down the discount means the rounding goes to the shop, not the customer.

**B3 — `AmountOff(amount)`** discounts exactly `amount`, independent of the price it is applied to:

```
discount(AmountOff(amount), p) = amount
```

**B4 — `BigSpenderBonus(threshold, amount)`** discounts `amount` when the price it is applied to reaches `threshold`, and nothing otherwise:

```
discount(BigSpenderBonus(threshold, amount), p) = if (p >= threshold) amount else 0
```

**B5 — the threshold test is inclusive.** `p == threshold` fires the bonus; `p == threshold − 1` does not.

**B6 — the bonus is tested against the price the rule is applied to, not the shelf price.**

**B7 — the bonus's own fields are the thresholds and amount used.**

**B8 — the bonus does not require any other rule to be present.**

**B9 — the price is clamped at 0 and never goes negative.**

**B10 — every rule in the list is applied, once per list element.**

**B11 — list order is significant.**

**B12 — a rule affects the result only through its kind and its field values.**

**B13 — `duck` affects the result only through `price`.**

**B14 — `priceFor` is pure.**

**B15 — no exceptions for in-domain input.**

**B16 — monotonicity.** Appending a rule to `rules` never increases the result.

## 3. Edge cases

**E1 — empty rule list.** `priceFor(duck, emptyList()) == duck.price`, for every duck.

**E2 — shelf price 0.** `duck.price == 0` gives `0` for every list of in-domain rules.

**E3 — zero-valued discounts.** `Percentage(0)`, `AmountOff(0)` and `BigSpenderBonus(t, 0)` all leave the price unchanged, whatever it is.

**E4 — `Percentage(100)`** takes the price it is applied to exactly to 0.

**E5 — `percent` above 100** is legal and clamps: `price = 100, [Percentage(150)] → 0`.

**E6 — `AmountOff` larger than the price** gives 0, not a negative number: `price = 30, [AmountOff(75)] → 0`.

**E7 — rounding, on both sides of the half.**

**E8 — a discount that rounds away to nothing.**

**E9 — threshold boundary.**

**E10 — `threshold == 0`** fires for every price, including 0.

**E11 — `threshold == Int.MAX_VALUE`** fires only for `price == Int.MAX_VALUE`.

**E12 — several bonuses.** Each is tested independently, at its own position, against the price at that point.

**E13 — many rules, price already at 0.** Once the running price is 0 it stays 0 for every remaining in-domain rule.

**E14 — the largest shelf price.**

**E15 — `Percentage(Int.MAX_VALUE)`** is in domain and yields 0 for any price.

**E16 — duck fields that are not the price.**

**E17 — the price the customer pays can be 0.**

## 4. Deliberately not specified

- **Order of promotions**: The order in which promotions are applied should be respected.
- **Complexity**: The function should handle complex scenarios efficiently, such as applying multiple discounts in sequence.

## 5. Open questions

- **Implementation details**: How are the discount rules applied? Should they be applied sequentially or simultaneously?
- **Performance**: What is the time complexity of `priceFor`? Can it be optimized for large lists of promotions?
