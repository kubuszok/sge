/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/MapLayers.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: Array -> DynamicArray; ClassReflection.isInstance -> clazz.isInstance
 *   Convention: implements Iterable -> extends Iterable; for loop -> while loop
 *   Idiom: get(name) returns Nullable[MapLayer] via boundary/break instead of return null
 *   Idiom: getIndex(name) uses Nullable.fold(-1)(getIndex) instead of passing null to getIndex
 *   Idiom: size() method -> override def size (Scala Iterable override)
 *   - getCount/getByType are delegation methods with parameters, not simple getters — kept as-is
 *   Audited: 2026-03-03
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 108
 * Covenant-baseline-methods: MapLayers,add,byType,count,get,getByType,getIndex,i,iterator,layers,n,remove,size
 * Covenant-source-reference: com/badlogic/gdx/maps/MapLayers.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package maps

import scala.util.boundary
import scala.util.boundary.break
import scala.reflect.ClassTag
import sge.utils.{ DynamicArray, MkArray, Nullable }

/** Ordered list of {@link MapLayer} instances owned by a {@link Map} */
class MapLayers extends Iterable[MapLayer] {
  private val layers: DynamicArray[MapLayer] = DynamicArray[MapLayer]()

  /** @param index
    * @return
    *   the MapLayer at the specified index
    */
  def get(index: Int): MapLayer =
    layers(index)

  /** @param name
    * @return
    *   the first layer having the specified name, if one exists, otherwise null
    */
  def get(name: String): Nullable[MapLayer] = boundary {
    var i = 0
    val n = layers.size
    while (i < n) {
      val layer = layers(i)
      if (name == layer.name) {
        break(Nullable(layer))
      }
      i += 1
    }
    Nullable.empty
  }

  /** Get the index of the layer having the specified name, or -1 if no such layer exists. */
  def getIndex(name: String): Int =
    get(name).fold(-1)(getIndex)

  /** Get the index of the layer in the collection, or -1 if no such layer exists. */
  def getIndex(layer: MapLayer): Int =
    layers.indexOf(layer)

  /** @return number of layers in the collection */
  def count: Int = layers.size

  /** @param layer layer to be added to the set */
  def add(layer: MapLayer): Unit =
    layers.add(layer)

  /** @param index removes layer at index */
  def remove(index: Int): Unit =
    layers.removeIndex(index)

  /** @param layer layer to be removed */
  def remove(layer: MapLayer): Unit =
    layers.removeValue(layer)

  /** @return the number of map layers * */
  override def size: Int = layers.size

  /** @param type
    * @return
    *   array with all the layers matching type
    */
  def byType[T <: MapLayer](using tag: ClassTag[T]): DynamicArray[T] =
    getByType(DynamicArray.createWithMk(MkArray.anyRef.asInstanceOf[MkArray[T]], 16, true))

  /** @param fill
    *   array to be filled with the matching layers
    * @return
    *   array with all the layers matching type
    */
  def getByType[T <: MapLayer](fill: DynamicArray[T])(using tag: ClassTag[T]): DynamicArray[T] = {
    fill.clear()
    var i = 0
    val n = layers.size
    while (i < n) {
      val layer = layers(i)
      if (tag.runtimeClass.isInstance(layer)) {
        fill.add(layer.asInstanceOf[T])
      }
      i += 1
    }
    fill
  }

  /** @return iterator to set of layers */
  override def iterator: Iterator[MapLayer] = layers.iterator
}
