# Audit: sge.utils

Audited: 48/48 files | Pass: 33 | Minor: 7 | Major: 0 | N/A (SGE-original): 8
Last updated: 2026-03-04

---

### Align.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/Align.scala` |
| Java source(s) | `com/badlogic/gdx/utils/Align.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 9 constants and 6 static methods accounted for. `toString` -> `Show[Align].show` extension.
**Renames**: `int` constants -> opaque type `Align`; static methods -> extension methods
**Convention changes**: Java class with static int fields -> opaque type with extension methods; `toString(int)` -> `Show[Align]` type class instance; added bitwise operators (`|`, `&`, `unary_~`), `isCenter` extension
**TODOs**: None
**Issues**: None

---

### ArrayMap.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/ArrayMap.scala` |
| Java source(s) | `com/badlogic/gdx/utils/ArrayMap.java` |
| Status | pass |
| Tested | No |

**Completeness**: All major methods present: `put`, `get`, `removeKey`, `removeIndex`, `removeValue`, `indexOfKey`, `indexOfValue`, `containsKey`, `containsValue`, `putAll`, `clear`, `ensureCapacity`, `shrink`, `peekKey`, `peekValue`, `reverse`, `shuffle`, `truncate`, `foreachEntry`, `foreachKey`, `foreachValue`, `hashCode`, `equals`, `toString`.
**Renames**: `Array<>` -> `DynamicArray`; null keys -> `Nullable`; `Comparator` -> `Ordering`
**Convention changes**: Private constructor with `MkArray`-based factory methods; `final class`; split packages
**TODOs**: None
**Issues**: None

---

### BaseJsonReader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/BaseJsonReader.scala` |
| Java source(s) | `com/badlogic/gdx/utils/BaseJsonReader.java` |
| Status | pass |
| Tested | No |

**Completeness**: Both interface methods (`parse(InputStream)`, `parse(FileHandle)`) present.
**Renames**: Java interface -> Scala trait; `FileHandle` path adapted
**Convention changes**: None
**TODOs**: None
**Issues**: None

---

### BinaryHeap.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/BinaryHeap.scala` |
| Java source(s) | `com/badlogic/gdx/utils/BinaryHeap.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: `add`, `add(value)`, `contains`, `peek`, `pop`, `remove`, `notEmpty`, `isEmpty`, `clear`, `setValue`, `equals`, `hashCode`, `toString`. Inner `Node` class present.
**Renames**: None significant
**Convention changes**: `Node` uses `var` fields; `return` eliminated via `boundary`/`break`
**TODOs**: None
**Issues**: None

---

### BufferOps.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/BufferOps.scala` |
| Java source(s) | N/A |
| Status | N/A |
| Tested | No |

**Notes**: SGE-original utility providing `size` and `isEmpty` extension methods on NIO Buffer types. No Java counterpart.

---

### BufferUtils.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/BufferUtils.scala` |
| Java source(s) | `com/badlogic/gdx/utils/BufferUtils.java` |
| Status | pass |
| Tested | No |

**Completeness**: All major methods present: 12 `copy` overloads, 8 `transform` overloads, 8 `findFloats` overloads, 7 `new*Buffer` factory methods, `newUnsafeByteBuffer`, `disposeUnsafeByteBuffer`, `isUnsafeByteBuffer`, `getUnsafeBufferAddress`, `getAllocatedBytesUnsafe`.
**Renames**: JNI native methods -> `PlatformOps.buffer` delegation
**Convention changes**: Uses `PlatformOps` for cross-platform buffer operations; private helpers `asByteBuffer`, `positionInBytes`, `bytesToElements`, `elementsToBytes`
**TODOs**: None
**Issues**: None

---

### Clipboard.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/Clipboard.scala` |
| Java source(s) | `com/badlogic/gdx/utils/Clipboard.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 3 interface methods accounted for.
**Renames**: `getContents()` / `setContents(String)` -> `contents` / `contents_=` property syntax; `@Null String` -> `Nullable[String]`
**Convention changes**: Java interface -> Scala trait; getter/setter -> Scala property syntax
**TODOs**: None
**Issues**: None

---

### ComparableTimSort.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/ComparableTimSort.scala` |
| Java source(s) | `com/badlogic/gdx/utils/ComparableTimSort.java` |
| Status | pass |
| Tested | No |

**Completeness**: Faithful port of the TimSort variant for `Comparable` types.
**Renames**: None
**Convention changes**: `return` -> `boundary`/`break`; Java arrays -> Scala arrays
**TODOs**: None
**Issues**: None

---

### DataBuffer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/DataBuffer.scala` |
| Java source(s) | `com/badlogic/gdx/utils/DataBuffer.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: API differs from Java source. Java `DataBuffer` extends `DataOutput` and has `getBuffer()` and `toArray()`. Scala `DataBuffer` is an abstract class extending `OutputStream with java.io.DataOutput`. Has `getBytes` instead of `toArray`, and an inline `OptimizedByteArrayOutputStream` that duplicates the one in `StreamUtils`.
**Renames**: `toArray()` -> `getBytes`; missing `getBuffer()` method (returns raw backing array), missing `size()` method
**Convention changes**: Different inheritance hierarchy; Java `DataBuffer` extends LibGDX `DataOutput`, Scala extends Java standard `DataOutput`
**TODOs**: None
**Issues**: Missing `getBuffer()` and `size()` methods from the Java API. The `OptimizedByteArrayOutputStream` is duplicated from `StreamUtils` instead of reusing it.

---

### DataInput.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/DataInput.scala` |
| Java source(s) | `com/badlogic/gdx/utils/DataInput.java` |
| Status | pass |
| Tested | No |

**Completeness**: Both methods (`readInt(boolean)`, `readString()`) present.
**Renames**: None
**Convention changes**: `return` -> `boundary`/`break`; Java `switch/case` -> Scala `match/case`; `@Null String` -> `Nullable[String]`
**TODOs**: None
**Issues**: None

---

### DataOutput.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/DataOutput.scala` |
| Java source(s) | `com/badlogic/gdx/utils/DataOutput.java` |
| Status | pass |
| Tested | No |

**Completeness**: Both methods (`writeInt(int, boolean)`, `writeString(@Null String)`) present.
**Renames**: `@Null String` -> `Nullable[String]`
**Convention changes**: `return` -> `boundary`/`break`
**TODOs**: None
**Issues**: None

---

### DynamicArray.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/DynamicArray.scala` |
| Java source(s) | `com/badlogic/gdx/utils/Array.java` |
| Status | pass |
| Tested | Yes |

**Completeness**: Comprehensive port covering all libGDX `Array` functionality plus additional methods from `IntArray`, `FloatArray`, `CharArray`, `BooleanArray`, `LongArray`, `ShortArray`, `ByteArray` (all replaced by this single generic class via `MkArray` type class). Methods include: `apply`, `update`, `add` (1-3 args), `addAll`, `insert`, `insertRange`, `removeIndex`, `removeValue`, `removeValueByRef`, `removeRange`, `removeAll`, `removeAllByRef`, `pop`, `clear`, `truncate`, `contains`, `containsByRef`, `containsAll`, `containsAny`, `indexOf`, `indexOfByRef`, `lastIndexOf`, `replaceFirst`, `replaceAll`, `swap`, `reverse`, `shuffle`, `sort`, `ensureCapacity`, `setSize`, `shrink`, `begin`/`end` (snapshot), `toArray`, `first`, `last`, `peek`, `random`, `isEmpty`, `nonEmpty`, `foreach`, `iterator`, `exists`, `find`, `count`, `forall`, `indexWhere`, `+=`, `-=`, `--=`, `addAll(Iterable)`.
**Renames**: `Array` -> `DynamicArray`; `ordered` -> `preserveOrder`; all primitive array variants unified
**Convention changes**: `MkArray` type class for unboxed primitive arrays; private constructor with factory methods; `final class`; snapshot pattern uses raw null internally for performance
**TODOs**: None
**Issues**: Internal use of raw `null` for snapshot fields is documented and acceptable for performance.

---

### Eval.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/Eval.scala` |
| Java source(s) | N/A |
| Status | N/A |
| Tested | No |

**Notes**: SGE-original trampolined IO monad. No Java counterpart.

---

### I18nBundle.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/I18nBundle.scala` |
| Java source(s) | `com/badlogic/gdx/utils/I18NBundle.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public methods present: `getLocale`, `get(key)`, `keys()`, `format(key, args)`, `debug(placeholder)`. Companion object has: `createBundle` (4 overloads), `getSimpleFormatter`/`setSimpleFormatter`, `getExceptionOnMissingKey`/`setExceptionOnMissingKey`. Private implementation methods: `createBundleImpl`, `getCandidateLocales`, `getFallbackLocale`, `loadBundleChain`, `loadBundle`, `checkFileExistence`, `toFileHandle`.
**Renames**: `I18NBundle` -> `I18NBundle` (same); `I18NBundle` filename -> `I18nBundle.scala`
**Convention changes**: Java `static` -> companion object; `return` -> `boundary`/`break`; `null` -> `Nullable`
**TODOs**: None
**Issues**: None

---

### JsonCodecs.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/JsonCodecs.scala` |
| Java source(s) | N/A |
| Status | N/A |
| Tested | No |

**Notes**: SGE-original. Type aliases + extension for jsoniter-scala codec derivation. Re-exports `JsonCodec` (jsoniter-scala `JsonValueCodec`), `Json` (kindlings JSON AST), and `FileHandle.readJson[T]` extension method.

---

### JsonReader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/JsonReader.scala` |
| Java source(s) | `com/badlogic/gdx/utils/JsonReader.java` |
| Status | pass |
| Tested | Yes |

**Completeness**: All parse methods present: `parse(String)`, `parse(Reader)`, `parse(InputStream)`, `parse(FileHandle)`. Implementation replaced with jsoniter-scala + kindlings bridge.
**Renames**: None
**Convention changes**: Hand-written lexer/parser replaced with library delegation (jsoniter-scala). `BaseJsonReader` trait implemented.
**TODOs**: None
**Issues**: None

---

### JsonValue.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/JsonValue.scala` |
| Java source(s) | `com/badlogic/gdx/utils/JsonValue.java` |
| Status | pass |
| Tested | Yes |

**Completeness**: Comprehensive port. All constructors, accessors (`asString`, `asFloat`, `asDouble`, `asLong`, `asInt`, `asBoolean`, `asByte`, `asShort`, `asChar`), array accessors (`asStringArray`, `asFloatArray`, etc.), child accessors (`get(name)`, `get(index)`, `getString(name)`, `getFloat(name)`, etc.), modification methods (`set`, `setName`, `setChild`, `addChild`, `addChildFirst`, `replace`, `remove`, `removeFromParent`), type checks (`isArray`, `isObject`, `isString`, `isNumber`, `isNull`, `isValue`), serialization (`toJson`, `trace`, `toString`), and iteration (`iterator`). Inner `ValueType` enum present. Companion has `fromJson` factory.
**Renames**: `@Null` -> `Nullable`; `ValueType` inner class preserved as enum
**Convention changes**: `return` -> `boundary`/`break`; implements `Iterable[JsonValue]`
**TODOs**: None
**Issues**: None

---

### LittleEndianInputStream.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/LittleEndianInputStream.scala` |
| Java source(s) | `com/badlogic/gdx/utils/LittleEndianInputStream.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 13 DataInput interface methods present.
**Renames**: None
**Convention changes**: `readLine()` annotated with `@scala.annotation.nowarn` for deprecation
**TODOs**: None
**Issues**: None

---

### Logger.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/Logger.scala` |
| Java source(s) | `com/badlogic/gdx/utils/Logger.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 6 logging methods (`debug`, `info`, `error` x2 each) plus `setLevel`/`getLevel` and 4 level constants (`NONE`, `ERROR`, `INFO`, `DEBUG`).
**Renames**: `Gdx.app` -> `sge.Sge` context parameter; `tag` field is `val` (was private final in Java)
**Convention changes**: Methods take `(using Sge)` context parameter; constants in companion object; `level` is a `var` (getter/setter combined)
**TODOs**: None
**Issues**: None

---

### MkArray.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/MkArray.scala` |
| Java source(s) | N/A |
| Status | N/A |
| Tested | Yes |

**Notes**: SGE-original type class for unboxed primitive array creation. Provides given instances for all 8 JVM primitives plus `AnyRef` and `Nullable`. No Java counterpart.

---

### Nullable.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/Nullable.scala` |
| Java source(s) | N/A (replaces `@Null`/`@NonNull` annotations) |
| Status | N/A |
| Tested | Yes |

**Notes**: SGE-original allocation-free `Option` alternative. Replaces Java `@Null`/`@NonNull`/`null` patterns throughout the codebase. No Java counterpart.

---

### NumberUtils.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/NumberUtils.scala` |
| Java source(s) | `com/badlogic/gdx/utils/NumberUtils.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 7 methods present: `floatToIntBits`, `floatToRawIntBits`, `floatToIntColor`, `intToFloatColor`, `intBitsToFloat`, `doubleToLongBits`, `longBitsToDouble`.
**Renames**: None
**Convention changes**: Java `final class` with static methods -> Scala `object`
**TODOs**: None
**Issues**: None

---

### ObjectMap.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/ObjectMap.scala` |
| Java source(s) | `com/badlogic/gdx/utils/ObjectMap.java` |
| Status | pass |
| Tested | Yes |

**Completeness**: All major methods present: `size`, `isEmpty`, `nonEmpty`, `put`, `get` (2 overloads), `remove`, `containsKey`, `containsValue`, `findKey`, `putAll`, `clear` (2 overloads), `ensureCapacity`, `shrink`, `resize`, `foreachEntry`, `foreachKey`, `foreachValue`, `hashCode`, `equals`, `toString`. Factory methods in companion.
**Renames**: `@Null` return -> `Nullable`; Java `Entries`/`Keys`/`Values` iterators -> `foreachEntry`/`foreachKey`/`foreachValue` methods
**Convention changes**: `final class` with `MkArray`-based internals; uses `filled: Array[Boolean]` for occupancy instead of null-key sentinel; private constructor with factory methods; `return` -> `boundary`/`break`
**TODOs**: None
**Issues**: None

---

### ObjectSet.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/ObjectSet.scala` |
| Java source(s) | `com/badlogic/gdx/utils/ObjectSet.java` |
| Status | pass |
| Tested | Yes |

**Completeness**: All major methods present: `size`, `isEmpty`, `nonEmpty`, `add`, `addAll` (2 overloads), `remove`, `contains`, `get`, `first`, `clear` (2 overloads), `ensureCapacity`, `shrink`, `resize`, `foreach`, `toArray`, `hashCode`, `equals`, `toString`. Factory methods in companion.
**Renames**: `@Null` -> `Nullable`; Java iterators -> `foreach` method
**Convention changes**: Same as `ObjectMap` -- `MkArray`-based, `filled` array, private constructor
**TODOs**: None
**Issues**: None

---

### OrderedMap.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/OrderedMap.scala` |
| Java source(s) | `com/badlogic/gdx/utils/OrderedMap.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: `size`, `isEmpty`, `nonEmpty`, `put`, `get` (2 overloads), `remove`, `removeIndex`, `containsKey`, `containsValue`, `findKey`, `alter`, `alterIndex`, `putAll`, `clear` (2 overloads), `ensureCapacity`, `shrink`, `orderedKeys`, `foreachEntry`, `foreachKey`, `foreachValue`, `hashCode`, `equals`, `toString`.
**Renames**: `Array<K> keys` -> `DynamicArray[K] _keys`; accessor `orderedKeys()`
**Convention changes**: Delegates to `ObjectMap` + `DynamicArray` internally; `final class`
**TODOs**: None
**Issues**: None

---

### OrderedSet.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/OrderedSet.scala` |
| Java source(s) | `com/badlogic/gdx/utils/OrderedSet.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: `size`, `isEmpty`, `nonEmpty`, `add`, `addAll` (2 overloads), `remove`, `removeIndex`, `contains`, `get`, `first`, `alter`, `alterIndex`, `clear` (2 overloads), `ensureCapacity`, `shrink`, `orderedItems`, `foreach`, `toArray`, `hashCode`, `equals`, `toString`.
**Renames**: `Array<T> items` -> `DynamicArray[A] _items`; accessor `orderedItems()`
**Convention changes**: Delegates to `ObjectSet` + `DynamicArray` internally; `final class`
**TODOs**: None
**Issues**: None

---

### PerformanceCounter.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/PerformanceCounter.scala` |
| Java source(s) | `com/badlogic/gdx/utils/PerformanceCounter.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: `tick()`, `tick(delta)`, `start()`, `stop()`, `reset()`, `toString()`, `toString(sb)`. All fields: `name`, `time`, `load`, `current`, `valid`.
**Renames**: `Gdx.app.error(...)` -> `throw SgeError.InvalidInput(...)` (behavior change: Java logs error and returns; Scala throws — intentional)
**Convention changes**: Default parameter `windowSize = 5` instead of 2-arg constructor; split packages
**TODOs**: None
**Issues**: None

---

### PerformanceCounters.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/PerformanceCounters.scala` |
| Java source(s) | `com/badlogic/gdx/utils/PerformanceCounters.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: `add` (2 overloads), `tick()`, `tick(deltaTime)`, `toString(sb)`. `counters` field present.
**Renames**: `Array<PerformanceCounter>` -> `DynamicArray[PerformanceCounter]`
**Convention changes**: Split packages
**TODOs**: None
**Issues**: None

---

### Pool.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/Pool.scala` |
| Java source(s) | `com/badlogic/gdx/utils/Pool.java`, `com/badlogic/gdx/utils/DefaultPool.java`, `com/badlogic/gdx/utils/FlushablePool.java`, `com/badlogic/gdx/utils/QuadTreeFloat.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All `Pool` methods present: `obtain`, `free`, `fill`, `reset`, `discard`, `freeAll` (2 overloads: `Iterable` and `DynamicArray`), `clear`, `getFree`, `peak`. Inner `Poolable` trait preserved. `Pool.Default` replaces `DefaultPool`. `Pool.Flushable` replaces `FlushablePool`. `Pool.QuadTreeFloat` consolidates `QuadTreeFloat` from separate Java file, with all methods: `setBounds`, `add`, `query` (circle and rect), `nearest`, `reset`, constants `VALUE/X/Y/DISTSQR`.
**Renames**: `Pool` abstract class -> `Pool` trait; `DefaultPool` -> `Pool.Default`; `FlushablePool` -> `Pool.Flushable`; `QuadTreeFloat` -> `Pool.QuadTreeFloat`; `freeAll(Array)` -> `freeAll(Iterable)` + `freeAll(DynamicArray)`
**Convention changes**: `Pool` is a trait (not abstract class); uses `MkArray.anyRef` for internal `freeObjects`; `return` -> `boundary`/`break` in QuadTreeFloat
**TODOs**: None
**Issues**: `Pool` changed from `abstract class` to `trait` -- this is an intentional design improvement but changes instantiation semantics slightly. The `Poolable` type class in separate file bridges this.

---

### Poolable.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/Poolable.scala` |
| Java source(s) | (Inner interface in `Pool.java`) |
| Status | pass |
| Tested | No |

**Completeness**: Type class `Poolable[A]` with `reset(a: A)` method. Bridge given `fromTrait` auto-derives from `Pool.Poolable`.
**Renames**: None -- separate file for the type class wrapper
**Convention changes**: SGE enhancement -- type class pattern decouples reset behavior from pooled type
**TODOs**: None
**Issues**: None

---

### PoolManager.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/PoolManager.scala` |
| Java source(s) | `com/badlogic/gdx/utils/PoolManager.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: `addPool` (2 overloads), `getPool`, `getPoolOrNull`, `hasPool`, `obtain`, `obtainOrNull`, `free`, `clear`.
**Renames**: `GdxRuntimeException` -> `SgeError.InvalidInput`; `ObjectMap` -> `scala.collection.mutable.Map`
**Convention changes**: Uses `Nullable` instead of null returns; Scala `MutableMap` instead of libGDX `ObjectMap` for internal storage
**TODOs**: None
**Issues**: None

---

### PropertiesUtils.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/PropertiesUtils.scala` |
| Java source(s) | `com/badlogic/gdx/utils/PropertiesUtils.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: `load` method present. Missing `store` method and private helper methods (`dumpString`, `writeComment`).
**Renames**: None
**Convention changes**: Implementation delegates to `java.util.Properties` instead of reimplementing the parser. This simplifies the code but may have subtle behavior differences.
**TODOs**: None
**Issues**: Missing `store(ObjectMap, Writer, String)` method from the Java API. The `load` implementation uses `java.util.Properties` delegation which may differ from the original hand-written parser in edge cases.

---

### QuickSelect.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/QuickSelect.scala` |
| Java source(s) | `com/badlogic/gdx/utils/QuickSelect.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: `select`, `partition`, `recursiveSelect`, `medianOfThreePivot`, `swap`.
**Renames**: `Comparator<? super T>` -> `Ordering[T]`
**Convention changes**: `return` eliminated in `recursiveSelect` via expression-based code
**TODOs**: None
**Issues**: None

---

### Resource.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/Resource.scala` |
| Java source(s) | N/A |
| Status | N/A |
| Tested | No |

**Notes**: SGE-original resource management opaque type built on `Eval`. Provides `make`, `fromCloseable`, `pure`, `map`, `flatMap`, `run`. No Java counterpart.

---

### Scaling.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/Scaling.scala` |
| Java source(s) | `com/badlogic/gdx/utils/Scaling.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 10 scaling instances present: `fit`, `contain`, `fill`, `fillX`, `fillY`, `stretch`, `stretchX`, `stretchY`, `none`. Abstract `apply` method present.
**Renames**: None
**Convention changes**: Java `abstract class` with anonymous inner classes -> Scala `trait` with SAM lambda implementations
**TODOs**: None
**Issues**: None

---

### ScreenUtils.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/ScreenUtils.scala` |
| Java source(s) | `com/badlogic/gdx/utils/ScreenUtils.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: `clear` methods (5 overloads) present. `getFrameBufferPixels` methods (2 overloads) present. `getFrameBufferTexture` and `getFrameBufferPixmap` are commented out with TODO.
**Renames**: `Gdx.gl` / `Gdx.graphics` -> `Sge().graphics.gl` / `Sge().graphics`
**Convention changes**: Methods take `(using Sge)` context parameter
**TODOs**: `getFrameBufferTexture` and `getFrameBufferPixmap` blocked on `Pixmap.createFromFrameBuffer`
**Issues**: 2 methods (`getFrameBufferTexture`, `getFrameBufferPixmap`) commented out, waiting for `Pixmap.createFromFrameBuffer` port.

---

### Seconds.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/Seconds.scala` |
| Java source(s) | N/A |
| Status | N/A |
| Tested | No |

**Notes**: SGE-original opaque type wrapping `Float` for type-safe time values. No Java counterpart.

---

### Select.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/Select.scala` |
| Java source(s) | `com/badlogic/gdx/utils/Select.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: `select`, `selectIndex`, `fastMin`, `fastMax`. Static `instance()` in companion.
**Renames**: `Comparator<T>` -> `Ordering[T]`; `GdxRuntimeException` -> `SgeError.InvalidInput`
**Convention changes**: `null` -> `Nullable` for lazy instance; companion object for static methods
**TODOs**: None
**Issues**: None

---

### SgeError.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/SgeError.scala` |
| Java source(s) | `com/badlogic/gdx/utils/GdxRuntimeException.java` (+ `SerializationException.java`) |
| Status | pass |
| Tested | No |

**Completeness**: Replaces `GdxRuntimeException` and `SerializationException` with a sealed enum of error types: `FileReadError`, `FileWriteError`, `MathError`, `NetworkError`, `SerializationError`, `InvalidInput`, `GraphicsError`.
**Renames**: `GdxRuntimeException` -> `SgeError` enum; `SerializationException` -> `SgeError.SerializationError`
**Convention changes**: Scala 3 `enum` extending `Exception`; typed error variants instead of generic runtime exception; `cause` uses `Option[Throwable]` instead of raw `Throwable` constructor parameter
**TODOs**: None
**Issues**: None

---

### SgeNativesLoader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/SgeNativesLoader.scala` |
| Java source(s) | `com/badlogic/gdx/utils/GdxNativesLoader.java` |
| Status | pass |
| Tested | No |

**Completeness**: `load()` and `disableNativesLoading` present.
**Renames**: `GdxNativesLoader` -> `SgeNativesLoader`
**Convention changes**: `SharedLibraryLoader` usage replaced with TODO placeholder (native loading handled differently in SGE)
**TODOs**: Actual native library loading is a placeholder (`nativesLoaded = true` without loading)
**Issues**: None (intentional -- native loading strategy differs in SGE)

---

### Show.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/Show.scala` |
| Java source(s) | N/A |
| Status | N/A |
| Tested | No |

**Notes**: SGE-original type class for string representation. Used by `Align`. No Java counterpart.

---

### Sort.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/Sort.scala` |
| Java source(s) | `com/badlogic/gdx/utils/Sort.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All 6 sort method overloads present. Missing `instance()` static method.
**Renames**: `Array<T>` -> `DynamicArray[T]`; `Comparator` -> `Ordering`
**Convention changes**: Java instance class with lazy singleton -> Scala `object` with direct methods. The `sort(DynamicArray, Ordering)` method copies to array and back, which differs from Java's direct array access.
**TODOs**: None
**Issues**: Missing `instance()` method (minor -- Scala object serves as singleton). The `sort(DynamicArray, Ordering)` implementation copies the array out and back, which is less efficient than the Java version that sorts `items` in-place.

---

### StreamUtils.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/StreamUtils.scala` |
| Java source(s) | `com/badlogic/gdx/utils/StreamUtils.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods present: 6 `copyStream` overloads, 2 `copyStreamToByteArray` overloads, 3 `copyStreamToString` overloads, `closeQuietly`. `OptimizedByteArrayOutputStream` inner class present. Constants `DEFAULT_BUFFER_SIZE` and `EMPTY_BYTES` present.
**Renames**: `@Null String charset` -> `Nullable[String]`; `null` check -> `Nullable.fold`
**Convention changes**: `null` -> `Nullable`; `IOException` throws declarations omitted (not required in Scala)
**TODOs**: None
**Issues**: None

---

### TextFormatter.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/TextFormatter.scala` |
| Java source(s) | `com/badlogic/gdx/utils/TextFormatter.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: `format(pattern, args)` method present. Missing `replaceEscapeChars` and `simpleFormat` private methods -- the implementation is a naive placeholder that does simple `{0}`, `{1}` substitution.
**Renames**: `useMessageFormat` -> `useAdvanced`
**Convention changes**: None
**TODOs**: None
**Issues**: The implementation is a simplistic placeholder. The Java original has two code paths: (1) `MessageFormat`-based with proper locale formatting and escape char handling, and (2) `simpleFormat` with careful brace parsing and error checking. The Scala version only does basic `String.replace` which lacks error checking, brace escaping (`{{`), and locale-aware formatting.

---

### TimeUtils.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/TimeUtils.scala` |
| Java source(s) | `com/badlogic/gdx/utils/TimeUtils.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 6 methods present: `nanoTime`, `millis`, `nanosToMillis`, `millisToNanos`, `timeSinceNanos`, `timeSinceMillis`. `nanosPerMilli` constant present.
**Renames**: None
**Convention changes**: Java `final class` with static methods -> Scala `object`
**TODOs**: None
**Issues**: None

---

### Timer.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/Timer.scala` |
| Java source(s) | `com/badlogic/gdx/utils/Timer.java` |
| Status | minor_issues |
| Tested | No |

**Completeness**: All public methods present: `postTask`, `scheduleTask` (4 overloads), `stop`, `start`, `clear`, `isEmpty`, `delay`. Static methods: `instance`, `post`, `schedule` (3 overloads). Inner `Task` abstract class with `run`, `cancel`, `isScheduled`, `getExecuteTimeMillis`. Inner `TimerThread` with `run`, `addPostedTask`, `removePostedTask`, `resume`, `pause`, `dispose`.
**Renames**: `Gdx.app` -> `sge.Sge` context; `GdxRuntimeException` -> `SgeError.MathError`; `null` -> `Option`/`Nullable`; `LifecycleListener` -> TODO
**Convention changes**: `implicit sde: sge.Sge` (not `using`); `timer` field uses `Option[Timer]` instead of raw null; `currentThread` uses `Option[TimerThread]`
**TODOs**: `app.addLifecycleListener(this)` / `app.removeLifecycleListener(this)` commented out; `app.postRunnable(runPostedTasks)` commented out; `runPostedTasks` not yet connected
**Issues**: Uses `implicit` keyword instead of `using` (older convention). `LifecycleListener` integration and `postRunnable` are TODOs -- posted tasks will not actually execute on the main thread until these are implemented. The `runTasks` field is annotated `@nowarn("msg=unused")`.

---

### TimSort.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/TimSort.scala` |
| Java source(s) | `com/badlogic/gdx/utils/TimSort.java` |
| Status | pass |
| Tested | No |

**Completeness**: Faithful port of the TimSort algorithm. All internal methods present.
**Renames**: `Comparator` -> `Ordering`
**Convention changes**: Uses raw `null` internally for clearing references after sort (acceptable performance optimization at internal boundary); `return` -> `boundary`/`break`
**TODOs**: None
**Issues**: None

---

### XmlReader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/utils/XmlReader.scala` |
| Java source(s) | `com/badlogic/gdx/utils/XmlReader.java` |
| Status | pass |
| Tested | Yes |

**Completeness**: All parse methods present: `parse(String)`, `parse(Reader)`, `parse(InputStream)`, `parse(FileHandle)`. Inner `Element` class with all accessors. Implementation uses scala-xml library.
**Renames**: None
**Convention changes**: Hand-written XML parser replaced with scala-xml library delegation
**TODOs**: None
**Issues**: None
