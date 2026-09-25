package sge.port

import balticporter.tir.RuleScope
import balticporter.transform.CallSiteSubstitutionTransform.Entry

/** sge's `SgeError` in place of libGDX's `GdxRuntimeException` and `SerializationException`: every constructor call becomes an `SgeError` variant carrying java's message and cause. The variant
  * is sge's API (its tests and callers match on it), so a member where sge's hand port chose a variant gets that one; everywhere else a `GdxRuntimeException` is `InvalidInput` and a
  * `SerializationException` `SerializationError`. A member whose hand port threw a JDK exception instead keeps java's type, mapped to the default.
  */
object LibgdxExceptions {

  private val Gdx = "com.badlogic.gdx.utils.GdxRuntimeException"
  private val Ser = "com.badlogic.gdx.utils.SerializationException"

  /** the template building `variant` from one constructor overload: `RuntimeException(Throwable)` takes the cause's `toString` as its message, `SerializationException(Throwable)` the empty string.
    */
  private def template(exception: String, overload: String, variant: String): String = {
    val v = s"sge.utils.SgeError.$variant"
    overload match {
      case "(String)"           => s"$v({arg0})"
      case "(String,Throwable)" => s"$v({arg0}, scala.Option({arg1}))"
      case "(Throwable)" if exception == Ser => s"""$v("", scala.Option({arg0}))"""
      case "(Throwable)"        => s"{{ val bpCause: java.lang.Throwable = {arg0}; $v(if (bpCause == null) null else bpCause.toString, scala.Option(bpCause)) }}"
      case other                => sys.error(s"no SgeError template for the constructor overload $other")
    }
  }

  private val Overloads = List("(String)", "(Throwable)", "(String,Throwable)")

  /** the unscoped defaults, one per constructor overload core calls. */
  val Defaults: List[Entry] =
    Overloads.map(o => Entry(s"$Gdx#<init>$o", template(Gdx, o, "InvalidInput"))) ++
      Overloads.map(o => Entry(s"$Ser#<init>$o", template(Ser, o, "SerializationError")))

  /** Per variant, the `GdxRuntimeException` constructions sge's hand port gave that variant: the enclosing member and the constructor overload called there. `JsonReader` is not here although the
    * hand port threw `InvalidInput`: java's tiled-map loaders catch its `SerializationException`, and a different variant would escape that catch.
    */
  val HandPortVariants: Map[String, List[(String, String)]] = Map(
    "GraphicsError" -> List(
      "com.badlogic.gdx.assets.loaders.BitmapFontLoader#loadSync" -> "(String)",
      "com.badlogic.gdx.graphics.Cubemap#reload" -> "(String)",
      "com.badlogic.gdx.graphics.Mesh#calculateBoundingBox" -> "(String)",
      "com.badlogic.gdx.graphics.Mesh#calculateRadiusSquared" -> "(String)",
      "com.badlogic.gdx.graphics.Mesh#enableInstancedRendering" -> "(String)",
      "com.badlogic.gdx.graphics.Mesh#extendBoundingBox" -> "(String)",
      "com.badlogic.gdx.graphics.Mesh#render" -> "(String)",
      "com.badlogic.gdx.graphics.Mesh#setInstanceData" -> "(String)",
      "com.badlogic.gdx.graphics.PixmapIO#writePNG" -> "(String,Throwable)",
      "com.badlogic.gdx.graphics.PixmapIO$CIM#read" -> "(String,Throwable)",
      "com.badlogic.gdx.graphics.PixmapIO$CIM#write" -> "(String,Throwable)",
      "com.badlogic.gdx.graphics.Texture#draw" -> "(String)",
      "com.badlogic.gdx.graphics.Texture#load" -> "(String)",
      "com.badlogic.gdx.graphics.Texture#reload" -> "(String)",
      "com.badlogic.gdx.graphics.Texture3D#<init>" -> "(String)",
      "com.badlogic.gdx.graphics.Texture3D#load" -> "(String)",
      "com.badlogic.gdx.graphics.Texture3D#reload" -> "(String)",
      "com.badlogic.gdx.graphics.TextureArray#<init>" -> "(String)",
      "com.badlogic.gdx.graphics.TextureArray#load" -> "(String)",
      "com.badlogic.gdx.graphics.TextureArray#reload" -> "(String)",
      "com.badlogic.gdx.graphics.g2d.CpuSpriteBatch#draw" -> "(String)",
      "com.badlogic.gdx.graphics.g2d.CpuSpriteBatch#flushAndSyncTransformMatrix" -> "(String)",
      "com.badlogic.gdx.graphics.g2d.ParticleEffect#loadEmitters" -> "(String,Throwable)",
      "com.badlogic.gdx.graphics.g2d.SpriteCache#endCache" -> "(String)",
      "com.badlogic.gdx.graphics.g2d.TextureAtlas$TextureAtlasData#load" -> "(String,Throwable)",
      "com.badlogic.gdx.graphics.g3d.Environment#add" -> "(String)",
      "com.badlogic.gdx.graphics.g3d.particles.ParticleShader#bindMaterial" -> "(String)",
      "com.badlogic.gdx.graphics.g3d.shaders.BaseShader#init" -> "(String)",
      "com.badlogic.gdx.graphics.g3d.shaders.BaseShader#register" -> "(String)",
      "com.badlogic.gdx.graphics.g3d.shaders.DefaultShader#bindMaterial" -> "(String)",
      "com.badlogic.gdx.graphics.g3d.utils.BaseShaderProvider#getShader" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.ETC1TextureData#consumeCustomData" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.ETC1TextureData#consumePixmap" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.ETC1TextureData#disposePixmap" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.ETC1TextureData#prepare" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.FacedCubemapData#prepare" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.FileTextureArrayData#prepare" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.FloatFrameBuffer#checkExtensions" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.FloatTextureData#consumeCustomData" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.FrameBufferCubemap#nextSide" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.GLFrameBuffer#checkValidBuilder" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.GLOnlyTextureData#consumePixmap" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.GLOnlyTextureData#disposePixmap" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.GLOnlyTextureData#prepare" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20#createDefaultShader" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.IndexBufferObject#bind" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.IndexBufferObjectSubData#bind" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.InstanceBufferObject#<init>" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.InstanceBufferObject#setBuffer" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.InstanceBufferObjectSubData#updateInstanceData" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.KTXTextureData#consumeCustomData" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.KTXTextureData#consumePixmap" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.KTXTextureData#disposePixmap" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.KTXTextureData#getFormat" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.KTXTextureData#prepare" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.KTXTextureData#prepare" -> "(String,Throwable)",
      "com.badlogic.gdx.graphics.glutils.MipMapGenerator#generateMipMapCPU" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.MipMapTextureData#consumePixmap" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.PixmapTextureData#consumeCustomData" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.PixmapTextureData#prepare" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.VertexBufferObject#setBuffer" -> "(String)",
      "com.badlogic.gdx.graphics.glutils.VertexBufferObjectSubData#updateVertices" -> "(String)",
      "com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile#getCurrentFrameIndex" -> "(String)",
      "com.badlogic.gdx.scenes.scene2d.ui.SplitPane#setMaxSplitAmount" -> "(String)",
      "com.badlogic.gdx.scenes.scene2d.ui.SplitPane#setMinSplitAmount" -> "(String)"
    ),
    "MathError" -> List(
      "com.badlogic.gdx.Version#<clinit>" -> "(String,Throwable)",
      "com.badlogic.gdx.math.Affine2#inv" -> "(String)",
      "com.badlogic.gdx.math.Bezier#set" -> "(String)",
      "com.badlogic.gdx.math.Rectangle#fromString" -> "(String)",
      "com.badlogic.gdx.math.Vector2#fromString" -> "(String)",
      "com.badlogic.gdx.math.Vector3#fromString" -> "(String)",
      "com.badlogic.gdx.math.Vector4#fromString" -> "(String)"
    ),
    "SerializationError" -> List(
      "com.badlogic.gdx.assets.AssetLoadingTask#handleAsyncLoader" -> "(String,Throwable)"
    )
  )

  /** Per variant, the `FileHandle` members whose `GdxRuntimeException` sge's hand port made a file error naming the handle itself (`FileReadError` also where it writes, as the hand port has it).
    * The loaders that named a file handle they hold in a local keep the default: a template can name the enclosing instance, not a local.
    */
  val HandPortFileVariants: Map[String, List[(String, String)]] = Map(
    "FileReadError" -> List(
      "list" -> "(String)",
      "map" -> "(String)",
      "map" -> "(String,Throwable)",
      "read" -> "(String)",
      "read" -> "(String,Throwable)",
      "readBytes" -> "(String,Throwable)",
      "readString" -> "(String,Throwable)",
      "reader" -> "(String,Throwable)",
      "sibling" -> "(String)",
      "write" -> "(String)",
      "write" -> "(String,Throwable)",
      "writeBytes" -> "(String,Throwable)",
      "writeString" -> "(String,Throwable)",
      "writer" -> "(String)",
      "writer" -> "(String,Throwable)"
    ),
    "FileWriteError" -> List(
      "copyTo" -> "(String)",
      "delete" -> "(String)",
      "deleteDirectory" -> "(String)",
      "emptyDirectory" -> "(String)",
      "mkdirs" -> "(String)",
      "moveTo" -> "(String)"
    )
  )

  private def fileTemplate(overload: String, variant: String): String = {
    val v = s"sge.utils.SgeError.$variant"
    overload match {
      case "(String)"           => s"$v({this}, {arg0})"
      case "(String,Throwable)" => s"$v({this}, {arg0}, scala.Option({arg1}))"
      case other                => sys.error(s"no file-error template for the constructor overload $other")
    }
  }

  /** the hand port's variants, each scoped to its member so it wins over the default there. */
  val Scoped: List[Entry] =
    HandPortVariants.toList.sortBy(_._1).flatMap { (variant, sites) =>
      sites.map((member, overload) => Entry(s"$Gdx#<init>$overload", template(Gdx, overload, variant), RuleScope.Only(Set(member))))
    } ++ HandPortFileVariants.toList.sortBy(_._1).flatMap { (variant, sites) =>
      sites.map((member, overload) =>
        Entry(s"$Gdx#<init>$overload", fileTemplate(overload, variant), RuleScope.Only(Set(s"com.badlogic.gdx.files.FileHandle#$member")))
      )
    }

  private val AssetManager = "com.badlogic.gdx.assets.AssetManager"

  /** sge's `AssetManager.get(fileName)` and `get(descriptor)` answer an empty `Nullable` for an asset that is not loaded (`apply` throws): java's `required = false` lookup. The lookup by name and
    * type is renamed `apply` instead (the renames step), which carries its callers with it.
    */
  val AssetManagerGetBodies: Map[String, String] = Map(
    s"$AssetManager#get(String)" -> "this.get[T](fileName, false)",
    s"$AssetManager#get(AssetDescriptor)" -> "this.get[T](assetDescriptor.fileName, assetDescriptor.`type`, false)"
  )

  /** core's own callers of those two keep java's throw: the `required = true` lookup java's bodies delegated to. The one call by name alone reads a texture, which the template states because a
    * lookup by name alone takes its type from the call site.
    */
  val AssetManagerRequiredCalls: List[Entry] = List(
    Entry(
      s"$AssetManager#get(AssetDescriptor)",
      "{{ val bpManager = {recv}; val bpDescriptor = {arg0}; bpManager.get(bpDescriptor.fileName, bpDescriptor.`type`, true).orNull }}"
    ),
    Entry(
      s"$AssetManager#get(String)",
      "{recv}.get[sge.graphics.Texture]({arg0}, true).orNull",
      RuleScope.Only(Set("com.badlogic.gdx.graphics.g2d.PolygonRegionLoader#load"))
    )
  )

  /** every call-site entry of the exceptions step. */
  val Calls: List[Entry] = Defaults ++ Scoped ++ AssetManagerRequiredCalls
}
