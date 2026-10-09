package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.AdmissionPolicy
import org.jetbrains.kotlin.course.duck.shop.admission.Shop

/**
 * A chain of shops. **Data only** — what a chain *does* with this data is not defined here, because
 * defining it is the capstone: the learner inherits a specification and must decide what is
 * right, what is merely stated, and what has not been decided at all.
 *
 * @property admissionPolicy the chain's own rule, in addition to each shop's. How the two combine is one of
 *   the things the brief deliberately leaves open.
 * @property promotions the chain's own promotions, in addition to each shop's.
 */
data class Franchise(
    val name: String,
    val admissionPolicy: AdmissionPolicy,
    val promotions: List<DiscountRule>,
    val shops: List<Shop>,
)

/** Where a duck can be bought and at what price. Naming the shop makes a tie between shops observable. */
data class Offer(val shop: Shop, val price: Int)
