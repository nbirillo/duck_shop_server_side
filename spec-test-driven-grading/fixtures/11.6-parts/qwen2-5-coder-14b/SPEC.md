# Specification — `bestOffer`

> Copy this file to `SPEC.md` and fill it in. Keep the headings: they are what gets checked for
> completeness, and each one holds a different kind of statement.

## 1. What it does

The function `bestOffer` returns the cheapest offer available for a given duck within a franchise. An offer includes the shop where the duck can be bought and the price at which it is sold. If no shop in the franchise will sell the duck, the function returns `null`.

## 2. Behaviour

1. **Admission Check**: A shop only considers selling a duck if it admits the duck according to its `admissionPolicy`.
2. **Promotion Application**: The price of the duck is calculated by applying all applicable promotions from both the franchise and the shop.
3. **Price Calculation**:
   - For a `Percentage` discount rule, reduce the price by `percent` percent of the original price, rounding down.
   - For an `AmountOff` discount rule, subtract the specified amount from the original price, but not below zero.
   - For a `BigSpenderBonus` discount rule, if the total price after applying other discounts is greater than or equal to the threshold, add the bonus amount to the price.
4. **Cheapest Offer Selection**: Among all shops that admit and sell the duck, select the one with the lowest final price. If there are multiple shops with the same lowest price, any of them can be returned.

## 3. Edge Cases

1. **No Shops Admit the Duck**:
   - Input: A franchise with no shops or shops that do not admit the duck.
   - Output: `null`
2. **Zero Price After Discounts**:
   - Input: A duck with a price of zero and any discount rules.
   - Output: An offer with a price of zero.
3. **Multiple Shops with Same Cheapest Price**:
   - Input: Multiple shops that admit the duck and have the same lowest price after discounts.
   - Output: Any one of these shops can be returned in the offer.

## 4. Deliberately not specified

1. **Order of Promotion Application**: The order in which promotions are applied is left open. Implementations may choose to apply them in any sequence without affecting correctness.
2. **Handling of Negative Percentages in Percentage Rule**:
   - If a `Percentage` rule has a negative percentage, it should be treated as no discount (i.e., the price remains unchanged).
3. **Handling of Zero Amount in AmountOff Rule**:
   - If an `AmountOff` rule specifies zero amount to subtract, the price remains unchanged.

## 5. Open questions

1. **Interaction Between Different Types of Discounts**:
   - What is the expected behavior when multiple types of discounts (e.g., a percentage discount followed by an amount off) are applied? Should they be applied sequentially or in some other order?
2. **Handling of Non-Standard Duck Attributes**:
   - How should non-standard attributes of a duck (e.g., `hasKotlinAttribute`, `accessories`) affect admission policies and promotions? Are there any implicit rules based on these attributes that need to be considered?
