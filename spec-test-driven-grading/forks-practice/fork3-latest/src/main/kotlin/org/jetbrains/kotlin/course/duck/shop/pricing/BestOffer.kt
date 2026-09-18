package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

/**
 * FORK 3 — when two shops reach the same price, the LAST in the chain's list wins.
 *
 * Every OTHER fork is held at the reference reading on purpose: a reading that varied two forks at
 * once would make a rejected test unattributable, and the report would name the wrong decision.
 */
fun bestOffer(duck: Duck, franchise: Franchise): Offer? {
    if (!franchise.admissionPolicy.admits(duck)) return null
    val offers = franchise.shops
        .filter { it.admits(duck) }
        .map { Offer(it, priceFor(duck, it.promotions + franchise.promotions)) }
    val cheapest = offers.minOfOrNull { it.price } ?: return null
    return offers.last { it.price == cheapest }
}
