package sge
package math

import sge.utils.DynamicArray

class BezierTest extends munit.FunSuite {

  private val epsilon               = Float.MinPositiveValue
  private val epsilonApproximations = 1e-6f

  sealed private trait ImportType
  private object ImportType {
    case object DynamicArrays extends ImportType
    case object ScalaArrays extends ImportType
    case object VarArgs extends ImportType
    val values: List[ImportType] = List(DynamicArrays, ScalaArrays, VarArgs)
  }

  private def createBezier(points: Array[Vector2], importType: ImportType, useSetter: Boolean): Bezier[Vector2] =
    if (useSetter) {
      val bezier = new Bezier[Vector2]()
      importType match {
        case ImportType.DynamicArrays =>
          val da = DynamicArray[Vector2]()
          points.foreach(da.add)
          bezier.set(da, 0, points.length)
        case ImportType.ScalaArrays =>
          bezier.set(points, 0, points.length)
        case ImportType.VarArgs =>
          bezier.set(points*)
      }
      bezier
    } else {
      importType match {
        case ImportType.DynamicArrays =>
          val da = DynamicArray[Vector2]()
          points.foreach(da.add)
          new Bezier[Vector2](da, 0, points.length)
        case ImportType.ScalaArrays =>
          new Bezier[Vector2](points, 0, points.length)
        case ImportType.VarArgs =>
          new Bezier[Vector2](points*)
      }
    }

  for {
    importType <- ImportType.values
    useSetter <- List(true, false)
  } {
    val label = s"imported type $importType use setter $useSetter"

    test(s"linear2D ($label)") {
      val points = Array(new Vector2(0, 0), new Vector2(1, 1))
      val bezier = createBezier(points, importType, useSetter)

      val len = bezier.approxLength(2)
      assertEqualsDouble(len.toDouble, Math.sqrt(2), epsilonApproximations.toDouble)

      val d = bezier.derivativeAt(new Vector2(), 0.5f)
      assertEqualsDouble(d.x.toDouble, 1.0, epsilon.toDouble)
      assertEqualsDouble(d.y.toDouble, 1.0, epsilon.toDouble)

      val v = bezier.valueAt(new Vector2(), 0.5f)
      assertEqualsDouble(v.x.toDouble, 0.5, epsilon.toDouble)
      assertEqualsDouble(v.y.toDouble, 0.5, epsilon.toDouble)

      val t = bezier.approximate(new Vector2(.5f, .5f))
      assertEqualsDouble(t.toDouble, 0.5, epsilonApproximations.toDouble)

      val l = bezier.locate(new Vector2(.5f, .5f))
      assertEqualsDouble(l.toDouble, 0.5, epsilon.toDouble)
    }
  }
}
