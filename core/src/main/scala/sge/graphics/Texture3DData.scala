/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/Texture3DData.java
 * Original authors: mgsx
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

/** Used by a {@link Texture3D} to load the pixel data. The Texture3D will request the Texture3DData to prepare itself through {@link #prepare()} and upload its data using {@link #consume3DData()} .
  * These are the first methods to be called by Texture3D. After that the Texture3D will invoke the other methods to find out about the size of the image data, the format, whether the Texture3DData is
  * able to manage the pixel data if the OpenGL ES context is lost. </p>
  *
  * Before a call to either {@link #consume3DData()} , Texture3D will bind the OpenGL ES texture. </p>
  *
  * @author
  *   mgsx
  */
trait Texture3DData {

  /** @return whether the TextureData is prepared or not. */
  def isPrepared(): Boolean

  /** Prepares the TextureData for a call to {@link #consume3DData()} . This method can be called from a non OpenGL thread and should thus not interact with OpenGL.
    */
  def prepare(): Unit

  /** @return the width of this Texture3D */
  def getWidth(): Int

  /** @return the height of this Texture3D */
  def getHeight(): Int

  /** @return the depth of this Texture3D */
  def getDepth(): Int

  /** @return the internal format of this Texture3D */
  def getInternalFormat(): Int

  /** @return the GL type of this Texture3D */
  def getGLType(): Int

  /** @return whether to generate mipmaps or not. */
  def useMipMaps(): Boolean

  /** Uploads the pixel data to the OpenGL ES texture. The caller must bind an OpenGL ES texture. A call to {@link #prepare()} must preceed a call to this method. Any internal data structures created
    * in {@link #prepare()} should be disposed of here.
    */
  def consume3DData(): Unit

  /** @return whether this implementation can cope with a EGL context loss. */
  def isManaged(): Boolean
}
