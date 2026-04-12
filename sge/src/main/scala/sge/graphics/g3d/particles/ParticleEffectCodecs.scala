/*
 * JSON codecs for the 3D particle effect data model.
 *
 * Provides jsoniter-scala codecs for all particle value types, enabling
 * serialization and deserialization of ParticleEffect JSON files.
 *
 * Since the particle data model uses mutable `class` (not `case class`) with `var`
 * fields, Kindlings derivation macros cannot be used directly. Instead, codecs
 * are written manually using jsoniter-scala's low-level API.
 *
 * Ported from libGDX Json.Serializable write/read methods:
 * - values: ParticleValue, NumericValue, RangedNumericValue, ScaledNumericValue,
 *           GradientColorValue, SpawnShapeValue, PrimitiveSpawnShapeValue, EllipseSpawnShapeValue
 * - emitters: Emitter, RegularEmitter
 * - influencers: ColorInfluencer.Single, DynamicsInfluencer, DynamicsModifier.*,
 *                RegionInfluencer.*, SimpleInfluencer, SpawnInfluencer
 * - ParticleControllerComponent
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package graphics
package g3d
package particles

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

import com.github.plokhotnyuk.jsoniter_scala.core.{ JsonReader, JsonValueCodec, JsonWriter, readFromArray }

import sge.graphics.g3d.particles.emitters.{ Emitter, RegularEmitter }
import sge.graphics.g3d.particles.influencers._
import sge.graphics.g3d.particles.renderers._
import sge.graphics.g3d.particles.values._
import sge.utils.Nullable

/** Transport structure for ParticleControllerInfluencer serialization.
  * Maps a ParticleEffect asset filename to the indices of controllers within that effect.
  */
final case class EffectReference(effectFilename: String, controllerIndices: Array[Int])

/** Central registry of 3D particle JSON codecs. Import `ParticleEffectCodecs.given` to bring all codecs into scope.
  */
object ParticleEffectCodecs {

  // ── Helpers ──────────────────────────────────────────────────────────

  private def readBoolean(in: JsonReader): Boolean =
    in.readBoolean()

  private def readFloat(in: JsonReader): Float =
    in.readFloat()

  private def readInt(in: JsonReader): Int =
    in.readInt()

  private def readString(in: JsonReader): String =
    in.readString(null)

  private def readFloatArray(in: JsonReader): Array[Float] = {
    val buf = ArrayBuffer[Float]()
    if (in.isNextToken('[')) {
      if (!in.isNextToken(']')) {
        in.rollbackToken()
        buf += in.readFloat()
        while (in.isNextToken(',')) buf += in.readFloat()
      }
    }
    buf.toArray
  }

  private def writeFloatArray(arr: Array[Float], out: JsonWriter): Unit = {
    out.writeArrayStart()
    var i = 0
    while (i < arr.length) {
      out.writeVal(arr(i))
      i += 1
    }
    out.writeArrayEnd()
  }

  private def readIntArray(in: JsonReader): Array[Int] = {
    val buf = ArrayBuffer[Int]()
    if (in.isNextToken('[')) {
      if (!in.isNextToken(']')) {
        in.rollbackToken()
        buf += in.readInt()
        while (in.isNextToken(',')) buf += in.readInt()
      }
    }
    buf.toArray
  }

  private def writeIntArray(arr: Array[Int], out: JsonWriter): Unit = {
    out.writeArrayStart()
    var i = 0
    while (i < arr.length) {
      out.writeVal(arr(i))
      i += 1
    }
    out.writeArrayEnd()
  }

  private def readStringArray(in: JsonReader): Array[String] = {
    val buf = ArrayBuffer[String]()
    if (in.isNextToken('[')) {
      if (!in.isNextToken(']')) {
        in.rollbackToken()
        buf += in.readString(null)
        while (in.isNextToken(',')) buf += in.readString(null)
      }
    }
    buf.toArray
  }

  private def writeStringArray(arr: Array[String], out: JsonWriter): Unit = {
    out.writeArrayStart()
    var i = 0
    while (i < arr.length) {
      out.writeVal(arr(i))
      i += 1
    }
    out.writeArrayEnd()
  }

  /** Reads a polymorphic JSON object, extracts "class" field, and returns (className, bufferedBytes).
    * The bufferedBytes can be re-parsed with the appropriate type-specific codec.
    * This is necessary because jsoniter-scala doesn't support rewinding the reader.
    */
  private def readPolymorphicObject(in: JsonReader): (Nullable[String], Array[Byte]) = {
    // We need to buffer the entire object to re-parse it
    // jsoniter-scala provides no rewind, so we capture raw bytes
    var className: Nullable[String] = Nullable.empty
    val buf      = ArrayBuffer[Byte]()

    // Read opening brace
    if (!in.isNextToken('{')) {
      return (Nullable.empty, Array.emptyByteArray)
    }
    buf += '{'

    if (!in.isNextToken('}')) {
      in.rollbackToken()
      var continue = true
      var first    = true
      while (continue) {
        if (!first) buf += ','
        first = false
        val key = in.readKeyAsString()
        // Write key to buffer
        buf += '"'
        buf ++= key.getBytes("UTF-8")
        buf += '"'
        buf += ':'
        if (key == "class") {
          val value = in.readString(null)
          className = Nullable(value)
          buf += '"'
          buf ++= value.getBytes("UTF-8")
          buf += '"'
        } else {
          // Buffer the value - we need to capture arbitrary JSON
          bufferJsonValue(in, buf)
        }
        continue = in.isNextToken(',')
      }
    }
    buf += '}'

    (className, buf.toArray)
  }

  /** Buffers a single JSON value (object, array, string, number, boolean, null) into the buffer. */
  private def bufferJsonValue(in: JsonReader, buf: ArrayBuffer[Byte]): Unit = {
    val b = in.nextToken()
    in.rollbackToken()
    b match {
      case '{' =>
        // Object
        buf += '{'
        in.isNextToken('{') // consume
        if (!in.isNextToken('}')) {
          in.rollbackToken()
          var first = true
          var continue = true
          while (continue) {
            if (!first) buf += ','
            first = false
            val key = in.readKeyAsString()
            buf += '"'
            buf ++= key.getBytes("UTF-8")
            buf += '"'
            buf += ':'
            bufferJsonValue(in, buf)
            continue = in.isNextToken(',')
          }
        }
        buf += '}'
      case '[' =>
        // Array
        buf += '['
        in.isNextToken('[') // consume
        if (!in.isNextToken(']')) {
          in.rollbackToken()
          var first = true
          var continue = true
          while (continue) {
            if (!first) buf += ','
            first = false
            bufferJsonValue(in, buf)
            continue = in.isNextToken(',')
          }
        }
        buf += ']'
      case '"' =>
        // String
        val s = in.readString(null)
        buf += '"'
        // Escape the string properly
        buf ++= escapeJsonString(s).getBytes("UTF-8")
        buf += '"'
      case 't' | 'f' =>
        // Boolean
        val v = in.readBoolean()
        buf ++= (if (v) "true" else "false").getBytes("UTF-8")
      case 'n' =>
        // null
        in.readNullOrError(null, "expected null")
        buf ++= "null".getBytes("UTF-8")
      case _ =>
        // Number
        val num = in.readDouble()
        // Check if it's an integer
        if (num == num.toLong.toDouble && num >= Long.MinValue && num <= Long.MaxValue) {
          buf ++= num.toLong.toString.getBytes("UTF-8")
        } else {
          buf ++= num.toString.getBytes("UTF-8")
        }
    }
  }

  private def escapeJsonString(s: String): String = {
    if (s == null) return ""
    val sb = new StringBuilder
    var i = 0
    while (i < s.length) {
      val c = s.charAt(i)
      c match {
        case '"'  => sb.append("\\\"")
        case '\\' => sb.append("\\\\")
        case '\b' => sb.append("\\b")
        case '\f' => sb.append("\\f")
        case '\n' => sb.append("\\n")
        case '\r' => sb.append("\\r")
        case '\t' => sb.append("\\t")
        case _ =>
          if (c < ' ') sb.append(f"\\u${c.toInt}%04x")
          else sb.append(c)
      }
      i += 1
    }
    sb.toString
  }

  /** Reads a JSON object calling fieldHandler for each key. Skips unknown fields. */
  private inline def readFields[A](in: JsonReader, obj: A)(fieldHandler: (String, A, JsonReader) => Boolean): A = {
    if (in.isNextToken('{')) {
      if (!in.isNextToken('}')) {
        in.rollbackToken()
        var continue = true
        while (continue) {
          val key = in.readKeyAsString()
          if (!fieldHandler(key, obj, in)) {
            in.skip() // skip unknown field
          }
          continue = in.isNextToken(',')
        }
      }
    } else in.readNullOrTokenError(obj, '{')
    obj
  }

  // ── ParticleValue codec ─────────────────────────────────────────────

  given particleValueCodec: JsonValueCodec[ParticleValue] = new JsonValueCodec[ParticleValue] {
    override def decodeValue(in: JsonReader, default: ParticleValue): ParticleValue = {
      val value = if (default != null) default else new ParticleValue()
      readFields(in, value) { (key, v, reader) =>
        key match {
          case "active" => v.active = readBoolean(reader); true
          case _        => false
        }
      }
    }

    override def encodeValue(x: ParticleValue, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeKey("active")
      out.writeVal(x.active)
      out.writeObjectEnd()
    }

    override def nullValue: ParticleValue = null
  }

  // ── NumericValue codec ──────────────────────────────────────────────

  given numericValueCodec: JsonValueCodec[NumericValue] = new JsonValueCodec[NumericValue] {
    override def decodeValue(in: JsonReader, default: NumericValue): NumericValue = {
      val value = if (default != null) default else new NumericValue()
      readFields(in, value) { (key, v, reader) =>
        key match {
          case "active" => v.active = readBoolean(reader); true
          case "value"  => v.value = readFloat(reader); true
          case _        => false
        }
      }
    }

    override def encodeValue(x: NumericValue, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeKey("active")
      out.writeVal(x.active)
      out.writeKey("value")
      out.writeVal(x.value)
      out.writeObjectEnd()
    }

    override def nullValue: NumericValue = null
  }

  // ── RangedNumericValue codec ────────────────────────────────────────

  given rangedNumericValueCodec: JsonValueCodec[RangedNumericValue] = new JsonValueCodec[RangedNumericValue] {
    override def decodeValue(in: JsonReader, default: RangedNumericValue): RangedNumericValue = {
      val value = if (default != null) default else new RangedNumericValue()
      readFields(in, value) { (key, v, reader) =>
        key match {
          case "active" => v.active = readBoolean(reader); true
          case "lowMin" => v.lowMin = readFloat(reader); true
          case "lowMax" => v.lowMax = readFloat(reader); true
          case _        => false
        }
      }
    }

    override def encodeValue(x: RangedNumericValue, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeKey("active")
      out.writeVal(x.active)
      out.writeKey("lowMin")
      out.writeVal(x.lowMin)
      out.writeKey("lowMax")
      out.writeVal(x.lowMax)
      out.writeObjectEnd()
    }

    override def nullValue: RangedNumericValue = null
  }

  // ── ScaledNumericValue codec ────────────────────────────────────────

  given scaledNumericValueCodec: JsonValueCodec[ScaledNumericValue] = new JsonValueCodec[ScaledNumericValue] {
    override def decodeValue(in: JsonReader, default: ScaledNumericValue): ScaledNumericValue = {
      val value = if (default != null) default else new ScaledNumericValue()
      readFields(in, value) { (key, v, reader) =>
        key match {
          case "active"   => v.active = readBoolean(reader); true
          case "lowMin"   => v.lowMin = readFloat(reader); true
          case "lowMax"   => v.lowMax = readFloat(reader); true
          case "highMin"  => v.highMin = readFloat(reader); true
          case "highMax"  => v.highMax = readFloat(reader); true
          case "relative" => v.relative = readBoolean(reader); true
          case "scaling"  => v.scaling = readFloatArray(reader); true
          case "timeline" => v.timeline = readFloatArray(reader); true
          case _          => false
        }
      }
    }

    override def encodeValue(x: ScaledNumericValue, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeKey("active")
      out.writeVal(x.active)
      out.writeKey("lowMin")
      out.writeVal(x.lowMin)
      out.writeKey("lowMax")
      out.writeVal(x.lowMax)
      out.writeKey("highMin")
      out.writeVal(x.highMin)
      out.writeKey("highMax")
      out.writeVal(x.highMax)
      out.writeKey("relative")
      out.writeVal(x.relative)
      out.writeKey("scaling")
      writeFloatArray(x.scaling, out)
      out.writeKey("timeline")
      writeFloatArray(x.timeline, out)
      out.writeObjectEnd()
    }

    override def nullValue: ScaledNumericValue = null
  }

  // ── GradientColorValue codec ────────────────────────────────────────

  given gradientColorValueCodec: JsonValueCodec[GradientColorValue] = new JsonValueCodec[GradientColorValue] {
    override def decodeValue(in: JsonReader, default: GradientColorValue): GradientColorValue = {
      val value = if (default != null) default else new GradientColorValue()
      readFields(in, value) { (key, v, reader) =>
        key match {
          case "active"   => v.active = readBoolean(reader); true
          case "colors"   => v.colors = readFloatArray(reader); true
          case "timeline" => v.timeline = readFloatArray(reader); true
          case _          => false
        }
      }
    }

    override def encodeValue(x: GradientColorValue, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeKey("active")
      out.writeVal(x.active)
      out.writeKey("colors")
      writeFloatArray(x.colors, out)
      out.writeKey("timeline")
      writeFloatArray(x.timeline, out)
      out.writeObjectEnd()
    }

    override def nullValue: GradientColorValue = null
  }

  // ── SpawnShapeValue codecs ──────────────────────────────────────────

  // Helper to read/write RangedNumericValue as embedded object
  private def readRangedNumericValue(in: JsonReader): RangedNumericValue =
    rangedNumericValueCodec.decodeValue(in, null)

  private def writeRangedNumericValue(x: RangedNumericValue, out: JsonWriter): Unit =
    rangedNumericValueCodec.encodeValue(x, out)

  private def readScaledNumericValue(in: JsonReader): ScaledNumericValue =
    scaledNumericValueCodec.decodeValue(in, null)

  private def writeScaledNumericValue(x: ScaledNumericValue, out: JsonWriter): Unit =
    scaledNumericValueCodec.encodeValue(x, out)

  // Base SpawnShapeValue fields reader
  private def readSpawnShapeValueFields(key: String, v: SpawnShapeValue, reader: JsonReader): Boolean = key match {
    case "active"       => v.active = readBoolean(reader); true
    case "xOffsetValue" => v.xOffsetValue = readRangedNumericValue(reader); true
    case "yOffsetValue" => v.yOffsetValue = readRangedNumericValue(reader); true
    case "zOffsetValue" => v.zOffsetValue = readRangedNumericValue(reader); true
    case _              => false
  }

  // Base SpawnShapeValue fields writer
  private def writeSpawnShapeValueFields(x: SpawnShapeValue, out: JsonWriter): Unit = {
    out.writeKey("active")
    out.writeVal(x.active)
    out.writeKey("xOffsetValue")
    writeRangedNumericValue(x.xOffsetValue, out)
    out.writeKey("yOffsetValue")
    writeRangedNumericValue(x.yOffsetValue, out)
    out.writeKey("zOffsetValue")
    writeRangedNumericValue(x.zOffsetValue, out)
  }

  // PrimitiveSpawnShapeValue fields reader (extends SpawnShapeValue)
  private def readPrimitiveSpawnShapeValueFields(key: String, v: PrimitiveSpawnShapeValue, reader: JsonReader): Boolean = key match {
    case "spawnWidthValue"  => v.spawnWidthValue = readScaledNumericValue(reader); true
    case "spawnHeightValue" => v.spawnHeightValue = readScaledNumericValue(reader); true
    case "spawnDepthValue"  => v.spawnDepthValue = readScaledNumericValue(reader); true
    case "edges"            => v.edges = readBoolean(reader); true
    case _                  => readSpawnShapeValueFields(key, v, reader)
  }

  // PrimitiveSpawnShapeValue fields writer
  private def writePrimitiveSpawnShapeValueFields(x: PrimitiveSpawnShapeValue, out: JsonWriter): Unit = {
    writeSpawnShapeValueFields(x, out)
    out.writeKey("spawnWidthValue")
    writeScaledNumericValue(x.spawnWidthValue, out)
    out.writeKey("spawnHeightValue")
    writeScaledNumericValue(x.spawnHeightValue, out)
    out.writeKey("spawnDepthValue")
    writeScaledNumericValue(x.spawnDepthValue, out)
    out.writeKey("edges")
    out.writeVal(x.edges)
  }

  given pointSpawnShapeValueCodec: JsonValueCodec[PointSpawnShapeValue] = new JsonValueCodec[PointSpawnShapeValue] {
    override def decodeValue(in: JsonReader, default: PointSpawnShapeValue): PointSpawnShapeValue = {
      val value = if (default != null) default else new PointSpawnShapeValue()
      readFields(in, value) { (key, v, reader) =>
        readSpawnShapeValueFields(key, v, reader)
      }
    }

    override def encodeValue(x: PointSpawnShapeValue, out: JsonWriter): Unit = {
      out.writeObjectStart()
      writeSpawnShapeValueFields(x, out)
      out.writeObjectEnd()
    }

    override def nullValue: PointSpawnShapeValue = null
  }

  given cylinderSpawnShapeValueCodec: JsonValueCodec[CylinderSpawnShapeValue] = new JsonValueCodec[CylinderSpawnShapeValue] {
    override def decodeValue(in: JsonReader, default: CylinderSpawnShapeValue): CylinderSpawnShapeValue = {
      val value = if (default != null) default else new CylinderSpawnShapeValue()
      readFields(in, value) { (key, v, reader) =>
        readPrimitiveSpawnShapeValueFields(key, v, reader)
      }
    }

    override def encodeValue(x: CylinderSpawnShapeValue, out: JsonWriter): Unit = {
      out.writeObjectStart()
      writePrimitiveSpawnShapeValueFields(x, out)
      out.writeObjectEnd()
    }

    override def nullValue: CylinderSpawnShapeValue = null
  }

  given lineSpawnShapeValueCodec: JsonValueCodec[LineSpawnShapeValue] = new JsonValueCodec[LineSpawnShapeValue] {
    override def decodeValue(in: JsonReader, default: LineSpawnShapeValue): LineSpawnShapeValue = {
      val value = if (default != null) default else new LineSpawnShapeValue()
      readFields(in, value) { (key, v, reader) =>
        readPrimitiveSpawnShapeValueFields(key, v, reader)
      }
    }

    override def encodeValue(x: LineSpawnShapeValue, out: JsonWriter): Unit = {
      out.writeObjectStart()
      writePrimitiveSpawnShapeValueFields(x, out)
      out.writeObjectEnd()
    }

    override def nullValue: LineSpawnShapeValue = null
  }

  given rectangleSpawnShapeValueCodec: JsonValueCodec[RectangleSpawnShapeValue] = new JsonValueCodec[RectangleSpawnShapeValue] {
    override def decodeValue(in: JsonReader, default: RectangleSpawnShapeValue): RectangleSpawnShapeValue = {
      val value = if (default != null) default else new RectangleSpawnShapeValue()
      readFields(in, value) { (key, v, reader) =>
        readPrimitiveSpawnShapeValueFields(key, v, reader)
      }
    }

    override def encodeValue(x: RectangleSpawnShapeValue, out: JsonWriter): Unit = {
      out.writeObjectStart()
      writePrimitiveSpawnShapeValueFields(x, out)
      out.writeObjectEnd()
    }

    override def nullValue: RectangleSpawnShapeValue = null
  }

  given ellipseSpawnShapeValueCodec: JsonValueCodec[EllipseSpawnShapeValue] = new JsonValueCodec[EllipseSpawnShapeValue] {
    override def decodeValue(in: JsonReader, default: EllipseSpawnShapeValue): EllipseSpawnShapeValue = {
      val value = if (default != null) default else new EllipseSpawnShapeValue()
      readFields(in, value) { (key, v, reader) =>
        key match {
          case "side" =>
            val sideStr = readString(reader)
            v.side = PrimitiveSpawnShapeValue.SpawnSide.valueOf(sideStr)
            true
          case _ => readPrimitiveSpawnShapeValueFields(key, v, reader)
        }
      }
    }

    override def encodeValue(x: EllipseSpawnShapeValue, out: JsonWriter): Unit = {
      out.writeObjectStart()
      writePrimitiveSpawnShapeValueFields(x, out)
      out.writeKey("side")
      out.writeVal(x.side.toString)
      out.writeObjectEnd()
    }

    override def nullValue: EllipseSpawnShapeValue = null
  }

  // MeshSpawnShapeValue codecs (ISS-467)
  // Note: MeshSpawnShapeValue uses save/load (ResourceData) for mesh/model references,
  // not JSON fields. The JSON codec handles only the base SpawnShapeValue fields.

  given unweightedMeshSpawnShapeValueCodec: JsonValueCodec[UnweightedMeshSpawnShapeValue] =
    new JsonValueCodec[UnweightedMeshSpawnShapeValue] {
      override def decodeValue(in: JsonReader, default: UnweightedMeshSpawnShapeValue): UnweightedMeshSpawnShapeValue = {
        val value = if (default != null) default else new UnweightedMeshSpawnShapeValue()
        readFields(in, value) { (key, v, reader) =>
          readSpawnShapeValueFields(key, v, reader)
        }
      }

      override def encodeValue(x: UnweightedMeshSpawnShapeValue, out: JsonWriter): Unit = {
        out.writeObjectStart()
        writeSpawnShapeValueFields(x, out)
        out.writeObjectEnd()
      }

      override def nullValue: UnweightedMeshSpawnShapeValue = null
    }

  given weightMeshSpawnShapeValueCodec: JsonValueCodec[WeightMeshSpawnShapeValue] =
    new JsonValueCodec[WeightMeshSpawnShapeValue] {
      override def decodeValue(in: JsonReader, default: WeightMeshSpawnShapeValue): WeightMeshSpawnShapeValue = {
        val value = if (default != null) default else new WeightMeshSpawnShapeValue()
        readFields(in, value) { (key, v, reader) =>
          readSpawnShapeValueFields(key, v, reader)
        }
      }

      override def encodeValue(x: WeightMeshSpawnShapeValue, out: JsonWriter): Unit = {
        out.writeObjectStart()
        writeSpawnShapeValueFields(x, out)
        out.writeObjectEnd()
      }

      override def nullValue: WeightMeshSpawnShapeValue = null
    }

  // ── Emitter codecs ──────────────────────────────────────────────────

  // Base Emitter fields reader
  private def readEmitterFields(key: String, v: Emitter, reader: JsonReader): Boolean = key match {
    case "minParticleCount" => v.minParticleCount = readInt(reader); true
    case "maxParticleCount" => v.maxParticleCount = readInt(reader); true
    case _                  => false
  }

  // Base Emitter fields writer
  private def writeEmitterFields(x: Emitter, out: JsonWriter): Unit = {
    out.writeKey("minParticleCount")
    out.writeVal(x.minParticleCount)
    out.writeKey("maxParticleCount")
    out.writeVal(x.maxParticleCount)
  }

  given regularEmitterCodec: JsonValueCodec[RegularEmitter] = new JsonValueCodec[RegularEmitter] {
    override def decodeValue(in: JsonReader, default: RegularEmitter): RegularEmitter = {
      val value = if (default != null) default else new RegularEmitter()
      readFields(in, value) { (key, v, reader) =>
        key match {
          // Note: LibGDX JSON has typo "continous" instead of "continuous"
          case "continous" | "continuous" => v.continuous = readBoolean(reader); true
          case "emission"                 => v.emissionValue = readScaledNumericValue(reader); true
          case "delay"                    => v.delayValue = readRangedNumericValue(reader); true
          case "duration"                 => v.durationValue = readRangedNumericValue(reader); true
          case "life"                     => v.lifeValue = readScaledNumericValue(reader); true
          case "lifeOffset"               => v.lifeOffsetValue = readScaledNumericValue(reader); true
          case _                          => readEmitterFields(key, v, reader)
        }
      }
    }

    override def encodeValue(x: RegularEmitter, out: JsonWriter): Unit = {
      out.writeObjectStart()
      writeEmitterFields(x, out)
      // Preserve LibGDX typo for compatibility
      out.writeKey("continous")
      out.writeVal(x.continuous)
      out.writeKey("emission")
      writeScaledNumericValue(x.emissionValue, out)
      out.writeKey("delay")
      writeRangedNumericValue(x.delayValue, out)
      out.writeKey("duration")
      writeRangedNumericValue(x.durationValue, out)
      out.writeKey("life")
      writeScaledNumericValue(x.lifeValue, out)
      out.writeKey("lifeOffset")
      writeScaledNumericValue(x.lifeOffsetValue, out)
      out.writeObjectEnd()
    }

    override def nullValue: RegularEmitter = null
  }

  // ── Influencer codecs ───────────────────────────────────────────────

  given colorInfluencerSingleCodec: JsonValueCodec[ColorInfluencer.Single] = new JsonValueCodec[ColorInfluencer.Single] {
    override def decodeValue(in: JsonReader, default: ColorInfluencer.Single): ColorInfluencer.Single = {
      val value = if (default != null) default else new ColorInfluencer.Single()
      readFields(in, value) { (key, v, reader) =>
        key match {
          case "alpha" => v.alphaValue = readScaledNumericValue(reader); true
          case "color" => v.colorValue = gradientColorValueCodec.decodeValue(reader, null); true
          case _       => false
        }
      }
    }

    override def encodeValue(x: ColorInfluencer.Single, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeKey("alpha")
      writeScaledNumericValue(x.alphaValue, out)
      out.writeKey("color")
      gradientColorValueCodec.encodeValue(x.colorValue, out)
      out.writeObjectEnd()
    }

    override def nullValue: ColorInfluencer.Single = null
  }

  given colorInfluencerRandomCodec: JsonValueCodec[ColorInfluencer.Random] = new JsonValueCodec[ColorInfluencer.Random] {
    override def decodeValue(in: JsonReader, default: ColorInfluencer.Random): ColorInfluencer.Random = {
      val value = if (default != null) default else new ColorInfluencer.Random()
      // ColorInfluencer.Random has no serializable fields
      readFields(in, value) { (_, _, reader) =>
        reader.skip()
        true
      }
    }

    override def encodeValue(x: ColorInfluencer.Random, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeObjectEnd()
    }

    override def nullValue: ColorInfluencer.Random = null
  }

  // DynamicsModifier base fields
  private def readDynamicsModifierFields(key: String, v: DynamicsModifier, reader: JsonReader): Boolean = key match {
    case "isGlobal" => v.isGlobal = readBoolean(reader); true
    case _          => false
  }

  private def writeDynamicsModifierFields(x: DynamicsModifier, out: JsonWriter): Unit = {
    out.writeKey("isGlobal")
    out.writeVal(x.isGlobal)
  }

  // DynamicsModifier.Strength fields
  private def readStrengthFields(key: String, v: DynamicsModifier.Strength, reader: JsonReader): Boolean = key match {
    case "strengthValue" => v.strengthValue = readScaledNumericValue(reader); true
    case _               => readDynamicsModifierFields(key, v, reader)
  }

  private def writeStrengthFields(x: DynamicsModifier.Strength, out: JsonWriter): Unit = {
    writeDynamicsModifierFields(x, out)
    out.writeKey("strengthValue")
    writeScaledNumericValue(x.strengthValue, out)
  }

  // DynamicsModifier.Angular fields
  private def readAngularFields(key: String, v: DynamicsModifier.Angular, reader: JsonReader): Boolean = key match {
    case "thetaValue" => v.thetaValue = readScaledNumericValue(reader); true
    case "phiValue"   => v.phiValue = readScaledNumericValue(reader); true
    case _            => readStrengthFields(key, v, reader)
  }

  private def writeAngularFields(x: DynamicsModifier.Angular, out: JsonWriter): Unit = {
    writeStrengthFields(x, out)
    out.writeKey("thetaValue")
    writeScaledNumericValue(x.thetaValue, out)
    out.writeKey("phiValue")
    writeScaledNumericValue(x.phiValue, out)
  }

  given faceDirectionCodec: JsonValueCodec[DynamicsModifier.FaceDirection] = new JsonValueCodec[DynamicsModifier.FaceDirection] {
    override def decodeValue(in: JsonReader, default: DynamicsModifier.FaceDirection): DynamicsModifier.FaceDirection = {
      val value = if (default != null) default else new DynamicsModifier.FaceDirection()
      readFields(in, value) { (key, v, reader) =>
        readDynamicsModifierFields(key, v, reader)
      }
    }

    override def encodeValue(x: DynamicsModifier.FaceDirection, out: JsonWriter): Unit = {
      out.writeObjectStart()
      writeDynamicsModifierFields(x, out)
      out.writeObjectEnd()
    }

    override def nullValue: DynamicsModifier.FaceDirection = null
  }

  given rotational2DCodec: JsonValueCodec[DynamicsModifier.Rotational2D] = new JsonValueCodec[DynamicsModifier.Rotational2D] {
    override def decodeValue(in: JsonReader, default: DynamicsModifier.Rotational2D): DynamicsModifier.Rotational2D = {
      val value = if (default != null) default else new DynamicsModifier.Rotational2D()
      readFields(in, value) { (key, v, reader) =>
        readStrengthFields(key, v, reader)
      }
    }

    override def encodeValue(x: DynamicsModifier.Rotational2D, out: JsonWriter): Unit = {
      out.writeObjectStart()
      writeStrengthFields(x, out)
      out.writeObjectEnd()
    }

    override def nullValue: DynamicsModifier.Rotational2D = null
  }

  given rotational3DCodec: JsonValueCodec[DynamicsModifier.Rotational3D] = new JsonValueCodec[DynamicsModifier.Rotational3D] {
    override def decodeValue(in: JsonReader, default: DynamicsModifier.Rotational3D): DynamicsModifier.Rotational3D = {
      val value = if (default != null) default else new DynamicsModifier.Rotational3D()
      readFields(in, value) { (key, v, reader) =>
        readAngularFields(key, v, reader)
      }
    }

    override def encodeValue(x: DynamicsModifier.Rotational3D, out: JsonWriter): Unit = {
      out.writeObjectStart()
      writeAngularFields(x, out)
      out.writeObjectEnd()
    }

    override def nullValue: DynamicsModifier.Rotational3D = null
  }

  given centripetalAccelerationCodec: JsonValueCodec[DynamicsModifier.CentripetalAcceleration] =
    new JsonValueCodec[DynamicsModifier.CentripetalAcceleration] {
      override def decodeValue(in: JsonReader, default: DynamicsModifier.CentripetalAcceleration): DynamicsModifier.CentripetalAcceleration = {
        val value = if (default != null) default else new DynamicsModifier.CentripetalAcceleration()
        readFields(in, value) { (key, v, reader) =>
          readStrengthFields(key, v, reader)
        }
      }

      override def encodeValue(x: DynamicsModifier.CentripetalAcceleration, out: JsonWriter): Unit = {
        out.writeObjectStart()
        writeStrengthFields(x, out)
        out.writeObjectEnd()
      }

      override def nullValue: DynamicsModifier.CentripetalAcceleration = null
    }

  given polarAccelerationCodec: JsonValueCodec[DynamicsModifier.PolarAcceleration] =
    new JsonValueCodec[DynamicsModifier.PolarAcceleration] {
      override def decodeValue(in: JsonReader, default: DynamicsModifier.PolarAcceleration): DynamicsModifier.PolarAcceleration = {
        val value = if (default != null) default else new DynamicsModifier.PolarAcceleration()
        readFields(in, value) { (key, v, reader) =>
          readAngularFields(key, v, reader)
        }
      }

      override def encodeValue(x: DynamicsModifier.PolarAcceleration, out: JsonWriter): Unit = {
        out.writeObjectStart()
        writeAngularFields(x, out)
        out.writeObjectEnd()
      }

      override def nullValue: DynamicsModifier.PolarAcceleration = null
    }

  given tangentialAccelerationCodec: JsonValueCodec[DynamicsModifier.TangentialAcceleration] =
    new JsonValueCodec[DynamicsModifier.TangentialAcceleration] {
      override def decodeValue(in: JsonReader, default: DynamicsModifier.TangentialAcceleration): DynamicsModifier.TangentialAcceleration = {
        val value = if (default != null) default else new DynamicsModifier.TangentialAcceleration()
        readFields(in, value) { (key, v, reader) =>
          readAngularFields(key, v, reader)
        }
      }

      override def encodeValue(x: DynamicsModifier.TangentialAcceleration, out: JsonWriter): Unit = {
        out.writeObjectStart()
        writeAngularFields(x, out)
        out.writeObjectEnd()
      }

      override def nullValue: DynamicsModifier.TangentialAcceleration = null
    }

  given brownianAccelerationCodec: JsonValueCodec[DynamicsModifier.BrownianAcceleration] =
    new JsonValueCodec[DynamicsModifier.BrownianAcceleration] {
      override def decodeValue(in: JsonReader, default: DynamicsModifier.BrownianAcceleration): DynamicsModifier.BrownianAcceleration = {
        val value = if (default != null) default else new DynamicsModifier.BrownianAcceleration()
        readFields(in, value) { (key, v, reader) =>
          readStrengthFields(key, v, reader)
        }
      }

      override def encodeValue(x: DynamicsModifier.BrownianAcceleration, out: JsonWriter): Unit = {
        out.writeObjectStart()
        writeStrengthFields(x, out)
        out.writeObjectEnd()
      }

      override def nullValue: DynamicsModifier.BrownianAcceleration = null
    }

  // Polymorphic DynamicsModifier codec
  given dynamicsModifierCodec: JsonValueCodec[DynamicsModifier] = new JsonValueCodec[DynamicsModifier] {
    override def decodeValue(in: JsonReader, default: DynamicsModifier): DynamicsModifier = {
      // DynamicsModifier is abstract and serialized with type info in array context by DynamicsInfluencer
      // This codec handles direct serialization for embedded objects
      throw new UnsupportedOperationException(
        "DynamicsModifier cannot be decoded directly; use typed codecs or DynamicsInfluencer's velocities array"
      )
    }

    override def encodeValue(x: DynamicsModifier, out: JsonWriter): Unit =
      x match {
        case v: DynamicsModifier.FaceDirection          => faceDirectionCodec.encodeValue(v, out)
        case v: DynamicsModifier.Rotational2D           => rotational2DCodec.encodeValue(v, out)
        case v: DynamicsModifier.Rotational3D           => rotational3DCodec.encodeValue(v, out)
        case v: DynamicsModifier.CentripetalAcceleration => centripetalAccelerationCodec.encodeValue(v, out)
        case v: DynamicsModifier.PolarAcceleration      => polarAccelerationCodec.encodeValue(v, out)
        case v: DynamicsModifier.TangentialAcceleration => tangentialAccelerationCodec.encodeValue(v, out)
        case v: DynamicsModifier.BrownianAcceleration   => brownianAccelerationCodec.encodeValue(v, out)
        case _                                          => throw new UnsupportedOperationException(s"Unknown DynamicsModifier type: ${x.getClass}")
      }

    override def nullValue: DynamicsModifier = null
  }

  // Map of class names to DynamicsModifier decoders (ISS-464 fix: use byte array buffering)
  private val modifierTypes: Map[String, Array[Byte] => DynamicsModifier] = Map(
    classOf[DynamicsModifier.FaceDirection].getName -> (bytes => readFromArray[DynamicsModifier.FaceDirection](bytes)),
    classOf[DynamicsModifier.Rotational2D].getName -> (bytes => readFromArray[DynamicsModifier.Rotational2D](bytes)),
    classOf[DynamicsModifier.Rotational3D].getName -> (bytes => readFromArray[DynamicsModifier.Rotational3D](bytes)),
    classOf[DynamicsModifier.CentripetalAcceleration].getName -> (bytes => readFromArray[DynamicsModifier.CentripetalAcceleration](bytes)),
    classOf[DynamicsModifier.PolarAcceleration].getName -> (bytes => readFromArray[DynamicsModifier.PolarAcceleration](bytes)),
    classOf[DynamicsModifier.TangentialAcceleration].getName -> (bytes => readFromArray[DynamicsModifier.TangentialAcceleration](bytes)),
    classOf[DynamicsModifier.BrownianAcceleration].getName -> (bytes => readFromArray[DynamicsModifier.BrownianAcceleration](bytes)),
    // LibGDX class names for compatibility
    "com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier$FaceDirection" -> (bytes => readFromArray[DynamicsModifier.FaceDirection](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier$Rotational2D" -> (bytes => readFromArray[DynamicsModifier.Rotational2D](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier$Rotational3D" -> (bytes => readFromArray[DynamicsModifier.Rotational3D](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier$CentripetalAcceleration" -> (bytes =>
      readFromArray[DynamicsModifier.CentripetalAcceleration](bytes)
    ),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier$PolarAcceleration" -> (bytes =>
      readFromArray[DynamicsModifier.PolarAcceleration](bytes)
    ),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier$TangentialAcceleration" -> (bytes =>
      readFromArray[DynamicsModifier.TangentialAcceleration](bytes)
    ),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsModifier$BrownianAcceleration" -> (bytes =>
      readFromArray[DynamicsModifier.BrownianAcceleration](bytes)
    )
  )

  /** Decodes a polymorphic DynamicsModifier from a JSON object with "class" field. */
  private def decodeDynamicsModifier(in: JsonReader): DynamicsModifier = {
    val (classNameOpt, bytes) = readPolymorphicObject(in)
    classNameOpt.fold {
      throw new UnsupportedOperationException("DynamicsModifier object missing 'class' field")
    } { className =>
      modifierTypes.get(className) match {
        case Some(decoder) => decoder(bytes)
        case None =>
          throw new UnsupportedOperationException(s"Unknown DynamicsModifier type: $className")
      }
    }
  }

  // DynamicsInfluencer codec with polymorphic velocities array (ISS-464 fix)
  given dynamicsInfluencerCodec: JsonValueCodec[DynamicsInfluencer] = new JsonValueCodec[DynamicsInfluencer] {

    override def decodeValue(in: JsonReader, default: DynamicsInfluencer): DynamicsInfluencer = {
      val value = if (default != null) default else new DynamicsInfluencer()
      readFields(in, value) { (key, v, reader) =>
        key match {
          case "velocities" =>
            // LibGDX serializes Array with class tags: [{class: "...", ...}, ...]
            if (reader.isNextToken('[')) {
              if (!reader.isNextToken(']')) {
                reader.rollbackToken()
                var continue = true
                while (continue) {
                  // Use buffering approach to properly decode each modifier
                  val modifier = decodeDynamicsModifier(reader)
                  v.velocities.add(modifier)
                  continue = reader.isNextToken(',')
                }
              }
            }
            true
          case _ => false
        }
      }
    }

    override def encodeValue(x: DynamicsInfluencer, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeKey("velocities")
      out.writeArrayStart()
      for (i <- 0 until x.velocities.size) {
        val modifier = x.velocities(i)
        out.writeObjectStart()
        out.writeKey("class")
        out.writeVal(modifier.getClass.getName)
        // Write modifier fields
        modifier match {
          case v: DynamicsModifier.FaceDirection =>
            writeDynamicsModifierFields(v, out)
          case v: DynamicsModifier.Rotational2D =>
            writeStrengthFields(v, out)
          case v: DynamicsModifier.Rotational3D =>
            writeAngularFields(v, out)
          case v: DynamicsModifier.CentripetalAcceleration =>
            writeStrengthFields(v, out)
          case v: DynamicsModifier.PolarAcceleration =>
            writeAngularFields(v, out)
          case v: DynamicsModifier.TangentialAcceleration =>
            writeAngularFields(v, out)
          case v: DynamicsModifier.BrownianAcceleration =>
            writeStrengthFields(v, out)
          case _ => ()
        }
        out.writeObjectEnd()
      }
      out.writeArrayEnd()
      out.writeObjectEnd()
    }

    override def nullValue: DynamicsInfluencer = null
  }

  // RegionInfluencer.AspectTextureRegion codec
  given aspectTextureRegionCodec: JsonValueCodec[RegionInfluencer.AspectTextureRegion] =
    new JsonValueCodec[RegionInfluencer.AspectTextureRegion] {
      override def decodeValue(in: JsonReader, default: RegionInfluencer.AspectTextureRegion): RegionInfluencer.AspectTextureRegion = {
        val value = if (default != null) default else new RegionInfluencer.AspectTextureRegion()
        readFields(in, value) { (key, v, reader) =>
          key match {
            case "u"                  => v.u = readFloat(reader); true
            case "v"                  => v.v = readFloat(reader); true
            case "u2"                 => v.u2 = readFloat(reader); true
            case "v2"                 => v.v2 = readFloat(reader); true
            case "halfInvAspectRatio" => v.halfInvAspectRatio = readFloat(reader); true
            case "imageName" =>
              val s = readString(reader)
              v.imageName = if (s != null) Nullable(s) else Nullable.empty
              true
            case _ => false
          }
        }
      }

      override def encodeValue(x: RegionInfluencer.AspectTextureRegion, out: JsonWriter): Unit = {
        out.writeObjectStart()
        out.writeKey("u")
        out.writeVal(x.u)
        out.writeKey("v")
        out.writeVal(x.v)
        out.writeKey("u2")
        out.writeVal(x.u2)
        out.writeKey("v2")
        out.writeVal(x.v2)
        out.writeKey("halfInvAspectRatio")
        out.writeVal(x.halfInvAspectRatio)
        x.imageName.foreach { name =>
          out.writeKey("imageName")
          out.writeVal(name)
        }
        out.writeObjectEnd()
      }

      override def nullValue: RegionInfluencer.AspectTextureRegion = null
    }

  // RegionInfluencer base fields
  private def readRegionInfluencerFields(key: String, v: RegionInfluencer, reader: JsonReader): Boolean = key match {
    case "regions" =>
      v.regions.clear()
      if (reader.isNextToken('[')) {
        if (!reader.isNextToken(']')) {
          reader.rollbackToken()
          v.regions.add(aspectTextureRegionCodec.decodeValue(reader, null))
          while (reader.isNextToken(','))
            v.regions.add(aspectTextureRegionCodec.decodeValue(reader, null))
        }
      }
      true
    case _ => false
  }

  private def writeRegionInfluencerFields(x: RegionInfluencer, out: JsonWriter): Unit = {
    out.writeKey("regions")
    out.writeArrayStart()
    for (i <- 0 until x.regions.size)
      aspectTextureRegionCodec.encodeValue(x.regions(i), out)
    out.writeArrayEnd()
  }

  given regionInfluencerSingleCodec: JsonValueCodec[RegionInfluencer.Single] =
    new JsonValueCodec[RegionInfluencer.Single] {
      override def decodeValue(in: JsonReader, default: RegionInfluencer.Single): RegionInfluencer.Single = {
        val value = if (default != null) default else new RegionInfluencer.Single()
        readFields(in, value) { (key, v, reader) =>
          readRegionInfluencerFields(key, v, reader)
        }
      }

      override def encodeValue(x: RegionInfluencer.Single, out: JsonWriter): Unit = {
        out.writeObjectStart()
        writeRegionInfluencerFields(x, out)
        out.writeObjectEnd()
      }

      override def nullValue: RegionInfluencer.Single = null
    }

  given regionInfluencerRandomCodec: JsonValueCodec[RegionInfluencer.Random] =
    new JsonValueCodec[RegionInfluencer.Random] {
      override def decodeValue(in: JsonReader, default: RegionInfluencer.Random): RegionInfluencer.Random = {
        val value = if (default != null) default else new RegionInfluencer.Random()
        readFields(in, value) { (key, v, reader) =>
          readRegionInfluencerFields(key, v, reader)
        }
      }

      override def encodeValue(x: RegionInfluencer.Random, out: JsonWriter): Unit = {
        out.writeObjectStart()
        writeRegionInfluencerFields(x, out)
        out.writeObjectEnd()
      }

      override def nullValue: RegionInfluencer.Random = null
    }

  given regionInfluencerAnimatedCodec: JsonValueCodec[RegionInfluencer.Animated] =
    new JsonValueCodec[RegionInfluencer.Animated] {
      override def decodeValue(in: JsonReader, default: RegionInfluencer.Animated): RegionInfluencer.Animated = {
        val value = if (default != null) default else new RegionInfluencer.Animated()
        readFields(in, value) { (key, v, reader) =>
          readRegionInfluencerFields(key, v, reader)
        }
      }

      override def encodeValue(x: RegionInfluencer.Animated, out: JsonWriter): Unit = {
        out.writeObjectStart()
        writeRegionInfluencerFields(x, out)
        out.writeObjectEnd()
      }

      override def nullValue: RegionInfluencer.Animated = null
    }

  // SimpleInfluencer codec (base class)
  private def readSimpleInfluencerFields(key: String, v: SimpleInfluencer, reader: JsonReader): Boolean = key match {
    case "value" => v.value = readScaledNumericValue(reader); true
    case _       => false
  }

  private def writeSimpleInfluencerFields(x: SimpleInfluencer, out: JsonWriter): Unit = {
    out.writeKey("value")
    writeScaledNumericValue(x.value, out)
  }

  given scaleInfluencerCodec: JsonValueCodec[ScaleInfluencer] = new JsonValueCodec[ScaleInfluencer] {
    override def decodeValue(in: JsonReader, default: ScaleInfluencer): ScaleInfluencer = {
      val value = if (default != null) default else new ScaleInfluencer()
      readFields(in, value) { (key, v, reader) =>
        readSimpleInfluencerFields(key, v, reader)
      }
    }

    override def encodeValue(x: ScaleInfluencer, out: JsonWriter): Unit = {
      out.writeObjectStart()
      writeSimpleInfluencerFields(x, out)
      out.writeObjectEnd()
    }

    override def nullValue: ScaleInfluencer = null
  }

  // ── Polymorphic SpawnShapeValue codec ────────────────────────────────

  // Map of class names to SpawnShapeValue decoders
  private val spawnShapeTypes: Map[String, Array[Byte] => SpawnShapeValue] = Map(
    classOf[PointSpawnShapeValue].getName -> (bytes => readFromArray[PointSpawnShapeValue](bytes)),
    classOf[EllipseSpawnShapeValue].getName -> (bytes => readFromArray[EllipseSpawnShapeValue](bytes)),
    classOf[CylinderSpawnShapeValue].getName -> (bytes => readFromArray[CylinderSpawnShapeValue](bytes)),
    classOf[LineSpawnShapeValue].getName -> (bytes => readFromArray[LineSpawnShapeValue](bytes)),
    classOf[RectangleSpawnShapeValue].getName -> (bytes => readFromArray[RectangleSpawnShapeValue](bytes)),
    // MeshSpawnShapeValue types (ISS-467)
    classOf[UnweightedMeshSpawnShapeValue].getName -> (bytes => readFromArray[UnweightedMeshSpawnShapeValue](bytes)),
    classOf[WeightMeshSpawnShapeValue].getName -> (bytes => readFromArray[WeightMeshSpawnShapeValue](bytes)),
    // LibGDX class names for compatibility
    "com.badlogic.gdx.graphics.g3d.particles.values.PointSpawnShapeValue" -> (bytes => readFromArray[PointSpawnShapeValue](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.values.EllipseSpawnShapeValue" -> (bytes => readFromArray[EllipseSpawnShapeValue](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.values.CylinderSpawnShapeValue" -> (bytes => readFromArray[CylinderSpawnShapeValue](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.values.LineSpawnShapeValue" -> (bytes => readFromArray[LineSpawnShapeValue](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.values.RectangleSpawnShapeValue" -> (bytes => readFromArray[RectangleSpawnShapeValue](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.values.UnweightedMeshSpawnShapeValue" -> (bytes => readFromArray[UnweightedMeshSpawnShapeValue](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.values.WeightMeshSpawnShapeValue" -> (bytes => readFromArray[WeightMeshSpawnShapeValue](bytes))
  )

  /** Decodes a polymorphic SpawnShapeValue from a JSON object with "class" field. */
  private def decodeSpawnShapeValue(in: JsonReader): SpawnShapeValue = {
    val (classNameOpt, bytes) = readPolymorphicObject(in)
    classNameOpt.fold {
      // No class field, default to PointSpawnShapeValue
      new PointSpawnShapeValue()
    } { className =>
      spawnShapeTypes.get(className) match {
        case Some(decoder) => decoder(bytes)
        case None =>
          // Unknown type, default to PointSpawnShapeValue
          new PointSpawnShapeValue()
      }
    }
  }

  // SpawnInfluencer codec - with polymorphic SpawnShapeValue handling (ISS-463 fix)
  given spawnInfluencerCodec: JsonValueCodec[SpawnInfluencer] = new JsonValueCodec[SpawnInfluencer] {
    override def decodeValue(in: JsonReader, default: SpawnInfluencer): SpawnInfluencer = {
      val value = if (default != null) default else new SpawnInfluencer()
      readFields(in, value) { (key, v, reader) =>
        key match {
          case "spawnShape" =>
            v.spawnShapeValue = decodeSpawnShapeValue(reader)
            true
          case _ => false
        }
      }
    }

    override def encodeValue(x: SpawnInfluencer, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeKey("spawnShape")
      // Write polymorphic object with class field
      out.writeObjectStart()
      out.writeKey("class")
      out.writeVal(x.spawnShapeValue.getClass.getName)
      // Write shape fields based on type
      x.spawnShapeValue match {
        case v: EllipseSpawnShapeValue =>
          writePrimitiveSpawnShapeValueFields(v, out)
          out.writeKey("side")
          out.writeVal(v.side.toString)
        case v: CylinderSpawnShapeValue =>
          writePrimitiveSpawnShapeValueFields(v, out)
        case v: LineSpawnShapeValue =>
          writePrimitiveSpawnShapeValueFields(v, out)
        case v: RectangleSpawnShapeValue =>
          writePrimitiveSpawnShapeValueFields(v, out)
        case v: PointSpawnShapeValue =>
          writeSpawnShapeValueFields(v, out)
        // MeshSpawnShapeValue types use save/load for mesh data (ISS-467)
        case v: UnweightedMeshSpawnShapeValue =>
          writeSpawnShapeValueFields(v, out)
        case v: WeightMeshSpawnShapeValue =>
          writeSpawnShapeValueFields(v, out)
        case v =>
          writeSpawnShapeValueFields(v, out)
      }
      out.writeObjectEnd()
      out.writeObjectEnd()
    }

    override def nullValue: SpawnInfluencer = null
  }

  // ── Polymorphic Emitter codec ───────────────────────────────────────

  // Map of class names to Emitter decoders
  private val emitterTypes: Map[String, Array[Byte] => Emitter] = Map(
    classOf[RegularEmitter].getName -> (bytes => readFromArray[RegularEmitter](bytes)),
    // LibGDX class names for compatibility
    "com.badlogic.gdx.graphics.g3d.particles.emitters.RegularEmitter" -> (bytes => readFromArray[RegularEmitter](bytes))
  )

  /** Decodes a polymorphic Emitter from a JSON object with "class" field. */
  private def decodeEmitter(in: JsonReader): Emitter = {
    val (classNameOpt, bytes) = readPolymorphicObject(in)
    classNameOpt.fold {
      // No class field, try to decode as RegularEmitter
      new RegularEmitter()
    } { className =>
      emitterTypes.get(className) match {
        case Some(decoder) => decoder(bytes)
        case None          =>
          // Unknown type, default to RegularEmitter
          readFromArray[RegularEmitter](bytes)
      }
    }
  }

  given emitterCodec: JsonValueCodec[Emitter] = new JsonValueCodec[Emitter] {
    override def decodeValue(in: JsonReader, default: Emitter): Emitter =
      decodeEmitter(in)

    override def encodeValue(x: Emitter, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeKey("class")
      out.writeVal(x.getClass.getName)
      x match {
        case v: RegularEmitter =>
          writeEmitterFields(v, out)
          // Preserve LibGDX typo for compatibility
          out.writeKey("continous")
          out.writeVal(v.continuous)
          out.writeKey("emission")
          writeScaledNumericValue(v.emissionValue, out)
          out.writeKey("delay")
          writeRangedNumericValue(v.delayValue, out)
          out.writeKey("duration")
          writeRangedNumericValue(v.durationValue, out)
          out.writeKey("life")
          writeScaledNumericValue(v.lifeValue, out)
          out.writeKey("lifeOffset")
          writeScaledNumericValue(v.lifeOffsetValue, out)
        case _ =>
          writeEmitterFields(x, out)
      }
      out.writeObjectEnd()
    }

    override def nullValue: Emitter = null
  }

  // ── Polymorphic Influencer codec ────────────────────────────────────

  // Map of class names to Influencer decoders
  private val influencerTypes: Map[String, Array[Byte] => Influencer] = Map(
    // ColorInfluencer
    classOf[ColorInfluencer.Single].getName -> (bytes => readFromArray[ColorInfluencer.Single](bytes)),
    classOf[ColorInfluencer.Random].getName -> (bytes => readFromArray[ColorInfluencer.Random](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.ColorInfluencer$Single" -> (bytes => readFromArray[ColorInfluencer.Single](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.ColorInfluencer$Random" -> (bytes => readFromArray[ColorInfluencer.Random](bytes)),
    // DynamicsInfluencer
    classOf[DynamicsInfluencer].getName -> (bytes => readFromArray[DynamicsInfluencer](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.DynamicsInfluencer" -> (bytes => readFromArray[DynamicsInfluencer](bytes)),
    // RegionInfluencer
    classOf[RegionInfluencer.Single].getName -> (bytes => readFromArray[RegionInfluencer.Single](bytes)),
    classOf[RegionInfluencer.Random].getName -> (bytes => readFromArray[RegionInfluencer.Random](bytes)),
    classOf[RegionInfluencer.Animated].getName -> (bytes => readFromArray[RegionInfluencer.Animated](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer$Single" -> (bytes => readFromArray[RegionInfluencer.Single](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer$Random" -> (bytes => readFromArray[RegionInfluencer.Random](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.RegionInfluencer$Animated" -> (bytes => readFromArray[RegionInfluencer.Animated](bytes)),
    // ScaleInfluencer
    classOf[ScaleInfluencer].getName -> (bytes => readFromArray[ScaleInfluencer](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.ScaleInfluencer" -> (bytes => readFromArray[ScaleInfluencer](bytes)),
    // SpawnInfluencer
    classOf[SpawnInfluencer].getName -> (bytes => readFromArray[SpawnInfluencer](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.SpawnInfluencer" -> (bytes => readFromArray[SpawnInfluencer](bytes)),
    // ModelInfluencer
    classOf[ModelInfluencer.Single].getName -> (bytes => readFromArray[ModelInfluencer.Single](bytes)),
    classOf[ModelInfluencer.Random].getName -> (bytes => readFromArray[ModelInfluencer.Random](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.ModelInfluencer$Single" -> (bytes => readFromArray[ModelInfluencer.Single](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.ModelInfluencer$Random" -> (bytes => readFromArray[ModelInfluencer.Random](bytes)),
    // ParticleControllerInfluencer
    classOf[ParticleControllerInfluencer.Single].getName -> (bytes => readFromArray[ParticleControllerInfluencer.Single](bytes)),
    classOf[ParticleControllerInfluencer.Random].getName -> (bytes => readFromArray[ParticleControllerInfluencer.Random](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerInfluencer$Single" -> (bytes => readFromArray[ParticleControllerInfluencer.Single](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerInfluencer$Random" -> (bytes => readFromArray[ParticleControllerInfluencer.Random](bytes)),
    // ParticleControllerFinalizerInfluencer
    classOf[ParticleControllerFinalizerInfluencer].getName -> (bytes => readFromArray[ParticleControllerFinalizerInfluencer](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.influencers.ParticleControllerFinalizerInfluencer" -> (bytes => readFromArray[ParticleControllerFinalizerInfluencer](bytes))
  )

  /** Decodes a polymorphic Influencer from a JSON object with "class" field. */
  private def decodeInfluencer(in: JsonReader): Influencer = {
    val (classNameOpt, bytes) = readPolymorphicObject(in)
    classNameOpt.fold {
      throw new UnsupportedOperationException("Influencer object missing 'class' field")
    } { className =>
      influencerTypes.get(className) match {
        case Some(decoder) => decoder(bytes)
        case None =>
          throw new UnsupportedOperationException(s"Unknown Influencer type: $className")
      }
    }
  }

  given influencerCodec: JsonValueCodec[Influencer] = new JsonValueCodec[Influencer] {
    override def decodeValue(in: JsonReader, default: Influencer): Influencer =
      decodeInfluencer(in)

    override def encodeValue(x: Influencer, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeKey("class")
      out.writeVal(x.getClass.getName)
      x match {
        case v: ColorInfluencer.Single =>
          out.writeKey("alpha")
          writeScaledNumericValue(v.alphaValue, out)
          out.writeKey("color")
          gradientColorValueCodec.encodeValue(v.colorValue, out)
        case _: ColorInfluencer.Random =>
          // No fields
          ()
        case v: DynamicsInfluencer =>
          out.writeKey("velocities")
          out.writeArrayStart()
          for (i <- 0 until v.velocities.size) {
            val modifier = v.velocities(i)
            out.writeObjectStart()
            out.writeKey("class")
            out.writeVal(modifier.getClass.getName)
            modifier match {
              case m: DynamicsModifier.FaceDirection =>
                writeDynamicsModifierFields(m, out)
              case m: DynamicsModifier.Rotational2D =>
                writeStrengthFields(m, out)
              case m: DynamicsModifier.Rotational3D =>
                writeAngularFields(m, out)
              case m: DynamicsModifier.CentripetalAcceleration =>
                writeStrengthFields(m, out)
              case m: DynamicsModifier.PolarAcceleration =>
                writeAngularFields(m, out)
              case m: DynamicsModifier.TangentialAcceleration =>
                writeAngularFields(m, out)
              case m: DynamicsModifier.BrownianAcceleration =>
                writeStrengthFields(m, out)
              case _ => ()
            }
            out.writeObjectEnd()
          }
          out.writeArrayEnd()
        case v: RegionInfluencer =>
          writeRegionInfluencerFields(v, out)
        case v: ScaleInfluencer =>
          writeSimpleInfluencerFields(v, out)
        case v: SpawnInfluencer =>
          out.writeKey("spawnShape")
          out.writeObjectStart()
          out.writeKey("class")
          out.writeVal(v.spawnShapeValue.getClass.getName)
          v.spawnShapeValue match {
            case s: EllipseSpawnShapeValue =>
              writePrimitiveSpawnShapeValueFields(s, out)
              out.writeKey("side")
              out.writeVal(s.side.toString)
            case s: CylinderSpawnShapeValue =>
              writePrimitiveSpawnShapeValueFields(s, out)
            case s: LineSpawnShapeValue =>
              writePrimitiveSpawnShapeValueFields(s, out)
            case s: RectangleSpawnShapeValue =>
              writePrimitiveSpawnShapeValueFields(s, out)
            case s: PointSpawnShapeValue =>
              writeSpawnShapeValueFields(s, out)
            // MeshSpawnShapeValue types use save/load for mesh data (ISS-467)
            case s: UnweightedMeshSpawnShapeValue =>
              writeSpawnShapeValueFields(s, out)
            case s: WeightMeshSpawnShapeValue =>
              writeSpawnShapeValueFields(s, out)
            case s =>
              writeSpawnShapeValueFields(s, out)
          }
          out.writeObjectEnd()
        case _: ModelInfluencer =>
          // ModelInfluencer uses save/load (ResourceData), not write/read
          ()
        case _: ParticleControllerInfluencer =>
          // ParticleControllerInfluencer uses save/load (ResourceData), not write/read
          ()
        case _: ParticleControllerFinalizerInfluencer =>
          // No fields
          ()
        case _ =>
          ()
      }
      out.writeObjectEnd()
    }

    override def nullValue: Influencer = null
  }

  // ── Additional Influencer codecs for types without explicit serialization ──

  // ModelInfluencer codecs serialize model asset filenames as a JSON array.
  // The original Java uses ResourceData save/load with AssetManager; for JSON serialization
  // we store the model filenames that the models array references, to be resolved at load time.
  // Note: models cannot be fully deserialized without an AssetManager, so the modelFilenames
  // field is used as a transport mechanism - actual model loading requires post-processing.

  given modelInfluencerSingleCodec: JsonValueCodec[ModelInfluencer.Single] = new JsonValueCodec[ModelInfluencer.Single] {
    override def decodeValue(in: JsonReader, default: ModelInfluencer.Single): ModelInfluencer.Single = {
      val value = if (default != null) default else new ModelInfluencer.Single()
      // Store filenames for later resolution via AssetManager
      var modelFilenames: Array[String] = Array.empty
      readFields(in, value) { (fieldName, _, reader) =>
        fieldName match {
          case "modelFilenames" =>
            modelFilenames = readStringArray(reader)
            true
          case _ =>
            reader.skip()
            true
        }
      }
      // The modelFilenames are stored but cannot be resolved without AssetManager
      // They should be attached to the influencer for later resolution
      value._modelFilenames = modelFilenames
      value
    }

    override def encodeValue(x: ModelInfluencer.Single, out: JsonWriter): Unit = {
      out.writeObjectStart()
      // Write model filenames - since we don't have access to AssetManager here,
      // we rely on the modelFilenames being pre-populated before serialization
      // (typically via ParticleEffectLoader which has AssetManager access)
      if (x._modelFilenames != null && x._modelFilenames.nonEmpty) {
        out.writeKey("modelFilenames")
        writeStringArray(x._modelFilenames, out)
      }
      out.writeObjectEnd()
    }

    override def nullValue: ModelInfluencer.Single = null
  }

  given modelInfluencerRandomCodec: JsonValueCodec[ModelInfluencer.Random] = new JsonValueCodec[ModelInfluencer.Random] {
    override def decodeValue(in: JsonReader, default: ModelInfluencer.Random): ModelInfluencer.Random = {
      val value = if (default != null) default else new ModelInfluencer.Random()
      var modelFilenames: Array[String] = Array.empty
      readFields(in, value) { (fieldName, _, reader) =>
        fieldName match {
          case "modelFilenames" =>
            modelFilenames = readStringArray(reader)
            true
          case _ =>
            reader.skip()
            true
        }
      }
      value._modelFilenames = modelFilenames
      value
    }

    override def encodeValue(x: ModelInfluencer.Random, out: JsonWriter): Unit = {
      out.writeObjectStart()
      if (x._modelFilenames != null && x._modelFilenames.nonEmpty) {
        out.writeKey("modelFilenames")
        writeStringArray(x._modelFilenames, out)
      }
      out.writeObjectEnd()
    }

    override def nullValue: ModelInfluencer.Random = null
  }

  // ParticleControllerInfluencer codecs serialize effect references with controller indices.
  // The original Java save() builds a mapping from effect filenames to int[] of controller indices,
  // then stores both the effect assets and the indices. For JSON serialization we store this as:
  // { "effectReferences": [ { "effectFilename": "...", "controllerIndices": [...] }, ... ] }
  // At load time, these references are used to resolve templates from loaded ParticleEffects.

  /** Helper to read an array of EffectReference objects (effect filename + controller indices). */
  private def readEffectReferences(in: JsonReader): Array[EffectReference] = {
    val buf = ArrayBuffer[EffectReference]()
    if (in.isNextToken('[')) {
      if (!in.isNextToken(']')) {
        in.rollbackToken()
        buf += readEffectReference(in)
        while (in.isNextToken(',')) buf += readEffectReference(in)
      }
    }
    buf.toArray
  }

  /** Helper to read a single EffectReference object. */
  private def readEffectReference(in: JsonReader): EffectReference = {
    var effectFilename: String = ""
    var controllerIndices: Array[Int] = Array.empty
    if (in.isNextToken('{')) {
      if (!in.isNextToken('}')) {
        in.rollbackToken()
        var continue = true
        while (continue) {
          val key = in.readKeyAsString()
          key match {
            case "effectFilename" =>
              effectFilename = in.readString(null)
            case "controllerIndices" =>
              controllerIndices = readIntArray(in)
            case _ =>
              in.skip()
          }
          continue = in.isNextToken(',')
        }
      }
    }
    EffectReference(effectFilename, controllerIndices)
  }

  /** Helper to write an array of EffectReference objects. */
  private def writeEffectReferences(refs: Array[EffectReference], out: JsonWriter): Unit = {
    out.writeArrayStart()
    var i = 0
    while (i < refs.length) {
      writeEffectReference(refs(i), out)
      i += 1
    }
    out.writeArrayEnd()
  }

  /** Helper to write a single EffectReference object. */
  private def writeEffectReference(ref: EffectReference, out: JsonWriter): Unit = {
    out.writeObjectStart()
    out.writeKey("effectFilename")
    out.writeVal(ref.effectFilename)
    out.writeKey("controllerIndices")
    writeIntArray(ref.controllerIndices, out)
    out.writeObjectEnd()
  }

  given particleControllerInfluencerSingleCodec: JsonValueCodec[ParticleControllerInfluencer.Single] =
    new JsonValueCodec[ParticleControllerInfluencer.Single] {
      override def decodeValue(in: JsonReader, default: ParticleControllerInfluencer.Single): ParticleControllerInfluencer.Single = {
        val value = if (default != null) default else new ParticleControllerInfluencer.Single()
        var effectReferences: Array[EffectReference] = Array.empty
        readFields(in, value) { (fieldName, _, reader) =>
          fieldName match {
            case "effectReferences" =>
              effectReferences = readEffectReferences(reader)
              true
            case _ =>
              reader.skip()
              true
          }
        }
        value._effectReferences = effectReferences
        value
      }

      override def encodeValue(x: ParticleControllerInfluencer.Single, out: JsonWriter): Unit = {
        out.writeObjectStart()
        if (x._effectReferences != null && x._effectReferences.nonEmpty) {
          out.writeKey("effectReferences")
          writeEffectReferences(x._effectReferences, out)
        }
        out.writeObjectEnd()
      }

      override def nullValue: ParticleControllerInfluencer.Single = null
    }

  given particleControllerInfluencerRandomCodec: JsonValueCodec[ParticleControllerInfluencer.Random] =
    new JsonValueCodec[ParticleControllerInfluencer.Random] {
      override def decodeValue(in: JsonReader, default: ParticleControllerInfluencer.Random): ParticleControllerInfluencer.Random = {
        val value = if (default != null) default else new ParticleControllerInfluencer.Random()
        var effectReferences: Array[EffectReference] = Array.empty
        readFields(in, value) { (fieldName, _, reader) =>
          fieldName match {
            case "effectReferences" =>
              effectReferences = readEffectReferences(reader)
              true
            case _ =>
              reader.skip()
              true
          }
        }
        value._effectReferences = effectReferences
        value
      }

      override def encodeValue(x: ParticleControllerInfluencer.Random, out: JsonWriter): Unit = {
        out.writeObjectStart()
        if (x._effectReferences != null && x._effectReferences.nonEmpty) {
          out.writeKey("effectReferences")
          writeEffectReferences(x._effectReferences, out)
        }
        out.writeObjectEnd()
      }

      override def nullValue: ParticleControllerInfluencer.Random = null
    }

  given particleControllerFinalizerInfluencerCodec: JsonValueCodec[ParticleControllerFinalizerInfluencer] =
    new JsonValueCodec[ParticleControllerFinalizerInfluencer] {
      override def decodeValue(in: JsonReader, default: ParticleControllerFinalizerInfluencer): ParticleControllerFinalizerInfluencer = {
        val value = if (default != null) default else new ParticleControllerFinalizerInfluencer()
        readFields(in, value) { (_, _, reader) =>
          reader.skip()
          true
        }
      }

      override def encodeValue(x: ParticleControllerFinalizerInfluencer, out: JsonWriter): Unit = {
        out.writeObjectStart()
        out.writeObjectEnd()
      }

      override def nullValue: ParticleControllerFinalizerInfluencer = null
    }

  // ── Polymorphic ParticleControllerRenderer codec ────────────────────

  // Map of class names to renderer decoders
  private val rendererTypes: Map[String, Array[Byte] => ParticleControllerRenderer[?, ?]] = Map(
    classOf[BillboardRenderer].getName -> (bytes => readFromArray[BillboardRenderer](bytes)),
    classOf[PointSpriteRenderer].getName -> (bytes => readFromArray[PointSpriteRenderer](bytes)),
    classOf[ModelInstanceRenderer].getName -> (bytes => readFromArray[ModelInstanceRenderer](bytes)),
    classOf[ParticleControllerControllerRenderer].getName -> (bytes => readFromArray[ParticleControllerControllerRenderer](bytes)),
    // LibGDX class names for compatibility
    "com.badlogic.gdx.graphics.g3d.particles.renderers.BillboardRenderer" -> (bytes => readFromArray[BillboardRenderer](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.renderers.PointSpriteRenderer" -> (bytes => readFromArray[PointSpriteRenderer](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.renderers.ModelInstanceRenderer" -> (bytes => readFromArray[ModelInstanceRenderer](bytes)),
    "com.badlogic.gdx.graphics.g3d.particles.renderers.ParticleControllerControllerRenderer" -> (bytes => readFromArray[ParticleControllerControllerRenderer](bytes))
  )

  private def decodeRenderer(in: JsonReader): ParticleControllerRenderer[?, ?] = {
    val (classNameOpt, bytes) = readPolymorphicObject(in)
    classNameOpt.fold {
      throw new UnsupportedOperationException("Renderer object missing 'class' field")
    } { className =>
      rendererTypes.get(className) match {
        case Some(decoder) => decoder(bytes)
        case None =>
          throw new UnsupportedOperationException(s"Unknown renderer type: $className")
      }
    }
  }

  // Renderer codecs - these have no serializable fields
  given billboardRendererCodec: JsonValueCodec[BillboardRenderer] = new JsonValueCodec[BillboardRenderer] {
    override def decodeValue(in: JsonReader, default: BillboardRenderer): BillboardRenderer = {
      val value = if (default != null) default else new BillboardRenderer()
      readFields(in, value) { (_, _, reader) =>
        reader.skip()
        true
      }
    }

    override def encodeValue(x: BillboardRenderer, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeObjectEnd()
    }

    override def nullValue: BillboardRenderer = null
  }

  given pointSpriteRendererCodec: JsonValueCodec[PointSpriteRenderer] = new JsonValueCodec[PointSpriteRenderer] {
    override def decodeValue(in: JsonReader, default: PointSpriteRenderer): PointSpriteRenderer = {
      val value = if (default != null) default else new PointSpriteRenderer()
      readFields(in, value) { (_, _, reader) =>
        reader.skip()
        true
      }
    }

    override def encodeValue(x: PointSpriteRenderer, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeObjectEnd()
    }

    override def nullValue: PointSpriteRenderer = null
  }

  given modelInstanceRendererCodec: JsonValueCodec[ModelInstanceRenderer] = new JsonValueCodec[ModelInstanceRenderer] {
    override def decodeValue(in: JsonReader, default: ModelInstanceRenderer): ModelInstanceRenderer = {
      val value = if (default != null) default else new ModelInstanceRenderer()
      readFields(in, value) { (_, _, reader) =>
        reader.skip()
        true
      }
    }

    override def encodeValue(x: ModelInstanceRenderer, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeObjectEnd()
    }

    override def nullValue: ModelInstanceRenderer = null
  }

  given particleControllerControllerRendererCodec: JsonValueCodec[ParticleControllerControllerRenderer] =
    new JsonValueCodec[ParticleControllerControllerRenderer] {
      override def decodeValue(in: JsonReader, default: ParticleControllerControllerRenderer): ParticleControllerControllerRenderer = {
        val value = if (default != null) default else new ParticleControllerControllerRenderer()
        readFields(in, value) { (_, _, reader) =>
          reader.skip()
          true
        }
      }

      override def encodeValue(x: ParticleControllerControllerRenderer, out: JsonWriter): Unit = {
        out.writeObjectStart()
        out.writeObjectEnd()
      }

      override def nullValue: ParticleControllerControllerRenderer = null
    }

  given rendererCodec: JsonValueCodec[ParticleControllerRenderer[?, ?]] = new JsonValueCodec[ParticleControllerRenderer[?, ?]] {
    override def decodeValue(in: JsonReader, default: ParticleControllerRenderer[?, ?]): ParticleControllerRenderer[?, ?] =
      decodeRenderer(in)

    override def encodeValue(x: ParticleControllerRenderer[?, ?], out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeKey("class")
      out.writeVal(x.getClass.getName)
      // Renderers have no serializable fields
      out.writeObjectEnd()
    }

    override def nullValue: ParticleControllerRenderer[?, ?] = null
  }

  // ── ParticleController codec (ISS-465) ──────────────────────────────
  //
  // NOTE: ParticleController requires `(using Sge)` context parameter, which cannot be
  // provided during JSON deserialization. Decoding requires a pre-constructed default
  // instance to be passed (the Sge context will already be bound to it).
  //
  // For creating new ParticleController instances from JSON, use ResourceData.fromJson
  // which handles the full particle effect loading pipeline including asset dependencies.

  given particleControllerCodec: JsonValueCodec[ParticleController] = new JsonValueCodec[ParticleController] {
    override def decodeValue(in: JsonReader, default: ParticleController): ParticleController = {
      if (default == null) {
        throw new UnsupportedOperationException(
          "ParticleController cannot be decoded without a pre-constructed default instance (requires Sge context). " +
          "Use ResourceData.fromJson for full particle effect loading."
        )
      }
      readFields(in, default) { (key, v, reader) =>
        key match {
          case "name" =>
            v.name = readString(reader)
            true
          case "emitter" =>
            v.emitter = decodeEmitter(reader)
            true
          case "influencers" =>
            // Array of polymorphic Influencer objects
            v.influencers.clear()
            if (reader.isNextToken('[')) {
              if (!reader.isNextToken(']')) {
                reader.rollbackToken()
                var continue = true
                while (continue) {
                  val influencer = decodeInfluencer(reader)
                  v.influencers.add(influencer)
                  continue = reader.isNextToken(',')
                }
              }
            }
            true
          case "renderer" =>
            v.renderer = decodeRenderer(reader)
            true
          case _ => false
        }
      }
    }

    override def encodeValue(x: ParticleController, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeKey("name")
      out.writeVal(x.name)
      out.writeKey("emitter")
      emitterCodec.encodeValue(x.emitter, out)
      out.writeKey("influencers")
      out.writeArrayStart()
      for (i <- 0 until x.influencers.size) {
        influencerCodec.encodeValue(x.influencers(i), out)
      }
      out.writeArrayEnd()
      out.writeKey("renderer")
      rendererCodec.encodeValue(x.renderer, out)
      out.writeObjectEnd()
    }

    override def nullValue: ParticleController = null
  }

  // ── ResourceData codecs (ISS-466) ───────────────────────────────────
  //
  // NOTE: ResourceData already has proper serialization via its own toJson/fromJson
  // methods in ResourceData.scala which use the internal sge.utils.Json AST.
  // The jsoniter-scala codecs here are provided for interoperability but may not
  // handle all edge cases. For full particle effect loading, use ResourceData.fromJson.

  // ResourceData.AssetData codec
  given assetDataCodec: JsonValueCodec[ResourceData.AssetData[?]] = new JsonValueCodec[ResourceData.AssetData[?]] {
    override def decodeValue(in: JsonReader, default: ResourceData.AssetData[?]): ResourceData.AssetData[?] = {
      // Read into temporary vars since AssetData requires constructor params
      var filename: String   = ""
      var typeName: String   = ""

      readFields(in, ()) { (key, _, reader) =>
        key match {
          case "filename" =>
            filename = readString(reader)
            true
          case "type" =>
            typeName = readString(reader)
            true
          case _ => false
        }
      }

      // Resolve the class
      val clazz: Class[?] = ResourceData.resolveClassName(typeName)
      new ResourceData.AssetData(filename, clazz)
    }

    override def encodeValue(x: ResourceData.AssetData[?], out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeKey("filename")
      out.writeVal(x.filename)
      out.writeKey("type")
      out.writeVal(x.`type`.getName)
      out.writeObjectEnd()
    }

    override def nullValue: ResourceData.AssetData[?] = null
  }

  // ResourceData.SaveData codec
  given saveDataCodec: JsonValueCodec[ResourceData.SaveData] = new JsonValueCodec[ResourceData.SaveData] {
    override def decodeValue(in: JsonReader, default: ResourceData.SaveData): ResourceData.SaveData = {
      val value = if (default != null) default else new ResourceData.SaveData()
      readFields(in, value) { (key, v, reader) =>
        key match {
          case "data" =>
            // ObjectMap<String, Object> - we need to read it generically
            if (reader.isNextToken('{')) {
              if (!reader.isNextToken('}')) {
                reader.rollbackToken()
                var continue = true
                while (continue) {
                  val mapKey = reader.readKeyAsString()
                  // Read the value - we'll store it as raw JSON bytes for later processing
                  val valueBuf = ArrayBuffer[Byte]()
                  bufferJsonValue(reader, valueBuf)
                  // For now, we can re-parse simple values
                  val valueBytes = valueBuf.toArray
                  val valueStr = new String(valueBytes, "UTF-8")
                  // Try to parse as common types
                  val obj: AnyRef = valueStr match {
                    case s if s.startsWith("\"") && s.endsWith("\"") =>
                      s.substring(1, s.length - 1)
                    case "true"  => java.lang.Boolean.TRUE
                    case "false" => java.lang.Boolean.FALSE
                    case "null"  => null
                    case _ =>
                      // Try number
                      try {
                        if (valueStr.contains(".")) java.lang.Double.valueOf(valueStr)
                        else java.lang.Long.valueOf(valueStr)
                      } catch {
                        case _: NumberFormatException => valueStr
                      }
                  }
                  v.data.put(mapKey, obj)
                  continue = reader.isNextToken(',')
                }
              }
            }
            true
          case "indices" =>
            val arr = readIntArray(reader)
            for (i <- arr) v.assets.add(i)
            true
          case _ => false
        }
      }
    }

    override def encodeValue(x: ResourceData.SaveData, out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeKey("data")
      out.writeObjectStart()
      x.data.foreachEntry { (key, value) =>
        out.writeKey(key)
        value match {
          case s: String             => out.writeVal(s)
          case b: java.lang.Boolean  => out.writeVal(b.booleanValue())
          case i: java.lang.Integer  => out.writeVal(i.intValue())
          case l: java.lang.Long     => out.writeVal(l.longValue())
          case f: java.lang.Float    => out.writeVal(f.floatValue())
          case d: java.lang.Double   => out.writeVal(d.doubleValue())
          case null                  => out.writeNull()
          case other                 => out.writeVal(other.toString)
        }
      }
      out.writeObjectEnd()
      out.writeKey("indices")
      writeIntArray(x.assets.toArray, out)
      out.writeObjectEnd()
    }

    override def nullValue: ResourceData.SaveData = null
  }

  // ResourceData codec
  given resourceDataCodec: JsonValueCodec[ResourceData[?]] = new JsonValueCodec[ResourceData[?]] {
    override def decodeValue(in: JsonReader, default: ResourceData[?]): ResourceData[?] = {
      val value = if (default != null) default else new ResourceData[Any]()
      readFields(in, value.asInstanceOf[ResourceData[Any]]) { (key, v, reader) =>
        key match {
          case "unique" =>
            // ObjectMap<String, SaveData>
            if (reader.isNextToken('{')) {
              if (!reader.isNextToken('}')) {
                reader.rollbackToken()
                var continue = true
                while (continue) {
                  val mapKey = reader.readKeyAsString()
                  val saveData = saveDataCodec.decodeValue(reader, null)
                  saveData.resources = Nullable(v)
                  v.uniqueData.put(mapKey, saveData)
                  continue = reader.isNextToken(',')
                }
              }
            }
            true
          case "data" =>
            // Array<SaveData>
            if (reader.isNextToken('[')) {
              if (!reader.isNextToken(']')) {
                reader.rollbackToken()
                var continue = true
                while (continue) {
                  val saveData = saveDataCodec.decodeValue(reader, null)
                  saveData.resources = Nullable(v)
                  v.data.add(saveData)
                  continue = reader.isNextToken(',')
                }
              }
            }
            true
          case "assets" =>
            // Array<AssetData>
            if (reader.isNextToken('[')) {
              if (!reader.isNextToken(']')) {
                reader.rollbackToken()
                var continue = true
                while (continue) {
                  val assetData = assetDataCodec.decodeValue(reader, null)
                  v.sharedAssets.add(assetData.asInstanceOf[ResourceData.AssetData[Any]])
                  continue = reader.isNextToken(',')
                }
              }
            }
            true
          case "resource" =>
            // Resource is polymorphic - read and store it
            // Check for null first
            val b = reader.nextToken()
            reader.rollbackToken()
            if (b == 'n') {
              reader.readNullOrError(null, "expected null")
              // resource remains empty (Nullable.empty is the default)
            } else {
              // Read as polymorphic object with class field
              val (classNameOpt, _) = readPolymorphicObject(reader)
              classNameOpt.foreach { className =>
                // For ParticleEffect, we need Sge context - store the buffered bytes
                // The actual instantiation happens through ResourceData.fromJson pipeline
                // which provides proper context. Here we just try to restore what we can.
                if (className.endsWith("ParticleEffect") || className == classOf[ParticleEffect].getName) {
                  // ParticleEffect requires Sge context - cannot instantiate here
                  // The fromJson pipeline handles this properly. For codec interop,
                  // we leave resource empty and expect caller to use fromJson.
                } else {
                  // For other types, try reflection-based instantiation
                  try {
                    val clazz = Class.forName(className)
                    val instance = clazz.getDeclaredConstructor().newInstance()
                    v.resource = Nullable(instance.asInstanceOf[Any])
                  } catch {
                    case _: Exception => // Cannot instantiate, leave empty
                  }
                }
              }
            }
            true
          case _ => false
        }
      }
    }

    override def encodeValue(x: ResourceData[?], out: JsonWriter): Unit = {
      out.writeObjectStart()
      out.writeKey("unique")
      out.writeObjectStart()
      x.uniqueData.foreachEntry { (key, saveData) =>
        out.writeKey(key)
        saveDataCodec.encodeValue(saveData, out)
      }
      out.writeObjectEnd()
      out.writeKey("data")
      out.writeArrayStart()
      for (i <- 0 until x.data.size) {
        saveDataCodec.encodeValue(x.data(i), out)
      }
      out.writeArrayEnd()
      out.writeKey("assets")
      out.writeArrayStart()
      for (i <- 0 until x.sharedAssets.size) {
        assetDataCodec.encodeValue(x.sharedAssets(i), out)
      }
      out.writeArrayEnd()
      out.writeKey("resource")
      // Resource is polymorphic - write with class tag like libGDX Json does
      x.resource match {
        case res if Nullable.isEmpty(res) =>
          out.writeNull()
        case _ =>
          val resource = Nullable.get(x.resource)
          out.writeObjectStart()
          out.writeKey("class")
          out.writeVal(resource.getClass.getName)
          // Write type-specific fields
          resource match {
            case effect: ParticleEffect =>
              out.writeKey("controllers")
              out.writeArrayStart()
              for (i <- 0 until effect.controllers.size) {
                particleControllerCodec.encodeValue(effect.controllers(i), out)
              }
              out.writeArrayEnd()
            case _ =>
              // For unknown types, we can't serialize fields without reflection
              // libGDX uses reflection here; we write empty object
              ()
          }
          out.writeObjectEnd()
      }
      out.writeObjectEnd()
    }

    override def nullValue: ResourceData[?] = null
  }
}
