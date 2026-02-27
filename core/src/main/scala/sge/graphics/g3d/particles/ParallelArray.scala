/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/ParallelArray.java
 * Original authors: inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */

package sge
package graphics
package g3d
package particles

import sge.utils.DynamicArray
import sge.utils.Nullable
import sge.utils.SgeError

/** This class represents an group of elements like an array, but the properties of the elements are stored as separate arrays. These arrays are called [[Channel]] and are represented by
  * [[ParallelArray.ChannelDescriptor]]. It's not necessary to store primitive types in the channels but doing so will "exploit" data locality in the JVM, which is ensured for primitive types. Use
  * [[FloatChannel]], [[IntChannel]], [[ObjectChannel]] to store the data.
  * @author
  *   inferno
  */
class ParallelArray(
  /** the maximum amount of elements that this array can hold */
  var capacity: Int
) {

  import ParallelArray.*

  /** the channels added to the array */
  val arrays: DynamicArray[Channel] = DynamicArray[Channel]()

  /** the current amount of defined elements, do not change manually unless you know what you are doing. */
  var size: Int = 0

  /** Adds and returns a channel described by the channel descriptor parameter. If a channel with the same id already exists, no allocation is performed and that channel is returned.
    */
  def addChannel[T <: Channel](channelDescriptor: ChannelDescriptor): T =
    addChannel(channelDescriptor, Nullable.empty)

  /** Adds and returns a channel described by the channel descriptor parameter. If a channel with the same id already exists, no allocation is performed and that channel is returned. Otherwise a new
    * channel is allocated and initialized with the initializer.
    */
  def addChannel[T <: Channel](channelDescriptor: ChannelDescriptor, initializer: Nullable[ChannelInitializer[T]]): T = {
    val channel: Nullable[T] = getChannel(channelDescriptor)
    if (channel.isEmpty) {
      val allocated = allocateChannel[T](channelDescriptor)
      initializer.foreach(init => init.init(allocated))
      arrays.add(allocated)
      allocated
    } else {
      channel.getOrElse(throw SgeError.InvalidInput("Unreachable"))
    }
  }

  private def allocateChannel[T <: Channel](channelDescriptor: ChannelDescriptor): T = {
    val channel: Channel =
      if (channelDescriptor.`type` == classOf[Float]) {
        new FloatChannel(this, channelDescriptor.id, channelDescriptor.count, capacity)
      } else if (channelDescriptor.`type` == classOf[Int]) {
        new IntChannel(this, channelDescriptor.id, channelDescriptor.count, capacity)
      } else {
        new ObjectChannel[Any](
          this,
          channelDescriptor.id,
          channelDescriptor.count,
          capacity,
          channelDescriptor.arraySupplier.asInstanceOf[Int => Array[Any]]
        )
      }
    channel.asInstanceOf[T]
  }

  /** Removes the channel with the given id */
  def removeArray(id: Int): Unit =
    arrays.removeIndex(findIndex(id))

  private def findIndex(id: Int): Int =
    scala.util.boundary {
      var i = 0
      while (i < arrays.size) {
        val array = arrays(i)
        if (array.id == id) scala.util.boundary.break(i)
        i += 1
      }
      -1
    }

  /** Adds an element considering the values in the same order as the current channels in the array. The n_th value must have the same type and stride of the given channel at position n
    */
  def addElement(values: Any*): Unit = {
    /* FIXME make it grow... */
    if (size == capacity) throw SgeError.InvalidInput("Capacity reached, cannot add other elements")

    var k = 0
    for (strideArray <- arrays) {
      strideArray.add(k, values*)
      k += strideArray.strideSize
    }
    size += 1
  }

  /** Removes the element at the given index and swaps it with the last available element */
  def removeElement(index: Int): Unit = {
    val last = size - 1
    // Swap
    for (strideArray <- arrays)
      strideArray.swap(index, last)
    size = last
  }

  /** @return the channel with the same id as the one in the descriptor */
  def getChannel[T <: Channel](descriptor: ChannelDescriptor): Nullable[T] =
    scala.util.boundary {
      for (array <- arrays)
        if (array.id == descriptor.id) scala.util.boundary.break(Nullable(array.asInstanceOf[T]))
      Nullable.empty[T]
    }

  /** Removes all the channels and sets size to 0 */
  def clear(): Unit = {
    arrays.clear()
    size = 0
  }

  /** Sets the capacity. Each contained channel will be resized to match the required capacity and the current data will be preserved.
    */
  def setCapacity(requiredCapacity: Int): Unit =
    if (capacity != requiredCapacity) {
      for (channel <- arrays)
        channel.setCapacity(requiredCapacity)
      capacity = requiredCapacity
    }
}

object ParallelArray {

  /** This class describes the content of a [[Channel]] */
  class ChannelDescriptor(
    var id:            Int,
    var arraySupplier: Int => Array[?],
    var count:         Int
  ) {

    var `type`: Class[?] = arraySupplier(0).getClass.getComponentType
  }

  /** This class represents a container of values for all the elements for a given property */
  abstract class Channel(
    /** Reference to the owning ParallelArray, needed to read current size during add operations */
    val owner:      ParallelArray,
    var id:         Int,
    var data:       Any,
    var strideSize: Int
  ) {

    def add(index: Int, objects: Any*): Unit

    def swap(i: Int, k: Int): Unit

    protected[particles] def setCapacity(requiredCapacity: Int): Unit
  }

  /** This interface is used to provide custom initialization of the [[Channel]] data */
  trait ChannelInitializer[T <: Channel] {
    def init(channel: T): Unit
  }

  class FloatChannel(owner: ParallelArray, id: Int, strideSize: Int, initialSize: Int) extends Channel(owner, id, new Array[Float](initialSize * strideSize), strideSize) {

    var floatData: Array[Float] = data.asInstanceOf[Array[Float]]

    override def add(index: Int, objects: Any*): Unit = {
      var i = strideSize * owner.size
      val c = i + strideSize
      var k = 0
      while (i < c) {
        floatData(i) = objects(k).asInstanceOf[Float]
        i += 1
        k += 1
      }
    }

    override def swap(i: Int, k: Int): Unit = {
      var ii = strideSize * i
      var kk = strideSize * k
      val c  = ii + strideSize
      while (ii < c) {
        val t = floatData(ii)
        floatData(ii) = floatData(kk)
        floatData(kk) = t
        ii += 1
        kk += 1
      }
    }

    override protected[particles] def setCapacity(requiredCapacity: Int): Unit = {
      val newData = new Array[Float](strideSize * requiredCapacity)
      System.arraycopy(floatData, 0, newData, 0, Math.min(floatData.length, newData.length))
      floatData = newData
      data = newData
    }
  }

  class IntChannel(owner: ParallelArray, id: Int, strideSize: Int, initialSize: Int) extends Channel(owner, id, new Array[Int](initialSize * strideSize), strideSize) {

    var intData: Array[Int] = data.asInstanceOf[Array[Int]]

    override def add(index: Int, objects: Any*): Unit = {
      var i = strideSize * owner.size
      val c = i + strideSize
      var k = 0
      while (i < c) {
        intData(i) = objects(k).asInstanceOf[Int]
        i += 1
        k += 1
      }
    }

    override def swap(i: Int, k: Int): Unit = {
      var ii = strideSize * i
      var kk = strideSize * k
      val c  = ii + strideSize
      while (ii < c) {
        val t = intData(ii)
        intData(ii) = intData(kk)
        intData(kk) = t
        ii += 1
        kk += 1
      }
    }

    override protected[particles] def setCapacity(requiredCapacity: Int): Unit = {
      val newData = new Array[Int](strideSize * requiredCapacity)
      System.arraycopy(intData, 0, newData, 0, Math.min(intData.length, newData.length))
      intData = newData
      data = newData
    }
  }

  class ObjectChannel[T](owner: ParallelArray, id: Int, strideSize: Int, initialSize: Int, arraySupplier: Int => Array[T])
      extends Channel(owner, id, arraySupplier(initialSize * strideSize), strideSize) {

    var objectData: Array[T] = data.asInstanceOf[Array[T]]

    override def add(index: Int, objects: Any*): Unit = {
      var i = strideSize * owner.size
      val c = i + strideSize
      var k = 0
      while (i < c) {
        objectData(i) = objects(k).asInstanceOf[T]
        i += 1
        k += 1
      }
    }

    override def swap(i: Int, k: Int): Unit = {
      var ii = strideSize * i
      var kk = strideSize * k
      val c  = ii + strideSize
      while (ii < c) {
        val t = objectData(ii)
        objectData(ii) = objectData(kk)
        objectData(kk) = t
        ii += 1
        kk += 1
      }
    }

    override protected[particles] def setCapacity(requiredCapacity: Int): Unit = {
      val newData = java.util.Arrays.copyOf(objectData.asInstanceOf[Array[AnyRef]], strideSize * requiredCapacity).asInstanceOf[Array[T]]
      objectData = newData
      data = newData
    }
  }
}
