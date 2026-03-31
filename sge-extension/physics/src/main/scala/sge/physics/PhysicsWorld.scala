/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (2D physics API backed by Rapier2D)
 *   Convention: handle-based FFI, platform-agnostic trait
 *   Audited: 2026-03-08
 */
package sge
package physics

import sge.platform.PhysicsPlatform
import sge.utils.Nullable

/** A 2D physics simulation world backed by Rapier2D.
  *
  * Manages rigid bodies, colliders, joints, and provides ray cast / point query functionality. Each world is independent and can be stepped at its own rate.
  *
  * The world must be [[close]]d when no longer needed to free native resources.
  *
  * @param gravityX
  *   horizontal gravity component (default 0)
  * @param gravityY
  *   vertical gravity component (default -9.81, pointing down)
  */
class PhysicsWorld(gravityX: Float = 0f, gravityY: Float = -9.81f) extends AutoCloseable {

  private[physics] val ops:    sge.platform.PhysicsOps = PhysicsPlatform.ops
  private[physics] val handle: Long                    = ops.createWorld(gravityX, gravityY)
  private var closed:          Boolean                 = false

  /** Maximum number of contact events polled per step. */
  private val MaxEvents = 256

  /** Scratch buffers for contact event polling. */
  private val eventBuf1 = new Array[Long](MaxEvents)
  private val eventBuf2 = new Array[Long](MaxEvents)

  /** Scratch buffer for gravity query. */
  private val gravBuf = new Array[Float](2)

  /** Scratch buffer for ray cast results (9 floats: hitXY, normalXY, toi, bodyHandleLo/Hi, colliderHandleLo/Hi). */
  private val rayBuf = new Array[Float](9)

  /** Scratch buffer for point query results. */
  private val pointBuf = new Array[Long](64)

  private def checkNotClosed(): Unit =
    if (closed) throw new IllegalStateException("PhysicsWorld has been closed")

  /** Advances the simulation by `dt` seconds. */
  def step(dt: Float): Unit = {
    checkNotClosed()
    ops.worldStep(handle, dt)
  }

  /** Sets the world gravity vector. */
  def setGravity(x: Float, y: Float): Unit = {
    checkNotClosed()
    ops.worldSetGravity(handle, x, y)
  }

  /** Returns the current gravity vector as (gx, gy). */
  def gravity: (Float, Float) = {
    checkNotClosed()
    ops.worldGetGravity(handle, gravBuf)
    (gravBuf(0), gravBuf(1))
  }

  /** Creates a new rigid body in this world.
    *
    * @param bodyType
    *   the body type (Dynamic, Static, or Kinematic)
    * @param x
    *   initial x position
    * @param y
    *   initial y position
    * @param angle
    *   initial rotation angle in radians
    * @return
    *   the new [[RigidBody]] handle
    */
  def createBody(bodyType: BodyType, x: Float = 0f, y: Float = 0f, angle: Float = 0f): RigidBody = {
    checkNotClosed()
    val bh = bodyType match {
      case BodyType.Dynamic   => ops.createDynamicBody(handle, x, y, angle)
      case BodyType.Static    => ops.createStaticBody(handle, x, y, angle)
      case BodyType.Kinematic => ops.createKinematicBody(handle, x, y, angle)
    }
    RigidBody(this, bh, bodyType)
  }

  /** Destroys a rigid body and all its attached colliders. */
  def destroyBody(body: RigidBody): Unit = {
    checkNotClosed()
    ops.destroyBody(handle, body.handle)
  }

  /** Destroys a collider, detaching it from its parent body. */
  def destroyCollider(collider: Collider): Unit = {
    checkNotClosed()
    ops.destroyCollider(handle, collider.handle)
  }

  /** Creates a joint between two bodies.
    *
    * @param jointDef
    *   the joint definition
    * @return
    *   the new [[Joint]] handle
    */
  def createJoint(jointDef: JointDef): Joint = {
    checkNotClosed()
    val jh = jointDef match {
      case JointDef.Revolute(b1, b2, ax, ay) =>
        ops.createRevoluteJoint(handle, b1.handle, b2.handle, ax, ay)
      case JointDef.Prismatic(b1, b2, ax, ay) =>
        ops.createPrismaticJoint(handle, b1.handle, b2.handle, ax, ay)
      case JointDef.Fixed(b1, b2) =>
        ops.createFixedJoint(handle, b1.handle, b2.handle)
    }
    Joint(this, jh)
  }

  /** Destroys a joint. */
  def destroyJoint(joint: Joint): Unit = {
    checkNotClosed()
    ops.destroyJoint(handle, joint.handle)
  }

  /** Casts a ray and returns the first hit, or [[Nullable.empty]] if nothing was hit.
    *
    * @param originX
    *   ray origin x
    * @param originY
    *   ray origin y
    * @param dirX
    *   ray direction x (need not be normalized)
    * @param dirY
    *   ray direction y (need not be normalized)
    * @param maxDist
    *   maximum ray distance
    * @return
    *   the [[RayCastHit]] or empty
    */
  def rayCast(originX: Float, originY: Float, dirX: Float, dirY: Float, maxDist: Float): Nullable[RayCastHit] = {
    checkNotClosed()
    if (ops.rayCast(handle, originX, originY, dirX, dirY, maxDist, rayBuf)) {
      Nullable(
        RayCastHit(
          hitX = rayBuf(0),
          hitY = rayBuf(1),
          normalX = rayBuf(2),
          normalY = rayBuf(3),
          timeOfImpact = rayBuf(4),
          bodyHandle = (java.lang.Float.floatToRawIntBits(rayBuf(5)).toLong & 0xffffffffL) |
            ((java.lang.Float.floatToRawIntBits(rayBuf(6)).toLong & 0xffffffffL) << 32),
          colliderHandle = (java.lang.Float.floatToRawIntBits(rayBuf(7)).toLong & 0xffffffffL) |
            ((java.lang.Float.floatToRawIntBits(rayBuf(8)).toLong & 0xffffffffL) << 32)
        )
      )
    } else {
      Nullable.empty
    }
  }

  /** Finds all bodies whose colliders contain the given point.
    *
    * @return
    *   a sequence of body handles (up to 64 results)
    */
  def queryPoint(x: Float, y: Float): scala.collection.immutable.Seq[Long] = {
    checkNotClosed()
    val count = ops.queryPoint(handle, x, y, pointBuf, pointBuf.length)
    if (count == 0) {
      scala.collection.immutable.Seq.empty
    } else {
      val builder = scala.collection.immutable.Seq.newBuilder[Long]
      var i       = 0
      while (i < count) {
        builder += pointBuf(i)
        i += 1
      }
      builder.result()
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

  /** Releases all native resources held by this world. */
  override def close(): Unit =
    if (!closed) {
      closed = true
      ops.destroyWorld(handle)
    }
}
