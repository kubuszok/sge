package sge.port

import balticporter.transform.AddMembersTransform.MemberSpec
import balticporter.tir.Reason

/** Members spliced from the hand-written sge port into the generated port.
  * Committed as inline text so the reference tree extraction is no longer needed at build time.
  */
object AddedMembers {

  private def spec(name: String, source: String, owner: String, kind: String, static: Boolean): MemberSpec =
    MemberSpec(
      name = name,
      arity = 0,
      source = source,
      reason = Reason.Configured("add-members", owner + "#" + name),
      why = Some("the reference port's own " + kind + " `" + name + "`, spliced verbatim from its tree"),
      static = static
    )

  val all: Map[String, List[MemberSpec]] = Map(
    "com.badlogic.gdx.assets.loaders.CubemapLoader$CubemapParameter" -> List(
      spec("genMipMaps", """import sge.*
import sge.assets.*
var genMipMaps: Boolean = false""", "com.badlogic.gdx.assets.loaders.CubemapLoader$CubemapParameter", "var", false)
    ),
    "com.badlogic.gdx.assets.loaders.FileHandleResolver" -> List(
      spec("Prefix", """import sge.*
import sge.assets.*
import sge.files.FileHandle
class Prefix(var baseResolver: FileHandleResolver, var prefix: String) extends FileHandleResolver {
    override def resolve(fileName: String): FileHandle =
      baseResolver.resolve(prefix + fileName)
  }""", "com.badlogic.gdx.assets.loaders.FileHandleResolver", "class", true),
      spec("Resolution", """import sge.*
import sge.assets.*
final case class Resolution(portraitWidth: Int, portraitHeight: Int, folder: String)""", "com.badlogic.gdx.assets.loaders.FileHandleResolver", "class", true),
      spec("ForResolution", """import sge.*
import sge.assets.*
import sge.files.FileHandle
import sge.files.FileType
import lowlevel.Nullable
class ForResolution(protected val baseResolver: FileHandleResolver, protected val descriptors: Array[Resolution])(using Sge) extends FileHandleResolver {
    if (descriptors.isEmpty) throw new IllegalArgumentException("At least one Resolution needs to be supplied.")

    /** Creates a {@code ForResolution} based on a given {@link FileHandleResolver} and a list of {@link Resolution} s.
      * @param baseResolver
      *   The {@link FileHandleResolver} that will ultimately used to resolve the file.
      * @param descriptors
      *   A list of {@link Resolution} s. At least one has to be supplied.
      */
    def this(baseResolver: FileHandleResolver, descriptors: Resolution*)(using Sge) =
      this(baseResolver, descriptors.toArray)

    override def resolve(fileName: String): FileHandle = {
      val bestResolution = ForResolution.choose(descriptors*)
      val originalHandle = FileHandle(new java.io.File(fileName), FileType.Absolute)
      val handle         = baseResolver.resolve(resolve(originalHandle, bestResolution.folder))
      if (!handle.exists()) baseResolver.resolve(fileName) else handle
    }

    protected def resolve(originalHandle: FileHandle, suffix: String): String = {
      val parentString = Nullable(originalHandle.parent())
        .map { parent =>
          if (parent.name.equals("")) "" else parent.path + "/"
        }
        .getOrElse("")
      parentString + suffix + "/" + originalHandle.name
    }
  }""", "com.badlogic.gdx.assets.loaders.FileHandleResolver", "class", true),
      spec("ForResolution", """import sge.*
import sge.assets.*
object ForResolution {
    def choose(descriptors: Resolution*)(using Sge): Resolution = {
      val w = Sge().graphics.backBufferWidth
      val h = Sge().graphics.backBufferHeight

      // Prefer the shortest side.
      var best = descriptors(0)
      if (w < h) {
        var i = 0
        val n = descriptors.length
        while (i < n) {
          val other = descriptors(i)
          if (
            w.toInt >= other.portraitWidth && other.portraitWidth >= best.portraitWidth && h.toInt >= other.portraitHeight
            && other.portraitHeight >= best.portraitHeight
          ) {
            best = descriptors(i)
          }
          i += 1
        }
      } else {
        var i = 0
        val n = descriptors.length
        while (i < n) {
          val other = descriptors(i)
          if (
            w.toInt >= other.portraitHeight && other.portraitHeight >= best.portraitHeight && h.toInt >= other.portraitWidth
            && other.portraitWidth >= best.portraitWidth
          ) {
            best = descriptors(i)
          }
          i += 1
        }
      }
      best
    }
  }""", "com.badlogic.gdx.assets.loaders.FileHandleResolver", "object", true)
    ),
    "com.badlogic.gdx.graphics.g2d.Animation$PlayMode" -> List(
      spec("isLooping", """import sge.*
import sge.graphics.*
def isLooping: Boolean = this match {
      case NORMAL | REVERSED => false
      case _                 => true
    }""", "com.badlogic.gdx.graphics.g2d.Animation$PlayMode", "def", false),
      spec("isReversed", """import sge.*
import sge.graphics.*
def isReversed: Boolean = this match {
      case REVERSED | LOOP_REVERSED => true
      case _                        => false
    }""", "com.badlogic.gdx.graphics.g2d.Animation$PlayMode", "def", false)
    ),
    "com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy" -> List(
      spec("setCamera", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
import sge.graphics.Camera
def setCamera(camera: Camera): Unit =
    this.camera = camera""", "com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy", "def", false)
    ),
    "com.badlogic.gdx.graphics.g3d.particles.ResourceData" -> List(
      spec("toJson", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
import sge.utils.Json
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
  }""", "com.badlogic.gdx.graphics.g3d.particles.ResourceData", "def", false),
      spec("resourceJson", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
import sge.utils.Json
import lowlevel.Nullable
var resourceJson: Nullable[Json] = Nullable.empty""", "com.badlogic.gdx.graphics.g3d.particles.ResourceData", "var", false),
      spec("SaveValueCodec", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
import sge.utils.Json
trait SaveValueCodec {
    def encode(value: AnyRef): Json

    def decode(json: Json): AnyRef
  }""", "com.badlogic.gdx.graphics.g3d.particles.ResourceData", "trait", true),
      spec("saveValueToJson", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
import lowlevel.util.DynamicArray
import sge.utils.Json
import sge.utils.SgeError
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
    }""", "com.badlogic.gdx.graphics.g3d.particles.ResourceData", "def", true),
      spec("saveValueFromJson", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
import lowlevel.util.DynamicArray
import sge.utils.Json
import lowlevel.util.ObjectMap
import sge.utils.SgeError
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
    }""", "com.badlogic.gdx.graphics.g3d.particles.ResourceData", "def", true),
      spec("resolveClassName", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
import sge.utils.SgeError
private[particles] def resolveClassName(className: String): Class[?] =
    classNameMap.getOrElse(className, throw SgeError.InvalidInput("Unknown particle resource class: " + className))""", "com.badlogic.gdx.graphics.g3d.particles.ResourceData", "def", true),
      spec("taggedValue", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
import sge.utils.Json
private def taggedValue(tag: String, encoded: Json): Json =
    Json.obj("class" -> Json.fromString(tag), "value" -> encoded)""", "com.badlogic.gdx.graphics.g3d.particles.ResourceData", "def", true),
      spec("normalizeSaveValueTag", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
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
    }""", "com.badlogic.gdx.graphics.g3d.particles.ResourceData", "def", true),
      spec("classNameMap", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
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
  )""", "com.badlogic.gdx.graphics.g3d.particles.ResourceData", "val", true),
      spec("valueCodecs", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
private val valueCodecs: scala.collection.mutable.Map[String, SaveValueCodec] =
    scala.collection.mutable.Map.empty""", "com.badlogic.gdx.graphics.g3d.particles.ResourceData", "val", true),
      spec("registerValueCodec", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
private[particles] def registerValueCodec(clazz: Class[?], codec: SaveValueCodec): Unit =
    registerValueCodec(clazz.getName, codec)""", "com.badlogic.gdx.graphics.g3d.particles.ResourceData", "def", true),
      spec("registerValueCodec", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
private[particles] def registerValueCodec(className: String, codec: SaveValueCodec): Unit =
    valueCodecs.synchronized {
      valueCodecs.update(className, codec)
    }""", "com.badlogic.gdx.graphics.g3d.particles.ResourceData", "def", true),
      spec("assetDataFromJson", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
import sge.utils.Json
import sge.utils.SgeError
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
  }""", "com.badlogic.gdx.graphics.g3d.particles.ResourceData", "def", true),
      spec("saveDataFromJson", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
import sge.utils.Json
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
  }""", "com.badlogic.gdx.graphics.g3d.particles.ResourceData", "def", true),
      spec("saveDataToJson", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
import sge.utils.Json
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
  }""", "com.badlogic.gdx.graphics.g3d.particles.ResourceData", "def", true)
    ),
    "com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch" -> List(
      spec("ensureCodecRegistered", """def ensureCodecRegistered(): Unit = ()""", "com.badlogic.gdx.graphics.g3d.particles.batches.BillboardParticleBatch", "def", true)
    ),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.ModelInfluencer" -> List(
      spec("_modelFilenames", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
import sge.graphics.g3d.particles.*
var _modelFilenames: Array[String] = Array.empty""", "com.badlogic.gdx.graphics.g3d.particles.influencers.ModelInfluencer", "var", false)
    ),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerInfluencer" -> List(
      spec("_effectReferences", """import sge.*
import sge.graphics.*
import sge.graphics.g3d.*
import sge.graphics.g3d.particles.*
import sge.graphics.g3d.particles.EffectReference
var _effectReferences: Array[EffectReference] = Array.empty""", "com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerInfluencer", "var", false)
    ),
    "com.badlogic.gdx.maps.MapProperties" -> List(
      spec("getAs", """import sge.*
import lowlevel.Nullable
def getAs[T](key: String)(using tag: scala.reflect.ClassTag[T]): Nullable[T] =
    get(key).map(_.asInstanceOf[T])""", "com.badlogic.gdx.maps.MapProperties", "def", false),
      spec("getAs", """import sge.*
def getAs[T](key: String, defaultValue: T)(using tag: scala.reflect.ClassTag[T]): T = {
    val obj = get(key)
    obj.map(_.asInstanceOf[T]).getOrElse(defaultValue)
  }""", "com.badlogic.gdx.maps.MapProperties", "def", false)
    ),
    "com.badlogic.gdx.math.Vector" -> List(
      spec("copy", """import sge.*
def copy: T""", "com.badlogic.gdx.math.Vector", "def", false)
    ),
    "com.badlogic.gdx.math.Vector2" -> List(
      spec("*", """import sge.*
@annotation.targetName("times")
  def *(mat: Matrix3): this.type = {
    val newX = this.x * mat.values(0) + this.y * mat.values(3) + mat.values(6)
    val newY = this.x * mat.values(1) + this.y * mat.values(4) + mat.values(7)
    x = newX
    y = newY
    this
  }""", "com.badlogic.gdx.math.Vector2", "def", false),
      spec("copy", """import sge.*
override def copy: Vector2 = Vector2(x, y)""", "com.badlogic.gdx.math.Vector2", "def", false),
      spec("cross", """import sge.*
infix def cross(v: Vector2): Float = x * v.y - y * v.x""", "com.badlogic.gdx.math.Vector2", "def", false),
      spec("cross", """import sge.*
infix def cross(x: Float, y: Float): Float = this.x * y - this.y * x""", "com.badlogic.gdx.math.Vector2", "def", false)
    ),
    "com.badlogic.gdx.math.Vector3" -> List(
      spec("copy", """import sge.*
override def copy: Vector3 = Vector3(x, y, z)""", "com.badlogic.gdx.math.Vector3", "def", false)
    ),
    "com.badlogic.gdx.math.Vector4" -> List(
      spec("copy", """import sge.*
override def copy: Vector4 = Vector4(x, y, z, w)""", "com.badlogic.gdx.math.Vector4", "def", false)
    ),
    "com.badlogic.gdx.scenes.scene2d.Actor" -> List(
      spec("x_=", """import sge.*
import sge.scenes.*
def x_=(value: Float): Unit  = setX(value)""", "com.badlogic.gdx.scenes.scene2d.Actor", "def", false),
      spec("y_=", """import sge.*
import sge.scenes.*
def y_=(value: Float): Unit  = setY(value)""", "com.badlogic.gdx.scenes.scene2d.Actor", "def", false),
      spec("width_=", """import sge.*
import sge.scenes.*
def width_=(value: Float): Unit  = setWidth(value)""", "com.badlogic.gdx.scenes.scene2d.Actor", "def", false),
      spec("height_=", """import sge.*
import sge.scenes.*
def height_=(value: Float): Unit  = setHeight(value)""", "com.badlogic.gdx.scenes.scene2d.Actor", "def", false),
      spec("scaleX_=", """import sge.*
import sge.scenes.*
def scaleX_=(value: Float): Unit  = setScaleX(value)""", "com.badlogic.gdx.scenes.scene2d.Actor", "def", false),
      spec("scaleY_=", """import sge.*
import sge.scenes.*
def scaleY_=(value: Float): Unit  = setScaleY(value)""", "com.badlogic.gdx.scenes.scene2d.Actor", "def", false),
      spec("rotation_=", """import sge.*
import sge.scenes.*
def rotation_=(value: Float): Unit  = setRotation(value)""", "com.badlogic.gdx.scenes.scene2d.Actor", "def", false)
    ),
    "com.badlogic.gdx.utils.StreamUtils$OptimizedByteArrayOutputStream" -> List(
      spec("buffer", """import sge.*
def buffer: Array[Byte] =
      buf""", "com.badlogic.gdx.utils.StreamUtils$OptimizedByteArrayOutputStream", "def", false)
    ),
    "com.badlogic.gdx.utils.XmlReader" -> List(
      spec("Element", """import sge.*
type Element = XmlElement""", "com.badlogic.gdx.utils.XmlReader", "type", true),
      spec("Element", """import sge.*
import lowlevel.Nullable
def Element(name: String, parent: Nullable[XmlElement]): XmlElement = XmlElement(name, parent)""", "com.badlogic.gdx.utils.XmlReader", "def", true)
    )
  )
}
