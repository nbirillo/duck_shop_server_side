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
 * Records what one implementation of `quote` answers on a fixed corpus — the seam between admission
 * and pricing (11.5).
 *
 * ### What this is for, and it is not the same job as the pricing probe
 *
 * In 11.4 two recordings came from **two readers of one text**, and their disagreement measured how
 * much the text left open. Here the two recordings are usually **one implementation before and after
 * a refactor**, and their disagreement answers a different question: *did the behaviour change?*
 *
 * That question is worth a machine because the learner's own suite cannot answer it. A suite going red
 * after a shape change is ambiguous on its own — either the change was not behaviour-preserving, or the
 * suite pinned an accident. The recording says which:
 *
 * | behaviour changed | suite red | reading |
 * | --- | --- | --- |
 * | no | no | a clean refactor, and the suite is fine |
 * | no | **yes** | the suite pinned an accident — relax it |
 * | **yes** | no | not a refactor at all, and the suite missed it |
 * | yes | yes | behaviour changed and the suite caught it — fine, but call it what it is |
 *
 * ### The corpus
 *
 * `(duck, shop, promotions)`. The shop's policy is a random tree built the way 11.2's
 * `DifferentialProbe` builds them; the promotions are random rules built the way `PricingProbe` builds
 * them. Both generators are copied rather than shared because each probe has to stay readable on its
 * own, and because their bounds are tuned to different blind spots.
 *
 * Every bound here is a blind spot. `the corpus reaches the values it claims to` is not decoration:
 * the 11.2 probe twice reported a real divergence as "identical" because the generator never produced
 * the input that would have shown it.
 */
class QuoteProbe {

    @Test
    fun record() {
        val target = File(System.getProperty("probe.out") ?: "build/probe/recording.txt")
        target.parentFile.mkdirs()
        target.writeText(corpus().joinToString("\n") + "\n")
        println("[quote-probe] ${target.absolutePath}")
    }

    @Test
    fun `the corpus reaches the values it claims to`() {
        val cases = corpus()
        fun some(what: String, predicate: (String) -> Boolean) =
            assertTrue(cases.any(predicate), "no probed case has $what — the check is blind to it")

        some("a duck the shop refuses") { it.endsWith("=> none") }
        some("a duck the shop takes") { Regex("=> \\d").containsMatchIn(it) }
        some("an empty promotion list") { it.contains("promotions=[]") }
        some("a shop that admits everything") { it.contains("policy=AllOf()") }
        some("a shop that admits nothing") { it.contains("policy=AnyOf()") }
        some("a budget rule, where the shelf price is what gets compared") { it.contains("MaxBudget(") }
        some("a discount big enough to reach zero") { it.contains("Percentage(100)") }
        some("a percentage over 100") { it.contains("Percentage(150)") }
        some("a duck priced 0") { it.contains("price=0,") }
        some("a price at the top of the Int range") { it.contains("price=2147483647,") }
        // The one case where admission and pricing genuinely touch: a duck the shop takes, whose
        // price a promotion then drives to zero. Whether that is a quote of 0 or no quote at all is
        // the only decision this brief actually leaves open, so a corpus blind to it measures nothing.
        some("an admitted duck discounted all the way to zero") { line ->
            line.contains("policy=AllOf()") && line.contains("Percentage(100)")
        }
    }

    private fun corpus(): List<String> {
        val random = Random(SEED)
        val out = mutableListOf<String>()

        // Hand-picked first: every decision the corpus must be able to see, never left to chance.
        val fixed: List<Triple<Duck, AdmissionPolicy, List<DiscountRule>>> = listOf(
            Triple(duck(100), AllOf(emptyList()), emptyList()),
            Triple(duck(100), AnyOf(emptyList()), emptyList()),
            Triple(duck(100), AllOf(emptyList()), listOf(DiscountRule.Percentage(100))),
            Triple(duck(100), AllOf(emptyList()), listOf(DiscountRule.Percentage(150))),
            Triple(duck(0), AllOf(emptyList()), listOf(DiscountRule.Percentage(10))),
            Triple(duck(60), MaxBudget(50), listOf(DiscountRule.Percentage(25))),
            Triple(duck(50), MaxBudget(50), listOf(DiscountRule.Percentage(25))),
            Triple(duck(95), AllOf(emptyList()), listOf(DiscountRule.Percentage(10))),
            Triple(duck(100), KotlinOnly(), listOf(DiscountRule.AmountOff(250))),
            Triple(duck(100, kotlin = true), KotlinOnly(), listOf(DiscountRule.BigSpenderBonus(100, 20))),
            Triple(duck(Int.MAX_VALUE), AllOf(emptyList()), listOf(DiscountRule.Percentage(10))),
            Triple(
                duck(150),
                AllOf(emptyList()),
                listOf(DiscountRule.Percentage(50), DiscountRule.BigSpenderBonus(100, 20)),
            ),
            Triple(
                duck(100),
                Not(AllOf(emptyList())),
                listOf(DiscountRule.BestOf(listOf(DiscountRule.Percentage(10), DiscountRule.AmountOff(5)))),
            ),
        )
        fixed.forEach { (duck, policy, rules) -> out += line(duck, policy, rules) }

        repeat(CASES) {
            val price = when (random.nextInt(6)) {
                0 -> 0
                1 -> random.nextInt(1, 100)
                2 -> Int.MAX_VALUE
                else -> random.nextInt(1, 5_000)
            }
            val d = duck(price, random.nextBoolean(), accessories(random))
            val policy = randomPolicy(random, DEPTH)
            val rules = List(random.nextInt(0, MAX_RULES + 1)) { randomRule(random, 1) }
            out += line(d, policy, rules)
        }
        return out
    }

    // ── the two generators, one from each side of the seam ───────────────────────────────────────

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

    private fun randomRule(random: Random, depth: Int): DiscountRule = when (random.nextInt(if (depth <= 0) 3 else 6)) {
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
     * One recorded case. A throw is captured as a **value**, not a crash: an implementation that
     * rejects an input has made a decision, and a decision the other one did not make is exactly what
     * this is looking for. `none` is how `null` is rendered, so "no quote" and "a quote of 0" cannot
     * be confused in a diff.
     */
    private fun line(duck: Duck, policy: AdmissionPolicy, rules: List<DiscountRule>): String {
        val outcome = runCatching { quote(duck, Shop("s", policy), rules)?.toString() ?: "none" }
            .getOrElse { "threw ${it::class.simpleName}" }
        return "price=${duck.price}, kotlin=${duck.hasKotlinAttribute}, " +
            "accessories=${duck.accessories.size}, policy=${describe(policy)}, " +
            "promotions=[${rules.joinToString { describe(it) }}] => $outcome"
    }

    /**
     * Leaves are rendered **with their arguments**. The first version printed only the class name, and
     * the corpus guard caught it: a diff would have said two implementations disagree on a
     * `MaxBudget` without saying which budget, which is unreadable exactly when it matters.
     */
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

    // The combinators keep their children private, so the rendering reads them back through the only
    // door there is. Ugly, and the alternative — carrying a description alongside every generated
    // policy — made the generator twice the size for a string that is only ever printed.
    private fun children(policy: AdmissionPolicy): List<AdmissionPolicy> = policy.field("policies")
    private fun inner(policy: AdmissionPolicy): AdmissionPolicy = policy.field("policy")

    @Suppress("UNCHECKED_CAST")
    private fun <T> AdmissionPolicy.field(name: String): T =
        this::class.java.getDeclaredField(name).also { it.isAccessible = true }.get(this) as T

    private companion object {
        const val SEED = 20260818L
        const val CASES = 4_000
        const val MAX_RULES = 3
        const val MAX_WIDTH = 4
        const val DEPTH = 2
        val ACCESSORY_NAMES = listOf("Hat", "hat", "Scarf", "Bowtie")
    }
}
