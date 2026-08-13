# Agent instructions — implement `priceFor` from a specification

You are given a written specification for one function and the data types it works with. Implement
the function so that it behaves exactly as the specification says.

## What you have, and what you do not

The specification below is the **only** description of the behaviour you get. There is no reference
implementation, no test suite, and no original business brief. If something is not in the
specification, it is not available to you.

## Where the specification is silent

Do not ask questions, and do not refuse to decide. Where the specification does not settle
something, **choose an answer and implement it.** Pick whatever a careful reader of this
specification would most likely expect, and move on.

Do not write a comment explaining the choice, do not annotate it, and do not describe it anywhere.
Return the implementation and nothing else.

## Output contract

Return exactly one fenced Kotlin block containing the whole file. No prose before or after it, no
second block, no explanation.

````
```kotlin
package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int = TODO()
```
````

Rules for the file:

- Keep the package and the exact signature(s) shown in the surface below.
- Use the given `Duck` and `DiscountRule` types. **Do not redeclare them** — they already exist and
  are on the compile path.
- Add private helpers in the same file if you want them. Do not add new public declarations beyond
  the required function(s).
- It must compile on its own against the given types.
