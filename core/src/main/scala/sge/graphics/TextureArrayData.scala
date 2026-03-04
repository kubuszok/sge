/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/TextureArrayData.java
 * Original authors: Tomski
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Factory object replaces Java static inner class
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics

import sge.files.FileHandle
import sge.graphics.glutils.FileTextureArrayData

/** Used by a {@link TextureArray} to load the pixel data. The TextureArray will request the TextureArrayData to prepare itself through {@link #prepare()} and upload its data using
  * {@link #consumeTextureArrayData()} . These are the first methods to be called by TextureArray. After that the TextureArray will invoke the other methods to find out about the size of the image
  * data, the format, whether the TextureArrayData is able to manage the pixel data if the OpenGL ES context is lost. </p>
  *
  * Before a call to either {@link #consumeTextureArrayData()} , TextureArray will bind the OpenGL ES texture. </p>
  *
  * Look at {@link FileTextureArrayData} for example implementation of this interface.
  * @author
  *   Tomski
  */
trait TextureArrayData {

  /** @return whether the TextureArrayData is prepared or not. */
  def isPrepared(): Boolean

  /** Prepares the TextureArrayData for a call to {@link #consumeTextureArrayData()} . This method can be called from a non OpenGL thread and should thus not interact with OpenGL.
    */
  def prepare(): Unit

  /** Uploads the pixel data of the TextureArray layers of the TextureArray to the OpenGL ES texture. The caller must bind an OpenGL ES texture. A call to {@link #prepare()} must preceed a call to
    * this method. Any internal data structures created in {@link #prepare()} should be disposed of here.
    */
  def consumeTextureArrayData(): Unit

  /** @return the width of this TextureArray */
  def getWidth(): Int

  /** @return the height of this TextureArray */
  def getHeight(): Int

  /** @return the layer count of this TextureArray */
  def getDepth(): Int

  /** @return whether this implementation can cope with a EGL context loss. */
  def isManaged(): Boolean

  /** @return the internal format of this TextureArray */
  def getInternalFormat(): Int

  /** @return the GL type of this TextureArray */
  def getGLType(): Int
}

object TextureArrayData {

  /** Provides static method to instantiate the right implementation.
    * @author
    *   Tomski
    */
  object Factory {

    def loadFromFiles(format: Pixmap.Format, useMipMaps: Boolean, files: FileHandle*)(using Sge): TextureArrayData =
      new FileTextureArrayData(format, useMipMaps, files*)
  }
}
