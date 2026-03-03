/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/Cubemap.java
 * Original authors: Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics

import scala.collection.mutable

import sge.assets.AssetManager
// import sge.assets.loaders.CubemapLoader.CubemapParameter // TODO: Create CubemapLoader
import sge.files.FileHandle
import sge.graphics.Pixmap.Format
import sge.graphics.glutils.FacedCubemapData
import sge.math.Vector3
import sge.utils.{ Nullable, SgeError }
import sge.{ Application, Sge }

/** Wraps a standard OpenGL ES Cubemap. Must be disposed when it is no longer used.
  * @author
  *   Xoppa
  */
class Cubemap(protected var data: CubemapData)(using Sge) extends GLTexture(GL20.GL_TEXTURE_CUBE_MAP) {

  load(data)
  if (data.isManaged) Cubemap.addManagedCubemap(Sge().application, this)

  /** Construct a Cubemap with the specified texture files for the sides, does not generate mipmaps. */
  def this(positiveX: FileHandle, negativeX: FileHandle, positiveY: FileHandle, negativeY: FileHandle, positiveZ: FileHandle, negativeZ: FileHandle)(using Sge) =
    this(new FacedCubemapData(positiveX, negativeX, positiveY, negativeY, positiveZ, negativeZ))

  /** Construct a Cubemap with the specified texture files for the sides, optionally generating mipmaps. */
  def this(positiveX: FileHandle, negativeX: FileHandle, positiveY: FileHandle, negativeY: FileHandle, positiveZ: FileHandle, negativeZ: FileHandle, useMipMaps: Boolean)(using Sge) =
    this(new FacedCubemapData(positiveX, negativeX, positiveY, negativeY, positiveZ, negativeZ, useMipMaps))

  /** Construct a Cubemap with the specified {@link Pixmap}s for the sides, does not generate mipmaps. */
  def this(positiveX: Pixmap, negativeX: Pixmap, positiveY: Pixmap, negativeY: Pixmap, positiveZ: Pixmap, negativeZ: Pixmap)(using Sge) =
    this(
      new FacedCubemapData(
        Nullable(positiveX),
        Nullable(negativeX),
        Nullable(positiveY),
        Nullable(negativeY),
        Nullable(positiveZ),
        Nullable(negativeZ),
        false
      )
    )

  /** Construct a Cubemap with the specified {@link Pixmap}s for the sides, optionally generating mipmaps. */
  def this(positiveX: Pixmap, negativeX: Pixmap, positiveY: Pixmap, negativeY: Pixmap, positiveZ: Pixmap, negativeZ: Pixmap, useMipMaps: Boolean)(using Sge) =
    this(
      new FacedCubemapData(
        Nullable(positiveX),
        Nullable(negativeX),
        Nullable(positiveY),
        Nullable(negativeY),
        Nullable(positiveZ),
        Nullable(negativeZ),
        useMipMaps
      )
    )

  /** Construct a Cubemap with {@link Pixmap}s for each side of the specified size. */
  def this(width: Int, height: Int, depth: Int, format: Format)(using Sge) =
    this(new FacedCubemapData(width, height, depth, format))

  /** Construct a Cubemap with the specified {@link TextureData}'s for the sides */
  def this(positiveX: TextureData, negativeX: TextureData, positiveY: TextureData, negativeY: TextureData, positiveZ: TextureData, negativeZ: TextureData)(using Sge) =
    this(new FacedCubemapData(positiveX, negativeX, positiveY, negativeY, positiveZ, negativeZ))

  /** Sets the sides of this cubemap to the specified {@link CubemapData}. */
  def load(data: CubemapData): Unit = {
    if (!data.isPrepared) data.prepare()
    bind()
    unsafeSetFilter(minFilter, magFilter, true)
    unsafeSetWrap(uWrap, vWrap, true)
    unsafeSetAnisotropicFilter(anisotropicFilterLevel, true)
    data.consumeCubemapData()
    Sge().graphics.gl.glBindTexture(glTarget, 0)
  }

  def getCubemapData(): CubemapData = data

  override def isManaged: Boolean = data.isManaged

  override protected def reload(): Unit = {
    if (!isManaged) throw SgeError.GraphicsError("Tried to reload an unmanaged Cubemap")
    glHandle = TextureHandle(Sge().graphics.gl.glGenTexture())
    load(data)
  }

  override def getWidth: Int = data.getWidth

  override def getHeight: Int = data.getHeight

  override def getDepth: Int = 0

  /** Disposes all resources associated with the cubemap */
  override def close(): Unit =
    // this is a hack. reason: we have to set the glHandle to 0 for textures that are
    // reloaded through the asset manager as we first remove (and thus dispose) the texture
    // and then reload it. the glHandle is set to 0 in invalidateAllTextures prior to
    // removal from the asset manager.
    if (glHandle != TextureHandle.none) {
      delete()
      if (data.isManaged) {
        Cubemap.managedCubemaps.get(Sge().application) match {
          case Some(cubemaps) => cubemaps -= this
          case None           => // no cubemaps for this app
        }
      }
    }
}

object Cubemap {
  private var assetManager: Nullable[AssetManager]                         = Nullable.empty
  val managedCubemaps:      mutable.Map[Application, mutable.Set[Cubemap]] = mutable.Map()

  /** Enum to identify each side of a Cubemap */
  enum CubemapSide(val index: Int, val glEnum: Int, upX: Float, upY: Float, upZ: Float, directionX: Float, directionY: Float, directionZ: Float) {

    /** The positive X and first side of the cubemap */
    case PositiveX extends CubemapSide(0, GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, -1, 0, 1, 0, 0)

    /** The negative X and second side of the cubemap */
    case NegativeX extends CubemapSide(1, GL20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, -1, 0, -1, 0, 0)

    /** The positive Y and third side of the cubemap */
    case PositiveY extends CubemapSide(2, GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, 0, 1, 0, 1, 0)

    /** The negative Y and fourth side of the cubemap */
    case NegativeY extends CubemapSide(3, GL20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, 0, -1, 0, -1, 0)

    /** The positive Z and fifth side of the cubemap */
    case PositiveZ extends CubemapSide(4, GL20.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, -1, 0, 0, 0, 1)

    /** The negative Z and sixth side of the cubemap */
    case NegativeZ extends CubemapSide(5, GL20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, -1, 0, 0, 0, -1)

    /** The up vector to target the side. */
    val up: Vector3 = new Vector3(upX, upY, upZ)

    /** The direction vector to target the side. */
    val direction: Vector3 = new Vector3(directionX, directionY, directionZ)

    /** @return The OpenGL target (used for glTexImage2D) of the side. */
    def getGLEnum(): Int = glEnum

    /** @return The up vector of the side. */
    def getUp(out: Vector3): Vector3 = out.set(up)

    /** @return The direction vector of the side. */
    def getDirection(out: Vector3): Vector3 = out.set(direction)
  }

  private def addManagedCubemap(app: Application, cubemap: Cubemap): Unit =
    managedCubemaps.getOrElseUpdate(app, mutable.Set()) += cubemap

  /** Clears all managed cubemaps. This is an internal method. Do not use it! */
  def clearAllCubemaps(app: Application): Unit =
    managedCubemaps.remove(app)

  /** Invalidate all managed cubemaps. This is an internal method. Do not use it! */
  def invalidateAllCubemaps(app: Application): Unit =
    managedCubemaps.get(app) match {
      case Some(managedCubemapSet) =>
        if (assetManager.isEmpty) {
          for (cubemap <- managedCubemapSet)
            cubemap.reload()
        } else {
          // TODO: Implement full AssetManager functionality when available
          /*
          // first we have to make sure the AssetManager isn't loading anything anymore,
          // otherwise the ref counting trick below wouldn't work (when a cubemap is
          // currently on the task stack of the manager.)
          assetManager.orNull.finishLoading()

          // next we go through each cubemap and reload either directly or via the
          // asset manager.
          val cubemaps = managedCubemapSet.toSet // Copy to avoid concurrent modification
          for (cubemap <- cubemaps) {
            val fileName = assetManager.orNull.getAssetFileName(cubemap)
            if (fileName == null) {
              cubemap.reload()
            } else {
              // TODO: Implement CubemapParameter and CubemapLoader when they are available
              // get the ref count of the cubemap, then set it to 0 so we
              // can actually remove it from the assetmanager. Also set the
              // handle to zero, otherwise we might accidentially dispose
              // already reloaded cubemaps.
              val refCount = assetManager.orNull.getReferenceCount(fileName)
              assetManager.orNull.setReferenceCount(fileName, 0)
              cubemap.glHandle = TextureHandle.none

              // create the parameters, passing the reference to the cubemap as
              // well as a callback that sets the ref count.
              val params = new CubemapParameter()
              params.cubemapData = cubemap.getCubemapData()
              params.minFilter = cubemap.getMinFilter()
              params.magFilter = cubemap.getMagFilter()
              params.wrapU = cubemap.getUWrap()
              params.wrapV = cubemap.getVWrap()
              params.cubemap = cubemap // special parameter which will ensure that the references stay the same.
              params.loadedCallback = new LoadedCallback() {
                override def finishedLoading(assetManager: AssetManager, fileName: String, tpe: Class[_]): Unit = {
                  assetManager.setReferenceCount(fileName, refCount)
                }
              }

              // unload the c, create a new gl handle then reload it.
              assetManager.orNull.unload(fileName)
              cubemap.glHandle = TextureHandle(sge.graphics.gl.glGenTexture())
              assetManager.orNull.load(fileName, classOf[Cubemap], params)
            }
          }
          managedCubemapSet.clear()
          managedCubemapSet ++= cubemaps
           */
          // For now, just reload all cubemaps directly
          for (cubemap <- managedCubemapSet)
            cubemap.reload()
        }
      case None => // no cubemaps for this app
    }

  /** Sets the {@link AssetManager} . When the context is lost, cubemaps managed by the asset manager are reloaded by the manager on a separate thread (provided that a suitable {@link AssetLoader} is
    * registered with the manager). Cubemaps not managed by the AssetManager are reloaded via the usual means on the rendering thread.
    * @param manager
    *   the asset manager.
    */
  def setAssetManager(manager: AssetManager): Unit =
    Cubemap.assetManager = Nullable(manager)

  def getManagedStatus(): String = {
    val builder = new StringBuilder()
    builder.append("Managed cubemap/app: { ")
    for (app <- managedCubemaps.keys) {
      builder.append(managedCubemaps(app).size)
      builder.append(" ")
    }
    builder.append("}")
    builder.toString()
  }

  /** @return the number of managed cubemaps currently loaded */
  def getNumManagedCubemaps()(using Sge): Int =
    managedCubemaps.get(Sge().application).map(_.size).getOrElse(0)
}
