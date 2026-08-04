package org.jetbrains.kotlin.course.duck.shop.admission

import java.io.File
import kotlin.random.Random
import kotlin.test.Test

/**
 * Records what THIS module's algebra answers for a fixed set of cases, so two modules can be
 * compared without ever loading both algebras at once — they declare the same classes in the same
 * package, so they can never share a classpath.
 *
 * Compiled into `attacks/reference/` (the unmodified `:core`) and into every `attacks/<agent>/`
 * module. Each writes `build/probe.txt`; `verifyAttack` diffs the two files, and the first line that
 * differs is a duck the two implementations disagree about. If the learner's suite passed against
 * the attacking implementation and such a line exists, the suite has a hole and that line is the
 * counterexample.
 *
 * The cases are pseudo-random but fully determined by [SEED], so the two runs generate exactly the
 * same list in exactly the same order. Nothing here reveals which behaviours matter: the generator
 * is uniform over the algebra, and the accessory names differ in case and in prefix only because a
 * generator that never produced near-misses could not tell any two implementations apart.
 */
class DifferentialProbe {

    @Test
    fun record() {
        val random = Random(SEED)
        val lines = (1..CASES).map { index ->
            val (policy, description) = randomPolicy(random, depth = 3)
            val duck = randomDuck(random)
            val verdict = runCatching { policy.admits(duck) }
                .fold({ it.toString() }, { "threw ${it::class.simpleName}" })
            "$index\t$description | ${describe(duck)} -> $verdict"
        }
        File("build/probe.txt").apply { parentFile.mkdirs() }.writeText(lines.joinToString("\n") + "\n")
    }

    private fun randomDuck(random: Random): Duck = Duck(
        name = "duck-${random.nextInt(100)}",
        price = random.nextInt(-2, 12),
        hasKotlinAttribute = random.nextBoolean(),
        accessories = List(random.nextInt(0, 4)) { Accessory(ACCESSORY_NAMES[random.nextInt(ACCESSORY_NAMES.size)]) },
    )

    /** A random policy tree, returned together with a rendering of itself — policies have no toString. */
    private fun randomPolicy(random: Random, depth: Int): Pair<AdmissionPolicy, String> {
        if (depth <= 0) return randomLeaf(random)
        return when (random.nextInt(6)) {
            0 -> children(random, depth).let { (policies, rendered) ->
                AllOf(policies) to "AllOf($rendered)"
            }
            1 -> children(random, depth).let { (policies, rendered) ->
                AnyOf(policies) to "AnyOf($rendered)"
            }
            2 -> randomPolicy(random, depth - 1).let { (policy, rendered) -> Not(policy) to "Not($rendered)" }
            else -> randomLeaf(random)
        }
    }

    private fun children(random: Random, depth: Int): Pair<List<AdmissionPolicy>, String> {
        val generated = List(random.nextInt(0, 3)) { randomPolicy(random, depth - 1) }
        return generated.map { it.first } to generated.joinToString(", ") { it.second }
    }

    private fun randomLeaf(random: Random): Pair<AdmissionPolicy, String> =
        when (random.nextInt(4)) {
            0 -> KotlinOnly() to "KotlinOnly()"
            1 -> random.nextInt(-1, 12).let { MaxBudget(it) to "MaxBudget($it)" }
            2 -> ACCESSORY_NAMES[random.nextInt(ACCESSORY_NAMES.size)]
                .let { RequiresAccessory(it) to "RequiresAccessory(\"$it\")" }
            else -> random.nextInt(0, 4).let { MinAccessories(it) to "MinAccessories($it)" }
        }

    private fun describe(duck: Duck): String =
        "Duck(price=${duck.price}, kotlin=${duck.hasKotlinAttribute}, " +
            "accessories=[${duck.accessories.joinToString(", ") { it.name }}])"

    private companion object {
        const val SEED = 20260804L
        const val CASES = 2000

        /**
         * Deliberately includes names that differ only in case, only by a prefix, and an empty one:
         * two implementations that disagree about exact matching have to be given the chance to.
         */
        val ACCESSORY_NAMES = listOf("hat", "Hat", "hatband", "scarf", "Scarf ", "")
    }
}
