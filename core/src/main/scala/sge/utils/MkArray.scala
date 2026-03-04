/*
 * Migration notes:
 *   SGE-original file, no LibGDX counterpart
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
/*
 * SGE - Scala Game Engine
 * copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

import scala.reflect.ClassTag

/** Type class that abstracts primitive array creation, avoiding boxing overhead.
  *
  * Provides given instances for all 8 JVM primitives plus AnyRef, ensuring that `DynamicArray[Int]` creates real `int[]` arrays rather than boxed `Object[]`.
  */
trait MkArray[A] {
  def create(size:        Int):                               Array[A]
  def copyOf(source:      Array[A], newLength: Int):          Array[A]
  def copyOfRange(source: Array[A], from:      Int, to: Int): Array[A]
}

object MkArray {

  given mkByte: MkArray[Byte] = new MkArray[Byte] {
    def create(size: Int):                           Array[Byte] = new Array[Byte](size)
    def copyOf(source: Array[Byte], newLength: Int): Array[Byte] = {
      val dest = new Array[Byte](newLength)
      System.arraycopy(source, 0, dest, 0, Math.min(source.length, newLength))
      dest
    }
    def copyOfRange(source: Array[Byte], from: Int, to: Int): Array[Byte] = {
      val len  = to - from
      val dest = new Array[Byte](len)
      System.arraycopy(source, from, dest, 0, len)
      dest
    }
  }

  given mkShort: MkArray[Short] = new MkArray[Short] {
    def create(size: Int):                            Array[Short] = new Array[Short](size)
    def copyOf(source: Array[Short], newLength: Int): Array[Short] = {
      val dest = new Array[Short](newLength)
      System.arraycopy(source, 0, dest, 0, Math.min(source.length, newLength))
      dest
    }
    def copyOfRange(source: Array[Short], from: Int, to: Int): Array[Short] = {
      val len  = to - from
      val dest = new Array[Short](len)
      System.arraycopy(source, from, dest, 0, len)
      dest
    }
  }

  given mkChar: MkArray[Char] = new MkArray[Char] {
    def create(size: Int):                           Array[Char] = new Array[Char](size)
    def copyOf(source: Array[Char], newLength: Int): Array[Char] = {
      val dest = new Array[Char](newLength)
      System.arraycopy(source, 0, dest, 0, Math.min(source.length, newLength))
      dest
    }
    def copyOfRange(source: Array[Char], from: Int, to: Int): Array[Char] = {
      val len  = to - from
      val dest = new Array[Char](len)
      System.arraycopy(source, from, dest, 0, len)
      dest
    }
  }

  given mkInt: MkArray[Int] = new MkArray[Int] {
    def create(size: Int):                          Array[Int] = new Array[Int](size)
    def copyOf(source: Array[Int], newLength: Int): Array[Int] = {
      val dest = new Array[Int](newLength)
      System.arraycopy(source, 0, dest, 0, Math.min(source.length, newLength))
      dest
    }
    def copyOfRange(source: Array[Int], from: Int, to: Int): Array[Int] = {
      val len  = to - from
      val dest = new Array[Int](len)
      System.arraycopy(source, from, dest, 0, len)
      dest
    }
  }

  given mkLong: MkArray[Long] = new MkArray[Long] {
    def create(size: Int):                           Array[Long] = new Array[Long](size)
    def copyOf(source: Array[Long], newLength: Int): Array[Long] = {
      val dest = new Array[Long](newLength)
      System.arraycopy(source, 0, dest, 0, Math.min(source.length, newLength))
      dest
    }
    def copyOfRange(source: Array[Long], from: Int, to: Int): Array[Long] = {
      val len  = to - from
      val dest = new Array[Long](len)
      System.arraycopy(source, from, dest, 0, len)
      dest
    }
  }

  given mkFloat: MkArray[Float] = new MkArray[Float] {
    def create(size: Int):                            Array[Float] = new Array[Float](size)
    def copyOf(source: Array[Float], newLength: Int): Array[Float] = {
      val dest = new Array[Float](newLength)
      System.arraycopy(source, 0, dest, 0, Math.min(source.length, newLength))
      dest
    }
    def copyOfRange(source: Array[Float], from: Int, to: Int): Array[Float] = {
      val len  = to - from
      val dest = new Array[Float](len)
      System.arraycopy(source, from, dest, 0, len)
      dest
    }
  }

  given mkDouble: MkArray[Double] = new MkArray[Double] {
    def create(size: Int):                             Array[Double] = new Array[Double](size)
    def copyOf(source: Array[Double], newLength: Int): Array[Double] = {
      val dest = new Array[Double](newLength)
      System.arraycopy(source, 0, dest, 0, Math.min(source.length, newLength))
      dest
    }
    def copyOfRange(source: Array[Double], from: Int, to: Int): Array[Double] = {
      val len  = to - from
      val dest = new Array[Double](len)
      System.arraycopy(source, from, dest, 0, len)
      dest
    }
  }

  given mkBoolean: MkArray[Boolean] = new MkArray[Boolean] {
    def create(size: Int):                              Array[Boolean] = new Array[Boolean](size)
    def copyOf(source: Array[Boolean], newLength: Int): Array[Boolean] = {
      val dest = new Array[Boolean](newLength)
      System.arraycopy(source, 0, dest, 0, Math.min(source.length, newLength))
      dest
    }
    def copyOfRange(source: Array[Boolean], from: Int, to: Int): Array[Boolean] = {
      val len  = to - from
      val dest = new Array[Boolean](len)
      System.arraycopy(source, from, dest, 0, len)
      dest
    }
  }

  given anyRef[A <: AnyRef: ClassTag]: MkArray[A] = new MkArray[A] {
    def create(size: Int):                        Array[A] = new Array[A](size)
    def copyOf(source: Array[A], newLength: Int): Array[A] = {
      val dest = new Array[A](newLength)
      System.arraycopy(source, 0, dest, 0, Math.min(source.length, newLength))
      dest
    }
    def copyOfRange(source: Array[A], from: Int, to: Int): Array[A] = {
      val len  = to - from
      val dest = new Array[A](len)
      System.arraycopy(source, from, dest, 0, len)
      dest
    }
  }

  /** MkArray instance for Nullable types. At the JVM level, `Nullable[A]` erases to `Object`, so we use `Array[AnyRef]` as the backing store and cast.
    */
  given mkNullable[A]: MkArray[Nullable[A]] = anyRef[AnyRef](using scala.reflect.classTag[AnyRef]).asInstanceOf[MkArray[Nullable[A]]]
}
