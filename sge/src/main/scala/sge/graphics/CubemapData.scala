/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/CubemapData.java
 * Original authors: Vincent Bousquet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 48
 * Covenant-baseline-methods: CubemapData,consumeCubemapData,height,isManaged,isPrepared,prepare,width
 * Covenant-source-reference: com/badlogic/gdx/graphics/CubemapData.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics

/** Used by a {@link Cubemap} to load the pixel data. The Cubemap will request the CubemapData to prepare itself through {@link #prepare()} and upload its data using {@link #consumeCubemapData()} .
  * These are the first methods to be called by Cubemap. After that the Cubemap will invoke the other methods to find out about the size of the image data, the format, whether the CubemapData is able
  * to manage the pixel data if the OpenGL ES context is lost. </p>
  *
  * Before a call to either {@link #consumeCubemapData()} , Cubemap will bind the OpenGL ES texture. </p>
  *
  * Look at {@link KTXTextureData} for example implementation of this interface.
  * @author
  *   Vincent Bousquet
  */
trait CubemapData {

  /** @return whether the TextureData is prepared or not. */
  def isPrepared: Boolean

  /** Prepares the TextureData for a call to {@link #consumeCubemapData()} . This method can be called from a non OpenGL thread and should thus not interact with OpenGL.
    */
  def prepare(): Unit

  /** Uploads the pixel data for the 6 faces of the cube to the OpenGL ES texture. The caller must bind an OpenGL ES texture. A call to {@link #prepare()} must preceed a call to this method. Any
    * internal data structures created in {@link #prepare()} should be disposed of here.
    */
  def consumeCubemapData(): Unit

  /** @return the width of the pixel data */
  def width: Int

  /** @return the height of the pixel data */
  def height: Int

  /** @return whether this implementation can cope with a EGL context loss. */
  def isManaged: Boolean
}
