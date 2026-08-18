# Specification — `bestOffer`

## 1. What it does

`bestOffer(duck, franchise)` finds the cheapest shop in the chain that is willing to sell the given duck, and returns that shop together with the price the customer pays there. If no shop in the chain will sell that duck, it returns `null`. The function `priceFor` also returns an offer that is the cheapest price at which a customer can buy a duck from any shop in the chain. The function `bestOffer` takes a `Duck` object and a list of `DiscountRule` objects, and returns an `Offer?`. The `Offer` contains the name of the shop where the duck can be bought most cheaply and the price of the duck in that shop. `priceFor` returns the price of a duck in a shop that offers the best deal for that duck, considering all active promotions. If no shop is willing to sell the duck, it returns null.

## 2. Behaviour

### 2.0 Vocabulary

* **base price** — `duck.price`.
* **A shop admits the duck** — `shop.admits(duck)` is `true`. `Shop.admits(duck)` is exactly `shop.admissionPolicy.admits(duck)`; a shop's `promotions` and `name` play no part in admission.
* **The chain admits the duck** — `franchise.admissionPolicy.admits(duck)` is `true`.
* **Applying a rule** — evaluating a `DiscountRule` as a function of the duck and an incoming price, producing an outgoing price (see B7).

### 2.1 Which shops can sell the duck

**B1 (eligibility).** A shop `s` in `franchise.shops` is *eligible* if and only if **both** the chain and the shop admit the duck: `franchise.admissionPolicy.admits(duck) && s.admits(duck)`. A shop that admits the duck inside a chain that does not is **not** eligible; a chain that admits the duck does **not** override a shop's refusal.

**B2 (irrelevant inputs).** `franchise.name`, `Shop.name`, `franchise.promotions` and `s.promotions` have no influence on eligibility.

**B3 (closed world).** Only elements of `franchise.shops` are ever considered, and only such an element can appear in the returned `Offer`.

### 2.2 The price at one shop

**B4 (single base).** The computation for every eligible shop starts from the same value, `max(0, duck.price)` (see §3.8 for a negative base). There is no per-shop base price: the surface gives `Shop` no price of its own.

**B5 (which promotions apply).** For shop `s`, exactly the rules in `s.promotions` and the rules in `franchise.promotions` are applied — each **once per occurrence in those lists**. Consequences:

* the chain's promotions apply at every eligible shop, including shops whose own `promotions` is empty;
* a rule that occurs in both lists is applied **twice**;
* a rule that occurs twice in one list is applied **twice**;
* nothing is de-duplicated — not by equality, not by object identity.

**B6 (order of application).** The shop's own promotions are applied first, in list order (index `0` first), and then the chain's promotions, in list order. Writing `s.promotions = [s₁ … s_m]` and `franchise.promotions = [f₁ … f_n]`, the price at `s` is

```
p₀  = max(0, duck.price)
p_i = max(0, apply(sᵢ, duck, p_{i-1}))          for i = 1 … m
q_j = max(0, apply(f_j, duck, q_{j-1}))          for j = 1 … n,  with q₀ = p_m
price(s) = q_n
```

This order is observable: a 10-off rule and a 10%-off rule give different results depending on which runs first, so the order above is part of the contract, not an implementation detail.

**B7 (applying one rule).** A rule is applied as a pure function of the pair `(duck, incoming price)` returning an outgoing price, with whatever per-rule semantics `:core` already gives that rule kind (see B15). Applying a rule must not modify the duck, the rule, the shop or the franchise.

**B8 (never negative).** Every price is clamped at zero, after **each** individual rule application as written in B6. Therefore every intermediate price and the final price is `>= 0`, and a discount can never turn into money owed to the customer. `0` is a legal price and is reported as `0`.

**B9 (exact arithmetic).** Intermediate arithmetic must not wrap around. A percentage rule applied to a very large price must be computed exactly (e.g. in a wider type) rather than overflowing `Int`. Any exact result above `Int.MAX_VALUE` is reported as `Int.MAX_VALUE` (see §3.10); any exact result below `0` is reported as `0` (B8).

**B10 (no rule is discarded).** The price at a shop is whatever B6 computes, even if a rule leaves the price unchanged or raises it. A shop is not made ineligible, and a rule is not skipped, because it fails to lower the price.

### 2.3 Choosing the winner

**B11 (cheapest wins).** The result is the eligible shop whose `price(s)` is smallest.

**B12 (ties).** If several eligible shops share the smallest price, the one with the **lowest index in `franchise.shops`** is returned. The choice is therefore fully determined by the input, and depends on the order of `franchise.shops` (see §5, Q7).

**B13 (contents of the result).** `Offer.shop` equals (`==`) the winning element of `franchise.shops`, and `Offer.price` is exactly `price(shop)` as computed by B4–B9 for that shop — not a recomputation under different rules, and not the base price.

**B14 (nothing to offer).** If no shop is eligible, the result is `null`. This is the only reason to return `null`: whenever at least one shop is eligible, a non-`null` `Offer` is returned.

### 2.4 Determinism and scope of the change

**B15 (determinism, no mutation).** `bestOffer` mutates none of its arguments and keeps no state between calls. Two calls with equal arguments return equal results, provided the caller's policies and rules are themselves pure and deterministic (if they are not, see §4).

**B16 (what is inherited and not restated here).** The four leaves and three combinators of `AdmissionPolicy`, and the six `DiscountRule` kinds, keep exactly the semantics `:core` already gives them. This specification depends on those types only through two contracts:

* an `AdmissionPolicy` is a total, pure predicate on a `Duck`;
* a `DiscountRule` is a total, pure, `Int`-valued function of a `Duck` and an incoming price.

**B17 (no existing behaviour changes).** `Shop.promotions` is a new property with default `emptyList()`. A `Shop` constructed without it behaves exactly as a `Shop` did before, and every existing call that does not mention promotions computes what it computed before.

- The function checks each shop in the franchise to determine if it admits the duck using the shop's `admissionPolicy`.
- For shops that admit the duck, the function calculates the final price by applying all applicable promotions from both the franchise and the individual shop.
- A percentage rule reduces the price by `percent` percent of the original price, rounding down.
- An amount off rule directly subtracts the specified `amount` from the original price.
- A big spender bonus rule applies a fixed `amount` discount if the original price exceeds the specified `threshold`.
- The function compares the final prices across all admitting shops and returns the offer with the lowest price.
- If multiple shops have the same lowest price, any one of them may be returned.

## 3. Edge cases

1. **`franchise.shops` is empty** → `null`. No shop is eligible.
2. **No shop admits the duck** → `null`, whatever the chain's policy says.
3. **The chain does not admit the duck, some shops do** → `null` (B1). The shops are not consulted for the answer.
4. **The chain admits everything, no shop does** → `null`.
5. **Both promotion lists empty, several shops eligible** → every eligible shop prices at `max(0, duck.price)`; the first eligible shop in `franchise.shops` is returned (B12).
6. **`duck.price == 0`** → `0` at every eligible shop, unless a rule raises the price; the cheapest is then still `0` if any eligible shop leaves it at `0`.
7. **`duck.price == Int.MAX_VALUE`** → computed exactly, no wrap-around (B9). E.g. a 50%-off rule yields `1073741823` under floor rounding, not a negative number.
8. **`duck.price < 0`** (illegal as a price, but representable) → the base is taken as `0` before any rule is applied (B4), so an eligible shop offers `0`, never a negative price.
9. **Rules that would drive the price below zero** (a fixed amount larger than the price, or several such rules) → `0`, at that step and in the result (B8).
10. **Rules that would drive the price above `Int.MAX_VALUE`** → `Int.MAX_VALUE`.
11. **Two different shops with the same `name`** → they are two shops; the cheaper wins, and on a tie the earlier index wins. Names are never used to merge shops.
12. **The same `Shop` value listed twice in `franchise.shops`** → the earlier index wins; the returned price is the same either way.
13. **Two shops equal except for `promotions`** → distinct shops; the one whose promotions produce the lower price wins.
14. **Exactly one shop, eligible** → that shop, with its computed price, even if that price is higher than `duck.price` because a rule raised it (B10).
15. **A shop whose `admissionPolicy` is the leaf that admits nothing** → never eligible, no matter what the chain admits or how cheap it would have been.
16. **A shop that admits the duck but whose promotions raise the price above a plain shop's** → the plain shop wins; being the source of a promotion is not itself an advantage.
17. **Chain promotions non-empty, shop promotions empty** → the chain's rules alone apply, in list order.
18. **Shop promotions non-empty, chain promotions empty** → the shop's rules alone apply, in list order.
19. **The same rule in both lists** → applied twice. Two 10%-off applications on `100` give `81` (under floor rounding), not `80` and not `90`.
20. **Two percentage rules** → applied one after the other, i.e. multiplicatively: 10% then 20% off `100` gives `72` (under floor rounding), never `70`.
21. **All eligible shops price at `0`** → the first eligible shop in list order (B12).
22. **A duck with an empty `accessories` list, a blank `name`, or duplicate accessories** → no special treatment here; those fields matter only inside `admits` and inside rules that look at them.
23. **Duplicate entries inside one promotions list** → each occurrence is applied (B5).
24. **Many shops, many of them ineligible** → the ineligible ones are simply absent from the comparison; they can never win and never make the result `null` when an eligible shop exists.

- An empty chain: `priceFor` returns an offer with no shops and a price of 0.
- A single shop in the chain: `priceFor` returns an offer for that shop's price.
- A customer who cannot buy any duck from any shop (e.g., because they don't meet the admission policy): `priceFor` returns an offer with no shops and a price of 0.

- **Empty list of shops**: Return `null`.
- **Duck with zero price**: Return `null`.
- **Duck with high price**: Return the first shop in the chain that admits the duck.
- **Duck with no discounts**: Return the first shop in the chain that admits the duck.

- **Empty Franchise**: If the franchise has no shops or promotions, return null.
- **Zero Price Duck**: If the duck's price is zero, it should be considered as not being sold by any shop.
- **No Promotions**: If there are no promotions, the original price of the duck should be returned.
- **Boundary Values**: The function should handle edge cases such as the maximum and minimum values for prices and discounts.

## 4. Deliberately not specified

* **How many times a policy or rule is evaluated, and in what order across shops.** Whether the search short-circuits, evaluates all shops, evaluates lazily, or caches results is free — only the order *within* a price computation (B6) and the tie-break (B12) are fixed. So policies and rules must not be used for side effects, and a test must not count invocations.
* **Referential identity of `Offer.shop`.** It must be `==` to the winning element of `franchise.shops`; whether it is the same instance or an equal copy is free.
* **Behaviour when a caller-supplied `AdmissionPolicy` or `DiscountRule` throws, is nondeterministic, or reads mutable state.** B15 assumes purity. Outside that assumption the result is undefined: an implementation may propagate the exception, and (per the bullet above) may not have called the offending policy or rule at all.
* **Time and space complexity, allocation, and behaviour under concurrent calls.** No bound is promised beyond returning the specified answer.
* **Internal structure.** Whether the price-at-one-shop calculation is exposed as a public helper, and under what name, is free; only `bestOffer`'s signature and result are fixed.
* **Any exception type.** No input is specified to cause a throw, so no exception type is pinned.
* **Anything beyond one duck and one flat chain** — nested franchises, several chains, quantities, currency or human-readable formatting. Out of scope, and an implementation may not support it.
* **The semantics of the individual admission leaves, combinators and discount rule kinds.** They belong to `:core` (B16) and are not this specification's to freeze or restate.

- The order in which shops are considered when multiple have the same lowest price: any shop can be returned.
- Whether additional promotion types beyond those defined should be supported: only the given `DiscountRule` kinds are considered.
- Handling of invalid or malformed input (e.g., negative prices, percentages): assumed to be valid as per problem constraints.

- **Handling of Admission Policy**: The behavior when a shop's admission policy does not allow the duck to be sold is left open. It could either return null or consider the shop as not offering the duck.
- **Order of Promotion Application**: While the promotion with the highest discount is applied first, the order in which promotions are considered within the same discount level is not specified.

## 5. Open questions

**Q1. Do the chain's promotions stack with a shop's, or replace them — and do all promotions in a list apply, or only the best single one?** "The chain has rules of its own on top of that" reads as stacking, but a chain could equally mean "our promotion replaces the shop's" or "the customer gets the single best discount".
*Assumed:* every rule in both lists applies, sequentially (B5, B6).

**Q2. Which side goes first — the shop's rules or the chain's?** This changes the number whenever a percentage rule meets a fixed-amount rule.
*Assumed:* the shop's own rules first, then the chain's "on top" (B6).

**Q3. Is the chain's admission policy a filter on top of each shop's, or can the chain force a sale the shop would refuse (or the other way round)?**
*Assumed:* both must admit — plain conjunction (B1).

**Q4. Rounding for percentage rules: floor, half-up or ceiling — and does rounding happen after each rule or once at the end?** With whole-unit prices this decides many concrete numbers (see §3.19, §3.20, where floor is used only as an illustration).
*Assumed:* whatever `:core` already does, applied once per rule, since rules are applied one after another to a whole-unit price (B6, B7). I could not read `:core` from this folder to confirm it.

**Q5. The exact inventory and semantics of the four admission leaves, the three combinators and the six discount rule kinds.** The brief names their counts but not their meanings, and nothing in this folder defines them. This is the largest thing I could not verify.
*Assumed:* only the two contracts in B16. Everything in §2 is written so that it holds whatever those kinds turn out to be.

**Q6. Are there chain-level or shop-level constraints beyond admission and promotions** — stock ("this shop has no such duck"), opening hours, a chain-wide minimum price, a shop being suspended?
*Assumed:* no. A shop that admits the duck can sell it, always, in unlimited quantity.

**Q7. Is there a business preference for breaking a price tie** — nearest shop, flagship store, the one giving the larger discount?
*Assumed:* the first shop in `franchise.shops` (B12). This makes the answer depend on the order of a list the business may not think of as ordered, which is exactly why I would ask.

**Q8. May a promotion raise a price, and if so is such a shop still a valid offer?**
*Assumed:* rules are applied as given and the shop is still an offer (B10); the price is merely unattractive and will lose to a cheaper shop.

**Q9. Is a price of `0` acceptable, or is there a floor below which the chain will not sell** (e.g. at least 1 unit)?
*Assumed:* `0` is a legal, returnable price, and nothing goes negative (B8).

**Q10. Can the same duck have different base prices in different shops?** The surface gives `Shop` no price field, which suggests not, but a real chain usually prices per shop.
*Assumed:* `duck.price` is the base everywhere (B4).

- What happens if there are multiple offers with the same lowest price? Should we return all of them, or just one?
- How should we handle cases where a shop's admission policy changes after the chain has been established?

- **What happens if a shop has no discounts?**: Should it be considered as admitting the duck?
- **What happens if a shop does not admit the duck?**: Should it be considered as not admitting the duck?

- What is the expected behavior when multiple types of discounts (e.g., a percentage discount followed by an amount off) are applied? Should they be applied sequentially or in some other order?
- How should non-standard attributes of a duck (e.g., `hasKotlinAttribute`, `accessories`) affect admission policies and promotions? Are there any implicit rules based on these attributes that need to be considered?

- **Tie-breaking strategy for shops with identical lowest prices**: Currently, any shop can be returned, but a specific tie-breaking rule (e.g., alphabetical order by shop name) could be specified.
- **Handling of overlapping promotions**: The interaction between different promotion types and their application order is not defined. Assuming they are applied independently in the given order.
- **Currency unit**: While prices are whole units, the exact currency is unspecified and assumed to be consistent across all shops and franchises.
