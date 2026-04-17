/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (2D physics API backed by Rapier2D)
 *   Convention: JS stub — throws UnsupportedOperationException
 *   Idiom: split packages
 *   Audited: 2026-03-08
 */
package sge
package platform

/** Scala.js stub for [[PhysicsOps]].
  *
  * All methods throw [[UnsupportedOperationException]]. A future implementation may integrate with the Rapier2D WASM build via JavaScript interop.
  */
private[platform] object PhysicsOpsJs extends PhysicsOps {

  private def unsupported: Nothing =
    throw new UnsupportedOperationException("PhysicsOps is not yet available on Scala.js")

  // World lifecycle
  override def createWorld(gravityX:  Float, gravityY: Float):            Long = unsupported
  override def destroyWorld(world:    Long):                              Unit = unsupported
  override def worldStep(world:       Long, dt:        Float):            Unit = unsupported
  override def worldSetGravity(world: Long, gx:        Float, gy: Float): Unit = unsupported
  override def worldGetGravity(world: Long, out:       Array[Float]):     Unit = unsupported

  // Rigid body
  override def createDynamicBody(world:      Long, x:    Float, y:      Float, angle: Float): Long    = unsupported
  override def createStaticBody(world:       Long, x:    Float, y:      Float, angle: Float): Long    = unsupported
  override def createKinematicBody(world:    Long, x:    Float, y:      Float, angle: Float): Long    = unsupported
  override def destroyBody(world:            Long, body: Long):                               Unit    = unsupported
  override def bodyGetPosition(world:        Long, body: Long, out:     Array[Float]):        Unit    = unsupported
  override def bodyGetAngle(world:           Long, body: Long):                               Float   = unsupported
  override def bodyGetLinearVelocity(world:  Long, body: Long, out:     Array[Float]):        Unit    = unsupported
  override def bodyGetAngularVelocity(world: Long, body: Long):                               Float   = unsupported
  override def bodySetPosition(world:        Long, body: Long, x:       Float, y:     Float): Unit    = unsupported
  override def bodySetAngle(world:           Long, body: Long, angle:   Float):               Unit    = unsupported
  override def bodySetLinearVelocity(world:  Long, body: Long, vx:      Float, vy:    Float): Unit    = unsupported
  override def bodySetAngularVelocity(world: Long, body: Long, omega:   Float):               Unit    = unsupported
  override def bodyApplyForce(world:         Long, body: Long, fx:      Float, fy:    Float): Unit    = unsupported
  override def bodyApplyImpulse(world:       Long, body: Long, ix:      Float, iy:    Float): Unit    = unsupported
  override def bodyApplyTorque(world:        Long, body: Long, torque:  Float):               Unit    = unsupported
  override def bodySetLinearDamping(world:   Long, body: Long, damping: Float):               Unit    = unsupported
  override def bodySetAngularDamping(world:  Long, body: Long, damping: Float):               Unit    = unsupported
  override def bodySetGravityScale(world:    Long, body: Long, scale:   Float):               Unit    = unsupported
  override def bodyIsAwake(world:            Long, body: Long):                               Boolean = unsupported
  override def bodyWakeUp(world:             Long, body: Long):                               Unit    = unsupported
  override def bodySetFixedRotation(world:   Long, body: Long, fixed:   Boolean):             Unit    = unsupported

  // Collider
  override def createCircleCollider(world:   Long, body:     Long, radius:      Float):                                                  Long = unsupported
  override def createBoxCollider(world:      Long, body:     Long, halfWidth:   Float, halfHeight:         Float):                       Long = unsupported
  override def createCapsuleCollider(world:  Long, body:     Long, halfHeight:  Float, radius:             Float):                       Long = unsupported
  override def createPolygonCollider(world:  Long, body:     Long, vertices:    Array[Float], vertexCount: Int):                         Long = unsupported
  override def createSegmentCollider(world:  Long, body:     Long, x1:          Float, y1:                 Float, x2: Float, y2: Float): Long = unsupported
  override def createPolylineCollider(world: Long, body:     Long, vertices:    Array[Float], vertexCount: Int):                         Long = unsupported
  override def destroyCollider(world:        Long, collider: Long):                                                                      Unit = unsupported
  override def colliderSetDensity(world:     Long, collider: Long, density:     Float):                                                  Unit = unsupported
  override def colliderSetFriction(world:    Long, collider: Long, friction:    Float):                                                  Unit = unsupported
  override def colliderSetRestitution(world: Long, collider: Long, restitution: Float):                                                  Unit = unsupported
  override def colliderSetSensor(world:      Long, collider: Long, sensor:      Boolean):                                                Unit = unsupported

  // Collision filtering
  override def colliderSetCollisionGroups(world: Long, collider: Long, memberships: Int, filter: Int): Unit = unsupported
  override def colliderGetCollisionGroups(world: Long, collider: Long, out:         Array[Int]):       Unit = unsupported
  override def colliderSetSolverGroups(world:    Long, collider: Long, memberships: Int, filter: Int): Unit = unsupported
  override def colliderGetSolverGroups(world:    Long, collider: Long, out:         Array[Int]):       Unit = unsupported

  // Joints
  override def createRevoluteJoint(world:  Long, body1: Long, body2: Long, anchorX: Float, anchorY: Float): Long = unsupported
  override def createPrismaticJoint(world: Long, body1: Long, body2: Long, axisX:   Float, axisY:   Float): Long = unsupported
  override def createFixedJoint(world:     Long, body1: Long, body2: Long):                                 Long = unsupported
  override def createRopeJoint(world:      Long, body1: Long, body2: Long, maxDist: Float):                 Long = unsupported
  override def destroyJoint(world:         Long, joint: Long):                                              Unit = unsupported

  // Revolute joint limits/motors
  override def revoluteJointEnableLimits(world:      Long, joint: Long, enable: Boolean):             Unit    = unsupported
  override def revoluteJointSetLimits(world:         Long, joint: Long, lower:  Float, upper: Float): Unit    = unsupported
  override def revoluteJointGetLimits(world:         Long, joint: Long, out:    Array[Float]):        Unit    = unsupported
  override def revoluteJointIsLimitEnabled(world:    Long, joint: Long):                              Boolean = unsupported
  override def revoluteJointEnableMotor(world:       Long, joint: Long, enable: Boolean):             Unit    = unsupported
  override def revoluteJointSetMotorSpeed(world:     Long, joint: Long, speed:  Float):               Unit    = unsupported
  override def revoluteJointSetMaxMotorTorque(world: Long, joint: Long, torque: Float):               Unit    = unsupported
  override def revoluteJointGetMotorSpeed(world:     Long, joint: Long):                              Float   = unsupported
  override def revoluteJointGetAngle(world:          Long, joint: Long):                              Float   = unsupported

  // Prismatic joint limits/motors
  override def prismaticJointEnableLimits(world:     Long, joint: Long, enable: Boolean):             Unit  = unsupported
  override def prismaticJointSetLimits(world:        Long, joint: Long, lower:  Float, upper: Float): Unit  = unsupported
  override def prismaticJointGetLimits(world:        Long, joint: Long, out:    Array[Float]):        Unit  = unsupported
  override def prismaticJointEnableMotor(world:      Long, joint: Long, enable: Boolean):             Unit  = unsupported
  override def prismaticJointSetMotorSpeed(world:    Long, joint: Long, speed:  Float):               Unit  = unsupported
  override def prismaticJointSetMaxMotorForce(world: Long, joint: Long, force:  Float):               Unit  = unsupported
  override def prismaticJointGetTranslation(world:   Long, joint: Long):                              Float = unsupported

  // Motor joint
  override def createMotorJoint(world:              Long, body1: Long, body2:  Long):            Long  = unsupported
  override def motorJointSetLinearOffset(world:     Long, joint: Long, x:      Float, y: Float): Unit  = unsupported
  override def motorJointGetLinearOffset(world:     Long, joint: Long, out:    Array[Float]):    Unit  = unsupported
  override def motorJointSetAngularOffset(world:    Long, joint: Long, angle:  Float):           Unit  = unsupported
  override def motorJointGetAngularOffset(world:    Long, joint: Long):                          Float = unsupported
  override def motorJointSetMaxForce(world:         Long, joint: Long, force:  Float):           Unit  = unsupported
  override def motorJointSetMaxTorque(world:        Long, joint: Long, torque: Float):           Unit  = unsupported
  override def motorJointSetCorrectionFactor(world: Long, joint: Long, factor: Float):           Unit  = unsupported

  // Rope joint
  override def ropeJointSetMaxDistance(world: Long, joint: Long, maxDist: Float): Unit  = unsupported
  override def ropeJointGetMaxDistance(world: Long, joint: Long):                 Float = unsupported

  // Body mass/inertia
  override def bodyGetMass(world:                 Long, body: Long):                    Float = unsupported
  override def bodyGetInertia(world:              Long, body: Long):                    Float = unsupported
  override def bodyGetLocalCenterOfMass(world:    Long, body: Long, out: Array[Float]): Unit  = unsupported
  override def bodyRecomputeMassProperties(world: Long, body: Long):                    Unit  = unsupported

  // Queries
  override def queryAABB(world:  Long, minX:    Float, minY:    Float, maxX:      Float, maxY:             Float, outColliders: Array[Long], maxResults: Int):          Int     = unsupported
  override def rayCast(world:    Long, originX: Float, originY: Float, dirX:      Float, dirY:             Float, maxDist:      Float, out:              Array[Float]): Boolean = unsupported
  override def queryPoint(world: Long, x:       Float, y:       Float, outBodies: Array[Long], maxResults: Int):                                                        Int     = unsupported

  // Contact events
  override def pollContactStartEvents(world: Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int = unsupported
  override def pollContactStopEvents(world:  Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int = unsupported

  // Contact detail queries
  override def contactPairCount(world:  Long, collider1: Long, collider2: Long):                                    Int = unsupported
  override def contactPairPoints(world: Long, collider1: Long, collider2: Long, out: Array[Float], maxPoints: Int): Int = unsupported

  // Body forces at point
  override def bodyApplyForceAtPoint(world:   Long, body: Long, fx:      Float, fy: Float, px: Float, py: Float): Unit = unsupported
  override def bodyApplyImpulseAtPoint(world: Long, body: Long, ix:      Float, iy: Float, px: Float, py: Float): Unit = unsupported
  override def bodyApplyTorqueImpulse(world:  Long, body: Long, impulse: Float):                                  Unit = unsupported
  override def bodyResetForces(world:         Long, body: Long):                                                  Unit = unsupported
  override def bodyResetTorques(world:        Long, body: Long):                                                  Unit = unsupported

  // Body property getters
  override def bodyGetLinearDamping(world:       Long, body: Long):                                                     Float   = unsupported
  override def bodyGetAngularDamping(world:      Long, body: Long):                                                     Float   = unsupported
  override def bodyGetGravityScale(world:        Long, body: Long):                                                     Float   = unsupported
  override def bodyGetType(world:                Long, body: Long):                                                     Int     = unsupported
  override def bodySetEnabled(world:             Long, body: Long, enabled: Boolean):                                   Unit    = unsupported
  override def bodyIsEnabled(world:              Long, body: Long):                                                     Boolean = unsupported
  override def bodySetEnabledTranslations(world: Long, body: Long, allowX:  Boolean, allowY: Boolean):                  Unit    = unsupported
  override def bodyIsTranslationLockedX(world:   Long, body: Long):                                                     Boolean = unsupported
  override def bodyIsTranslationLockedY(world:   Long, body: Long):                                                     Boolean = unsupported
  override def bodyIsRotationLocked(world:       Long, body: Long):                                                     Boolean = unsupported
  override def bodySetDominanceGroup(world:      Long, body: Long, group:   Int):                                       Unit    = unsupported
  override def bodyGetDominanceGroup(world:      Long, body: Long):                                                     Int     = unsupported
  override def bodyGetWorldCenterOfMass(world:   Long, body: Long, out:     Array[Float]):                              Unit    = unsupported
  override def bodyEnableCcd(world:              Long, body: Long, enable:  Boolean):                                   Unit    = unsupported
  override def bodyIsCcdEnabled(world:           Long, body: Long):                                                     Boolean = unsupported
  override def bodySleep(world:                  Long, body: Long):                                                     Unit    = unsupported
  override def bodyGetVelocityAtPoint(world:     Long, body: Long, px:      Float, py:       Float, out: Array[Float]): Unit    = unsupported

  // Collider getters
  override def colliderGetDensity(world:              Long, collider: Long):                                         Float   = unsupported
  override def colliderGetFriction(world:             Long, collider: Long):                                         Float   = unsupported
  override def colliderGetRestitution(world:          Long, collider: Long):                                         Float   = unsupported
  override def colliderIsSensor(world:                Long, collider: Long):                                         Boolean = unsupported
  override def colliderSetEnabled(world:              Long, collider: Long, enabled: Boolean):                       Unit    = unsupported
  override def colliderIsEnabled(world:               Long, collider: Long):                                         Boolean = unsupported
  override def colliderGetPositionWrtParent(world:    Long, collider: Long, out:     Array[Float]):                  Unit    = unsupported
  override def colliderSetPositionWrtParent(world:    Long, collider: Long, x:       Float, y: Float, angle: Float): Unit    = unsupported
  override def colliderGetPosition(world:             Long, collider: Long, out:     Array[Float]):                  Unit    = unsupported
  override def colliderGetShapeType(world:            Long, collider: Long):                                         Int     = unsupported
  override def colliderGetAabb(world:                 Long, collider: Long, out:     Array[Float]):                  Unit    = unsupported
  override def colliderGetParentBody(world:           Long, collider: Long):                                         Long    = unsupported
  override def colliderGetMass(world:                 Long, collider: Long):                                         Float   = unsupported
  override def colliderSetMass(world:                 Long, collider: Long, mass:    Float):                         Unit    = unsupported
  override def colliderSetContactSkin(world:          Long, collider: Long, skin:    Float):                         Unit    = unsupported
  override def colliderSetActiveEvents(world:         Long, collider: Long, flags:   Int):                           Unit    = unsupported
  override def colliderGetActiveEvents(world:         Long, collider: Long):                                         Int     = unsupported
  override def colliderSetActiveCollisionTypes(world: Long, collider: Long, flags:   Int):                           Unit    = unsupported
  override def colliderGetActiveCollisionTypes(world: Long, collider: Long):                                         Int     = unsupported

  // New shapes
  override def createTriMeshCollider(world:     Long, body: Long, vertices: Array[Float], vertexCount: Int, indices: Array[Int], indexCount: Int):   Long = unsupported
  override def createHeightfieldCollider(world: Long, body: Long, heights:  Array[Float], numCols:     Int, scaleX:  Float, scaleY:          Float): Long = unsupported

  // Joint getters
  override def revoluteJointGetMaxMotorTorque(world: Long, joint: Long): Float = unsupported
  override def prismaticJointGetMotorSpeed(world:    Long, joint: Long): Float = unsupported
  override def prismaticJointGetMaxMotorForce(world: Long, joint: Long): Float = unsupported
  override def motorJointGetMaxForce(world:          Long, joint: Long): Float = unsupported
  override def motorJointGetMaxTorque(world:         Long, joint: Long): Float = unsupported
  override def motorJointGetCorrectionFactor(world:  Long, joint: Long): Float = unsupported

  // Spring joint
  override def createSpringJoint(world:        Long, body1: Long, body2:      Long, restLength: Float, stiffness: Float, damping: Float): Long  = unsupported
  override def springJointSetRestLength(world: Long, joint: Long, restLength: Float):                                                     Unit  = unsupported
  override def springJointGetRestLength(world: Long, joint: Long):                                                                        Float = unsupported
  override def springJointSetParams(world:     Long, joint: Long, stiffness:  Float, damping:   Float):                                   Unit  = unsupported

  // Advanced queries
  override def castShape(world: Long, shapeType: Int, shapeParams: Array[Float], originX: Float, originY: Float, dirX: Float, dirY: Float, maxDist: Float, out: Array[Float]): Boolean = unsupported
  override def rayCastAll(world:   Long, originX: Float, originY: Float, dirX: Float, dirY: Float, maxDist: Float, outHits: Array[Float], maxHits: Int): Int     = unsupported
  override def projectPoint(world: Long, x:       Float, y:       Float, out:  Array[Float]):                                                            Boolean = unsupported

  // Intersection events
  override def pollIntersectionStartEvents(world: Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int = unsupported
  override def pollIntersectionStopEvents(world:  Long, outCollider1: Array[Long], outCollider2: Array[Long], maxEvents: Int): Int = unsupported

  // Solver params
  override def worldSetNumSolverIterations(world:             Long, iters: Int): Unit = unsupported
  override def worldGetNumSolverIterations(world:             Long):             Int  = unsupported
  override def worldSetNumAdditionalFrictionIterations(world: Long, iters: Int): Unit = unsupported
  override def worldSetNumInternalPgsIterations(world:        Long, iters: Int): Unit = unsupported

  // Shape intersection
  override def intersectShape(world: Long, shapeType: Int, shapeParams: Array[Float], posX: Float, posY: Float, angle: Float, outColliders: Array[Long], maxResults: Int): Int = unsupported

  // Contact force events
  override def pollContactForceEvents(world:                Long, outCollider1: Array[Long], outCollider2: Array[Long], outForce: Array[Float], maxEvents: Int): Int   = unsupported
  override def colliderSetContactForceEventThreshold(world: Long, collider:     Long, threshold:           Float):                                               Unit  = unsupported
  override def colliderGetContactForceEventThreshold(world: Long, collider:     Long):                                                                           Float = unsupported

  // Active hooks / one-way direction
  override def colliderSetActiveHooks(world:     Long, collider: Long, flags: Int):                                   Unit    = unsupported
  override def colliderGetActiveHooks(world:     Long, collider: Long):                                               Int     = unsupported
  override def colliderSetOneWayDirection(world: Long, collider: Long, nx:    Float, ny: Float, allowedAngle: Float): Unit    = unsupported
  override def colliderGetOneWayDirection(world: Long, collider: Long, out:   Array[Float]):                          Boolean = unsupported
}
