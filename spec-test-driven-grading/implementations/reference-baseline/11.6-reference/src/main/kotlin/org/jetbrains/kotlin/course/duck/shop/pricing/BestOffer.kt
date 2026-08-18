package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

/**
 * TEACHER-ONLY reference for the 11.6 capstone, and it takes **the frontier's reading** of the brief,
 * recorded here so the hidden set has a definite answer to score against. Each decision is one the
 * brief deliberately leaves open, so this file is a *choice*, not a truth:
 *
 *  - **Admission is a conjunction.** A shop may sell only if it admits the duck *and* the chain does.
 *    "The chain has rules of its own on top of that" is read as a filter, not an override. The three
 *    local models we calibrated on never mentioned the chain's policy at all.
 *  - **Both sets of promotions apply, the shop's first and the chain's on top**, once per occurrence
 *    and with no deduplication. Order is observable, so it is contract.
 *  - **Ties go to the lowest index in `franchise.shops`** — `minByOrNull` keeps the first minimum.
 *    Naming the shop in `Offer` is what makes a tie observable at all.
 *  - **`null` exactly when no shop qualifies.**
 *
 * Pricing itself is not re-decided: it is `priceFor`, with the rounding, clamping and ordering the
 * earlier module already settled.
 */
fun bestOffer(duck: Duck, franchise: Franchise): Offer? {
    if (!franchise.admissionPolicy.admits(duck)) return null
    return franchise.shops
        .filter { it.admits(duck) }
        .map { Offer(it, priceFor(duck, it.promotions + franchise.promotions)) }
        .minByOrNull { it.price }
}
