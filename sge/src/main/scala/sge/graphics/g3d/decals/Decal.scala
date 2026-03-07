/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/decals/Decal.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes (audited 2026-03-03):
 * - vertices: Java protected -> Scala private[decals]: correct (DecalBatch accesses it)
 * - updated: Java protected -> Scala private[decals]: correct (DecalBatch accesses it)
 * - update(): Java protected -> Scala private[decals]: correct (DecalBatch calls it)
 * - transformationOffset: Java public null -> Scala Nullable[Vector2]: correct
 * - transformVertices: Nullable.fold replaces null check — equivalent logic
 * - material: Java protected field -> Scala public var param: slightly wider, acceptable
 *   since getMaterial/setMaterial are public anyway
 * - rotator: Java protected static -> Scala protected in companion: access works through
 *   Decal.rotator in the class body — correct
 * - All 6 newDecal factory overloads faithfully ported
 * - All vertex index constants (X1..V4) in companion object: correct
 * - All instance methods (setColor, setRotation*, translate*, setPosition*, setScale*,
 *   setDimensions, getVertices, setTextureRegion, setBlending, lookAt, etc.): all match
 * - No return statements, no null usage: correct
 * - Status: pass
 * - Fixes (2026-03-04): Java-style getters/setters → property accessors (x/y/z/scaleX/scaleY/
 *   width/height/color/textureRegion); position/rotation widened to public val; getMaterial/
 *   setMaterial removed (already public var); backing fields renamed _color/_vertices
 * Idiom: typed GL enums -- BlendFactor, CompareFunc, EnableCap
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

  private[decals] var _vertices: Array[Float] = new Array[Float](Decal.SIZE)
  val position:                  Vector3      = Vector3()
  val rotation:                  Quaternion   = Quaternion()
  protected val scale:           Vector2      = Vector2(1, 1)
  private val _color:            Color        = Color()

  /** The transformation offset can be used to change the pivot point for rotation and scaling. By default the pivot is the middle of the decal.
    */
  var transformationOffset: Nullable[Vector2] = Nullable.empty
  protected val dimensions: Vector2           = Vector2()

  private[decals] var updated: Boolean = false

  /** Set a multipurpose value which can be queried and used for things like group identification. */
  var value: Int = 0

  def this()(using Sge) = {
    this(DecalMaterial())
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
    _color.set(r, g, b, a)
    val intBits    = ((255 * a).toInt << 24) | ((255 * b).toInt << 16) | ((255 * g).toInt << 8) | (255 * r).toInt
    val colorFloat = NumberUtils.intToFloatColor(intBits)
    _vertices(Decal.C1) = colorFloat
    _vertices(Decal.C2) = colorFloat
    _vertices(Decal.C3) = colorFloat
    _vertices(Decal.C4) = colorFloat
  }

  /** Returns the color of this decal. The returned color should under no circumstances be modified.
    *
    * @return
    *   The color of this decal.
    */
  def color: Color = _color

  /** Sets the color used to tint this decal. Default is {@link Color#WHITE}. */
  def color_=(tint: Color): Unit = {
    _color.set(tint)
    val colorFloat = tint.toFloatBits()
    _vertices(Decal.C1) = colorFloat
    _vertices(Decal.C2) = colorFloat
    _vertices(Decal.C3) = colorFloat
    _vertices(Decal.C4) = colorFloat
  }

  /** Sets the color of this decal, expanding the alpha from 0-254 to 0-255.
    * @see
    *   #setColor(Color)
    */
  def setPackedColor(color: Float): Unit = {
    Color.abgr8888ToColor(_color, color)
    _vertices(Decal.C1) = color
    _vertices(Decal.C2) = color
    _vertices(Decal.C3) = color
    _vertices(Decal.C4) = color
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

  /** Moves by the specified amount of units along the x axis
    *
    * @param units
    *   Units to move the decal
    */
  def translateX(units: Float): Unit = {
    this.position.x += units
    updated = false
  }

  /** @return position on the x axis */
  def x: Float = this.position.x

  /** Sets the position on the x axis
    *
    * @param x
    *   Position to locate the decal at
    */
  def x_=(x: Float): Unit = {
    this.position.x = x
    updated = false
  }

  /** Moves by the specified amount of units along the y axis
    *
    * @param units
    *   Units to move the decal
    */
  def translateY(units: Float): Unit = {
    this.position.y += units
    updated = false
  }

  /** @return position on the y axis */
  def y: Float = this.position.y

  /** Sets the position on the y axis
    *
    * @param y
    *   Position to locate the decal at
    */
  def y_=(y: Float): Unit = {
    this.position.y = y
    updated = false
  }

  /** Moves by the specified amount of units along the z axis
    *
    * @param units
    *   Units to move the decal
    */
  def translateZ(units: Float): Unit = {
    this.position.z += units
    updated = false
  }

  /** @return position on the z axis */
  def z: Float = this.position.z

  /** Sets the position on the z axis
    *
    * @param z
    *   Position to locate the decal at
    */
  def z_=(z: Float): Unit = {
    this.position.z = z
    updated = false
  }

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

  /** @return Scale on the x axis */
  def scaleX: Float = this.scale.x

  /** Sets scale along the x axis
    *
    * @param scale
    *   New scale along x axis
    */
  def scaleX_=(scale: Float): Unit = {
    this.scale.x = scale
    updated = false
  }

  /** @return Scale on the y axis */
  def scaleY: Float = this.scale.y

  /** Sets scale along the y axis
    *
    * @param scale
    *   New scale along y axis
    */
  def scaleY_=(scale: Float): Unit = {
    this.scale.y = scale
    updated = false
  }

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

  /** @return width in world units */
  def width: Float = this.dimensions.x

  /** Sets the width in world units
    *
    * @param width
    *   Width in world units
    */
  def width_=(width: Float): Unit = {
    this.dimensions.x = width
    updated = false
  }

  /** @return height in world units */
  def height: Float = dimensions.y

  /** Sets the height in world units
    *
    * @param height
    *   Height in world units
    */
  def height_=(height: Float): Unit = {
    this.dimensions.y = height
    updated = false
  }

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
  def vertices: Array[Float] = {
    update()
    _vertices
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
    x = (_vertices(Decal.X1) + tx) * scale.x
    y = (_vertices(Decal.Y1) + ty) * scale.y
    z = _vertices(Decal.Z1)
    // then transform the vector using the rotation quaternion
    _vertices(Decal.X1) = rotation.w * x + rotation.y * z - rotation.z * y
    _vertices(Decal.Y1) = rotation.w * y + rotation.z * x - rotation.x * z
    _vertices(Decal.Z1) = rotation.w * z + rotation.x * y - rotation.y * x
    w = -rotation.x * x - rotation.y * y - rotation.z * z
    rotation.conjugate()
    x = _vertices(Decal.X1)
    y = _vertices(Decal.Y1)
    z = _vertices(Decal.Z1)
    _vertices(Decal.X1) = w * rotation.x + x * rotation.w + y * rotation.z - z * rotation.y
    _vertices(Decal.Y1) = w * rotation.y + y * rotation.w + z * rotation.x - x * rotation.z
    _vertices(Decal.Z1) = w * rotation.z + z * rotation.w + x * rotation.y - y * rotation.x
    rotation.conjugate() // <- don't forget to conjugate the rotation back to normal
    // finally translate the vector according to position
    _vertices(Decal.X1) += position.x - tx
    _vertices(Decal.Y1) += position.y - ty
    _vertices(Decal.Z1) += position.z

    /** Transform the second vertex */
    // first apply the scale to the vector
    x = (_vertices(Decal.X2) + tx) * scale.x
    y = (_vertices(Decal.Y2) + ty) * scale.y
    z = _vertices(Decal.Z2)
    // then transform the vector using the rotation quaternion
    _vertices(Decal.X2) = rotation.w * x + rotation.y * z - rotation.z * y
    _vertices(Decal.Y2) = rotation.w * y + rotation.z * x - rotation.x * z
    _vertices(Decal.Z2) = rotation.w * z + rotation.x * y - rotation.y * x
    w = -rotation.x * x - rotation.y * y - rotation.z * z
    rotation.conjugate()
    x = _vertices(Decal.X2)
    y = _vertices(Decal.Y2)
    z = _vertices(Decal.Z2)
    _vertices(Decal.X2) = w * rotation.x + x * rotation.w + y * rotation.z - z * rotation.y
    _vertices(Decal.Y2) = w * rotation.y + y * rotation.w + z * rotation.x - x * rotation.z
    _vertices(Decal.Z2) = w * rotation.z + z * rotation.w + x * rotation.y - y * rotation.x
    rotation.conjugate() // <- don't forget to conjugate the rotation back to normal
    // finally translate the vector according to position
    _vertices(Decal.X2) += position.x - tx
    _vertices(Decal.Y2) += position.y - ty
    _vertices(Decal.Z2) += position.z

    /** Transform the third vertex */
    // first apply the scale to the vector
    x = (_vertices(Decal.X3) + tx) * scale.x
    y = (_vertices(Decal.Y3) + ty) * scale.y
    z = _vertices(Decal.Z3)
    // then transform the vector using the rotation quaternion
    _vertices(Decal.X3) = rotation.w * x + rotation.y * z - rotation.z * y
    _vertices(Decal.Y3) = rotation.w * y + rotation.z * x - rotation.x * z
    _vertices(Decal.Z3) = rotation.w * z + rotation.x * y - rotation.y * x
    w = -rotation.x * x - rotation.y * y - rotation.z * z
    rotation.conjugate()
    x = _vertices(Decal.X3)
    y = _vertices(Decal.Y3)
    z = _vertices(Decal.Z3)
    _vertices(Decal.X3) = w * rotation.x + x * rotation.w + y * rotation.z - z * rotation.y
    _vertices(Decal.Y3) = w * rotation.y + y * rotation.w + z * rotation.x - x * rotation.z
    _vertices(Decal.Z3) = w * rotation.z + z * rotation.w + x * rotation.y - y * rotation.x
    rotation.conjugate() // <- don't forget to conjugate the rotation back to normal
    // finally translate the vector according to position
    _vertices(Decal.X3) += position.x - tx
    _vertices(Decal.Y3) += position.y - ty
    _vertices(Decal.Z3) += position.z

    /** Transform the fourth vertex */
    // first apply the scale to the vector
    x = (_vertices(Decal.X4) + tx) * scale.x
    y = (_vertices(Decal.Y4) + ty) * scale.y
    z = _vertices(Decal.Z4)
    // then transform the vector using the rotation quaternion
    _vertices(Decal.X4) = rotation.w * x + rotation.y * z - rotation.z * y
    _vertices(Decal.Y4) = rotation.w * y + rotation.z * x - rotation.x * z
    _vertices(Decal.Z4) = rotation.w * z + rotation.x * y - rotation.y * x
    w = -rotation.x * x - rotation.y * y - rotation.z * z
    rotation.conjugate()
    x = _vertices(Decal.X4)
    y = _vertices(Decal.Y4)
    z = _vertices(Decal.Z4)
    _vertices(Decal.X4) = w * rotation.x + x * rotation.w + y * rotation.z - z * rotation.y
    _vertices(Decal.Y4) = w * rotation.y + y * rotation.w + z * rotation.x - x * rotation.z
    _vertices(Decal.Z4) = w * rotation.z + z * rotation.w + x * rotation.y - y * rotation.x
    rotation.conjugate() // <- don't forget to conjugate the rotation back to normal
    // finally translate the vector according to position
    _vertices(Decal.X4) += position.x - tx
    _vertices(Decal.Y4) += position.y - ty
    _vertices(Decal.Z4) += position.z
    updated = true
  }

  /** Resets the position components of the vertices array based ont he dimensions (preparation for transformation) */
  protected def resetVertices(): Unit = {
    val left   = -dimensions.x / 2f
    val right  = left + dimensions.x
    val top    = dimensions.y / 2f
    val bottom = top - dimensions.y

    // left top
    _vertices(Decal.X1) = left
    _vertices(Decal.Y1) = top
    _vertices(Decal.Z1) = 0
    // right top
    _vertices(Decal.X2) = right
    _vertices(Decal.Y2) = top
    _vertices(Decal.Z2) = 0
    // left bot
    _vertices(Decal.X3) = left
    _vertices(Decal.Y3) = bottom
    _vertices(Decal.Z3) = 0
    // right bot
    _vertices(Decal.X4) = right
    _vertices(Decal.Y4) = bottom
    _vertices(Decal.Z4) = 0

    updated = false
  }

  /** Re-applies the uv coordinates from the material's texture region to the uv components of the vertices array */
  protected def updateUVs(): Unit = {
    val tr = material.textureRegion
    // left top
    _vertices(Decal.U1) = tr.u
    _vertices(Decal.V1) = tr.v
    // right top
    _vertices(Decal.U2) = tr.u2
    _vertices(Decal.V2) = tr.v
    // left bot
    _vertices(Decal.U3) = tr.u
    _vertices(Decal.V3) = tr.v2
    // right bot
    _vertices(Decal.U4) = tr.u2
    _vertices(Decal.V4) = tr.v2
  }

  /** @return the texture region this Decal uses. Do not modify it! */
  def textureRegion: TextureRegion = this.material.textureRegion

  /** Sets the texture region
    *
    * @param textureRegion
    *   Texture region to apply
    */
  def textureRegion_=(textureRegion: TextureRegion): Unit = {
    this.material.textureRegion = textureRegion
    updateUVs()
  }

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
  private val tmp  = Vector3()
  private val tmp2 = Vector3()

  private val dir: Vector3 = Vector3()

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

  protected val rotator: Quaternion = Quaternion(0, 0, 0, 0)

  /** Creates a decal assuming the dimensions of the texture region
    *
    * @param textureRegion
    *   Texture region to use
    * @return
    *   Created decal
    */
  def newDecal(textureRegion: TextureRegion)(using Sge): Decal =
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
  def newDecal(textureRegion: TextureRegion, hasTransparency: Boolean)(using Sge): Decal =
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
  def newDecal(width: Float, height: Float, textureRegion: TextureRegion)(using Sge): Decal =
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
  def newDecal(width: Float, height: Float, textureRegion: TextureRegion, hasTransparency: Boolean)(using Sge): Decal =
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
  def newDecal(width: Float, height: Float, textureRegion: TextureRegion, srcBlendFactor: Int, dstBlendFactor: Int)(using Sge): Decal = {
    val decal = Decal()
    decal.textureRegion = textureRegion
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
    val decal = Decal(material)
    decal.textureRegion = textureRegion
    decal.setBlending(srcBlendFactor, dstBlendFactor)
    decal.dimensions.x = width
    decal.dimensions.y = height
    decal.setColor(1, 1, 1, 1)
    decal
  }
}
