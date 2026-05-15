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
