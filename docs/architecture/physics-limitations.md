# Physics Module Limitations

SGE's physics extension uses **Rapier2D** (Rust) instead of Box2D (C++). This was an
intentional architectural decision: Rapier compiles to a single native library with
C ABI exports, enabling unified FFI across JVM (Panama FFM), Scala Native, and
eventually Scala.js (WASM).

This document lists Box2D features that have no Rapier2D equivalent and cannot be
implemented in SGE's physics module.

## Unsupported Joint Types

| Joint | Box2D | Rapier2D | Notes |
|-------|-------|----------|-------|
| Gear Joint | Yes | No | Mechanical coupling of two joints (e.g., gear ratio between revolute joints). No equivalent in Rapier. |
| Pulley Joint | Yes | No | Rope-over-pulley constraint with length ratio. No equivalent in Rapier. |
| Friction Joint | Yes | No | Planar friction resistance (top-down car physics). No direct equivalent. |
| Wheel Joint | Yes | No | Combined revolute + prismatic with suspension spring. Can be emulated with separate Revolute + Prismatic joints plus spring motor, but not provided as a single joint. |

## Unsupported Collision Callbacks

| Feature | Box2D | Rapier | SGE Alternative |
|---------|-------|--------|-----------------|
| preSolve callback | Yes | No | Not available. Rapier uses polling, not solver callbacks. |
| postSolve callback | Yes | No | Not available. Use contact events after step instead. |
| ContactFilter callback | Yes | No | Use `CollisionGroups` and `SolverGroups` for filtering. Same result, different API. |

Box2D's `ContactListener` provides four callbacks: `beginContact`, `endContact`,
`preSolve`, and `postSolve`. Rapier only provides begin/end contact events via
polling. The pre/post solve callbacks (which allow modifying contact parameters
mid-solve or reading impulse data during solve) have no equivalent.

## API Differences

### Collision Filtering

Box2D uses `categoryBits`, `maskBits`, and `groupIndex` on fixtures. SGE uses
Rapier's `CollisionGroups` with `memberships` and `filter` bitmasks:

```scala
// SGE collision filtering
collider.collisionGroups = CollisionGroups(
  memberships = 0x0001,  // belongs to group 0
  filter = 0x0002        // collides with group 1
)
```

Two colliders A and B collide if:
`(A.memberships & B.filter) != 0 && (B.memberships & A.filter) != 0`

SGE also provides `SolverGroups` to control force response separately from
detection (colliders can detect overlap without applying forces).

### Contact Details

Box2D provides detailed contact manifolds with impulses during solve.
SGE provides contact points and normals after the step via polling:

```scala
// Poll contact events after step
val contactStarts = world.pollContactStartEvents()  // Seq[(collider1Handle, collider2Handle)]
val contactStops = world.pollContactStopEvents()
```

Contact point details (positions, normals) are available via the collision
manifold polling API if implemented.

## Implementable Features Not Yet Ported

The following Box2D features have Rapier equivalents but are not yet implemented
in SGE:

| Feature | Status | Notes |
|---------|--------|-------|
| Distance Joint | Not implemented | Rapier supports via spring-damper RopeJoint |
| Rope Joint | Not implemented | Rapier supports max-distance constraint |
| Motor Joint | Not implemented | Rapier supports different motor API |
| Mouse Joint | Not implemented | Emulatable via spring joint to mouse position |
| Edge Shape | Not implemented | Rapier supports segment collider |
| Chain Shape | Not implemented | Rapier supports polyline collider |
| Contact manifold details | Partial | Points/normals available, impulses limited |

See ISS-457 for tracking of physics module gaps.

## Workarounds

### Wheel Joint Emulation

For vehicle suspension, combine:
1. `RevoluteJoint` for wheel rotation
2. `PrismaticJoint` along suspension axis with limits
3. Motor on prismatic joint for spring effect

### Top-Down Friction

For top-down games needing friction (e.g., car physics), apply manual velocity
damping per-frame rather than relying on a friction joint:

```scala
val vel = body.linearVelocity
val friction = 0.98f
body.linearVelocity = (vel._1 * friction, vel._2 * friction)
```

### Mouse/Drag Interaction

Create a kinematic body at mouse position and connect to target body with a
stiff spring (distance joint with high stiffness, low damping).
