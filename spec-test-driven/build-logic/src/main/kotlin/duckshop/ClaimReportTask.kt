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
 * **The appeal.** Scores a claim set the learner declares for themselves, instead of the one a model
 * read out of their prose.
 *
 * Layer 2 asks a model which claims a specification makes, and it is wrong often enough to matter:
 * measured against the hand key it misses a stated claim about one time in eight, and its single
 * worst habit is answering NO to a bound written as an inequality rather than as a sentence. A
 * learner whose correct spec is marked incomplete needs something better than "argue with the
 * report", so:
 *
 * > Declare the claims yourself, and this task runs **exactly those** properties against the
 * > reference and the mutants. From that point on no model is in the loop, and the answer is the
 * > same one the grading build would compute.
 *
 * It also prints the **disagreement** with the extraction, when one is on file, and that is the part
 * worth reading. Three cases, and they mean different things:
 *
 *  - *declared, not extracted* — either the checker misread you, or you are claiming something your
 *    text implies to you and states to nobody else. Both are worth knowing; only the sentence tells
 *    you which, so the task points at the sentence rather than deciding for you.
 *  - *extracted, not declared* — you said more than you meant to, and something downstream is now
 *    pinned that you never chose to pin.
 *  - *agreed* — the boring case, and the common one.
 *
 * Declaring a claim does not make it true. The property still has to pass against the reference, and
 * it still has to kill something to be load-bearing; a declaration that fails on correct code is a
 * claim that contradicts the feature, which the report says in as many words.
 *
 * Params: `-Pclaims=<ids>` or a `claims.txt` beside the spec, `[-Pagent=<extraction to compare>]`,
 * `[-Pspec=<SPEC.md>]`.
 */
abstract class ClaimReportTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    /** The property catalog, whose test names are the claim ids. */
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
        logger.lifecycle("Declared claims — no model in this report")
        logger.lifecycle("${declared.size} of ${claims.size} claims declared")

        compareWithExtraction(root, claims, declared)
        scoreAgainstMutants(root, claims, declared)
    }

    // ── what the learner declared ────────────────────────────────────────────────────────────────

    /**
     * Accepts claim **numbers** (`2,4,7`) and claim **names**, in full or as any unambiguous
     * fragment (`raises the price`). Names are the better habit — they survive a change to the
     * catalog, where a number silently comes to mean a different claim — but numbers are what the
     * report prints, so refusing them would be perverse.
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
                "Nothing declared. Either -Pclaims=<ids or names, comma-separated>, or a claims.txt " +
                    "beside your spec with one claim per line. The claims:\n" +
                    claims.mapIndexed { i, c -> "  ${i + 1}. $c" }.joinToString("\n"),
            )
        }

        val wanted = raw.map { it.trim().trim('-', '*', ' ') }.filter { it.isNotEmpty() }
        require(wanted.isNotEmpty()) { "The claim declaration is empty." }

        return wanted.map { token -> resolve(token, claims) }.toSortedSet()
    }

    private fun resolve(token: String, claims: List<String>): Int {
        token.toIntOrNull()?.let { n ->
            require(n in 1..claims.size) {
                "Claim $n does not exist — the catalog has ${claims.size}."
            }
            return n
        }
        val hits = claims.withIndex().filter { (_, name) -> name.contains(token, ignoreCase = true) }
        require(hits.isNotEmpty()) {
            "No claim matches \"$token\". The catalog:\n" +
                claims.mapIndexed { i, c -> "  ${i + 1}. $c" }.joinToString("\n")
        }
        require(hits.size == 1) {
            "\"$token\" matches ${hits.size} claims and must not be guessed at:\n" +
                hits.joinToString("\n") { "  ${it.index + 1}. ${it.value}" }
        }
        return hits.first().index + 1
    }

    // ── the disagreement ─────────────────────────────────────────────────────────────────────────

    private fun compareWithExtraction(root: File, claims: List<String>, declared: Set<Int>) {
        val who = agent.orNull ?: providers.gradleProperty("agent").orNull ?: return
        val spec = providers.gradleProperty("spec").orNull ?: return
        val parts = spec.trimEnd('/').removeSuffix(".md").split('/').filter { it.isNotEmpty() }
        val file = root.resolve("extractions/$who/${parts.takeLast(2).joinToString("/")}.txt")
        if (!file.isFile) {
            logger.lifecycle("No extraction on file for $who — declaration reported on its own.")
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
        logger.lifecycle("Against the extraction ($who): ${(declared intersect extracted).size} agreed")
        if (contested.isEmpty() && unclaimed.isEmpty()) {
            logger.lifecycle("   no disagreement.")
            return
        }
        contested.forEach {
            logger.lifecycle("   CONTESTED  #$it ${claims[it - 1]}")
        }
        if (contested.isNotEmpty()) {
            logger.lifecycle("     ↳ you say your spec states these and the checker did not find them. Go and")
            logger.lifecycle("       find the sentence. If it is there, the checker was wrong and the run below")
            logger.lifecycle("       stands. If what you find is vaguer than you remembered, the checker read it")
            logger.lifecycle("       the way a stranger would — which is the more useful result of the two.")
        }
        unclaimed.forEach {
            logger.lifecycle("   NOT DECLARED  #$it ${claims[it - 1]}")
        }
        if (unclaimed.isNotEmpty()) {
            logger.lifecycle("     ↳ your text says this and your declaration does not. Either it is a claim you")
            logger.lifecycle("       did not mean to make, or one you forgot you made. Both pin an implementation.")
        }
    }

    // ── what the declared claims are worth ───────────────────────────────────────────────────────

    private fun scoreAgainstMutants(root: File, claims: List<String>, declared: Set<Int>) {
        val out = outPath.get().trim('/')
        val baselineDir = root.resolve("$out/$BASELINE/build/test-results/test")
        val baseline = failingTests(baselineDir)
        if (baseline == null) {
            logger.lifecycle("")
            logger.lifecycle("The properties have not been run, so what these claims are worth is unknown.")
            logger.lifecycle("Run the property suite against the reference and the mutants first.")
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
        logger.lifecycle("Each declared claim, run as a property:")
        val wrong = mutableListOf<Int>()
        val idle = mutableListOf<Int>()
        declared.forEach { n ->
            val name = claims[n - 1]
            val kills = killedBy[name].orEmpty()
            val verdict = when {
                name in baseline -> { wrong += n; "FAILS ON THE REFERENCE — the claim contradicts correct code" }
                name !in ran -> "never ran"
                kills.isEmpty() -> { idle += n; "holds, kills nothing" }
                else -> "holds, kills ${kills.joinToString()}"
            }
            logger.lifecycle("   #$n ${name.padEnd(claims.maxOf { it.length } + 2)}$verdict")
        }

        val covered = declared.map { claims[it - 1] }.flatMap { killedBy[it].orEmpty() }.distinct()
        logger.lifecycle("")
        logger.lifecycle("Your claims kill ${covered.size}/${catalog.size} mutants.")
        val missed = catalog.map { it.id } - covered.toSet()
        if (missed.isNotEmpty()) {
            logger.lifecycle("Nothing you claimed catches: ${missed.joinToString()}")
            logger.lifecycle("  ↳ a behaviour the feature has and your specification does not decide.")
        }
        if (idle.isNotEmpty()) {
            logger.lifecycle("Claimed and load-bearing on nothing: ${idle.joinToString { "#$it" }}")
            logger.lifecycle("  ↳ may be perfectly true and still constrains no implementation anybody would write.")
        }
        if (wrong.isNotEmpty()) {
            logger.lifecycle("")
            logger.lifecycle("Declaring a claim does not make it true: ${wrong.joinToString { "#$it" }} fail")
            logger.lifecycle("against the reference. A specification can be wrong, and this is what that looks like.")
        }
    }

    private fun claimNames(file: File): List<String> {
        require(file.isFile) { "Property catalog not found: $file" }
        val names = Regex("fun `([^`]+)`").findAll(file.readText()).map { it.groupValues[1] }.toList()
        require(names.isNotEmpty()) { "No properties found in $file" }
        return names
    }
}
