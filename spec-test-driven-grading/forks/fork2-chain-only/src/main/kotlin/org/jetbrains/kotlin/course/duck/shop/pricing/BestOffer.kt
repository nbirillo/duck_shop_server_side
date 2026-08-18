package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

/**
 * FORK 2 — only the chain's promotions apply; joining the chain replaces local offers.
 *
 * Every OTHER fork is held at the reference reading on purpose: a reading that varied two forks at
 * once would make a rejected test unattributable, and the report would name the wrong decision.
 */
fun bestOffer(duck: Duck, franchise: Franchise): Offer? {
    if (!franchise.admissionPolicy.admits(duck)) return null
    return franchise.shops
        .filter { it.admits(duck) }
        .map { Offer(it, priceFor(duck, franchise.promotions)) }
        .minByOrNull { it.price }
}
