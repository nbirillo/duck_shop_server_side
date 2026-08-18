# Specification — `priceFor`

> Copy this file to `SPEC.md` and fill it in. Keep the headings: they are what gets checked for completeness, and each one holds a different kind of statement.

## 1. What it does

`priceFor` returns an offer that is the cheapest price at which a customer can buy a duck from any shop in the chain.

## 2. Behaviour

- The total cost of buying a duck from all shops is calculated by summing up the prices for each shop.
- Discounts are applied to each shop's price according to its rules, but only if the discount rule applies to that shop.
- If a shop does not have any applicable discounts, its price is used as is.

## 3. Edge cases

- An empty chain: `priceFor` returns an offer with no shops and a price of 0.
- A single shop in the chain: `priceFor` returns an offer for that shop's price.
- A customer who cannot buy any duck from any shop (e.g., because they don't meet the admission policy): `priceFor` returns an offer with no shops and a price of 0.

## 4. Deliberately not specified

- How to handle cases where two or more shops have the same cheapest price for a duck: this is left open.
- Whether to round up or down when applying discounts: this is left open.

## 5. Open questions

- What happens if there are multiple offers with the same lowest price? Should we return all of them, or just one?
- How should we handle cases where a shop's admission policy changes after the chain has been established?

Note that I've tried to be as precise and checkable as possible in my specification, while also leaving some room for interpretation.
