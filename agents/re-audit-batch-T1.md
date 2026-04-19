# Batch T1 -- Core LibGDX Test Mapping

Audit date: 2026-04-18
Scope: All test files in `original-src/libgdx/gdx/test/` mapped to SGE equivalents.

---

## AnimationControllerTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/graphics/g3d/utils/AnimationControllerTest.java`
SGE equivalent: `sge/src/test/scala/sge/graphics/g3d/utils/AnimationControllerTest.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testGetFirstKeyframeIndexAtTimeNominal | BaseAnimationController.getFirstKeyframeIndexAtTime with multiple keyframes | PORTED | "getFirstKeyframeIndexAtTime nominal" | Same assertions |
| 2 | testGetFirstKeyframeIndexAtTimeSingleKey | getFirstKeyframeIndexAtTime with single keyframe | PORTED | "getFirstKeyframeIndexAtTime single key" | Same assertions |
| 3 | testGetFirstKeyframeIndexAtTimeEmpty | getFirstKeyframeIndexAtTime with empty array | PORTED | "getFirstKeyframeIndexAtTime empty" | Same assertion |
| 4 | testEndUpActionAtDurationTime | AnimationController action ending at duration | PORTED | "end up action at duration time" | Same logic, uses Seconds opaque type |
| 5 | testEndUpActionAtDurationTimeReverse | AnimationController reverse action at duration | PORTED | "end up action at duration time reverse" | Same logic, uses Seconds opaque type |

---

## AnimationDescTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/graphics/g3d/utils/AnimationDescTest.java`
SGE equivalent: `sge/src/test/scala/sge/graphics/g3d/utils/AnimationDescTest.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testUpdateNominal | AnimationDesc.update forward nominal | PORTED | "update nominal" | Same assertions, uses Seconds |
| 2 | testUpdateJustEnd | AnimationDesc.update forward at exact end | PORTED | "update just end" | Same assertions |
| 3 | testUpdateBigDelta | AnimationDesc.update with large delta | PORTED | "update big delta" | Same assertions |
| 4 | testUpdateZeroDelta | AnimationDesc.update with zero delta | PORTED | "update zero delta" | Same assertions |
| 5 | testUpdateReverseNominal | AnimationDesc.update reverse nominal | PORTED | "update reverse nominal" | Same assertions |
| 6 | testUpdateReverseJustEnd | AnimationDesc.update reverse at exact end | PORTED | "update reverse just end" | Same assertions |
| 7 | testUpdateReverseBigDelta | AnimationDesc.update reverse with large delta | PORTED | "update reverse big delta" | Same assertions |
| 8 | testUpdateReverseZeroDelta | AnimationDesc.update reverse with zero delta | PORTED | "update reverse zero delta" | Same assertions |

---

## BSplineTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/math/BSplineTest.java`
SGE equivalent: `sge/src/test/scala/sge/math/BSplineTest.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testCubicSplineNonContinuous | BSpline.valueAt non-continuous | PORTED | "cubicSplineNonContinuous" | Same assertions |
| 2 | testCubicSplineContinuous | BSpline.valueAt continuous circle | PORTED | "cubicSplineContinuous" | Same assertions |
| 3 | testCubicDerivative | BSpline.derivativeAt | PORTED | "cubicDerivative" | Same assertions |
| 4 | testContinuousApproximation | BSpline.approximate continuous | PORTED | "continuousApproximation" | Same assertions |
| 5 | testNonContinuousApproximation | BSpline.approximate non-continuous | PORTED | "nonContinuousApproximation" | Same assertions |
| 6 | testSplineContinuity | Start/end equality for continuous | PORTED | "splineContinuity" | Same assertions |
| 7 | testEdgeCases | t=0 and t=1 edge values | PORTED | "edgeCases" | Same assertions |

---

## BezierTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/math/BezierTest.java`
SGE equivalent: `sge/src/test/scala/sge/math/BezierTest.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testLinear2D (parameterized x6) | Bezier linear 2D: approxLength, derivativeAt, valueAt, approximate, locate | PORTED | "linear2D" (parameterized x6) | All 6 parameter combinations tested; same assertions |

---

## ConvexHullTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/math/ConvexHullTest.java`
SGE equivalent: `sge/src/test/scala/sge/math/ConvexHullTest.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testComputePolygon | ConvexHull.computePolygon variants | PORTED | "computePolygon" | Same assertions, same helper |

---

## IntersectorTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/math/IntersectorTest.java`
SGE equivalent: `sge/src/test/scala/sge/math/IntersectorTest.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testSplitTriangle | Intersector.splitTriangle all 4 cases | PORTED | "splitTriangle" | Same assertions, same triangleEquals helper |
| 2 | intersectSegmentCircle | Intersector.intersectSegmentCircle 7 sub-cases | PORTED | "intersectSegmentCircle" | Same assertions with Nullable |
| 3 | testIntersectPlanes | Intersector.intersectPlanes frustum corners | PORTED | "intersectPlanes" | Same assertions |
| 4 | testIsPointInTriangle2D | Intersector.isPointInTriangle Vector2 | PORTED | "isPointInTriangle2D" | Same assertions |
| 5 | testIsPointInTriangle3D | Intersector.isPointInTriangle Vector3 | PORTED | "isPointInTriangle3D" | Same assertions including all sub-cases |
| 6 | testIntersectPolygons | Intersector.intersectPolygons corner case | PORTED | "intersectPolygons" | Same assertions |
| 7 | testIntersectPolygonsWithVertexLyingOnEdge | intersectPolygons vertex on edge | PORTED | "intersectPolygonsWithVertexLyingOnEdge" | Same assertions |
| 8 | testIntersectPolygonsWithTransformationsOnProvidedResultPolygon | intersectPolygons with transforms | PORTED | "intersectPolygonsWithTransformationsOnProvidedResultPolygon" | Same assertions |

SGE also has 20+ additional Intersector tests not in the original (intersectRayPlane, intersectRayTriangle, intersectRaySphere, intersectRayBounds, etc.).

---

## MathUtilsTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/math/MathUtilsTest.java`
SGE equivalent: `sge/src/test/scala/sge/math/MathUtilsTest.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | lerpAngle | MathUtils.lerpAngle radians | PORTED | "lerpAngle" | Same assertions including loop |
| 2 | lerpAngleDeg | MathUtils.lerpAngleDeg degrees | PORTED | "lerpAngleDeg" | Same assertions including loop |
| 3 | lerpAngleDegCrossingZero | lerpAngleDeg crossing 0/360 boundary | PORTED | "lerpAngleDegCrossingZero" | Same assertions |
| 4 | lerpAngleDegCrossingZeroBackwards | lerpAngleDeg crossing 0/360 backwards | PORTED | "lerpAngleDegCrossingZeroBackwards" | Same assertions |
| 5 | testNorm | MathUtils.norm | PORTED | "norm" | Same assertions |
| 6 | testMap | MathUtils.map | PORTED | "map" | Same assertions |
| 7 | testRandomLong | MathUtils.random(Long, Long) | PORTED | "randomLong" | Same assertions |
| 8 | testSinDeg | MathUtils.sinDeg | PORTED | "sinDeg" | Same assertions |
| 9 | testCosDeg | MathUtils.cosDeg | PORTED | "cosDeg" | Same assertions (slightly wider tolerance) |
| 10 | testTanDeg | MathUtils.tanDeg | PORTED | "tanDeg" | Same assertions |
| 11 | testAtan2Deg360 | MathUtils.atan2Deg360 | PORTED | "atan2Deg360" | Same assertions |

SGE also has an additional "tan uses range-reduced value" regression test.

---

## OctreeTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/math/OctreeTest.java`
SGE equivalent: `sge/src/test/scala/sge/math/OctreeTest.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testInsert | Octree add/remove/getAll | PARTIALLY_PORTED | "insert" | Missing isLeaf assertions (root is protected in SGE); intersects collider is stubbed with PositiveInfinity instead of using Intersector.intersectRayBounds |

---

## PolygonTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/math/PolygonTest.java`
SGE equivalent: `sge/src/test/scala/sge/math/PolygonTest.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testZeroRotation | Polygon.rotate(0) identity | PORTED | "zeroRotation" | Same assertions |
| 2 | test360Rotation | Polygon.rotate(360) identity | PORTED | "360Rotation" | Same assertions |
| 3 | testConcavePolygonArea | Polygon.area() concave | PORTED | "concavePolygonArea" | Uses Math.abs wrapper |
| 4 | testTriangleArea | Polygon.area() triangle | PORTED | "triangleArea" | Uses Math.abs wrapper |

---

## RectangleTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/math/RectangleTest.java`
SGE equivalent: `sge/src/test/scala/sge/math/RectangleTest.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testToString | Rectangle.toString | PORTED | "toString" | Different float values chosen for cross-platform compat |
| 2 | testFromString | Rectangle.fromString | PORTED | "fromString" | Same assertions |

---

## Shape2DTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/math/Shape2DTest.java`
SGE equivalent: `sge/src/test/scala/sge/math/Shape2DTest.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testCircle | Circle.overlaps, contains | PORTED | "circle" | Same assertions |
| 2 | testRectangle | Rectangle.overlaps, contains | PORTED | "rectangle" | Same assertions |

---

## Vector2Test.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/math/Vector2Test.java`
SGE equivalent: `sge/src/test/scala/sge/math/Vector2Test.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testToString | Vector2.toString | PORTED | "toString" | Different float values for cross-platform |
| 2 | testFromString | Vector2.fromString | PORTED | "fromString" | Same assertions |
| 3 | testAngle | Vector2.angleDeg() | PORTED | "angle" | Same assertion |
| 4 | testAngleRelative | Vector2.angleDeg(reference) | PORTED | "angleRelative" | Same assertion |
| 5 | testAngleStatic | Vector2.angleDeg(x,y) static | PORTED | "angleStatic" | Same assertion |
| 6 | testAngleRad | Vector2.angleRad() | PORTED | "angleRad" | Same assertion |
| 7 | testAngleRadRelative | Vector2.angleRad(reference) | PORTED | "angleRadRelative" | Same assertion |
| 8 | testAngleRadStatic | Vector2.angleRad(x,y) static | PORTED | "angleRadStatic" | Same assertion |

---

## Vector3Test.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/math/Vector3Test.java`
SGE equivalent: `sge/src/test/scala/sge/math/Vector3Test.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testToString | Vector3.toString | PORTED | "toString" | Different float values for cross-platform |
| 2 | testFromString | Vector3.fromString | PORTED | "fromString" | Same assertions |

---

## Vector4Test.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/math/Vector4Test.java`
SGE equivalent: `sge/src/test/scala/sge/math/Vector4Test.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testToString | Vector4.toString | PORTED | "toString" | Different float values for cross-platform |
| 2 | testFromString | Vector4.fromString | PORTED | "fromString" | Same assertions |

---

## CollisionTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/math/collision/CollisionTest.java`
SGE equivalent: `sge/src/test/scala/sge/math/collision/CollisionTest.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testBoundingBox | BoundingBox.contains | PORTED | "boundingBox" | Same assertions |
| 2 | testOrientedBoundingBox | OrientedBoundingBox.contains | PORTED | "orientedBoundingBox" | Same assertions |
| 3 | testOrientedBoundingBoxCollision | OrientedBoundingBox.intersects | PORTED | "orientedBoundingBoxCollision" | Same assertions |

---

## FlushablePoolTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/utils/FlushablePoolTest.java`
SGE equivalent: `sge/src/test/scala/sge/utils/FlushablePoolTest.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | initializeFlushablePoolTest1 | Default constructor | PORTED | "initialize flushable pool default" | Adapted to Pool.Default + Pool.Flushable trait |
| 2 | initializeFlushablePoolTest2 | Capacity constructor | PORTED | "initialize flushable pool with initial capacity" | Same |
| 3 | initializeFlushablePoolTest3 | Capacity + max constructor | PORTED | "initialize flushable pool with initial capacity and max" | Same |
| 4 | obtainTest | Obtain and flush cycle | PORTED | "obtain" | Same assertions |
| 5 | flushTest | Flush clears obtained | PORTED | "flush" | Same assertions |
| 6 | freeTest | Free individual element | PORTED | "free" | Same assertions |
| 7 | freeAllTest | Free all elements | PORTED | "freeAll" | Same assertions; uses Seq instead of Array |

---

## SortTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/utils/SortTest.java`
SGE equivalent: `sge/src/test/scala/sge/utils/SortTest.scala`

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testSortArrayComparable | Sort raw array Comparable | PORTED | "sort array comparable" | Same assertions |
| 2 | testSortArrayWithComparator | Sort array with Comparator | PORTED | "sort array with comparator" | Uses Ordering |
| 3 | testSortArrayWithComparatorAndRange | Sort array range with Comparator | PORTED | "sort array with comparator and range" | Same assertions |
| 4 | testSortArrayRange | Sort raw array with range | PORTED | "sort array range" | Same assertions |
| 5 | testSortArray | Sort Array<> (DynamicArray) | PORTED | "sort DynamicArray comparable" | Same assertions |
| 6 | testSortArrayComparableWithPreExistingComparableTimSort | Sort with pre-existing ComparableTimSort | NOT_APPLICABLE | - | Tests Java reflection on internal Sort fields; not applicable to Scala impl |
| 7 | testSortArrayComparableWithNullComparableTimSort | Sort with null ComparableTimSort | NOT_APPLICABLE | - | Tests Java reflection; not applicable |
| 8 | testSortArrayWithRangeWithNullComparableTimSort | Sort range with null ComparableTimSort | NOT_APPLICABLE | - | Tests Java reflection; not applicable |
| 9 | testSortArrayWithNullTimSort | Sort with null TimSort | NOT_APPLICABLE | - | Tests Java reflection; not applicable |
| 10 | testSortArrayWithNullTimSortArray | Sort Array with null TimSort | NOT_APPLICABLE | - | Tests Java reflection; not applicable |
| 11 | testSortArrayWithComparatorAndRangeWithNullTimSort | Sort range with null TimSort | NOT_APPLICABLE | - | Tests Java reflection; not applicable |
| 12 | testSortArrayWithCustomComparator | Sort with reverse comparator | PORTED | "sort DynamicArray with custom comparator (reverse)" | Same assertions |
| 13 | testSortEmptyArray | Sort empty array | PORTED | "sort empty array" | Same assertions |
| 14 | testSortSingleElementArray | Sort single element | PORTED | "sort single element array" | Same assertions |
| 15 | testSortArrayWithNulls | Sort array with nulls | PORTED | "sort array with nulls using NullsFirst ordering" | Same assertions |
| 16 | testSortArrayRangeWithInvalidIndices | Invalid range throws | PORTED | "sort array range with invalid indices throws" | Same behavior |
| 17 | testSortAlreadySortedArrayComparable | Already sorted DynamicArray | PORTED | "sort already sorted DynamicArray" | Same assertions |
| 18 | testSortArrayWithEqualElements | Equal elements DynamicArray | PORTED | "sort DynamicArray with equal elements" | Same assertions |
| 19 | testSortSingleElementArrayComparable | Single element DynamicArray | PORTED | "sort single element DynamicArray" | Same assertions |
| 20 | testSortEmptyArrayComparable | Empty DynamicArray | PORTED | "sort empty DynamicArray" | Same assertions |

SGE also has 6 additional large-array tests for TimSort coverage.

---

## AtomicQueueTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/utils/AtomicQueueTest.java`
SGE equivalent: none (class skipped)

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | PutTest | AtomicQueue.put capacity check | NOT_APPLICABLE | - | AtomicQueue not ported; replaced by stdlib |
| 2 | PullTest | AtomicQueue.poll | NOT_APPLICABLE | - | AtomicQueue not ported |
| 3 | LoopAroundTest | AtomicQueue wrap-around | NOT_APPLICABLE | - | AtomicQueue not ported |

---

## BitsTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/utils/BitsTest.java`
SGE equivalent: `sge/src/test/scala/sge/utils/QueueBitsTest.scala` (partial coverage)

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testHashcodeAndEquals | Bits hashCode/equals after set/clear | NOT_APPLICABLE | - | Bits replaced by mutable.BitSet; QueueBitsTest covers BitSet operations |
| 2 | testXor | Bits.xor with growth | NOT_APPLICABLE | - | Covered by "Bits: and, or, xor operations" using stdlib |
| 3 | testOr | Bits.or with growth | NOT_APPLICABLE | - | Covered by stdlib test |
| 4 | testAnd | Bits.and | NOT_APPLICABLE | - | Covered by stdlib test |
| 5 | testCopyConstructor | Bits copy constructor | NOT_APPLICABLE | - | mutable.BitSet is a standard collection |

---

## CharArrayTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/utils/CharArrayTest.java`
SGE equivalent: none (class not ported as standalone; CharArray merged into DynamicArray)

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | constructorTest | CharArray constructors (8 variants) | NOT_APPLICABLE | - | CharArray is a StringBuilder-like class; SGE uses DynamicArray or stdlib StringBuilder |
| 2 | addTest | CharArray add/addAll | NOT_APPLICABLE | - | Not a primitive array; string-builder functionality |
| 3 | getSetTest | CharArray get/set/incr/mul | NOT_APPLICABLE | - | Not applicable |
| 4 | removeTest | CharArray remove operations | NOT_APPLICABLE | - | Not applicable |
| 5 | searchTest | CharArray search (indexOf, contains) | NOT_APPLICABLE | - | Not applicable |
| 6 | stackTest | CharArray stack operations | NOT_APPLICABLE | - | Not applicable |
| 7 | arrayOperationsTest | CharArray sort/swap/reverse/truncate | NOT_APPLICABLE | - | Not applicable |
| 8 | appendTest | CharArray append (many types) | NOT_APPLICABLE | - | Not applicable |
| 9 | appendlnTest | CharArray appendln | NOT_APPLICABLE | - | Not applicable |
| 10 | paddingTest | CharArray padding methods | NOT_APPLICABLE | - | Not applicable |
| 11 | deleteTest | CharArray delete methods | NOT_APPLICABLE | - | Not applicable |
| 12 | replaceTest | CharArray replace methods | NOT_APPLICABLE | - | Not applicable |
| 13 | insertTest | CharArray insert methods | NOT_APPLICABLE | - | Not applicable |
| 14 | substringTest | CharArray substring methods | NOT_APPLICABLE | - | Not applicable |
| 15 | stringComparisonTest | CharArray string comparisons | NOT_APPLICABLE | - | Not applicable |
| 16 | charSequenceTest | CharArray CharSequence impl | NOT_APPLICABLE | - | Not applicable |
| 17 | trimCapacityTest | CharArray trim/capacity | NOT_APPLICABLE | - | Not applicable |
| 18 | hashCodeEqualsTest | CharArray hashCode/equals | NOT_APPLICABLE | - | Not applicable |
| 19 | readerWriterTest | CharArray Reader/Writer | NOT_APPLICABLE | - | Not applicable |
| 20 | unicodeTest | CharArray Unicode/codepoint | NOT_APPLICABLE | - | Not applicable |
| 21 | iteratorTest | CharArray iterator/appendAll | NOT_APPLICABLE | - | Not applicable |
| 22 | edgeCasesTest | CharArray edge cases | NOT_APPLICABLE | - | Not applicable |
| 23 | toArrayTest | CharArray toCharArray | NOT_APPLICABLE | - | Not applicable |
| 24 | drainTest | CharArray drain methods | NOT_APPLICABLE | - | Not applicable |
| 25 | appendSeparatorTest | CharArray appendSeparator | NOT_APPLICABLE | - | Not applicable |
| 26 | randomTest | CharArray random/shuffle | NOT_APPLICABLE | - | Not applicable |
| 27 | appendToTest | CharArray appendTo | NOT_APPLICABLE | - | Not applicable |
| 28 | setCharAtTest | CharArray setCharAt | NOT_APPLICABLE | - | Not applicable |
| 29 | toStringAndClearTest | CharArray toStringAndClear | NOT_APPLICABLE | - | Not applicable |
| 30 | toStringWithSeparatorTest | CharArray toString(separator) | NOT_APPLICABLE | - | Not applicable |

---

## JsonMatcherTests.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/utils/JsonMatcherTests.java`
SGE equivalent: none (class not ported)

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | singlePatterns | JsonMatcher single pattern matching (~80 sub-cases) | NOT_APPLICABLE | - | JsonMatcher class does not exist in SGE; SGE uses a completely different JSON approach (JsonCodec) |
| 2 | wholeDocument | JsonMatcher whole document matching | NOT_APPLICABLE | - | Same |
| 3 | unescaping | JsonMatcher unescaping (~25 sub-cases) | NOT_APPLICABLE | - | Same |
| 4 | multiplePatterns | JsonMatcher multiple patterns | NOT_APPLICABLE | - | Same |
| 5 | keys | JsonMatcher key collection | NOT_APPLICABLE | - | Same |
| 6 | earlyEnd | JsonMatcher early end optimization | NOT_APPLICABLE | - | Same |
| 7 | rejection | JsonMatcher rejection/filtering | NOT_APPLICABLE | - | Same |
| 8 | explicitEnd | JsonMatcher explicit end | NOT_APPLICABLE | - | Same |
| 9 | explicitStop | JsonMatcher explicit stop | NOT_APPLICABLE | - | Same |
| 10 | parseValue | JsonMatcher parseValue | NOT_APPLICABLE | - | Same |
| 11 | paths | JsonMatcher path tracking | NOT_APPLICABLE | - | Same |
| 12 | dataTypes | JsonMatcher data type handling | NOT_APPLICABLE | - | Same |
| 13 | filtering | JsonMatcher pattern filtering | NOT_APPLICABLE | - | Same |
| 14 | invalidPattern1-15 | JsonMatcher invalid pattern validation | NOT_APPLICABLE | - | Same |

---

## JsonTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/utils/JsonTest.java`
SGE equivalent: `sge/src/test/scala/sge/utils/JsonTest.scala` (different approach)

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testFromJsonObject | Json.fromJson object | NOT_APPLICABLE | - | SGE uses JsonCodec instead of LibGDX Json class; SGE JsonTest tests completely different API |
| 2 | testFromJsonArray | Json.fromJson array | NOT_APPLICABLE | - | Same |
| 3 | testCharFromNumber | Json.fromJson char from number | NOT_APPLICABLE | - | Same |
| 4 | testReuseReader | Json reader reuse | NOT_APPLICABLE | - | Same |

---

## JsonValueTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/utils/JsonValueTest.java`
SGE equivalent: none (class not ported)

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testAddingRemovedValue | JsonValue add/remove child | NOT_APPLICABLE | - | JsonValue class does not exist in SGE; SGE uses Json AST (Json.Obj/Arr/Str/Num/Bool/Null) |
| 2 | testReplaceValue | JsonValue setChild | NOT_APPLICABLE | - | Same |
| 3 | testCopyConstructor | JsonValue copy constructor | NOT_APPLICABLE | - | Same |

---

## LongArrayTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/utils/LongArrayTest.java`
SGE equivalent: `sge/src/test/scala/sge/utils/PrimitiveArrayTest.scala` (partial coverage via DynamicArray)

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | addTest | LongArray add/addAll | PARTIALLY_PORTED | "Long: add, get, set, removeIndex" | Basic add tested; addAll variants not tested |
| 2 | getTest | LongArray.get | PORTED | "Long: add, get, set, removeIndex" | Covered |
| 3 | setTest | LongArray.set | PORTED | "Long: add, get, set, removeIndex" | Covered |
| 4 | incrTest | LongArray.incr | NOT_PORTED | - | DynamicArray has incr but not tested for Long |
| 5 | mulTest | LongArray.mul | NOT_PORTED | - | DynamicArray has mul but not tested for Long |
| 6 | insertTest | LongArray.insert/insertRange | NOT_PORTED | - | Not tested |
| 7 | swapTest | LongArray.swap | NOT_PORTED | - | Not tested |
| 8 | containsTest | LongArray.contains | PORTED | "Long: contains and indexOf" | Covered |
| 9 | indexOfTest | LongArray.indexOf/lastIndexOf | PORTED | "Long: contains and indexOf" | Covered |
| 10 | removeTest | LongArray removeValue/removeIndex/removeRange/removeAll | PARTIALLY_PORTED | "Long: add, get, set, removeIndex" | Only removeIndex tested |
| 11 | popPeekFirstTest | LongArray pop/peek/first | NOT_PORTED | - | Not tested |
| 12 | emptyTest | LongArray isEmpty/notEmpty | PORTED | "Long: clear, size, isEmpty, toArray" | Covered |
| 13 | clearTest | LongArray.clear | PORTED | "Long: clear, size, isEmpty, toArray" | Covered |
| 14 | shrinkTest | LongArray.shrink | NOT_PORTED | - | Not tested |
| 15 | ensureCapacityTest | LongArray.ensureCapacity | NOT_PORTED | - | Not tested |
| 16 | setSizeTest | LongArray.setSize | NOT_PORTED | - | Not tested |
| 17 | resizeTest | LongArray.resize | NOT_PORTED | - | Not tested |
| 18 | sortAndReverseTest | LongArray.sort/reverse | PARTIALLY_PORTED | "Long: sort" | Sort tested; reverse not tested |
| 19 | equalsTest | LongArray.equals | NOT_PORTED | - | Not tested |

---

## LongQueueTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/utils/LongQueueTest.java`
SGE equivalent: none (class replaced by stdlib)

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | addFirstAndLastTest | LongQueue addFirst/addLast | NOT_APPLICABLE | - | LongQueue replaced by stdlib; QueueBitsTest covers ArrayDeque |
| 2 | removeLastTest | LongQueue removeLast | NOT_APPLICABLE | - | Same |
| 3 | removeFirstTest | LongQueue removeFirst | NOT_APPLICABLE | - | Same |
| 4 | resizableQueueTest | LongQueue resize behavior | NOT_APPLICABLE | - | Same |
| 5 | resizableDequeTest | LongQueue deque resize | NOT_APPLICABLE | - | Same |
| 6 | getTest | LongQueue.get | NOT_APPLICABLE | - | Same |
| 7 | removeTest | LongQueue.removeIndex | NOT_APPLICABLE | - | Same |
| 8 | indexOfTest | LongQueue.indexOf | NOT_APPLICABLE | - | Same |
| 9 | toStringTest | LongQueue.toString | NOT_APPLICABLE | - | Same |
| 10 | hashEqualsTest | LongQueue hash/equals | NOT_APPLICABLE | - | Same |

---

## MixedPutRemoveTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/utils/MixedPutRemoveTest.java`
SGE equivalent: `sge/src/test/scala/sge/utils/ObjectMapTest.scala` (partial)

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testLongMapPut | LongMap put correctness vs HashMap | NOT_APPLICABLE | - | LongMap not ported; replaced by ObjectMap |
| 2 | testLongMapMix | LongMap mixed put/remove vs HashMap | NOT_APPLICABLE | - | Same |
| 3 | testLongMapIterator | LongMap iterator + concurrent add | NOT_APPLICABLE | - | Same |
| 4 | testIntMapPut | IntMap put correctness vs HashMap | NOT_APPLICABLE | - | IntMap not ported |
| 5 | testIntMapMix | IntMap mixed put/remove vs HashMap | NOT_APPLICABLE | - | Same |
| 6 | testIntMapIterator | IntMap iterator + concurrent add | NOT_APPLICABLE | - | Same |
| 7 | testObjectMapPut | ObjectMap put correctness vs HashMap | NOT_PORTED | - | ObjectMap exists in SGE; ObjectMapTest does not test put vs HashMap correctness |
| 8 | testObjectMapMix | ObjectMap mixed put/remove vs HashMap | NOT_PORTED | - | ObjectMap exists in SGE; mixed put/remove not tested against reference |

---

## PooledLinkedListTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/utils/PooledLinkedListTest.java`
SGE equivalent: none (class replaced by stdlib)

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | size | PooledLinkedList.size | NOT_APPLICABLE | - | Replaced by stdlib |
| 2 | iteration | Forward iteration | NOT_APPLICABLE | - | Same |
| 3 | reverseIteration | Reverse iteration | NOT_APPLICABLE | - | Same |
| 4 | remove | Remove during iteration | NOT_APPLICABLE | - | Same |
| 5 | removeLast | Remove last element | NOT_APPLICABLE | - | Same |
| 6 | clear | Clear all | NOT_APPLICABLE | - | Same |

---

## QueueTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/utils/QueueTest.java`
SGE equivalent: `sge/src/test/scala/sge/utils/QueueBitsTest.scala` (stdlib replacement)

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | addFirstAndLastTest | Queue addFirst/addLast | NOT_APPLICABLE | - | Queue replaced by stdlib ArrayDeque; basic tests in QueueBitsTest |
| 2 | removeLastTest | Queue removeLast | NOT_APPLICABLE | - | Same |
| 3 | removeFirstTest | Queue removeFirst | NOT_APPLICABLE | - | Same |
| 4 | resizableQueueTest | Queue resize | NOT_APPLICABLE | - | Same |
| 5 | resizableDequeTest | Queue deque | NOT_APPLICABLE | - | Same |
| 6 | getTest | Queue.get | NOT_APPLICABLE | - | Same |
| 7 | removeTest | Queue.removeIndex | NOT_APPLICABLE | - | Same |
| 8 | indexOfTest | Queue.indexOf | NOT_APPLICABLE | - | Same |
| 9 | iteratorTest | Queue iterator + remove | NOT_APPLICABLE | - | Same |
| 10 | iteratorRemoveEdgeCaseTest | Queue iterator edge case #4300 | NOT_APPLICABLE | - | Same |
| 11 | toStringTest | Queue.toString | NOT_APPLICABLE | - | Same |
| 12 | hashEqualsTest | Queue hash/equals | NOT_APPLICABLE | - | Same |

---

## SortedIntListTest.java
Original path: `original-src/libgdx/gdx/test/com/badlogic/gdx/utils/SortedIntListTest.java`
SGE equivalent: none (class replaced by stdlib)

| # | Test Method | Tests What | Status | SGE Equivalent | Notes |
|---|-------------|-----------|--------|---------------|-------|
| 1 | testIteratorWithAllocation | SortedIntList iterator with allocation | NOT_APPLICABLE | - | SortedIntList replaced by stdlib |

---

# Summary

## Totals by Original Test File

| Original Test File | Total Methods | Ported | Partially Ported | Not Ported | Not Applicable |
|---|---|---|---|---|---|
| AnimationControllerTest.java | 5 | 5 | 0 | 0 | 0 |
| AnimationDescTest.java | 8 | 8 | 0 | 0 | 0 |
| BSplineTest.java | 7 | 7 | 0 | 0 | 0 |
| BezierTest.java | 1 (x6 params) | 1 | 0 | 0 | 0 |
| ConvexHullTest.java | 1 | 1 | 0 | 0 | 0 |
| IntersectorTest.java | 8 | 8 | 0 | 0 | 0 |
| MathUtilsTest.java | 11 | 11 | 0 | 0 | 0 |
| OctreeTest.java | 1 | 0 | 1 | 0 | 0 |
| PolygonTest.java | 4 | 4 | 0 | 0 | 0 |
| RectangleTest.java | 2 | 2 | 0 | 0 | 0 |
| Shape2DTest.java | 2 | 2 | 0 | 0 | 0 |
| Vector2Test.java | 8 | 8 | 0 | 0 | 0 |
| Vector3Test.java | 2 | 2 | 0 | 0 | 0 |
| Vector4Test.java | 2 | 2 | 0 | 0 | 0 |
| CollisionTest.java | 3 | 3 | 0 | 0 | 0 |
| FlushablePoolTest.java | 7 | 7 | 0 | 0 | 0 |
| SortTest.java | 20 | 14 | 0 | 0 | 6 |
| AtomicQueueTest.java | 3 | 0 | 0 | 0 | 3 |
| BitsTest.java | 5 | 0 | 0 | 0 | 5 |
| CharArrayTest.java | 30 | 0 | 0 | 0 | 30 |
| JsonMatcherTests.java | 28 | 0 | 0 | 0 | 28 |
| JsonTest.java | 4 | 0 | 0 | 0 | 4 |
| JsonValueTest.java | 3 | 0 | 0 | 0 | 3 |
| LongArrayTest.java | 19 | 5 | 3 | 11 | 0 |
| LongQueueTest.java | 10 | 0 | 0 | 0 | 10 |
| MixedPutRemoveTest.java | 8 | 0 | 0 | 2 | 6 |
| PooledLinkedListTest.java | 6 | 0 | 0 | 0 | 6 |
| QueueTest.java | 12 | 0 | 0 | 0 | 12 |
| SortedIntListTest.java | 1 | 0 | 0 | 0 | 1 |

## Grand Totals

| Status | Count | Percentage |
|---|---|---|
| **PORTED** | 90 | 42.7% |
| **PARTIALLY_PORTED** | 4 | 1.9% |
| **NOT_PORTED** | 13 | 6.2% |
| **NOT_APPLICABLE** | 104 | 49.3% |
| **TOTAL** | 211 | 100% |

Excluding NOT_APPLICABLE (stdlib replacements / intentionally different APIs):

| Status | Count | Percentage |
|---|---|---|
| **PORTED** | 90 | 84.1% |
| **PARTIALLY_PORTED** | 4 | 3.7% |
| **NOT_PORTED** | 13 | 12.1% |
| **TOTAL (applicable)** | 107 | 100% |

## Priority List -- Tests That SHOULD Be Ported

### High Priority (tested functionality exists in SGE, test is missing)

1. **ObjectMap put/mix correctness tests** (from MixedPutRemoveTest.java)
   - `testObjectMapPut` -- 1M put operations compared against java.util.HashMap
   - `testObjectMapMix` -- 1M mixed put/remove compared against java.util.HashMap
   - ObjectMap exists in SGE; these stress tests verify hash distribution correctness

### Medium Priority (functionality exists via DynamicArray, more thorough testing needed)

2. **DynamicArray[Long] deeper coverage** (from LongArrayTest.java)
   - `insertTest` -- insert/insertRange for DynamicArray
   - `swapTest` -- swap elements
   - `removeTest` (full) -- removeValue, removeRange, removeAll
   - `popPeekFirstTest` -- pop/peek/first stack operations
   - `shrinkTest` -- shrink to fit
   - `ensureCapacityTest` -- capacity management
   - `setSizeTest` -- resize
   - `sortAndReverseTest` (reverse) -- reverse operation
   - `equalsTest` -- equality semantics
   - `incrTest` -- element increment
   - `mulTest` -- element multiplication

### Low Priority (minor gaps)

3. **OctreeTest "insert"** -- fill in the isLeaf assertions and intersectRayBounds collider (currently stubbed)
