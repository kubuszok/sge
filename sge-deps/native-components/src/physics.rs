// 2D Physics — wraps Rapier2D for rigid body simulation
//
// Provides C ABI functions for:
//   - Desktop JVM via Panama FFM (java.lang.foreign)
//   - Scala Native via @extern
//
// All public functions are prefixed with sge_phys_ to avoid symbol collisions.
// The world state is stored in a heap-allocated PhysicsWorld struct, passed
// as an opaque *mut c_void handle. Body/collider/joint handles are Rapier's
// internal indices encoded as u64.

use std::ffi::c_void;
use std::slice;

use rapier2d::prelude::*;

// ---------------------------------------------------------------------------
// World state
// ---------------------------------------------------------------------------

struct PhysicsWorld {
    gravity: Vector<Real>,
    integration_parameters: IntegrationParameters,
    physics_pipeline: PhysicsPipeline,
    island_manager: IslandManager,
    broad_phase: DefaultBroadPhase,
    narrow_phase: NarrowPhase,
    rigid_body_set: RigidBodySet,
    collider_set: ColliderSet,
    impulse_joint_set: ImpulseJointSet,
    multibody_joint_set: MultibodyJointSet,
    ccd_solver: CCDSolver,
    query_pipeline: QueryPipeline,
    // Contact event buffers
    contact_start_buf: Vec<(u64, u64)>,
    contact_stop_buf: Vec<(u64, u64)>,
}

impl PhysicsWorld {
    fn new(gx: f32, gy: f32) -> Self {
        PhysicsWorld {
            gravity: vector![gx, gy],
            integration_parameters: IntegrationParameters::default(),
            physics_pipeline: PhysicsPipeline::new(),
            island_manager: IslandManager::new(),
            broad_phase: DefaultBroadPhase::new(),
            narrow_phase: NarrowPhase::new(),
            rigid_body_set: RigidBodySet::new(),
            collider_set: ColliderSet::new(),
            impulse_joint_set: ImpulseJointSet::new(),
            multibody_joint_set: MultibodyJointSet::new(),
            ccd_solver: CCDSolver::new(),
            query_pipeline: QueryPipeline::new(),
            contact_start_buf: Vec::new(),
            contact_stop_buf: Vec::new(),
        }
    }

    fn step(&mut self, dt: f32) {
        self.integration_parameters.dt = dt;

        // Collect contact events before stepping
        self.contact_start_buf.clear();
        self.contact_stop_buf.clear();

        let (contact_send, contact_recv) = crossbeam::channel::unbounded();
        let (intersection_send, _intersection_recv) = crossbeam::channel::unbounded();

        let event_handler = ChannelEventCollector::new(contact_send, intersection_send);

        self.physics_pipeline.step(
            &self.gravity,
            &self.integration_parameters,
            &mut self.island_manager,
            &mut self.broad_phase,
            &mut self.narrow_phase,
            &mut self.rigid_body_set,
            &mut self.collider_set,
            &mut self.impulse_joint_set,
            &mut self.multibody_joint_set,
            &mut self.ccd_solver,
            None,
            &(),
            &event_handler,
        );

        // Drain collision events
        while let Ok(event) = contact_recv.try_recv() {
            match event {
                CollisionEvent::Started(c1, c2, _flags) => {
                    self.contact_start_buf.push((
                        collider_handle_to_u64(c1),
                        collider_handle_to_u64(c2),
                    ));
                }
                CollisionEvent::Stopped(c1, c2, _flags) => {
                    self.contact_stop_buf.push((
                        collider_handle_to_u64(c1),
                        collider_handle_to_u64(c2),
                    ));
                }
            }
        }

        self.query_pipeline.update(&self.collider_set);
    }
}

// ---------------------------------------------------------------------------
// Handle encoding/decoding
// ---------------------------------------------------------------------------

fn body_handle_to_u64(h: RigidBodyHandle) -> u64 {
    let (index, generation) = h.into_raw_parts();
    ((generation as u64) << 32) | (index as u64)
}

fn u64_to_body_handle(v: u64) -> RigidBodyHandle {
    let index = v as u32;
    let generation = (v >> 32) as u32;
    RigidBodyHandle::from_raw_parts(index, generation)
}

fn collider_handle_to_u64(h: ColliderHandle) -> u64 {
    let (index, generation) = h.into_raw_parts();
    ((generation as u64) << 32) | (index as u64)
}

fn u64_to_collider_handle(v: u64) -> ColliderHandle {
    let index = v as u32;
    let generation = (v >> 32) as u32;
    ColliderHandle::from_raw_parts(index, generation)
}

fn joint_handle_to_u64(h: ImpulseJointHandle) -> u64 {
    let (index, generation) = h.into_raw_parts();
    ((generation as u64) << 32) | (index as u64)
}

// ---------------------------------------------------------------------------
// World lifecycle (C ABI)
// ---------------------------------------------------------------------------

#[no_mangle]
pub extern "C" fn sge_phys_create_world(gx: f32, gy: f32) -> *mut c_void {
    let world = Box::new(PhysicsWorld::new(gx, gy));
    Box::into_raw(world) as *mut c_void
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_destroy_world(world: *mut c_void) {
    if !world.is_null() {
        drop(Box::from_raw(world as *mut PhysicsWorld));
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_world_step(world: *mut c_void, dt: f32) {
    let w = &mut *(world as *mut PhysicsWorld);
    w.step(dt);
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_world_set_gravity(world: *mut c_void, gx: f32, gy: f32) {
    let w = &mut *(world as *mut PhysicsWorld);
    w.gravity = vector![gx, gy];
}

/// Fills `out` with [gx, gy].
#[no_mangle]
pub unsafe extern "C" fn sge_phys_world_get_gravity(world: *mut c_void, out: *mut f32) {
    let w = &*(world as *mut PhysicsWorld);
    let arr = slice::from_raw_parts_mut(out, 2);
    arr[0] = w.gravity.x;
    arr[1] = w.gravity.y;
}

// ---------------------------------------------------------------------------
// Rigid body lifecycle
// ---------------------------------------------------------------------------

unsafe fn create_body(world: *mut c_void, body_type: RigidBodyType, x: f32, y: f32, angle: f32) -> u64 {
    let w = &mut *(world as *mut PhysicsWorld);
    let body = RigidBodyBuilder::new(body_type)
        .translation(vector![x, y])
        .rotation(angle)
        .build();
    body_handle_to_u64(w.rigid_body_set.insert(body))
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_create_dynamic_body(world: *mut c_void, x: f32, y: f32, angle: f32) -> u64 {
    create_body(world, RigidBodyType::Dynamic, x, y, angle)
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_create_static_body(world: *mut c_void, x: f32, y: f32, angle: f32) -> u64 {
    create_body(world, RigidBodyType::Fixed, x, y, angle)
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_create_kinematic_body(world: *mut c_void, x: f32, y: f32, angle: f32) -> u64 {
    create_body(world, RigidBodyType::KinematicPositionBased, x, y, angle)
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_destroy_body(world: *mut c_void, body: u64) {
    let w = &mut *(world as *mut PhysicsWorld);
    let handle = u64_to_body_handle(body);
    w.rigid_body_set.remove(
        handle,
        &mut w.island_manager,
        &mut w.collider_set,
        &mut w.impulse_joint_set,
        &mut w.multibody_joint_set,
        true,
    );
}

// ---------------------------------------------------------------------------
// Body accessors
// ---------------------------------------------------------------------------

/// Fills `out` with [x, y].
#[no_mangle]
pub unsafe extern "C" fn sge_phys_body_get_position(world: *mut c_void, body: u64, out: *mut f32) {
    let w = &*(world as *mut PhysicsWorld);
    if let Some(b) = w.rigid_body_set.get(u64_to_body_handle(body)) {
        let pos = b.translation();
        let arr = slice::from_raw_parts_mut(out, 2);
        arr[0] = pos.x;
        arr[1] = pos.y;
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_body_get_angle(world: *mut c_void, body: u64) -> f32 {
    let w = &*(world as *mut PhysicsWorld);
    w.rigid_body_set
        .get(u64_to_body_handle(body))
        .map(|b| b.rotation().angle())
        .unwrap_or(0.0)
}

/// Fills `out` with [vx, vy].
#[no_mangle]
pub unsafe extern "C" fn sge_phys_body_get_linear_velocity(world: *mut c_void, body: u64, out: *mut f32) {
    let w = &*(world as *mut PhysicsWorld);
    if let Some(b) = w.rigid_body_set.get(u64_to_body_handle(body)) {
        let vel = b.linvel();
        let arr = slice::from_raw_parts_mut(out, 2);
        arr[0] = vel.x;
        arr[1] = vel.y;
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_body_get_angular_velocity(world: *mut c_void, body: u64) -> f32 {
    let w = &*(world as *mut PhysicsWorld);
    w.rigid_body_set
        .get(u64_to_body_handle(body))
        .map(|b| b.angvel())
        .unwrap_or(0.0)
}

// ---------------------------------------------------------------------------
// Body setters
// ---------------------------------------------------------------------------

#[no_mangle]
pub unsafe extern "C" fn sge_phys_body_set_position(world: *mut c_void, body: u64, x: f32, y: f32) {
    let w = &mut *(world as *mut PhysicsWorld);
    if let Some(b) = w.rigid_body_set.get_mut(u64_to_body_handle(body)) {
        b.set_translation(vector![x, y], true);
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_body_set_angle(world: *mut c_void, body: u64, angle: f32) {
    let w = &mut *(world as *mut PhysicsWorld);
    if let Some(b) = w.rigid_body_set.get_mut(u64_to_body_handle(body)) {
        b.set_rotation(Rotation::new(angle), true);
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_body_set_linear_velocity(world: *mut c_void, body: u64, vx: f32, vy: f32) {
    let w = &mut *(world as *mut PhysicsWorld);
    if let Some(b) = w.rigid_body_set.get_mut(u64_to_body_handle(body)) {
        b.set_linvel(vector![vx, vy], true);
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_body_set_angular_velocity(world: *mut c_void, body: u64, omega: f32) {
    let w = &mut *(world as *mut PhysicsWorld);
    if let Some(b) = w.rigid_body_set.get_mut(u64_to_body_handle(body)) {
        b.set_angvel(omega, true);
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_body_apply_force(world: *mut c_void, body: u64, fx: f32, fy: f32) {
    let w = &mut *(world as *mut PhysicsWorld);
    if let Some(b) = w.rigid_body_set.get_mut(u64_to_body_handle(body)) {
        b.add_force(vector![fx, fy], true);
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_body_apply_impulse(world: *mut c_void, body: u64, ix: f32, iy: f32) {
    let w = &mut *(world as *mut PhysicsWorld);
    if let Some(b) = w.rigid_body_set.get_mut(u64_to_body_handle(body)) {
        b.apply_impulse(vector![ix, iy], true);
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_body_apply_torque(world: *mut c_void, body: u64, torque: f32) {
    let w = &mut *(world as *mut PhysicsWorld);
    if let Some(b) = w.rigid_body_set.get_mut(u64_to_body_handle(body)) {
        b.add_torque(torque, true);
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_body_set_linear_damping(world: *mut c_void, body: u64, damping: f32) {
    let w = &mut *(world as *mut PhysicsWorld);
    if let Some(b) = w.rigid_body_set.get_mut(u64_to_body_handle(body)) {
        b.set_linear_damping(damping);
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_body_set_angular_damping(world: *mut c_void, body: u64, damping: f32) {
    let w = &mut *(world as *mut PhysicsWorld);
    if let Some(b) = w.rigid_body_set.get_mut(u64_to_body_handle(body)) {
        b.set_angular_damping(damping);
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_body_set_gravity_scale(world: *mut c_void, body: u64, scale: f32) {
    let w = &mut *(world as *mut PhysicsWorld);
    if let Some(b) = w.rigid_body_set.get_mut(u64_to_body_handle(body)) {
        b.set_gravity_scale(scale, true);
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_body_is_awake(world: *mut c_void, body: u64) -> i32 {
    let w = &*(world as *mut PhysicsWorld);
    w.rigid_body_set
        .get(u64_to_body_handle(body))
        .map(|b| !b.is_sleeping() as i32)
        .unwrap_or(0)
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_body_wake_up(world: *mut c_void, body: u64) {
    let w = &mut *(world as *mut PhysicsWorld);
    if let Some(b) = w.rigid_body_set.get_mut(u64_to_body_handle(body)) {
        b.wake_up(true);
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_body_set_fixed_rotation(world: *mut c_void, body: u64, fixed: i32) {
    let w = &mut *(world as *mut PhysicsWorld);
    if let Some(b) = w.rigid_body_set.get_mut(u64_to_body_handle(body)) {
        b.lock_rotations(fixed != 0, true);
    }
}

// ---------------------------------------------------------------------------
// Collider creation
// ---------------------------------------------------------------------------

#[no_mangle]
pub unsafe extern "C" fn sge_phys_create_circle_collider(
    world: *mut c_void,
    body: u64,
    radius: f32,
) -> u64 {
    let w = &mut *(world as *mut PhysicsWorld);
    let collider = ColliderBuilder::ball(radius).build();
    let handle = w.collider_set.insert_with_parent(
        collider,
        u64_to_body_handle(body),
        &mut w.rigid_body_set,
    );
    collider_handle_to_u64(handle)
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_create_box_collider(
    world: *mut c_void,
    body: u64,
    half_width: f32,
    half_height: f32,
) -> u64 {
    let w = &mut *(world as *mut PhysicsWorld);
    let collider = ColliderBuilder::cuboid(half_width, half_height).build();
    let handle = w.collider_set.insert_with_parent(
        collider,
        u64_to_body_handle(body),
        &mut w.rigid_body_set,
    );
    collider_handle_to_u64(handle)
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_create_capsule_collider(
    world: *mut c_void,
    body: u64,
    half_height: f32,
    radius: f32,
) -> u64 {
    let w = &mut *(world as *mut PhysicsWorld);
    let collider = ColliderBuilder::capsule_y(half_height, radius).build();
    let handle = w.collider_set.insert_with_parent(
        collider,
        u64_to_body_handle(body),
        &mut w.rigid_body_set,
    );
    collider_handle_to_u64(handle)
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_create_polygon_collider(
    world: *mut c_void,
    body: u64,
    vertices: *const f32,
    vertex_count: i32,
) -> u64 {
    let w = &mut *(world as *mut PhysicsWorld);
    let verts = slice::from_raw_parts(vertices, (vertex_count * 2) as usize);
    let points: Vec<Point<Real>> = (0..vertex_count as usize)
        .map(|i| point![verts[i * 2], verts[i * 2 + 1]])
        .collect();

    let collider = ColliderBuilder::convex_hull(&points)
        .unwrap_or_else(|| ColliderBuilder::ball(0.1))
        .build();
    let handle = w.collider_set.insert_with_parent(
        collider,
        u64_to_body_handle(body),
        &mut w.rigid_body_set,
    );
    collider_handle_to_u64(handle)
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_destroy_collider(world: *mut c_void, collider: u64) {
    let w = &mut *(world as *mut PhysicsWorld);
    w.collider_set.remove(
        u64_to_collider_handle(collider),
        &mut w.island_manager,
        &mut w.rigid_body_set,
        true,
    );
}

// ---------------------------------------------------------------------------
// Collider properties
// ---------------------------------------------------------------------------

#[no_mangle]
pub unsafe extern "C" fn sge_phys_collider_set_density(world: *mut c_void, collider: u64, density: f32) {
    let w = &mut *(world as *mut PhysicsWorld);
    if let Some(c) = w.collider_set.get_mut(u64_to_collider_handle(collider)) {
        c.set_density(density);
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_collider_set_friction(world: *mut c_void, collider: u64, friction: f32) {
    let w = &mut *(world as *mut PhysicsWorld);
    if let Some(c) = w.collider_set.get_mut(u64_to_collider_handle(collider)) {
        c.set_friction(friction);
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_collider_set_restitution(world: *mut c_void, collider: u64, restitution: f32) {
    let w = &mut *(world as *mut PhysicsWorld);
    if let Some(c) = w.collider_set.get_mut(u64_to_collider_handle(collider)) {
        c.set_restitution(restitution);
    }
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_collider_set_sensor(world: *mut c_void, collider: u64, sensor: i32) {
    let w = &mut *(world as *mut PhysicsWorld);
    if let Some(c) = w.collider_set.get_mut(u64_to_collider_handle(collider)) {
        c.set_sensor(sensor != 0);
    }
}

// ---------------------------------------------------------------------------
// Joints
// ---------------------------------------------------------------------------

#[no_mangle]
pub unsafe extern "C" fn sge_phys_create_revolute_joint(
    world: *mut c_void,
    body1: u64,
    body2: u64,
    anchor_x: f32,
    anchor_y: f32,
) -> u64 {
    let w = &mut *(world as *mut PhysicsWorld);
    let joint = RevoluteJointBuilder::new()
        .local_anchor1(point![anchor_x, anchor_y])
        .local_anchor2(point![0.0, 0.0])
        .build();
    let handle = w.impulse_joint_set.insert(
        u64_to_body_handle(body1),
        u64_to_body_handle(body2),
        joint,
        true,
    );
    joint_handle_to_u64(handle)
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_create_prismatic_joint(
    world: *mut c_void,
    body1: u64,
    body2: u64,
    axis_x: f32,
    axis_y: f32,
) -> u64 {
    let w = &mut *(world as *mut PhysicsWorld);
    let axis = UnitVector::new_normalize(vector![axis_x, axis_y]);
    let joint = PrismaticJointBuilder::new(axis).build();
    let handle = w.impulse_joint_set.insert(
        u64_to_body_handle(body1),
        u64_to_body_handle(body2),
        joint,
        true,
    );
    joint_handle_to_u64(handle)
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_create_fixed_joint(
    world: *mut c_void,
    body1: u64,
    body2: u64,
) -> u64 {
    let w = &mut *(world as *mut PhysicsWorld);
    let joint = FixedJointBuilder::new().build();
    let handle = w.impulse_joint_set.insert(
        u64_to_body_handle(body1),
        u64_to_body_handle(body2),
        joint,
        true,
    );
    joint_handle_to_u64(handle)
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_destroy_joint(world: *mut c_void, joint: u64) {
    let w = &mut *(world as *mut PhysicsWorld);
    let index = joint as u32;
    let generation = (joint >> 32) as u32;
    let handle = ImpulseJointHandle::from_raw_parts(index, generation);
    w.impulse_joint_set.remove(handle, true);
}

// ---------------------------------------------------------------------------
// Queries
// ---------------------------------------------------------------------------

/// Ray cast. Fills `out` with [hitX, hitY, normalX, normalY, toi, bodyHandleLo, bodyHandleHi] (7 floats).
/// The body handle is split across two f32 slots: low 32 bits in `out[5]`, high 32 bits in `out[6]`.
/// Returns 1 if hit, 0 otherwise.
#[no_mangle]
pub unsafe extern "C" fn sge_phys_ray_cast(
    world: *mut c_void,
    origin_x: f32,
    origin_y: f32,
    dir_x: f32,
    dir_y: f32,
    max_dist: f32,
    out: *mut f32,
) -> i32 {
    let w = &*(world as *mut PhysicsWorld);
    let ray = Ray::new(point![origin_x, origin_y], vector![dir_x, dir_y]);

    if let Some((handle, toi)) = w.query_pipeline.cast_ray(
        &w.rigid_body_set,
        &w.collider_set,
        &ray,
        max_dist,
        true,
        QueryFilter::default(),
    ) {
        let hit_point = ray.point_at(toi);
        // Get normal by doing a full intersection
        let arr = slice::from_raw_parts_mut(out, 7);
        arr[0] = hit_point.x;
        arr[1] = hit_point.y;
        arr[2] = 0.0; // normal x (simplified — full intersection needed for normals)
        arr[3] = 0.0; // normal y
        arr[4] = toi;
        // Encode collider handle as body handle via parent body (full 64-bit, split across 2 floats)
        arr[5] = 0.0;
        arr[6] = 0.0;
        if let Some(collider) = w.collider_set.get(handle) {
            if let Some(parent) = collider.parent() {
                let h = body_handle_to_u64(parent);
                arr[5] = f32::from_bits(h as u32);         // low 32 bits
                arr[6] = f32::from_bits((h >> 32) as u32); // high 32 bits (generation)
            }
        }
        1
    } else {
        0
    }
}

/// Point query. Fills `out_bodies` with body handles. Returns count of hits.
#[no_mangle]
pub unsafe extern "C" fn sge_phys_query_point(
    world: *mut c_void,
    x: f32,
    y: f32,
    out_bodies: *mut u64,
    max_results: i32,
) -> i32 {
    let w = &*(world as *mut PhysicsWorld);
    let point = point![x, y];
    let out = slice::from_raw_parts_mut(out_bodies, max_results as usize);
    let mut count = 0i32;

    w.query_pipeline.intersections_with_point(
        &w.rigid_body_set,
        &w.collider_set,
        &point,
        QueryFilter::default(),
        |handle| {
            if count < max_results {
                if let Some(collider) = w.collider_set.get(handle) {
                    if let Some(parent) = collider.parent() {
                        out[count as usize] = body_handle_to_u64(parent);
                        count += 1;
                    }
                }
            }
            count < max_results // continue if we have room
        },
    );
    count
}

// ---------------------------------------------------------------------------
// Contact events (polling)
// ---------------------------------------------------------------------------

#[no_mangle]
pub unsafe extern "C" fn sge_phys_poll_contact_start_events(
    world: *mut c_void,
    out_collider1: *mut u64,
    out_collider2: *mut u64,
    max_events: i32,
) -> i32 {
    let w = &*(world as *mut PhysicsWorld);
    let c1 = slice::from_raw_parts_mut(out_collider1, max_events as usize);
    let c2 = slice::from_raw_parts_mut(out_collider2, max_events as usize);
    let count = w.contact_start_buf.len().min(max_events as usize);
    for i in 0..count {
        c1[i] = w.contact_start_buf[i].0;
        c2[i] = w.contact_start_buf[i].1;
    }
    count as i32
}

#[no_mangle]
pub unsafe extern "C" fn sge_phys_poll_contact_stop_events(
    world: *mut c_void,
    out_collider1: *mut u64,
    out_collider2: *mut u64,
    max_events: i32,
) -> i32 {
    let w = &*(world as *mut PhysicsWorld);
    let c1 = slice::from_raw_parts_mut(out_collider1, max_events as usize);
    let c2 = slice::from_raw_parts_mut(out_collider2, max_events as usize);
    let count = w.contact_stop_buf.len().min(max_events as usize);
    for i in 0..count {
        c1[i] = w.contact_stop_buf[i].0;
        c2[i] = w.contact_stop_buf[i].1;
    }
    count as i32
}
