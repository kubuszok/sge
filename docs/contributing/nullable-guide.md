# Nullable[A] Usage Guide

SGE uses `sge.utils.Nullable[A]` as an opaque type to replace Java's nullable references.
This provides type-safe null handling without the overhead of `Option` boxing.

## Initialization

```scala
// Instead of: var x: Foo = null
var x: Nullable[Foo] = Nullable.empty

// Instead of: var x: Foo = someValue
var x: Nullable[Foo] = Nullable(someValue)
```

**Never** use `null`, `_`, or `scala.compiletime.uninitialized` for `Nullable` vars.
Always use `Nullable.empty`.

## Pattern Replacements

### 1. Null-or-value (getOrElse)

```scala
// Before:
if (nullable == null) defaultValue else nullable.asInstanceOf[A]

// After:
nullable.getOrElse(defaultValue)
```

### 2. Null-or-throw (fold)

```scala
// Before:
if (nullable == null) throw new Error("missing")
else { val a = nullable.asInstanceOf[A]; doSomething(a) }

// After:
nullable.fold(throw new Error("missing"))(a => doSomething(a))
```

### 3. Null-or-compute (fold)

```scala
// Before:
if (nullable == null) computeDefault()
else { val a = nullable.asInstanceOf[A]; transform(a) }

// After:
nullable.fold(computeDefault())(a => transform(a))
```

### 4. Non-null only (foreach)

```scala
// Before:
if (nullable != null) { val a = nullable.asInstanceOf[A]; doSomething(a) }

// After:
nullable.foreach { a => doSomething(a) }
```

### 5. Boolean checks

```scala
// Before:                       // After:
nullable != null                 nullable.isDefined
!(nullable == null)              nullable.isDefined
nullable == null                 nullable.isEmpty
```

## Summary

| Situation | Method |
|-----------|--------|
| Initialize empty | `Nullable.empty` |
| Initialize with value | `Nullable(value)` |
| Branch on null vs non-null | `fold(ifEmpty)(ifPresent)` |
| Return default if null | `getOrElse(default)` |
| Act only if non-null | `foreach(action)` |
| Check if present | `isDefined` |
| Check if absent | `isEmpty` |
