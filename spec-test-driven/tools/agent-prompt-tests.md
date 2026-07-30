# Agent instructions — write tests for the admission-policy algebra

You are given a set of Kotlin classes: a small "admission policy" algebra for a duck shop. Write
a thorough test suite for them.

## Task

Write tests that pin down the behaviour of every given class and that would catch a wrong
implementation. Cover the ordinary cases AND every edge case you can think of. Use `kotlin.test`
(`@Test`, `assertTrue`, `assertFalse`, `assertEquals`). Do not redeclare the classes under test —
they are provided on the classpath.

## Output contract

Return **exactly one** Kotlin test file in a **single** fenced block:

````
```kotlin
package org.jetbrains.kotlin.course.duck.shop.admission

import kotlin.test.Test
// ...one test class with your @Test methods...
```
````

One `package` declaration at the top, a single test class, no prose outside the block, no extra
files.
