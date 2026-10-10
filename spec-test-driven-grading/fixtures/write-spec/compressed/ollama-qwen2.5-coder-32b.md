# Specification — `priceFor`

## 1. What it does

`priceFor(duck, rules)` returns the final price of a duck after applying all discount rules in the list to its original shelf price (`duck.price`). The answer is a whole number and never less than 0.

## 2. Behaviour

**Input domain:** This section defines the answer for `duck.price >= 0` and for rules whose every field is non-negative: `Percentage.percent >= 0`, `AmountOff.amount >= 0`, `BigSpenderBonus.threshold >= 0`, `BigSpenderBonus.amount >= 0`. `rules` may be empty, contain any number of rules in any order, and include duplicates.

**B1 — the value is a left fold over the rule list.**
```
p₀ = duck.price
pᵢ = max(0, pᵢ₋₁ − discount(rᵢ, pᵢ₋₁))     for i = 1 … n
priceFor(duck, rules) = pₙ
```

**B2 — `Percentage(percent)`** discounts `percent` percent of the price it is applied to, rounded down:
```
discount(Percentage(percent), p) = floor(p × percent / 100)
```

**B3 — `AmountOff(amount)`** discounts exactly `amount`:
```
discount(AmountOff(amount), p) = amount
```

**B4 — `BigSpenderBonus(threshold, amount)`** discounts `amount` if the price is at least `threshold`:
```
discount(BigSpenderBonus(threshold, amount), p) = if (p >= threshold) amount else 0
```

**B5 — the threshold test is inclusive.**

**B6 — the bonus is tested against the price the rule is applied to.**

**B7 — the bonus's own fields are used for threshold and amount.**

**B8 — the bonus does not require any other rule to be present.**

**B9 — the price is clamped at 0 and never goes negative.**

**B10 — every rule in the list is applied once per list element.**

**B11 — list order is significant.**

**B12 — a rule affects the result only through its kind and field values.**

**B13 — `duck` affects the result only through `price`.**

**B14 — `priceFor` is pure.**

**B15 — no exceptions for in-domain input.**

**B16 — monotonicity:** Appending a rule to `rules` never increases the result.

## 3. Edge cases

**E1 — empty rule list:** `priceFor(duck, emptyList()) == duck.price`.

**E2 — shelf price 0:** `duck.price == 0` gives `0` for every list of in-domain rules.

**E3 — zero-valued discounts:** `Percentage(0)`, `AmountOff(0)` and `BigSpenderBonus(t, 0)` leave the price unchanged.

**E4 — `Percentage(100)` takes the price to 0.**

**E5 — `percent` above 100 is legal and clamps:**

**E6 — `AmountOff` larger than the price gives 0.**

**E7 — rounding down always applies.**

**E8 — a discount that rounds away to nothing has no effect.**

**E9 — threshold boundary:** `price = 100, [BigSpenderBonus(100, 20)] → 80`; `price = 99, [BigSpenderBonus(100, 20)] → 99`.

**E10 — `threshold == 0` fires for every price.**

**E11 — `threshold == Int.MAX_VALUE` fires only for `price == Int.MAX_VALUE`.**

**E12 — several bonuses are tested independently.**

**E13 — many rules, price already at 0, stay 0.**

**E14 — the largest shelf price:** `price = Int.MAX_VALUE (2147483647), [Percentage(10)] → 1932735283`.

**E15 — `Percentage(Int.MAX_VALUE)` yields 0 for any price.**

**E16 — duck fields that are not the price do not affect the result.**

## 4. Deliberately not specified

**N1 — behaviour outside the input domain is left free.**

**N2 — how the value is computed is unspecified.**

**N3 — clamping at 0 can happen after each rule or only on the final result.**

**N4 — early exit if price reaches 0 is allowed.**

**N5 — performance and complexity are not guaranteed.**

**N6 — packaging of the function is left open.**

## 5. Open questions

**Q1 — promotions compound, applying to the running price.**

**Q2 — big-spender bonus applies to the current price.**

**Q3 — rounding down always applies to the discount.**

**Q4 — rules are assumed validated upstream.**

**Q5 — `percent > 100` is legal and clamps to a free duck.**

**Q6 — no floor above 0 or cap on total discount.**

**Q7 — duplicates in the rule list are included.**

**Q8 — `priceFor`'s result does not include tax, rounding, or other factors.**
