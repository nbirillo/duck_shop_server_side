package duckshop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/** The single package every admission-policy source lives in. */
internal const val ADMISSION_PACKAGE_PATH = "org/jetbrains/kotlin/course/duck/shop/admission"

/**
 * Name of the generated module that runs the selected suite against the UNMUTATED `:core`. Mutation
 * scores only mean something once this one is green.
 */
internal const val BASELINE = "baseline"

/**
 * Every test in a module's JUnit XML results, whether it passed or failed. Empty if it never ran.
 */
internal fun everyTest(dir: File): List<String> =
    (dir.listFiles { f -> f.name.startsWith("TEST-") && f.extension == "xml" } ?: emptyArray())
        .flatMap { xml ->
            Regex("<testcase name=\"([^\"]*)\"").findAll(xml.readText())
                .map { it.groupValues[1].removeSuffix("()") }
        }

/**
 * The tests that failed in a module, or `null` if the module has no results at all — which is a
 * different fact from "nothing failed" and the callers treat it as one.
 */
internal fun failingTests(dir: File): Set<String>? {
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

/**
 * One entry of a mutant catalog: a single textual change applied to one `:core` source file.
 *
 * @property id the mutant's folder name, e.g. `max-budget-strict`.
 * @property file the `:core` file to mutate, relative to the admission package dir (e.g. `Leaves.kt`).
 * @property find the exact snippet to replace; must occur exactly once in [file].
 * @property replace what to put in its place.
 * @property bucket `must-kill` (a faithful suite has to kill it), `spec-dependent` (survival is
 *   acceptable — the behaviour it changes is not part of the specification), or `conformant`
 *   (a legal refactoring: the suite must stay GREEN, and a failure here is a FALSE ALARM — a test
 *   pinning an implementation detail the contract leaves free).
 * @property what one line describing the injected defect; **optional**.
 * @property hint what kind of test kills it; printed for survivors. **Optional**.
 *
 * `what` and `hint` are omitted from the catalog a learner can see: spelling out the defect and the
 * test that catches it would hand the exercise's answers to the learner — and to any agent reading
 * the folder. They are filled in only in the teacher-only catalog.
 */
internal data class Mutant(
    val id: String,
    val file: String,
    val find: String,
    val replace: String,
    val bucket: String,
    val what: String = "",
    val hint: String = "",
) {
    val mustKill: Boolean get() = bucket == MUST_KILL
    val conformant: Boolean get() = bucket == CONFORMANT

    companion object {
        const val MUST_KILL = "must-kill"
        const val SPEC_DEPENDENT = "spec-dependent"
        const val CONFORMANT = "conformant"

        val BUCKETS = listOf(MUST_KILL, SPEC_DEPENDENT, CONFORMANT)
    }
}

/**
 * Reads a catalog of generated modules — `mutants/catalog.json` (injected defects) or
 * `variants/catalog.json` (legal refactorings). The array is named after what the catalog holds, so
 * either key is accepted; the entries have the same shape and the bucket says which way to read them.
 */
internal fun loadCatalog(catalog: File): List<Mutant> {
    require(catalog.isFile) { "Catalog not found: $catalog" }
    val root = Json.parseToJsonElement(catalog.readText()).jsonObject
    val mutants = (root["mutants"] ?: root["variants"])
        ?.jsonArray
        ?: error("Catalog $catalog has no \"mutants\" or \"variants\" array")

    return mutants.map { element ->
        val o = element.jsonObject
        fun str(name: String): String =
            o[name]?.jsonPrimitive?.content ?: error("Entry in $catalog is missing \"$name\": $o")
        fun optional(name: String): String = o[name]?.jsonPrimitive?.content.orEmpty()
        Mutant(
            id = str("id"),
            file = str("file"),
            find = str("find"),
            replace = str("replace"),
            bucket = str("bucket").also {
                require(it in Mutant.BUCKETS) {
                    "Mutant '${str("id")}' has bucket '$it' (use one of ${Mutant.BUCKETS})"
                }
            },
            what = optional("what"),
            hint = optional("hint"),
        )
    }.also { list ->
        val duplicates = list.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Duplicate mutant id(s) in $catalog: $duplicates" }
    }
}
