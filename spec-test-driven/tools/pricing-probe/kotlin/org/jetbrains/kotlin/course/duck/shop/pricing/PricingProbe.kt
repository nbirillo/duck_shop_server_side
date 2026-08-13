package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Records what one implementation of `priceFor` answers on a fixed corpus of inputs, so that two
 * implementations of the **same specification** can be compared against each other and against the
 * reference.
 *
 * ### What this measures that the property catalog cannot
 *
 * Scoring an implementation against the reference stops discriminating once the implementer is
 * strong enough: a frontier agent fills a gap with the same default the reference chose, and a
 * specification that pins almost nothing scores the same as one that pins everything. We measured
 * exactly that — a spec with no rounding rule, no compounding rule and a self-contradictory bonus
 * came out 8/8, level with the best in the corpus.
 *
 * Two implementations of the same text separate the cases, because agreement is the measurement and
 * no model has to be believed about anything:
 *
 * | A vs B | vs reference | reading |
 * | --- | --- | --- |
 * | agree | agree | the specification settled it, the way we did |
 * | agree | differ | the specification settled it **differently**. Not a defect — the reference is one legal answer |
 * | differ | — | the specification **left it open**, and each agent filled the gap its own way |
 *
 * The third row is the number that says how much a specification leaves to chance, and it is the one
 * the reference-agreement score loses.
 *
 * ### The corpus, and why it is guarded
 *
 * Cases are pseudo-random but fully determined by [SEED], so two runs generate identical inputs and
 * any difference in the recording is a difference in behaviour. `the corpus reaches the values it
 * claims to` is not decoration: the 11.2 probe twice reported a real divergence as "identical"
 * because the generator never produced the inputs that would have shown it — once from too small a
 * combinator arity, once from too narrow an argument range. A blind corpus fails silently and looks
 * like good news.
 */
class PricingProbe {

    @Test
    fun record() {
        val target = File(System.getProperty("probe.out") ?: "build/probe/recording.txt")
        target.parentFile.mkdirs()
        target.writeText(corpus().joinToString("\n") + "\n")
        println("[probe] ${target.absolutePath}")
    }

    @Test
    fun `the corpus reaches the values it claims to`() {
        val cases = corpus()
        fun some(what: String, predicate: (String) -> Boolean) =
            assertTrue(cases.any(predicate), "no probed case has $what — the check is blind to it")

        some("an empty rule list") { it.contains("rules=[]") }
        some("a duck priced 0") { it.contains("price=0,") }
        some("a price at the top of the Int range") { it.contains("price=2147483647,") }
        some("a 0% discount") { it.contains("Percentage(0)") }
        some("a 100% discount") { it.contains("Percentage(100)") }
        some("a percentage above 100") { it.contains("Percentage(150)") }
        some("an amount larger than any price here") { it.contains("AmountOff(100000)") }
        some("a zero threshold") { it.contains("BigSpenderBonus(0,") }
        // The case that separates "the bonus reads the shelf price" from "the bonus reads the price
        // it is applied to" — without it, the single most contested decision in the corpus is
        // invisible and every implementation looks the same.
        some("a discount before a bonus whose threshold sits between the two prices") { line ->
            val i = line.indexOf("Percentage(50)")
            i >= 0 && line.indexOf("BigSpenderBonus(100,", i) > i
        }
        some("two rules of the same kind in a row") { it.contains("Percentage(10), Percentage(10)") }
    }

    private fun corpus(): List<String> {
        val random = Random(SEED)
        val out = mutableListOf<String>()

        // Hand-picked cases first: every decision the corpus has to be able to see, stated once and
        // never left to chance. The random cases below add breadth, not coverage of these.
        val fixed: List<Pair<Int, List<DiscountRule>>> = listOf(
            0 to emptyList(),
            100 to emptyList(),
            95 to listOf(DiscountRule.Percentage(10)),
            94 to listOf(DiscountRule.Percentage(10)),
            5 to listOf(DiscountRule.Percentage(10)),
            100 to listOf(DiscountRule.Percentage(0)),
            100 to listOf(DiscountRule.Percentage(100)),
            100 to listOf(DiscountRule.Percentage(150)),
            30 to listOf(DiscountRule.AmountOff(100000)),
            100 to listOf(DiscountRule.Percentage(10), DiscountRule.Percentage(10)),
            100 to listOf(DiscountRule.AmountOff(5), DiscountRule.Percentage(10)),
            100 to listOf(DiscountRule.Percentage(10), DiscountRule.AmountOff(5)),
            99 to listOf(DiscountRule.BigSpenderBonus(100, 20)),
            100 to listOf(DiscountRule.BigSpenderBonus(100, 20)),
            101 to listOf(DiscountRule.BigSpenderBonus(100, 20)),
            100 to listOf(DiscountRule.BigSpenderBonus(0, 20)),
            // Shelf price over the threshold, running price under it after the first rule.
            150 to listOf(DiscountRule.Percentage(50), DiscountRule.BigSpenderBonus(100, 20)),
            120 to listOf(DiscountRule.Percentage(50), DiscountRule.BigSpenderBonus(100, 20)),
            Int.MAX_VALUE to listOf(DiscountRule.Percentage(10)),
            Int.MAX_VALUE to listOf(DiscountRule.AmountOff(1)),
            2_000_000_000 to listOf(DiscountRule.Percentage(37)),
        )
        fixed.forEach { (price, rules) -> out += line(duck(price), rules) }

        repeat(CASES) {
            val price = when (random.nextInt(6)) {
                0 -> 0
                1 -> random.nextInt(1, 100)
                2 -> Int.MAX_VALUE
                else -> random.nextInt(1, 50_000)
            }
            val rules = List(random.nextInt(0, MAX_RULES + 1)) { randomRule(random) }
            out += line(duck(price, random.nextBoolean()), rules)
        }
        return out
    }

    private fun randomRule(random: Random): DiscountRule = when (random.nextInt(3)) {
        0 -> DiscountRule.Percentage(random.nextInt(0, 160))
        1 -> DiscountRule.AmountOff(random.nextInt(0, 2_000))
        else -> DiscountRule.BigSpenderBonus(random.nextInt(0, 3_000), random.nextInt(0, 500))
    }

    private fun duck(price: Int, kotlin: Boolean = false) = Duck("d", price, kotlin)

    /**
     * One recorded case. The result is captured as text so that a throw is a **value** and not a
     * crashed run: an implementation that rejects an input has made a decision, and a decision the
     * other implementation did not make is exactly what this is looking for.
     */
    private fun line(duck: Duck, rules: List<DiscountRule>): String {
        val outcome = runCatching { priceFor(duck, rules).toString() }
            .getOrElse { "threw ${it::class.simpleName}" }
        return "price=${duck.price}, kotlin=${duck.hasKotlinAttribute}, " +
            "rules=[${rules.joinToString { describe(it) }}] => $outcome"
    }

    private fun describe(rule: DiscountRule): String = when (rule) {
        is DiscountRule.Percentage -> "Percentage(${rule.percent})"
        is DiscountRule.AmountOff -> "AmountOff(${rule.amount})"
        is DiscountRule.BigSpenderBonus -> "BigSpenderBonus(${rule.threshold}, ${rule.amount})"
        is DiscountRule.Then -> "Then(${describe(rule.first)}, ${describe(rule.second)})"
        is DiscountRule.BestOf -> "BestOf[${rule.options.joinToString { describe(it) }}]"
        is DiscountRule.OnlyIf -> "OnlyIf(${rule.condition::class.simpleName}, ${describe(rule.rule)})"
    }

    private companion object {
        const val SEED = 20260813L
        const val CASES = 4_000
        const val MAX_RULES = 4
    }
}
