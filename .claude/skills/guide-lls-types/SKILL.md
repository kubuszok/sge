---
description: Types from lls (lowlevel.*) used in SGE — MkArray, Nullable, ArrayView, DynamicArray, collections
---

Load the lls (lowlevel-scala) type usage reference.

$READ docs/contributing/type-mappings.md

## lls Types in SGE

SGE depends on [lls](https://github.com/kubuszok/lls) (`com.kubuszok:lls`) for
shared zero-allocation types. The canonical source is `../lls/` — changes to
`lowlevel.*` types go to lls, not sge.

### Package mapping

| Old (sge) | New (lls) | Type |
|-----------|-----------|------|
| `sge.utils.Nullable[A]` | `lowlevel.Nullable[A]` | Opaque union type (allocation-free Option) |
| `sge.utils.MkArray[A]` | `lowlevel.MkArray[A]` | Sealed type class for unboxed array ops |
| N/A | `lowlevel.ArrayView[A,_,_]` | Zero-allocation array iteration (lls-only) |
| `sge.utils.DynamicArray[A]` | `lowlevel.util.DynamicArray[A]` | Unboxed resizable array |
| `sge.utils.ObjectMap[K,V]` | `lowlevel.util.ObjectMap[K,V]` | Fibonacci hashing map |
| `sge.utils.ObjectSet[A]` | `lowlevel.util.ObjectSet[A]` | Open-addressing set |
| `sge.utils.OrderedMap[K,V]` | `lowlevel.util.OrderedMap[K,V]` | Insertion-ordered map |
| `sge.utils.OrderedSet[A]` | `lowlevel.util.OrderedSet[A]` | Insertion-ordered set |
| `sge.utils.ArrayMap[K,V]` | `lowlevel.util.ArrayMap[K,V]` | Linear-scan map |
| `sge.utils.Eval[A]` | `lowlevel.util.Eval[A]` | Stack-safe lazy evaluation |
| `sge.utils.Resource[A]` | `lowlevel.util.Resource[A]` | Resource management with cleanup |
| `sge.utils.Sort` | `lowlevel.util.Sort` | TimSort facade |
| `sge.math.MathUtils` | `lowlevel.math.MathUtils` | Fast math (sin tables, interpolation) |

### MkArray for opaque types — the correct pattern

For opaque types backed by primitives, declare the given with the **concrete MkArray subclass** using the `ofXxxAs` helper:

```scala
opaque type Pixels = Int
object Pixels {
  def apply(v: Int): Pixels = v

  // CORRECT: concrete subclass + ofIntAs helper
  given MkArray.OfInts[Pixels] = MkArray.ofIntAs[Pixels]
}
```

Quick reference:
```
Int-backed:     given MkArray.OfInts[MyType]     = MkArray.ofIntAs[MyType]
Float-backed:   given MkArray.OfFloats[MyType]   = MkArray.ofFloatAs[MyType]
Long-backed:    given MkArray.OfLongs[MyType]    = MkArray.ofLongAs[MyType]
Double-backed:  given MkArray.OfDoubles[MyType]  = MkArray.ofDoubleAs[MyType]
Short-backed:   given MkArray.OfShorts[MyType]   = MkArray.ofShortAs[MyType]
Byte-backed:    given MkArray.OfBytes[MyType]    = MkArray.ofByteAs[MyType]
Char-backed:    given MkArray.OfChars[MyType]    = MkArray.ofCharAs[MyType]
Boolean-backed: given MkArray.OfBooleans[MyType] = MkArray.ofBooleanAs[MyType]
```

This ONE given serves three purposes:
1. **Collection factories** (`DynamicArray[Pixels]()`, `ObjectMap[K, Pixels]()`) — works via lls upcast givens that derive `MkArray[A]` from `OfInts[A]`
2. **Iteration specialization** (`MkArray.withResolved`/`summonFrom`) — dispatches to specialized `OfInts` path, zero boxing
3. **Sort** — `DynamicArray[Pixels].sort()` uses the stored `mk` field

What NOT to do:
```scala
// WRONG: upcasts to trait, loses specialization
given MkArray[Pixels] = MkArray.ofInt.asInstanceOf[MkArray[Pixels]]

// WRONG: old naming, doesn't exist anymore
given MkArray.OfInts[Pixels] = MkArray.mkInt
```

For reference types (String, case classes, etc.), `MkArray.anyRef[A]` is summoned automatically — no manual given needed.

### Sort API

All `Sort.sort` overloads that take `Ordering` now require `MkArray[T]`:

```scala
// Sort a DynamicArray — use the instance method (uses internal mk):
da.sort(ordering)

// Sort a raw Array — must provide MkArray:
Sort.sort(array, MkArray.ofInt, Ordering.Int)
```

Do NOT call `Sort.sort(dynamicArray, ordering)` — that overload was removed because it defaults to `OfRefs[AnyRef]` which crashes on primitive arrays.

### ArrayView zero-allocation patterns

Use `arr.leanView` for zero-allocation iteration over `Array[T]` and `IArray[T]`:

```scala
import lowlevel.leanView

// Simple foreach (no lambda boxing):
arr.leanView.foreach(elem => doSomething(elem))

// With index (no tuple allocation):
arr.leanView.zipWithIndex.foreach { (elem, i) => doSomething(elem, i) }

// Filtered:
arr.leanView.withFilter(_.isValid).foreach(process)

// Map to new array (needs MkArray[B]):
val result: Array[Int] = arr.leanView.map(_.size)
```

Do NOT use `leanView` for:
- Non-unit stride loops (`i += 5`)
- Early exit / break patterns
- Reverse iteration
- DynamicArray iteration (use its `inline foreach` instead)

### DynamicArray.createRef

For generic reference-type DynamicArrays where `MkArray[T]` is not in scope:
```scala
import sge.utils.createRef
val items: DynamicArray[T] = DynamicArray.createRef[T]()
val custom = DynamicArray.createRef[T](capacity = 32, preserveOrder = false)
```

### Common mistakes

1. **Using `MkArray[A]` trait type instead of concrete subclass** — compiles but `withResolved` falls through to the boxing path. Always use `OfInts`, `OfFloats`, etc.
2. **Stale incremental compilation** — if `summonInline[MkArray[MyType]]` fails for an opaque type, run `sbt clean compile`. The Scala 3 incremental compiler can cache stale type info for opaque types.
3. **Calling Sort.sort without MkArray on primitive arrays** — use `da.sort(ordering)` instance method instead.
