/*
 * SGE - Scala Game Engine
 * copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package utils

import sge.Input.{ Button, Key }
import sge.audio.{ Pan, Pitch, Position, SoundId, Volume }
import sge.graphics.{
  BlendEquation,
  BlendFactor,
  BufferHandle,
  BufferTarget,
  BufferUsage,
  ClearMask,
  CompareFunc,
  CullFace,
  DataType,
  EnableCap,
  FramebufferHandle,
  PixelFormat,
  PrimitiveMode,
  ProgramHandle,
  RenderbufferHandle,
  ShaderHandle,
  ShaderType,
  StencilOp,
  TextureHandle,
  TextureTarget
}
import sge.math.{ Degrees, Epsilon, Radians }
import sge.net.HttpStatus

class MkArrayOpaqueTest extends munit.FunSuite {

  // Helper to verify the backing array is the expected primitive type, not Array[Object]
  private def assertBackingType[A](da: DynamicArray[A], expectedComponentType: Class[?])(using loc: munit.Location): Unit = {
    val itemsField = da.getClass.getDeclaredField("_items")
    itemsField.setAccessible(true)
    val backingArray = itemsField.get(da)
    assertEquals(
      backingArray.getClass.getComponentType,
      expectedComponentType,
      s"Expected backing array component type ${expectedComponentType.getName}"
    )
  }

  test("Int-backed opaque types use Array[Int] backing") {
    assertBackingType(DynamicArray[Key](4), classOf[Int])
    assertBackingType(DynamicArray[Button](4), classOf[Int])
    assertBackingType(DynamicArray[Pixels](4), classOf[Int])
    assertBackingType(DynamicArray[Align](4), classOf[Int])
    assertBackingType(DynamicArray[HttpStatus](4), classOf[Int])
    assertBackingType(DynamicArray[TextureHandle](4), classOf[Int])
    assertBackingType(DynamicArray[BufferHandle](4), classOf[Int])
    assertBackingType(DynamicArray[ShaderHandle](4), classOf[Int])
    assertBackingType(DynamicArray[ProgramHandle](4), classOf[Int])
    assertBackingType(DynamicArray[FramebufferHandle](4), classOf[Int])
    assertBackingType(DynamicArray[RenderbufferHandle](4), classOf[Int])
    assertBackingType(DynamicArray[ShaderType](4), classOf[Int])
    assertBackingType(DynamicArray[StencilOp](4), classOf[Int])
    assertBackingType(DynamicArray[CompareFunc](4), classOf[Int])
    assertBackingType(DynamicArray[BlendFactor](4), classOf[Int])
    assertBackingType(DynamicArray[BlendEquation](4), classOf[Int])
    assertBackingType(DynamicArray[PrimitiveMode](4), classOf[Int])
    assertBackingType(DynamicArray[BufferTarget](4), classOf[Int])
    assertBackingType(DynamicArray[BufferUsage](4), classOf[Int])
    assertBackingType(DynamicArray[PixelFormat](4), classOf[Int])
    assertBackingType(DynamicArray[DataType](4), classOf[Int])
    assertBackingType(DynamicArray[ClearMask](4), classOf[Int])
    assertBackingType(DynamicArray[CullFace](4), classOf[Int])
    assertBackingType(DynamicArray[EnableCap](4), classOf[Int])
    assertBackingType(DynamicArray[TextureTarget](4), classOf[Int])
  }

  test("Float-backed opaque types use Array[Float] backing") {
    assertBackingType(DynamicArray[Degrees](4), classOf[Float])
    assertBackingType(DynamicArray[Radians](4), classOf[Float])
    assertBackingType(DynamicArray[Epsilon](4), classOf[Float])
    assertBackingType(DynamicArray[Volume](4), classOf[Float])
    assertBackingType(DynamicArray[Pitch](4), classOf[Float])
    assertBackingType(DynamicArray[Pan](4), classOf[Float])
    assertBackingType(DynamicArray[Position](4), classOf[Float])
    assertBackingType(DynamicArray[Seconds](4), classOf[Float])
  }

  test("Long-backed opaque types use Array[Long] backing") {
    assertBackingType(DynamicArray[SoundId](4), classOf[Long])
    assertBackingType(DynamicArray[Millis](4), classOf[Long])
    assertBackingType(DynamicArray[Nanos](4), classOf[Long])
  }

  test("DynamicArray[Key] stores and retrieves values correctly") {
    val da = DynamicArray[Key](4)
    da.add(Key(65)) // 'A'
    da.add(Key(66)) // 'B'
    da.add(Key(67)) // 'C'
    assertEquals(da.size, 3)
    assertEquals(da(0).toInt, 65)
    assertEquals(da(1).toInt, 66)
    assertEquals(da(2).toInt, 67)
  }

  test("DynamicArray[Seconds] stores and retrieves values correctly") {
    val da = DynamicArray[Seconds](4)
    da.add(Seconds(1.5f))
    da.add(Seconds(2.0f))
    assertEquals(da.size, 2)
    assertEquals(da(0).toFloat, 1.5f)
    assertEquals(da(1).toFloat, 2.0f)
  }

  test("DynamicArray[Millis] stores and retrieves values correctly") {
    val da = DynamicArray[Millis](4)
    da.add(Millis(1000L))
    da.add(Millis(2000L))
    assertEquals(da.size, 2)
    assertEquals(da(0).toLong, 1000L)
    assertEquals(da(1).toLong, 2000L)
  }
}
