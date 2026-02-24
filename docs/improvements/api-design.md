# API Design Improvements

## AD-001: Sorted Params via given Ordering

- **LibGDX**: Various container classes accept undocumented sorted parameters
- **SGE**: Uses `given Ordering[T]` for sort-dependent operations
- **Problem**: LibGDX containers sometimes expect elements to be sorted but this
  requirement is not expressed in the type system. Passing unsorted data leads to
  silent corruption or incorrect results.
- **Improvement**: Scala's `given Ordering[T]` makes sort requirements explicit in
  the type signature. The compiler ensures an ordering is available.
- **Status**: implemented

## AD-002: Comparator to given Ordering

- **LibGDX**: Uses `java.util.Comparator<T>` for custom comparison logic
- **SGE**: Uses `given Ordering[T]`
- **Problem**: `Comparator` is a Java SAM interface that requires explicit instantiation
  and doesn't compose well with Scala's collection operations.
- **Improvement**: `Ordering[T]` is Scala's native comparison abstraction. It:
  - Composes with `sorted`, `max`, `min` on all Scala collections
  - Can be derived automatically for case classes via `derives Ordering`
  - Works with `given`/`using` for implicit resolution
- **Status**: implemented

## AD-003: Adapter Classes Collapsed into Base Traits

- **LibGDX**: `ApplicationListener` + `ApplicationAdapter`, `InputProcessor` + `InputAdapter`,
  `Screen` + `ScreenAdapter`
- **SGE**: `Application`, `Input`, `Screen` (single trait with default methods)
- **Problem**: LibGDX uses the Adapter pattern (abstract class with empty default methods)
  as a separate class from the interface. This is a Java limitation — interfaces couldn't
  have default methods before Java 8, and LibGDX maintained backward compatibility.
- **Improvement**: Scala traits have always supported default method implementations.
  The adapter is merged into the base trait, reducing the number of types and eliminating
  the question of "should I extend the interface or the adapter?"
- **Status**: implemented

## AD-004: LibGDX Collections Replaced with Scala Stdlib

- **LibGDX**: Custom collection classes (`Array`, `ObjectMap`, `IntArray`, `ObjectSet`, etc.)
- **SGE**: `ArrayBuffer`, `mutable.Map`, `DynamicArray`, `Set`, `BitSet`, etc.
- **Problem**: LibGDX implements its own collection library optimized for game development
  (GC-friendly, primitive-specialized). This creates a parallel ecosystem incompatible
  with Java's and Scala's standard collections.
- **Improvement**: Use Scala's stdlib collections which:
  - Have a richer API (map, filter, fold, etc.)
  - Are well-tested and documented
  - Integrate with the entire Scala ecosystem
  - Support Scala.js and Scala Native
  Performance-critical specializations can be added later if profiling shows the need.
- **Status**: implemented
