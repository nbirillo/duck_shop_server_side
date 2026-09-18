package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Accessory
import org.jetbrains.kotlin.course.duck.shop.admission.AdmissionPolicy
import org.jetbrains.kotlin.course.duck.shop.admission.AllOf
import org.jetbrains.kotlin.course.duck.shop.admission.AnyOf
import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.KotlinOnly
import org.jetbrains.kotlin.course.duck.shop.admission.MaxBudget
import org.jetbrains.kotlin.course.duck.shop.admission.MinAccessories
import org.jetbrains.kotlin.course.duck.shop.admission.Not
import org.jetbrains.kotlin.course.duck.shop.admission.RequiresAccessory
import org.jetbrains.kotlin.course.duck.shop.admission.Shop
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Records what one implementation of `bestOffer` answers on a fixed corpus — the 11.6 capstone.
 *
 * ### Why the shop's name is in the output
 *
 * `Offer` names a shop, so **a tie between two shops at the same price is observable**. That is the
 * point: the brief does not say which one wins, and a specification has to. The three local models we
 * calibrated on all wrote "any of them can be returned", which is not a contract — and the recording
 * turns that non-answer into a visible difference the moment two implementations pick differently.
 *
 * ### What the corpus has to be able to see
 *
 * Every bound is a blind spot. Beyond the usual (empty, zero, the top of the `Int` range) this corpus
 * has to reach the situations the brief leaves open, or it measures nothing:
 *
 *  - two shops that end up at the **same** price
 *  - a chain whose own policy **rejects** a duck every shop would take, and the reverse
 *  - a chain with **no shops**
 *  - shops whose promotions differ, so "cheapest" is not the same shop before and after they apply
 *
 * The guard below asserts each of those really occurs. The 11.2 probe twice reported a real divergence
 * as "identical" because its generator never built the input that would have shown it.
 */
class FranchiseProbe {

    @Test
    fun record() {
        val target = File(System.getProperty("probe.out") ?: "build/probe/recording.txt")
        target.parentFile.mkdirs()
        target.writeText(corpus().joinToString("\n") + "\n")
        println("[franchise-probe] ${target.absolutePath}")
    }

    @Test
    fun `the corpus reaches the values it claims to`() {
        val cases = corpus()
        fun some(what: String, predicate: (String) -> Boolean) =
            assertTrue(cases.any(predicate), "no probed case has $what — the check is blind to it")

        some("no offer at all") { it.endsWith("=> none") }
        some("an offer") { Regex("=> s\\d+@").containsMatchIn(it) }
        some("a chain with no shops") { it.contains("shops=[]") }
        some("a chain whose own policy admits everything") { it.contains("chain=AllOf()") }
        some("a chain whose own policy admits nothing") { it.contains("chain=AnyOf()") }
        some("a chain with no promotions of its own") { it.contains("chainPromos=[]") }
        some("a duck priced 0") { it.contains("price=0,") }
        some("a price at the top of the Int range") { it.contains("price=2147483647,") }
        some("two shops that reach the same price") { it.contains("TIE") }
        // The chain rejecting a duck its shops would take is the one case that separates "the chain is
        // a filter" from "the chain is ignored" — and all three local models ignored the field.
        some("a chain that refuses what its shops would sell") { it.contains("CHAIN-VETO") }
        some("shops whose promotions differ") { it.contains("DIFFERING-PROMOS") }
    }

    private fun corpus(): List<String> {
        val random = Random(SEED)
        val out = mutableListOf<String>()
        val open: AdmissionPolicy = AllOf(emptyList())
        val closed: AdmissionPolicy = AnyOf(emptyList())

        fun shop(i: Int, policy: AdmissionPolicy = open, promos: List<DiscountRule> = emptyList()) =
            Shop("s$i", policy, promos)

        // Hand-picked: every situation the brief leaves open, stated once and never left to chance.
        val fixed: List<Triple<Duck, Franchise, String>> = listOf(
            Triple(duck(100), Franchise("f", open, emptyList(), emptyList()), ""),
            Triple(duck(100), Franchise("f", open, emptyList(), listOf(shop(0))), ""),
            // two shops, identical everything — a tie by construction
            Triple(duck(100), Franchise("f", open, emptyList(), listOf(shop(0), shop(1))), "TIE"),
            // same price by different routes: 10% off 100 and 10 off 100
            Triple(
                duck(100),
                Franchise("f", open, emptyList(), listOf(
                    shop(0, promos = listOf(DiscountRule.Percentage(10))),
                    shop(1, promos = listOf(DiscountRule.AmountOff(10))),
                )),
                "TIE DIFFERING-PROMOS",
            ),
            // the chain refuses what the shops would sell
            Triple(duck(100), Franchise("f", closed, emptyList(), listOf(shop(0), shop(1))), "CHAIN-VETO"),
            // and the reverse: the chain would take it, no shop will
            Triple(duck(100), Franchise("f", open, emptyList(), listOf(shop(0, closed), shop(1, closed))), ""),
            // cheapest is a different shop before and after the chain's promotion applies
            Triple(
                duck(100),
                Franchise("f", open, listOf(DiscountRule.BigSpenderBonus(90, 50)), listOf(
                    shop(0, promos = listOf(DiscountRule.Percentage(20))),
                    shop(1, promos = listOf(DiscountRule.AmountOff(5))),
                )),
                "DIFFERING-PROMOS",
            ),
            Triple(duck(0), Franchise("f", open, emptyList(), listOf(shop(0))), ""),
            Triple(
                duck(Int.MAX_VALUE),
                Franchise("f", open, listOf(DiscountRule.Percentage(10)), listOf(shop(0))),
                "",
            ),
            Triple(
                duck(60),
                Franchise("f", open, emptyList(), listOf(shop(0, MaxBudget(50)), shop(1, MaxBudget(100)))),
                "",
            ),
        )
        fixed.forEach { (d, f, tag) -> out += line(d, f, tag) }

        repeat(CASES) {
            val price = when (random.nextInt(6)) {
                0 -> 0
                1 -> random.nextInt(1, 100)
                2 -> Int.MAX_VALUE
                else -> random.nextInt(1, 5_000)
            }
            val d = duck(price, random.nextBoolean(), accessories(random))
            val shops = List(random.nextInt(0, MAX_SHOPS + 1)) { i ->
                shop(i, randomPolicy(random, DEPTH), List(random.nextInt(0, 3)) { randomRule(random, 1) })
            }
            val f = Franchise(
                "f",
                randomPolicy(random, DEPTH),
                List(random.nextInt(0, 3)) { randomRule(random, 1) },
                shops,
            )
            out += line(d, f, if (shops.map { it.promotions }.distinct().size > 1) "DIFFERING-PROMOS" else "")
        }
        return out
    }

    // ── generators, borrowed from the two probes that came before ────────────────────────────────

    private fun randomPolicy(random: Random, depth: Int): AdmissionPolicy {
        if (depth <= 0) return randomLeaf(random)
        return when (random.nextInt(6)) {
            0 -> AllOf(List(random.nextInt(0, MAX_WIDTH)) { randomPolicy(random, depth - 1) })
            1 -> AnyOf(List(random.nextInt(0, MAX_WIDTH)) { randomPolicy(random, depth - 1) })
            2 -> Not(randomPolicy(random, depth - 1))
            else -> randomLeaf(random)
        }
    }

    private fun randomLeaf(random: Random): AdmissionPolicy = when (random.nextInt(4)) {
        0 -> KotlinOnly()
        1 -> MaxBudget(random.nextInt(-1, 5_000))
        2 -> RequiresAccessory(ACCESSORY_NAMES[random.nextInt(ACCESSORY_NAMES.size)])
        else -> MinAccessories(random.nextInt(0, MAX_WIDTH + 1))
    }

    private fun randomRule(random: Random, depth: Int): DiscountRule =
        when (random.nextInt(if (depth <= 0) 3 else 6)) {
            0 -> DiscountRule.Percentage(random.nextInt(0, 160))
            1 -> DiscountRule.AmountOff(random.nextInt(0, 2_000))
            2 -> DiscountRule.BigSpenderBonus(random.nextInt(0, 3_000), random.nextInt(0, 500))
            3 -> DiscountRule.Then(randomRule(random, depth - 1), randomRule(random, depth - 1))
            4 -> DiscountRule.BestOf(List(random.nextInt(0, 3)) { randomRule(random, depth - 1) })
            else -> DiscountRule.OnlyIf(randomLeaf(random), randomRule(random, depth - 1))
        }

    private fun accessories(random: Random) =
        List(random.nextInt(0, MAX_WIDTH)) { Accessory(ACCESSORY_NAMES[random.nextInt(ACCESSORY_NAMES.size)]) }

    private fun duck(price: Int, kotlin: Boolean = false, accessories: List<Accessory> = emptyList()) =
        Duck("d", price, kotlin, accessories)

    /**
     * One recorded case. A throw is a **value**, not a crash — an implementation that rejects an input
     * has made a decision. `none` renders `null`, and an offer renders as `shop@price`, so "no offer"
     * and "an offer of 0" cannot be confused, and neither can two shops at the same price.
     */
    private fun line(duck: Duck, franchise: Franchise, tag: String): String {
        val outcome = runCatching {
            bestOffer(duck, franchise)?.let { "${it.shop.name}@${it.price}" } ?: "none"
        }.getOrElse { "threw ${it::class.simpleName}" }
        val shops = franchise.shops.joinToString { s ->
            "${s.name}{${describe(s.admissionPolicy)}, [${s.promotions.joinToString { describe(it) }}]}"
        }
        return "price=${duck.price}, kotlin=${duck.hasKotlinAttribute}, accessories=${duck.accessories.size}, " +
            "chain=${describe(franchise.admissionPolicy)}, " +
            "chainPromos=[${franchise.promotions.joinToString { describe(it) }}], " +
            "shops=[$shops]${if (tag.isEmpty()) "" else " $tag"} => $outcome"
    }

    private fun describe(policy: AdmissionPolicy): String = when (policy) {
        is AllOf -> "AllOf(${children(policy).joinToString { describe(it) }})"
        is AnyOf -> "AnyOf(${children(policy).joinToString { describe(it) }})"
        is Not -> "Not(${describe(inner(policy))})"
        is KotlinOnly -> "KotlinOnly()"
        is MaxBudget -> "MaxBudget(${policy.field<Int>("maxPrice")})"
        is RequiresAccessory -> "RequiresAccessory(\"${policy.field<String>("accessoryName")}\")"
        is MinAccessories -> "MinAccessories(${policy.field<Int>("min")})"
        else -> policy::class.simpleName ?: "policy"
    }

    private fun describe(rule: DiscountRule): String = when (rule) {
        is DiscountRule.Percentage -> "Percentage(${rule.percent})"
        is DiscountRule.AmountOff -> "AmountOff(${rule.amount})"
        is DiscountRule.BigSpenderBonus -> "BigSpenderBonus(${rule.threshold}, ${rule.amount})"
        is DiscountRule.Then -> "Then(${describe(rule.first)}, ${describe(rule.second)})"
        is DiscountRule.BestOf -> "BestOf[${rule.options.joinToString { describe(it) }}]"
        is DiscountRule.OnlyIf -> "OnlyIf(${describe(rule.condition)}, ${describe(rule.rule)})"
    }

    private fun children(policy: AdmissionPolicy): List<AdmissionPolicy> = policy.field("policies")
    private fun inner(policy: AdmissionPolicy): AdmissionPolicy = policy.field("policy")

    @Suppress("UNCHECKED_CAST")
    private fun <T> AdmissionPolicy.field(name: String): T =
        this::class.java.getDeclaredField(name).also { it.isAccessible = true }.get(this) as T

    private companion object {
        const val SEED = 20260818L
        const val CASES = 3_000
        const val MAX_SHOPS = 3
        const val MAX_WIDTH = 3
        const val DEPTH = 2
        val ACCESSORY_NAMES = listOf("Hat", "hat", "Scarf")
    }
}
