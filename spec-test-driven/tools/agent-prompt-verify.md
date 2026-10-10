# Agent instructions — verify and harden a test suite

You are given a small Kotlin "admission policy" algebra (given and correct) and a test suite that
another AI wrote for it. The suite may contain a WRONG test (one that asserts the wrong thing and
would fail against the correct code) and it likely MISSES cases.

## Task

1. Fix any test that asserts incorrect behaviour, so the whole suite passes against the correct
   algebra.
2. Add tests for the cases the suite misses — the cases where an implementation could plausibly
   be wrong. Find them yourself from the algebra's behaviour.

## Output contract

Return the complete corrected suite as **exactly one** Kotlin test file in a **single** fenced
block:

````
```kotlin
package org.jetbrains.kotlin.course.duck.shop.admission

import kotlin.test.Test
// ...one test class with the fixed + added tests...
```
````

One `package` declaration at the top, a single test class, no prose outside the block. Do not
redeclare the classes under test — they are provided.
