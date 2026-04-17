/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: handle-based FFI, platform-agnostic public API
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 373
 * Covenant-baseline-methods: ContactPoint3d,MaxEvents,PhysicsWorld3d,aabbBuf,bh,buf,buildEventPairs,checkNotClosed,close,closed,count,createBody,createJoint,destroyBody,destroyCollider,destroyJoint,eventBuf1,eventBuf2,forceBuf,getContactPoints,gravBuf,gravity,handle,numSolverIterations,numSolverIterations_,ops,pointBuf,pollContactForceEvents,pollContactStartEvents,pollContactStopEvents,pollIntersectionStartEvents,pollIntersectionStopEvents,queryAABB,queryPoint,rayBuf,rayCast,rayCastAll,setGravity,setNumAdditionalFrictionIterations,setNumInternalPgsIterations,step
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-17
 */
package sge
package physics3d

import sge.platform.PhysicsPlatform3d
import sge.utils.Nullable

/** A 3D physics simulation world backed by Rapier3D.
  *
  * Manages rigid bodies, colliders, joints, and provides ray cast functionality. Each world is independent and can be stepped at its own rate.
  *
  * The world must be [[close]]d when no longer needed to free native resources.
  *
  * @param gravityX
  *   x component of gravity (default 0)
  * @param gravityY
  *   y component of gravity (default -9.81, pointing down)
  * @param gravityZ
  *   z component of gravity (default 0)
  */
class PhysicsWorld3d(gravityX: Float = 0f, gravityY: Float = -9.81f, gravityZ: Float = 0f) extends AutoCloseable {

  private[physics3d] val ops:    sge.platform.PhysicsOps3d = PhysicsPlatform3d.ops
  private[physics3d] val handle: Long                      = ops.createWorld(gravityX, gravityY, gravityZ)
  private var closed:            Boolean                   = false

  /** Maximum number of contact events polled per step. */
  private val MaxEvents = 256

  /** Scratch buffers for contact event polling. */
  private val eventBuf1 = new Array[Long](MaxEvents)
  private val eventBuf2 = new Array[Long](MaxEvents)

  /** Scratch buffer for gravity query. */
  private val gravBuf = new Array[Float](3)

  /** Scratch buffer for ray cast results (9 floats: hitXYZ, normalXYZ, toi, colliderHandleLo/Hi). */
  private val rayBuf = new Array[Float](9)

  private def checkNotClosed(): Unit =
    if (closed) throw new IllegalStateException("PhysicsWorld3d has been closed")

  /** Advances the simulation by `dt` seconds. */
  def step(dt: Float): Unit = {
    checkNotClosed()
    ops.worldStep(handle, dt)
  }

  /** Sets the world gravity vector. */
  def setGravity(x: Float, y: Float, z: Float): Unit = {
    checkNotClosed()
    ops.worldSetGravity(handle, x, y, z)
  }

  /** Returns the current gravity vector as (gx, gy, gz). */
  def gravity: (Float, Float, Float) = {
    checkNotClosed()
    ops.worldGetGravity(handle, gravBuf)
    (gravBuf(0), gravBuf(1), gravBuf(2))
  }

  /** Creates a new 3D rigid body in this world.
    *
    * @param bodyType
    *   the body type (Dynamic, Static, or Kinematic)
    * @param x
    *   initial x position
    * @param y
    *   initial y position
    * @param z
    *   initial z position
    * @param qx
    *   quaternion x component (default 0)
    * @param qy
    *   quaternion y component (default 0)
    * @param qz
    *   quaternion z component (default 0)
    * @param qw
    *   quaternion w component (default 1, identity rotation)
    * @return
    *   the new [[RigidBody3d]] handle
    */
  def createBody(
    bodyType: BodyType3d,
    x:        Float = 0f,
    y:        Float = 0f,
    z:        Float = 0f,
    qx:       Float = 0f,
    qy:       Float = 0f,
    qz:       Float = 0f,
    qw:       Float = 1f
  ): RigidBody3d = {
    checkNotClosed()
    val bh = bodyType match {
      case BodyType3d.Dynamic   => ops.createDynamicBody(handle, x, y, z, qx, qy, qz, qw)
      case BodyType3d.Static    => ops.createStaticBody(handle, x, y, z, qx, qy, qz, qw)
      case BodyType3d.Kinematic => ops.createKinematicBody(handle, x, y, z, qx, qy, qz, qw)
    }
    RigidBody3d(this, bh, bodyType)
  }

  /** Destroys a rigid body and all its attached colliders. */
  def destroyBody(body: RigidBody3d): Unit = {
    checkNotClosed()
    ops.destroyBody(handle, body.handle)
  }

  /** Destroys a collider, detaching it from its parent body. */
  def destroyCollider(collider: Collider3d): Unit = {
    checkNotClosed()
    ops.destroyCollider(handle, collider.handle)
  }

  /** Creates a joint between two bodies.
    *
    * @param jointDef
    *   the joint definition
    * @return
    *   the new [[Joint3d]] handle
    */
  def createJoint(jointDef: JointDef3d): Joint3d = {
    checkNotClosed()
    jointDef match {
      case JointDef3d.Fixed(b1, b2) =>
        val jh = ops.createFixedJoint(handle, b1.handle, b2.handle)
        new FixedJoint3d(this, jh)
      case JointDef3d.Rope(b1, b2, maxDist) =>
        val jh = ops.createRopeJoint(handle, b1.handle, b2.handle, maxDist)
        new RopeJoint3d(this, jh)
      case JointDef3d.Revolute(b1, b2, ax, ay, az, axisX, axisY, axisZ) =>
        val jh = ops.createRevoluteJoint(handle, b1.handle, b2.handle, ax, ay, az, axisX, axisY, axisZ)
        new RevoluteJoint3d(this, jh)
      case JointDef3d.Prismatic(b1, b2, axisX, axisY, axisZ) =>
        val jh = ops.createPrismaticJoint(handle, b1.handle, b2.handle, axisX, axisY, axisZ)
        new PrismaticJoint3d(this, jh)
      case JointDef3d.Motor(b1, b2) =>
        val jh = ops.createMotorJoint(handle, b1.handle, b2.handle)
        new MotorJoint3d(this, jh)
      case JointDef3d.Spring(b1, b2, restLength, stiffness, damping) =>
        val jh = ops.createSpringJoint(handle, b1.handle, b2.handle, restLength, stiffness, damping)
        new SpringJoint3d(this, jh)
    }
  }

  /** Destroys a joint. */
  def destroyJoint(joint: Joint3d): Unit = {
    checkNotClosed()
    ops.destroyJoint(handle, joint.handle)
  }

  /** Casts a ray and returns the first hit, or [[Nullable.empty]] if nothing was hit.
    *
    * @param originX
    *   ray origin x
    * @param originY
    *   ray origin y
    * @param originZ
    *   ray origin z
    * @param dirX
    *   ray direction x (need not be normalized)
    * @param dirY
    *   ray direction y (need not be normalized)
    * @param dirZ
    *   ray direction z (need not be normalized)
    * @param maxDist
    *   maximum ray distance
    * @return
    *   the [[RayCastHit3d]] or empty
    */
  def rayCast(originX: Float, originY: Float, originZ: Float, dirX: Float, dirY: Float, dirZ: Float, maxDist: Float): Nullable[RayCastHit3d] = {
    checkNotClosed()
    if (ops.rayCast(handle, originX, originY, originZ, dirX, dirY, dirZ, maxDist, rayBuf)) {
      Nullable(
        RayCastHit3d(
          hitX = rayBuf(0),
          hitY = rayBuf(1),
          hitZ = rayBuf(2),
          normalX = rayBuf(3),
          normalY = rayBuf(4),
          normalZ = rayBuf(5),
          timeOfImpact = rayBuf(6),
          colliderHandle = (java.lang.Float.floatToRawIntBits(rayBuf(7)).toLong & 0xffffffffL) |
            ((java.lang.Float.floatToRawIntBits(rayBuf(8)).toLong & 0xffffffffL) << 32)
        )
      )
    } else {
      Nullable.empty
    }
  }

  /** Polls contact-start events that occurred during the last step.
    *
    * @return
    *   a sequence of (collider1Handle, collider2Handle) pairs
    */
  def pollContactStartEvents(): scala.collection.immutable.Seq[(Long, Long)] = {
    checkNotClosed()
    val count = ops.pollContactStartEvents(handle, eventBuf1, eventBuf2, MaxEvents)
    buildEventPairs(count)
  }

  /** Polls contact-stop events that occurred during the last step.
    *
    * @return
    *   a sequence of (collider1Handle, collider2Handle) pairs
    */
  def pollContactStopEvents(): scala.collection.immutable.Seq[(Long, Long)] = {
    checkNotClosed()
    val count = ops.pollContactStopEvents(handle, eventBuf1, eventBuf2, MaxEvents)
    buildEventPairs(count)
  }

  private def buildEventPairs(count: Int): scala.collection.immutable.Seq[(Long, Long)] =
    if (count == 0) {
      scala.collection.immutable.Seq.empty
    } else {
      val builder = scala.collection.immutable.Seq.newBuilder[(Long, Long)]
      var i       = 0
      while (i < count) {
        builder += ((eventBuf1(i), eventBuf2(i)))
        i += 1
      }
      builder.result()
    }

  // ─── Additional queries ──────────────────────────────────────────────

  private val forceBuf = new Array[Float](MaxEvents)
  private val aabbBuf  = new Array[Long](256)
  private val pointBuf = new Array[Long](64)

  /** Finds all colliders intersecting the given 3D AABB. */
  def queryAABB(minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float): scala.collection.immutable.Seq[Long] = {
    checkNotClosed()
    val count = ops.queryAABB(handle, minX, minY, minZ, maxX, maxY, maxZ, aabbBuf, aabbBuf.length)
    if (count == 0) scala.collection.immutable.Seq.empty
    else {
      val builder = scala.collection.immutable.Seq.newBuilder[Long]
      var i       = 0
      while (i < count) { builder += aabbBuf(i); i += 1 }
      builder.result()
    }
  }

  /** Finds all bodies whose colliders contain the given 3D point. */
  def queryPoint(x: Float, y: Float, z: Float): scala.collection.immutable.Seq[Long] = {
    checkNotClosed()
    val count = ops.queryPoint(handle, x, y, z, pointBuf, pointBuf.length)
    if (count == 0) scala.collection.immutable.Seq.empty
    else {
      val builder = scala.collection.immutable.Seq.newBuilder[Long]
      var i       = 0
      while (i < count) { builder += pointBuf(i); i += 1 }
      builder.result()
    }
  }

  /** Casts a ray and returns ALL hits. */
  def rayCastAll(
    originX: Float,
    originY: Float,
    originZ: Float,
    dirX:    Float,
    dirY:    Float,
    dirZ:    Float,
    maxDist: Float,
    maxHits: Int = 64
  ): scala.collection.immutable.Seq[RayCastHit3d] = {
    checkNotClosed()
    val buf   = new Array[Float](maxHits * 9)
    val count = ops.rayCastAll(handle, originX, originY, originZ, dirX, dirY, dirZ, maxDist, buf, maxHits)
    if (count == 0) scala.collection.immutable.Seq.empty
    else {
      val builder = scala.collection.immutable.Seq.newBuilder[RayCastHit3d]
      var i       = 0
      while (i < count) {
        val off = i * 9
        builder += RayCastHit3d(
          buf(off),
          buf(off + 1),
          buf(off + 2),
          buf(off + 3),
          buf(off + 4),
          buf(off + 5),
          buf(off + 6),
          (java.lang.Float.floatToRawIntBits(buf(off + 7)).toLong & 0xffffffffL) |
            ((java.lang.Float.floatToRawIntBits(buf(off + 8)).toLong & 0xffffffffL) << 32)
        )
        i += 1
      }
      builder.result()
    }
  }

  // ─── Contact details ──────────────────────────────────────────────────

  /** Gets contact details between two colliders. Returns 3D contact points. */
  def getContactPoints(collider1: Collider3d, collider2: Collider3d): Array[ContactPoint3d] = {
    checkNotClosed()
    val count = ops.contactPairCount(handle, collider1.handle, collider2.handle)
    if (count == 0) Array.empty
    else {
      val buf    = new Array[Float](count * 7)
      val actual = ops.contactPairPoints(handle, collider1.handle, collider2.handle, buf, count)
      Array.tabulate(actual) { i =>
        val off = i * 7
        ContactPoint3d(buf(off), buf(off + 1), buf(off + 2), buf(off + 3), buf(off + 4), buf(off + 5), buf(off + 6))
      }
    }
  }

  // ─── Intersection events ──────────────────────────────────────────────

  def pollIntersectionStartEvents(): scala.collection.immutable.Seq[(Long, Long)] = {
    checkNotClosed()
    val count = ops.pollIntersectionStartEvents(handle, eventBuf1, eventBuf2, MaxEvents)
    buildEventPairs(count)
  }

  def pollIntersectionStopEvents(): scala.collection.immutable.Seq[(Long, Long)] = {
    checkNotClosed()
    val count = ops.pollIntersectionStopEvents(handle, eventBuf1, eventBuf2, MaxEvents)
    buildEventPairs(count)
  }

  // ─── Contact force events ──────────────────────────────────────────────

  /** Polls contact force events that occurred during the last step. */
  def pollContactForceEvents(): scala.collection.immutable.Seq[(Long, Long, Float)] = {
    checkNotClosed()
    val count = ops.pollContactForceEvents(handle, eventBuf1, eventBuf2, forceBuf, MaxEvents)
    if (count == 0) scala.collection.immutable.Seq.empty
    else {
      val builder = scala.collection.immutable.Seq.newBuilder[(Long, Long, Float)]
      var i       = 0
      while (i < count) { builder += ((eventBuf1(i), eventBuf2(i), forceBuf(i))); i += 1 }
      builder.result()
    }
  }

  // ─── Solver parameters ────────────────────────────────────────────────

  def numSolverIterations:                            Int  = { checkNotClosed(); ops.worldGetNumSolverIterations(handle) }
  def numSolverIterations_=(iters:              Int): Unit = { checkNotClosed(); ops.worldSetNumSolverIterations(handle, iters) }
  def setNumAdditionalFrictionIterations(iters: Int): Unit = { checkNotClosed(); ops.worldSetNumAdditionalFrictionIterations(handle, iters) }
  def setNumInternalPgsIterations(iters:        Int): Unit = { checkNotClosed(); ops.worldSetNumInternalPgsIterations(handle, iters) }

  /** Releases all native resources held by this world. */
  override def close(): Unit =
    if (!closed) {
      closed = true
      ops.destroyWorld(handle)
    }
}

/** A 3D contact point between two colliders. */
final case class ContactPoint3d(
  normalX:     Float,
  normalY:     Float,
  normalZ:     Float,
  pointX:      Float,
  pointY:      Float,
  pointZ:      Float,
  penetration: Float
)
