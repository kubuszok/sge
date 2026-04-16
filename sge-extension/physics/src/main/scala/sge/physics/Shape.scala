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

/** A 2D collision shape that can be attached to a [[RigidBody]] via a [[Collider]]. */
sealed trait Shape

object Shape {

  /** A circle centered at the body origin.
    *
    * @param radius
    *   the circle radius in world units
    */
  final case class Circle(radius: Float) extends Shape

  /** An axis-aligned box centered at the body origin.
    *
    * @param halfWidth
    *   half the box width
    * @param halfHeight
    *   half the box height
    */
  final case class Box(halfWidth: Float, halfHeight: Float) extends Shape

  /** A capsule (rectangle with semicircle caps) centered at the body origin.
    *
    * @param halfHeight
    *   half the height of the rectangular section (total height = 2 * halfHeight + 2 * radius)
    * @param radius
    *   the cap radius
    */
  final case class Capsule(halfHeight: Float, radius: Float) extends Shape

  /** A convex polygon defined by its vertices in counter-clockwise order.
    *
    * @param vertices
    *   flat array of [x0, y0, x1, y1, ...] vertex positions
    */
  final case class Polygon(vertices: Array[Float]) extends Shape

  /** A line segment (edge) defined by two endpoints.
    *
    * @param x1
    *   first endpoint x
    * @param y1
    *   first endpoint y
    * @param x2
    *   second endpoint x
    * @param y2
    *   second endpoint y
    */
  final case class Segment(x1: Float, y1: Float, x2: Float, y2: Float) extends Shape

  /** A polyline (chain) shape defined by a sequence of connected vertices.
    *
    * Unlike [[Polygon]], a polyline is not closed and has no interior.
    *
    * @param vertices
    *   flat array of [x0, y0, x1, y1, ...] vertex positions
    */
  final case class Polyline(vertices: Array[Float]) extends Shape
}
