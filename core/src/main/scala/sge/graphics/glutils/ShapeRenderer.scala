/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/ShapeRenderer.java
 * Original authors: mzechner, stbachmann, Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import sge.graphics.{ Color, GL20 }
import sge.math.{ MathUtils, Matrix4, Vector2, Vector3 }
import sge.utils.Nullable
import sge.Sge

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
class ShapeRenderer(using sge: Sge) extends AutoCloseable {

  /** Shape types to be used with begin(ShapeType).
    * @author
    *   mzechner, stbachmann
    */
  enum ShapeType(val glType: Int) {
    case Point extends ShapeType(GL20.GL_POINTS)
    case Line extends ShapeType(GL20.GL_LINES)
    case Filled extends ShapeType(GL20.GL_TRIANGLES)

    def getGlType(): Int = glType
  }

  private var renderer:             ImmediateModeRenderer = scala.compiletime.uninitialized
  private var matrixDirty:          Boolean               = false
  private val projectionMatrix:     Matrix4               = new Matrix4()
  private val transformMatrix:      Matrix4               = new Matrix4()
  private val combinedMatrix:       Matrix4               = new Matrix4()
  private val tmp:                  Vector2               = new Vector2()
  private val color:                Color                 = new Color(1, 1, 1, 1)
  private var shapeType:            Nullable[ShapeType]   = Nullable.empty
  private var autoShapeType:        Boolean               = false
  private var defaultRectLineWidth: Float                 = 0.75f

  /** Create a new ShapeRenderer with default maxVertices of 5000 */
  def this(maxVertices: Int)(using sge: Sge) = {
    this()
    // TODO: Initialize renderer when implementation is available
    projectionMatrix.setToOrtho2D(0, 0, sge.graphics.getWidth().toFloat, sge.graphics.getHeight().toFloat)
    matrixDirty = true
  }

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

  /** Sets the ShapeType. The default is Line.
    * @param autoShapeType
    *   whether to auto-switch shape types
    */
  def setAutoShapeType(autoShapeType: Boolean): Unit =
    this.autoShapeType = autoShapeType

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
    // TODO: renderer.begin(combinedMatrix, shapeType.getGlType())
  }

  def set(shapeType: ShapeType): Unit =
    if (!this.shapeType.fold(false)(_ == shapeType)) {
      if (this.shapeType.isEmpty) throw new IllegalStateException("begin must be called first.")
      if (!autoShapeType) throw new IllegalStateException("autoShapeType must be enabled.")
      end()
      begin(shapeType)
    }

  /** Finishes the batch of shapes and ensures they get rendered. */
  def end(): Unit =
    // TODO: renderer.end()
    shapeType = Nullable.empty

  def isDrawing: Boolean = shapeType.isDefined

  /** Draws a line using ShapeType.Line or ShapeType.Filled. */
  def line(x: Float, y: Float, z: Float, x2: Float, y2: Float, z2: Float): Unit =
    line(x, y, z, x2, y2, z2, color, color)

  /** Draws a line using ShapeType.Line or ShapeType.Filled. */
  def line(x: Float, y: Float, x2: Float, y2: Float): Unit =
    line(x, y, 0.0f, x2, y2, 0.0f, color, color)

  /** Draws a line using ShapeType.Line or ShapeType.Filled. The line is drawn with two colors interpolated between the start and end points.
    */
  def line(x: Float, y: Float, z: Float, x2: Float, y2: Float, z2: Float, c1: Color, c2: Color): Unit =
    if (shapeType.fold(false)(_ == ShapeType.Filled)) {
      rectLine(x, y, x2, y2, defaultRectLineWidth, c1, c2)
    } else {
      check(ShapeType.Line, Nullable.empty, 2)
      // TODO: renderer.color(c1.r, c1.g, c1.b, c1.a)
      // TODO: renderer.vertex(x, y, z)
      // TODO: renderer.color(c2.r, c2.g, c2.b, c2.a)
      // TODO: renderer.vertex(x2, y2, z2)
    }

  /** Draws a rectangle using ShapeType.Line or ShapeType.Filled. */
  def rectangle(x: Float, y: Float, width: Float, height: Float): Unit =
    rectangle(x, y, width, height, color, color, color, color)

  /** Draws a rectangle using ShapeType.Line or ShapeType.Filled. */
  def rectangle(x: Float, y: Float, width: Float, height: Float, col1: Color, col2: Color, col3: Color, col4: Color): Unit = {
    check(ShapeType.Line, Nullable(ShapeType.Filled), 8)

    if (shapeType.fold(false)(_ == ShapeType.Line)) {
      // TODO: Implement line rectangle
    } else {
      // TODO: Implement filled rectangle
    }
  }

  /** Draws a rectangle line using ShapeType.Filled. */
  def rectLine(x1: Float, y1: Float, x2: Float, y2: Float, width: Float, c1: Color, c2: Color): Unit =
    check(ShapeType.Filled, Nullable.empty, 8)
  // TODO: Implement rectangle line

  /** Draws a circle using ShapeType.Line or ShapeType.Filled. */
  def circle(x: Float, y: Float, radius: Float): Unit =
    circle(x, y, radius, Math.max(1, (6 * Math.cbrt(radius)).toInt))

  /** Draws a circle using ShapeType.Line or ShapeType.Filled. */
  def circle(x: Float, y: Float, radius: Float, segments: Int): Unit = {
    if (segments <= 0) throw new IllegalArgumentException("segments must be > 0.")
    check(ShapeType.Line, Nullable(ShapeType.Filled), segments * 2 + 2)

    val angle = 2 * MathUtils.PI / segments
    var cos   = MathUtils.cos(angle)
    var sin   = MathUtils.sin(angle)
    var cx    = radius
    var cy    = 0f

    if (shapeType.fold(false)(_ == ShapeType.Line)) {
      // TODO: Implement line circle
    } else {
      // TODO: Implement filled circle
    }
  }

  private def check(preferred: ShapeType, other: Nullable[ShapeType], newVertices: Int): Unit = {
    if (shapeType.isEmpty) throw new IllegalStateException("begin must be called first.")
    if (!shapeType.fold(false)(st => st == preferred || other.fold(false)(_ == st))) {
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
    }
    // TODO: Check if we need to flush due to too many vertices
  }

  override def close(): Unit = {
    // TODO: renderer.close()
  }
}

object ShapeRenderer {
  // TODO: Add companion object methods if needed
}
