/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/utils/JsonValue.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: `@Null` -> `Nullable`; `ValueType` inner class preserved as enum
 *   Convention: `return` -> `boundary`/`break`; implements `Iterable[JsonValue]`
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *   TODO: Remove once all consumers migrate to jsoniter-scala codec derivation (see docs/improvements/dependencies.md B5).
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package utils

import java.io.{ IOException, StringWriter, Writer }
import java.util.NoSuchElementException
import scala.util.boundary
import scala.util.boundary.break

/** Container for a JSON object, array, string, double, long, boolean, or null.
  *
  * JsonValue children are a linked list. Iteration of arrays or objects is easily done using a for-each or the `next` field, both shown below. This is much more efficient than accessing children by
  * index when there are many children.
  *
  * {{{
  * val map: JsonValue = ...
  * // Allocates an iterator:
  * for (entry <- map)
  *   println(entry.name + " = " + entry.asString())
  * // No allocation:
  * var entry = map.child
  * while (entry != null) { println(entry.name + " = " + entry.asString()); entry = entry.next }
  * }}}
  */
class JsonValue(private var _type: JsonValue.ValueType) extends Iterable[JsonValue] {
  import JsonValue.ValueType

  private var stringValue: Nullable[String] = Nullable.empty
  private var doubleValue: Double           = 0
  private var longValue:   Long             = 0

  var name:      Nullable[String]    = Nullable.empty
  var child:     Nullable[JsonValue] = Nullable.empty
  var lastChild: Nullable[JsonValue] = Nullable.empty
  var parent:    Nullable[JsonValue] = Nullable.empty

  /** When changing this field the parent `size` may need to be changed. */
  var next:          Nullable[JsonValue] = Nullable.empty
  var prev:          Nullable[JsonValue] = Nullable.empty
  var childCount:    Int                 = 0
  override def size: Int                 = childCount

  /** Creates a value with the given string, or nullValue type if the string is null. */
  def this(value: Nullable[String]) = {
    this(JsonValue.ValueType.nullValue)
    set(value)
  }

  def this(value: Double) = {
    this(JsonValue.ValueType.doubleValue)
    set(value, Nullable.empty)
  }

  def this(value: Long) = {
    this(JsonValue.ValueType.longValue)
    set(value, Nullable.empty)
  }

  def this(value: Double, stringValue: Nullable[String]) = {
    this(JsonValue.ValueType.doubleValue)
    set(value, stringValue)
  }

  def this(value: Long, stringValue: Nullable[String]) = {
    this(JsonValue.ValueType.longValue)
    set(value, stringValue)
  }

  def this(value: Boolean) = {
    this(JsonValue.ValueType.booleanValue)
    set(value)
  }

  // -- Navigation --

  /** Returns the child at the specified index. This requires walking the linked list to the specified entry. */
  def get(index: Int): Nullable[JsonValue] =
    if (index == size - 1) lastChild
    else {
      var current = child
      var i       = index
      while (current.isDefined && i > 0) {
        i -= 1
        current = current.flatMap(_.next)
      }
      current
    }

  /** Returns the child with the specified name. */
  def get(name: String): Nullable[JsonValue] = {
    var current = child
    while (current.isDefined && current.exists(c => c.name.isEmpty || c.name.forall(_ != name)))
      current = current.flatMap(_.next)
    current
  }

  /** Returns the child with the specified name, ignoring case. */
  def getIgnoreCase(name: String): Nullable[JsonValue] = {
    var current = child
    while (current.isDefined && current.exists(c => c.name.isEmpty || c.name.forall(!_.equalsIgnoreCase(name))))
      current = current.flatMap(_.next)
    current
  }

  /** Returns true if a child with the specified name exists. */
  def has(name: String): Boolean = get(name).isDefined

  /** Returns the child at the specified index.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def require(index: Int): JsonValue = {
    val current = get(index)
    current.getOrElse(throw new IllegalArgumentException("Child not found with index: " + index))
  }

  /** Returns the child with the specified name.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def require(name: String): JsonValue = {
    val current = get(name)
    current.getOrElse(throw new IllegalArgumentException("Child not found with name: " + name))
  }

  /** Removes the child with the specified index. */
  def remove(index: Int): Nullable[JsonValue] = {
    val c = get(index)
    c.foreach(removeChild)
    c
  }

  /** Removes the child with the specified name. */
  def remove(name: String): Nullable[JsonValue] = {
    val c = get(name)
    c.foreach(removeChild)
    c
  }

  /** Removes this value from its parent. */
  def removeFromParent(): Unit = {
    val p = parent.getOrElse(throw new IllegalStateException())
    if (p.lastChild.exists(_ eq this)) p.lastChild = prev
    if (prev.isEmpty) {
      p.child = next
      p.child.foreach(_.prev = Nullable.empty)
    } else {
      prev.foreach(_.next = next)
      next.foreach(_.prev = prev)
    }
    p.childCount -= 1
  }

  private def removeChild(c: JsonValue): Unit = {
    if (lastChild.exists(_ eq c)) lastChild = c.prev
    if (c.prev.isEmpty) {
      child = c.next
      child.foreach(_.prev = Nullable.empty)
    } else {
      c.prev.foreach(_.next = c.next)
      c.next.foreach(_.prev = c.prev)
    }
    childCount -= 1
  }

  /** Returns true if there are one or more children in the array or object. */
  def notEmpty: Boolean = size > 0

  /** Returns true if there are no children in the array or object. */
  override def isEmpty: Boolean = size == 0

  /** Unwraps stringValue, assuming it is defined. Only call when `_type` guarantees a string is present. */
  private def stringValueGet: String = stringValue.getOrElse(throw new IllegalStateException("Missing string value"))

  // -- Value conversion (as*) --

  /** Returns this value as a string.
    * @throws IllegalStateException
    *   if this is an array or object.
    */
  def asString(): Nullable[String] = _type match {
    case ValueType.stringValue  => stringValue
    case ValueType.doubleValue  => if (stringValue.isDefined) stringValue else Nullable(doubleValue.toString)
    case ValueType.longValue    => if (stringValue.isDefined) stringValue else Nullable(longValue.toString)
    case ValueType.booleanValue => Nullable(if (longValue != 0) "true" else "false")
    case ValueType.nullValue    => Nullable.empty
    case _                      => throw new IllegalStateException("Value cannot be converted to string: " + _type)
  }

  /** Returns this value as a float.
    * @throws IllegalStateException
    *   if this is an array or object.
    */
  def asFloat(): Float = _type match {
    case ValueType.stringValue  => java.lang.Float.parseFloat(stringValueGet)
    case ValueType.doubleValue  => doubleValue.toFloat
    case ValueType.longValue    => longValue.toFloat
    case ValueType.booleanValue => if (longValue != 0) 1f else 0f
    case _                      => throw new IllegalStateException("Value cannot be converted to float: " + _type)
  }

  /** Returns this value as a double.
    * @throws IllegalStateException
    *   if this is an array or object.
    */
  def asDouble(): Double = _type match {
    case ValueType.stringValue  => java.lang.Double.parseDouble(stringValueGet)
    case ValueType.doubleValue  => doubleValue
    case ValueType.longValue    => longValue.toDouble
    case ValueType.booleanValue => if (longValue != 0) 1.0 else 0.0
    case _                      => throw new IllegalStateException("Value cannot be converted to double: " + _type)
  }

  /** Returns this value as a long.
    * @throws IllegalStateException
    *   if this is an array or object.
    */
  def asLong(): Long = _type match {
    case ValueType.stringValue  => java.lang.Long.parseLong(stringValueGet)
    case ValueType.doubleValue  => doubleValue.toLong
    case ValueType.longValue    => longValue
    case ValueType.booleanValue => if (longValue != 0) 1L else 0L
    case _                      => throw new IllegalStateException("Value cannot be converted to long: " + _type)
  }

  /** Returns this value as an int.
    * @throws IllegalStateException
    *   if this is an array or object.
    */
  def asInt(): Int = _type match {
    case ValueType.stringValue  => java.lang.Integer.parseInt(stringValueGet)
    case ValueType.doubleValue  => doubleValue.toInt
    case ValueType.longValue    => longValue.toInt
    case ValueType.booleanValue => if (longValue != 0) 1 else 0
    case _                      => throw new IllegalStateException("Value cannot be converted to int: " + _type)
  }

  /** Returns this value as a boolean.
    * @throws IllegalStateException
    *   if this is an array or object.
    */
  def asBoolean(): Boolean = _type match {
    case ValueType.stringValue  => stringValue.exists(_.equalsIgnoreCase("true"))
    case ValueType.doubleValue  => doubleValue != 0
    case ValueType.longValue    => longValue != 0
    case ValueType.booleanValue => longValue != 0
    case _                      => throw new IllegalStateException("Value cannot be converted to boolean: " + _type)
  }

  /** Returns this value as a byte.
    * @throws IllegalStateException
    *   if this is an array or object.
    */
  def asByte(): Byte = _type match {
    case ValueType.stringValue  => java.lang.Byte.parseByte(stringValueGet)
    case ValueType.doubleValue  => doubleValue.toByte
    case ValueType.longValue    => longValue.toByte
    case ValueType.booleanValue => if (longValue != 0) 1.toByte else 0.toByte
    case _                      => throw new IllegalStateException("Value cannot be converted to byte: " + _type)
  }

  /** Returns this value as a short.
    * @throws IllegalStateException
    *   if this is an array or object.
    */
  def asShort(): Short = _type match {
    case ValueType.stringValue  => java.lang.Short.parseShort(stringValueGet)
    case ValueType.doubleValue  => doubleValue.toShort
    case ValueType.longValue    => longValue.toShort
    case ValueType.booleanValue => if (longValue != 0) 1.toShort else 0.toShort
    case _                      => throw new IllegalStateException("Value cannot be converted to short: " + _type)
  }

  /** Returns this value as a char.
    * @throws IllegalStateException
    *   if this is an array or object.
    */
  def asChar(): Char = _type match {
    case ValueType.stringValue  => stringValue.map(s => if (s.isEmpty) 0.toChar else s.charAt(0)).getOrElse(0.toChar)
    case ValueType.doubleValue  => doubleValue.toChar
    case ValueType.longValue    => longValue.toChar
    case ValueType.booleanValue => if (longValue != 0) 1.toChar else 0.toChar
    case _                      => throw new IllegalStateException("Value cannot be converted to char: " + _type)
  }

  // -- Array conversion (as*Array) --

  private def requireArray(): Unit =
    if (_type != ValueType.array) throw new IllegalStateException("Value is not an array: " + _type)

  /** Returns the children of this value as a newly allocated String array.
    * @throws IllegalStateException
    *   if this is not an array.
    */
  def asStringArray(): Array[String] = {
    requireArray()
    val array   = new Array[String](size)
    var i       = 0
    var current = child
    while (current.isDefined)
      current.foreach { value =>
        array(i) = value.asString().getOrElse("")
        i += 1
        current = value.next
      }
    array
  }

  /** Returns the children of this value as a newly allocated float array.
    * @throws IllegalStateException
    *   if this is not an array.
    */
  def asFloatArray(): Array[Float] = {
    requireArray()
    val array   = new Array[Float](size)
    var i       = 0
    var current = child
    while (current.isDefined)
      current.foreach { value =>
        array(i) = value.asFloat()
        i += 1
        current = value.next
      }
    array
  }

  /** Returns the children of this value as a newly allocated double array.
    * @throws IllegalStateException
    *   if this is not an array.
    */
  def asDoubleArray(): Array[Double] = {
    requireArray()
    val array   = new Array[Double](size)
    var i       = 0
    var current = child
    while (current.isDefined)
      current.foreach { value =>
        array(i) = value.asDouble()
        i += 1
        current = value.next
      }
    array
  }

  /** Returns the children of this value as a newly allocated long array.
    * @throws IllegalStateException
    *   if this is not an array.
    */
  def asLongArray(): Array[Long] = {
    requireArray()
    val array   = new Array[Long](size)
    var i       = 0
    var current = child
    while (current.isDefined)
      current.foreach { value =>
        array(i) = value.asLong()
        i += 1
        current = value.next
      }
    array
  }

  /** Returns the children of this value as a newly allocated int array.
    * @throws IllegalStateException
    *   if this is not an array.
    */
  def asIntArray(): Array[Int] = {
    requireArray()
    val array   = new Array[Int](size)
    var i       = 0
    var current = child
    while (current.isDefined)
      current.foreach { value =>
        array(i) = value.asInt()
        i += 1
        current = value.next
      }
    array
  }

  /** Returns the children of this value as a newly allocated boolean array.
    * @throws IllegalStateException
    *   if this is not an array.
    */
  def asBooleanArray(): Array[Boolean] = {
    requireArray()
    val array   = new Array[Boolean](size)
    var i       = 0
    var current = child
    while (current.isDefined)
      current.foreach { value =>
        array(i) = value.asBoolean()
        i += 1
        current = value.next
      }
    array
  }

  /** Returns the children of this value as a newly allocated byte array.
    * @throws IllegalStateException
    *   if this is not an array.
    */
  def asByteArray(): Array[Byte] = {
    requireArray()
    val array   = new Array[Byte](size)
    var i       = 0
    var current = child
    while (current.isDefined)
      current.foreach { value =>
        array(i) = value.asByte()
        i += 1
        current = value.next
      }
    array
  }

  /** Returns the children of this value as a newly allocated short array.
    * @throws IllegalStateException
    *   if this is not an array.
    */
  def asShortArray(): Array[Short] = {
    requireArray()
    val array   = new Array[Short](size)
    var i       = 0
    var current = child
    while (current.isDefined)
      current.foreach { value =>
        array(i) = value.asShort()
        i += 1
        current = value.next
      }
    array
  }

  /** Returns the children of this value as a newly allocated char array.
    * @throws IllegalStateException
    *   if this is not an array.
    */
  def asCharArray(): Array[Char] = {
    requireArray()
    val array   = new Array[Char](size)
    var i       = 0
    var current = child
    while (current.isDefined)
      current.foreach { value =>
        array(i) = value.asChar()
        i += 1
        current = value.next
      }
    array
  }

  // -- Named child getters (with default) --

  /** Returns true if a child with the specified name exists and has a child. */
  def hasChild(name: String): Boolean = getChild(name).isDefined

  /** Finds the child with the specified name and returns its first child. */
  def getChild(name: String): Nullable[JsonValue] =
    get(name).flatMap(_.child)

  /** Finds the child with the specified name and returns it as a string. Returns defaultValue if not found. */
  def getString(name: String, defaultValue: Nullable[String]): Nullable[String] =
    get(name).fold(defaultValue)(v => if (!v.isValue || v.isNull) defaultValue else v.asString())

  /** Finds the child with the specified name and returns it as a float. Returns defaultValue if not found. */
  def getFloat(name: String, defaultValue: Float): Float =
    get(name).fold(defaultValue)(v => if (!v.isValue || v.isNull) defaultValue else v.asFloat())

  /** Finds the child with the specified name and returns it as a double. Returns defaultValue if not found. */
  def getDouble(name: String, defaultValue: Double): Double =
    get(name).fold(defaultValue)(v => if (!v.isValue || v.isNull) defaultValue else v.asDouble())

  /** Finds the child with the specified name and returns it as a long. Returns defaultValue if not found. */
  def getLong(name: String, defaultValue: Long): Long =
    get(name).fold(defaultValue)(v => if (!v.isValue || v.isNull) defaultValue else v.asLong())

  /** Finds the child with the specified name and returns it as an int. Returns defaultValue if not found. */
  def getInt(name: String, defaultValue: Int): Int =
    get(name).fold(defaultValue)(v => if (!v.isValue || v.isNull) defaultValue else v.asInt())

  /** Finds the child with the specified name and returns it as a boolean. Returns defaultValue if not found. */
  def getBoolean(name: String, defaultValue: Boolean): Boolean =
    get(name).fold(defaultValue)(v => if (!v.isValue || v.isNull) defaultValue else v.asBoolean())

  /** Finds the child with the specified name and returns it as a byte. Returns defaultValue if not found. */
  def getByte(name: String, defaultValue: Byte): Byte =
    get(name).fold(defaultValue)(v => if (!v.isValue || v.isNull) defaultValue else v.asByte())

  /** Finds the child with the specified name and returns it as a short. Returns defaultValue if not found. */
  def getShort(name: String, defaultValue: Short): Short =
    get(name).fold(defaultValue)(v => if (!v.isValue || v.isNull) defaultValue else v.asShort())

  /** Finds the child with the specified name and returns it as a char. Returns defaultValue if not found. */
  def getChar(name: String, defaultValue: Char): Char =
    get(name).fold(defaultValue)(v => if (!v.isValue || v.isNull) defaultValue else v.asChar())

  // -- Named child getters (required) --

  private def requireChild(name: String): JsonValue =
    get(name).getOrElse(throw new IllegalArgumentException("Named value not found: " + name))

  private def requireChild(index: Int): JsonValue =
    get(index).getOrElse(throw new IllegalArgumentException("Indexed value not found: " + name))

  /** Finds the child with the specified name and returns it as a string.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getString(name: String): Nullable[String] = requireChild(name).asString()

  /** Finds the child with the specified name and returns it as a float.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getFloat(name: String): Float = requireChild(name).asFloat()

  /** Finds the child with the specified name and returns it as a double.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getDouble(name: String): Double = requireChild(name).asDouble()

  /** Finds the child with the specified name and returns it as a long.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getLong(name: String): Long = requireChild(name).asLong()

  /** Finds the child with the specified name and returns it as an int.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getInt(name: String): Int = requireChild(name).asInt()

  /** Finds the child with the specified name and returns it as a boolean.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getBoolean(name: String): Boolean = requireChild(name).asBoolean()

  /** Finds the child with the specified name and returns it as a byte.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getByte(name: String): Byte = requireChild(name).asByte()

  /** Finds the child with the specified name and returns it as a short.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getShort(name: String): Short = requireChild(name).asShort()

  /** Finds the child with the specified name and returns it as a char.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getChar(name: String): Char = requireChild(name).asChar()

  // -- Index child getters (required) --

  /** Finds the child with the specified index and returns it as a string.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getString(index: Int): Nullable[String] = requireChild(index).asString()

  /** Finds the child with the specified index and returns it as a float.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getFloat(index: Int): Float = requireChild(index).asFloat()

  /** Finds the child with the specified index and returns it as a double.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getDouble(index: Int): Double = requireChild(index).asDouble()

  /** Finds the child with the specified index and returns it as a long.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getLong(index: Int): Long = requireChild(index).asLong()

  /** Finds the child with the specified index and returns it as an int.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getInt(index: Int): Int = requireChild(index).asInt()

  /** Finds the child with the specified index and returns it as a boolean.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getBoolean(index: Int): Boolean = requireChild(index).asBoolean()

  /** Finds the child with the specified index and returns it as a byte.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getByte(index: Int): Byte = requireChild(index).asByte()

  /** Finds the child with the specified index and returns it as a short.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getShort(index: Int): Short = requireChild(index).asShort()

  /** Finds the child with the specified index and returns it as a char.
    * @throws IllegalArgumentException
    *   if the child was not found.
    */
  def getChar(index: Int): Char = requireChild(index).asChar()

  // -- Type checking --

  def valueType: ValueType = _type

  def setType(t: ValueType): Unit = _type = t

  def isArray: Boolean = _type == ValueType.array

  def isObject: Boolean = _type == ValueType.`object`

  def isString: Boolean = _type == ValueType.stringValue

  /** Returns true if this is a double or long value. */
  def isNumber: Boolean = _type == ValueType.doubleValue || _type == ValueType.longValue

  def isDouble: Boolean = _type == ValueType.doubleValue

  def isLong: Boolean = _type == ValueType.longValue

  def isBoolean: Boolean = _type == ValueType.booleanValue

  def isNull: Boolean = _type == ValueType.nullValue

  /** Returns true if this is not an array or object. */
  def isValue: Boolean = _type match {
    case ValueType.stringValue | ValueType.doubleValue | ValueType.longValue | ValueType.booleanValue | ValueType.nullValue => true
    case _                                                                                                                  => false
  }

  // -- Setters --

  def setName(name: Nullable[String]): Unit = this.name = name

  /** Sets the name of the specified value and replaces an existing child with the same name, else adds it after the last child. */
  def setChild(name: String, value: JsonValue): Unit = {
    value.name = Nullable(name)
    setChild(value)
  }

  /** Replaces an existing child with the same name as the specified value, else adds it after the last child. */
  def setChild(value: JsonValue): Unit = boundary {
    val n = value.name
    if (n.isEmpty) throw new IllegalStateException("An object child requires a name: " + value)
    var current = child
    while (current.isDefined)
      current.foreach { c =>
        if (c.name.isDefined && c.name.exists(cn => n.exists(_ == cn))) {
          c.replace(value)
          break()
        }
        current = c.next
      }
    addChild(value)
  }

  /** Replaces this value in its parent with the specified value. */
  def replace(value: JsonValue): Unit = {
    val p = parent.getOrElse(throw new IllegalStateException("Cannot replace a value without a parent"))
    if (p.lastChild.exists(_ eq this)) p.lastChild = Nullable(value)
    if (prev.isDefined)
      prev.foreach(_.next = Nullable(value))
    else
      p.child = Nullable(value)
    value.prev = prev
    value.next = next
    next.foreach(_.prev = Nullable(value))
    value.parent = parent
    prev = Nullable.empty
    next = Nullable.empty
    parent = Nullable.empty
  }

  /** Sets the name of the specified value and adds it after the last child. */
  def addChild(name: String, value: JsonValue): Unit = {
    value.name = Nullable(name)
    addChild(value)
  }

  /** Adds the specified value after the last child.
    * @throws IllegalStateException
    *   if this is an object and the specified child's name is null.
    */
  def addChild(value: JsonValue): Unit = {
    if (_type == ValueType.`object` && value.name.isEmpty)
      throw new IllegalStateException("An object child requires a name: " + value)
    value.parent = Nullable(this)
    value.next = Nullable.empty
    if (child.isEmpty) {
      value.prev = Nullable.empty
      child = Nullable(value)
    } else {
      lastChild.foreach(_.next = Nullable(value))
      value.prev = lastChild
    }
    lastChild = Nullable(value)
    childCount += 1
  }

  /** Adds the specified value as the first child.
    * @throws IllegalStateException
    *   if this is an object and the specified child's name is null.
    */
  def addChildFirst(value: JsonValue): Unit = {
    if (_type == ValueType.`object` && value.name.isEmpty)
      throw new IllegalStateException("An object child requires a name: " + value)
    value.parent = Nullable(this)
    value.next = child
    value.prev = Nullable.empty
    if (child.isEmpty) {
      child = Nullable(value)
      lastChild = Nullable(value)
    } else {
      child.foreach(_.prev = Nullable(value))
      child = Nullable(value)
    }
    childCount += 1
  }

  def setNext(next: Nullable[JsonValue]): Unit = this.next = next
  def setPrev(prev: Nullable[JsonValue]): Unit = this.prev = prev

  /** Sets the type and value to the specified JsonValue. */
  def set(value: JsonValue): Unit = {
    _type = value._type
    stringValue = value.stringValue
    doubleValue = value.doubleValue
    longValue = value.longValue
  }

  def set(value: Nullable[String]): Unit = {
    stringValue = value
    _type = if (value.isEmpty) ValueType.nullValue else ValueType.stringValue
  }

  def setNull(): Unit = {
    stringValue = Nullable.empty
    _type = ValueType.nullValue
  }

  def set(value: Double, sv: Nullable[String]): Unit = {
    doubleValue = value
    longValue = value.toLong
    stringValue = sv
    _type = ValueType.doubleValue
  }

  def set(value: Long, sv: Nullable[String]): Unit = {
    longValue = value
    doubleValue = value.toDouble
    stringValue = sv
    _type = ValueType.longValue
  }

  def set(value: Boolean): Unit = {
    longValue = if (value) 1 else 0
    _type = ValueType.booleanValue
  }

  // -- Comparison --

  def equalsString(value: String): Boolean =
    asString().contains(value)

  def nameEquals(value: String): Boolean =
    name.contains(value)

  // -- Serialization --

  /** Returns a JSON string representation. */
  def toJson(): String =
    if (isValue) {
      asString().getOrElse("null")
    } else {
      val writer = new StringWriter(512)
      try
        toJson(writer)
      catch {
        case ex: IOException => throw SgeError.SerializationError("Error writing JSON", Some(ex))
      }
      writer.toString
    }

  /** Writes a JSON representation to the writer. */
  def toJson(writer: Writer): Unit =
    if (isObject) {
      writer.write('{')
      var c = child
      while (c.isDefined)
        c.foreach { entry =>
          writer.write('"')
          writer.write(entry.name.getOrElse(""))
          writer.write("\":")
          entry.toJson(writer)
          if (entry.next.isDefined) writer.write(',')
          c = entry.next
        }
      writer.write('}')
    } else if (isArray) {
      writer.write('[')
      var c = child
      while (c.isDefined)
        c.foreach { entry =>
          entry.toJson(writer)
          if (entry.next.isDefined) writer.write(',')
          c = entry.next
        }
      writer.write(']')
    } else if (isString) {
      writer.write('"')
      writer.write(stringValueGet)
      writer.write('"')
    } else if (isDouble) {
      val dv = asDouble()
      val lv = asLong()
      writer.write(if (dv == lv) lv.toString else dv.toString)
    } else if (isLong) {
      writer.write(asLong().toString)
    } else if (isBoolean) {
      writer.write(if (asBoolean()) "true" else "false")
    } else if (isNull) {
      writer.write("null")
    } else {
      throw SgeError.SerializationError("Unknown object type: " + this)
    }

  // -- Iteration --

  /** Iterates the children of this array or object. */
  override def iterator: Iterator[JsonValue] = new Iterator[JsonValue] {
    private var entry: Nullable[JsonValue] = child

    override def hasNext: Boolean = entry.isDefined

    override def next(): JsonValue = {
      val current = entry.getOrElse(throw new NoSuchElementException())
      entry = current.next
      current
    }
  }

  /** Returns a human readable string representing the path from the root of the JSON object graph to this value. */
  def trace(): String =
    parent.fold {
      if (_type == ValueType.array) "[]"
      else if (_type == ValueType.`object`) "{}"
      else ""
    } { p =>
      val t =
        if (p._type == ValueType.array) {
          var tr = "[]"
          var i  = 0
          var c  = p.child
          while (c.isDefined)
            c.foreach { cv =>
              if (cv eq this) {
                tr = "[" + i + "]"
                c = Nullable.empty // break
              } else {
                i += 1
                c = cv.next
              }
            }
          tr
        } else if (name.exists(_.indexOf('.') != -1))
          name.map(n => ".\"" + n.replace("\"", "\\\"") + "\"").getOrElse("")
        else
          "." + name.getOrElse("")
      p.trace() + t
    }

  override def toString(): String =
    if (isValue) {
      val s = asString()
      name.map(n => n + ": " + s.getOrElse("null")).getOrElse(s.getOrElse("null"))
    } else {
      name.map(_ + ": ").getOrElse("") + toJson()
    }
}

object JsonValue {

  /** The type of a JsonValue. */
  enum ValueType {
    case `object`, array, stringValue, doubleValue, longValue, booleanValue, nullValue
  }

  /** Converts a kindlings Json AST into a JsonValue linked-list tree. */
  def fromJson(json: hearth.kindlings.jsoniterjson.Json): JsonValue = {
    import hearth.kindlings.jsoniterjson.Json
    json match {
      case Json.Null =>
        JsonValue(ValueType.nullValue)
      case Json.Bool(v) =>
        JsonValue(v)
      case Json.Num(n) =>
        // Prefer long if it fits, else double
        val raw = n.value
        n.toLong match {
          case Some(l) if l.toString == raw => JsonValue(l, Nullable(raw))
          case _                            =>
            n.toDouble match {
              case Some(d) => JsonValue(d, Nullable(raw))
              case _       => JsonValue(Nullable(raw): Nullable[String])
            }
        }
      case Json.Str(s) =>
        JsonValue(Nullable(s): Nullable[String])
      case Json.Arr(values) =>
        val result = JsonValue(ValueType.array)
        values.foreach { elem =>
          result.addChild(fromJson(elem))
        }
        result
      case Json.Obj(fields) =>
        val result = JsonValue(ValueType.`object`)
        fields.fields.foreach { case (key, value) =>
          val child = fromJson(value)
          child.name = Nullable(key)
          result.addChild(child)
        }
        result
    }
  }
}
