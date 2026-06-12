/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/ResourceData.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All public methods ported faithfully
 * - Json.Serializable replaced by fromJson using kindlings JSON AST
 * - resource field: Nullable[T] instead of T|null
 * - SaveData.resources: Nullable[ResourceData[?]] instead of ResourceData|null
 * - SaveData.loadAsset: returns Nullable[AssetDescriptor[?]] instead of AssetDescriptor|null
 * - SaveData.load[K]: returns Nullable[K] instead of K|null
 * - getSaveData(key): returns Nullable[SaveData] instead of SaveData|null
 * - IntArray -> DynamicArray[Int] for SaveData.assets
 * - Configurable interface: Java used Configurable<T> generic; Scala uses ResourceData[?]
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 638
 * Covenant-baseline-methods: AssetData,Configurable,ResourceData,SaveData,SaveValueCodec,assetDataFromJson,assetDescriptors,assetJsons,assets,classNameMap,createSaveData,currentLoadIndex,data,dataFields,dataJsons,decode,descriptors,encode,encodeResourceJson,fields,filename,fromJson,getAssetData,getSaveData,indices,load,loadAsset,loadIndex,normalizeSaveValueTag,rd,registerValueCodec,resolveClassName,resource,resourceJson,resources,result,save,saveAsset,saveData,saveDataFromJson,saveDataToJson,saveValueFromJson,saveValueToJson,sharedAssets,taggedValue,this,toJson,uniqueData,uniqueFields
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/ResourceData.java
 * Covenant-verified: 2026-06-12
 *
 * upstream-commit: 34cc595deb4ac09ee476c6b1aba1b805f4dc81a7
 */

package sge
package graphics
package g3d
package particles

import scala.reflect.ClassTag

import sge.assets.AssetDescriptor
import sge.assets.AssetManager
import lowlevel.util.DynamicArray
import sge.utils.Json
import lowlevel.Nullable
import lowlevel.util.ObjectMap
import sge.utils.SgeError

/** This class handles the assets and configurations required by a given resource when de/serialized. It's handy when a given object or one of its members requires some assets to be loaded to work
  * properly after being deserialized. To save the assets, the object should implement the [[ResourceData.Configurable]] interface and obtain a [[ResourceData.SaveData]] object to store every required
  * asset or information which will be used during the loading phase. The passed in [[AssetManager]] is generally used to find the asset file name for a given resource of a given type. The class can
  * also store global configurations, this is useful when dealing with objects which should be allocated once (i.e singleton). The deserialization process must happen in the same order of
  * serialization, because the per object [[ResourceData.SaveData]] blocks are stored as a [[DynamicArray]] within the [[ResourceData]], while the global [[ResourceData.SaveData]] instances can be
  * accessed in any order because require a unique [[String]] and are stored in an [[ObjectMap]].
  * @author
  *   Inferno
  */
class ResourceData[T]() {

  import ResourceData.*

  /** Unique data, can be used to save/load generic data which is not always loaded back after saving. Must be used to store data which is uniquely addressable by a given string (i.e a system
    * configuration).
    */
  private[particles] val uniqueData: ObjectMap[String, SaveData] = ObjectMap[String, SaveData]()

  /** Objects save data, must be loaded in the same saving order */
  private[particles] val data: DynamicArray[SaveData] = DynamicArray[SaveData]()

  /** Shared assets among all the configurable objects */
  var sharedAssets:             DynamicArray[AssetData[?]] = DynamicArray[AssetData[?]]()
  private var currentLoadIndex: Int                        = 0
  var resource:                 Nullable[T]                = Nullable.empty

  /** Raw JSON AST of the serialized resource graph, captured verbatim by [[ResourceData.fromJson]].
    *
    * Java's `ResourceData.read` restores the resource directly (`resource = json.readValue("resource", null, jsonData)`, ResourceData.java line 232) because LibGDX Json has no per-application context
    * to bind. The SGE resource graph (a [[ParticleEffect]] of [[ParticleController]]s) requires the `(using Sge)` context to be reconstructed, which is not available during the context-free
    * [[ResourceData.fromJson]] parse. We therefore capture the resource sub-AST here at parse time and let the Sge-aware caller ([[ParticleEffectLoader.loadSync]]) build [[resource]] from it via
    * [[ParticleEffectCodecs.decodeResource]]. This keeps the Java write/read contract intact end-to-end: the resource is always serialized by `toJson` and always restored by the load pipeline.
    */
  var resourceJson: Nullable[Json] = Nullable.empty

  def this(resource: T) = {
    this()
    this.resource = Nullable(resource)
  }

  /** Serializes this ResourceData to a JSON AST. Produces the same structure consumed by [[ResourceData.fromJson]]. */
  def toJson: Json = {
    val fields = Vector.newBuilder[(String, Json)]

    // assets
    val assetJsons = Vector.newBuilder[Json]
    for (ad <- sharedAssets)
      assetJsons += Json.obj(
        "filename" -> Json.fromString(ad.filename),
        "type" -> Json.fromString(ad.`type`.getName)
      )
    fields += "assets" -> Json.arr(assetJsons.result()*)

    // data (ordered SaveData list)
    val dataJsons = Vector.newBuilder[Json]
    for (sd <- this.data)
      dataJsons += ResourceData.saveDataToJson(sd)
    fields += "data" -> Json.arr(dataJsons.result()*)

    // unique (keyed SaveData map)
    val uniqueFields = Vector.newBuilder[(String, Json)]
    uniqueData.foreachEntry { (k, v) =>
      uniqueFields += k -> ResourceData.saveDataToJson(v)
    }
    fields += "unique" -> Json.fromJsonObject(sge.utils.JsonObject(uniqueFields.result()))

    // resource (the full effect graph). Java write serializes it via
    // `json.writeValue("resource", resource, null)` (ResourceData.java line 216).
    // The resource is polymorphic; ParticleEffectCodecs.encodeResource emits the
    // same {"class": ..., "controllers": [...]} structure consumed on load.
    val resourceJson: Json = resource.fold(Json.Null) { res =>
      ResourceData.encodeResourceJson(res)
    }
    fields += "resource" -> resourceJson

    Json.obj(fields.result()*)
  }

  private[particles] def getAssetData[K](filename: String, `type`: Class[K]): Int =
    scala.util.boundary {
      var i = 0
      for (assetData <- sharedAssets) {
        if (assetData.filename == filename && assetData.`type` == `type`) {
          scala.util.boundary.break(i)
        }
        i += 1
      }
      -1
    }

  def assetDescriptors: DynamicArray[AssetDescriptor[?]] = {
    val descriptors = DynamicArray[AssetDescriptor[?]]()
    for (assetData <- sharedAssets)
      descriptors.add(new AssetDescriptor(assetData.filename, assetData.`type`))
    descriptors
  }

  def assets: DynamicArray[AssetData[?]] =
    sharedAssets

  /** Creates and adds a new SaveData object to the save data list */
  def createSaveData(): SaveData = {
    val saveData = SaveData(this)
    data.add(saveData)
    saveData
  }

  /** Creates and adds a new and unique SaveData object to the save data map */
  def createSaveData(key: String): SaveData = {
    val saveData = SaveData(this)
    if (uniqueData.containsKey(key)) throw new RuntimeException("Key already used, data must be unique, use a different key")
    uniqueData.put(key, saveData)
    saveData
  }

  /** @return the next save data in the list */
  def saveData: SaveData = {
    val result = data(currentLoadIndex)
    currentLoadIndex += 1
    result
  }

  /** @return the unique save data in the map */
  def getSaveData(key: String): Nullable[SaveData] =
    uniqueData.get(key)
}

object ResourceData {

  /** This interface must be implemented by any class requiring additional assets to be loaded/saved */
  trait Configurable {
    def save(manager: AssetManager, resources: ResourceData[?]): Unit

    def load(manager: AssetManager, resources: ResourceData[?]): Unit
  }

  /** Contains all the saved data. [[SaveData.data]] is a map which links an asset name to its instance. [[SaveData.assets]] is an array of indices addressing a given [[AssetData]] in the
    * [[ResourceData]]
    */
  class SaveData(
    var resources: Nullable[ResourceData[?]] = Nullable.empty
  ) {

    val data:              ObjectMap[String, AnyRef] = ObjectMap[String, AnyRef]()
    val assets:            DynamicArray[Int]         = DynamicArray[Int]()
    private var loadIndex: Int                       = 0

    def this(resources: ResourceData[?]) =
      this(Nullable(resources))

    def saveAsset[K](filename: String, `type`: Class[K]): Unit =
      resources.foreach { res =>
        var i = res.getAssetData(filename, `type`)
        if (i == -1) {
          res.sharedAssets.add(AssetData(filename, `type`))
          i = res.sharedAssets.size - 1
        }
        assets.add(i)
      }

    def saveAsset[K: ClassTag](filename: String): Unit =
      saveAsset(filename, summon[ClassTag[K]].runtimeClass.asInstanceOf[Class[K]])

    def save(key: String, value: AnyRef): Unit =
      data.put(key, value)

    def loadAsset(): Nullable[AssetDescriptor[?]] =
      if (loadIndex == assets.size) Nullable.empty
      else {
        resources.fold(Nullable.empty[AssetDescriptor[?]]) { res =>
          val assetData = res.sharedAssets(assets.items(loadIndex))
          loadIndex += 1
          Nullable(new AssetDescriptor(assetData.filename, assetData.`type`))
        }
      }

    def load[K](key: String): Nullable[K] =
      data.get(key).map(_.asInstanceOf[K])
  }

  /** This class contains all the information related to a given asset */
  class AssetData[T](
    var filename: String,
    var `type`:   Class[T]
  )

  /** Serializes a single SaveData value, preserving its runtime type so the deserialized value is structurally and type-identical. This mirrors Java's `json.writeValue("data", data, ObjectMap.class)`
    * (ResourceData.java line 97): LibGDX Json writes each `Object` map value with a `class` tag so a stored `java.lang.Integer` comes back as an `Integer` (not a `Long`) and a stored
    * `Array<IntArray>` comes back identically typed.
    *
    * Without LibGDX's reflective Json, each value is wrapped in `{ "class": "<tag>", "value": <encoded> }` where the tag names the exact type. This closes the ISS-550 toString hole: previously every
    * non-primitive value was serialized via `other.toString` (corrupting [[DynamicArray]] payloads) and every integral number was restored as a `java.lang.Long` (crashing the `load[Int]` access
    * pattern in [[values.MeshSpawnShapeValue.load]] with a ClassCastException).
    */
  private[particles] def saveValueToJson(value: AnyRef): Json =
    value match {
      case null => Json.Null
      case s: String            => taggedValue("String", Json.fromString(s))
      case b: java.lang.Boolean => taggedValue("Boolean", Json.fromBoolean(b))
      case i: java.lang.Integer => taggedValue("Integer", Json.fromInt(i))
      case l: java.lang.Long    => taggedValue("Long", Json.fromLong(l))
      case f: java.lang.Float   =>
        taggedValue("Float", Json.fromFloat(f).getOrElse(Json.fromString(f.toString)))
      case d: java.lang.Double =>
        taggedValue("Double", Json.fromDouble(d).getOrElse(Json.fromString(d.toString)))
      case arr: DynamicArray[?] =>
        val elems = Vector.newBuilder[Json]
        var i     = 0
        while (i < arr.size) {
          elems += saveValueToJson(arr(i).asInstanceOf[AnyRef])
          i += 1
        }
        taggedValue("DynamicArray", Json.arr(elems.result()*))
      case other =>
        // Delegate to any registered codec for application-defined SaveData payloads
        // (e.g. BillboardParticleBatch.Config). Mirrors LibGDX's reflective Object
        // handling without a reflection dependency. If no codec is registered, fail
        // loudly rather than silently corrupting the value via toString.
        valueCodecs.get(other.getClass.getName) match {
          case Some(codec) =>
            taggedValue(other.getClass.getName, codec.encode(other))
          case None =>
            throw SgeError.InvalidInput(
              "No SaveData codec registered for value of type: " + other.getClass.getName
            )
        }
    }

  /** Builds the `{ "class": tag, "value": encoded }` wrapper used by [[saveValueToJson]]. */
  private def taggedValue(tag: String, encoded: Json): Json =
    Json.obj("class" -> Json.fromString(tag), "value" -> encoded)

  /** Maps the fully-qualified boxed/primitive class names LibGDX Json writes (`type.getName`, e.g. `java.lang.Integer`; Json.java writeType lines 768-771) onto the short tags this codec emits, so a
    * genuine LibGDX/Flame-authored `.pfx` — which tags EVERY primitive and String SaveData value with its `java.lang.*` name (Json.java lines 506-515) — decodes through the same branches as
    * SGE-written files. Unrecognised names pass through unchanged for the registered-codec lookup.
    */
  private def normalizeSaveValueTag(tag: String): String =
    tag match {
      case "java.lang.String"    => "String"
      case "java.lang.Boolean"   => "Boolean"
      case "java.lang.Integer"   => "Integer"
      case "java.lang.Long"      => "Long"
      case "java.lang.Float"     => "Float"
      case "java.lang.Double"    => "Double"
      case "java.lang.Short"     => "Short"
      case "java.lang.Byte"      => "Byte"
      case "java.lang.Character" => "Character"
      case other                 => other
    }

  /** Reconstructs a SaveData value from the tagged JSON produced by [[saveValueToJson]], restoring its exact runtime type. Inverse of [[saveValueToJson]] (Java:
    * `json.readValue("data", ObjectMap.class, jsonData)`, ResourceData.java line 103).
    */
  private[particles] def saveValueFromJson(json: Json): AnyRef =
    json match {
      case Json.Null => null
      // Bare (untagged) JSON scalars. This codec always tags its own output
      // (see saveValueToJson), so a bare scalar can only reach here from an
      // externally hand-authored / hand-edited file. LibGDX Json itself does
      // NOT write Object values bare: when the known (declared) type is null —
      // always the case for ObjectMap<String,Object> SaveData values — it wraps
      // every boxed primitive and String in a tagged object
      // `{ "class": "java.lang.Integer", "value": 7 }` (Json.java lines
      // 506-515; the tag is type.getName, Json.java writeType lines 768-771).
      // We still accept bare scalars leniently for robustness: integral numbers
      // restore as Long, fractional as Double (the JSON-native widths), strings
      // and booleans as themselves. Exact Int typing is preserved only for
      // values carrying an "Integer"/"java.lang.Integer" tag.
      case Json.Str(s)  => s
      case Json.Bool(b) => java.lang.Boolean.valueOf(b)
      case Json.Num(n)  =>
        n.toLong
          .map(l => java.lang.Long.valueOf(l): AnyRef)
          .getOrElse(
            n.toDouble.map(d => java.lang.Double.valueOf(d): AnyRef).getOrElse(n.value.asInstanceOf[AnyRef])
          )
      case Json.Arr(elems) =>
        // Bare arrays restore as a DynamicArray of recursively-restored elements.
        val arr = DynamicArray[AnyRef](elems.size)
        elems.foreach(e => arr.add(saveValueFromJson(e)))
        arr
      // Tagged wrappers { "class": tag, "value": encoded } restore the exact type.
      case Json.Obj(obj) if obj("class").exists(_.isString) && obj("value").isDefined =>
        val rawTag = obj("class").collect { case Json.Str(s) => s }.getOrElse("")
        val value  = obj("value").getOrElse(Json.Null)
        // LibGDX Json writes the tag as `type.getName` (Json.java writeType
        // lines 768-771), so a genuine .pfx carries the fully-qualified boxed
        // name (e.g. "java.lang.Integer") for every primitive/String SaveData
        // value (Json.java lines 506-515 emit the tagged wrapper whenever the
        // known type is null, which is always for ObjectMap<String,Object>
        // values). Treat those names as aliases of the short tags this codec
        // writes so genuine LibGDX/Flame files load. Non-aliased names fall
        // through unchanged to the registered-codec lookup.
        val tag = normalizeSaveValueTag(rawTag)
        tag match {
          case "String" =>
            value match {
              case Json.Str(s) => s
              case _           => throw SgeError.InvalidInput("Malformed String SaveData value")
            }
          case "Boolean" =>
            value match {
              case Json.Bool(b) => java.lang.Boolean.valueOf(b)
              case _            => throw SgeError.InvalidInput("Malformed Boolean SaveData value")
            }
          case "Integer" =>
            value match {
              case Json.Num(n) =>
                java.lang.Integer.valueOf(n.toInt.getOrElse(throw SgeError.InvalidInput("Malformed Integer SaveData value")))
              case _ => throw SgeError.InvalidInput("Malformed Integer SaveData value")
            }
          case "Long" =>
            value match {
              case Json.Num(n) =>
                java.lang.Long.valueOf(n.toLong.getOrElse(throw SgeError.InvalidInput("Malformed Long SaveData value")))
              case _ => throw SgeError.InvalidInput("Malformed Long SaveData value")
            }
          case "Float" =>
            value match {
              case Json.Num(n) =>
                java.lang.Float.valueOf(n.toFloat.getOrElse(throw SgeError.InvalidInput("Malformed Float SaveData value")))
              case _ => throw SgeError.InvalidInput("Malformed Float SaveData value")
            }
          case "Double" =>
            value match {
              case Json.Num(n) =>
                java.lang.Double.valueOf(n.toDouble.getOrElse(throw SgeError.InvalidInput("Malformed Double SaveData value")))
              case _ => throw SgeError.InvalidInput("Malformed Double SaveData value")
            }
          case "Short" =>
            value match {
              case Json.Num(n) =>
                java.lang.Short.valueOf(n.toInt.getOrElse(throw SgeError.InvalidInput("Malformed Short SaveData value")).toShort)
              case _ => throw SgeError.InvalidInput("Malformed Short SaveData value")
            }
          case "Byte" =>
            value match {
              case Json.Num(n) =>
                java.lang.Byte.valueOf(n.toInt.getOrElse(throw SgeError.InvalidInput("Malformed Byte SaveData value")).toByte)
              case _ => throw SgeError.InvalidInput("Malformed Byte SaveData value")
            }
          case "Character" =>
            // LibGDX writes a Character value as its single-character string
            // (Json.java writes the boxed value inline under "value").
            value match {
              case Json.Str(s) if s.length == 1 => java.lang.Character.valueOf(s.charAt(0))
              case Json.Num(n)                  =>
                java.lang.Character.valueOf(n.toInt.getOrElse(throw SgeError.InvalidInput("Malformed Character SaveData value")).toChar)
              case _ => throw SgeError.InvalidInput("Malformed Character SaveData value")
            }
          case "DynamicArray" =>
            value match {
              case Json.Arr(elems) =>
                val arr = DynamicArray[AnyRef](elems.size)
                elems.foreach(e => arr.add(saveValueFromJson(e)))
                arr
              case _ => throw SgeError.InvalidInput("Malformed DynamicArray SaveData value")
            }
          case className =>
            valueCodecs.get(className) match {
              case Some(codec) => codec.decode(value)
              case None        =>
                throw SgeError.InvalidInput("No SaveData codec registered for value of type: " + className)
            }
        }
      // Inline-field tagged object { "class": tag, <field>: <value>, ... } with
      // NO "value" key. This is the shape LibGDX Json writes for a genuine,
      // non-Serializable application object (e.g. a Flame/LibGDX-authored
      // BillboardParticleBatch$Config): the default object branch emits the
      // class tag and then writes the object's fields INLINE
      // (`writeObjectStart(actualType, knownType)` writes only the `class`
      // field — Json.java line 689, writeType lines 768-771 — followed by
      // `writeFields(value)` — Json.java line 690 — which appends each field
      // directly to the same object). The `{class,value}` wrapper handled above
      // is ONLY for boxed primitives / String (Json.java lines 506-515); a plain
      // object never carries a "value" key. We treat such a block as an
      // inline-field object: strip the `class` tag and hand the remaining
      // fields, as a JSON object, to the registered codec (whose decode accepts
      // exactly that field shape).
      case Json.Obj(obj) if obj("class").exists(_.isString) =>
        val rawTag = obj("class").collect { case Json.Str(s) => s }.getOrElse("")
        val tag    = normalizeSaveValueTag(rawTag)
        valueCodecs.get(tag) match {
          case Some(codec) =>
            val inlineFields = obj.fields.filterNot { case (k, _) => k == "class" }
            codec.decode(Json.fromJsonObject(sge.utils.JsonObject(inlineFields)))
          case None =>
            throw SgeError.InvalidInput("No SaveData codec registered for value of type: " + tag)
        }
      case Json.Obj(_) =>
        // Untagged JSON object as a SaveData value: not produced by this codec
        // (which always tags objects) and not reconstructible without a known
        // target type. Fail loudly rather than corrupt.
        throw SgeError.InvalidInput("Untagged JSON object cannot be restored as a SaveData value")
    }

  /** Converts a SaveData to JSON AST. */
  private[particles] def saveDataToJson(sd: SaveData): Json = {
    val fields = Vector.newBuilder[(String, Json)]

    // data map
    val dataFields = Vector.newBuilder[(String, Json)]
    sd.data.foreachEntry { (k, v) =>
      dataFields += k -> saveValueToJson(v)
    }
    fields += "data" -> Json.fromJsonObject(sge.utils.JsonObject(dataFields.result()))

    // indices
    val indices = Vector.newBuilder[Json]
    for (i <- 0 until sd.assets.size)
      indices += Json.fromInt(sd.assets(i))
    fields += "indices" -> Json.arr(indices.result()*)

    Json.obj(fields.result()*)
  }

  /** Pluggable codec for application-defined SaveData payloads that ResourceData cannot structurally encode (e.g. `BillboardParticleBatch.Config`). Registered via [[registerValueCodec]] from the
    * owning class so ResourceData need not depend on higher-level packages — the faithful, reflection-free analogue of LibGDX Json's reflective handling of arbitrary `Object` values.
    */
  trait SaveValueCodec {
    def encode(value: AnyRef): Json

    def decode(json: Json): AnyRef
  }

  private val valueCodecs: scala.collection.mutable.Map[String, SaveValueCodec] =
    scala.collection.mutable.Map.empty

  /** Registers a [[SaveValueCodec]] for the given runtime class so values of that type survive the SaveData round-trip with their exact type. Keyed by the class' fully-qualified name (the tag written
    * by [[saveValueToJson]]).
    */
  private[particles] def registerValueCodec(clazz: Class[?], codec: SaveValueCodec): Unit =
    registerValueCodec(clazz.getName, codec)

  /** Registers a [[SaveValueCodec]] under the given fully-qualified class name. Use this overload when the tag to recognise is a name with no live [[Class]] in this runtime — e.g. the legacy
    * `com.badlogic.gdx.…` type name carried by a LibGDX/Flame-authored `.pfx` for a type that SGE renamed. The name must match the tag exactly as written by [[saveValueToJson]] / read by
    * [[saveValueFromJson]].
    */
  private[particles] def registerValueCodec(className: String, codec: SaveValueCodec): Unit =
    valueCodecs.synchronized {
      valueCodecs.update(className, codec)
    }

  /** Resolves a class name from a particle effect JSON file. Handles both LibGDX (com.badlogic.gdx) and SGE (sge) class names.
    */
  /** Known class name mappings for particle effect resource types. Uses a hardcoded map instead of Class.forName for Scala.js/Native compatibility.
    */
  private val classNameMap: Map[String, Class[?]] = Map(
    "sge.graphics.Texture" -> classOf[sge.graphics.Texture],
    "sge.graphics.g3d.particles.ParticleEffect" -> classOf[sge.graphics.g3d.particles.ParticleEffect],
    "sge.graphics.g3d.Model" -> classOf[sge.graphics.g3d.Model],
    "sge.graphics.g2d.TextureAtlas" -> classOf[sge.graphics.g2d.TextureAtlas],
    "sge.graphics.g3d.particles.ParticleController" -> classOf[sge.graphics.g3d.particles.ParticleController],
    // LibGDX legacy names
    "com.badlogic.gdx.graphics.Texture" -> classOf[sge.graphics.Texture],
    "com.badlogic.gdx.graphics.g3d.particles.ParticleEffect" -> classOf[sge.graphics.g3d.particles.ParticleEffect],
    "com.badlogic.gdx.graphics.g3d.Model" -> classOf[sge.graphics.g3d.Model],
    "com.badlogic.gdx.graphics.g2d.TextureAtlas" -> classOf[sge.graphics.g2d.TextureAtlas],
    "com.badlogic.gdx.graphics.g3d.particles.ParticleController" -> classOf[sge.graphics.g3d.particles.ParticleController]
  )

  private[particles] def resolveClassName(className: String): Class[?] =
    classNameMap.getOrElse(className, throw SgeError.InvalidInput("Unknown particle resource class: " + className))

  /** Serializes the resource graph to a JSON AST. Mirrors Java's `json.writeValue("resource", resource, null)` (ResourceData.java line 216): the resource is polymorphic and written with a `class`
    * tag. Only [[ParticleEffect]] resources are supported (the only type that flows through this pipeline); [[ParticleEffectCodecs.encodeResource]] emits the controller graph.
    */
  private[particles] def encodeResourceJson(resource: Any): Json =
    resource match {
      case effect: ParticleEffect => ParticleEffectCodecs.encodeResource(effect)
      case other =>
        throw SgeError.InvalidInput("Cannot serialize particle resource of type: " + other.getClass.getName)
    }

  /** Parses an AssetData from a JSON AST node. Expected format: { "filename": "...", "type": "fully.qualified.ClassName" }
    */
  private[particles] def assetDataFromJson(json: Json): AssetData[?] = json match {
    case Json.Obj(fields) =>
      var filename: String = ""
      var typeName: String = ""
      fields.fields.foreach { case (k, v) =>
        k match {
          case "filename" =>
            v match {
              case Json.Str(s) => filename = s
              case _           => ()
            }
          case "type" =>
            v match {
              case Json.Str(s) => typeName = s
              case _           => ()
            }
          case _ => ()
        }
      }
      if (filename.isEmpty || typeName.isEmpty)
        throw SgeError.InvalidInput("AssetData missing filename or type")
      AssetData(filename, resolveClassName(typeName))
    case _ => throw SgeError.InvalidInput("Expected JSON object for AssetData")
  }

  /** Parses a SaveData from a JSON AST node, linking it back to the parent ResourceData. Expected format: { "data": { ... }, "indices": [int, ...] }
    */
  private[particles] def saveDataFromJson(json: Json, parent: ResourceData[?]): SaveData = {
    val saveData = SaveData(parent)
    json match {
      case Json.Obj(fields) =>
        fields.fields.foreach { case (k, v) =>
          k match {
            case "data" =>
              v match {
                case Json.Obj(dataFields) =>
                  dataFields.fields.foreach { case (dk, dv) =>
                    // Each value is a type-tagged wrapper (see saveValueToJson);
                    // restore it with its exact runtime type.
                    saveData.data.put(dk, saveValueFromJson(dv))
                  }
                case _ => ()
              }
            case "indices" =>
              v match {
                case Json.Arr(elems) =>
                  elems.foreach {
                    case Json.Num(n) =>
                      n.toDouble.foreach { d =>
                        saveData.assets.add(d.toInt)
                      }
                    case _ => ()
                  }
                case _ => ()
              }
            case _ => ()
          }
        }
      case _ => ()
    }
    saveData
  }

  /** Parses a ResourceData from a JSON AST. Populates sharedAssets, data (SaveData list), and uniqueData. The resource field is NOT populated - that must be handled by the caller. Expected format: {
    * "unique": { ... }, "data": [ ... ], "assets": [ ... ], "resource": { ... } }
    */
  private[particles] def fromJson[T](json: Json): ResourceData[T] = {
    val rd = ResourceData[T]()
    json match {
      case Json.Obj(fields) =>
        // First pass: populate sharedAssets so SaveData indices are valid
        fields.fields.foreach { case (k, v) =>
          if (k == "assets") {
            v match {
              case Json.Arr(elems) =>
                for (elem <- elems)
                  rd.sharedAssets.add(assetDataFromJson(elem))
              case _ => ()
            }
          }
        }
        // Second pass: populate data and uniqueData
        fields.fields.foreach { case (k, v) =>
          k match {
            case "data" =>
              v match {
                case Json.Arr(elems) =>
                  for (elem <- elems)
                    rd.data.add(saveDataFromJson(elem, rd))
                case _ => ()
              }
            case "unique" =>
              v match {
                case Json.Obj(uniqueFields) =>
                  uniqueFields.fields.foreach { case (uk, uv) =>
                    rd.uniqueData.put(uk, saveDataFromJson(uv, rd))
                  }
                case _ => ()
              }
            // resource: captured verbatim. Java read restores the resource here
            // (`resource = json.readValue("resource", null, jsonData)`,
            // ResourceData.java line 232). The SGE resource graph needs a
            // (using Sge) context that fromJson does not carry, so we store the
            // sub-AST and let the Sge-aware load pipeline build it (see
            // ResourceData.resourceJson and ParticleEffectLoader.loadSync).
            case "resource" =>
              v match {
                case Json.Null => ()
                case other     => rd.resourceJson = Nullable(other)
              }
            case _ => ()
          }
        }
      case _ => throw SgeError.InvalidInput("Expected JSON object for ResourceData")
    }
    rd
  }
}
