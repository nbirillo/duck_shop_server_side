package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.Shop

/**
 * TEACHER-ONLY reference for the 11.5 seam. Every decision it makes and why:
 *
 *  - **Admission is a gate, and it reads the shelf price.** Not a choice so much as the only reading
 *    `AdmissionPolicy.admits(duck: Duck)` supports — it receives a duck, never a running price, so
 *    "admission sees the discounted price" would mean handing it a fabricated `Duck`. Measured during
 *    calibration: three local models assumed this silently and the frontier argued it explicitly.
 *  - **A duck the shop refuses has no quote** — `null`, which the brief settles outright.
 *  - **An admitted duck always has a number, and 0 is a number.** This is the one decision the brief
 *    genuinely leaves open: a duck discounted to nothing is free, not unquotable. Chosen because
 *    `null` already means one thing (refused) and making it mean two would be a worse contract.
 *  - **Pricing itself is not re-decided here.** It is `priceFor`, unchanged, with every rounding,
 *    clamping and ordering decision it already carries. That is the whole point of the seam: the
 *    earlier specification is an asset, and this file is three lines because of it.
 */
fun quote(duck: Duck, shop: Shop, promotions: List<DiscountRule>): Int? =
    if (!shop.admits(duck)) null else priceFor(duck, promotions)
