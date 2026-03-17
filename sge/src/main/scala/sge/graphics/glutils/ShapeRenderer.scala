/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/ShapeRenderer.java
 * Original authors: mzechner, stbachmann, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: rect() -> rectangle(), dispose() -> close()
 *   Convention: ShapeType enum in companion object; uses (using Sge); rect() -> rectangle()
 *   Idiom: split packages; boundary/break instead of return; Nullable[ShapeType] instead of null
 *   Audited: 2026-03-06
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.graphics.{ Color, PrimitiveMode }
import sge.math.{ MathUtils, Matrix4, Vector2, Vector3 }
import sge.utils.Nullable
import sge.Sge
import scala.util.boundary
import scala.util.boundary.break

/** Renders points, lines, shape outlines and filled shapes. <p> By default a 2D orthographic projection with the origin in the lower left corner is used and units are specified in screen pixels. This
  * can be changed by configuring the projection matrix, usually using the Camera#combined matrix. If the screen resolution changes, the projection matrix may need to be updated. <p> Shapes are
  * rendered in batches to increase performance. Standard usage pattern looks as follows:
  *
  * <pre> {@code camera.update(); shapeRenderer.setProjectionMatrix(camera.combined);
  *
  * shapeRenderer.begin(ShapeType.Line); shapeRenderer.setColor(1, 1, 0, 1); shapeRenderer.line(x, y, x2, y2); shapeRenderer.rect(x, y, width, height); shapeRenderer.circle(x, y, radius);
  * shapeRenderer.end();
  *
  * shapeRenderer.begin(ShapeType.Filled); shapeRenderer.setColor(0, 1, 0, 1); shapeRenderer.rect(x, y, width, height); shapeRenderer.circle(x, y, radius); shapeRenderer.end(); } </pre>
  *
  * ShapeRenderer has a second matrix called the transformation matrix which is used to rotate, scale and translate shapes in a more flexible manner. The following example shows how to rotate a
  * rectangle around its center using the z-axis as the rotation axis and placing it's center at (20, 12, 2):
  *
  * <pre> shapeRenderer.begin(ShapeType.Line); shapeRenderer.identity(); shapeRenderer.translate(20, 12, 2); shapeRenderer.rotate(0, 0, 1, 90); shapeRenderer.rect(-width / 2, -height / 2, width,
  * height); shapeRenderer.end(); </pre>
  *
  * Matrix operations all use postmultiplication and work just like glTranslate, glScale and glRotate. The last transformation specified will be the first that is applied to a shape (rotate then
  * translate in the above example). <p> The projection and transformation matrices are a state of the ShapeRenderer, just like the color, and will be applied to all shapes until they are changed.
  * @author
  *   mzechner
  * @author
  *   stbachmann
  * @author
  *   Nathan Sweet
  */
class ShapeRenderer(maxVertices: Int, defaultShader: Nullable[ShaderProgram] = Nullable.empty)(using Sge) extends AutoCloseable {

  import ShapeRenderer.ShapeType

  private val renderer: ImmediateModeRenderer = defaultShader.fold {
    ImmediateModeRenderer20(maxVertices, false, true, 0)
  } { shader =>
    ImmediateModeRenderer20(maxVertices, false, true, 0, shader)
  }

  private var matrixDirty:          Boolean             = false
  private val projectionMatrix:     Matrix4             = Matrix4()
  private val transformMatrix:      Matrix4             = Matrix4()
  private val combinedMatrix:       Matrix4             = Matrix4()
  private val tmp:                  Vector2             = Vector2()
  private val color:                Color               = Color(1, 1, 1, 1)
  private var shapeType:            Nullable[ShapeType] = Nullable.empty
  private var autoShapeType:        Boolean             = false
  private val defaultRectLineWidth: Float               = 0.75f

  // Constructor body
  projectionMatrix.setToOrtho2D(0, 0, Sge().graphics.getWidth().toFloat, Sge().graphics.getHeight().toFloat)
  matrixDirty = true

  /** Create a new ShapeRenderer with default maxVertices of 5000 */
  def this()(using Sge) =
    this(5000)

  /** Sets the color to be used by the next shapes drawn. */
  def setColor(color: Color): Unit =
    this.color.set(color)

  /** Sets the color to be used by the next shapes drawn. */
  def setColor(r: Float, g: Float, b: Float, a: Float): Unit =
    this.color.set(r, g, b, a)

  def getColor(): Color = color

  def updateMatrices(): Unit =
    matrixDirty = true

  /** Sets the projection matrix to be used for rendering. Usually this will be set to Camera#combined.
    * @param matrix
    *   the matrix
    */
  def setProjectionMatrix(matrix: Matrix4): Unit = {
    projectionMatrix.set(matrix)
    matrixDirty = true
  }

  def getProjectionMatrix(): Matrix4 = projectionMatrix

  def setTransformMatrix(matrix: Matrix4): Unit = {
    transformMatrix.set(matrix)
    matrixDirty = true
  }

  def getTransformMatrix(): Matrix4 = transformMatrix

  /** Sets the transformation matrix to identity. */
  def identity(): Unit = {
    transformMatrix.idt()
    matrixDirty = true
  }

  /** Multiplies the current transformation matrix by a translation matrix.
    * @param x
    *   the translation in x
    * @param y
    *   the translation in y
    * @param z
    *   the translation in z
    */
  def translate(x: Float, y: Float, z: Float): Unit = {
    transformMatrix.translate(x, y, z)
    matrixDirty = true
  }

  /** Multiplies the current transformation matrix by a rotation matrix.
    * @param axisX
    *   the x component of the axis vector
    * @param axisY
    *   the y component of the axis vector
    * @param axisZ
    *   the z component of the axis vector
    * @param degrees
    *   the rotation angle in degrees
    */
  def rotate(axisX: Float, axisY: Float, axisZ: Float, degrees: Float): Unit = {
    transformMatrix.rotate(axisX, axisY, axisZ, degrees)
    matrixDirty = true
  }

  /** Multiplies the current transformation matrix by a scale matrix.
    * @param scaleX
    *   the scale in x
    * @param scaleY
    *   the scale in y
    * @param scaleZ
    *   the scale in z
    */
  def scale(scaleX: Float, scaleY: Float, scaleZ: Float): Unit = {
    transformMatrix.scale(scaleX, scaleY, scaleZ)
    matrixDirty = true
  }

  /** If true, when drawing a shape cannot be performed with the current shape type, the batch is flushed and the shape type is changed automatically. This can increase the number of batch flushes if
    * care is not taken to draw the same type of shapes together. Default is false.
    */
  def setAutoShapeType(autoShapeType: Boolean): Unit =
    this.autoShapeType = autoShapeType

  /** Begins a new batch without specifying a shape type.
    * @throws IllegalStateException
    *   if autoShapeType is false.
    */
  def begin(): Unit = {
    if (!autoShapeType) throw new IllegalStateException("autoShapeType must be true to use this method.")
    begin(ShapeType.Line)
  }

  /** Starts a new batch of shapes. Shapes drawn within the batch will attempt to use the type specified. The call to this method must be paired with a call to end().
    * @param shapeType
    *   the renderType.
    */
  def begin(shapeType: ShapeType): Unit = {
    if (this.shapeType.isDefined) throw new IllegalStateException("Call end() before beginning a new shape batch.")
    this.shapeType = Nullable(shapeType)
    if (matrixDirty) {
      combinedMatrix.set(projectionMatrix)
      combinedMatrix.mul(transformMatrix)
      matrixDirty = false
    }
    renderer.begin(combinedMatrix, shapeType.getGlType())
  }

  def set(shapeType: ShapeType): Unit =
    if (!this.shapeType.contains(shapeType)) {
      if (this.shapeType.isEmpty) throw new IllegalStateException("begin must be called first.")
      if (!autoShapeType) throw new IllegalStateException("autoShapeType must be enabled.")
      end()
      begin(shapeType)
    }

  /** Draws a point using ShapeType.Point, ShapeType.Line or ShapeType.Filled. */
  def point(x: Float, y: Float, z: Float): Unit =
    boundary {
      if (shapeType.contains(ShapeType.Line)) {
        val size = defaultRectLineWidth * 0.5f
        line(x - size, y - size, z, x + size, y + size, z)
        break(())
      } else if (shapeType.contains(ShapeType.Filled)) {
        val size = defaultRectLineWidth * 0.5f
        box(x - size, y - size, z - size, defaultRectLineWidth, defaultRectLineWidth, defaultRectLineWidth)
        break(())
      }
      check(ShapeType.Point, Nullable.empty, 1)
      renderer.color(color)
      renderer.vertex(x, y, z)
    }

  /** Draws a line using ShapeType.Line or ShapeType.Filled. */
  def line(x: Float, y: Float, z: Float, x2: Float, y2: Float, z2: Float): Unit =
    line(x, y, z, x2, y2, z2, color, color)

  /** @see #line(float, float, float, float, float, float) */
  def line(v0: Vector3, v1: Vector3): Unit =
    line(v0.x, v0.y, v0.z, v1.x, v1.y, v1.z, color, color)

  /** @see #line(float, float, float, float, float, float) */
  def line(x: Float, y: Float, x2: Float, y2: Float): Unit =
    line(x, y, 0.0f, x2, y2, 0.0f, color, color)

  /** @see #line(float, float, float, float, float, float) */
  def line(v0: Vector2, v1: Vector2): Unit =
    line(v0.x, v0.y, 0.0f, v1.x, v1.y, 0.0f, color, color)

  /** @see #line(float, float, float, float, float, float, Color, Color) */
  def line(x: Float, y: Float, x2: Float, y2: Float, c1: Color, c2: Color): Unit =
    line(x, y, 0.0f, x2, y2, 0.0f, c1, c2)

  /** Draws a line using ShapeType.Line or ShapeType.Filled. The line is drawn with two colors interpolated between the start and end points. */
  def line(x: Float, y: Float, z: Float, x2: Float, y2: Float, z2: Float, c1: Color, c2: Color): Unit =
    if (shapeType.contains(ShapeType.Filled)) {
      rectLine(x, y, x2, y2, defaultRectLineWidth, c1, c2)
    } else {
      check(ShapeType.Line, Nullable.empty, 2)
      renderer.color(c1.r, c1.g, c1.b, c1.a)
      renderer.vertex(x, y, z)
      renderer.color(c2.r, c2.g, c2.b, c2.a)
      renderer.vertex(x2, y2, z2)
    }

  /** Draws a curve using ShapeType.Line. */
  def curve(x1: Float, y1: Float, cx1: Float, cy1: Float, cx2: Float, cy2: Float, x2: Float, y2: Float, segments: Int): Unit = {
    check(ShapeType.Line, Nullable.empty, segments * 2 + 2)
    val colorBits = color.toFloatBits()

    // Algorithm from: http://www.antigrain.com/research/bezier_interpolation/index.html#PAGE_BEZIER_INTERPOLATION
    val subdivStep  = 1f / segments
    val subdivStep2 = subdivStep * subdivStep
    val subdivStep3 = subdivStep * subdivStep * subdivStep

    val pre1 = 3 * subdivStep
    val pre2 = 3 * subdivStep2
    val pre4 = 6 * subdivStep2
    val pre5 = 6 * subdivStep3

    val tmp1x = x1 - cx1 * 2 + cx2
    val tmp1y = y1 - cy1 * 2 + cy2

    val tmp2x = (cx1 - cx2) * 3 - x1 + x2
    val tmp2y = (cy1 - cy2) * 3 - y1 + y2

    var fx = x1
    var fy = y1

    var dfx = (cx1 - x1) * pre1 + tmp1x * pre2 + tmp2x * subdivStep3
    var dfy = (cy1 - y1) * pre1 + tmp1y * pre2 + tmp2y * subdivStep3

    var ddfx = tmp1x * pre4 + tmp2x * pre5
    var ddfy = tmp1y * pre4 + tmp2y * pre5

    val dddfx = tmp2x * pre5
    val dddfy = tmp2y * pre5

    var i = segments
    while (i > 0) {
      renderer.color(colorBits)
      renderer.vertex(fx, fy, 0)
      fx += dfx
      fy += dfy
      dfx += ddfx
      dfy += ddfy
      ddfx += dddfx
      ddfy += dddfy
      renderer.color(colorBits)
      renderer.vertex(fx, fy, 0)
      i -= 1
    }
    renderer.color(colorBits)
    renderer.vertex(fx, fy, 0)
    renderer.color(colorBits)
    renderer.vertex(x2, y2, 0)
  }

  /** Draws a triangle in x/y plane using ShapeType.Line or ShapeType.Filled. */
  def triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Unit = {
    check(ShapeType.Line, Nullable(ShapeType.Filled), 6)
    val colorBits = color.toFloatBits()
    if (shapeType.contains(ShapeType.Line)) {
      renderer.color(colorBits)
      renderer.vertex(x1, y1, 0)
      renderer.color(colorBits)
      renderer.vertex(x2, y2, 0)

      renderer.color(colorBits)
      renderer.vertex(x2, y2, 0)
      renderer.color(colorBits)
      renderer.vertex(x3, y3, 0)

      renderer.color(colorBits)
      renderer.vertex(x3, y3, 0)
      renderer.color(colorBits)
      renderer.vertex(x1, y1, 0)
    } else {
      renderer.color(colorBits)
      renderer.vertex(x1, y1, 0)
      renderer.color(colorBits)
      renderer.vertex(x2, y2, 0)
      renderer.color(colorBits)
      renderer.vertex(x3, y3, 0)
    }
  }

  /** Draws a triangle in x/y plane with colored corners using ShapeType.Line or ShapeType.Filled. */
  def triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, col1: Color, col2: Color, col3: Color): Unit = {
    check(ShapeType.Line, Nullable(ShapeType.Filled), 6)
    if (shapeType.contains(ShapeType.Line)) {
      renderer.color(col1.r, col1.g, col1.b, col1.a)
      renderer.vertex(x1, y1, 0)
      renderer.color(col2.r, col2.g, col2.b, col2.a)
      renderer.vertex(x2, y2, 0)

      renderer.color(col2.r, col2.g, col2.b, col2.a)
      renderer.vertex(x2, y2, 0)
      renderer.color(col3.r, col3.g, col3.b, col3.a)
      renderer.vertex(x3, y3, 0)

      renderer.color(col3.r, col3.g, col3.b, col3.a)
      renderer.vertex(x3, y3, 0)
      renderer.color(col1.r, col1.g, col1.b, col1.a)
      renderer.vertex(x1, y1, 0)
    } else {
      renderer.color(col1.r, col1.g, col1.b, col1.a)
      renderer.vertex(x1, y1, 0)
      renderer.color(col2.r, col2.g, col2.b, col2.a)
      renderer.vertex(x2, y2, 0)
      renderer.color(col3.r, col3.g, col3.b, col3.a)
      renderer.vertex(x3, y3, 0)
    }
  }

  /** Draws a rectangle in the x/y plane using ShapeType.Line or ShapeType.Filled. */
  def rectangle(x: Float, y: Float, width: Float, height: Float): Unit = {
    check(ShapeType.Line, Nullable(ShapeType.Filled), 8)
    val colorBits = color.toFloatBits()
    if (shapeType.contains(ShapeType.Line)) {
      renderer.color(colorBits)
      renderer.vertex(x, y, 0)
      renderer.color(colorBits)
      renderer.vertex(x + width, y, 0)

      renderer.color(colorBits)
      renderer.vertex(x + width, y, 0)
      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, 0)

      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, 0)
      renderer.color(colorBits)
      renderer.vertex(x, y + height, 0)

      renderer.color(colorBits)
      renderer.vertex(x, y + height, 0)
      renderer.color(colorBits)
      renderer.vertex(x, y, 0)
    } else {
      renderer.color(colorBits)
      renderer.vertex(x, y, 0)
      renderer.color(colorBits)
      renderer.vertex(x + width, y, 0)
      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, 0)

      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, 0)
      renderer.color(colorBits)
      renderer.vertex(x, y + height, 0)
      renderer.color(colorBits)
      renderer.vertex(x, y, 0)
    }
  }

  /** Draws a rectangle in the x/y plane using ShapeType.Line or ShapeType.Filled.
    * @param col1
    *   The color at (x, y).
    * @param col2
    *   The color at (x + width, y).
    * @param col3
    *   The color at (x + width, y + height).
    * @param col4
    *   The color at (x, y + height).
    */
  def rectangle(x: Float, y: Float, width: Float, height: Float, col1: Color, col2: Color, col3: Color, col4: Color): Unit = {
    check(ShapeType.Line, Nullable(ShapeType.Filled), 8)

    if (shapeType.contains(ShapeType.Line)) {
      renderer.color(col1.r, col1.g, col1.b, col1.a)
      renderer.vertex(x, y, 0)
      renderer.color(col2.r, col2.g, col2.b, col2.a)
      renderer.vertex(x + width, y, 0)

      renderer.color(col2.r, col2.g, col2.b, col2.a)
      renderer.vertex(x + width, y, 0)
      renderer.color(col3.r, col3.g, col3.b, col3.a)
      renderer.vertex(x + width, y + height, 0)

      renderer.color(col3.r, col3.g, col3.b, col3.a)
      renderer.vertex(x + width, y + height, 0)
      renderer.color(col4.r, col4.g, col4.b, col4.a)
      renderer.vertex(x, y + height, 0)

      renderer.color(col4.r, col4.g, col4.b, col4.a)
      renderer.vertex(x, y + height, 0)
      renderer.color(col1.r, col1.g, col1.b, col1.a)
      renderer.vertex(x, y, 0)
    } else {
      renderer.color(col1.r, col1.g, col1.b, col1.a)
      renderer.vertex(x, y, 0)
      renderer.color(col2.r, col2.g, col2.b, col2.a)
      renderer.vertex(x + width, y, 0)
      renderer.color(col3.r, col3.g, col3.b, col3.a)
      renderer.vertex(x + width, y + height, 0)

      renderer.color(col3.r, col3.g, col3.b, col3.a)
      renderer.vertex(x + width, y + height, 0)
      renderer.color(col4.r, col4.g, col4.b, col4.a)
      renderer.vertex(x, y + height, 0)
      renderer.color(col1.r, col1.g, col1.b, col1.a)
      renderer.vertex(x, y, 0)
    }
  }

  /** Draws a rectangle in the x/y plane using ShapeType.Line or ShapeType.Filled. The x and y specify the lower left corner. The originX and originY specify the point about which to rotate the
    * rectangle.
    */
  def rectangle(
    x:       Float,
    y:       Float,
    originX: Float,
    originY: Float,
    width:   Float,
    height:  Float,
    scaleX:  Float,
    scaleY:  Float,
    degrees: Float
  ): Unit =
    rectangle(x, y, originX, originY, width, height, scaleX, scaleY, degrees, color, color, color, color)

  /** Draws a rectangle in the x/y plane using ShapeType.Line or ShapeType.Filled. The x and y specify the lower left corner. The originX and originY specify the point about which to rotate the
    * rectangle.
    * @param col1
    *   The color at (x, y)
    * @param col2
    *   The color at (x + width, y)
    * @param col3
    *   The color at (x + width, y + height)
    * @param col4
    *   The color at (x, y + height)
    */
  def rectangle(
    x:       Float,
    y:       Float,
    originX: Float,
    originY: Float,
    width:   Float,
    height:  Float,
    scaleX:  Float,
    scaleY:  Float,
    degrees: Float,
    col1:    Color,
    col2:    Color,
    col3:    Color,
    col4:    Color
  ): Unit = {
    check(ShapeType.Line, Nullable(ShapeType.Filled), 8)

    val cos = MathUtils.cosDeg(degrees)
    val sin = MathUtils.sinDeg(degrees)
    var fx  = -originX
    var fy  = -originY
    var fx2 = width - originX
    var fy2 = height - originY

    if (scaleX != 1 || scaleY != 1) {
      fx *= scaleX
      fy *= scaleY
      fx2 *= scaleX
      fy2 *= scaleY
    }

    val worldOriginX = x + originX
    val worldOriginY = y + originY

    val x1 = cos * fx - sin * fy + worldOriginX
    val y1 = sin * fx + cos * fy + worldOriginY

    val x2 = cos * fx2 - sin * fy + worldOriginX
    val y2 = sin * fx2 + cos * fy + worldOriginY

    val x3 = cos * fx2 - sin * fy2 + worldOriginX
    val y3 = sin * fx2 + cos * fy2 + worldOriginY

    val x4 = x1 + (x3 - x2)
    val y4 = y3 - (y2 - y1)

    if (shapeType.contains(ShapeType.Line)) {
      renderer.color(col1.r, col1.g, col1.b, col1.a)
      renderer.vertex(x1, y1, 0)
      renderer.color(col2.r, col2.g, col2.b, col2.a)
      renderer.vertex(x2, y2, 0)

      renderer.color(col2.r, col2.g, col2.b, col2.a)
      renderer.vertex(x2, y2, 0)
      renderer.color(col3.r, col3.g, col3.b, col3.a)
      renderer.vertex(x3, y3, 0)

      renderer.color(col3.r, col3.g, col3.b, col3.a)
      renderer.vertex(x3, y3, 0)
      renderer.color(col4.r, col4.g, col4.b, col4.a)
      renderer.vertex(x4, y4, 0)

      renderer.color(col4.r, col4.g, col4.b, col4.a)
      renderer.vertex(x4, y4, 0)
      renderer.color(col1.r, col1.g, col1.b, col1.a)
      renderer.vertex(x1, y1, 0)
    } else {
      renderer.color(col1.r, col1.g, col1.b, col1.a)
      renderer.vertex(x1, y1, 0)
      renderer.color(col2.r, col2.g, col2.b, col2.a)
      renderer.vertex(x2, y2, 0)
      renderer.color(col3.r, col3.g, col3.b, col3.a)
      renderer.vertex(x3, y3, 0)

      renderer.color(col3.r, col3.g, col3.b, col3.a)
      renderer.vertex(x3, y3, 0)
      renderer.color(col4.r, col4.g, col4.b, col4.a)
      renderer.vertex(x4, y4, 0)
      renderer.color(col1.r, col1.g, col1.b, col1.a)
      renderer.vertex(x1, y1, 0)
    }
  }

  /** Draws a line using a rotated rectangle, where with one edge is centered at x1, y1 and the opposite edge centered at x2, y2. */
  def rectLine(x1: Float, y1: Float, x2: Float, y2: Float, width: Float): Unit = {
    check(ShapeType.Line, Nullable(ShapeType.Filled), 8)
    val colorBits = color.toFloatBits()
    val t         = tmp.set(y2 - y1, x1 - x2).normalize()
    val w         = width * 0.5f
    val tx        = t.x * w
    val ty        = t.y * w
    if (shapeType.contains(ShapeType.Line)) {
      renderer.color(colorBits)
      renderer.vertex(x1 + tx, y1 + ty, 0)
      renderer.color(colorBits)
      renderer.vertex(x1 - tx, y1 - ty, 0)

      renderer.color(colorBits)
      renderer.vertex(x2 + tx, y2 + ty, 0)
      renderer.color(colorBits)
      renderer.vertex(x2 - tx, y2 - ty, 0)

      renderer.color(colorBits)
      renderer.vertex(x2 + tx, y2 + ty, 0)
      renderer.color(colorBits)
      renderer.vertex(x1 + tx, y1 + ty, 0)

      renderer.color(colorBits)
      renderer.vertex(x2 - tx, y2 - ty, 0)
      renderer.color(colorBits)
      renderer.vertex(x1 - tx, y1 - ty, 0)
    } else {
      renderer.color(colorBits)
      renderer.vertex(x1 + tx, y1 + ty, 0)
      renderer.color(colorBits)
      renderer.vertex(x1 - tx, y1 - ty, 0)
      renderer.color(colorBits)
      renderer.vertex(x2 + tx, y2 + ty, 0)

      renderer.color(colorBits)
      renderer.vertex(x2 - tx, y2 - ty, 0)
      renderer.color(colorBits)
      renderer.vertex(x2 + tx, y2 + ty, 0)
      renderer.color(colorBits)
      renderer.vertex(x1 - tx, y1 - ty, 0)
    }
  }

  /** Draws a line using a rotated rectangle, where with one edge is centered at x1, y1 and the opposite edge centered at x2, y2. */
  def rectLine(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, c1: Color, c2: Color): Unit = {
    check(ShapeType.Line, Nullable(ShapeType.Filled), 8)
    val col1Bits = c1.toFloatBits()
    val col2Bits = c2.toFloatBits()
    val t        = tmp.set(y2 - y1, x1 - x2).normalize()
    val w        = width * 0.5f
    val tx       = t.x * w
    val ty       = t.y * w
    if (shapeType.contains(ShapeType.Line)) {
      renderer.color(col1Bits)
      renderer.vertex(x1 + tx, y1 + ty, 0)
      renderer.color(col1Bits)
      renderer.vertex(x1 - tx, y1 - ty, 0)

      renderer.color(col2Bits)
      renderer.vertex(x2 + tx, y2 + ty, 0)
      renderer.color(col2Bits)
      renderer.vertex(x2 - tx, y2 - ty, 0)

      renderer.color(col2Bits)
      renderer.vertex(x2 + tx, y2 + ty, 0)
      renderer.color(col1Bits)
      renderer.vertex(x1 + tx, y1 + ty, 0)

      renderer.color(col2Bits)
      renderer.vertex(x2 - tx, y2 - ty, 0)
      renderer.color(col1Bits)
      renderer.vertex(x1 - tx, y1 - ty, 0)
    } else {
      renderer.color(col1Bits)
      renderer.vertex(x1 + tx, y1 + ty, 0)
      renderer.color(col1Bits)
      renderer.vertex(x1 - tx, y1 - ty, 0)
      renderer.color(col2Bits)
      renderer.vertex(x2 + tx, y2 + ty, 0)

      renderer.color(col2Bits)
      renderer.vertex(x2 - tx, y2 - ty, 0)
      renderer.color(col2Bits)
      renderer.vertex(x2 + tx, y2 + ty, 0)
      renderer.color(col1Bits)
      renderer.vertex(x1 - tx, y1 - ty, 0)
    }
  }

  /** @see #rectLine(float, float, float, float, float) */
  def rectLine(p1: Vector2, p2: Vector2, width: Float): Unit =
    rectLine(p1.x, p1.y, p2.x, p2.y, width)

  /** Draws a cube using ShapeType.Line or ShapeType.Filled. The x, y and z specify the bottom, left, front corner of the rectangle. */
  def box(x: Float, y: Float, z: Float, width: Float, height: Float, depth: Float): Unit = {
    val d         = -depth
    val colorBits = color.toFloatBits()
    if (shapeType.contains(ShapeType.Line)) {
      check(ShapeType.Line, Nullable(ShapeType.Filled), 24)

      renderer.color(colorBits)
      renderer.vertex(x, y, z)
      renderer.color(colorBits)
      renderer.vertex(x + width, y, z)

      renderer.color(colorBits)
      renderer.vertex(x + width, y, z)
      renderer.color(colorBits)
      renderer.vertex(x + width, y, z + d)

      renderer.color(colorBits)
      renderer.vertex(x + width, y, z + d)
      renderer.color(colorBits)
      renderer.vertex(x, y, z + d)

      renderer.color(colorBits)
      renderer.vertex(x, y, z + d)
      renderer.color(colorBits)
      renderer.vertex(x, y, z)

      renderer.color(colorBits)
      renderer.vertex(x, y, z)
      renderer.color(colorBits)
      renderer.vertex(x, y + height, z)

      renderer.color(colorBits)
      renderer.vertex(x, y + height, z)
      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, z)

      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, z)
      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, z + d)

      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, z + d)
      renderer.color(colorBits)
      renderer.vertex(x, y + height, z + d)

      renderer.color(colorBits)
      renderer.vertex(x, y + height, z + d)
      renderer.color(colorBits)
      renderer.vertex(x, y + height, z)

      renderer.color(colorBits)
      renderer.vertex(x + width, y, z)
      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, z)

      renderer.color(colorBits)
      renderer.vertex(x + width, y, z + d)
      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, z + d)

      renderer.color(colorBits)
      renderer.vertex(x, y, z + d)
      renderer.color(colorBits)
      renderer.vertex(x, y + height, z + d)
    } else {
      check(ShapeType.Line, Nullable(ShapeType.Filled), 36)

      // Front
      renderer.color(colorBits)
      renderer.vertex(x, y, z)
      renderer.color(colorBits)
      renderer.vertex(x + width, y, z)
      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, z)

      renderer.color(colorBits)
      renderer.vertex(x, y, z)
      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, z)
      renderer.color(colorBits)
      renderer.vertex(x, y + height, z)

      // Back
      renderer.color(colorBits)
      renderer.vertex(x + width, y, z + d)
      renderer.color(colorBits)
      renderer.vertex(x, y, z + d)
      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, z + d)

      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, z + d)
      renderer.color(colorBits)
      renderer.vertex(x, y, z + d)
      renderer.color(colorBits)
      renderer.vertex(x, y + height, z + d)

      // Left
      renderer.color(colorBits)
      renderer.vertex(x, y, z + d)
      renderer.color(colorBits)
      renderer.vertex(x, y, z)
      renderer.color(colorBits)
      renderer.vertex(x, y + height, z)

      renderer.color(colorBits)
      renderer.vertex(x, y, z + d)
      renderer.color(colorBits)
      renderer.vertex(x, y + height, z)
      renderer.color(colorBits)
      renderer.vertex(x, y + height, z + d)

      // Right
      renderer.color(colorBits)
      renderer.vertex(x + width, y, z)
      renderer.color(colorBits)
      renderer.vertex(x + width, y, z + d)
      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, z + d)

      renderer.color(colorBits)
      renderer.vertex(x + width, y, z)
      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, z + d)
      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, z)

      // Top
      renderer.color(colorBits)
      renderer.vertex(x, y + height, z)
      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, z)
      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, z + d)

      renderer.color(colorBits)
      renderer.vertex(x, y + height, z)
      renderer.color(colorBits)
      renderer.vertex(x + width, y + height, z + d)
      renderer.color(colorBits)
      renderer.vertex(x, y + height, z + d)

      // Bottom
      renderer.color(colorBits)
      renderer.vertex(x, y, z + d)
      renderer.color(colorBits)
      renderer.vertex(x + width, y, z + d)
      renderer.color(colorBits)
      renderer.vertex(x + width, y, z)

      renderer.color(colorBits)
      renderer.vertex(x, y, z + d)
      renderer.color(colorBits)
      renderer.vertex(x + width, y, z)
      renderer.color(colorBits)
      renderer.vertex(x, y, z)
    }
  }

  /** Draws two crossed lines using ShapeType.Line or ShapeType.Filled. */
  def x(x: Float, y: Float, size: Float): Unit = {
    line(x - size, y - size, x + size, y + size)
    line(x - size, y + size, x + size, y - size)
  }

  /** @see #x(float, float, float) */
  def x(p: Vector2, size: Float): Unit =
    x(p.x, p.y, size)

  /** Calls arc(float, float, float, float, float, int) by estimating the number of segments needed for a smooth arc. */
  def arc(x: Float, y: Float, radius: Float, start: Float, degrees: Float): Unit =
    arc(x, y, radius, start, degrees, Math.max(1, (6 * Math.cbrt(radius).toFloat * (degrees / 360.0f)).toInt))

  /** Draws an arc using ShapeType.Line or ShapeType.Filled. */
  def arc(x: Float, y: Float, radius: Float, start: Float, degrees: Float, segments: Int): Unit = {
    if (segments <= 0) throw new IllegalArgumentException("segments must be > 0.")
    val colorBits = color.toFloatBits()
    val theta     = (2 * MathUtils.PI * (degrees / 360.0f)) / segments
    val cos       = MathUtils.cos(theta)
    val sin       = MathUtils.sin(theta)
    var cx        = radius * MathUtils.cos(start * MathUtils.degreesToRadians)
    var cy        = radius * MathUtils.sin(start * MathUtils.degreesToRadians)

    if (shapeType.contains(ShapeType.Line)) {
      check(ShapeType.Line, Nullable(ShapeType.Filled), segments * 2 + 2)

      renderer.color(colorBits)
      renderer.vertex(x, y, 0)
      renderer.color(colorBits)
      renderer.vertex(x + cx, y + cy, 0)
      var i = 0
      while (i < segments) {
        renderer.color(colorBits)
        renderer.vertex(x + cx, y + cy, 0)
        val temp = cx
        cx = cos * cx - sin * cy
        cy = sin * temp + cos * cy
        renderer.color(colorBits)
        renderer.vertex(x + cx, y + cy, 0)
        i += 1
      }
      renderer.color(colorBits)
      renderer.vertex(x + cx, y + cy, 0)
    } else {
      check(ShapeType.Line, Nullable(ShapeType.Filled), segments * 3 + 3)

      var i = 0
      while (i < segments) {
        renderer.color(colorBits)
        renderer.vertex(x, y, 0)
        renderer.color(colorBits)
        renderer.vertex(x + cx, y + cy, 0)
        val temp = cx
        cx = cos * cx - sin * cy
        cy = sin * temp + cos * cy
        renderer.color(colorBits)
        renderer.vertex(x + cx, y + cy, 0)
        i += 1
      }
      renderer.color(colorBits)
      renderer.vertex(x, y, 0)
      renderer.color(colorBits)
      renderer.vertex(x + cx, y + cy, 0)
    }

    cx = 0
    cy = 0
    renderer.color(colorBits)
    renderer.vertex(x + cx, y + cy, 0)
  }

  /** Calls circle(float, float, float, int) by estimating the number of segments needed for a smooth circle. */
  def circle(x: Float, y: Float, radius: Float): Unit =
    circle(x, y, radius, Math.max(1, (6 * Math.cbrt(radius).toFloat).toInt))

  /** Draws a circle using ShapeType.Line or ShapeType.Filled. */
  def circle(x: Float, y: Float, radius: Float, segments: Int): Unit = {
    if (segments <= 0) throw new IllegalArgumentException("segments must be > 0.")
    val colorBits = color.toFloatBits()
    val angle     = 2 * MathUtils.PI / segments
    val cos       = MathUtils.cos(angle)
    val sin       = MathUtils.sin(angle)
    var cx        = radius
    var cy        = 0f
    if (shapeType.contains(ShapeType.Line)) {
      check(ShapeType.Line, Nullable(ShapeType.Filled), segments * 2 + 2)
      var i = 0
      while (i < segments) {
        renderer.color(colorBits)
        renderer.vertex(x + cx, y + cy, 0)
        val temp = cx
        cx = cos * cx - sin * cy
        cy = sin * temp + cos * cy
        renderer.color(colorBits)
        renderer.vertex(x + cx, y + cy, 0)
        i += 1
      }
      // Ensure the last segment is identical to the first.
      renderer.color(colorBits)
      renderer.vertex(x + cx, y + cy, 0)
    } else {
      check(ShapeType.Line, Nullable(ShapeType.Filled), segments * 3 + 3)
      val segs = segments - 1
      var i    = 0
      while (i < segs) {
        renderer.color(colorBits)
        renderer.vertex(x, y, 0)
        renderer.color(colorBits)
        renderer.vertex(x + cx, y + cy, 0)
        val temp = cx
        cx = cos * cx - sin * cy
        cy = sin * temp + cos * cy
        renderer.color(colorBits)
        renderer.vertex(x + cx, y + cy, 0)
        i += 1
      }
      // Ensure the last segment is identical to the first.
      renderer.color(colorBits)
      renderer.vertex(x, y, 0)
      renderer.color(colorBits)
      renderer.vertex(x + cx, y + cy, 0)
    }

    cx = radius
    cy = 0
    renderer.color(colorBits)
    renderer.vertex(x + cx, y + cy, 0)
  }

  /** Calls ellipse(float, float, float, float, int) by estimating the number of segments needed for a smooth ellipse. */
  def ellipse(x: Float, y: Float, width: Float, height: Float): Unit =
    ellipse(x, y, width, height, Math.max(1, (12 * Math.cbrt(Math.max(width * 0.5f, height * 0.5f)).toFloat).toInt))

  /** Draws an ellipse using ShapeType.Line or ShapeType.Filled. */
  def ellipse(x: Float, y: Float, width: Float, height: Float, segments: Int): Unit = {
    if (segments <= 0) throw new IllegalArgumentException("segments must be > 0.")
    check(ShapeType.Line, Nullable(ShapeType.Filled), segments * 3)
    val colorBits = color.toFloatBits()
    val angle     = 2 * MathUtils.PI / segments

    val cx = x + width / 2
    val cy = y + height / 2
    if (shapeType.contains(ShapeType.Line)) {
      var i = 0
      while (i < segments) {
        renderer.color(colorBits)
        renderer.vertex(cx + (width * 0.5f * MathUtils.cos(i * angle)), cy + (height * 0.5f * MathUtils.sin(i * angle)), 0)

        renderer.color(colorBits)
        renderer.vertex(
          cx + (width * 0.5f * MathUtils.cos((i + 1) * angle)),
          cy + (height * 0.5f * MathUtils.sin((i + 1) * angle)),
          0
        )
        i += 1
      }
    } else {
      var i = 0
      while (i < segments) {
        renderer.color(colorBits)
        renderer.vertex(cx + (width * 0.5f * MathUtils.cos(i * angle)), cy + (height * 0.5f * MathUtils.sin(i * angle)), 0)

        renderer.color(colorBits)
        renderer.vertex(cx, cy, 0)

        renderer.color(colorBits)
        renderer.vertex(
          cx + (width * 0.5f * MathUtils.cos((i + 1) * angle)),
          cy + (height * 0.5f * MathUtils.sin((i + 1) * angle)),
          0
        )
        i += 1
      }
    }
  }

  /** Calls ellipse(float, float, float, float, float, int) by estimating the number of segments needed for a smooth ellipse. */
  def ellipse(x: Float, y: Float, width: Float, height: Float, rotation: Float): Unit =
    ellipse(
      x,
      y,
      width,
      height,
      rotation,
      Math.max(1, (12 * Math.cbrt(Math.max(width * 0.5f, height * 0.5f)).toFloat).toInt)
    )

  /** Draws an ellipse using ShapeType.Line or ShapeType.Filled. */
  def ellipse(x: Float, y: Float, width: Float, height: Float, rotation: Float, segments: Int): Unit = {
    if (segments <= 0) throw new IllegalArgumentException("segments must be > 0.")
    check(ShapeType.Line, Nullable(ShapeType.Filled), segments * 3)
    val colorBits = color.toFloatBits()
    val angle     = 2 * MathUtils.PI / segments

    val rot = MathUtils.PI * rotation / 180f
    val sin = MathUtils.sin(rot)
    val cos = MathUtils.cos(rot)

    val cx = x + width / 2
    val cy = y + height / 2
    var x1 = width * 0.5f
    var y1 = 0f
    if (shapeType.contains(ShapeType.Line)) {
      var i = 0
      while (i < segments) {
        renderer.color(colorBits)
        renderer.vertex(cx + cos * x1 - sin * y1, cy + sin * x1 + cos * y1, 0)

        x1 = width * 0.5f * MathUtils.cos((i + 1) * angle)
        y1 = height * 0.5f * MathUtils.sin((i + 1) * angle)

        renderer.color(colorBits)
        renderer.vertex(cx + cos * x1 - sin * y1, cy + sin * x1 + cos * y1, 0)
        i += 1
      }
    } else {
      var i = 0
      while (i < segments) {
        renderer.color(colorBits)
        renderer.vertex(cx + cos * x1 - sin * y1, cy + sin * x1 + cos * y1, 0)

        renderer.color(colorBits)
        renderer.vertex(cx, cy, 0)

        x1 = width * 0.5f * MathUtils.cos((i + 1) * angle)
        y1 = height * 0.5f * MathUtils.sin((i + 1) * angle)

        renderer.color(colorBits)
        renderer.vertex(cx + cos * x1 - sin * y1, cy + sin * x1 + cos * y1, 0)
        i += 1
      }
    }
  }

  /** Calls cone(float, float, float, float, float, int) by estimating the number of segments needed for a smooth circular base. */
  def cone(x: Float, y: Float, z: Float, radius: Float, height: Float): Unit =
    cone(x, y, z, radius, height, Math.max(1, (4 * Math.sqrt(radius).toFloat).toInt))

  /** Draws a cone using ShapeType.Line or ShapeType.Filled. */
  def cone(x: Float, y: Float, z: Float, radius: Float, height: Float, segments: Int): Unit = {
    if (segments <= 0) throw new IllegalArgumentException("segments must be > 0.")
    check(ShapeType.Line, Nullable(ShapeType.Filled), segments * 4 + 2)
    val colorBits = color.toFloatBits()
    val angle     = 2 * MathUtils.PI / segments
    val cos       = MathUtils.cos(angle)
    val sin       = MathUtils.sin(angle)
    var cx        = radius
    var cy        = 0f
    if (shapeType.contains(ShapeType.Line)) {
      var i = 0
      while (i < segments) {
        renderer.color(colorBits)
        renderer.vertex(x + cx, y + cy, z)
        renderer.color(colorBits)
        renderer.vertex(x, y, z + height)
        renderer.color(colorBits)
        renderer.vertex(x + cx, y + cy, z)
        val temp = cx
        cx = cos * cx - sin * cy
        cy = sin * temp + cos * cy
        renderer.color(colorBits)
        renderer.vertex(x + cx, y + cy, z)
        i += 1
      }
      // Ensure the last segment is identical to the first.
      renderer.color(colorBits)
      renderer.vertex(x + cx, y + cy, z)
    } else {
      val segs = segments - 1
      var i    = 0
      while (i < segs) {
        renderer.color(colorBits)
        renderer.vertex(x, y, z)
        renderer.color(colorBits)
        renderer.vertex(x + cx, y + cy, z)
        val temp  = cx
        val temp2 = cy
        cx = cos * cx - sin * cy
        cy = sin * temp + cos * cy
        renderer.color(colorBits)
        renderer.vertex(x + cx, y + cy, z)

        renderer.color(colorBits)
        renderer.vertex(x + temp, y + temp2, z)
        renderer.color(colorBits)
        renderer.vertex(x + cx, y + cy, z)
        renderer.color(colorBits)
        renderer.vertex(x, y, z + height)
        i += 1
      }
      // Ensure the last segment is identical to the first.
      renderer.color(colorBits)
      renderer.vertex(x, y, z)
      renderer.color(colorBits)
      renderer.vertex(x + cx, y + cy, z)
    }
    val temp  = cx
    val temp2 = cy
    cx = radius
    cy = 0
    renderer.color(colorBits)
    renderer.vertex(x + cx, y + cy, z)
    if (!shapeType.contains(ShapeType.Line)) {
      renderer.color(colorBits)
      renderer.vertex(x + temp, y + temp2, z)
      renderer.color(colorBits)
      renderer.vertex(x + cx, y + cy, z)
      renderer.color(colorBits)
      renderer.vertex(x, y, z + height)
    }
  }

  /** Draws a polygon in the x/y plane using ShapeType.Line. The vertices must contain at least 3 points (6 floats x,y). */
  def polygon(vertices: Array[Float], offset: Int, count: Int): Unit = {
    if (count < 6) throw new IllegalArgumentException("Polygons must contain at least 3 points.")
    if (count % 2 != 0) throw new IllegalArgumentException("Polygons must have an even number of vertices.")

    check(ShapeType.Line, Nullable.empty, count)
    val colorBits = color.toFloatBits()
    val firstX    = vertices(0)
    val firstY    = vertices(1)

    var i = offset
    val n = offset + count
    while (i < n) {
      val x1 = vertices(i)
      val y1 = vertices(i + 1)

      val x2: Float =
        if (i + 2 >= count) firstX
        else vertices(i + 2)
      val y2: Float =
        if (i + 2 >= count) firstY
        else vertices(i + 3)

      renderer.color(colorBits)
      renderer.vertex(x1, y1, 0)
      renderer.color(colorBits)
      renderer.vertex(x2, y2, 0)
      i += 2
    }
  }

  /** @see #polygon(float[], int, int) */
  def polygon(vertices: Array[Float]): Unit =
    polygon(vertices, 0, vertices.length)

  /** Draws a polyline in the x/y plane using ShapeType.Line. The vertices must contain at least 2 points (4 floats x,y). */
  def polyline(vertices: Array[Float], offset: Int, count: Int): Unit = {
    if (count < 4) throw new IllegalArgumentException("Polylines must contain at least 2 points.")
    if (count % 2 != 0) throw new IllegalArgumentException("Polylines must have an even number of vertices.")

    check(ShapeType.Line, Nullable.empty, count)
    val colorBits = color.toFloatBits()
    var i         = offset
    val n         = offset + count - 2
    while (i < n) {
      val x1 = vertices(i)
      val y1 = vertices(i + 1)

      val x2 = vertices(i + 2)
      val y2 = vertices(i + 3)

      renderer.color(colorBits)
      renderer.vertex(x1, y1, 0)
      renderer.color(colorBits)
      renderer.vertex(x2, y2, 0)
      i += 2
    }
  }

  /** @see #polyline(float[], int, int) */
  def polyline(vertices: Array[Float]): Unit =
    polyline(vertices, 0, vertices.length)

  /** Checks whether the correct ShapeType was set. If not and autoShapeType is enabled, it flushes the batch and changes the shape type. The batch is also flushed, when the matrix has been changed or
    * not enough vertices remain.
    *
    * @param preferred
    *   usually ShapeType.Line
    * @param other
    *   usually ShapeType.Filled. May be empty.
    * @param newVertices
    *   vertices count of geometric figure you want to draw
    */
  private def check(preferred: ShapeType, other: Nullable[ShapeType], newVertices: Int): Unit = {
    if (shapeType.isEmpty) throw new IllegalStateException("begin must be called first.")
    if (!shapeType.exists(st => st == preferred || other.contains(st))) {
      // Shape type is not valid.
      if (!autoShapeType) {
        other.fold {
          throw new IllegalStateException(s"Must call begin(ShapeType.${preferred.toString}).")
        } { o =>
          throw new IllegalStateException(s"Must call begin(ShapeType.${preferred.toString}) or begin(ShapeType.${o.toString}).")
        }
      } else {
        end()
        begin(preferred)
      }
    } else if (matrixDirty) {
      // Matrix has been changed.
      val st = shapeType
      end()
      st.foreach(begin)
    } else if (renderer.getMaxVertices() - renderer.getNumVertices() < newVertices) {
      // Not enough space.
      val st = shapeType
      end()
      st.foreach(begin)
    }
  }

  /** Finishes the batch of shapes and ensures they get rendered. */
  def end(): Unit = {
    renderer.end()
    shapeType = Nullable.empty
  }

  def flush(): Unit =
    shapeType.foreach { st =>
      end()
      begin(st)
    }

  /** Returns the current shape type. */
  def getCurrentType(): Nullable[ShapeType] = shapeType

  def getRenderer(): ImmediateModeRenderer = renderer

  /** @return true if currently between begin and end. */
  def isDrawing: Boolean = shapeType.isDefined

  override def close(): Unit =
    renderer.dispose()
}

object ShapeRenderer {

  /** Shape types to be used with begin(ShapeType).
    * @author
    *   mzechner, stbachmann
    */
  enum ShapeType(val glType: PrimitiveMode) {
    case Point extends ShapeType(PrimitiveMode.Points)
    case Line extends ShapeType(PrimitiveMode.Lines)
    case Filled extends ShapeType(PrimitiveMode.Triangles)

    def getGlType(): PrimitiveMode = glType
  }
}
