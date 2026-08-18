package duckshop

import java.io.File

/**
 * Removes an agent's redeclarations of types that **already exist** on its compile path.
 *
 * ### Why the harness rescues this at all
 *
 * Same principle as the `package` / `// FILE:` normalisation in `RunAgentTask`: *we grade the agent's
 * logic, not its plumbing.* A model that reconstructs `data class Franchise(...)` from a field list it
 * was shown for reference has not made a pricing decision — it has failed a formatting contract, and
 * the resulting `Redeclaration:` hides every real finding behind a build failure.
 *
 * ### Why it is structural instead of another sentence in the prompt
 *
 * The 11.6 implementation surface was reworded **four times** against this exact symptom — declarations
 * shown in full got copied; then case names needed qualifying; then field types; then packages — and it
 * still fired on 4 of 6 specs while the same model, same surface, stayed clean on the other 2. It is
 * stochastic, so no wording closes it. What does close it: the file is compiled against a known set of
 * types, so *ask that set* rather than describe it.
 *
 * ### What it deliberately does NOT do
 *
 * It only removes a type whose name really is declared in the types source set. A helper type the agent
 * invented stays, and so does every line of its logic. Every removal is returned so the caller can print
 * it and record it next to the module: **a rescue that leaves no trace would let a broken output contract
 * read as a clean run** — which is exactly how the extraction-filename collision produced a fake 18/20.
 *
 * ### The one removal that is NOT neutral, and is opt-in for that reason
 *
 * [existingFunctions] strips a redeclared **function**, which changes behaviour: the agent's own calls
 * then resolve to the existing implementation instead of the copy it wrote. That is scoping, not
 * normalisation, so it happens only where a caller names a directory as out of scope — see
 * `-PimplExisting`. Say so wherever the resulting artifact is described; do not present it as neutral.
 */
internal object Redeclarations {

    /** One removal, kept so the caller can report it. */
    internal data class Rescue(val what: String, val detail: String)

    internal data class Result(val code: String, val rescues: List<Rescue>) {
        val clean: Boolean get() = rescues.isEmpty()
    }

    /**
     * Maps every top-level type declared under [typesDir] to the package it lives in.
     *
     * Derived, not listed: a hand-kept list is the same guess that failed four times, and it would go
     * stale the moment `:core` gains a type.
     */
    internal fun existingTypes(typesDir: File): Map<String, String> {
        if (!typesDir.isDirectory) return emptyMap()
        val found = mutableMapOf<String, String>()
        typesDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            val pkg = PACKAGE.find(text)?.groupValues?.get(1) ?: ""
            topLevelDeclarations(strippedOfCommentsAndStrings(text)).forEach { (name, _) ->
                found.putIfAbsent(name, pkg)
            }
        }
        return found
    }

    /**
     * Names of top-level **functions** declared under [dir].
     *
     * Separate from [existingTypes] because stripping a redeclared function is a stronger act: the
     * agent's version has a body, and removing it changes which implementation its own calls resolve to.
     * So this is only ever applied to a directory the caller states is **out of scope** — in the capstone,
     * `priceFor`, which was settled two exercises earlier and which the surface tells the agent to use
     * rather than write.
     *
     * It is needed because they write it anyway. Both `qwen2.5-coder:32b` and `:14b` reimplemented
     * `priceFor` from a surface saying "ALREADY WRITTEN AND SETTLED — use it, do not reimplement
     * pricing", and each got it wrong in its own way (an Int overflow and an exclusive threshold in one,
     * a big-spender bonus that ADDED money and a reversed `Then` in the other). That is the same failure
     * family as reconstructing a type from its field list: shown a declaration, a model writes one.
     */
    internal fun existingFunctions(dir: File): Set<String> {
        if (!dir.isDirectory) return emptySet()
        return dir.walkTopDown().filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                topLevelFunctions(strippedOfCommentsAndStrings(file.readText())).map { it.first }
            }
            .toSet()
    }

    /**
     * Drops redeclarations of [existing] types, and imports of those types from the wrong package.
     *
     * The import half is not scope creep: after a declaration is removed, an
     * `import ….admission.Franchise` for a type that lives in `….pricing` is left behind pointing at
     * nothing, so the rescue would not actually rescue anything. Only demonstrably wrong imports go —
     * a correct one is left exactly as written.
     */
    internal fun strip(
        code: String,
        existing: Map<String, String>,
        existingFunctions: Set<String> = emptySet(),
    ): Result {
        if (existing.isEmpty() && existingFunctions.isEmpty()) return Result(code, emptyList())
        val rescues = mutableListOf<Rescue>()

        // Functions first: a stripped function must not take a type declaration's span with it.
        var withFunctions = code
        while (true) {
            val masked = strippedOfCommentsAndStrings(withFunctions)
            val hit = topLevelFunctions(masked).firstOrNull { (name, _) -> name in existingFunctions } ?: break
            val (name, start) = hit
            val end = declarationEnd(masked, start)
            rescues += Rescue("reimplemented an out-of-scope function", "$name — it already exists and was settled")
            withFunctions = (withFunctions.substring(0, start) + withFunctions.substring(end))
                .replace(Regex("\n{3,}"), "\n\n")
        }

        // Work out spans on a comment- and string-free copy of the SAME LENGTH, so every index found
        // there addresses the same character in the original. Blanking rather than deleting is what
        // keeps that true — a KDoc containing a brace would otherwise throw the brace matching off.
        var text = withFunctions
        while (true) {
            val masked = strippedOfCommentsAndStrings(text)
            val hit = topLevelDeclarations(masked).firstOrNull { (name, _) -> name in existing } ?: break
            val (name, start) = hit
            val end = declarationEnd(masked, start)
            rescues += Rescue("redeclared type", "$name — it already exists in ${existing[name]}")
            text = (text.substring(0, start) + text.substring(end)).replace(Regex("\n{3,}"), "\n\n")
        }

        val kept = text.lines().filterNot { line ->
            val import = IMPORT.matchEntire(line.trim()) ?: return@filterNot false
            val fqName = import.groupValues[1]
            val simple = fqName.substringAfterLast('.')
            val pkg = fqName.substringBeforeLast('.', "")
            val actual = existing[simple]
            val wrong = actual != null && pkg != actual
            if (wrong) rescues += Rescue("import from the wrong package", "$fqName — $simple lives in $actual")
            wrong
        }
        return Result(kept.joinToString("\n").trim(), rescues)
    }

    /** Every top-level (column-zero) function declaration, as name to start index. */
    private fun topLevelFunctions(masked: String): List<Pair<String, Int>> =
        FUNCTION.findAll(masked)
            .filter { it.range.first == 0 || masked[it.range.first - 1] == '\n' }
            .map { it.groupValues[1] to it.range.first }
            .toList()

    /** Every top-level (column-zero) type declaration, as name to start index. */
    private fun topLevelDeclarations(masked: String): List<Pair<String, Int>> =
        DECLARATION.findAll(masked)
            .filter { it.range.first == 0 || masked[it.range.first - 1] == '\n' }
            .map { it.groupValues[1] to it.range.first }
            .toList()

    /**
     * The index just past a declaration that starts at [start].
     *
     * A body ends at its matching brace. A body-less one — `data class Offer(val shop: Shop, val
     * price: Int)` — ends at the first newline outside brackets, unless what follows is a supertype
     * list or a brace, which would mean the header simply continued onto the next line.
     */
    private fun declarationEnd(masked: String, start: Int): Int {
        var i = start
        var paren = 0
        var brace = 0
        var opened = false
        while (i < masked.length) {
            when (masked[i]) {
                '(' -> paren++
                ')' -> paren--
                '{' -> { brace++; opened = true }
                '}' -> {
                    brace--
                    if (opened && brace == 0) return i + 1
                }
                '\n' -> if (paren <= 0 && brace == 0 && !opened) {
                    val next = masked.drop(i + 1).indexOfFirst { !it.isWhitespace() }
                    val following = if (next < 0) null else masked[i + 1 + next]
                    if (following != '{' && following != ':') return i
                }
            }
            i++
        }
        return masked.length
    }

    /**
     * The same text with comments and string literals blanked to spaces — **same length**, so indices
     * carry over to the original. Without this, a KDoc mentioning `{` breaks the brace matching, and
     * models write KDoc.
     */
    private fun strippedOfCommentsAndStrings(text: String): String {
        val out = StringBuilder(text)
        var i = 0
        fun blankTo(end: Int) {
            for (j in i until minOf(end, text.length)) if (out[j] != '\n') out[j] = ' '
            i = end
        }
        while (i < text.length) {
            when {
                text.startsWith("//", i) -> blankTo(text.indexOf('\n', i).let { if (it < 0) text.length else it })
                text.startsWith("/*", i) -> blankTo(text.indexOf("*/", i).let { if (it < 0) text.length else it + 2 })
                text.startsWith("\"\"\"", i) ->
                    blankTo(text.indexOf("\"\"\"", i + 3).let { if (it < 0) text.length else it + 3 })
                text[i] == '"' -> {
                    var j = i + 1
                    while (j < text.length && text[j] != '"' && text[j] != '\n') j += if (text[j] == '\\') 2 else 1
                    blankTo(minOf(j + 1, text.length))
                }
                else -> i++
            }
        }
        return out.toString()
    }

    private val FUNCTION = Regex(
        "(?:@\\w+\\s+)*(?:public\\s+|internal\\s+|private\\s+)?(?:inline\\s+|tailrec\\s+|operator\\s+|suspend\\s+)*" +
            "fun\\s+([A-Za-z_]\\w*)\\s*\\(",
    )
    private val PACKAGE = Regex("^\\s*package\\s+([\\w.]+)", RegexOption.MULTILINE)
    private val IMPORT = Regex("import\\s+([\\w.]+)")
    private val DECLARATION = Regex(
        "(?:@\\w+\\s+)*(?:public\\s+|internal\\s+|private\\s+)?" +
            // `fun` belongs here for `fun interface AdmissionPolicy` — omitting it kept the one type
            // an agent is most likely to reach for out of the known set entirely.
            "(?:abstract\\s+|open\\s+|final\\s+|sealed\\s+|data\\s+|value\\s+|enum\\s+|annotation\\s+|inner\\s+|fun\\s+)*" +
            "(?:class|interface|object)\\s+([A-Za-z_]\\w*)",
    )
}
