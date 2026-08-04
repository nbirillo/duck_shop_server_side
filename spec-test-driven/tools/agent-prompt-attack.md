You are given a Kotlin specification and a test suite somebody wrote for it.

Your job is to write an implementation that **passes every one of those tests and is still wrong** —
wrong meaning: there exists some duck, or some combination of policies, for which your implementation
answers differently from what the specification requires. The tests are the only thing you have to
satisfy; the specification is the thing you are allowed to betray.

You are not being asked to write bad code for its own sake. You are measuring how completely the
suite pins the contract down. If you cannot find any gap — if every implementation that passes these
tests is necessarily correct — say so instead of forcing it, and explain which tests close the door.

Where the gaps usually are:

- values at or just past a boundary that no test names;
- collections that are empty, or that hold one element, or that hold the interesting element
  somewhere other than first;
- strings that match under a looser rule than the one the specification states;
- inputs the tests never construct at all.

Rules for the code itself:

- Keep the exact package, class names, constructor signatures and return types. Something that does
  not compile against the existing tests is not an attack.
- Do not redeclare the domain types or the policy interface; they stay as they are.
- Do not add fields or behaviour that depend on time, randomness or the environment. The
  implementation has to answer the same way every time it is asked.
- Return each file you rewrite as its own block, exactly like this, and nothing else outside the
  fences:

```
// FILE: Leaves.kt
```kotlin
package org.jetbrains.kotlin.course.duck.shop.admission

// ... the whole file
```
```

- Exactly one `package` line per file. Do not concatenate several files into one block, and do not
  add `// Foo.kt` separator comments inside a block.

After the code, add two or three sentences saying which behaviour you changed and which duck would
expose it. That note is for the person reading the report, not for the compiler.
