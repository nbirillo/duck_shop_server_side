# Specification — `priceFor`

> Copy this file to `SPEC.md` and fill it in. Keep the headings: they are what gets checked for
> completeness, and each one holds a different kind of statement.

## 1. What it does

The function `priceFor` takes a `Duck` object and a list of `DiscountRule` objects as input, and returns an integer representing the final price of the duck in a shop with the given promotions applied.

## 2. Behaviour

- **Percentage off**: For example, 10% off.
  - If the duck's price is $100, applying a 10% discount results in $90.
  - If the duck's price is $50, applying a 10% discount results in $45.

- **Amount off**: For example, 5 off.
  - If the duck's price is $100, applying a 5 off discount results in $95.
  - If the duck's price is $50, applying a 5 off discount results in $45.

- **Big-spender bonus**: A duck priced from 100 gets a further 20 off.
  - If the duck's price is $100, applying a big-spender bonus results in $80.
  - If the duck's price is $50, applying a big-spender bonus results in $30.

## 3. Edge cases

- **Empty list of promotions**: If no promotions are applied, the price remains unchanged.
  - `priceFor(Duck("Duck", 100), emptyList()) == 100`

- **Zero price**: If the duck's price is zero, applying any promotion results in zero.
  - `priceFor(Duck("Duck", 0), listOf(Percentage(10))) == 0`

- **Boundary values**: For prices close to the maximum value of an integer (e.g., $2^31 - 1), the discount should not cause overflow.
  - `priceFor(Duck("Duck", Int.MAX_VALUE), listOf(AmountOff(1))) == Int.MAX_VALUE - 1`

- **Threshold for big-spender bonus**: If the duck's price is below the threshold, no bonus is applied.
  - `priceFor(Duck("Duck", 90), listOf(BigSpenderBonus(100, 20))) == 90`

## 4. Deliberately not specified

- **Order of promotions**: The order in which promotions are applied should be respected.
  - `priceFor(Duck("Duck", 100), listOf(AmountOff(5), Percentage(10))) == 95`
- **Complexity**: The function should handle complex scenarios efficiently, such as applying multiple discounts in sequence.

## 5. Open questions

- **Implementation details**: How are the discount rules applied? Should they be applied sequentially or simultaneously?
- **Performance**: What is the time complexity of `priceFor`? Can it be optimized for large lists of promotions?

The difference from section 4 matters. Section 4 is "any answer is fine". This section is "there is a right answer and I do not know it".
