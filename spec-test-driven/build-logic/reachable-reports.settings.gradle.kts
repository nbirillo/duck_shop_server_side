// Makes the aggregate reports REACHABLE when a module fails to compile. Applied from the
// settings.gradle.kts of every build that registers one of them.
//
// ### The problem this exists for
//
// Every report here aggregates over generated modules, and **"this module did not compile" is one of
// the verdicts it exists to print** — `verifyForks` has DID NOT COMPILE, `verifyImplementations` has
// DID NOT BUILD, `MutationReportTask` reports a mutant whose suite never built.
//
// Wired with `dependsOn(":<dir>:<module>:test")`, none of those branches could ever run. Gradle skips
// a task whose dependency failed, and `--continue` does not change that: it keeps going with the
// tasks that are NOT downstream of the failure, and a report is downstream by construction. So a
// compile error anywhere in the fan-out took the report with it.
//
// Measured, not assumed: one type error in the learner's own test suite made `./gradlew verifyMutants
// --continue` — the command printed on the slides — emit six identical compiler stack traces and no
// report at all. Which is the single most likely state for somebody in the middle of writing tests.
//
// ### The fix
//
// The report declares ORDER ONLY (`mustRunAfter`, in the convention plugin and in the grading build),
// so it is no longer downstream of anything that can fail. Scheduling then has to come from
// somewhere, and that is this file: when a report is on the command line, the per-module `test` tasks
// are requested alongside it, and continue-on-failure is switched on so one bad module does not stop
// the others. The build still exits non-zero — something did fail — but the report gets printed.
//
// Continue-on-failure is set here rather than left to the learner because forgetting `--continue` is
// otherwise indistinguishable from the bug above. The flag stays in the printed commands, where it
// documents the intent; this just stops it from being load-bearing.
//
// ### The safety net, and why it is enough
//
// `mustRunAfter` is an ordering rule, not a guarantee that anything ran. If this file ever stops
// matching a report — a renamed task, a new one — the report does not silently pass: it reads test
// results from disk and says so. `verifyForks` answers NO TESTS YET / RAN NO TESTS, and refuses a
// `-PforkTests` that disagrees with the `generated-with.txt` the modules were built under;
// `MutationReportTask` reports a mutant with no results rather than a dead one. A missing entry here
// shows up as "nothing ran", never as a clean score.
//
// ### Directories come from properties, never from literals
//
// Four defects in this repository have been a hard-coded name standing in for a configured one
// (`mutantTests`, `mutantsCoreSrc`, `specSurface`, and `verifyForks`'s own `":forks:"` prefix — that
// last one reported a full fork verdict for a catalog whose tests had never run). So each entry below
// reads the same gradle property its task reads, and falls back to the same default.

// report task -> (property naming its generated directory, that property's default)
val aggregated = mapOf(
    "verifyMutants" to ("mutantsOut" to "mutants"),
    "verifyVariants" to ("variantsOut" to "variants"),
    "verifyForks" to ("forksOut" to "forks"),
    // Set with set() in the grading build rather than by property, hence no property name to read.
    "verifyPricingMutants" to ("" to "pricing-mutants"),
    // Both of these depend on verifyPricingMutants, so they need the same modules scheduled.
    "pricingPropertyMatrix" to ("" to "pricing-mutants"),
    "verifyClaims" to ("" to "pricing-mutants"),
    "verifyImplementations" to ("" to "implementations"),
    "verifyAttack" to ("" to "attacks"),
)

// Reports scoped to one run by -Pagent. Requesting every module under their directory would compile
// dozens of unrelated agent outputs, so the filter the task itself applies is applied here too.
// `verifyAttack` also needs attacks/reference — the unmodified algebra it differentials against.
val scopedByAgent = mapOf(
    "verifyImplementations" to emptyList<String>(),
    "verifyAttack" to listOf("reference"),
)

val properties = gradle.startParameter.projectProperties
val requested = gradle.startParameter.taskNames

/**
 * Every `<path>:test` under [dir], at whatever depth the build script sits (implementations/ is two).
 *
 * Resolved against [settingsDir], NOT with `file()`: this script is applied from another directory,
 * and `file()` would resolve relative to the script's own folder. It did, and silently — every
 * lookup found nothing, so no suite was scheduled and the report read the PREVIOUS run's results and
 * printed a clean 1/4 for a suite that no longer compiled. Caught by `--dry-run` showing zero
 * scheduled test tasks, which is the check worth repeating if this file is ever moved.
 */
fun testTasksUnder(dir: String, keep: List<String>?): List<String> {
    val root = settingsDir.resolve(dir)
    if (!root.isDirectory) return emptyList()
    val found = mutableListOf<String>()
    fun walk(candidate: java.io.File, path: String) {
        if (candidate.resolve("build.gradle.kts").isFile) {
            found += "$path:test"
            return
        }
        candidate.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedBy { it.name }
            ?.forEach { walk(it, "$path:${it.name}") }
    }
    root.listFiles()
        ?.filter { it.isDirectory }
        ?.filter { keep == null || it.name in keep }
        ?.sortedBy { it.name }
        ?.forEach { walk(it, ":$dir:${it.name}") }
    return found
}

val suites = aggregated
    .filterKeys { task -> requested.any { it.substringAfterLast(':') == task } }
    .flatMap { (task, where) ->
        val (property, default) = where
        val dir = property.takeIf { it.isNotEmpty() }?.let { properties[it] } ?: default
        val keep = scopedByAgent[task]?.let { extra ->
            // No -Pagent means the task reports on everything, so schedule everything.
            properties["agent"]?.let { listOf(it) + extra }
        }
        testTasksUnder(dir, keep)
    }
    .distinct()

if (suites.isNotEmpty()) {
    gradle.startParameter.setTaskNames(suites + requested)
    gradle.startParameter.isContinueOnFailure = true
}
