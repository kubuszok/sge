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
 * Covenant-baseline-loc: 372
 * Covenant-baseline-methods: AssetData,Configurable,ResourceData,SaveData,assetDataFromJson,assetDescriptors,assetJsons,assets,classNameMap,createSaveData,currentLoadIndex,data,dataFields,dataJsons,descriptors,fields,filename,fromJson,getAssetData,getSaveData,indices,load,loadAsset,loadIndex,rd,resolveClassName,resource,resources,result,save,saveAsset,saveData,saveDataFromJson,saveDataToJson,sharedAssets,this,toJson,uniqueData,uniqueFields
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/ResourceData.java
 * Covenant-verified: 2026-04-19
 */

package sge
package graphics
package g3d
package particles

import scala.reflect.ClassTag

import sge.assets.AssetDescriptor
import sge.assets.AssetManager
import sge.utils.DynamicArray
import sge.utils.Json
import sge.utils.Nullable
import sge.utils.ObjectMap
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

  /** Converts a SaveData to JSON AST. */
  private[particles] def saveDataToJson(sd: SaveData): Json = {
    val fields = Vector.newBuilder[(String, Json)]

    // data map
    val dataFields = Vector.newBuilder[(String, Json)]
    sd.data.foreachEntry { (k, v) =>
      val jsonVal: Json = v match {
        case s: String            => Json.fromString(s)
        case l: java.lang.Long    => Json.fromLong(l)
        case d: java.lang.Double  => Json.fromBigDecimal(BigDecimal(d))
        case b: java.lang.Boolean => Json.fromBoolean(b)
        case i: java.lang.Integer => Json.fromInt(i)
        case f: java.lang.Float   => Json.fromBigDecimal(BigDecimal(f.toDouble))
        case other => Json.fromString(other.toString)
      }
      dataFields += k -> jsonVal
    }
    fields += "data" -> Json.fromJsonObject(sge.utils.JsonObject(dataFields.result()))

    // indices
    val indices = Vector.newBuilder[Json]
    for (i <- 0 until sd.assets.size)
      indices += Json.fromInt(sd.assets(i))
    fields += "indices" -> Json.arr(indices.result()*)

    Json.obj(fields.result()*)
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
                    val value: AnyRef = dv match {
                      case Json.Str(s) => s
                      case Json.Num(n) =>
                        // Prefer Long for integer values, Double for fractional
                        n.toLong
                          .map(l => java.lang.Long.valueOf(l))
                          .getOrElse(
                            n.toDouble.map(d => java.lang.Double.valueOf(d)).getOrElse(n.value.asInstanceOf[AnyRef])
                          )
                      case Json.Bool(b) => java.lang.Boolean.valueOf(b)
                      case other        => other.toString
                    }
                    saveData.data.put(dk, value)
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
            case _ => ()
          }
        }
      case _ => throw SgeError.InvalidInput("Expected JSON object for ResourceData")
    }
    rd
  }
}
