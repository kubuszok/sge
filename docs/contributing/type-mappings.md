# LibGDX to SGE Type Mappings

## Package Mappings

| LibGDX Package | SGE Package | Notes |
|---------------|------------|-------|
| `com.badlogic.gdx` | `sge` | |
| `com.badlogic.gdx.assets` | `sge.assets` | |
| `com.badlogic.gdx.assets.loaders` | `sge.assets.loaders` | |
| `com.badlogic.gdx.audio` | `sge.audio` | |
| `com.badlogic.gdx.files` | `sge.files` | |
| `com.badlogic.gdx.graphics` | `sge.graphics` | |
| `com.badlogic.gdx.graphics.g2d` | `sge.graphics.g2d` | |
| `com.badlogic.gdx.graphics.g3d` | `sge.graphics.g3d` | Not yet started |
| `com.badlogic.gdx.graphics.glutils` | `sge.graphics.glutils` | |
| `com.badlogic.gdx.graphics.profiling` | `sge.graphics.profiling` | |
| `com.badlogic.gdx.input` | `sge.input` | |
| `com.badlogic.gdx.maps` | `sge.maps` | Not yet started |
| `com.badlogic.gdx.math` | `sge.math` | |
| `com.badlogic.gdx.math.collision` | `sge.math.collision` | |
| `com.badlogic.gdx.net` | `sge.net` | |
| `com.badlogic.gdx.scenes.scene2d` | `sge.scenes.scene2d` | Complete |
| `com.badlogic.gdx.utils` | `sge.utils` | Partial |
| `com.badlogic.gdx.utils.compression` | `sge.utils.compression` | |
| `com.badlogic.gdx.utils.viewport` | `sge.utils.viewport` | |

## Renamed Classes

| LibGDX Class | SGE Class |
|-------------|-----------|
| `Gdx` | `Sge` |
| `Files.FileType` | `sge.files.FileType` |
| `DefaultPool` | `Pool.Default` |
| `FlushablePool` | `Pool.Flushable` |
| `GdxRuntimeException` | `SgeError` |
| `GdxNativesLoader` | `SgeNativesLoader` |
| `SerializationException` | `SgeError.SerializationError` |
| `QuadTreeFloat` | `Pool.QuadTreeFloat` |

## Skipped Packages

| LibGDX Package | Reason |
|---------------|--------|
| `utils.async` | Scala has `Future`, `Promise`, `ExecutionContext` |
| `utils.reflect` | Scala avoids runtime reflection by convention |

## Skipped Classes

### Collections — Arrays

| LibGDX Class | SGE Replacement | Reason |
|-------------|-----------------|--------|
| `Array` | `ArrayBuffer` / `ArraySeq` | Scala stdlib equivalent |
| `ByteArray`, `CharArray`, `FloatArray`, `IntArray`, `LongArray`, `ShortArray` | `DynamicArray[T]` | Unified via MkArray type class |
| `DelayedRemovalArray` | `.view`s | Scala collections have lazy views |
| `SnapshotArray` | `ArrayBuffer` with copy-on-modify | Simple pattern suffices |

### Collections — Lists

| LibGDX Class | SGE Replacement | Reason |
|-------------|-----------------|--------|
| `PooledLinkedList`, `SortedIntList` | Scala stdlib lists | Scala stdlib equivalent |

### Collections — Maps

| LibGDX Class | SGE Replacement | Reason |
|-------------|-----------------|--------|
| `ObjectMap`, `IntMap`, `IntIntMap`, `IntFloatMap`, `LongMap`, `ObjectIntMap`, `ObjectFloatMap`, `ObjectLongMap` | `ObjectMap[K, V]` | Unified MkArray-backed map |
| `ArrayMap`, `IdentityMap` | `ArrayMap[K, V]` | MkArray-backed ordered map |
| `OrderedMap` | `OrderedMap[K, V]` | Insertion-ordered variant |

### Collections — Sets

| LibGDX Class | SGE Replacement | Reason |
|-------------|-----------------|--------|
| `ObjectSet`, `IntSet` | `ObjectSet[A]` | MkArray-backed set |
| `OrderedSet` | `OrderedSet[A]` | Insertion-ordered variant |

### Collections — Queues & Bit Sets

| LibGDX Class | SGE Replacement | Reason |
|-------------|-----------------|--------|
| `AtomicQueue`, `LongQueue`, `Queue` | Scala stdlib queues | Scala stdlib equivalent |
| `BooleanArray`, `Bits` | `mutable.BitSet` | Scala stdlib equivalent |

### Collections — Other

| LibGDX Class | SGE Replacement | Reason |
|-------------|-----------------|--------|
| `Collections` | — | Scala has views, mutable/immutable split |
| `StringBuilder` | `scala.collection.mutable.StringBuilder` | Scala stdlib equivalent |

### JSON/XML

All `Json*`, `UBJson*`, `XmlReader`, `XmlWriter` classes are skipped — Scala has dedicated JSON/XML libraries.

### Other Skipped Classes

| LibGDX Class | SGE Replacement | Reason |
|-------------|-----------------|--------|
| `PauseableThread` | `Future`s | Scala async primitives |
| `Null`, `NonNull`, `NonNullByDefault` | `Nullable[A]` opaque type | Zero-allocation null safety |
| `ArraySupplier`, `Predicate` | Plain Scala functions | Scala functions replace SAM types |
| `Base64Coder` | `java.util.Base64` | Available since JDK 7 |
| `Disposable` | `AutoCloseable` / `Resource` | Scala/JDK standard |
