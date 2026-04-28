/*
 * Ported from libGDX gdx-ai - https://github.com/libgdx/gdx-ai
 * Original source: com/badlogic/gdx/ai/btree/utils/DistributionAdapters.java
 * Original authors: davebaol
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `com.badlogic.gdx.ai.btree.utils` -> `sge.ai.btree.utils`; `ObjectMap` -> `scala.collection.mutable.HashMap`
 *   Convention: split packages
 *   Idiom: Java class-based registry -> Scala type-safe adapter registry using ClassTag
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 282
 * Covenant-baseline-methods: Adapter,DistributionAdapters,DistributionFormatException,DoubleAdapter,FloatAdapter,IntegerAdapter,LongAdapter,adapter,adapters,add,args,categories,converter,defaultAdapters,i,invalidNumberOfArgumentsException,m,message,parseDouble,parseFloat,parseInteger,parseLong,sb,st,this,toDistribution,toParameters,toString,typeMap,typeName
 * Covenant-source-reference: com/badlogic/gdx/ai/btree/utils/DistributionAdapters.java
 *   Renames: `com.badlogic.gdx.ai.btree.utils` -> `sge.ai.btree.utils`; `ObjectMap` -> `scala.collection.mutable.HashMap`
 *   Convention: split packages
 *   Idiom: Java class-based registry -> Scala type-safe adapter registry using ClassTag
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 282
 * Covenant-baseline-methods: Adapter,DistributionAdapters,DistributionFormatException,DoubleAdapter,FloatAdapter,IntegerAdapter,LongAdapter,adapter,adapters,add,args,categories,converter,defaultAdapters,i,invalidNumberOfArgumentsException,m,message,parseDouble,parseFloat,parseInteger,parseLong,sb,st,this,toDistribution,toParameters,toString,typeMap,typeName
 * Covenant-verified: 2026-04-19
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 6726e345248ddcad7cec0737f6ad83e4e028266d
 */
package sge
package ai
package btree
package utils

import java.util.StringTokenizer
import scala.collection.mutable
import scala.reflect.ClassTag

import sge.ai.utils.random.*

/** Converts between string representations and distribution objects.
  *
  * @author
  *   davebaol (original implementation)
  */
class DistributionAdapters {

  private val adapters: mutable.HashMap[Class[?], DistributionAdapters.Adapter[?]]                          = mutable.HashMap.empty
  private val typeMap:  mutable.HashMap[Class[?], mutable.HashMap[String, DistributionAdapters.Adapter[?]]] =
    mutable.HashMap.empty

  // Register default adapters
  DistributionAdapters.defaultAdapters.foreach { case (clazz, adapter) => add(clazz, adapter) }

  final def add(clazz: Class[?], adapter: DistributionAdapters.Adapter[?]): Unit = {
    adapters.put(clazz, adapter)
    val m = typeMap.getOrElseUpdate(adapter.distributionType, mutable.HashMap.empty)
    m.put(adapter.category, adapter)
  }

  def toDistribution[T <: Distribution](value: String, clazz: Class[T]): T = {
    val st = new StringTokenizer(value, ", \t\f")
    if (!st.hasMoreTokens) throw new DistributionAdapters.DistributionFormatException("Missing distribution type")
    val typeName   = st.nextToken()
    val categories = typeMap.getOrElse(
      clazz,
      throw new DistributionAdapters.DistributionFormatException(
        s"Cannot create a '${clazz.getSimpleName}' of type '$typeName'"
      )
    )
    val converter = categories
      .getOrElse(
        typeName,
        throw new DistributionAdapters.DistributionFormatException(
          s"Cannot create a '${clazz.getSimpleName}' of type '$typeName'"
        )
      )
      .asInstanceOf[DistributionAdapters.Adapter[T]]
    val args = new Array[String](st.countTokens())
    var i    = 0
    while (i < args.length) {
      args(i) = st.nextToken()
      i += 1
    }
    converter.toDistribution(args)
  }

  def toString(distribution: Distribution): String = {
    val adapter = adapters(distribution.getClass).asInstanceOf[DistributionAdapters.Adapter[Distribution]]
    val args    = adapter.toParameters(distribution)
    val sb      = new StringBuilder(adapter.category)
    args.foreach { a =>
      sb.append(',')
      sb.append(a)
    }
    sb.toString()
  }
}

object DistributionAdapters {

  /** Thrown to indicate that the application has attempted to convert a string to one of the distribution types, but that the string does not have the appropriate format.
    */
  class DistributionFormatException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
    def this() = this(null, null) // @nowarn — null required for Java interop (RuntimeException constructor)
    def this(message: String) = this(message, null) // @nowarn — null required for Java interop (RuntimeException constructor)
    def this(cause:   Throwable) = this(if (cause != null) cause.toString else null, cause) // @nowarn — null required for Java interop
  }

  /** Base adapter for converting between distributions and string representations. */
  abstract class Adapter[D <: Distribution](val category: String, val distributionType: Class[?]) {
    def toDistribution(args:       Array[String]): D
    def toParameters(distribution: D):             Array[String]

    protected def parseDouble(v: String): Double =
      try v.toDouble
      catch { case nfe: NumberFormatException => throw new DistributionFormatException("Not a double value: " + v, nfe) }

    protected def parseFloat(v: String): Float =
      try v.toFloat
      catch { case nfe: NumberFormatException => throw new DistributionFormatException("Not a float value: " + v, nfe) }

    protected def parseInteger(v: String): Int =
      try v.toInt
      catch { case nfe: NumberFormatException => throw new DistributionFormatException("Not an int value: " + v, nfe) }

    protected def parseLong(v: String): Long =
      try v.toLong
      catch { case nfe: NumberFormatException => throw new DistributionFormatException("Not a long value: " + v, nfe) }
  }

  /** Convenience adapter base for [[DoubleDistribution]] types. */
  abstract class DoubleAdapter[D <: DoubleDistribution](category: String) extends Adapter[D](category, classOf[DoubleDistribution])

  /** Convenience adapter base for [[FloatDistribution]] types. */
  abstract class FloatAdapter[D <: FloatDistribution](category: String) extends Adapter[D](category, classOf[FloatDistribution])

  /** Convenience adapter base for [[IntegerDistribution]] types. */
  abstract class IntegerAdapter[D <: IntegerDistribution](category: String) extends Adapter[D](category, classOf[IntegerDistribution])

  /** Convenience adapter base for [[LongDistribution]] types. */
  abstract class LongAdapter[D <: LongDistribution](category: String) extends Adapter[D](category, classOf[LongDistribution])

  private def invalidNumberOfArgumentsException(found: Int, expected: Int*): DistributionFormatException = {
    var message = "Found " + found + " arguments in triangular distribution; expected "
    if (expected.length < 2) {
      message += expected.length
    } else {
      var sep = ""
      var i   = 0
      while (i < expected.length - 1) {
        message += sep + expected(i)
        sep = ", "
        i += 1
      }
      message += " or " + expected(i)
    }
    new DistributionFormatException(message)
  }

  // ── Default adapter registry ──────────────────────────────────────────

  private val defaultAdapters: Seq[(Class[?], Adapter[?])] = Seq(
    //
    // Constant distributions
    //

    classOf[ConstantDoubleDistribution] -> new DoubleAdapter[ConstantDoubleDistribution]("constant") {
      def toDistribution(args: Array[String]): ConstantDoubleDistribution = {
        if (args.length != 1) throw invalidNumberOfArgumentsException(args.length, 1)
        new ConstantDoubleDistribution(parseDouble(args(0)))
      }
      def toParameters(d: ConstantDoubleDistribution): Array[String] = Array(d.value.toString)
    },
    classOf[ConstantFloatDistribution] -> new FloatAdapter[ConstantFloatDistribution]("constant") {
      def toDistribution(args: Array[String]): ConstantFloatDistribution = {
        if (args.length != 1) throw invalidNumberOfArgumentsException(args.length, 1)
        new ConstantFloatDistribution(parseFloat(args(0)))
      }
      def toParameters(d: ConstantFloatDistribution): Array[String] = Array(d.value.toString)
    },
    classOf[ConstantIntegerDistribution] -> new IntegerAdapter[ConstantIntegerDistribution]("constant") {
      def toDistribution(args: Array[String]): ConstantIntegerDistribution = {
        if (args.length != 1) throw invalidNumberOfArgumentsException(args.length, 1)
        new ConstantIntegerDistribution(parseInteger(args(0)))
      }
      def toParameters(d: ConstantIntegerDistribution): Array[String] = Array(d.value.toString)
    },
    classOf[ConstantLongDistribution] -> new LongAdapter[ConstantLongDistribution]("constant") {
      def toDistribution(args: Array[String]): ConstantLongDistribution = {
        if (args.length != 1) throw invalidNumberOfArgumentsException(args.length, 1)
        new ConstantLongDistribution(parseLong(args(0)))
      }
      def toParameters(d: ConstantLongDistribution): Array[String] = Array(d.value.toString)
    },

    //
    // Gaussian distributions
    //

    classOf[GaussianDoubleDistribution] -> new DoubleAdapter[GaussianDoubleDistribution]("gaussian") {
      def toDistribution(args: Array[String]): GaussianDoubleDistribution = {
        if (args.length != 2) throw invalidNumberOfArgumentsException(args.length, 2)
        new GaussianDoubleDistribution(parseDouble(args(0)), parseDouble(args(1)))
      }
      def toParameters(d: GaussianDoubleDistribution): Array[String] = Array(d.mean.toString, d.standardDeviation.toString)
    },
    classOf[GaussianFloatDistribution] -> new FloatAdapter[GaussianFloatDistribution]("gaussian") {
      def toDistribution(args: Array[String]): GaussianFloatDistribution = {
        if (args.length != 2) throw invalidNumberOfArgumentsException(args.length, 2)
        new GaussianFloatDistribution(parseFloat(args(0)), parseFloat(args(1)))
      }
      def toParameters(d: GaussianFloatDistribution): Array[String] = Array(d.mean.toString, d.standardDeviation.toString)
    },

    //
    // Triangular distributions
    //

    classOf[TriangularDoubleDistribution] -> new DoubleAdapter[TriangularDoubleDistribution]("triangular") {
      def toDistribution(args: Array[String]): TriangularDoubleDistribution = args.length match {
        case 1 => new TriangularDoubleDistribution(parseDouble(args(0)))
        case 2 => new TriangularDoubleDistribution(parseDouble(args(0)), parseDouble(args(1)))
        case 3 => new TriangularDoubleDistribution(parseDouble(args(0)), parseDouble(args(1)), parseDouble(args(2)))
        case n => throw invalidNumberOfArgumentsException(n, 1, 2, 3)
      }
      def toParameters(d: TriangularDoubleDistribution): Array[String] =
        Array(d.low.toString, d.high.toString, d.mode.toString)
    },
    classOf[TriangularFloatDistribution] -> new FloatAdapter[TriangularFloatDistribution]("triangular") {
      def toDistribution(args: Array[String]): TriangularFloatDistribution = args.length match {
        case 1 => new TriangularFloatDistribution(parseFloat(args(0)))
        case 2 => new TriangularFloatDistribution(parseFloat(args(0)), parseFloat(args(1)))
        case 3 => new TriangularFloatDistribution(parseFloat(args(0)), parseFloat(args(1)), parseFloat(args(2)))
        case n => throw invalidNumberOfArgumentsException(n, 1, 2, 3)
      }
      def toParameters(d: TriangularFloatDistribution): Array[String] =
        Array(d.low.toString, d.high.toString, d.mode.toString)
    },
    classOf[TriangularIntegerDistribution] -> new IntegerAdapter[TriangularIntegerDistribution]("triangular") {
      def toDistribution(args: Array[String]): TriangularIntegerDistribution = args.length match {
        case 1 => new TriangularIntegerDistribution(parseInteger(args(0)))
        case 2 => new TriangularIntegerDistribution(parseInteger(args(0)), parseInteger(args(1)))
        case 3 => new TriangularIntegerDistribution(parseInteger(args(0)), parseInteger(args(1)), java.lang.Float.valueOf(args(2)))
        case n => throw invalidNumberOfArgumentsException(n, 1, 2, 3)
      }
      def toParameters(d: TriangularIntegerDistribution): Array[String] =
        Array(d.low.toString, d.high.toString, d.mode.toString)
    },
    classOf[TriangularLongDistribution] -> new LongAdapter[TriangularLongDistribution]("triangular") {
      def toDistribution(args: Array[String]): TriangularLongDistribution = args.length match {
        case 1 => new TriangularLongDistribution(parseLong(args(0)))
        case 2 => new TriangularLongDistribution(parseLong(args(0)), parseLong(args(1)))
        case 3 => new TriangularLongDistribution(parseLong(args(0)), parseLong(args(1)), parseDouble(args(2)))
        case n => throw invalidNumberOfArgumentsException(n, 1, 2, 3)
      }
      def toParameters(d: TriangularLongDistribution): Array[String] =
        Array(d.low.toString, d.high.toString, d.mode.toString)
    },

    //
    // Uniform distributions
    //

    classOf[UniformDoubleDistribution] -> new DoubleAdapter[UniformDoubleDistribution]("uniform") {
      def toDistribution(args: Array[String]): UniformDoubleDistribution = args.length match {
        case 1 => new UniformDoubleDistribution(parseDouble(args(0)))
        case 2 => new UniformDoubleDistribution(parseDouble(args(0)), parseDouble(args(1)))
        case n => throw invalidNumberOfArgumentsException(n, 1, 2)
      }
      def toParameters(d: UniformDoubleDistribution): Array[String] = Array(d.low.toString, d.high.toString)
    },
    classOf[UniformFloatDistribution] -> new FloatAdapter[UniformFloatDistribution]("uniform") {
      def toDistribution(args: Array[String]): UniformFloatDistribution = args.length match {
        case 1 => new UniformFloatDistribution(parseFloat(args(0)))
        case 2 => new UniformFloatDistribution(parseFloat(args(0)), parseFloat(args(1)))
        case n => throw invalidNumberOfArgumentsException(n, 1, 2)
      }
      def toParameters(d: UniformFloatDistribution): Array[String] = Array(d.low.toString, d.high.toString)
    },
    classOf[UniformIntegerDistribution] -> new IntegerAdapter[UniformIntegerDistribution]("uniform") {
      def toDistribution(args: Array[String]): UniformIntegerDistribution = args.length match {
        case 1 => new UniformIntegerDistribution(parseInteger(args(0)))
        case 2 => new UniformIntegerDistribution(parseInteger(args(0)), parseInteger(args(1)))
        case n => throw invalidNumberOfArgumentsException(n, 1, 2)
      }
      def toParameters(d: UniformIntegerDistribution): Array[String] = Array(d.low.toString, d.high.toString)
    },
    classOf[UniformLongDistribution] -> new LongAdapter[UniformLongDistribution]("uniform") {
      def toDistribution(args: Array[String]): UniformLongDistribution = args.length match {
        case 1 => new UniformLongDistribution(parseLong(args(0)))
        case 2 => new UniformLongDistribution(parseLong(args(0)), parseLong(args(1)))
        case n => throw invalidNumberOfArgumentsException(n, 1, 2)
      }
      def toParameters(d: UniformLongDistribution): Array[String] = Array(d.low.toString, d.high.toString)
    }
  )
}
