package duckshop

import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Lets you check the claims you say your specification makes.
 *
 * Normally, a model reads your spec and decides which claims it contains. But that
 * reading can be wrong. In our checks, it missed roughly one stated claim in eight,
 * especially when a limit was written as an inequality.
 *
 * Here you can declare the claims yourself. This task runs those exact properties
 * against the reference and the mutants. No model decides the result.
 *
 * If a model extraction is available, the report also compares it with your list:
 *
 * - Declared but not extracted: the model may have missed a claim, or your wording
 *   may not be clear enough to another reader. Check what the spec actually says.
 * - Extracted but not declared: the spec may promise more than you intended.
 * - Agreed: both lists include the same claim.
 *
 * Declaring a claim doesn't make it correct. It still has to pass against the
 * reference. And to catch a mutant, it needs to rule out a real mistake.
 *
 * Use `-Pclaims=<ids>` or a `claims.txt` next to the spec. You can also pass
 * `-Pagent=<extraction to compare>` and `-Pspec=<SPEC.md>`.
 */
abstract class ClaimReportTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    /** Property tests. Their names are the claim IDs. */
    @get:Input
    abstract val propertyFile: Property<String>

    @get:Input
    abstract val catalogPath: Property<String>

    @get:Input
    abstract val outPath: Property<String>

    @get:Input
    @get:Optional
    abstract val agent: Property<String>

    @TaskAction
    fun run() {
        val root = layout.projectDirectory.asFile
        val claims = claimNames(root.resolve(propertyFile.get()))
        val declared = declared(root, claims)

        logger.lifecycle("")
        logger.lifecycle("Your declared claims — checked without a model")
        logger.lifecycle("You declared ${declared.size} of ${claims.size} claims")

        compareWithExtraction(root, claims, declared)
        scoreAgainstMutants(root, claims, declared)
    }

    // ── Claims chosen by the learner ─────────────────────────────────────────────────────────────

    /**
     * You can select claims by number (`2,4,7`), full name, or a unique part of a name
     * (`raises the price`). Names are safer because numbers can change when the catalog changes.
     */
    private fun declared(root: File, claims: List<String>): Set<Int> {
        val inline = providers.gradleProperty("claims").orNull
        val file = providers.gradleProperty("claimsFile").orNull?.let { root.resolve(it) }
            ?: providers.gradleProperty("spec").orNull
                ?.let { root.resolve(it) }
                ?.let { if (it.isDirectory) it else it.parentFile }
                ?.resolve("claims.txt")

        val raw = when {
            inline != null -> inline.split(',', '\n')
            file != null && file.isFile -> {
                logger.lifecycle("Read from ${file.relativeTo(root)}")
                file.readLines().map { it.substringBefore('#') }
            }
            else -> error(
                "No claims provided. Use -Pclaims=<ids or names, comma-separated>, or put a claims.txt " +
                        "next to your spec with one claim per line. Available claims:\n" +
                        claims.mapIndexed { i, c -> "  ${i + 1}. $c" }.joinToString("\n"),
            )
        }

        val wanted = raw.map { it.trim().trim('-', '*', ' ') }.filter { it.isNotEmpty() }
        require(wanted.isNotEmpty()) { "Your claim list is empty." }

        return wanted.map { token -> resolve(token, claims) }.toSortedSet()
    }

    private fun resolve(token: String, claims: List<String>): Int {
        token.toIntOrNull()?.let { n ->
            require(n in 1..claims.size) {
                "Claim $n is not in the catalog. There are ${claims.size} claims."
            }
            return n
        }
        val hits = claims.withIndex().filter { (_, name) -> name.contains(token, ignoreCase = true) }
        require(hits.isNotEmpty()) {
            "No claim matches \"$token\". Available claims:\n" +
                    claims.mapIndexed { i, c -> "  ${i + 1}. $c" }.joinToString("\n")
        }
        require(hits.size == 1) {
            "\"$token\" matches ${hits.size} claims. Please use a more specific name:\n" +
                    hits.joinToString("\n") { "  ${it.index + 1}. ${it.value}" }
        }
        return hits.first().index + 1
    }

    // ── Differences from the model extraction ───────────────────────────────────────────────────

    private fun compareWithExtraction(root: File, claims: List<String>, declared: Set<Int>) {
        val who = agent.orNull ?: providers.gradleProperty("agent").orNull ?: return
        val spec = providers.gradleProperty("spec").orNull ?: return
        val parts = spec.trimEnd('/').removeSuffix(".md").split('/').filter { it.isNotEmpty() }
        val file = root.resolve("extractions/$who/${parts.takeLast(2).joinToString("/")}.txt")
        if (!file.isFile) {
            logger.lifecycle("No model extraction found for $who. Showing your declared claims only.")
            return
        }

        val extracted = Regex("""^\s*(\d+)\s*:\s*(\w+)""", RegexOption.MULTILINE)
            .findAll(file.readText())
            .filter { it.groupValues[2].uppercase().startsWith("Y") }
            .map { it.groupValues[1].toInt() }
            .toSet()

        val contested = declared - extracted
        val unclaimed = extracted - declared

        logger.lifecycle("")
        logger.lifecycle("Compared with the model extraction ($who): ${(declared intersect extracted).size} claims match")
        if (contested.isEmpty() && unclaimed.isEmpty()) {
            logger.lifecycle("   No differences found.")
            return
        }
        contested.forEach {
            logger.lifecycle("   CONTESTED  #$it ${claims[it - 1]}")
        }
        if (contested.isNotEmpty()) {
            logger.lifecycle("     ↳ You declared these claims, but the model did not find them. Check")
            logger.lifecycle("       the wording in your spec. If the claim is clearly there, the model")
            logger.lifecycle("       missed it. If the wording is vague, another reader might miss it too.")
            logger.lifecycle("       The checks below still use the claims you declared.")
        }
        unclaimed.forEach {
            logger.lifecycle("   NOT DECLARED  #$it ${claims[it - 1]}")
        }
        if (unclaimed.isNotEmpty()) {
            logger.lifecycle("     ↳ The model found these claims in your spec, but you did not declare them.")
            logger.lifecycle("       Check whether you meant to promise this behavior.")
        }
    }

    // ── Check the declared claims against real code ──────────────────────────────────────────────

    private fun scoreAgainstMutants(root: File, claims: List<String>, declared: Set<Int>) {
        val out = outPath.get().trim('/')
        val baselineDir = root.resolve("$out/$BASELINE/build/test-results/test")
        val baseline = failingTests(baselineDir)
        if (baseline == null) {
            logger.lifecycle("")
            logger.lifecycle("The property tests have not run yet, so these claims cannot be checked.")
            logger.lifecycle("Run the property tests against the reference and mutants first.")
            return
        }

        val catalog = loadCatalog(root.resolve(catalogPath.get().trim('/'))).filter { it.mustKill }
        val killedBy = linkedMapOf<String, MutableList<String>>()
        catalog.forEach { mutant ->
            val failed = failingTests(root.resolve("$out/${mutant.id}/build/test-results/test")) ?: return@forEach
            (failed - baseline).forEach { killedBy.getOrPut(it) { mutableListOf() } += mutant.id }
        }

        val ran = everyTest(baselineDir).toSet()
        logger.lifecycle("")
        logger.lifecycle("Results for each claim you declared:")
        val wrong = mutableListOf<Int>()
        val idle = mutableListOf<Int>()
        declared.forEach { n ->
            val name = claims[n - 1]
            val kills = killedBy[name].orEmpty()
            val verdict = when {
                name in baseline -> { wrong += n; "FAILS ON THE REFERENCE — this claim disagrees with correct code" }
                name !in ran -> "not run"
                kills.isEmpty() -> { idle += n; "passes, but catches no mutants" }
                else -> "passes, catches ${kills.joinToString()}"
            }
            logger.lifecycle("   #$n ${name.padEnd(claims.maxOf { it.length } + 2)}$verdict")
        }

        val covered = declared.map { claims[it - 1] }.flatMap { killedBy[it].orEmpty() }.distinct()
        logger.lifecycle("")
        logger.lifecycle("Your claims catch ${covered.size}/${catalog.size} mutants.")
        val missed = catalog.map { it.id } - covered.toSet()
        if (missed.isNotEmpty()) {
            logger.lifecycle("Mutants not caught by your claims: ${missed.joinToString()}")
            logger.lifecycle("  ↳ These changes are not ruled out by the claims you declared.")
        }
        if (idle.isNotEmpty()) {
            logger.lifecycle("Claims that catch no mutants: ${idle.joinToString { "#$it" }}")
            logger.lifecycle("  ↳ They may be correct, but they do not catch any changes in this catalog.")
        }
        if (wrong.isNotEmpty()) {
            logger.lifecycle("")
            logger.lifecycle("Declaring a claim does not make it correct: ${wrong.joinToString { "#$it" }} fail")
            logger.lifecycle("against the reference. Check whether your spec promises the wrong behavior.")
        }
    }

    private fun claimNames(file: File): List<String> {
        require(file.isFile) { "Cannot find the property catalog: $file" }
        val names = Regex("fun `([^`]+)`").findAll(file.readText()).map { it.groupValues[1] }.toList()
        require(names.isNotEmpty()) { "No property tests found in $file" }
        return names
    }
}
