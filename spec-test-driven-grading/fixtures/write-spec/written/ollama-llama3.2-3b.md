# Specification — `priceFor`

> Copy this file to `SPEC.md` and fill it in. Keep the headings: they are what gets checked for completeness, and each one holds a different kind of statement.

## 1. What it does

The function `priceFor` returns the price that a duck would pay after applying all active discount rules.

## 2. Behaviour

- A percentage rule reduces the price by `percent` percent of the price it is applied to, rounding down.
- An amount off rule subtracts `amount` from the price.
- The Big Spender Bonus rule applies only if the duck's price is at least `threshold`, and then reduces the price by `amount`.

## 3. Edge cases

- If the input list of rules is empty, the function should return the original price of the duck.
- If the input list of rules contains a percentage rule with `percent` equal to 0, the function should return the original price of the duck.
- If the input list of rules contains an amount off rule with `amount` equal to 0, the function should subtract 0 from the price and return the result.
- If the input list of rules contains a Big Spender Bonus rule with `threshold` less than or equal to 0, it is deliberately not specified how this rule behaves.

## 4. Deliberately not specified

- The order in which discount rules are applied is deliberately not specified.
- Any duck that has a price below the threshold for the Big Spender Bonus rule is deliberately not specified how its price should be handled.

## 5. Open questions

- What happens if the input list of rules contains multiple percentage rules with different `percent` values? Should they be added together, or applied sequentially?
- How does the function handle a duck that has no accessories?
