/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/decals/Decal.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package decals

import sge.graphics.Color
import sge.graphics.GL20
import sge.graphics.g2d.TextureRegion
import sge.math.Quaternion
import sge.math.Vector2
import sge.math.Vector3
import sge.utils.NumberUtils
import sge.utils.Nullable

/** <p/> Represents a sprite in 3d space. Typical 3d transformations such as translation, rotation and scaling are supported. The position includes a z component other than setting the depth no manual
  * layering has to be performed, correct overlay is guaranteed by using the depth buffer. <p/> Decals are handled by the {@link DecalBatch}.
  */
class Decal(var material: DecalMaterial) {

  private[decals] var vertices: Array[Float] = new Array[Float](Decal.SIZE)
  protected val position:       Vector3      = new Vector3()
  protected val rotation:       Quaternion   = new Quaternion()
  protected val scale:          Vector2      = new Vector2(1, 1)
  protected val color:          Color        = new Color()

  /** The transformation offset can be used to change the pivot point for rotation and scaling. By default the pivot is the middle of the decal.
    */
  var transformationOffset: Nullable[Vector2] = Nullable.empty
  protected val dimensions: Vector2           = new Vector2()

  private[decals] var updated: Boolean = false

  /** Set a multipurpose value which can be queried and used for things like group identification. */
  var value: Int = 0

  def this() = {
    this(new DecalMaterial())
  }

  /** Sets the color of all four vertices to the specified color
    *
    * @param r
    *   Red component
    * @param g
    *   Green component
    * @param b
    *   Blue component
    * @param a
    *   Alpha component
    */
  def setColor(r: Float, g: Float, b: Float, a: Float): Unit = {
    color.set(r, g, b, a)
    val intBits    = ((255 * a).toInt << 24) | ((255 * b).toInt << 16) | ((255 * g).toInt << 8) | (255 * r).toInt
    val colorFloat = NumberUtils.intToFloatColor(intBits)
    vertices(Decal.C1) = colorFloat
    vertices(Decal.C2) = colorFloat
    vertices(Decal.C3) = colorFloat
    vertices(Decal.C4) = colorFloat
  }

  /** Sets the color used to tint this decal. Default is {@link Color#WHITE}. */
  def setColor(tint: Color): Unit = {
    color.set(tint)
    val colorFloat = tint.toFloatBits()
    vertices(Decal.C1) = colorFloat
    vertices(Decal.C2) = colorFloat
    vertices(Decal.C3) = colorFloat
    vertices(Decal.C4) = colorFloat
  }

  /** Sets the color of this decal, expanding the alpha from 0-254 to 0-255.
    * @see
    *   #setColor(Color)
    */
  def setPackedColor(color: Float): Unit = {
    Color.abgr8888ToColor(this.color, color)
    vertices(Decal.C1) = color
    vertices(Decal.C2) = color
    vertices(Decal.C3) = color
    vertices(Decal.C4) = color
  }

  /** Sets the rotation on the local X axis to the specified angle
    *
    * @param angle
    *   Angle in degrees to set rotation to
    */
  def setRotationX(angle: Float): Unit = {
    rotation.set(Vector3.X, angle)
    updated = false
  }

  /** Sets the rotation on the local Y axis to the specified angle
    *
    * @param angle
    *   Angle in degrees to set rotation to
    */
  def setRotationY(angle: Float): Unit = {
    rotation.set(Vector3.Y, angle)
    updated = false
  }

  /** Sets the rotation on the local Z axis to the specified angle
    *
    * @param angle
    *   Angle in degrees to set rotation to
    */
  def setRotationZ(angle: Float): Unit = {
    rotation.set(Vector3.Z, angle)
    updated = false
  }

  /** Rotates along local X axis by the specified angle
    *
    * @param angle
    *   Angle in degrees to rotate by
    */
  def rotateX(angle: Float): Unit = {
    Decal.rotator.set(Vector3.X, angle)
    rotation.mul(Decal.rotator)
    updated = false
  }

  /** Rotates along local Y axis by the specified angle
    *
    * @param angle
    *   Angle in degrees to rotate by
    */
  def rotateY(angle: Float): Unit = {
    Decal.rotator.set(Vector3.Y, angle)
    rotation.mul(Decal.rotator)
    updated = false
  }

  /** Rotates along local Z axis by the specified angle
    *
    * @param angle
    *   Angle in degrees to rotate by
    */
  def rotateZ(angle: Float): Unit = {
    Decal.rotator.set(Vector3.Z, angle)
    rotation.mul(Decal.rotator)
    updated = false
  }

  /** Sets the rotation of this decal to the given angles on all axes.
    * @param yaw
    *   Angle in degrees to rotate around the Y axis
    * @param pitch
    *   Angle in degrees to rotate around the X axis
    * @param roll
    *   Angle in degrees to rotate around the Z axis
    */
  def setRotation(yaw: Float, pitch: Float, roll: Float): Unit = {
    rotation.setEulerAngles(yaw, pitch, roll)
    updated = false
  }

  /** Sets the rotation of this decal based on the (normalized) direction and up vector.
    * @param dir
    *   the direction vector
    * @param up
    *   the up vector
    */
  def setRotation(dir: Vector3, up: Vector3): Unit = {
    Decal.tmp.set(up).crs(dir).nor()
    Decal.tmp2.set(dir).crs(Decal.tmp).nor()
    rotation.setFromAxes(Decal.tmp.x, Decal.tmp2.x, dir.x, Decal.tmp.y, Decal.tmp2.y, dir.y, Decal.tmp.z, Decal.tmp2.z, dir.z)
    updated = false
  }

  /** Sets the rotation of this decal based on the provided Quaternion
    * @param q
    *   desired Rotation
    */
  def setRotation(q: Quaternion): Unit = {
    rotation.set(q)
    updated = false
  }

  /** Returns the rotation. The returned quaternion should under no circumstances be modified.
    *
    * @return
    *   Quaternion representing the rotation
    */
  def getRotation: Quaternion = rotation

  /** Moves by the specified amount of units along the x axis
    *
    * @param units
    *   Units to move the decal
    */
  def translateX(units: Float): Unit = {
    this.position.x += units
    updated = false
  }

  /** Sets the position on the x axis
    *
    * @param x
    *   Position to locate the decal at
    */
  def setX(x: Float): Unit = {
    this.position.x = x
    updated = false
  }

  /** @return position on the x axis */
  def getX: Float = this.position.x

  /** Moves by the specified amount of units along the y axis
    *
    * @param units
    *   Units to move the decal
    */
  def translateY(units: Float): Unit = {
    this.position.y += units
    updated = false
  }

  /** Sets the position on the y axis
    *
    * @param y
    *   Position to locate the decal at
    */
  def setY(y: Float): Unit = {
    this.position.y = y
    updated = false
  }

  /** @return position on the y axis */
  def getY: Float = this.position.y

  /** Moves by the specified amount of units along the z axis
    *
    * @param units
    *   Units to move the decal
    */
  def translateZ(units: Float): Unit = {
    this.position.z += units
    updated = false
  }

  /** Sets the position on the z axis
    *
    * @param z
    *   Position to locate the decal at
    */
  def setZ(z: Float): Unit = {
    this.position.z = z
    updated = false
  }

  /** @return position on the z axis */
  def getZ: Float = this.position.z

  /** Translates by the specified amount of units
    *
    * @param x
    *   Units to move along the x axis
    * @param y
    *   Units to move along the y axis
    * @param z
    *   Units to move along the z axis
    */
  def translate(x: Float, y: Float, z: Float): Unit = {
    this.position.add(x, y, z)
    updated = false
  }

  /** @see Decal#translate(float, float, float) */
  def translate(trans: Vector3): Unit = {
    this.position.add(trans)
    updated = false
  }

  /** Sets the position to the given world coordinates
    *
    * @param x
    *   X position
    * @param y
    *   Y Position
    * @param z
    *   Z Position
    */
  def setPosition(x: Float, y: Float, z: Float): Unit = {
    this.position.set(x, y, z)
    updated = false
  }

  /** @see Decal#setPosition(float, float, float) */
  def setPosition(pos: Vector3): Unit = {
    this.position.set(pos)
    updated = false
  }

  /** Returns the color of this decal. The returned color should under no circumstances be modified.
    *
    * @return
    *   The color of this decal.
    */
  def getColor: Color = color

  /** Returns the position of this decal. The returned vector should under no circumstances be modified.
    *
    * @return
    *   vector representing the position
    */
  def getPosition: Vector3 = position

  /** Sets scale along the x axis
    *
    * @param scale
    *   New scale along x axis
    */
  def setScaleX(scale: Float): Unit = {
    this.scale.x = scale
    updated = false
  }

  /** @return Scale on the x axis */
  def getScaleX: Float = this.scale.x

  /** Sets scale along the y axis
    *
    * @param scale
    *   New scale along y axis
    */
  def setScaleY(scale: Float): Unit = {
    this.scale.y = scale
    updated = false
  }

  /** @return Scale on the y axis */
  def getScaleY: Float = this.scale.y

  /** Sets scale along both the x and y axis
    *
    * @param scaleX
    *   Scale on the x axis
    * @param scaleY
    *   Scale on the y axis
    */
  def setScale(scaleX: Float, scaleY: Float): Unit = {
    this.scale.set(scaleX, scaleY)
    updated = false
  }

  /** Sets scale along both the x and y axis
    *
    * @param scale
    *   New scale
    */
  def setScale(scale: Float): Unit = {
    this.scale.set(scale, scale)
    updated = false
  }

  /** Sets the width in world units
    *
    * @param width
    *   Width in world units
    */
  def setWidth(width: Float): Unit = {
    this.dimensions.x = width
    updated = false
  }

  /** @return width in world units */
  def getWidth: Float = this.dimensions.x

  /** Sets the height in world units
    *
    * @param height
    *   Height in world units
    */
  def setHeight(height: Float): Unit = {
    this.dimensions.y = height
    updated = false
  }

  /** @return height in world units */
  def getHeight: Float = dimensions.y

  /** Sets the width and height in world units
    *
    * @param width
    *   Width in world units
    * @param height
    *   Height in world units
    */
  def setDimensions(width: Float, height: Float): Unit = {
    dimensions.set(width, height)
    updated = false
  }

  /** Returns the vertices backing this sprite.<br/> The returned value should under no circumstances be modified.
    *
    * @return
    *   vertex array backing the decal
    */
  def getVertices: Array[Float] = {
    update()
    vertices
  }

  /** Recalculates vertices array if it grew out of sync with the properties (position, ..) */
  private[decals] def update(): Unit =
    if (!updated) {
      resetVertices()
      transformVertices()
    }

  /** Transforms the position component of the vertices using properties such as position, scale, etc. */
  protected def transformVertices(): Unit = {

    /** It would be possible to also load the x,y,z into a Vector3 and apply all the transformations using already existing methods. Especially the quaternion rotation already exists in the Quaternion
      * class, it then would look like this: ---------------------------------------------------------------------------------------------------- v3.set(vertices[xIndex] * scale.x, vertices[yIndex] *
      * scale.y, vertices[zIndex]); rotation.transform(v3); v3.add(position); vertices[xIndex] = v3.x; vertices[yIndex] = v3.y; vertices[zIndex] = v3.z;
      * ---------------------------------------------------------------------------------------------------- However, a half ass benchmark with dozens of thousands decals showed that doing it "by
      * hand", as done here, is about 10% faster. So while duplicate code should be avoided for maintenance reasons etc. the performance gain is worth it. The math doesn't change.
      */
    var x, y, z, w: Float = 0f
    var tx, ty:     Float = 0f
    transformationOffset.fold {
      tx = 0f
      ty = 0f
    } { offset =>
      tx = -offset.x
      ty = -offset.y
    }

    /** Transform the first vertex */
    // first apply the scale to the vector
    x = (vertices(Decal.X1) + tx) * scale.x
    y = (vertices(Decal.Y1) + ty) * scale.y
    z = vertices(Decal.Z1)
    // then transform the vector using the rotation quaternion
    vertices(Decal.X1) = rotation.w * x + rotation.y * z - rotation.z * y
    vertices(Decal.Y1) = rotation.w * y + rotation.z * x - rotation.x * z
    vertices(Decal.Z1) = rotation.w * z + rotation.x * y - rotation.y * x
    w = -rotation.x * x - rotation.y * y - rotation.z * z
    rotation.conjugate()
    x = vertices(Decal.X1)
    y = vertices(Decal.Y1)
    z = vertices(Decal.Z1)
    vertices(Decal.X1) = w * rotation.x + x * rotation.w + y * rotation.z - z * rotation.y
    vertices(Decal.Y1) = w * rotation.y + y * rotation.w + z * rotation.x - x * rotation.z
    vertices(Decal.Z1) = w * rotation.z + z * rotation.w + x * rotation.y - y * rotation.x
    rotation.conjugate() // <- don't forget to conjugate the rotation back to normal
    // finally translate the vector according to position
    vertices(Decal.X1) += position.x - tx
    vertices(Decal.Y1) += position.y - ty
    vertices(Decal.Z1) += position.z

    /** Transform the second vertex */
    // first apply the scale to the vector
    x = (vertices(Decal.X2) + tx) * scale.x
    y = (vertices(Decal.Y2) + ty) * scale.y
    z = vertices(Decal.Z2)
    // then transform the vector using the rotation quaternion
    vertices(Decal.X2) = rotation.w * x + rotation.y * z - rotation.z * y
    vertices(Decal.Y2) = rotation.w * y + rotation.z * x - rotation.x * z
    vertices(Decal.Z2) = rotation.w * z + rotation.x * y - rotation.y * x
    w = -rotation.x * x - rotation.y * y - rotation.z * z
    rotation.conjugate()
    x = vertices(Decal.X2)
    y = vertices(Decal.Y2)
    z = vertices(Decal.Z2)
    vertices(Decal.X2) = w * rotation.x + x * rotation.w + y * rotation.z - z * rotation.y
    vertices(Decal.Y2) = w * rotation.y + y * rotation.w + z * rotation.x - x * rotation.z
    vertices(Decal.Z2) = w * rotation.z + z * rotation.w + x * rotation.y - y * rotation.x
    rotation.conjugate() // <- don't forget to conjugate the rotation back to normal
    // finally translate the vector according to position
    vertices(Decal.X2) += position.x - tx
    vertices(Decal.Y2) += position.y - ty
    vertices(Decal.Z2) += position.z

    /** Transform the third vertex */
    // first apply the scale to the vector
    x = (vertices(Decal.X3) + tx) * scale.x
    y = (vertices(Decal.Y3) + ty) * scale.y
    z = vertices(Decal.Z3)
    // then transform the vector using the rotation quaternion
    vertices(Decal.X3) = rotation.w * x + rotation.y * z - rotation.z * y
    vertices(Decal.Y3) = rotation.w * y + rotation.z * x - rotation.x * z
    vertices(Decal.Z3) = rotation.w * z + rotation.x * y - rotation.y * x
    w = -rotation.x * x - rotation.y * y - rotation.z * z
    rotation.conjugate()
    x = vertices(Decal.X3)
    y = vertices(Decal.Y3)
    z = vertices(Decal.Z3)
    vertices(Decal.X3) = w * rotation.x + x * rotation.w + y * rotation.z - z * rotation.y
    vertices(Decal.Y3) = w * rotation.y + y * rotation.w + z * rotation.x - x * rotation.z
    vertices(Decal.Z3) = w * rotation.z + z * rotation.w + x * rotation.y - y * rotation.x
    rotation.conjugate() // <- don't forget to conjugate the rotation back to normal
    // finally translate the vector according to position
    vertices(Decal.X3) += position.x - tx
    vertices(Decal.Y3) += position.y - ty
    vertices(Decal.Z3) += position.z

    /** Transform the fourth vertex */
    // first apply the scale to the vector
    x = (vertices(Decal.X4) + tx) * scale.x
    y = (vertices(Decal.Y4) + ty) * scale.y
    z = vertices(Decal.Z4)
    // then transform the vector using the rotation quaternion
    vertices(Decal.X4) = rotation.w * x + rotation.y * z - rotation.z * y
    vertices(Decal.Y4) = rotation.w * y + rotation.z * x - rotation.x * z
    vertices(Decal.Z4) = rotation.w * z + rotation.x * y - rotation.y * x
    w = -rotation.x * x - rotation.y * y - rotation.z * z
    rotation.conjugate()
    x = vertices(Decal.X4)
    y = vertices(Decal.Y4)
    z = vertices(Decal.Z4)
    vertices(Decal.X4) = w * rotation.x + x * rotation.w + y * rotation.z - z * rotation.y
    vertices(Decal.Y4) = w * rotation.y + y * rotation.w + z * rotation.x - x * rotation.z
    vertices(Decal.Z4) = w * rotation.z + z * rotation.w + x * rotation.y - y * rotation.x
    rotation.conjugate() // <- don't forget to conjugate the rotation back to normal
    // finally translate the vector according to position
    vertices(Decal.X4) += position.x - tx
    vertices(Decal.Y4) += position.y - ty
    vertices(Decal.Z4) += position.z
    updated = true
  }

  /** Resets the position components of the vertices array based ont he dimensions (preparation for transformation) */
  protected def resetVertices(): Unit = {
    val left   = -dimensions.x / 2f
    val right  = left + dimensions.x
    val top    = dimensions.y / 2f
    val bottom = top - dimensions.y

    // left top
    vertices(Decal.X1) = left
    vertices(Decal.Y1) = top
    vertices(Decal.Z1) = 0
    // right top
    vertices(Decal.X2) = right
    vertices(Decal.Y2) = top
    vertices(Decal.Z2) = 0
    // left bot
    vertices(Decal.X3) = left
    vertices(Decal.Y3) = bottom
    vertices(Decal.Z3) = 0
    // right bot
    vertices(Decal.X4) = right
    vertices(Decal.Y4) = bottom
    vertices(Decal.Z4) = 0

    updated = false
  }

  /** Re-applies the uv coordinates from the material's texture region to the uv components of the vertices array */
  protected def updateUVs(): Unit = {
    val tr = material.textureRegion
    // left top
    vertices(Decal.U1) = tr.getU()
    vertices(Decal.V1) = tr.getV()
    // right top
    vertices(Decal.U2) = tr.getU2()
    vertices(Decal.V2) = tr.getV()
    // left bot
    vertices(Decal.U3) = tr.getU()
    vertices(Decal.V3) = tr.getV2()
    // right bot
    vertices(Decal.U4) = tr.getU2()
    vertices(Decal.V4) = tr.getV2()
  }

  /** Sets the texture region
    *
    * @param textureRegion
    *   Texture region to apply
    */
  def setTextureRegion(textureRegion: TextureRegion): Unit = {
    this.material.textureRegion = textureRegion
    updateUVs()
  }

  /** @return the texture region this Decal uses. Do not modify it! */
  def getTextureRegion: TextureRegion = this.material.textureRegion

  /** Sets the blending parameters for this decal
    *
    * @param srcBlendFactor
    *   Source blend factor used by glBlendFunc
    * @param dstBlendFactor
    *   Destination blend factor used by glBlendFunc
    */
  def setBlending(srcBlendFactor: Int, dstBlendFactor: Int): Unit = {
    material.srcBlendFactor = srcBlendFactor
    material.dstBlendFactor = dstBlendFactor
  }

  def getMaterial: DecalMaterial = material

  /** Set material
    *
    * @param material
    *   custom material
    */
  def setMaterial(material: DecalMaterial): Unit =
    this.material = material

  /** Sets the rotation of the Decal to face the given point. Useful for billboarding.
    * @param position
    * @param up
    */
  def lookAt(position: Vector3, up: Vector3): Unit = {
    Decal.dir.set(position).sub(this.position).nor()
    setRotation(Decal.dir, up)
  }
}

object Decal {
  // 3(x,y,z) + 1(color) + 2(u,v)
  /** Size of a decal vertex in floats */
  final private val VERTEX_SIZE = 3 + 1 + 2

  /** Size of the decal in floats. It takes a float[SIZE] to hold the decal. */
  final val SIZE: Int = 4 * VERTEX_SIZE

  /** Temporary vector for various calculations. */
  private val tmp  = new Vector3()
  private val tmp2 = new Vector3()

  private val dir: Vector3 = new Vector3()

  // meaning of the floats in the vertices array
  final val X1 = 0
  final val Y1 = 1
  final val Z1 = 2
  final val C1 = 3
  final val U1 = 4
  final val V1 = 5
  final val X2 = 6
  final val Y2 = 7
  final val Z2 = 8
  final val C2 = 9
  final val U2 = 10
  final val V2 = 11
  final val X3 = 12
  final val Y3 = 13
  final val Z3 = 14
  final val C3 = 15
  final val U3 = 16
  final val V3 = 17
  final val X4 = 18
  final val Y4 = 19
  final val Z4 = 20
  final val C4 = 21
  final val U4 = 22
  final val V4 = 23

  protected val rotator: Quaternion = new Quaternion(0, 0, 0, 0)

  /** Creates a decal assuming the dimensions of the texture region
    *
    * @param textureRegion
    *   Texture region to use
    * @return
    *   Created decal
    */
  def newDecal(textureRegion: TextureRegion): Decal =
    newDecal(
      textureRegion.regionWidth.toFloat,
      textureRegion.regionHeight.toFloat,
      textureRegion,
      DecalMaterial.NO_BLEND,
      DecalMaterial.NO_BLEND
    )

  /** Creates a decal assuming the dimensions of the texture region and adding transparency
    *
    * @param textureRegion
    *   Texture region to use
    * @param hasTransparency
    *   Whether or not this sprite will be treated as having transparency (transparent png, etc.)
    * @return
    *   Created decal
    */
  def newDecal(textureRegion: TextureRegion, hasTransparency: Boolean): Decal =
    newDecal(
      textureRegion.regionWidth.toFloat,
      textureRegion.regionHeight.toFloat,
      textureRegion,
      if (hasTransparency) GL20.GL_SRC_ALPHA else DecalMaterial.NO_BLEND,
      if (hasTransparency) GL20.GL_ONE_MINUS_SRC_ALPHA else DecalMaterial.NO_BLEND
    )

  /** Creates a decal using the region for texturing
    *
    * @param width
    *   Width of the decal in world units
    * @param height
    *   Height of the decal in world units
    * @param textureRegion
    *   TextureRegion to use
    * @return
    *   Created decal
    */
  // TODO : it would be convenient if {@link com.badlogic.gdx.graphics.Texture} had a getFormat() method to assume transparency
  // from RGBA,..
  def newDecal(width: Float, height: Float, textureRegion: TextureRegion): Decal =
    newDecal(width, height, textureRegion, DecalMaterial.NO_BLEND, DecalMaterial.NO_BLEND)

  /** Creates a decal using the region for texturing
    *
    * @param width
    *   Width of the decal in world units
    * @param height
    *   Height of the decal in world units
    * @param textureRegion
    *   TextureRegion to use
    * @param hasTransparency
    *   Whether or not this sprite will be treated as having transparency (transparent png, etc.)
    * @return
    *   Created decal
    */
  def newDecal(width: Float, height: Float, textureRegion: TextureRegion, hasTransparency: Boolean): Decal =
    newDecal(
      width,
      height,
      textureRegion,
      if (hasTransparency) GL20.GL_SRC_ALPHA else DecalMaterial.NO_BLEND,
      if (hasTransparency) GL20.GL_ONE_MINUS_SRC_ALPHA else DecalMaterial.NO_BLEND
    )

  /** Creates a decal using the region for texturing and the specified blending parameters for blending
    *
    * @param width
    *   Width of the decal in world units
    * @param height
    *   Height of the decal in world units
    * @param textureRegion
    *   TextureRegion to use
    * @param srcBlendFactor
    *   Source blend used by glBlendFunc
    * @param dstBlendFactor
    *   Destination blend used by glBlendFunc
    * @return
    *   Created decal
    */
  def newDecal(width: Float, height: Float, textureRegion: TextureRegion, srcBlendFactor: Int, dstBlendFactor: Int): Decal = {
    val decal = new Decal()
    decal.setTextureRegion(textureRegion)
    decal.setBlending(srcBlendFactor, dstBlendFactor)
    decal.dimensions.x = width
    decal.dimensions.y = height
    decal.setColor(1, 1, 1, 1)
    decal
  }

  /** Creates a decal using the region for texturing and the specified blending parameters for blending
    *
    * @param width
    *   Width of the decal in world units
    * @param height
    *   Height of the decal in world units
    * @param textureRegion
    *   TextureRegion to use
    * @param srcBlendFactor
    *   Source blend used by glBlendFunc
    * @param dstBlendFactor
    *   Destination blend used by glBlendFunc
    * @param material
    *   Custom decal material
    * @return
    *   Created decal
    */
  def newDecal(width: Float, height: Float, textureRegion: TextureRegion, srcBlendFactor: Int, dstBlendFactor: Int, material: DecalMaterial): Decal = {
    val decal = new Decal(material)
    decal.setTextureRegion(textureRegion)
    decal.setBlending(srcBlendFactor, dstBlendFactor)
    decal.dimensions.x = width
    decal.dimensions.y = height
    decal.setColor(1, 1, 1, 1)
    decal
  }
}
