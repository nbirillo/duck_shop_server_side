package duckshop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * The fork-discrimination engine for the 11.6 capstone — the fourth direction of the same idea, and
 * the one the capstone is actually graded on.
 *
 * ### Why it had to exist
 *
 * The capstone was planned as "does the learner's work pass a hidden set of key corner cases". Then it
 * was measured: **every artifact we produced passes every settled corner case**, including a
 * specification that disagrees with the reference on 884 of 3010 probed inputs. All the divergence sat
 * in the forks the brief deliberately leaves open, and two independently written weak specifications
 * missed *exactly the same three*. A hidden corner-case set therefore grades nothing.
 *
 * ### What replaces it
 *
 * A mutant asks *"does your suite catch a defect?"* A **reading** asks *"does your suite express a
 * decision?"* — because on an open fork there is no defect to catch, only a choice nobody made.
 *
 *   **A fork is settled exactly when the suite accepts one reading of it and rejects the others.**
 *
 * Accepting several is not a failure to detect a bug; it is a specification that never said anything.
 * Accepting none means the suite contradicts every reading we hold, which is the baseline-invalid
 * situation and makes the rest of the number meaningless — the same discipline the mutation report
 * learned when a flawed starter suite scored a fake 4/4.
 *
 * ### It is the mirror of the conformance check, and that pairing is the lesson
 *
 * `verifyVariants` says *do not pin what the contract leaves free*. `verifyForks` says *do pin what is
 * a decision*. Both fail a suite that cannot tell the two apart, and telling them apart is the whole
 * skill the capstone is for. Neither check names a right answer.
 */
internal data class Reading(
    /** Module folder name, e.g. `fork1-chain-ignored`. */
    val id: String,
    /** Which fork of the brief this is a reading of — readings sharing a fork are scored together. */
    val fork: String,
    /** How this reading answers the fork, in the learner's own terms. Never "wrong": it is a choice. */
    val label: String,
    /** The `bestOffer` source implementing this reading, relative to the catalog's directory. */
    val source: String,
    /** Teacher-only: why the brief admits this reading. Omitted from a catalog a learner can see. */
    val note: String = "",
)

/** One fork's outcome. [accepted] are the readings the suite let through. */
internal data class ForkVerdict(
    val fork: String,
    val accepted: List<Reading>,
    val rejected: List<Reading>,
    val unbuilt: List<Reading>,
) {
    val settled: Boolean get() = accepted.size == 1 && unbuilt.isEmpty()
    val contradictory: Boolean get() = accepted.isEmpty() && unbuilt.isEmpty()
    val leftOpen: Boolean get() = accepted.size > 1
}

internal fun loadForkCatalog(catalog: File): List<Reading> {
    require(catalog.isFile) { "Fork catalog not found: $catalog" }
    val root = Json.parseToJsonElement(catalog.readText()).jsonObject
    val readings = root["readings"]?.jsonArray
        ?: error("Catalog $catalog has no \"readings\" array")

    return readings.map { element ->
        val o = element.jsonObject
        fun str(name: String): String =
            o[name]?.jsonPrimitive?.content ?: error("Entry in $catalog is missing \"$name\": $o")
        Reading(
            id = str("id"),
            fork = str("fork"),
            label = str("label"),
            source = str("source"),
            note = o["note"]?.jsonPrimitive?.content.orEmpty(),
        )
    }.also { list ->
        val duplicates = list.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "Duplicate reading id(s) in $catalog: $duplicates" }
        // A fork with one reading can never be settled OR left open — it would silently report
        // "settled" for a suite that pins nothing, which is the exact failure this engine exists to
        // stop. The mutant engine learned the same lesson as "a score needs a valid baseline".
        val lonely = list.groupBy { it.fork }.filterValues { it.size < 2 }.keys
        require(lonely.isEmpty()) {
            "Fork(s) $lonely have fewer than 2 readings in $catalog — a fork with one reading cannot " +
                "discriminate anything, so it would report a suite that decides nothing as settled."
        }
    }
}

/**
 * The build script of one reading's module.
 *
 * Self-contained on purpose: the reading and the pricing it needs are **copied in** rather than picked
 * up from a source directory. Adding the reference's whole source directory would drag its own
 * `bestOffer` along and every module would fail with `Redeclaration:` — the same trap the mutant
 * generator sidesteps by writing `…Mutated.kt`.
 *
 * `ignoreFailures` is on because a red test here is the measurement, not a broken build: a suite that
 * rejects a reading is exactly what "this fork is settled" looks like.
 */
internal fun forkBuildScript(coreSrc: String, testsSrc: String): String {
    val tests = if (testsSrc.isEmpty()) "" else "kotlin.srcDir(rootDir.resolve(\"$testsSrc\"))"
    return """
    // GENERATED — do not edit by hand. Edit the catalog and re-run generateForks.
    //
    // One READING of a fork the brief leaves open, compiled with the suite under test. A failing test
    // means the suite rejects this reading, which is how a decision becomes visible.

    plugins {
        kotlin("jvm")
    }

    kotlin {
        jvmToolchain(21)

        sourceSets.named("main") {
            kotlin.srcDir(rootDir.resolve("$coreSrc"))
        }
        sourceSets.named("test") {
            $tests
        }
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation(kotlin("test"))
    }

    tasks.test {
        useJUnitPlatform()
        ignoreFailures = true
    }

    """.trimIndent()
}
