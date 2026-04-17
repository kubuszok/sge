/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Origin: SGE-original (3D physics API backed by Rapier3D)
 *   Convention: sealed trait + case classes for shape variants
 */
package sge
package physics3d

/** A 3D collision shape that can be attached to a [[RigidBody3d]] via a [[Collider3d]]. */
sealed trait Shape3d

object Shape3d {

  /** A sphere centered at the body origin.
    *
    * @param radius
    *   the sphere radius in world units
    */
  final case class Sphere(radius: Float) extends Shape3d

  /** An axis-aligned box centered at the body origin.
    *
    * @param halfX
    *   half-extent along the x axis
    * @param halfY
    *   half-extent along the y axis
    * @param halfZ
    *   half-extent along the z axis
    */
  final case class Box(halfX: Float, halfY: Float, halfZ: Float) extends Shape3d

  /** A capsule (cylinder with hemispherical caps) aligned along the Y axis.
    *
    * @param halfHeight
    *   half the height of the cylindrical section (total height = 2 * halfHeight + 2 * radius)
    * @param radius
    *   the cap radius
    */
  final case class Capsule(halfHeight: Float, radius: Float) extends Shape3d

  /** A cylinder aligned along the Y axis.
    *
    * @param halfHeight
    *   half the height of the cylinder
    * @param radius
    *   the cylinder radius
    */
  final case class Cylinder(halfHeight: Float, radius: Float) extends Shape3d

  /** A cone aligned along the Y axis.
    *
    * @param halfHeight
    *   half the height of the cone
    * @param radius
    *   the base radius of the cone
    */
  final case class Cone(halfHeight: Float, radius: Float) extends Shape3d

  /** A convex hull computed from a set of 3D points.
    *
    * @param vertices
    *   flat array of [x0, y0, z0, x1, y1, z1, ...] vertex positions
    */
  final case class ConvexHull(vertices: Array[Float]) extends Shape3d

  /** A triangle mesh shape defined by vertices and triangle indices.
    *
    * Typically used for complex static terrain or level geometry.
    *
    * @param vertices
    *   flat array of [x0, y0, z0, x1, y1, z1, ...] vertex positions
    * @param indices
    *   flat array of triangle indices [i0, i1, i2, ...] (must be a multiple of 3)
    */
  final case class TriMesh(vertices: Array[Float], indices: Array[Int]) extends Shape3d

  /** A 3D heightfield shape defined by a grid of height values.
    *
    * @param heights
    *   row-major array of height values (nrows x ncols)
    * @param nrows
    *   number of rows
    * @param ncols
    *   number of columns
    * @param scaleX
    *   scale along x axis
    * @param scaleY
    *   scale along y axis (height)
    * @param scaleZ
    *   scale along z axis
    */
  final case class Heightfield(heights: Array[Float], nrows: Int, ncols: Int, scaleX: Float, scaleY: Float, scaleZ: Float) extends Shape3d
}
