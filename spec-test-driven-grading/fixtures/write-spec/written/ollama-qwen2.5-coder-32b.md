# Specification — `priceFor`

> Copy this file to `SPEC.md` and fill it in. Keep the headings: they are what gets checked for
> completeness, and each one holds a different kind of statement.

## 1. What it does

The function `priceFor` calculates the final price of a duck after applying all applicable discount rules from a given list to its original shelf price.

## 2. Behaviour

- A percentage rule reduces the price by `percent` percent of the original price, rounding down to the nearest whole unit.
- An amount off rule directly subtracts the specified `amount` from the original price, with the result not going below zero.
- A big-spender bonus rule applies an additional discount of `amount` if the original price is greater than or equal to `threshold`, with the result not going below zero.
- Discount rules are applied in the order they appear in the list.
- If multiple percentage off rules apply, each is applied sequentially to the current price after previous discounts.
- If multiple amount off rules apply, each is applied sequentially to the current price after previous discounts.
- If a big-spender bonus rule applies, it is applied last after all other discount rules have been processed.

## 3. Edge cases

- **Empty list of rules**: The final price is the original shelf price of the duck.
- **Zero percentage**: The final price remains unchanged from the original shelf price.
- **Percentage greater than or equal to 100%**: The final price becomes zero.
- **Amount off greater than or equal to the original price**: The final price becomes zero.
- **Threshold not met for big-spender bonus**: The final price is determined by other rules, if any.
- **Multiple percentage off rules**: Each rule is applied sequentially, reducing the price progressively.
- **Multiple amount off rules**: Each rule is applied sequentially, reducing the price progressively.
- **Combination of all rule types**: Rules are applied in sequence as specified, with big-spender bonus applied last.

## 4. Deliberately not specified

- The order of discount rules if they are of the same type (e.g., multiple percentage off rules) is assumed to be the order in which they appear in the list.
- Whether additional types of discount rules might be added in the future is left open, as this would require changes to the `DiscountRule` interface and its implementations.

## 5. Open questions

- **Interaction between different rule types**: It is unclear whether there are any specific interaction rules (e.g., does a big-spender bonus apply before or after other discounts if it meets the threshold?).
- **Handling of negative prices**: The business did not specify what should happen if discount calculations result in a negative price. Assuming that the final price cannot be negative and will be clamped to zero.
- **Decimal handling during percentage calculation**: It is assumed that any fractional part resulting from percentage calculations is discarded (rounded down).
