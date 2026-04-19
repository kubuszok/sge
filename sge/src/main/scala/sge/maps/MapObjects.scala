/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/MapObjects.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Array -> DynamicArray; ClassReflection.isInstance -> clazz.isInstance
 *   Convention: implements Iterable -> extends Iterable; for loop -> while loop
 *   Idiom: get(name) returns Nullable[MapObject] via boundary/break instead of return null
 *   Idiom: getIndex(name) uses Nullable.fold(-1)(getIndex) instead of passing null to getIndex
 *   - getCount/getByType are delegation methods with parameters, not simple getters — kept as-is
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 106
 * Covenant-baseline-methods: MapObjects,add,byType,count,get,getByType,getIndex,i,iterator,n,objects,remove
 * Covenant-source-reference: com/badlogic/gdx/maps/MapObjects.java
 * Covenant-verified: 2026-04-19
 */
package sge
package maps

import scala.util.boundary
import scala.util.boundary.break
import scala.reflect.ClassTag
import sge.utils.{ DynamicArray, MkArray, Nullable }

/** @brief Collection of MapObject instances */
class MapObjects extends Iterable[MapObject] {

  private val objects: DynamicArray[MapObject] = DynamicArray[MapObject]()

  /** @param index
    * @return
    *   the MapObject at the specified index
    */
  def get(index: Int): MapObject =
    objects(index)

  /** @param name
    * @return
    *   the first object having the specified name, if one exists, otherwise null
    */
  def get(name: String): Nullable[MapObject] = boundary {
    var i = 0
    val n = objects.size
    while (i < n) {
      val obj = objects(i)
      if (name == obj.name) {
        break(Nullable(obj))
      }
      i += 1
    }
    Nullable.empty
  }

  /** Get the index of the object having the specified name, or -1 if no such object exists. */
  def getIndex(name: String): Int =
    get(name).fold(-1)(getIndex)

  /** Get the index of the object in the collection, or -1 if no such object exists. */
  def getIndex(obj: MapObject): Int =
    objects.indexOf(obj)

  /** @return number of objects in the collection */
  def count: Int = objects.size

  /** @param object instance to be added to the collection */
  def add(obj: MapObject): Unit =
    objects.add(obj)

  /** @param index removes MapObject instance at index */
  def remove(index: Int): Unit =
    objects.removeIndex(index)

  /** @param object instance to be removed */
  def remove(obj: MapObject): Unit =
    objects.removeValue(obj)

  /** @param type
    *   class of the objects we want to retrieve
    * @return
    *   array filled with all the objects in the collection matching type
    */
  def byType[T <: MapObject](using tag: ClassTag[T]): DynamicArray[T] =
    getByType(DynamicArray.createWithMk(MkArray.anyRef.asInstanceOf[MkArray[T]], 16, true))

  /** @param fill
    *   collection to put the returned objects in
    * @return
    *   array filled with all the objects in the collection matching type
    */
  def getByType[T <: MapObject](fill: DynamicArray[T])(using tag: ClassTag[T]): DynamicArray[T] = {
    fill.clear()
    var i = 0
    val n = objects.size
    while (i < n) {
      val obj = objects(i)
      if (tag.runtimeClass.isInstance(obj)) {
        fill.add(obj.asInstanceOf[T])
      }
      i += 1
    }
    fill
  }

  /** @return iterator for the objects within the collection */
  override def iterator: Iterator[MapObject] = objects.iterator
}
