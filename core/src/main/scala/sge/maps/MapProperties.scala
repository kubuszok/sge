/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/maps/MapProperties.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 *   Renames: ObjectMap -> mutable.HashMap; Object -> Any
 *   Convention: containsKey uses .contains (Scala Map API)
 *   Idiom: get(key) returns null via getOrElse(key, null) -- raw null at Java-interop boundary
 *   Idiom: get(key, defaultValue, clazz) uses Nullable(obj).fold instead of == null check
 *   Idiom: equals uses pattern match instead of instanceof
 *   Idiom: toString uses string interpolation
 *   TODO: Java-style getters/setters — getKeys, getValues
 *   Audited: 2026-03-03
 */
package sge
package maps

import scala.collection.mutable
import sge.utils.Nullable

/** @brief
  *   Set of string indexed values representing map elements' properties, allowing to retrieve, modify and add properties to the set.
  */
class MapProperties {

  private val properties: mutable.HashMap[String, Any] = mutable.HashMap.empty

  /** @param key
    *   property name
    * @return
    *   true if and only if the property exists
    */
  def containsKey(key: String): Boolean =
    properties.contains(key)

  /** @param key
    *   property name
    * @return
    *   the value for that property if it exists, otherwise, null
    */
  def get(key: String): Any =
    properties.getOrElse(key, null)

  /** Returns the object for the given key, casting it to clazz.
    * @param key
    *   the key of the object
    * @param clazz
    *   the class of the object
    * @return
    *   the object or null if the object is not in the map
    * @throws ClassCastException
    *   if the object with the given key is not of type clazz
    */
  def get[T](key: String, clazz: Class[T]): T =
    get(key).asInstanceOf[T]

  /** Returns the object for the given key, casting it to clazz.
    * @param key
    *   the key of the object
    * @param defaultValue
    *   the default value
    * @param clazz
    *   the class of the object
    * @return
    *   the object or the defaultValue if the object is not in the map
    * @throws ClassCastException
    *   if the object with the given key is not of type clazz
    */
  def get[T](key: String, defaultValue: T, clazz: Class[T]): T = {
    val obj = get(key)
    Nullable(obj).fold(defaultValue)(_.asInstanceOf[T])
  }

  /** @param key
    *   property name
    * @param value
    *   value to be inserted or modified (if it already existed)
    */
  def put(key: String, value: Any): Unit =
    properties.put(key, value)

  /** @param properties set of properties to be added */
  def putAll(other: MapProperties): Unit =
    properties.addAll(other.properties)

  /** @param key property name to be removed */
  def remove(key: String): Unit =
    properties.remove(key)

  /** Removes all properties */
  def clear(): Unit =
    properties.clear()

  /** @return iterator for the property names */
  def getKeys: Iterator[String] =
    properties.keysIterator

  /** @return iterator to properties' values */
  def getValues: Iterator[Any] =
    properties.valuesIterator

  override def toString: String =
    s"MapProperties{properties=$properties}"

  override def equals(o: Any): Boolean = o match {
    case that: MapProperties => properties == that.properties
    case _ => false
  }

  override def hashCode(): Int =
    properties.hashCode()
}
