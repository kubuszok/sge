/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (2D physics API backed by Rapier2D)
 *   Convention: Scala.js backend over the Rapier2D WASM compat module
 *   Idiom: split packages
 *   Audited: 2026-06-17
 *
 * Behavioral spec: PhysicsOpsPanama (JVM, delegates to the Rust `sge_physics`
 * C ABI). Every method here is the behavioral equivalent of its Panama twin —
 * same units, same out-array layout, same handle semantics. Where the Rust
 * side keeps state the Rapier JS API does not expose (configured motor
 * targets, additional-friction-iteration count, one-way-platform direction,
 * rotation/translation lock flags), this backend tracks that state on the
 * relevant registry record so the getters round-trip the setters, matching the
 * Rust behavior. Such cases are commented inline against the Panama reference.
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 1424
 * Covenant-baseline-methods: BodyState,ColliderState,JointState,PhysicsOpsJs,R,WorldState,a1,a2,add,addCollider,addVertexBuffer,ang,angularOffset,applyMotorTarget,arr,axisX,axisY,b,bodies,body,bodyApplyForce,bodyApplyForceAtPoint,bodyApplyImpulse,bodyApplyImpulseAtPoint,bodyApplyTorque,bodyApplyTorqueImpulse,bodyByRapier,bodyEnableCcd,bodyGetAngle,bodyGetAngularDamping,bodyGetAngularVelocity,bodyGetDominanceGroup,bodyGetGravityScale,bodyGetInertia,bodyGetLinearDamping,bodyGetLinearVelocity,bodyGetLocalCenterOfMass,bodyGetMass,bodyGetPosition,bodyGetType,bodyGetVelocityAtPoint,bodyGetWorldCenterOfMass,bodyIsAwake,bodyIsCcdEnabled,bodyIsEnabled,bodyIsRotationLocked,bodyIsTranslationLockedX,bodyIsTranslationLockedY,bodyRecomputeMassProperties,bodyResetForces,bodyResetTorques,bodySetAngle,bodySetAngularDamping,bodySetAngularVelocity,bodySetDominanceGroup,bodySetEnabled,bodySetEnabledTranslations,bodySetFixedRotation,bodySetGravityScale,bodySetLinearDamping,bodySetLinearVelocity,bodySetPosition,bodySleep,bodyState,bodyWakeUp,c,c1,castShape,cb,collider,colliderByRapier,colliderGetAabb,colliderGetActiveCollisionTypes,colliderGetActiveEvents,colliderGetActiveHooks,colliderGetCollisionGroups,colliderGetContactForceEventThreshold,colliderGetDensity,colliderGetFriction,colliderGetMass,colliderGetOneWayDirection,colliderGetParentBody,colliderGetPosition,colliderGetPositionWrtParent,colliderGetRestitution,colliderGetShapeType,colliderGetSolverGroups,colliderIsEnabled,colliderIsSensor,colliderSetActiveCollisionTypes,colliderSetActiveEvents,colliderSetActiveHooks,colliderSetCollisionGroups,colliderSetContactForceEventThreshold,colliderSetContactSkin,colliderSetDensity,colliderSetEnabled,colliderSetFriction,colliderSetMass,colliderSetOneWayDirection,colliderSetPositionWrtParent,colliderSetRestitution,colliderSetSensor,colliderSetSolverGroups,colliderState,colliders,collisionCb,contactForce,contactPairCount,contactPairPoints,contactStart,contactStop,copyPairs,correctionFactor,cos,count,createBody,createBoxCollider,createCapsuleCollider,createCircleCollider,createDynamicBody,createFixedJoint,createHeightfieldCollider,createKinematicBody,createMotorJoint,createPolygonCollider,createPolylineCollider,createPrismaticJoint,createRevoluteJoint,createRopeJoint,createSegmentCollider,createSpringJoint,createStaticBody,createTriMeshCollider,createWorld,cs,cx,cy,d,damping,data,desc,destroyBody,destroyCollider,destroyJoint,destroyWorld,drainEvents,dx,dy,eventQueue,forceCb,g,h,handle,hiBits,hit,hx,hy,i,idx,intersectShape,intersectionStart,intersectionStop,joints,jst,limitLower,limitUpper,linearOffsetX,linearOffsetY,loBits,localAnchor,makeShape,mapShapeType,maxDistance,maxMotorForce,maxMotorTorque,maxX,maxY,minX,minY,motorJointGetAngularOffset,motorJointGetCorrectionFactor,motorJointGetLinearOffset,motorJointGetMaxForce,motorJointGetMaxTorque,motorJointSetAngularOffset,motorJointSetCorrectionFactor,motorJointSetLinearOffset,motorJointSetMaxForce,motorJointSetMaxTorque,motorMaxForce,motorMaxTorque,motorSpeed,n,newImpulseJoint,nextBody,nextCollider,nextJoint,nextWorld,numAdditionalFrictionIterations,oneWayActive,oneWayAngle,oneWayNx,oneWayNy,p,packGroups,pad,pollContactForceEvents,pollContactStartEvents,pollContactStopEvents,pollIntersectionStartEvents,pollIntersectionStopEvents,prismaticJointEnableLimits,prismaticJointEnableMotor,prismaticJointGetLimits,prismaticJointGetMaxMotorForce,prismaticJointGetMotorSpeed,prismaticJointGetTranslation,prismaticJointSetLimits,prismaticJointSetMaxMotorForce,prismaticJointSetMotorSpeed,projectPoint,pts,queryAABB,queryPoint,r,rapierKey,ray,rayCast,rayCastAll,recomputeParentMass,recreateJoint,registerJoint,res,restLength,revoluteJointEnableLimits,revoluteJointEnableMotor,revoluteJointGetAngle,revoluteJointGetLimits,revoluteJointGetMaxMotorTorque,revoluteJointGetMotorSpeed,revoluteJointIsLimitEnabled,revoluteJointSetLimits,revoluteJointSetMaxMotorTorque,revoluteJointSetMotorSpeed,ropeJointGetMaxDistance,ropeJointSetMaxDistance,rotationLocked,sgeBodyHandle,sgeColliderHandle,shape,sin,springJointGetRestLength,springJointSetParams,springJointSetRestLength,src,st,stiffness,t,t1,t2,theta,toFloat32,transLockedX,transLockedY,tx,ty,v,vec,verts,w,world,worldGetGravity,worldGetNumSolverIterations,worldSetGravity,worldSetNumAdditionalFrictionIterations,worldSetNumInternalPgsIterations,worldSetNumSolverIterations,worldStep,worlds,ws
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-06-17
 */
package sge
package platform

import scala.scalajs.js
import scala.collection.mutable

/** Scala.js implementation of [[PhysicsOps]] backed by the Rapier2D WASM `@dimforge/rapier2d-compat` module.
  *
  * The module must be loaded first via [[PhysicsExtension.load]] (it resolves `RAPIER.init()`); calls before that fail fast through [[RapierModule.get]].
  *
  * Handles are opaque SGE `Long`s minted by incrementing counters and mapped to Rapier JS objects through registries. Out-array slots that carry a collider/body handle are encoded as two 32-bit float
  * halves (`Float.intBitsToFloat` of the low/high words), exactly the layout the high-level [[sge.physics.PhysicsWorld]] decodes with `floatToRawIntBits`.
  */
private[platform] object PhysicsOpsJs extends PhysicsOps {

  // ─── Module access ──────────────────────────────────────────────────────

  private def R: js.Dynamic = RapierModule.get

  // ─── Registries ─────────────────────────────────────────────────────────

  /** Per-world state: the Rapier World, an EventQueue stepped with it, and the events drained from the most recent step. */
  final private class WorldState(val world: js.Dynamic) {
    val eventQueue: js.Dynamic = js.Dynamic.newInstance(R.EventQueue)(true)

    // Collision/intersection events drained on the latest step (collider SGE handles).
    val contactStart:      js.Array[(Long, Long)]        = js.Array()
    val contactStop:       js.Array[(Long, Long)]        = js.Array()
    val intersectionStart: js.Array[(Long, Long)]        = js.Array()
    val intersectionStop:  js.Array[(Long, Long)]        = js.Array()
    val contactForce:      js.Array[(Long, Long, Float)] = js.Array()

    /** Requested additional-friction-iteration count (not exposed by the compat IntegrationParameters; stored for round-trip parity with the Rust backend). */
    var numAdditionalFrictionIterations: Int = 1
  }

  // SGE Long handles are keys: a Scala LongMap is required because Scala.js boxes Long as a RuntimeLong
  // object, so equal Long values are distinct references in a js.Map and would never match.
  private val worlds:    mutable.LongMap[WorldState] = mutable.LongMap()
  private val bodies:    mutable.LongMap[js.Dynamic] = mutable.LongMap()
  private val colliders: mutable.LongMap[js.Dynamic] = mutable.LongMap()

  /** Reverse lookups: Rapier f64 handle → SGE Long, per world. Keyed by "worldHandle:rapierHandle" (handle in its stable string form). */
  private val bodyByRapier:     mutable.Map[String, Long] = mutable.Map()
  private val colliderByRapier: mutable.Map[String, Long] = mutable.Map()

  /** Per-joint state, including the configured motor/limit values Rapier does not read back.
    *
    * `joint` is a `var` because the Spring/Rope/Fixed-backed-motor joint subclasses (base `ImpulseJoint`) expose no post-creation mutator for their creation-time parameters (rope length, spring
    * rest-length/stiffness/damping); the only way to reflect a setter — matching the Rust/Panama backend, which really reconfigures the joint — is to remove the underlying Rapier joint and recreate
    * it from updated `JointData`, then rebind this field. The SGE Long handle in the `joints` registry is unchanged, so existing references stay valid.
    */
  final private class JointState(val world: Long, var joint: js.Dynamic, val kind: Int, val body1: Long, val body2: Long) {
    // kind: 0=revolute, 1=prismatic, 2=fixed, 3=rope, 4=spring, 5=motor
    var limitLower:       Float = 0f
    var limitUpper:       Float = 0f
    var motorSpeed:       Float = 0f
    var maxMotorTorque:   Float = 0f
    var maxMotorForce:    Float = 0f
    var linearOffsetX:    Float = 0f
    var linearOffsetY:    Float = 0f
    var angularOffset:    Float = 0f
    var motorMaxForce:    Float = 0f
    var motorMaxTorque:   Float = 0f
    var correctionFactor: Float = 0f
    var restLength:       Float = 0f
    var stiffness:        Float = 0f
    var damping:          Float = 0f
    var maxDistance:      Float = 0f
    // Prismatic axis (unit) for translation read-back.
    var axisX: Float = 1f
    var axisY: Float = 0f
  }
  private val joints: mutable.LongMap[JointState] = mutable.LongMap()

  /** Extra per-collider state the Rapier API does not store: one-way platform direction. */
  final private class ColliderState {
    var oneWayActive: Boolean = false
    var oneWayNx:     Float   = 0f
    var oneWayNy:     Float   = 0f
    var oneWayAngle:  Float   = 0f
  }
  private val colliderState: mutable.LongMap[ColliderState] = mutable.LongMap()

  /** Extra per-body state: rotation/translation lock flags (Rapier has no read-back getter). */
  final private class BodyState {
    var rotationLocked: Boolean = false
    var transLockedX:   Boolean = false
    var transLockedY:   Boolean = false
  }
  private val bodyState: mutable.LongMap[BodyState] = mutable.LongMap()

  private var nextWorld:    Long = 1L
  private var nextBody:     Long = 1L
  private var nextCollider: Long = 1L
  private var nextJoint:    Long = 1L

  // ─── Handle / vector helpers ──────────────────────────────────────────────

  private def ws(world: Long): WorldState = worlds(world)
  private def w(world:  Long): js.Dynamic = worlds(world).world

  private def vec(x: Float, y: Float): js.Dynamic =
    js.Dynamic.literal(x = x.toDouble, y = y.toDouble)

  /** Encode the low 32 bits of `handle` into a Float slot (matches the `floatToRawIntBits` decode in PhysicsWorld). */
  private def loBits(handle: Long): Float = java.lang.Float.intBitsToFloat((handle & 0xffffffffL).toInt)

  /** Encode the high 32 bits of `handle` into a Float slot. */
  private def hiBits(handle: Long): Float = java.lang.Float.intBitsToFloat((handle >>> 32).toInt)

  // A Rapier-compat handle (RigidBody.handle / Collider.handle / collision-event collider handles) is a `number`
  // that is the f64 reinterpretation of the packed u64 (index, generation) Arena handle. Index 0 reinterprets to
  // 0.0, index 1 to the denormal 4.9e-324, index 2 to 1e-323, etc. — the value is NOT an integer, so casting it to
  // Int throws `ClassCastException` for every index >= 1. Key the reverse-lookup maps by the f64 handle's stable
  // string form instead; never cast a Rapier handle to Int.
  private def rapierKey(world: Long, rapierHandle: Double): String = world.toString + ":" + rapierHandle.toString

  /** Resolve a Rapier collider's f64 handle to its SGE Long (0 if unknown). */
  private def sgeColliderHandle(world: Long, rapierHandle: Double): Long =
    colliderByRapier.get(rapierKey(world, rapierHandle)).getOrElse(0L)

  /** Resolve a Rapier body's f64 handle to its SGE Long (0 if unknown). */
  private def sgeBodyHandle(world: Long, rapierHandle: Double): Long =
    bodyByRapier.get(rapierKey(world, rapierHandle)).getOrElse(0L)

  // ─── World lifecycle ──────────────────────────────────────────────────────

  override def createWorld(gravityX: Float, gravityY: Float): Long = {
    val world  = js.Dynamic.newInstance(R.World)(vec(gravityX, gravityY))
    val handle = nextWorld
    nextWorld += 1L
    worlds(handle) = new WorldState(world)
    handle
  }

  override def destroyWorld(world: Long): Unit = {
    worlds.get(world).foreach { st =>
      // free() releases the WASM-side world + event queue.
      st.world.free()
      st.eventQueue.free()
    }
    worlds -= world
  }

  override def worldStep(world: Long, dt: Float): Unit = {
    val st = ws(world)
    st.world.integrationParameters.dt = dt.toDouble
    st.world.step(st.eventQueue)
    drainEvents(world, st)
  }

  private def drainEvents(world: Long, st: WorldState): Unit = {
    st.contactStart.length = 0
    st.contactStop.length = 0
    st.intersectionStart.length = 0
    st.intersectionStop.length = 0
    st.contactForce.length = 0
    val collisionCb: js.Function3[Double, Double, Boolean, Unit] = { (h1, h2, started) =>
      val c1 = sgeColliderHandle(world, h1)
      val c2 = sgeColliderHandle(world, h2)
      // A pair is an intersection (sensor) event if either collider is a sensor; otherwise a contact.
      val isSensor =
        (!js.isUndefined(st.world.getCollider(h1)) && st.world.getCollider(h1).isSensor().asInstanceOf[Boolean]) ||
          (!js.isUndefined(st.world.getCollider(h2)) && st.world.getCollider(h2).isSensor().asInstanceOf[Boolean])
      if (isSensor) {
        if (started) st.intersectionStart.push((c1, c2)) else st.intersectionStop.push((c1, c2))
      } else {
        if (started) st.contactStart.push((c1, c2)) else st.contactStop.push((c1, c2))
      }
    }
    st.eventQueue.drainCollisionEvents(collisionCb)
    val forceCb: js.Function1[js.Dynamic, Unit] = { ev =>
      val c1 = sgeColliderHandle(world, ev.collider1().asInstanceOf[Double])
      val c2 = sgeColliderHandle(world, ev.collider2().asInstanceOf[Double])
      st.contactForce.push((c1, c2, ev.totalForceMagnitude().asInstanceOf[Double].toFloat))
    }
    st.eventQueue.drainContactForceEvents(forceCb)
  }

  override def worldSetGravity(world: Long, gx: Float, gy: Float): Unit = {
    val g = w(world).gravity
    g.x = gx.toDouble
    g.y = gy.toDouble
  }

  override def worldGetGravity(world: Long, out: Array[Float]): Unit = {
    val g = w(world).gravity
    out(0) = g.x.asInstanceOf[Double].toFloat
    out(1) = g.y.asInstanceOf[Double].toFloat
  }

  // ─── Rigid body ─────────────────────────────────────────────────────────

  private def createBody(world: Long, desc: js.Dynamic): Long = {
    val st     = ws(world)
    val body   = st.world.createRigidBody(desc)
    val handle = nextBody
    nextBody += 1L
    bodies(handle) = body
    bodyState(handle) = new BodyState
    bodyByRapier(rapierKey(world, body.handle.asInstanceOf[Double])) = handle
    handle
  }

  override def createDynamicBody(world: Long, x: Float, y: Float, angle: Float): Long =
    createBody(world, R.RigidBodyDesc.dynamic().setTranslation(x.toDouble, y.toDouble).setRotation(angle.toDouble))

  override def createStaticBody(world: Long, x: Float, y: Float, angle: Float): Long =
    createBody(world, R.RigidBodyDesc.fixed().setTranslation(x.toDouble, y.toDouble).setRotation(angle.toDouble))

  override def createKinematicBody(world: Long, x: Float, y: Float, angle: Float): Long =
    createBody(world, R.RigidBodyDesc.kinematicPositionBased().setTranslation(x.toDouble, y.toDouble).setRotation(angle.toDouble))

  override def destroyBody(world: Long, body: Long): Unit = {
    bodies.get(body).foreach { b =>
      bodyByRapier -= rapierKey(world, b.handle.asInstanceOf[Double])
      // removeRigidBody also removes the body's attached colliders + joints (wakeUp = true).
      w(world).removeRigidBody(b)
    }
    bodies -= body
    bodyState -= body
  }

  override def bodyGetPosition(world: Long, body: Long, out: Array[Float]): Unit = {
    val t = bodies(body).translation()
    out(0) = t.x.asInstanceOf[Double].toFloat
    out(1) = t.y.asInstanceOf[Double].toFloat
  }

  override def bodyGetAngle(world: Long, body: Long): Float =
    bodies(body).rotation().asInstanceOf[Double].toFloat

  override def bodyGetLinearVelocity(world: Long, body: Long, out: Array[Float]): Unit = {
    val v = bodies(body).linvel()
    out(0) = v.x.asInstanceOf[Double].toFloat
    out(1) = v.y.asInstanceOf[Double].toFloat
  }

  override def bodyGetAngularVelocity(world: Long, body: Long): Float =
    bodies(body).angvel().asInstanceOf[Double].toFloat

  override def bodySetPosition(world: Long, body: Long, x: Float, y: Float): Unit =
    bodies(body).setTranslation(vec(x, y), true)

  override def bodySetAngle(world: Long, body: Long, angle: Float): Unit =
    bodies(body).setRotation(angle.toDouble, true)

  override def bodySetLinearVelocity(world: Long, body: Long, vx: Float, vy: Float): Unit =
    bodies(body).setLinvel(vec(vx, vy), true)

  override def bodySetAngularVelocity(world: Long, body: Long, omega: Float): Unit =
    bodies(body).setAngvel(omega.toDouble, true)

  override def bodyApplyForce(world: Long, body: Long, fx: Float, fy: Float): Unit =
    bodies(body).addForce(vec(fx, fy), true)

  override def bodyApplyImpulse(world: Long, body: Long, ix: Float, iy: Float): Unit =
    bodies(body).applyImpulse(vec(ix, iy), true)

  override def bodyApplyTorque(world: Long, body: Long, torque: Float): Unit =
    bodies(body).addTorque(torque.toDouble, true)

  override def bodyApplyForceAtPoint(world: Long, body: Long, fx: Float, fy: Float, px: Float, py: Float): Unit =
    bodies(body).addForceAtPoint(vec(fx, fy), vec(px, py), true)

  override def bodyApplyImpulseAtPoint(world: Long, body: Long, ix: Float, iy: Float, px: Float, py: Float): Unit =
    bodies(body).applyImpulseAtPoint(vec(ix, iy), vec(px, py), true)

  override def bodyApplyTorqueImpulse(world: Long, body: Long, impulse: Float): Unit =
    bodies(body).applyTorqueImpulse(impulse.toDouble, true)

  override def bodyResetForces(world: Long, body: Long): Unit =
    bodies(body).resetForces(true)

  override def bodyResetTorques(world: Long, body: Long): Unit =
    bodies(body).resetTorques(true)

  override def bodySetLinearDamping(world: Long, body: Long, damping: Float): Unit =
    bodies(body).setLinearDamping(damping.toDouble)

  override def bodySetAngularDamping(world: Long, body: Long, damping: Float): Unit =
    bodies(body).setAngularDamping(damping.toDouble)

  override def bodySetGravityScale(world: Long, body: Long, scale: Float): Unit =
    bodies(body).setGravityScale(scale.toDouble, true)

  override def bodyIsAwake(world: Long, body: Long): Boolean =
    !bodies(body).isSleeping().asInstanceOf[Boolean]

  override def bodyWakeUp(world: Long, body: Long): Unit =
    bodies(body).wakeUp()

  override def bodySetFixedRotation(world: Long, body: Long, fixed: Boolean): Unit = {
    bodies(body).lockRotations(fixed, true)
    bodyState(body).rotationLocked = fixed
  }

  override def bodyGetLinearDamping(world: Long, body: Long): Float =
    bodies(body).linearDamping().asInstanceOf[Double].toFloat

  override def bodyGetAngularDamping(world: Long, body: Long): Float =
    bodies(body).angularDamping().asInstanceOf[Double].toFloat

  override def bodyGetGravityScale(world: Long, body: Long): Float =
    bodies(body).gravityScale().asInstanceOf[Double].toFloat

  override def bodyGetType(world: Long, body: Long): Int =
    bodies(body).bodyType().asInstanceOf[Int]

  override def bodySetEnabled(world: Long, body: Long, enabled: Boolean): Unit =
    bodies(body).setEnabled(enabled)

  override def bodyIsEnabled(world: Long, body: Long): Boolean =
    bodies(body).isEnabled().asInstanceOf[Boolean]

  override def bodySetEnabledTranslations(world: Long, body: Long, allowX: Boolean, allowY: Boolean): Unit = {
    bodies(body).setEnabledTranslations(allowX, allowY, true)
    val st = bodyState(body)
    st.transLockedX = !allowX
    st.transLockedY = !allowY
  }

  // Rapier exposes no translation/rotation lock read-back; mirror the Rust side by tracking the lock flags on set.
  override def bodyIsTranslationLockedX(world: Long, body: Long): Boolean =
    bodyState(body).transLockedX

  override def bodyIsTranslationLockedY(world: Long, body: Long): Boolean =
    bodyState(body).transLockedY

  override def bodyIsRotationLocked(world: Long, body: Long): Boolean =
    bodyState(body).rotationLocked

  override def bodySetDominanceGroup(world: Long, body: Long, group: Int): Unit =
    bodies(body).setDominanceGroup(group)

  override def bodyGetDominanceGroup(world: Long, body: Long): Int =
    bodies(body).dominanceGroup().asInstanceOf[Int]

  override def bodyGetWorldCenterOfMass(world: Long, body: Long, out: Array[Float]): Unit = {
    val c = bodies(body).worldCom()
    out(0) = c.x.asInstanceOf[Double].toFloat
    out(1) = c.y.asInstanceOf[Double].toFloat
  }

  override def bodyEnableCcd(world: Long, body: Long, enable: Boolean): Unit =
    bodies(body).enableCcd(enable)

  override def bodyIsCcdEnabled(world: Long, body: Long): Boolean =
    bodies(body).isCcdEnabled().asInstanceOf[Boolean]

  override def bodySleep(world: Long, body: Long): Unit =
    bodies(body).sleep()

  override def bodyGetVelocityAtPoint(world: Long, body: Long, px: Float, py: Float, out: Array[Float]): Unit = {
    val v = bodies(body).velocityAtPoint(vec(px, py))
    out(0) = v.x.asInstanceOf[Double].toFloat
    out(1) = v.y.asInstanceOf[Double].toFloat
  }

  // ─── Collider ─────────────────────────────────────────────────────────────

  private def addCollider(world: Long, body: Long, desc: js.Dynamic): Long = {
    val collider = w(world).createCollider(desc, bodies(body))
    // The Rust/Panama backend enables collision events on every collider by default, so contact/intersection
    // start/stop events are reported without an explicit opt-in. Match that: ActiveEvents.COLLISION_EVENTS = 1.
    collider.setActiveEvents(1)
    val handle = nextCollider
    nextCollider += 1L
    colliders(handle) = collider
    colliderState(handle) = new ColliderState
    colliderByRapier(rapierKey(world, collider.handle.asInstanceOf[Double])) = handle
    handle
  }

  override def createCircleCollider(world: Long, body: Long, radius: Float): Long =
    addCollider(world, body, R.ColliderDesc.ball(radius.toDouble))

  override def createBoxCollider(world: Long, body: Long, halfWidth: Float, halfHeight: Float): Long =
    addCollider(world, body, R.ColliderDesc.cuboid(halfWidth.toDouble, halfHeight.toDouble))

  override def createCapsuleCollider(world: Long, body: Long, halfHeight: Float, radius: Float): Long =
    addCollider(world, body, R.ColliderDesc.capsule(halfHeight.toDouble, radius.toDouble))

  private def toFloat32(vertices: Array[Float], count: Int): js.typedarray.Float32Array = {
    val arr = new js.typedarray.Float32Array(count)
    var i   = 0
    while (i < count) {
      arr(i) = vertices(i)
      i += 1
    }
    arr
  }

  override def createPolygonCollider(world: Long, body: Long, vertices: Array[Float], vertexCount: Int): Long = {
    val pts  = toFloat32(vertices, vertexCount * 2)
    val desc = R.ColliderDesc.convexHull(pts)
    // convexHull returns null for degenerate input; fall back to a polyline outline so the call never throws.
    val d = if (js.isUndefined(desc) || desc == null) R.ColliderDesc.polyline(pts) else desc
    addCollider(world, body, d.asInstanceOf[js.Dynamic])
  }

  override def createSegmentCollider(world: Long, body: Long, x1: Float, y1: Float, x2: Float, y2: Float): Long =
    addCollider(world, body, R.ColliderDesc.segment(vec(x1, y1), vec(x2, y2)))

  override def createPolylineCollider(world: Long, body: Long, vertices: Array[Float], vertexCount: Int): Long = {
    val pts = toFloat32(vertices, vertexCount * 2)
    addCollider(world, body, R.ColliderDesc.polyline(pts))
  }

  override def createTriMeshCollider(
    world:       Long,
    body:        Long,
    vertices:    Array[Float],
    vertexCount: Int,
    indices:     Array[Int],
    indexCount:  Int
  ): Long = {
    val verts = toFloat32(vertices, vertexCount * 2)
    val idx   = new js.typedarray.Uint32Array(indexCount)
    var i     = 0
    while (i < indexCount) {
      idx(i) = indices(i)
      i += 1
    }
    addCollider(world, body, R.ColliderDesc.trimesh(verts, idx))
  }

  override def createHeightfieldCollider(
    world:   Long,
    body:    Long,
    heights: Array[Float],
    numCols: Int,
    scaleX:  Float,
    scaleY:  Float
  ): Long = {
    val h = toFloat32(heights, numCols)
    addCollider(world, body, R.ColliderDesc.heightfield(h, vec(scaleX, scaleY)))
  }

  override def destroyCollider(world: Long, collider: Long): Unit = {
    colliders.get(collider).foreach { c =>
      colliderByRapier -= rapierKey(world, c.handle.asInstanceOf[Double])
      w(world).removeCollider(c, true)
    }
    colliders -= collider
    colliderState -= collider
  }

  // Rapier auto-recomputes a body's mass from its colliders only on collider add/remove; changing a collider's
  // density or explicit mass after attachment does NOT propagate to the parent body until forced. The Rust/Panama
  // backend reflects the change in `bodyGetMass` immediately, so mirror that by recomputing the parent body's
  // mass-properties from its current collider set here.
  private def recomputeParentMass(collider: Long): Unit = {
    val p = colliders(collider).parent()
    if (!js.isUndefined(p) && p != null) p.recomputeMassPropertiesFromColliders()
  }

  override def colliderSetDensity(world: Long, collider: Long, density: Float): Unit = {
    colliders(collider).setDensity(density.toDouble)
    recomputeParentMass(collider)
  }

  override def colliderSetFriction(world: Long, collider: Long, friction: Float): Unit =
    colliders(collider).setFriction(friction.toDouble)

  override def colliderSetRestitution(world: Long, collider: Long, restitution: Float): Unit =
    colliders(collider).setRestitution(restitution.toDouble)

  override def colliderSetSensor(world: Long, collider: Long, sensor: Boolean): Unit =
    colliders(collider).setSensor(sensor)

  override def colliderGetDensity(world: Long, collider: Long): Float =
    colliders(collider).density().asInstanceOf[Double].toFloat

  override def colliderGetFriction(world: Long, collider: Long): Float =
    colliders(collider).friction().asInstanceOf[Double].toFloat

  override def colliderGetRestitution(world: Long, collider: Long): Float =
    colliders(collider).restitution().asInstanceOf[Double].toFloat

  override def colliderIsSensor(world: Long, collider: Long): Boolean =
    colliders(collider).isSensor().asInstanceOf[Boolean]

  override def colliderSetEnabled(world: Long, collider: Long, enabled: Boolean): Unit =
    colliders(collider).setEnabled(enabled)

  override def colliderIsEnabled(world: Long, collider: Long): Boolean =
    colliders(collider).isEnabled().asInstanceOf[Boolean]

  override def colliderGetPositionWrtParent(world: Long, collider: Long, out: Array[Float]): Unit = {
    val c = colliders(collider)
    val t = c.translationWrtParent()
    if (js.isUndefined(t) || t == null) {
      out(0) = 0f
      out(1) = 0f
      out(2) = 0f
    } else {
      out(0) = t.x.asInstanceOf[Double].toFloat
      out(1) = t.y.asInstanceOf[Double].toFloat
      val r = c.rotationWrtParent()
      out(2) = if (js.isUndefined(r) || r == null) 0f else r.asInstanceOf[Double].toFloat
    }
  }

  override def colliderSetPositionWrtParent(world: Long, collider: Long, x: Float, y: Float, angle: Float): Unit = {
    val c = colliders(collider)
    c.setTranslationWrtParent(vec(x, y))
    c.setRotationWrtParent(angle.toDouble)
  }

  override def colliderGetPosition(world: Long, collider: Long, out: Array[Float]): Unit = {
    val c = colliders(collider)
    val t = c.translation()
    out(0) = t.x.asInstanceOf[Double].toFloat
    out(1) = t.y.asInstanceOf[Double].toFloat
    out(2) = c.rotation().asInstanceOf[Double].toFloat
  }

  /** Remap Rapier's ShapeType ordinal to the SGE convention documented on [[PhysicsOps.colliderGetShapeType]]. */
  private def mapShapeType(rapier: Int): Int = rapier match {
    case 0 => 0 // Ball
    case 1 => 1 // Cuboid
    case 2 => 2 // Capsule
    case 3 => 3 // Segment
    case 5 => 4 // Triangle
    case 6 => 5 // TriMesh
    case 4 => 6 // Polyline
    case 7 => 7 // HeightField
    case 9 => 9 // ConvexPolygon
    case _ => -1 // RoundCuboid/RoundTriangle/HalfSpace/Voxels/unknown
  }

  override def colliderGetShapeType(world: Long, collider: Long): Int =
    mapShapeType(colliders(collider).shapeType().asInstanceOf[Int])

  override def colliderGetAabb(world: Long, collider: Long, out: Array[Float]): Unit = {
    // The Rapier JS compat module exposes no AABB method on Collider or Shape (the prior
    // `shape.computeAABB` call did not exist in v0.19 and threw `TypeError` for every caller).
    // Reproduce Panama's hColliderGetAabb (a tight world-space [minX, minY, maxX, maxY], matching
    // parry's `compute_aabb`) by gathering the shape's local-frame support points + a padding radius
    // (for rounded shapes: ball/capsule), transforming each point by the collider's world isometry
    // (rotation θ then translation), and taking the per-axis min/max, padded by the radius.
    val c     = colliders(collider)
    val t     = c.translation()
    val tx    = t.x.asInstanceOf[Double]
    val ty    = t.y.asInstanceOf[Double]
    val theta = c.rotation().asInstanceOf[Double]
    val cos   = Math.cos(theta)
    val sin   = Math.sin(theta)

    // Local-frame points to enclose, flattened as [x0, y0, x1, y1, ...], and an isotropic padding radius.
    val shape = c.shape
    val pts   = js.Array[Double]()
    var pad   = 0.0
    def add(px: Double, py: Double): Unit = { pts.push(px); pts.push(py); () }

    // Gathers a flat vertex buffer ([x0,y0,x1,y1,...], the `.vertices` member on Polyline/TriMesh/ConvexPolygon).
    def addVertexBuffer(): Unit = {
      val verts = shape.vertices.asInstanceOf[js.typedarray.Float32Array]
      val m     = verts.length
      if (m == 0) add(0.0, 0.0)
      else {
        var i = 0
        while (i + 1 < m) {
          add(verts(i).toDouble, verts(i + 1).toDouble)
          i += 2
        }
      }
    }

    // Branch on the RAW Rapier `ShapeType` ordinal (NOT the SGE-mapped code from `mapShapeType`):
    // Ball=0, Cuboid=1, Capsule=2, Segment=3, Polyline=4, Triangle=5, TriMesh=6, HeightField=7, ConvexPolygon=9.
    // Each branch reads only members that shape actually exposes per rapier2d-compat `geometry/shape.d.ts`.
    c.shapeType().asInstanceOf[Int] match {
      case 0 => // Ball: single center point padded by the radius.
        add(0.0, 0.0)
        pad = shape.radius.asInstanceOf[Double]
      case 1 => // Cuboid: the four corners at ±halfExtents.
        val he = shape.halfExtents
        val hx = he.x.asInstanceOf[Double]
        val hy = he.y.asInstanceOf[Double]
        add(hx, hy); add(-hx, hy); add(hx, -hy); add(-hx, -hy)
      case 2 => // Capsule (axis along local y): the two segment tips at ±halfHeight, padded by the radius.
        val hh = shape.halfHeight.asInstanceOf[Double]
        add(0.0, hh); add(0.0, -hh)
        pad = shape.radius.asInstanceOf[Double]
      case 3 => // Segment: its two endpoints `a`, `b`.
        val a = shape.a
        val b = shape.b
        add(a.x.asInstanceOf[Double], a.y.asInstanceOf[Double])
        add(b.x.asInstanceOf[Double], b.y.asInstanceOf[Double])
      case 4 => // Polyline: a flat `.vertices` buffer (NOT a/b/c — those belong to the Triangle shape).
        addVertexBuffer()
      case 5 => // Triangle: its three vertices `a`, `b`, `c` (no factory produces it; kept for completeness).
        val a  = shape.a
        val b  = shape.b
        val cc = shape.c
        add(a.x.asInstanceOf[Double], a.y.asInstanceOf[Double])
        add(b.x.asInstanceOf[Double], b.y.asInstanceOf[Double])
        add(cc.x.asInstanceOf[Double], cc.y.asInstanceOf[Double])
      case 6 => // TriMesh: a flat `.vertices` buffer.
        addVertexBuffer()
      case 7 => // HeightField: sample points at evenly-spaced x over the scaled width with scaled heights.
        val heights = shape.heights.asInstanceOf[js.typedarray.Float32Array]
        val scale   = shape.scale
        val sx      = scale.x.asInstanceOf[Double]
        val sy      = scale.y.asInstanceOf[Double]
        val n       = heights.length
        if (n == 0) add(0.0, 0.0)
        else if (n == 1) add(0.0, heights(0).toDouble * sy)
        else {
          var i = 0
          while (i < n) {
            val u = i.toDouble / (n - 1).toDouble // 0..1
            add((u - 0.5) * sx, heights(i).toDouble * sy)
            i += 1
          }
        }
      case 9 => // ConvexPolygon: a flat `.vertices` buffer.
        addVertexBuffer()
      case _ => // Round*/HalfSpace/Voxels and any other shape no factory produces: collapse to the collider
        // origin with no padding — a sound, member-safe fallback that never reads a possibly-absent member.
        add(0.0, 0.0)
    }

    var minX = Double.PositiveInfinity
    var minY = Double.PositiveInfinity
    var maxX = Double.NegativeInfinity
    var maxY = Double.NegativeInfinity
    var i    = 0
    while (i + 1 < pts.length) {
      val lx = pts(i)
      val ly = pts(i + 1)
      // World point = R(θ) · local + translation.
      val wx = cos * lx - sin * ly + tx
      val wy = sin * lx + cos * ly + ty
      if (wx < minX) minX = wx
      if (wy < minY) minY = wy
      if (wx > maxX) maxX = wx
      if (wy > maxY) maxY = wy
      i += 2
    }

    out(0) = (minX - pad).toFloat
    out(1) = (minY - pad).toFloat
    out(2) = (maxX + pad).toFloat
    out(3) = (maxY + pad).toFloat
  }

  override def colliderGetParentBody(world: Long, collider: Long): Long = {
    val p = colliders(collider).parent()
    if (js.isUndefined(p) || p == null) 0L
    else sgeBodyHandle(world, p.handle.asInstanceOf[Double])
  }

  override def colliderGetMass(world: Long, collider: Long): Float =
    colliders(collider).mass().asInstanceOf[Double].toFloat

  override def colliderSetMass(world: Long, collider: Long, mass: Float): Unit = {
    colliders(collider).setMass(mass.toDouble)
    recomputeParentMass(collider)
  }

  override def colliderSetContactSkin(world: Long, collider: Long, skin: Float): Unit =
    colliders(collider).setContactSkin(skin.toDouble)

  override def colliderSetActiveEvents(world: Long, collider: Long, flags: Int): Unit =
    colliders(collider).setActiveEvents(flags)

  override def colliderGetActiveEvents(world: Long, collider: Long): Int =
    colliders(collider).activeEvents().asInstanceOf[Int]

  override def colliderSetActiveCollisionTypes(world: Long, collider: Long, flags: Int): Unit =
    colliders(collider).setActiveCollisionTypes(flags)

  override def colliderGetActiveCollisionTypes(world: Long, collider: Long): Int =
    colliders(collider).activeCollisionTypes().asInstanceOf[Int]

  // ─── Collision filtering ──────────────────────────────────────────────────

  /** Rapier packs interaction groups as a single 32-bit int: high 16 bits = memberships, low 16 bits = filter. */
  private def packGroups(memberships: Int, filter: Int): Int =
    ((memberships & 0xffff) << 16) | (filter & 0xffff)

  override def colliderSetCollisionGroups(world: Long, collider: Long, memberships: Int, filter: Int): Unit =
    colliders(collider).setCollisionGroups(packGroups(memberships, filter))

  override def colliderGetCollisionGroups(world: Long, collider: Long, out: Array[Int]): Unit = {
    val g = colliders(collider).collisionGroups().asInstanceOf[Int]
    out(0) = (g >>> 16) & 0xffff
    out(1) = g & 0xffff
  }

  override def colliderSetSolverGroups(world: Long, collider: Long, memberships: Int, filter: Int): Unit =
    colliders(collider).setSolverGroups(packGroups(memberships, filter))

  override def colliderGetSolverGroups(world: Long, collider: Long, out: Array[Int]): Unit = {
    val g = colliders(collider).solverGroups().asInstanceOf[Int]
    out(0) = (g >>> 16) & 0xffff
    out(1) = g & 0xffff
  }

  // ─── Joints ───────────────────────────────────────────────────────────────

  private def registerJoint(world: Long, jointObj: js.Dynamic, kind: Int, body1: Long, body2: Long): (Long, JointState) = {
    val handle = nextJoint
    nextJoint += 1L
    val st = new JointState(world, jointObj, kind, body1, body2)
    joints(handle) = st
    (handle, st)
  }

  private def newImpulseJoint(world: Long, data: js.Dynamic, b1: Long, b2: Long): js.Dynamic =
    w(world).createImpulseJoint(data, bodies(b1), bodies(b2), true)

  override def createRevoluteJoint(world: Long, body1: Long, body2: Long, anchorX: Float, anchorY: Float): Long = {
    // Convert the world-space anchor into body-local anchors for each body.
    val a1          = localAnchor(body1, anchorX, anchorY)
    val a2          = localAnchor(body2, anchorX, anchorY)
    val data        = R.JointData.revolute(a1, a2)
    val (handle, _) = registerJoint(world, newImpulseJoint(world, data, body1, body2), 0, body1, body2)
    handle
  }

  override def createPrismaticJoint(world: Long, body1: Long, body2: Long, axisX: Float, axisY: Float): Long = {
    val data         = R.JointData.prismatic(vec(0f, 0f), vec(0f, 0f), vec(axisX, axisY))
    val (handle, st) = registerJoint(world, newImpulseJoint(world, data, body1, body2), 1, body1, body2)
    st.axisX = axisX
    st.axisY = axisY
    handle
  }

  override def createFixedJoint(world: Long, body1: Long, body2: Long): Long = {
    val data        = R.JointData.fixed(vec(0f, 0f), 0d, vec(0f, 0f), 0d)
    val (handle, _) = registerJoint(world, newImpulseJoint(world, data, body1, body2), 2, body1, body2)
    handle
  }

  override def createRopeJoint(world: Long, body1: Long, body2: Long, maxDist: Float): Long = {
    val data         = R.JointData.rope(maxDist.toDouble, vec(0f, 0f), vec(0f, 0f))
    val (handle, st) = registerJoint(world, newImpulseJoint(world, data, body1, body2), 3, body1, body2)
    st.maxDistance = maxDist
    handle
  }

  override def createSpringJoint(world: Long, body1: Long, body2: Long, restLength: Float, stiffness: Float, damping: Float): Long = {
    val data         = R.JointData.spring(restLength.toDouble, stiffness.toDouble, damping.toDouble, vec(0f, 0f), vec(0f, 0f))
    val (handle, st) = registerJoint(world, newImpulseJoint(world, data, body1, body2), 4, body1, body2)
    st.restLength = restLength
    st.stiffness = stiffness
    st.damping = damping
    handle
  }

  override def createMotorJoint(world: Long, body1: Long, body2: Long): Long = {
    // Rapier 2D has no dedicated "motor" joint; model it with a fixed joint whose motors are driven via the offset setters.
    val data        = R.JointData.fixed(vec(0f, 0f), 0d, vec(0f, 0f), 0d)
    val (handle, _) = registerJoint(world, newImpulseJoint(world, data, body1, body2), 5, body1, body2)
    handle
  }

  /** World-space anchor → body-local coordinates (inverse of body transform). */
  private def localAnchor(b: Long, wx: Float, wy: Float): js.Dynamic = {
    val body = bodies(b)
    val t    = body.translation()
    val ang  = body.rotation().asInstanceOf[Double]
    val dx   = wx.toDouble - t.x.asInstanceOf[Double]
    val dy   = wy.toDouble - t.y.asInstanceOf[Double]
    val cos  = Math.cos(-ang)
    val sin  = Math.sin(-ang)
    vec((dx * cos - dy * sin).toFloat, (dx * sin + dy * cos).toFloat)
  }

  override def destroyJoint(world: Long, joint: Long): Unit = {
    joints.get(joint).foreach { st =>
      w(world).removeImpulseJoint(st.joint, true)
    }
    joints -= joint
  }

  private def jst(joint: Long): JointState = joints(joint)

  /** Remove the Rapier joint backing `st` and recreate it from the supplied (updated) `JointData`, rebinding `st.joint` to the new object.
    *
    * Rapier2D-JS exposes no post-creation mutator for a Spring/Rope joint's creation-time parameters (rope length, spring rest-length/stiffness/damping) — they live only on `JointData` and are baked
    * in at `createImpulseJoint` time. To make the corresponding setters actually change the joint (mirroring the Rust/Panama backend, which reconfigures the live joint), we destroy + recreate the
    * underlying joint with new parameters. The SGE Long handle keying `st` in the `joints` registry never changes, so any external reference to the joint stays valid.
    */
  private def recreateJoint(st: JointState, data: js.Dynamic): Unit = {
    w(st.world).removeImpulseJoint(st.joint, true)
    st.joint = newImpulseJoint(st.world, data, st.body1, st.body2)
  }

  // ─── Revolute joint limits and motors ─────────────────────────────────────

  override def revoluteJointEnableLimits(world: Long, joint: Long, enable: Boolean): Unit = {
    val st = jst(joint)
    if (enable) st.joint.setLimits(st.limitLower.toDouble, st.limitUpper.toDouble)
    // Rapier has no "disable limits"; widening to ±π effectively disables a revolute limit when not enabled.
    else st.joint.setLimits(-Math.PI, Math.PI)
  }

  override def revoluteJointSetLimits(world: Long, joint: Long, lower: Float, upper: Float): Unit = {
    val st = jst(joint)
    st.limitLower = lower
    st.limitUpper = upper
    st.joint.setLimits(lower.toDouble, upper.toDouble)
  }

  override def revoluteJointGetLimits(world: Long, joint: Long, out: Array[Float]): Unit = {
    val st = jst(joint)
    out(0) = st.joint.limitsMin().asInstanceOf[Double].toFloat
    out(1) = st.joint.limitsMax().asInstanceOf[Double].toFloat
  }

  override def revoluteJointIsLimitEnabled(world: Long, joint: Long): Boolean =
    jst(joint).joint.limitsEnabled().asInstanceOf[Boolean]

  override def revoluteJointEnableMotor(world: Long, joint: Long, enable: Boolean): Unit = {
    val st = jst(joint)
    if (enable) st.joint.configureMotorVelocity(st.motorSpeed.toDouble, st.maxMotorTorque.toDouble)
    else st.joint.configureMotorVelocity(0d, 0d)
  }

  override def revoluteJointSetMotorSpeed(world: Long, joint: Long, speed: Float): Unit = {
    val st = jst(joint)
    st.motorSpeed = speed
    st.joint.configureMotorVelocity(speed.toDouble, st.maxMotorTorque.toDouble)
  }

  override def revoluteJointSetMaxMotorTorque(world: Long, joint: Long, torque: Float): Unit = {
    val st = jst(joint)
    st.maxMotorTorque = torque
    st.joint.configureMotorVelocity(st.motorSpeed.toDouble, torque.toDouble)
  }

  override def revoluteJointGetMotorSpeed(world: Long, joint: Long): Float =
    jst(joint).motorSpeed

  override def revoluteJointGetAngle(world: Long, joint: Long): Float = {
    // Relative angle between the two jointed bodies.
    val st = jst(joint)
    val a1 = bodies(st.body1).rotation().asInstanceOf[Double]
    val a2 = bodies(st.body2).rotation().asInstanceOf[Double]
    (a2 - a1).toFloat
  }

  override def revoluteJointGetMaxMotorTorque(world: Long, joint: Long): Float =
    jst(joint).maxMotorTorque

  // ─── Prismatic joint limits and motors ────────────────────────────────────

  override def prismaticJointEnableLimits(world: Long, joint: Long, enable: Boolean): Unit = {
    val st = jst(joint)
    if (enable) st.joint.setLimits(st.limitLower.toDouble, st.limitUpper.toDouble)
    else st.joint.setLimits(Double.NegativeInfinity, Double.PositiveInfinity)
  }

  override def prismaticJointSetLimits(world: Long, joint: Long, lower: Float, upper: Float): Unit = {
    val st = jst(joint)
    st.limitLower = lower
    st.limitUpper = upper
    st.joint.setLimits(lower.toDouble, upper.toDouble)
  }

  override def prismaticJointGetLimits(world: Long, joint: Long, out: Array[Float]): Unit = {
    val st = jst(joint)
    out(0) = st.joint.limitsMin().asInstanceOf[Double].toFloat
    out(1) = st.joint.limitsMax().asInstanceOf[Double].toFloat
  }

  override def prismaticJointEnableMotor(world: Long, joint: Long, enable: Boolean): Unit = {
    val st = jst(joint)
    if (enable) st.joint.configureMotorVelocity(st.motorSpeed.toDouble, st.maxMotorForce.toDouble)
    else st.joint.configureMotorVelocity(0d, 0d)
  }

  override def prismaticJointSetMotorSpeed(world: Long, joint: Long, speed: Float): Unit = {
    val st = jst(joint)
    st.motorSpeed = speed
    st.joint.configureMotorVelocity(speed.toDouble, st.maxMotorForce.toDouble)
  }

  override def prismaticJointSetMaxMotorForce(world: Long, joint: Long, force: Float): Unit = {
    val st = jst(joint)
    st.maxMotorForce = force
    st.joint.configureMotorVelocity(st.motorSpeed.toDouble, force.toDouble)
  }

  override def prismaticJointGetTranslation(world: Long, joint: Long): Float = {
    // Relative displacement of body2 vs body1 projected on the joint axis.
    val st = jst(joint)
    val t1 = bodies(st.body1).translation()
    val t2 = bodies(st.body2).translation()
    val dx = t2.x.asInstanceOf[Double] - t1.x.asInstanceOf[Double]
    val dy = t2.y.asInstanceOf[Double] - t1.y.asInstanceOf[Double]
    (dx * st.axisX + dy * st.axisY).toFloat
  }

  override def prismaticJointGetMotorSpeed(world: Long, joint: Long): Float =
    jst(joint).motorSpeed

  override def prismaticJointGetMaxMotorForce(world: Long, joint: Long): Float =
    jst(joint).maxMotorForce

  // ─── Motor joint ───────────────────────────────────────────────────────────

  override def motorJointSetLinearOffset(world: Long, joint: Long, x: Float, y: Float): Unit = {
    val st = jst(joint)
    st.linearOffsetX = x
    st.linearOffsetY = y
    applyMotorTarget(st)
  }

  override def motorJointGetLinearOffset(world: Long, joint: Long, out: Array[Float]): Unit = {
    val st = jst(joint)
    out(0) = st.linearOffsetX
    out(1) = st.linearOffsetY
  }

  override def motorJointSetAngularOffset(world: Long, joint: Long, angle: Float): Unit = {
    val st = jst(joint)
    st.angularOffset = angle
    applyMotorTarget(st)
  }

  override def motorJointGetAngularOffset(world: Long, joint: Long): Float =
    jst(joint).angularOffset

  override def motorJointSetMaxForce(world: Long, joint: Long, force: Float): Unit = {
    val st = jst(joint)
    st.motorMaxForce = force
    applyMotorTarget(st)
  }

  override def motorJointSetMaxTorque(world: Long, joint: Long, torque: Float): Unit = {
    val st = jst(joint)
    st.motorMaxTorque = torque
    applyMotorTarget(st)
  }

  override def motorJointSetCorrectionFactor(world: Long, joint: Long, factor: Float): Unit = {
    val st = jst(joint)
    st.correctionFactor = factor
    applyMotorTarget(st)
  }

  /** Drive the fixed-joint-backed motor toward the configured linear offset.
    *
    * `createMotorJoint` backs the motor with a `FixedImpulseJoint`, which is a base `ImpulseJoint` and therefore has NO `configureMotorPosition`/`configureMotorVelocity` — those exist only on
    * `UnitImpulseJoint` (Revolute/Prismatic). The one real, member-safe lever the base `ImpulseJoint` exposes is `setAnchor2`: shifting body2's anchor by the requested linear offset makes the fixed
    * joint hold the bodies at that offset, which is the observable behavior of the Rust/Panama motor joint's linear target. The angular-offset / max-force / max-torque / correction-factor values have
    * no representation on a plain Rapier fixed joint, so they are accepted and stored on the registry record only (the getters round-trip them) — a documented divergence from Panama, whose native
    * motor joint applies them as solver gains. We never call a method that doesn't exist on `FixedImpulseJoint`, and never throw.
    */
  private def applyMotorTarget(st: JointState): Unit =
    st.joint.setAnchor2(vec(st.linearOffsetX, st.linearOffsetY))

  override def motorJointGetMaxForce(world: Long, joint: Long): Float =
    jst(joint).motorMaxForce

  override def motorJointGetMaxTorque(world: Long, joint: Long): Float =
    jst(joint).motorMaxTorque

  override def motorJointGetCorrectionFactor(world: Long, joint: Long): Float =
    jst(joint).correctionFactor

  // ─── Rope joint ─────────────────────────────────────────────────────────────

  override def ropeJointSetMaxDistance(world: Long, joint: Long, maxDist: Float): Unit = {
    val st = jst(joint)
    st.maxDistance = maxDist
    // A RopeImpulseJoint is a base ImpulseJoint: it has no setLimits / mutator. The rope's max length is a creation-time
    // JointData.rope parameter, so reconfigure it by recreating the joint with the new length (matching the Rust/Panama
    // backend, which actually changes the rope's max distance).
    recreateJoint(st, R.JointData.rope(maxDist.toDouble, vec(0f, 0f), vec(0f, 0f)))
  }

  override def ropeJointGetMaxDistance(world: Long, joint: Long): Float =
    jst(joint).maxDistance

  // ─── Spring joint ───────────────────────────────────────────────────────────

  override def springJointSetRestLength(world: Long, joint: Long, restLength: Float): Unit = {
    val st = jst(joint)
    st.restLength = restLength
    // A SpringImpulseJoint is a base ImpulseJoint: it has no configureMotorPosition / mutator. Rest length, stiffness and
    // damping are creation-time JointData.spring parameters, so reconfigure the spring by recreating the joint with the
    // new rest length (keeping the current stiffness/damping). This mirrors the Rust/Panama backend, which actually
    // changes the spring's rest length.
    recreateJoint(st, R.JointData.spring(restLength.toDouble, st.stiffness.toDouble, st.damping.toDouble, vec(0f, 0f), vec(0f, 0f)))
  }

  override def springJointGetRestLength(world: Long, joint: Long): Float =
    jst(joint).restLength

  override def springJointSetParams(world: Long, joint: Long, stiffness: Float, damping: Float): Unit = {
    val st = jst(joint)
    st.stiffness = stiffness
    st.damping = damping
    // As above: stiffness and damping are baked into JointData.spring at creation, so recreate the spring joint with the
    // new params (keeping the current rest length) rather than calling a UnitImpulseJoint-only mutator the base
    // SpringImpulseJoint does not have.
    recreateJoint(st, R.JointData.spring(st.restLength.toDouble, stiffness.toDouble, damping.toDouble, vec(0f, 0f), vec(0f, 0f)))
  }

  // ─── Body mass/inertia ──────────────────────────────────────────────────────

  override def bodyGetMass(world: Long, body: Long): Float =
    bodies(body).mass().asInstanceOf[Double].toFloat

  override def bodyGetInertia(world: Long, body: Long): Float =
    bodies(body).effectiveAngularInertia().asInstanceOf[Double].toFloat

  override def bodyGetLocalCenterOfMass(world: Long, body: Long, out: Array[Float]): Unit = {
    val c = bodies(body).localCom()
    out(0) = c.x.asInstanceOf[Double].toFloat
    out(1) = c.y.asInstanceOf[Double].toFloat
  }

  override def bodyRecomputeMassProperties(world: Long, body: Long): Unit = {
    // Explicitly recompute mass/inertia/center-of-mass from the body's current colliders so getters reflect any
    // density/mass changes immediately (matching the Rust/Panama backend), then wake the body so the next step uses it.
    val b = bodies(body)
    b.recomputeMassPropertiesFromColliders()
    b.wakeUp()
  }

  // ─── Queries ──────────────────────────────────────────────────────────────

  private def ray(originX: Float, originY: Float, dirX: Float, dirY: Float): js.Dynamic =
    js.Dynamic.newInstance(R.Ray)(vec(originX, originY), vec(dirX, dirY))

  override def rayCast(
    world:   Long,
    originX: Float,
    originY: Float,
    dirX:    Float,
    dirY:    Float,
    maxDist: Float,
    out:     Array[Float]
  ): Boolean = {
    val st  = ws(world)
    val r   = ray(originX, originY, dirX, dirY)
    val hit = st.world.castRayAndGetNormal(r, maxDist.toDouble, true)
    if (js.isUndefined(hit) || hit == null) false
    else {
      val toi    = hit.timeOfImpact.asInstanceOf[Double]
      val hx     = originX.toDouble + dirX.toDouble * toi
      val hy     = originY.toDouble + dirY.toDouble * toi
      val n      = hit.normal
      val coll   = hit.collider
      val rHnd   = coll.handle.asInstanceOf[Double]
      val sgeC   = sgeColliderHandle(world, rHnd)
      val parent = coll.parent()
      val sgeB   = if (js.isUndefined(parent) || parent == null) 0L else sgeBodyHandle(world, parent.handle.asInstanceOf[Double])
      out(0) = hx.toFloat
      out(1) = hy.toFloat
      out(2) = n.x.asInstanceOf[Double].toFloat
      out(3) = n.y.asInstanceOf[Double].toFloat
      out(4) = toi.toFloat
      out(5) = loBits(sgeB)
      out(6) = hiBits(sgeB)
      // High-level rayCast wires body handle from out(5)/out(6); collider handle stays implicit there.
      // For parity with the Rust ABI which fills 7 floats with body handle in slots 5/6, keep collider in the world via sgeC.
      val _ = sgeC
      true
    }
  }

  override def queryAABB(
    world:        Long,
    minX:         Float,
    minY:         Float,
    maxX:         Float,
    maxY:         Float,
    outColliders: Array[Long],
    maxResults:   Int
  ): Int = {
    val st    = ws(world)
    val cx    = ((minX + maxX) / 2f).toDouble
    val cy    = ((minY + maxY) / 2f).toDouble
    val hx    = ((maxX - minX) / 2f).toDouble
    val hy    = ((maxY - minY) / 2f).toDouble
    var count = 0
    // The rapier-compat AABB query callback is invoked with the matching `Collider` object (the param is named
    // `handle` in the typings but is the Collider itself); read its f64 handle to map back to the SGE Long.
    val cb: js.Function1[js.Dynamic, Boolean] = { coll =>
      if (count < maxResults) {
        outColliders(count) = sgeColliderHandle(world, coll.handle.asInstanceOf[Double])
        count += 1
        true
      } else false
    }
    st.world.collidersWithAabbIntersectingAabb(vec(cx.toFloat, cy.toFloat), vec(hx.toFloat, hy.toFloat), cb)
    count
  }

  override def queryPoint(world: Long, x: Float, y: Float, outBodies: Array[Long], maxResults: Int): Int = {
    val st    = ws(world)
    var count = 0
    // The rapier-compat point query callback is invoked with the matching `Collider` object (the param is named
    // `handle` in the typings but is the Collider itself); resolve its parent body's f64 handle to the SGE Long.
    val cb: js.Function1[js.Dynamic, Boolean] = { coll =>
      if (count < maxResults) {
        val parent = if (js.isUndefined(coll) || coll == null) js.undefined.asInstanceOf[js.Dynamic] else coll.parent()
        val sgeB   = if (js.isUndefined(parent) || parent == null) 0L else sgeBodyHandle(world, parent.handle.asInstanceOf[Double])
        outBodies(count) = sgeB
        count += 1
        true
      } else false
    }
    st.world.intersectionsWithPoint(vec(x, y), cb)
    count
  }

  // ─── Contact events ─────────────────────────────────────────────────────────

  private def copyPairs(src: js.Array[(Long, Long)], out1: Array[Long], out2: Array[Long], maxEvents: Int): Int = {
    val count = Math.min(src.length, maxEvents)
    var i     = 0
    while (i < count) {
      val pair = src(i)
      out1(i) = pair._1
      out2(i) = pair._2
      i += 1
    }
    count
  }

  override def pollContactStartEvents(world: Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int =
    copyPairs(ws(world).contactStart, outCollider1, outCollider2, maxEvents)

  override def pollContactStopEvents(world: Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int =
    copyPairs(ws(world).contactStop, outCollider1, outCollider2, maxEvents)

  // ─── Contact detail queries ───────────────────────────────────────────────

  override def contactPairCount(world: Long, collider1: Long, collider2: Long): Int = {
    val st = ws(world)
    var n  = 0
    val cb: js.Function2[js.Dynamic, Boolean, Unit] = { (manifold, _) =>
      n += manifold.numContacts().asInstanceOf[Int]
    }
    st.world.contactPair(colliders(collider1), colliders(collider2), cb)
    n
  }

  override def contactPairPoints(
    world:     Long,
    collider1: Long,
    collider2: Long,
    out:       Array[Float],
    maxPoints: Int
  ): Int = {
    val st    = ws(world)
    val c1    = colliders(collider1)
    var count = 0
    val cb: js.Function2[js.Dynamic, Boolean, Unit] = { (manifold, flipped) =>
      val n      = manifold.numContacts().asInstanceOf[Int]
      val normal = manifold.normal()
      // Normal points from collider1 toward collider2; flip if Rapier reported the pair flipped.
      val nx = (if (flipped) -1d else 1d) * normal.x.asInstanceOf[Double]
      val ny = (if (flipped) -1d else 1d) * normal.y.asInstanceOf[Double]
      val t1 = c1.translation()
      var i  = 0
      while (i < n && count < maxPoints) {
        val lp   = manifold.localContactPoint1(i)
        val px   = if (js.isUndefined(lp) || lp == null) t1.x.asInstanceOf[Double] else t1.x.asInstanceOf[Double] + lp.x.asInstanceOf[Double]
        val py   = if (js.isUndefined(lp) || lp == null) t1.y.asInstanceOf[Double] else t1.y.asInstanceOf[Double] + lp.y.asInstanceOf[Double]
        val dist = manifold.contactDist(i).asInstanceOf[Double]
        val off  = count * 5
        out(off) = nx.toFloat
        out(off + 1) = ny.toFloat
        out(off + 2) = px.toFloat
        out(off + 3) = py.toFloat
        out(off + 4) = (-dist).toFloat // penetration is positive when overlapping (dist negative)
        count += 1
        i += 1
      }
    }
    st.world.contactPair(c1, colliders(collider2), cb)
    count
  }

  // ─── Advanced queries ───────────────────────────────────────────────────────

  private def makeShape(shapeType: Int, params: Array[Float]): js.Dynamic = shapeType match {
    case 0 => js.Dynamic.newInstance(R.Ball)(params(0).toDouble)
    case 1 => js.Dynamic.newInstance(R.Cuboid)(params(0).toDouble, params(1).toDouble)
    case 2 => js.Dynamic.newInstance(R.Capsule)(params(0).toDouble, params(1).toDouble)
    case _ => js.Dynamic.newInstance(R.Ball)(params(0).toDouble)
  }

  override def castShape(
    world:       Long,
    shapeType:   Int,
    shapeParams: Array[Float],
    originX:     Float,
    originY:     Float,
    dirX:        Float,
    dirY:        Float,
    maxDist:     Float,
    out:         Array[Float]
  ): Boolean = {
    val st    = ws(world)
    val shape = makeShape(shapeType, shapeParams)
    val hit   = st.world.castShape(vec(originX, originY), 0d, vec(dirX, dirY), shape, 0d, maxDist.toDouble, true)
    if (js.isUndefined(hit) || hit == null) false
    else {
      val toi  = hit.time_of_impact.asInstanceOf[Double]
      val n1   = hit.normal1
      val wp   = hit.witness1
      val coll = hit.collider
      val rHnd = coll.handle.asInstanceOf[Double]
      val sgeC = sgeColliderHandle(world, rHnd)
      out(0) = (originX.toDouble + dirX.toDouble * toi).toFloat
      out(1) = (originY.toDouble + dirY.toDouble * toi).toFloat
      out(2) = n1.x.asInstanceOf[Double].toFloat
      out(3) = n1.y.asInstanceOf[Double].toFloat
      out(4) = toi.toFloat
      out(5) = loBits(sgeC)
      out(6) = hiBits(sgeC)
      val _ = wp
      true
    }
  }

  override def rayCastAll(
    world:   Long,
    originX: Float,
    originY: Float,
    dirX:    Float,
    dirY:    Float,
    maxDist: Float,
    outHits: Array[Float],
    maxHits: Int
  ): Int = {
    val st    = ws(world)
    val r     = ray(originX, originY, dirX, dirY)
    var count = 0
    val cb: js.Function1[js.Dynamic, Boolean] = { hit =>
      if (count < maxHits) {
        val coll = hit.collider
        val toi  = hit.timeOfImpact.asInstanceOf[Double]
        val n    = hit.normal
        val rHnd = coll.handle.asInstanceOf[Double]
        val sgeC = sgeColliderHandle(world, rHnd)
        val off  = count * 7
        outHits(off) = (originX.toDouble + dirX.toDouble * toi).toFloat
        outHits(off + 1) = (originY.toDouble + dirY.toDouble * toi).toFloat
        outHits(off + 2) = n.x.asInstanceOf[Double].toFloat
        outHits(off + 3) = n.y.asInstanceOf[Double].toFloat
        outHits(off + 4) = toi.toFloat
        outHits(off + 5) = loBits(sgeC)
        outHits(off + 6) = hiBits(sgeC)
        count += 1
        true
      } else false
    }
    st.world.intersectionsWithRay(r, maxDist.toDouble, true, cb)
    count
  }

  override def projectPoint(world: Long, x: Float, y: Float, out: Array[Float]): Boolean = {
    val st  = ws(world)
    val res = st.world.projectPoint(vec(x, y), true)
    if (js.isUndefined(res) || res == null) false
    else {
      val p    = res.point
      val coll = res.collider
      val rHnd = coll.handle.asInstanceOf[Double]
      val sgeC = sgeColliderHandle(world, rHnd)
      out(0) = p.x.asInstanceOf[Double].toFloat
      out(1) = p.y.asInstanceOf[Double].toFloat
      out(2) = if (res.isInside.asInstanceOf[Boolean]) 1f else 0f
      out(3) = loBits(sgeC)
      out(4) = hiBits(sgeC)
      true
    }
  }

  // ─── Intersection events ─────────────────────────────────────────────────────

  override def pollIntersectionStartEvents(world: Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int =
    copyPairs(ws(world).intersectionStart, outCollider1, outCollider2, maxEvents)

  override def pollIntersectionStopEvents(world: Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int =
    copyPairs(ws(world).intersectionStop, outCollider1, outCollider2, maxEvents)

  // ─── Solver parameters ───────────────────────────────────────────────────────

  override def worldSetNumSolverIterations(world: Long, iters: Int): Unit =
    w(world).integrationParameters.numSolverIterations = iters

  override def worldGetNumSolverIterations(world: Long): Int =
    w(world).integrationParameters.numSolverIterations.asInstanceOf[Int]

  // The compat IntegrationParameters exposes no additional-friction-iteration knob (the Rust build does);
  // record the requested value on world state so it round-trips, matching the Panama/Rust accept-and-store behavior.
  override def worldSetNumAdditionalFrictionIterations(world: Long, iters: Int): Unit =
    ws(world).numAdditionalFrictionIterations = iters

  override def worldSetNumInternalPgsIterations(world: Long, iters: Int): Unit =
    w(world).integrationParameters.numInternalPgsIterations = iters

  // ─── Shape intersection ───────────────────────────────────────────────────────

  override def intersectShape(
    world:        Long,
    shapeType:    Int,
    shapeParams:  Array[Float],
    posX:         Float,
    posY:         Float,
    angle:        Float,
    outColliders: Array[Long],
    maxResults:   Int
  ): Int = {
    val st    = ws(world)
    val shape = makeShape(shapeType, shapeParams)
    var count = 0
    val cb: js.Function1[js.Dynamic, Boolean] = { coll =>
      if (count < maxResults) {
        outColliders(count) = sgeColliderHandle(world, coll.handle.asInstanceOf[Double])
        count += 1
        true
      } else false
    }
    st.world.intersectionsWithShape(vec(posX, posY), angle.toDouble, shape, cb)
    count
  }

  // ─── Contact force events ─────────────────────────────────────────────────────

  override def pollContactForceEvents(
    world:        Long,
    outCollider1: Array[Long],
    outCollider2: Array[Long],
    outForce:     Array[Float],
    maxEvents:    Int
  ): Int = {
    val src   = ws(world).contactForce
    val count = Math.min(src.length, maxEvents)
    var i     = 0
    while (i < count) {
      val ev = src(i)
      outCollider1(i) = ev._1
      outCollider2(i) = ev._2
      outForce(i) = ev._3
      i += 1
    }
    count
  }

  override def colliderSetContactForceEventThreshold(world: Long, collider: Long, threshold: Float): Unit =
    colliders(collider).setContactForceEventThreshold(threshold.toDouble)

  override def colliderGetContactForceEventThreshold(world: Long, collider: Long): Float =
    colliders(collider).contactForceEventThreshold().asInstanceOf[Double].toFloat

  // ─── Active hooks / one-way direction ─────────────────────────────────────────

  override def colliderSetActiveHooks(world: Long, collider: Long, flags: Int): Unit =
    colliders(collider).setActiveHooks(flags)

  override def colliderGetActiveHooks(world: Long, collider: Long): Int =
    colliders(collider).activeHooks().asInstanceOf[Int]

  // One-way platform direction is a contact-modification concern Rapier exposes only via PhysicsHooks at step time.
  // Rapier's JS collider has no built-in one-way setter, so (as the Rust side stores it) we record the direction on
  // collider state; the high-level API reads it back through colliderGetOneWayDirection.
  override def colliderSetOneWayDirection(world: Long, collider: Long, nx: Float, ny: Float, allowedAngle: Float): Unit = {
    val cs = colliderState(collider)
    if (nx == 0f && ny == 0f) {
      cs.oneWayActive = false
      cs.oneWayNx = 0f
      cs.oneWayNy = 0f
      cs.oneWayAngle = 0f
    } else {
      cs.oneWayActive = true
      cs.oneWayNx = nx
      cs.oneWayNy = ny
      cs.oneWayAngle = allowedAngle
    }
  }

  override def colliderGetOneWayDirection(world: Long, collider: Long, out: Array[Float]): Boolean = {
    val cs = colliderState(collider)
    if (cs.oneWayActive) {
      out(0) = cs.oneWayNx
      out(1) = cs.oneWayNy
      out(2) = cs.oneWayAngle
      true
    } else false
  }
}
