# Specification — `priceFor`

> Copy this file to `SPEC.md` and fill it in. Keep the headings: they are what gets checked for completeness, and each one holds a different kind of statement.

## 1. What it does

The function `priceFor` takes a `Duck` object and a list of `DiscountRule` objects, and returns an `Offer?`. The `Offer` contains the name of the shop where the duck can be bought most cheaply and the price of the duck in that shop.

## 2. Behaviour

The rules of the calculation are as follows:

1. If no shop in the chain sells the duck, return `null`.
2. For each shop in the chain:
   - If the shop admits the duck, calculate the price after applying all discounts.
   - If the calculated price is less than or equal to the price of the duck in the shop, return that shop and price.
3. If no shop satisfies the above conditions, return `null`.

## 3. Edge cases

1. **Empty list of shops**: Return `null`.
2. **Duck with zero price**: Return `null`.
3. **Duck with high price**: Return the first shop in the chain that admits the duck.
4. **Duck with no discounts**: Return the first shop in the chain that admits the duck.

## 4. Deliberately not specified

The function does not specify whether it should return the cheapest shop or the first shop in the chain if multiple shops satisfy the conditions. This is left to the implementation.

## 5. Open questions

1. **What happens if a shop has no discounts?**: Should it be considered as admitting the duck?
2. **What happens if a shop does not admit the duck?**: Should it be considered as not admitting the duck?

The difference from section 4 matters. Section 4 is "any answer is fine". This section is "there is a right answer and I do not know it".
