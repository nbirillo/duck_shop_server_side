package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

/**
 * FORK 1 — the shop decides alone; the chain's own rule is not a gate on selling.
 *
 * Every OTHER fork is held at the reference reading on purpose: a reading that varied two forks at
 * once would make a rejected test unattributable, and the report would name the wrong decision.
 */
fun bestOffer(duck: Duck, franchise: Franchise): Offer? =
    franchise.shops
        .filter { it.admits(duck) }
        .map { Offer(it, priceFor(duck, it.promotions + franchise.promotions)) }
        .minByOrNull { it.price }
