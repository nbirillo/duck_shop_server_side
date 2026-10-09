# Specification — `priceFor`

> Copy this file to `SPEC.md` and fill it in. Keep the headings: they are what gets checked for
> completeness, and each one holds a different kind of statement.

## 1. What it does

The `priceFor` function calculates the final price of a duck after applying a list of discount rules to its shelf price. The function takes into account various types of discounts such as percentage off, amount off, and big-spender bonus.

## 2. Behaviour

- **Percentage Off**: A percentage rule reduces the price by `percent` percent of the price it is applied to, rounding down.
  - Example: If a duck's price is 100 and there is a 10% discount, the new price will be 90 (100 - (100 * 10 / 100)).

- **Amount Off**: An amount off rule reduces the price by a fixed `amount`.
  - Example: If a duck's price is 100 and there is a $5 discount, the new price will be 95 (100 - 5).

- **Big Spender Bonus**: A big-spender bonus rule applies an additional discount if the duck's price is above a certain `threshold`.
  - Example: If a duck's price is 120 and there is a big-spender bonus with a threshold of 100 and an amount of 20, the new price will be 100 (120 - 20).

- **Order of Application**: The discount rules are applied in the order they appear in the `rules` list. Each rule is applied to the current price after all previous rules have been applied.

## 3. Edge cases

- **Empty Rules List**: If there are no discount rules, the duck's original price should be returned.
  - Example: If a duck's price is 100 and the rules list is empty, the final price will be 100.

- **Zero Price Duck**: If a duck's price is zero, applying any discount rule should result in a price of zero.
  - Example: If a duck's price is 0 and there is a 10% discount, the final price will be 0.

- **Negative Price Duck**: If a duck's price is negative, applying any discount rule should result in a non-positive price.
  - Example: If a duck's price is -50 and there is a $10 discount, the final price will be -60.

- **Threshold for Big Spender Bonus**: If a duck's price is exactly equal to the threshold for a big-spender bonus, the bonus should not be applied.
  - Example: If a duck's price is 100 and there is a big-spender bonus with a threshold of 100 and an amount of 20, the final price will be 100.

- **Combination of Discounts**: Multiple discount rules can be applied to a single duck. The order of application should follow the list provided.
  - Example: If a duck's price is 150, there is a 10% discount followed by a $20 discount, and then a big-spender bonus with a threshold of 100 and an amount of 30, the final price will be 98 (150 - (150 * 10 / 100) - 20 - 30).

## 4. Deliberately not specified

- **Order of Discount Application**: While the order in which rules are applied is specified to follow the list, it is not specified whether discounts should be applied sequentially or if there should be any interaction between different types of discounts (e.g., applying a percentage discount followed by an amount discount versus vice versa).

## 5. Open questions

- **Handling of Non-Positive Percentages**: What should happen if a percentage rule has a non-positive percentage value? Should it be ignored, or should it result in no change to the price?
  - Assumption: A non-positive percentage should be ignored.

- **Handling of Non-Positive Amounts Off**: What should happen if an amount off rule has a non-positive amount value? Should it be ignored, or should it result in no change to the price?
  - Assumption: A non-positive amount should be ignored.
