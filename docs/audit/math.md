# Audit: sge.math

Audited: 33/33 files | Pass: 30 | Minor: 0 | Major: 0 | N/A: 3
Last updated: 2026-03-04

---

### SGE-Original Opaque Types (3 files — N/A)

- **Degrees.scala** — SGE-original opaque type. No LibGDX counterpart.
- **Radians.scala** — SGE-original opaque type. No LibGDX counterpart.
- **Epsilon.scala** — SGE-original opaque type. No LibGDX counterpart.

---

### Shape2D.scala — pass
All 2 interface methods ported. Trait instead of Java interface.

### Path.scala — pass
All 5 interface methods ported.

### FloatCounter.scala — pass
All methods ported. `mean` changed from nullable `WindowedMean` to `Option[WindowedMean]`.

### GridPoints.scala — pass
Both `GridPoint2` and `GridPoint3` ported as `final case class`. All methods present.

### WindowedMean.scala — pass
All methods ported.

### Bresenham2.scala — pass
All methods ported. Uses `DynamicArray` and `Pool.Default`.

### RandomXS128.scala — pass
All constructors and methods ported.

### Circle.scala — pass
All 5 constructors and methods ported.

### Ellipse.scala — pass
All 5 constructors and methods ported.

### Rectangle.scala — pass
All methods ported including `fromString`/`toString`.

### Polygon.scala — pass
All methods ported.

### Polyline.scala — pass
All methods ported.

### Frustum.scala — pass
All methods ported including `boundsInFrustum(OrientedBoundingBox)`.

### Plane.scala — pass
All methods ported. `PlaneSide` converted from `scala.Enumeration` to Scala 3 `enum extends java.lang.Enum`.

### CumulativeDistribution.scala — pass
All methods ported. Uses `boundary`/`break` for binary search.

### Octree.scala — pass
All methods ported. `rayCast` now calls `Intersector.intersectRayBounds` (was placeholder).

### Affine2.scala — pass
All methods ported.

### GeometryUtils.scala — pass
All methods ported.

### ConvexHull.scala — pass
All methods ported. Uses `DynamicArray`.

### BSpline.scala — pass
All methods ported. Uses `ClassTag[T]` for array creation.

### CatmullRomSpline.scala — pass
All methods ported.

### Bezier.scala — pass
All methods ported.

### DelaunayTriangulator.scala — pass
All methods ported. Uses `DynamicArray`.

### EarClippingTriangulator.scala — pass
All methods ported.

### Interpolation.scala — pass
SAM trait with all 36 named interpolation instances and 15 inner classes.

### MathUtils.scala — pass
All constants and methods ported. Sin/Cos/Atan2 lookup tables present.

### Vectors.scala — pass
`Vector` trait + `Vector2`/`Vector3`/`Vector4` with all methods.

### Matrices.scala — pass
`Matrix3` + `Matrix4` with all methods.

### Quaternion.scala — pass
All methods ported.

### Intersector.scala — pass
All methods ported including `SplitTriangle` and `MinimumTranslationVector`.
