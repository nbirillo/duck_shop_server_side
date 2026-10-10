package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

/**
 * FORK 2 — both sets of promotions apply, the shop's first and the chain's on top.
 *
 * Every OTHER fork is held at the reference reading on purpose: a reading that varied two forks at
 * once would make a rejected test unattributable, and the report would name the wrong decision.
 */
fun bestOffer(duck: Duck, franchise: Franchise): Offer? {
    if (!franchise.admissionPolicy.admits(duck)) return null
    return franchise.shops
        .filter { it.admits(duck) }
        .map { Offer(it, priceFor(duck, it.promotions + franchise.promotions)) }
        .minByOrNull { it.price }
}
