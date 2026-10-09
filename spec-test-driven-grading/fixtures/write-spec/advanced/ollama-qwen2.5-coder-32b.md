# Specification — `priceFor` (composing promotions)

## 1. What it does

The function `priceFor` calculates the final price of a duck after applying a given promotion rule, taking into account any nested or combined rules.

## 2. Behaviour

- A `Percentage` rule reduces the price by `percent` percent of the original price, rounding down.
- An `AmountOff` rule reduces the price by a fixed `amount`.
- A `BigSpenderBonus` rule applies an additional `amount` discount if the original price is greater than or equal to `threshold`.
- A `Then` rule applies the `first` promotion and then applies the `second` promotion on the resulting price.
- A `BestOf` rule evaluates all options in the list and returns the lowest final price after applying each option.
- An `OnlyIf` rule applies the nested `rule` only if the `duck` satisfies the `condition`; otherwise, it leaves the price unchanged.

## 3. Edge cases

- **Empty `BestOf`**: If the `options` list is empty, the price remains unchanged.
- **Zero percentage**: A `Percentage(0)` rule does not change the price.
- **Zero amount off**: An `AmountOff(0)` rule does not change the price.
- **Threshold not met for bonus**: A `BigSpenderBonus` with a `threshold` higher than the original price does not apply the bonus.
- **No qualifying condition**: An `OnlyIf` rule with a condition that never qualifies leaves the price unchanged.

## 4. Deliberately not specified

- The order of evaluation in a `BestOf` rule when multiple options result in the same lowest price is left unspecified, as it does not affect the final output.
- Whether rounding down in a `Percentage` rule should use floor division or integer division is assumed to be consistent with Kotlin's behavior.

## 5. Open questions

- **Handling of negative prices**: The business has not specified how to handle cases where a promotion results in a negative price. For now, we assume that the price cannot go below zero.
- **Combining `OnlyIf` rules**: It is unclear whether nested `OnlyIf` rules should short-circuit evaluation if an earlier condition fails. We assume they do not and evaluate all conditions.

## 6. Laws

1. **Identity Law for `Then`**:
   - Identity: `Then(rule, Percentage(0)) == rule`
   - Guaranteed: Yes
   - Failure case: None (since applying a zero percent discount does not change the price)

2. **Commutative Law for `BestOf` with two options**:
   - Identity: `BestOf(listOf(option1, option2)) == BestOf(listOf(option2, option1))`
   - Guaranteed: Yes
   - Failure case: None (since order does not affect the minimum price)

3. **Idempotent Law for `AmountOff`**:
   - Identity: `Then(AmountOff(amount), AmountOff(0)) == AmountOff(amount)`
   - Guaranteed: Yes
   - Failure case: None (since adding zero to an amount off does not change it)

4. **Distributive Law for nested `OnlyIf`**:
   - Identity: `OnlyIf(condition1, OnlyIf(condition2, rule)) == OnlyIf({ duck -> condition1(duck) && condition2(duck) }, rule)`
   - Guaranteed: No (since the business has not specified whether to short-circuit)
   - Failure case: A duck that satisfies `condition1` but not `condition2`

5. **Neutral Element for `BestOf`**:
   - Identity: `BestOf(listOf(rule, AmountOff(0))) == rule`
   - Guaranteed: Yes
   - Failure case: None (since adding an option that does not change the price does not affect the result)
