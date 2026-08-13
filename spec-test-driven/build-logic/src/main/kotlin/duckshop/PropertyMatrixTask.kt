package duckshop

import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Which property kills which mutant — the table that gives "load-bearing" a definition.
 *
 * `verifyMutants` answers "did this suite kill that mutant". This inverts it: for each property, the
 * mutants it is the reason for killing. That matters because of what it says about the ones with an
 * empty row.
 *
 * > **A claim is load-bearing exactly when the property derived from it kills at least one mutant.**
 *
 * A property that kills nothing is not necessarily false — it may be perfectly true and still
 * constrain no implementation anybody could have written. A specification full of those reads as
 * rigour and decides nothing, which is the failure the advanced tier of 11.2 called claiming a law
 * nothing can violate. Scoring a learner's spec then comes down to two numbers: the mutants the
 * claims it makes still kill, and how long it is.
 *
 * Reads the JUnit XML the mutant modules already produce, so it costs nothing extra to run.
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

        val baseline = failing(root.resolve("$out/$BASELINE/build/test-results/test"))
        if (baseline == null) {
            logger.lifecycle("The baseline never ran — generate the modules and run the suite first.")
            return
        }
        if (baseline.isNotEmpty()) {
            logger.lifecycle("")
            logger.lifecycle("${baseline.size} propert(y/ies) fail against the REFERENCE and are simply wrong:")
            baseline.sorted().forEach { logger.lifecycle("   $it") }
            logger.lifecycle("They are excluded below — a property that contradicts correct code proves nothing.")
        }

        val killedBy = linkedMapOf<String, MutableList<String>>()
        val notRun = mutableListOf<String>()
        catalog.filter { it.mustKill }.forEach { mutant ->
            val f = failing(root.resolve("$out/${mutant.id}/build/test-results/test"))
            if (f == null) { notRun += mutant.id; return@forEach }
            (f - baseline).forEach { killedBy.getOrPut(it) { mutableListOf() } += mutant.id }
        }

        val all = (killedBy.keys + everyTest(root.resolve("$out/$BASELINE/build/test-results/test"))).distinct().sorted()
        val width = (all.maxOfOrNull { it.length } ?: 0) + 2

        logger.lifecycle("")
        logger.lifecycle("Property → the mutants it kills")
        all.filterNot { it in baseline }.forEach { prop ->
            val kills = killedBy[prop].orEmpty()
            logger.lifecycle("  ${prop.padEnd(width)}${if (kills.isEmpty()) "— kills nothing" else kills.joinToString()}")
        }

        val idle = all.filterNot { it in baseline }.filter { killedBy[it].isNullOrEmpty() }
        val covered = killedBy.values.flatten().distinct()
        val uncovered = catalog.filter { it.mustKill }.map { it.id } - covered.toSet()

        logger.lifecycle("")
        logger.lifecycle("${covered.size}/${catalog.count { it.mustKill }} mutants are killed by some property.")
        if (uncovered.isNotEmpty()) {
            logger.lifecycle("No property kills: ${uncovered.joinToString()} — the catalog has a behaviour nothing claims.")
        }
        if (idle.isNotEmpty()) {
            logger.lifecycle("Kills nothing: ${idle.joinToString()}")
            logger.lifecycle("  ↳ true, perhaps, but constraining no implementation. Not load-bearing.")
        }
        if (notRun.isNotEmpty()) logger.lifecycle("Never ran: ${notRun.joinToString()}")
    }

    private fun everyTest(dir: File): List<String> =
        (dir.listFiles { f -> f.name.startsWith("TEST-") && f.extension == "xml" } ?: emptyArray())
            .flatMap { xml -> Regex("<testcase name=\"([^\"]*)\"").findAll(xml.readText()).map { it.groupValues[1].removeSuffix("()") } }

    private fun failing(dir: File): Set<String>? {
        val xmls = dir.listFiles { f -> f.name.startsWith("TEST-") && f.extension == "xml" } ?: return null
        if (xmls.isEmpty()) return null
        return xmls.flatMap { xml ->
            xml.readText().split("<testcase ").drop(1).mapNotNull { chunk ->
                val n = Regex("name=\"([^\"]*)\"").find(chunk)?.groupValues?.get(1) ?: return@mapNotNull null
                val body = chunk.substringBefore("</testcase>")
                if (body.contains("<failure") || body.contains("<error")) n.removeSuffix("()") else null
            }
        }.toSet()
    }
}
