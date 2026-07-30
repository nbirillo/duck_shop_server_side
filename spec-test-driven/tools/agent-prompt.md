# Agent instructions — Duck Shop opening schedule

You are implementing the opening-schedule logic for a Kotlin "duck shop" exercise: a shop admits
ducks only while it is open, according to a weekly schedule.

## Task

Implement the stub files you are given (`DailyWindow.covers` and `OpeningSchedule.isOpenAt`) so
that they satisfy the following semantics. Use the given `DailyWindow` / `SpecialClosure` types
from `:core` — do not redeclare them.

Semantics:
1. A `DailyWindow` runs from `open` (inclusive) to `close` (exclusive): at exactly `close` it is
   NOT covered.
2. If `close < open` the window wraps past midnight: it covers `[open, 24:00)` on its own `day`
   AND `[00:00, close)` on the following day.
3. If `close == open` the window covers nothing.
4. `OpeningSchedule.isOpenAt(at)` is open iff at least one window covers `at`.
5. A `SpecialClosure` whose date equals `at`'s date closes the shop all day, overriding every
   window.
6. An empty schedule (no windows) is always closed.
7. No time zones — interpret every `LocalDateTime` as-is.

## Output contract

Return one fenced Kotlin block **per file**, each immediately preceded by a `// FILE:` line with
the file's path (exactly as given in the stubs):

````
// FILE: schedule/WindowMatching.kt
```kotlin
package org.jetbrains.kotlin.course.duck.shop.admission.schedule

// ...implementation...
```

// FILE: schedule/OpeningSchedule.kt
```kotlin
package org.jetbrains.kotlin.course.duck.shop.admission.schedule

// ...implementation...
```
````

Rules: exactly one `package` declaration per file, keep the given paths/packages/signatures, no
prose outside the blocks, no extra files.

## Note

Do not search for or copy any reference/answer implementation; implement the logic yourself from
the given types and the semantics above.
