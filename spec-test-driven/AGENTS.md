# Agent instructions — Duck Shop admission policies

You are implementing the admission-policy logic for a Kotlin "duck shop" exercise.

## Task

Given the contract and the stub types below, implement every stub with a correct, idiomatic
Kotlin body. The behaviour of each type is fully described by the KDoc on the contract and the
stubs — follow it exactly, including edge cases (price boundaries, empty collections, and the
vacuous-truth results of the `AllOf`/`AnyOf` combinators over an empty list).

- Do **not** redeclare the `AdmissionPolicy` interface or the `Duck`/`Accessory`/`Shop` domain
  types — they come from the `:core` module and are already on the classpath.
- Keep the same package: `org.jetbrains.kotlin.course.duck.shop.admission`.
- Do not add new public types beyond the stubs you are given.

## Output contract

Return **exactly one** Kotlin file, named `Solution.kt`, inside a **single** fenced block:

````
```kotlin
package org.jetbrains.kotlin.course.duck.shop.admission

// ...all the concrete policy classes with working bodies...
```
````

No prose before or after the block. No extra files.

Strictly: **exactly one `package` declaration** at the very top, all classes in that one file.
Do not concatenate several files, do not repeat the `package` line, and do not add `// Foo.kt`
file-separator comments.

## Note

Do not search for or copy any reference/answer implementation; implement the logic yourself
from the contract and stubs.
