/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (2D physics API backed by Rapier2D)
 *   Convention: handle-based FFI, platform-agnostic trait
 *   Audited: 2026-03-08
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 587
 * Covenant-baseline-methods: ContactPoint,MaxEvents,PhysicsWorld,aabbBuf,bh,buf,buildEventPairs,castShape,checkNotClosed,close,closed,count,createBody,createJoint,destroyBody,destroyCollider,destroyJoint,eventBuf1,eventBuf2,forceBuf,getContactPoints,gravBuf,gravity,handle,intersectShape,numSolverIterations,numSolverIterations_,ops,pointBuf,pollContactForceEvents,pollContactStartEvents,pollContactStopEvents,pollIntersectionStartEvents,pollIntersectionStopEvents,projectBuf,projectPoint,queryAABB,queryPoint,rayBuf,rayCast,rayCastAll,setGravity,setNumAdditionalFrictionIterations,setNumInternalPgsIterations,shapeAndParams,shapeCastBuf,step
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-17
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
  private val forceBuf  = new Array[Float](MaxEvents)

  /** Scratch buffer for gravity query. */
  private val gravBuf = new Array[Float](2)

  /** Scratch buffer for ray cast results (9 floats: hitXY, normalXY, toi, bodyHandleLo/Hi, colliderHandleLo/Hi). */
  private val rayBuf = new Array[Float](9)

  /** Scratch buffer for point query results. */
  private val pointBuf = new Array[Long](64)

  /** Scratch buffer for AABB query results. */
  private val aabbBuf = new Array[Long](256)

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
    jointDef match {
      case JointDef.Revolute(b1, b2, ax, ay) =>
        val jh = ops.createRevoluteJoint(handle, b1.handle, b2.handle, ax, ay)
        new RevoluteJoint(this, jh)
      case JointDef.Prismatic(b1, b2, ax, ay) =>
        val jh = ops.createPrismaticJoint(handle, b1.handle, b2.handle, ax, ay)
        new PrismaticJoint(this, jh)
      case JointDef.Fixed(b1, b2) =>
        val jh = ops.createFixedJoint(handle, b1.handle, b2.handle)
        new FixedJoint(this, jh)
      case JointDef.Rope(b1, b2, maxDist) =>
        val jh = ops.createRopeJoint(handle, b1.handle, b2.handle, maxDist)
        new RopeJoint(this, jh)
      case JointDef.Motor(b1, b2) =>
        val jh = ops.createMotorJoint(handle, b1.handle, b2.handle)
        new MotorJoint(this, jh)
      case JointDef.Mouse(body, tx, ty) =>
        // Create a hidden kinematic anchor body at the target position
        val anchorHandle = ops.createKinematicBody(handle, tx, ty, 0f)
        // Create a motor joint between the anchor and the dragged body
        val jh = ops.createMotorJoint(handle, anchorHandle, body.handle)
        // Configure the motor for responsive dragging
        ops.motorJointSetMaxForce(handle, jh, 1000f)
        ops.motorJointSetCorrectionFactor(handle, jh, 0.3f)
        new MouseJoint(this, jh, anchorHandle)
      case JointDef.Spring(b1, b2, restLength, stiffness, damping) =>
        val jh = ops.createSpringJoint(handle, b1.handle, b2.handle, restLength, stiffness, damping)
        new SpringJoint(this, jh)
    }
  }

  /** Destroys a joint.
    *
    * For [[MouseJoint]], this also destroys the internal kinematic anchor body.
    */
  def destroyJoint(joint: Joint): Unit = {
    checkNotClosed()
    ops.destroyJoint(handle, joint.handle)
    joint match {
      case mj: MouseJoint => ops.destroyBody(handle, mj.anchorHandle)
      case _ => ()
    }
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

  /** Finds all colliders intersecting the given axis-aligned bounding box.
    *
    * @param minX
    *   minimum x coordinate of the box
    * @param minY
    *   minimum y coordinate of the box
    * @param maxX
    *   maximum x coordinate of the box
    * @param maxY
    *   maximum y coordinate of the box
    * @return
    *   a sequence of collider handles (up to 256 results)
    */
  def queryAABB(minX: Float, minY: Float, maxX: Float, maxY: Float): scala.collection.immutable.Seq[Long] = {
    checkNotClosed()
    val count = ops.queryAABB(handle, minX, minY, maxX, maxY, aabbBuf, aabbBuf.length)
    if (count == 0) {
      scala.collection.immutable.Seq.empty
    } else {
      val builder = scala.collection.immutable.Seq.newBuilder[Long]
      var i       = 0
      while (i < count) {
        builder += aabbBuf(i)
        i += 1
      }
      builder.result()
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

  // ─── Advanced queries ──────────────────────────────────────────────────

  /** Scratch buffer for shape cast / point projection results. */
  private val shapeCastBuf = new Array[Float](7)
  private val projectBuf   = new Array[Float](5)

  /** Casts a shape and returns the first hit, or [[Nullable.empty]] if nothing was hit.
    *
    * @param shape
    *   the shape to cast (Circle, Box, or Capsule)
    * @param originX
    *   sweep origin x
    * @param originY
    *   sweep origin y
    * @param dirX
    *   sweep direction x
    * @param dirY
    *   sweep direction y
    * @param maxDist
    *   maximum sweep distance
    * @return
    *   the [[ShapeCastHit]] or empty
    */
  def castShape(
    shape:   Shape,
    originX: Float,
    originY: Float,
    dirX:    Float,
    dirY:    Float,
    maxDist: Float
  ): Nullable[ShapeCastHit] = {
    checkNotClosed()
    val shapeAndParams: Option[(Int, Array[Float])] = shape match {
      case Shape.Circle(r)      => Some((0, Array(r, 0f)))
      case Shape.Box(hw, hh)    => Some((1, Array(hw, hh)))
      case Shape.Capsule(hh, r) => Some((2, Array(hh, r)))
      case _                    => None // only circle/box/capsule supported for sweep
    }
    shapeAndParams match {
      case None                      => Nullable.empty
      case Some((shapeType, params)) =>
        if (ops.castShape(handle, shapeType, params, originX, originY, dirX, dirY, maxDist, shapeCastBuf)) {
          Nullable(
            ShapeCastHit(
              hitX = shapeCastBuf(0),
              hitY = shapeCastBuf(1),
              normalX = shapeCastBuf(2),
              normalY = shapeCastBuf(3),
              timeOfImpact = shapeCastBuf(4),
              colliderHandle = (java.lang.Float.floatToRawIntBits(shapeCastBuf(5)).toLong & 0xffffffffL) |
                ((java.lang.Float.floatToRawIntBits(shapeCastBuf(6)).toLong & 0xffffffffL) << 32)
            )
          )
        } else {
          Nullable.empty
        }
    }
  }

  /** Casts a ray and returns ALL hits (up to `maxHits`).
    *
    * @return
    *   a sequence of [[RayCastHit]] results
    */
  def rayCastAll(
    originX: Float,
    originY: Float,
    dirX:    Float,
    dirY:    Float,
    maxDist: Float,
    maxHits: Int = 64
  ): scala.collection.immutable.Seq[RayCastHit] = {
    checkNotClosed()
    val buf   = new Array[Float](maxHits * 7)
    val count = ops.rayCastAll(handle, originX, originY, dirX, dirY, maxDist, buf, maxHits)
    if (count == 0) {
      scala.collection.immutable.Seq.empty
    } else {
      val builder = scala.collection.immutable.Seq.newBuilder[RayCastHit]
      var i       = 0
      while (i < count) {
        val off = i * 7
        builder += RayCastHit(
          hitX = buf(off),
          hitY = buf(off + 1),
          normalX = buf(off + 2),
          normalY = buf(off + 3),
          timeOfImpact = buf(off + 4),
          bodyHandle = 0L, // ray_cast_all returns collider handle, not body
          colliderHandle = (java.lang.Float.floatToRawIntBits(buf(off + 5)).toLong & 0xffffffffL) |
            ((java.lang.Float.floatToRawIntBits(buf(off + 6)).toLong & 0xffffffffL) << 32)
        )
        i += 1
      }
      builder.result()
    }
  }

  /** Projects a point onto the nearest collider surface.
    *
    * @return
    *   the [[PointProjection]] or empty if no colliders exist
    */
  def projectPoint(x: Float, y: Float): Nullable[PointProjection] = {
    checkNotClosed()
    if (ops.projectPoint(handle, x, y, projectBuf)) {
      Nullable(
        PointProjection(
          projX = projectBuf(0),
          projY = projectBuf(1),
          isInside = projectBuf(2) != 0f,
          colliderHandle = (java.lang.Float.floatToRawIntBits(projectBuf(3)).toLong & 0xffffffffL) |
            ((java.lang.Float.floatToRawIntBits(projectBuf(4)).toLong & 0xffffffffL) << 32)
        )
      )
    } else {
      Nullable.empty
    }
  }

  // ─── Intersection events (sensor overlaps) ────────────────────────────

  /** Polls intersection-start events (sensor overlaps) that occurred during the last step.
    *
    * @return
    *   a sequence of (collider1Handle, collider2Handle) pairs
    */
  def pollIntersectionStartEvents(): scala.collection.immutable.Seq[(Long, Long)] = {
    checkNotClosed()
    val count = ops.pollIntersectionStartEvents(handle, eventBuf1, eventBuf2, MaxEvents)
    buildEventPairs(count)
  }

  /** Polls intersection-stop events (sensor separation) that occurred during the last step.
    *
    * @return
    *   a sequence of (collider1Handle, collider2Handle) pairs
    */
  def pollIntersectionStopEvents(): scala.collection.immutable.Seq[(Long, Long)] = {
    checkNotClosed()
    val count = ops.pollIntersectionStopEvents(handle, eventBuf1, eventBuf2, MaxEvents)
    buildEventPairs(count)
  }

  // ─── Contact force events ──────────────────────────────────────────────

  /** Polls contact force events that occurred during the last step.
    *
    * Contact force events fire when the total force at a contact exceeds the collider's threshold. Requires `ActiveEvents::CONTACT_FORCE_EVENTS` flag set on the collider.
    *
    * @return
    *   a sequence of (collider1Handle, collider2Handle, totalForceMagnitude)
    */
  def pollContactForceEvents(): scala.collection.immutable.Seq[(Long, Long, Float)] = {
    checkNotClosed()
    val count = ops.pollContactForceEvents(handle, eventBuf1, eventBuf2, forceBuf, MaxEvents)
    if (count == 0) {
      scala.collection.immutable.Seq.empty
    } else {
      val builder = scala.collection.immutable.Seq.newBuilder[(Long, Long, Float)]
      var i       = 0
      while (i < count) {
        builder += ((eventBuf1(i), eventBuf2(i), forceBuf(i)))
        i += 1
      }
      builder.result()
    }
  }

  // ─── Solver parameters ────────────────────────────────────────────────

  /** Gets the number of solver iterations. */
  def numSolverIterations: Int = {
    checkNotClosed()
    ops.worldGetNumSolverIterations(handle)
  }

  /** Sets the number of solver iterations. More iterations = more accuracy but slower. */
  def numSolverIterations_=(iters: Int): Unit = {
    checkNotClosed()
    ops.worldSetNumSolverIterations(handle, iters)
  }

  /** Sets the number of additional friction iterations. */
  def setNumAdditionalFrictionIterations(iters: Int): Unit = {
    checkNotClosed()
    ops.worldSetNumAdditionalFrictionIterations(handle, iters)
  }

  /** Sets the number of internal PGS iterations. */
  def setNumInternalPgsIterations(iters: Int): Unit = {
    checkNotClosed()
    ops.worldSetNumInternalPgsIterations(handle, iters)
  }

  // ─── Contact details ──────────────────────────────────────────────────

  /** Gets contact details between two colliders.
    *
    * Returns an array of [[ContactPoint]] describing each contact manifold point, including the contact normal, world-space position, and penetration depth.
    *
    * @param collider1
    *   the first collider
    * @param collider2
    *   the second collider
    * @return
    *   an array of contact points (empty if no contact)
    */
  def getContactPoints(collider1: Collider, collider2: Collider): Array[ContactPoint] = {
    checkNotClosed()
    val count = ops.contactPairCount(handle, collider1.handle, collider2.handle)
    if (count == 0) Array.empty
    else {
      val buf    = new Array[Float](count * 5)
      val actual = ops.contactPairPoints(handle, collider1.handle, collider2.handle, buf, count)
      Array.tabulate(actual) { i =>
        val off = i * 5
        ContactPoint(buf(off), buf(off + 1), buf(off + 2), buf(off + 3), buf(off + 4))
      }
    }
  }

  // ─── Shape intersection ────────────────────────────────────────────────

  /** Tests if a shape at a given position overlaps any collider.
    *
    * @param shape
    *   the shape to test (Circle, Box, or Capsule)
    * @param posX
    *   x position of the shape
    * @param posY
    *   y position of the shape
    * @param angle
    *   rotation angle of the shape in radians
    * @return
    *   a sequence of collider handles that overlap
    */
  def intersectShape(
    shape: Shape,
    posX:  Float,
    posY:  Float,
    angle: Float = 0f
  ): scala.collection.immutable.Seq[Long] = {
    checkNotClosed()
    val (shapeType, params) = shape match {
      case Shape.Circle(r)      => (0, Array(r, 0f))
      case Shape.Box(hw, hh)    => (1, Array(hw, hh))
      case Shape.Capsule(hh, r) => (2, Array(hh, r))
      case _                    => (-1, Array(0f, 0f)) // unsupported
    }
    if (shapeType < 0) {
      scala.collection.immutable.Seq.empty
    } else {
      val count = ops.intersectShape(handle, shapeType, params, posX, posY, angle, aabbBuf, aabbBuf.length.min(256))
      if (count == 0) {
        scala.collection.immutable.Seq.empty
      } else {
        val builder = scala.collection.immutable.Seq.newBuilder[Long]
        var i       = 0
        while (i < count) {
          builder += aabbBuf(i)
          i += 1
        }
        builder.result()
      }
    }
  }

  /** Releases all native resources held by this world. */
  override def close(): Unit =
    if (!closed) {
      closed = true
      ops.destroyWorld(handle)
    }
}

/** A single contact point between two colliders.
  *
  * @param normalX
  *   x component of the contact normal (pointing from collider1 toward collider2)
  * @param normalY
  *   y component of the contact normal
  * @param pointX
  *   world-space x coordinate of the contact point
  * @param pointY
  *   world-space y coordinate of the contact point
  * @param penetration
  *   the penetration depth (positive means overlapping)
  */
final case class ContactPoint(
  normalX:     Float,
  normalY:     Float,
  pointX:      Float,
  pointY:      Float,
  penetration: Float
)
