package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

/**
 * FORK 4 — choose the cheapest shop on its OWN promotions, then apply the chain's to the winner.
 *
 * Every OTHER fork is held at the reference reading on purpose: a reading that varied two forks at
 * once would make a rejected test unattributable, and the report would name the wrong decision.
 */
fun bestOffer(duck: Duck, franchise: Franchise): Offer? {
    if (!franchise.admissionPolicy.admits(duck)) return null
    val shop = franchise.shops
        .filter { it.admits(duck) }
        .minByOrNull { priceFor(duck, it.promotions) }
        ?: return null
    return Offer(shop, priceFor(duck, shop.promotions + franchise.promotions))
}
