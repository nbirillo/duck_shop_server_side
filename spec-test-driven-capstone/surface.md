# The surface your agent writes against

This is what already exists and compiles. Hand it to the agent together with **your** specification and
nothing else — no brief, no notes to yourself, no tests. If you hand over more than the specification,
what you measure afterwards is the extra, not the specification.

`./gradlew runAgent -Pmode=impl-from-spec -Pagent=<name> -Pspec=<your spec> -Pprovider=ollama -Pmodel=<model>`
sends exactly this file plus the spec you name.

```kotlin
// EVERYTHING BELOW ALREADY EXISTS on your compile path. Import it; never declare it. Field types are
// given because guessing them is the single most common way this file fails to compile.
//
// The imports you need, EXACTLY — anything else you reach for is in one of these two packages, and
// nothing at all lives anywhere else:
//
//   package org.jetbrains.kotlin.course.duck.shop.pricing   // ← your file's own package
//
//   import org.jetbrains.kotlin.course.duck.shop.admission.Duck
//   import org.jetbrains.kotlin.course.duck.shop.admission.Shop
//   import org.jetbrains.kotlin.course.duck.shop.admission.AdmissionPolicy
//   import org.jetbrains.kotlin.course.duck.shop.admission.Accessory
//
//   Franchise, Offer, DiscountRule and priceFor are in YOUR OWN package and need NO import.
//
//   Duck(name: String, price: Int, hasKotlinAttribute: Boolean, accessories: List<Accessory>)
//   Shop(name: String, admissionPolicy: AdmissionPolicy, promotions: List<DiscountRule>)
//     — and Shop.admits(duck: Duck): Boolean
//   Franchise(name: String, admissionPolicy: AdmissionPolicy, promotions: List<DiscountRule>,
//             shops: List<Shop>)
//   Offer(shop: Shop, price: Int)
//
//   AdmissionPolicy — an interface with admits(duck: Duck): Boolean. Implementations:
//     KotlinOnly()                       MaxBudget(maxPrice: Int)
//     RequiresAccessory(accessoryName: String)   MinAccessories(min: Int)
//     AllOf(policies: List<AdmissionPolicy>)     AnyOf(policies: List<AdmissionPolicy>)
//     Not(policy: AdmissionPolicy)
//
//   DiscountRule — sealed, SIX NESTED cases, so a `when` over it must be exhaustive and every name
//   needs the `DiscountRule.` qualifier:
//     DiscountRule.Percentage(percent: Int)
//     DiscountRule.AmountOff(amount: Int)
//     DiscountRule.BigSpenderBonus(threshold: Int, amount: Int)
//     DiscountRule.Then(first: DiscountRule, second: DiscountRule)
//     DiscountRule.BestOf(options: List<DiscountRule>)     // a LIST, of any length, possibly empty
//     DiscountRule.OnlyIf(condition: AdmissionPolicy, rule: DiscountRule)
//
// ALREADY WRITTEN AND SETTLED — use it, do not reimplement pricing:
//
//   priceFor(duck: Duck, rules: List<DiscountRule>): Int
//     applies a list of discount rules to a duck's price, in order, and returns whole units.
//
// Write this one function and nothing else public:

fun bestOffer(duck: Duck, franchise: Franchise): Offer?
```
