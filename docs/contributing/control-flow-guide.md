# Control Flow Guide: Removing return, break and continue

This project does **not** allow `return`, `break` or `continue` keywords.
Use `scala.util.boundary` and `scala.util.boundary.break` instead.

## 1. Replacing `return`

```scala
// Before:
def method = {
  // code
  return value
  // code
}

// After:
def method = scala.util.boundary {
  // code
  scala.util.boundary.break(value)
  // code
}
```

## 2. Replacing `break` (loop exit)

```scala
// Before (Java):
while (cond) {
  // code
  break;
}

// After:
scala.util.boundary {
  while (cond) {
    // code
    scala.util.boundary.break()
  }
}
```

## 3. Replacing `continue` (skip iteration)

```scala
// Before (Java):
while (cond) {
  // code
  continue;
}

// After:
while (cond) {
  scala.util.boundary {
    // code
    scala.util.boundary.break()
  }
}
```

Note the difference: for `break`, the `boundary` wraps the **loop**;
for `continue`, the `boundary` wraps the **loop body**.
