# Specification — `priceFor`

## 1. What it does

`priceFor` returns the price of a duck in a shop that offers the best deal for that duck, considering all active promotions. If no shop is willing to sell the duck, it returns null.

## 2. Behaviour

- The function calculates the price of a duck after applying all active promotions.
- It considers promotions from both the franchise and individual shops.
- The promotion with the highest discount is applied first.
- If multiple promotions result in the same discounted price, the shop offering that promotion is chosen.
- If no shop offers the duck at any price, the function returns null.

## 3. Edge cases

- **Empty Franchise**: If the franchise has no shops or promotions, return null.
- **Zero Price Duck**: If the duck's price is zero, it should be considered as not being sold by any shop.
- **No Promotions**: If there are no promotions, the original price of the duck should be returned.
- **Boundary Values**: The function should handle edge cases such as the maximum and minimum values for prices and discounts.

## 4. Deliberately not specified

- **Handling of Admission Policy**: The behavior when a shop's admission policy does not allow the duck to be sold is left open. It could either return null or consider the shop as not offering the duck.
- **Order of Promotion Application**: While the promotion with the highest discount is applied first, the order in which promotions are considered within the same discount level is not specified.

## 5. Open questions

- What should happen if a shop's admission policy does not allow the duck to be sold? Should it return null or consider the shop as not offering the duck?
- How should the function handle multiple promotions that result in the same discounted price? Should it choose the shop offering that promotion, or is there another criterion to decide?

---

This specification aims to provide a clear and precise contract for the `priceFor` function while leaving some decisions open to ensure flexibility and adaptability.
