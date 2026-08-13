# Specification — `priceFor`

## 1. What it does

`priceFor` returns the final price of a duck after applying all active promotions to its shelf price.

## 2. Behaviour

- **Percentage off**: A percentage rule reduces the price by `percent` percent of the price it is applied to, rounding down.
- **Amount off**: An amount off rule subtracts a fixed amount from the price.
- **Big-spender bonus**: If the duck's shelf price is greater than or equal to the threshold, an additional discount of `amount` is applied.

## 3. Edge cases

- **Empty list of rules**: The price remains unchanged.
- **Zero discount percentage or amount**: No discount is applied.
- **Threshold for big-spender bonus not met**: No additional discount is applied.
- **Negative shelf price**: The function should handle negative prices gracefully, possibly by returning an error or a default value.

## 4. Deliberately not specified

- **Handling of overlapping rules**: If multiple rules apply to the same duck, the order in which they are applied is not specified. Implementations may choose any valid order.
- **Error handling for invalid inputs**: The function should handle cases where the input `duck` or `rules` are null or contain invalid data gracefully.

## 5. Open questions

- What should be done if a rule has an invalid percentage or amount (e.g., negative values)?
- How should the function behave when the shelf price is zero?
- Should there be a maximum limit on the discount percentage or amount?
