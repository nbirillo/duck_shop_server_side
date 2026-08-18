# Specification — `quote`

## 0. Vocabulary and assumed types

Referred to throughout; nothing here is a new decision, it fixes the words.

```kotlin
data class Duck(
    val name: String,
    val price: Int,                    // the shop's list price for this duck, in whole units
    val hasKotlinAttribute: Boolean,
    val accessories: Collection<String>,
)
```

* **list price** — `duck.price`, as given. Never modified.
* **running price** — the integer the calculation carries while it walks the promotions. It starts
  from the list price (step 2 of §2.1) and each rule maps it to a new running price.
* **admits** — `AdmissionPolicy.admits(duck) == true`.
* A **rule** (`DiscountRule`) denotes a total function `Int -> Int` from a running price to a running
  price, *relative to a fixed duck*. §2.4 gives that function for each of the six shapes.
* All arithmetic in §2.4 is exact integer arithmetic on 64-bit signed values (`Long`), and only the
  final clamped result is an `Int`. See §3.14.

## 1. What it does

`quote` answers one question: *what would this shop charge this customer for this duck today.* It
first asks the shop whether it will take the duck at all; if it will not, there is no price and the
answer is `null`. If it will, `quote` starts from the duck's list price, applies every promotion in
`promotions` in order, and returns the resulting whole-unit price — never negative, never higher than
the list price.

## 2. Behaviour

### 2.1 The calculation, top level

Given `duck`, `shop`, `promotions`:

1. **Admission.** If `shop.admits(duck)` is `false`, return `null`. Nothing else is computed and no
   discount is evaluated.
2. **Start.** Let `p := max(0, duck.price)`.
3. **Promotions.** For each rule `r` in `promotions`, in list order from index `0` upward, set
   `p := apply(r, duck, p)` (§2.4). Each rule is applied to the running price left by the rule before
   it, not to the list price.
4. **Result.** Return `p`.

Consequences that are part of the contract:

* `quote` returns `null` **if and only if** the shop does not admit the duck. An admitted duck always
  gets a non-negative `Int`, possibly `0`.
* `promotions` is empty ⇒ the result is `max(0, duck.price)`.
* The order of `promotions` is significant (§3.5). A rule appearing twice is applied twice (§3.6).
* `duck.name` and `shop.name` never affect the result. Neither does anything about the shop other
  than its `admissionPolicy`.

### 2.2 Admission is decided on the duck alone, before any discount

`shop.admits(duck)` is exactly `shop.admissionPolicy.admits(duck)` — the `Shop` adds no rule of its
own and does not consult `promotions`.

Admission is therefore decided from the duck's **list price**, never from a discounted price. A duck
whose list price is above a shop's `MaxBudget` is refused (`null`) even when a promotion in
`promotions` would have brought the running price under that budget. Symmetrically, a duck admitted
under `MaxBudget(200)` at a list price of `200` keeps its quote even though the promotions may take it
to `0`.

### 2.3 Admission policies

`admits(duck)` for each shape, where `A = duck.accessories`:

| Policy | Admits iff |
| --- | --- |
| `KotlinOnly` | `duck.hasKotlinAttribute` is `true` |
| `MaxBudget(maxPrice)` | `duck.price <= maxPrice` — **inclusive** |
| `RequiresAccessory(name)` | some element of `A` is equal to `name` by exact `String` equality — case-sensitive, no trimming, no normalisation |
| `MinAccessories(min)` | `A.size >= min`, where `A.size` counts the elements as given, duplicates included |
| `AllOf(policies)` | every policy in `policies` admits. Empty list ⇒ **admits** |
| `AnyOf(policies)` | at least one policy in `policies` admits. Empty list ⇒ **refuses** |
| `Not(policy)` | `policy` does not admit |

`AdmissionPolicy` is a `fun interface`, so a caller may pass any predicate on `Duck`, including
lambdas that are none of the shapes above. `quote` treats every policy — leaf, combinator, or
caller-supplied lambda — solely as `Duck -> Boolean`; it must not special-case the known shapes to
reach a different answer than calling `admits` would give.

Callers are expected to supply policies that are **pure and deterministic**: same duck ⇒ same answer,
no observable side effects. §4 says what is left free because of this.

### 2.4 Discount rules

Two laws govern every leaf rule:

* **L1 (reduction law).** A leaf rule names a *reduction* `r`, computed from the running price `p` and
  the rule's fields. The rule's result is `p - clamp(r, 0, p)`. So a reduction below `0` reduces
  nothing, and a reduction above `p` takes the price to exactly `0`.
* **L2 (no rule raises a price, no price goes negative).** For every rule `r`, duck `d` and price
  `p >= 0`: `0 <= apply(r, d, p) <= p`. This holds for the leaves by L1 and is preserved by all three
  combinators, so it holds for arbitrarily nested rules.

`apply(rule, duck, p)` for `p >= 0`:

**`Percentage(percent)`** — reduction `r = floor(p * percent / 100)`, i.e. the exact product `p *
percent` divided by 100 rounding **down**. Result `p - clamp(r, 0, p)`.
The reduction is floored, so the *price* is rounded **up**: the shop keeps the fraction.
`percent == 0` ⇒ unchanged; `percent == 100` ⇒ `0`; `percent > 100` ⇒ `0`; `percent < 0` ⇒ unchanged.

**`AmountOff(amount)`** — reduction `r = amount`. Result `p - clamp(amount, 0, p)`.
`amount <= 0` ⇒ unchanged; `amount >= p` ⇒ `0`.

**`BigSpenderBonus(threshold, amount)`** — if `p >= threshold` (**inclusive**), reduction
`r = amount`, result `p - clamp(amount, 0, p)`; otherwise the result is `p`, unchanged.
The comparison is against the **running price** at the moment this rule runs, not the list price, and
not the final result. So an earlier promotion can push the price under the threshold and cost the
customer this bonus (§3.5).

**`Then(first, second)`** — `apply(second, duck, apply(first, duck, p))`. `first` runs first.
`Then(a, b)` at position `i` of `promotions` is exactly equivalent to `a, b` at positions `i, i+1`.

**`BestOf(options)`** — every option is applied **independently to the same input `p`**, and the
result is the smallest of those results:
`min { apply(o, duck, p) | o in options }` — the outcome cheapest for the customer.
Options do not compose with each other: `BestOf([a, b])` never applies `a` and `b` together.
`options` empty ⇒ the result is `p`, unchanged. By L2 the result is always `<= p`, so `BestOf` can
never be worse for the customer than skipping it.
Two options producing the same price are indistinguishable; see §4.

**`OnlyIf(condition, rule)`** — if `condition.admits(duck)` then `apply(rule, duck, p)`, else `p`
unchanged. This is the mechanism for a promotion aimed at particular ducks.
`condition` is evaluated against the **duck**, using §2.3 unchanged; it is *not* a test on the running
price. In particular `OnlyIf(MaxBudget(n), …)` compares `n` against `duck.price`, the list price, even
when the running price has already been discounted below `n` (§3.9). A condition is not required to
have any relation to the shop's own `admissionPolicy`; a shop may run a promotion for ducks it would
refuse, in which case the promotion is simply never reached.

### 2.5 Purity

`quote` is a pure function of `(duck, shop, promotions)` up to the purity of the caller's policies: it
mutates none of its arguments and, given policies that behave as §2.3 requires, returns the same
answer for the same inputs every time.

## 3. Edge cases

Each line is the answer, not a hint.

1. **`promotions` empty** — result is `max(0, duck.price)`. `Duck(price = 100)` ⇒ `100`.
2. **Refused duck with a huge promotion list** — `null`. The promotions are irrelevant and no
   reduction is computed.
3. **List price `0`** — an admitted duck quotes `0`; every rule leaves it at `0` (by L2). A
   `BigSpenderBonus(0, 5)` fires at `p = 0` and still yields `0`, since the reduction clamps to `p`.
4. **Negative list price** (legal for `Int`) — admission uses the raw negative value
   (`MaxBudget(-5)` admits a duck priced `-10`), and the quote is `0`: step 2 of §2.1 clamps the start
   to `0`.
5. **Order matters** — `price = 100`, `[Percentage(50), BigSpenderBonus(100, 10)]` ⇒ `100 → 50`, then
   `50 < 100` so no bonus ⇒ **`50`**. The same two rules reversed,
   `[BigSpenderBonus(100, 10), Percentage(50)]` ⇒ `100 → 90 → 45` ⇒ **`45`**.
6. **Repeated rule** — `price = 100`, `[AmountOff(10), AmountOff(10)]` ⇒ `80`. Two occurrences, two
   reductions.
7. **Rounding, worked** — `price = 10`, `Percentage(25)`: reduction `floor(2.5) = 2` ⇒ **`8`** (not
   `7`). `price = 3`, `Percentage(50)`: reduction `1` ⇒ **`2`**. `price = 1`, `Percentage(99)`:
   reduction `floor(0.99) = 0` ⇒ **`1`**; a price of `1` cannot be discounted by percentage at all.
8. **Percentage out of `[0, 100]`** — `Percentage(150)` on `40` ⇒ `0` (reduction `60` clamps to `40`).
   `Percentage(-10)` on `40` ⇒ `40`. `Percentage(0)` on `40` ⇒ `40`.
9. **`AmountOff` bigger than the price** — `AmountOff(500)` on `40` ⇒ `0`; the shop does not owe the
   customer `460`. `AmountOff(0)` and `AmountOff(-7)` ⇒ unchanged.
10. **`BigSpenderBonus` at the boundary** — `BigSpenderBonus(100, 10)`: `p = 100` ⇒ `90`; `p = 99` ⇒
    `99`. `threshold = Int.MIN_VALUE` ⇒ always fires. `threshold` above any reachable price ⇒ never
    fires. `amount <= 0` ⇒ fires but changes nothing.
11. **`BestOf`** — `price = 100`, `BestOf([Percentage(10), AmountOff(15)])` ⇒ `min(90, 85)` = **`85`**.
    On `price = 20` the same rule ⇒ `min(18, 5)` = **`5`**. `BestOf(emptyList())` ⇒ unchanged.
    `BestOf([single])` ⇒ identical to that single rule. Nested `BestOf`/`Then` compose by §2.4 with no
    extra rule.
12. **`OnlyIf` with a false condition** — unchanged, exactly as if the rule were absent.
    `OnlyIf(KotlinOnly, AmountOff(10))` on a duck with `hasKotlinAttribute = false` and `price = 50`
    ⇒ `50`.
13. **`OnlyIf(AllOf(emptyList()), rule)`** ⇒ always applies `rule` (empty `AllOf` admits).
    `OnlyIf(AnyOf(emptyList()), rule)` ⇒ never applies it.
14. **Extreme magnitudes / no overflow** — `price = Int.MAX_VALUE` with `Percentage(1)`: the product
    is formed in 64-bit, reduction `21474836`, result `2126008811`. `Percentage(Int.MAX_VALUE)` on any
    `p > 0` ⇒ `0`. Because L1 clamps every reduction into `[0, p]` before subtracting, no
    intermediate or final value can overflow `Int`, for any combination of field values.
15. **Empty accessories** — `MinAccessories(0)` admits (as does any `min <= 0`); `MinAccessories(1)`
    refuses; `RequiresAccessory(anything)` refuses.
16. **Duplicate accessories** — `["hat", "hat"]` satisfies `MinAccessories(2)`; the count is of
    elements, not of distinct values. (If `Duck.accessories` is a `Set`, duplicates cannot arise and
    the rule is unaffected.)
17. **Accessory name matching** — `RequiresAccessory("Hat")` is refused by `["hat"]`; exact
    case-sensitive equality. `RequiresAccessory("")` admits iff `""` is an element.
18. **Deeply nested policies** — `Not(Not(KotlinOnly))` ≡ `KotlinOnly`; `AllOf([AnyOf([]), …])`
    refuses whatever else it contains. Nesting depth carries no special rule.
19. **A policy that throws** — the exception propagates to `quote`'s caller unchanged. `quote`
    catches nothing and substitutes no default; a throwing policy does not become a refusal.
20. **Two shops, one duck** — nothing is cached or carried between calls; each call is decided from
    its own arguments only.

## 4. Deliberately not specified

An implementation may do any of these either way without being wrong, and a test must not pin them
down.

1. **How many times a policy or rule is evaluated, and in what order.** Whether `AllOf`/`AnyOf`
   short-circuit, whether `BestOf` evaluates every option or stops early, whether repeated
   sub-expressions are memoised. For pure policies (§2.3) none of this changes the returned number.
   This is the reason §2.3 requires purity: with an impure policy the result becomes
   implementation-dependent, and that is accepted rather than defined.
2. **Which option of a `BestOf` "won", including ties.** `BestOf` yields a number, not a choice; the
   winning option is not returned, not logged in any contracted way, and on a tie either option may
   be considered the winner.
3. **Whether a rule that provably cannot change the price is applied at all** — e.g. `AmountOff(0)`,
   `Percentage(0)`, a `Then` of two such. The observable price is the same either way.
4. **Recursion vs. an explicit stack, and behaviour on pathologically deep nesting.** A rule or policy
   tree deep enough to exhaust the JVM stack may fail; nothing here promises a depth limit or a
   graceful answer at it.
5. **Performance, complexity, allocation.** No bound is specified.
6. **Thread-safety of caller-supplied policies.** `quote` itself holds no mutable state; whether it
   may be called concurrently with a given policy is that policy's business.
7. **Everything not about the returned number** — `toString`, `equals`/`hashCode` beyond what `data
   class` gives, logging, internal helper names and visibility, whether the leaves are declared inside
   `AdmissionPolicy` or beside it.
8. **The exact concrete type of `Duck.accessories`** (`List` vs `Set` vs other `Collection`), and the
   iteration order of it. §2.3 depends only on membership and size, both order-independent.
9. **Whether unreachable promotions are rejected up front.** A `promotions` list containing a rule
   whose `OnlyIf` condition contradicts the shop's `admissionPolicy` is legal input; an
   implementation may pre-filter it or not.

## 5. Open questions

There is a right answer to each of these and the brief does not contain it. Each states the
assumption in force, so implementation can proceed now; if the business answers differently, the
listed section changes and nothing else does.

1. **Rounding direction on `Percentage`.** "Prices are whole units" fixes that a fraction cannot
   survive, not who gets it. Floor-the-discount (assumed, §2.4) means the shop keeps the fraction;
   floor-the-price would mean the customer does. On `price = 10, percent = 25` the answers are `8`
   and `7`. *Assumed: the discount is floored, so the price rounds up.* Also unasked: whether the
   business wants half-up rounding instead of either.
2. **What `BigSpenderBonus` measures.** "Big spender" could mean the duck's list price, what the
   customer actually pays after other promotions, or the price at that point in the chain.
   *Assumed: the running price at the moment the rule runs* (§2.4, §3.5). If it should be the list
   price, `BigSpenderBonus` becomes independent of its position in `promotions`.
3. **Whether promotions stack at all.** "A shop runs promotions — the same ones as before" does not
   say whether a customer gets all of them in sequence or only the best one. *Assumed: all of them,
   in list order, cumulatively* (§2.1) — `BestOf` exists precisely to express "only the best one", so
   the list itself is read as stacking.
4. **Whether `promotions` order is meaningful input or an accident of collection.** The rules are
   order-sensitive (§3.5), so a caller assembling promotions in an arbitrary order can get an
   arbitrary price. *Assumed: the caller controls the order deliberately and `quote` preserves it.*
   The alternative — a canonical order imposed by `quote` — needs a business ranking that does not
   exist yet.
5. **Whether admission should see the discounted price.** A shop with `MaxBudget(100)` arguably wants
   the duck a promotion brings down to `90`. *Assumed: no — admission is decided on the list price,
   once, before discounting* (§2.2). This is also the only reading the given signature supports, since
   `AdmissionPolicy.admits` receives a `Duck` and no price.
6. **Inclusive or exclusive boundaries.** `MaxBudget(maxPrice)` and `BigSpenderBonus(threshold, …)`
   both sit on a boundary the brief does not name. *Assumed: both inclusive* — `price == maxPrice` is
   admitted, `p == threshold` fires the bonus.
7. **Out-of-range fields: bad data or valid input?** `Percentage(150)`, `Percentage(-10)`,
   `AmountOff(-5)`, `MinAccessories(-1)` are all constructible. *Assumed: valid input, absorbed by the
   clamping in L1 with no exception* (§3.8, §3.9). The business may prefer construction to fail, which
   is a change to the data classes rather than to `quote`.
8. **Is `0` an acceptable quote?** *Assumed: yes* — a free duck is a legal answer and is distinct from
   `null`, which means only "not for sale here" (§3.3, §3.9). If the business wants a floor of `1`, or
   wants a free duck refused, that is a new rule in §2.1 step 4.
9. **Accessory identity.** Whether `"Hat"`, `"hat"` and `" hat "` name the same accessory, and whether
   duplicates in `accessories` are meaningful. *Assumed: exact case-sensitive string equality, and
   duplicates count toward `MinAccessories`* (§2.3, §3.16, §3.17).
10. **Units and currency.** "Whole units" is assumed to be one currency, the same for the duck's
    price, `AmountOff`, `MaxBudget` and `BigSpenderBonus`, with no conversion anywhere. *Assumed: a
    single implicit unit*; if shops price in different currencies, every comparison above needs a
    currency alongside the number.
