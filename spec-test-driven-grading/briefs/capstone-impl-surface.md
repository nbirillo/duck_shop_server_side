# Implementation surface — 11.6

Teacher-only. Pass this as `-PimplSurface` when an agent **implements**, and the brief itself when a
learner **specifies**. They are not the same artifact, and conflating them cost four runs — worth
listing, because the same mistake was made four different ways and every one looked like the model's
fault at first:

1. the surface showed `data class Franchise(...)` in full → both models **copied the declaration**
   (`Redeclaration:`), because a declaration is a thing you write;
2. cases listed as bare names → `Percentage` written without its `DiscountRule.` qualifier;
3. field names given without **types** → `BestOf(options)` read as a two-way choice (`options.first`);
4. types listed without their **packages** → everything imported from `…admission`, where `Duck` lives,
   while `Franchise`, `Offer` and `DiscountRule` are in `…pricing`.

The lesson is not a wording. **An implementation surface has to be complete enough to compile against**,
so the import block below is given literally: there is nothing left to guess, and nothing to copy.

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
//   Franchise, Offer and DiscountRule are in YOUR OWN package (…shop.pricing) and need NO import.
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
// Any private helper that has to evaluate an OnlyIf condition needs the Duck passed to it.
//
// Write this one function and nothing else public:

fun bestOffer(duck: Duck, franchise: Franchise): Offer?
```
