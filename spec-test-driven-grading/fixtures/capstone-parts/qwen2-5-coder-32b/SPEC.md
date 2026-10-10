# Specification — `bestOffer`

> Copy this file to `SPEC.md` and fill it in. Keep the headings: they are what gets checked for
> completeness, and each one holds a different kind of statement.

## 1. What it does

The function `bestOffer` returns an `Offer` representing the cheapest shop within a given franchise where a customer can buy a specified duck, along with the price after applying any applicable promotions. If no shop in the franchise will sell the duck, it returns `null`.

## 2. Behaviour

- The function checks each shop in the franchise to determine if it admits the duck using the shop's `admissionPolicy`.
- For shops that admit the duck, the function calculates the final price by applying all applicable promotions from both the franchise and the individual shop.
- A percentage rule reduces the price by `percent` percent of the original price, rounding down.
- An amount off rule directly subtracts the specified `amount` from the original price.
- A big spender bonus rule applies a fixed `amount` discount if the original price exceeds the specified `threshold`.
- The function compares the final prices across all admitting shops and returns the offer with the lowest price.
- If multiple shops have the same lowest price, any one of them may be returned.

## 3. Edge cases

- **No shops admit the duck**: Returns `null`.
- **Empty franchise promotions list**: Only shop-specific promotions are considered.
- **Empty shop promotions list**: Only franchise-wide promotions are considered.
- **Zero percent discount**: The original price is unchanged.
- **Amount off greater than or equal to the original price**: The final price is 0.
- **Big spender bonus threshold not met**: No discount is applied.
- **Multiple shops with identical lowest prices**: Any one of them may be returned.

## 4. Deliberately not specified

- The order in which shops are considered when multiple have the same lowest price: any shop can be returned.
- Whether additional promotion types beyond those defined should be supported: only the given `DiscountRule` kinds are considered.
- Handling of invalid or malformed input (e.g., negative prices, percentages): assumed to be valid as per problem constraints.

## 5. Open questions

- **Tie-breaking strategy for shops with identical lowest prices**: Currently, any shop can be returned, but a specific tie-breaking rule (e.g., alphabetical order by shop name) could be specified.
- **Handling of overlapping promotions**: The interaction between different promotion types and their application order is not defined. Assuming they are applied independently in the given order.
- **Currency unit**: While prices are whole units, the exact currency is unspecified and assumed to be consistent across all shops and franchises.
