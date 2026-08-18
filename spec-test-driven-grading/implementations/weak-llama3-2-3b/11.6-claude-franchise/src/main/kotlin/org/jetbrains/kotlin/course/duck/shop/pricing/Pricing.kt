package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.Shop
import org.jetbrains.kotlin.course.duck.shop.admission.AdmissionPolicy
import org.jetbrains.kotlin.course.duck.shop.admission.Accessory
import org.jetbrains.kotlin.course.duck.shop.pricing.DiscountRule

fun bestOffer(duck: Duck, franchise: Franchise): Offer? {
    val eligibleShops = franchise.shops.filter { it.admits(duck) }
    
    if (eligibleShops.isEmpty()) return null
    
    var minPrice = Int.MAX_VALUE
    var winningShop = Shop()
    
    for (shop in eligibleShops) {
        var price = maxOf(0, duck.price)
        
        for (rule in shop.promotions + franchise.promotions) {
            val newPrice = apply(rule, duck, price)
            if (newPrice < 0) continue
            price = newPrice
        }
        
        if (price < minPrice) {
            minPrice = price
            winningShop = shop
        } else if (price == minPrice && winningShop.name != shop.name) {
            // Choose the first eligible shop in list order
            for (otherShop in eligibleShops) {
                if (otherShop.name != shop.name) continue
                if (otherShop.price < price) return null
            }
        }
    }
    
    return Offer(shop = winningShop, price = minPrice)
}

fun apply(rule: DiscountRule, duck: Duck, price: Int): Int {
    val result = rule.apply(duck, price)
    return maxOf(0, result)
}
