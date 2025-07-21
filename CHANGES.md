# How was each class from LibGDX ported?

Some of the classes have Scala counterparts in Scala's stdlib, some exists as micro-optimizations and if they are needed, we can port them later.

* **ported** packages (until listed below, assume that class was ported with the same name):
  * `com.badlogic.gdx` -> `sge`
  * `com.badlogic.gdx.audio` -> `sge.audio`
  * `com.badlogic.gdx.files` -> `sge.files`
  * `com.badlogic.gdx.input` -> `sge.input`
  * `com.badlogic.gdx.math` -> `sge.math`
  * `com.badlogic.gdx.net` -> `sge.net`
  * `com.badlogic.gdx.utils` -> `sge.utils` (wip)
* **ported** classes (relative path chages, or the name):
  * `com.badlogic.gdx.Gdx` -> `sge.Sge`
  * `com.badlogic.gdx.Files.FileType` -> `sge.utils.files.FileType`
  * `com.badlogic.gdx.utils.DefaultPool` -> `sge.utils.Pool.Default`
  * `com.badlogic.gdx.utils.FlushablePool` -> `sge.utils.Pool.Flushable`
  * `com.badlogic.gdx.utils.GdxRuntimeException` -> `sge.utils.SgeError`
  * `com.badlogic.gdx.utils.GdxNativesLoader` -> `sge.utils.SgeNativesLoader`
  * `com.badlogic.gdx.utils.SerializationException` -> `sge.utils.SgeError.SerializationError`
  * `com.badlogic.gdx.utils.QuadTreeFloat` -> `com.badlogic.gdx.utils.Pool.QuadTreeFloat`
* **skipped** packages:
  * `com.badlogic.gdx.utils.async` - Scala has `Future` etc build-in
  * `com.badlogic.gdx.utils.reflect` - in Scala we avoid runtime reflection, so at least for now, we're not porting it
* **skipped** classes (all the other classes in package were ported):
  * arrays - we have `scala.collection.mutable.ArrayBuffer` or `scala.collection.mutable.ArraySeq`:
    * `com.badlogic.gdx.utils.Array` - use one of the above
    * `com.badlogic.gdx.utils.ByteArray` - not yet decided, we use the above for now
    * `com.badlogic.gdx.utils.CharArray` - not yet decided, we use the above for now
    * `com.badlogic.gdx.utils.DelayedRemovalArray` - we have `.view`s
    * `com.badlogic.gdx.utils.FloatArray` - not yet decided, we use the above for now
    * `com.badlogic.gdx.utils.IntArray` - not yet decided, we use the above for now
    * `com.badlogic.gdx.utils.SnapshotArray`
  * lists:
    * `com.badlogic.gdx.utils.PooledLinkedList`
    * `com.badlogic.gdx.utils.SortedIntList`
  * maps - we have many `Map`s, mutable and immutable:
    * `com.badlogic.gdx.utils.ArrayMap` - not yet certain that it would be needed
    * `com.badlogic.gdx.utils.IdentityMap` - same as above
    * `com.badlogic.gdx.utils.IntFloatMap` - same as above
    * `com.badlogic.gdx.utils.IntIntMap` - same as above
    * `com.badlogic.gdx.utils.IntMap` - same as above
    * `com.badlogic.gdx.utils.LongMap` - same as above
    * `com.badlogic.gdx.utils.ObjectFloatMap` - same as above
    * `com.badlogic.gdx.utils.ObjectLongMap` - same as above
    * `com.badlogic.gdx.utils.ObjectIntMap` - same as above
    * `com.badlogic.gdx.utils.ObjectMap` - same as above
  * queues:
    * `com.badlogic.gdx.utils.AtomicQueue` - not yet decided
    * `com.badlogic.gdx.utils.LongQueue` - not yet decided
    * `com.badlogic.gdx.utils.Queue` - not yet decided
  * sets:
    * `com.badlogic.gdx.utils.ObjectSet`
    * `com.badlogic.gdx.utils.OrderedSet`
  * bit-sets - we have `scala.collection.mutable.BitSet`:
    * `com.badlogic.gdx.utils.BooleanArray` - authors make a case for small sets and CPU usage, but we can skip it for initial versions
    * `com.badlogic.gdx.utils.Bits`
    * `com.badlogic.gdx.utils.IntSet`
  * other collections:
    * `com.badlogic.gdx.utils.Collections` - we have views, mutable and immutable collections, etc
    * `com.badlogic.gdx.utils.StringBuilder` - we have `StringBuilder` in Scala
  * JSON types - we have Json libraries in Scala, exact replacement not yet decided:
    * `com.badlogic.gdx.utils.BaseJsonReader`
    * `com.badlogic.gdx.utils.Json`
    * `com.badlogic.gdx.utils.JsonReader`
    * `com.badlogic.gdx.utils.JsonSkimmer`
    * `com.badlogic.gdx.utils.JsonString`
    * `com.badlogic.gdx.utils.JsonValue`
    * `com.badlogic.gdx.utils.JsonWriter`
    * `com.badlogic.gdx.utils.UBJsonReader`
    * `com.badlogic.gdx.utils.UBJsonWriter`
  * XML types:
    * `com.badlogic.gdx.utils.XmlReader`
    * `com.badlogic.gdx.utils.XmlWriter`
  * async-related classes - we have `Future`s, etc
    * `com.badlogic.gdx.utils.PauseableThread`
  * null-related annotations - we have `sde.utils.Nullable` opaque type:
    * `com.badlogic.gdx.Null`
    * `com.badlogic.gdx.NonNull`
    * `com.badlogic.gdx.NullByDefault`
  * functions in disguise - Scala APIs work with plain functions:
    * `com.badlogic.gdx.utils.ArraySupplier`
    * `com.badlogic.gdx.utils.Predicate`  
  * `com.badlogic.gdx.utils.Base64Decoder` - we have `java.util.Base64` post JDK7
  * `com.badlogic.gdx.utils.Disposeable` - we have `Closeable`/`AutoCloseable`, but also we can use `Resource`s
