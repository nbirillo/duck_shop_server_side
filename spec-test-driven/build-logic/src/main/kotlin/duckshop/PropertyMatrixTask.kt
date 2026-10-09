package duckshop

import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Shows which mutants each property catches. This is how we check whether a claim really matters.
 *
 * `verifyMutants` tells you whether the whole test suite catches a mutant. This report looks at it
 * from the other direction: which mutants does each individual property catch?
 *
 * **A claim is load-bearing if its property catches at least one mutant.**
 *
 * A property that catches nothing is not necessarily wrong. It might be true, but it does not rule
 * out any of the alternative implementations in our catalog. A long specification can sound very
 * precise without actually making useful decisions. That is why we look at both the mutants its
 * claims catch and how much the specification says.
 *
 * This task reads the JUnit XML results already produced by the mutant modules. No extra test runs
 * are needed.
 */
abstract class PropertyMatrixTask @Inject constructor(
    private val layout: ProjectLayout,
) : DefaultTask() {

    @get:Input
    abstract val catalogPath: Property<String>

    @get:Input
    abstract val outPath: Property<String>

    @TaskAction
    fun run() {
        val root = layout.projectDirectory.asFile
        val catalog = loadCatalog(root.resolve(catalogPath.get().trim('/')))
        val out = outPath.get().trim('/')

        val baseline = failingTests(root.resolve("$out/$BASELINE/build/test-results/test"))
        if (baseline == null) {
            logger.lifecycle("No reference test results yet. Generate the modules and run the tests first.")
            return
        }
        if (baseline.isNotEmpty()) {
            logger.lifecycle("")
            logger.lifecycle("${baseline.size} properties fail on the REFERENCE implementation:")
            baseline.sorted().forEach { logger.lifecycle("   $it") }
            logger.lifecycle("These are left out below. A property that fails on correct code cannot show that a mutant is wrong.")
        }

        val killedBy = linkedMapOf<String, MutableList<String>>()
        val notRun = mutableListOf<String>()
        catalog.filter { it.mustKill }.forEach { mutant ->
            val f = failingTests(root.resolve("$out/${mutant.id}/build/test-results/test"))
            if (f == null) { notRun += mutant.id; return@forEach }
            (f - baseline).forEach { killedBy.getOrPut(it) { mutableListOf() } += mutant.id }
        }

        val all = (killedBy.keys + everyTest(root.resolve("$out/$BASELINE/build/test-results/test"))).distinct().sorted()
        val width = (all.maxOfOrNull { it.length } ?: 0) + 2

        logger.lifecycle("")
        logger.lifecycle("Each property and the mutants it catches")
        all.filterNot { it in baseline }.forEach { prop ->
            val kills = killedBy[prop].orEmpty()
            logger.lifecycle("  ${prop.padEnd(width)}${if (kills.isEmpty()) "— catches no mutants" else kills.joinToString()}")
        }

        val idle = all.filterNot { it in baseline }.filter { killedBy[it].isNullOrEmpty() }
        val covered = killedBy.values.flatten().distinct()
        val uncovered = catalog.filter { it.mustKill }.map { it.id } - covered.toSet()

        logger.lifecycle("")
        logger.lifecycle("${covered.size}/${catalog.count { it.mustKill }} mutants are caught by at least one property.")
        if (uncovered.isNotEmpty()) {
            logger.lifecycle("No property catches these mutants: ${uncovered.joinToString()}. Your claims do not cover these behaviours.")
        }
        if (idle.isNotEmpty()) {
            logger.lifecycle("Properties that catch no mutants: ${idle.joinToString()}")
            logger.lifecycle("  ↳ They might be true, but they do not rule out any implementation here. They are not load-bearing.")
        }
        if (notRun.isNotEmpty()) logger.lifecycle("Mutants with no test results: ${notRun.joinToString()}")
    }
}
