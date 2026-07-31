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
 * One entry of a mutant catalog: a single textual change applied to one `:core` source file.
 *
 * @property id the mutant's folder name, e.g. `max-budget-strict`.
 * @property file the `:core` file to mutate, relative to the admission package dir (e.g. `Leaves.kt`).
 * @property find the exact snippet to replace; must occur exactly once in [file].
 * @property replace what to put in its place.
 * @property bucket `must-kill` (a faithful suite has to kill it) or `spec-dependent`
 *   (survival is acceptable — the behaviour it changes is not part of the specification).
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

    companion object {
        const val MUST_KILL = "must-kill"
        const val SPEC_DEPENDENT = "spec-dependent"
    }
}

/** Reads a mutant catalog (see `mutants/catalog.json`). */
internal fun loadCatalog(catalog: File): List<Mutant> {
    require(catalog.isFile) { "Mutant catalog not found: $catalog" }
    val mutants = Json.parseToJsonElement(catalog.readText())
        .jsonObject["mutants"]
        ?.jsonArray
        ?: error("Mutant catalog $catalog has no \"mutants\" array")

    return mutants.map { element ->
        val o = element.jsonObject
        fun str(name: String): String =
            o[name]?.jsonPrimitive?.content ?: error("Mutant entry in $catalog is missing \"$name\": $o")
        fun optional(name: String): String = o[name]?.jsonPrimitive?.content.orEmpty()
        Mutant(
            id = str("id"),
            file = str("file"),
            find = str("find"),
            replace = str("replace"),
            bucket = str("bucket").also {
                require(it == Mutant.MUST_KILL || it == Mutant.SPEC_DEPENDENT) {
                    "Mutant '${str("id")}' has bucket '$it' (use ${Mutant.MUST_KILL} or ${Mutant.SPEC_DEPENDENT})"
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
