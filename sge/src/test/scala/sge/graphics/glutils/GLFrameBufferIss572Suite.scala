/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge
package graphics
package glutils

import lowlevel.util.DynamicArray
import sge.utils.createRef

/** ISS-572 regression pin (wave 2026-07-18-H, territory H3).
  *
  * GLFrameBuffer keeps its colour attachments in a `DynamicArray.createRef[T]()` field (GLFrameBuffer.scala:61) where the element type `T <: GLTexture` is an ABSTRACT-bound reference type, then
  * iterates them with `.foreach` (GLFrameBuffer.scala:302, reading `texture.glTarget`) and reads the head with `.first` (GLFrameBuffer.scala:99/:286). The sge-side `createRef` extension
  * (sge.utils.DynamicArrayOps) builds the array via `MkArray.anyRef[AnyRef].asInstanceOf[MkArray[A]]`, i.e. an `Object[]` backing.
  *
  * Under lls < 0.2.0 the inlined `mk0.castArray(_items)` inside `DynamicArray.foreach`/`.first` expanded — at a call site whose element type bound erases to `GLTexture` — to a reified whole-array
  * CHECKCAST of the `Object[]` backing to `GLTexture[]`, which every runtime rejects with `[Ljava.lang.Object; cannot be cast to [Lsge.graphics.GLTexture;`. That is the ClassCastException the ISS-485
  * desktop harness first surfaced at GLFrameBuffer.scala:302 (ISS-572), of the lls MkArray/castArray class fixed by ISS-686 in lls 0.2.0 (project pins lls 0.3.0).
  *
  * This suite reproduces the EXACT production shape headlessly (no GL context): a generic `FakeFrameBuffer[T <: AttachmentBase]` whose field is `DynamicArray.createRef[T]()`, populated via `.add`,
  * iterated via `.foreach` reading an abstract-bound member, and read via `.first`. It pins the mechanism GREEN on JVM + JS + Native. If lls ever regresses the withResolved witness, the
  * `.foreach`/`.first` here throw ClassCastException and this suite goes red.
  */
class GLFrameBufferIss572Suite extends munit.FunSuite {

  // Abstract base mirroring GLTexture: an abstract reference type carrying the
  // member (`glTarget`) that GLFrameBuffer reads inside its foreach at :302.
  abstract class AttachmentBase { def glTarget: Int }
  final class Attachment(val glTarget: Int) extends AttachmentBase

  // Mirror of GLFrameBuffer[T <: GLTexture]: the `createRef` field, `.add`,
  // `.foreach`, and `.first` all live inside a generic type whose element bound
  // is abstract — exactly the site that made the inlined castArray reify to
  // `AttachmentBase[]` over an `Object[]` backing.
  abstract class FakeFrameBuffer[T <: AttachmentBase] {
    // GLFrameBuffer.scala:61
    protected val attachments: DynamicArray[T] = DynamicArray.createRef[T]()

    // GLFrameBuffer.scala:236/:254
    protected def addAttachment(texture: T): Unit = attachments.add(texture)

    // GLFrameBuffer.scala:302 shape: foreach reading an abstract-bound member.
    def collectTargets(): List[Int] = {
      var acc = List.empty[Int]
      attachments.foreach { texture =>
        acc = texture.glTarget :: acc
      }
      acc.reverse
    }

    // GLFrameBuffer.scala:99/:286 shape: `.first` narrowing the Object[] head.
    def firstTarget(): Int = attachments.first.glTarget
  }

  final class ConcreteFrameBuffer(specs: Int*) extends FakeFrameBuffer[Attachment] {
    specs.foreach(t => addAttachment(new Attachment(t)))
  }

  test("createRef-backed foreach over abstract-bound attachments does not CCE (ISS-572)") {
    val fbo = new ConcreteFrameBuffer(1, 2, 3)
    // Pre-lls-0.2.0 this line threw
    //   ClassCastException: [Ljava.lang.Object; cannot be cast to [L...AttachmentBase;
    assertEquals(fbo.collectTargets(), List(1, 2, 3))
  }

  test("createRef-backed .first over abstract-bound attachments does not CCE (ISS-572)") {
    val fbo = new ConcreteFrameBuffer(7, 8)
    assertEquals(fbo.firstTarget(), 7)
  }

  test("createRef-backed foreach on a single-attachment fbo (ISS-572)") {
    val fbo = new ConcreteFrameBuffer(42)
    assertEquals(fbo.collectTargets(), List(42))
    assertEquals(fbo.firstTarget(), 42)
  }
}
