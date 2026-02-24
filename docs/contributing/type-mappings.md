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
| `com.badlogic.gdx.scenes.scene2d` | `sge.scenes.scene2d` | Not yet started |
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

## Skipped Packages (Scala stdlib replacements)

| LibGDX Package | Reason |
|---------------|--------|
| `utils.async` | Scala has `Future`, `Promise`, `ExecutionContext` |
| `utils.reflect` | Scala avoids runtime reflection by convention |

## Skipped Classes

### Collections

| LibGDX Class | SGE Replacement |
|-------------|-----------------|
| `Array` | `ArrayBuffer` / `ArraySeq` |
| `ByteArray`, `CharArray`, `FloatArray`, `IntArray`, `LongArray`, `ShortArray` | `DynamicArray[T]` |
| `DelayedRemovalArray` | `.view`s |
| `SnapshotArray` | `ArrayBuffer` with copy-on-modify |
| `PooledLinkedList`, `SortedIntList` | Scala stdlib lists |
| `ObjectMap`, `IntMap`, `IntIntMap`, `IntFloatMap`, `LongMap`, `ObjectIntMap`, `ObjectFloatMap`, `ObjectLongMap` | `ObjectMap[K, V]` |
| `ArrayMap`, `IdentityMap` | `ArrayMap[K, V]` |
| `OrderedMap` | `OrderedMap[K, V]` |
| `ObjectSet`, `IntSet` | `ObjectSet[A]` |
| `OrderedSet` | `OrderedSet[A]` |
| `AtomicQueue`, `LongQueue`, `Queue` | Scala stdlib queues |
| `BooleanArray`, `Bits` | `mutable.BitSet` |
| `Collections` | Scala has views, mutable/immutable split |
| `StringBuilder` | `scala.collection.mutable.StringBuilder` |

### JSON/XML (use Scala libraries)

All `Json*`, `UBJson*`, `XmlReader`, `XmlWriter` classes are skipped.

### Other Skipped Classes

| LibGDX Class | Reason |
|-------------|--------|
| `PauseableThread` | Use `Future`s |
| `Null`, `NonNull`, `NonNullByDefault` | Use `Nullable[A]` opaque type |
| `ArraySupplier`, `Predicate` | Use plain Scala functions |
| `Base64Coder` | Use `java.util.Base64` (post JDK7) |
| `Disposable` | Use `AutoCloseable` / `Resource` |
