/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package jbump

import scala.language.implicitConversions

import sge.jbump.util.Nullable

import scala.collection.mutable.ArrayBuffer

/** Gap-coverage for jbump collision math: project(), Response.{slide,bounce,touch,cross} geometric output, querySegment / querySegmentWithCoords ordering + entry/exit fractions, and the overlapping /
  * filter-null / multi-item edge cases handled by the gdx-jbump originals.
  *
  * Every expected value below is hand-derived from the gdx-jbump Java sources (cited inline by file:line), NOT by running the Scala port. Coordinates, normals, hit order and ti fractions are pinned
  * with tight float deltas so that a mutation in the production math (wrong normal sign, unsorted hits, dropped collision, bounce-using-slide-math, etc.) fails.
  */
class CollisionMathSuite extends munit.FunSuite {

  private val eps = 1e-4f

  // ---------------------------------------------------------------------------
  // project() directly — World.java:240-280, RectHelper.java rect_detectCollision
  // ---------------------------------------------------------------------------

  test("project: single tunneling collision pins touch, normal, ti (RectHelper tunnel branch)") {
    // Layout: player (0,0,1,1) moving to goal (5,0); wall (3,0,1,1).
    // rect_getDiff(0,0,1,1, 3,0,1,1) = (2,-1,2,2)  [Rect.java:125-127]
    // segment (0,0)->(5,0): left side r=(-2)/(-5)=0.4 -> ti1=0.4 nx1=-1; right r=4/5=0.8.
    // tunnel branch (RectHelper.java:95-104): ti=0.4, normal=(-1,0); tx=0+5*0.4=2, ty=0.
    val world  = World[String](64f)
    val player = Item[String]("player")
    world.add(player, 0, 0, 1, 1)
    val wall = Item[String]("wall")
    world.add(wall, 3, 0, 1, 1)

    val cols = Collisions()
    world.project(player, 0, 0, 1, 1, 5, 0, CollisionFilter.defaultFilter, cols)

    assertEquals(cols.size, 1)
    val col = cols.get(0).get
    assertEquals(col.other.get, wall)
    assert(!col.overlaps)
    assertEqualsFloat(col.ti, 0.4f, eps)
    assertEqualsFloat(col.touch.x, 2.0f, eps)
    assertEqualsFloat(col.touch.y, 0.0f, eps)
    assertEquals(col.normal.x, -1)
    assertEquals(col.normal.y, 0)
    assertEqualsFloat(col.move.x, 5.0f, eps)
    assertEqualsFloat(col.move.y, 0.0f, eps)
  }

  test("project: two walls returned sorted by ti ascending (World.java:276-278 collisions.sort)") {
    // wall2 (2,0,1,1) closer -> ti=0.2 touch.x=1 ; wall1 (3,0,1,1) -> ti=0.4 touch.x=2.
    // tileMode default true => collisions.sort() orders nearest-first (Collisions.java sort()).
    val world  = World[String](64f)
    val player = Item[String]("player")
    world.add(player, 0, 0, 1, 1)
    val wall1 = Item[String]("wall1")
    world.add(wall1, 3, 0, 1, 1)
    val wall2 = Item[String]("wall2")
    world.add(wall2, 2, 0, 1, 1)

    val cols = Collisions()
    world.project(player, 0, 0, 1, 1, 5, 0, CollisionFilter.defaultFilter, cols)

    assertEquals(cols.size, 2)
    // Collisions.get reuses a single shared Collision instance (Collisions.java:118), so capture
    // each row's values before requesting the next index.
    val c0       = cols.get(0).get
    val c0Other  = c0.other.get
    val c0Ti     = c0.ti
    val c0TouchX = c0.touch.x
    val c1       = cols.get(1).get
    val c1Other  = c1.other.get
    val c1Ti     = c1.ti
    val c1TouchX = c1.touch.x
    // Nearest (wall2, ti=0.2) must come first; mutation dropping sort or a collision fails here.
    assertEquals(c0Other, wall2)
    assertEqualsFloat(c0Ti, 0.2f, eps)
    assertEqualsFloat(c0TouchX, 1.0f, eps)
    assertEquals(c1Other, wall1)
    assertEqualsFloat(c1Ti, 0.4f, eps)
    assertEqualsFloat(c1TouchX, 2.0f, eps)
  }

  test("project: already-overlapping & not moving -> overlaps, ti=-area, MDV normal (RectHelper.java:114-128)") {
    // player (0,0,2,2) overlaps other (1,1,2,2); goal == origin (dx=dy=0).
    // diff = (-1,-1,4,4); containsPoint(0,0) true => overlaps.
    // nearestCorner: px=-1, py=-1; wi=min(2,1)=1, hi=min(2,1)=1; ti=-1.
    // dx==dy==0 branch: |px|<|py|? 1<1 false => px=0; nx=sign(0)=0, ny=sign(-1)=-1;
    // tx=x1+px=0, ty=y1+py=-1.
    val world  = World[String](64f)
    val player = Item[String]("player")
    world.add(player, 0, 0, 2, 2)
    val other = Item[String]("other")
    world.add(other, 1, 1, 2, 2)

    val cols = Collisions()
    world.project(player, 0, 0, 2, 2, 0, 0, CollisionFilter.defaultFilter, cols)

    assertEquals(cols.size, 1)
    val col = cols.get(0).get
    assert(col.overlaps)
    assertEqualsFloat(col.ti, -1.0f, eps)
    assertEquals(col.normal.x, 0)
    assertEquals(col.normal.y, -1)
    assertEqualsFloat(col.touch.x, 0.0f, eps)
    assertEqualsFloat(col.touch.y, -1.0f, eps)
  }

  test("project: filter returning null skips that item (World.java:264-265)") {
    // Two walls in range, but the filter rejects wall1 -> only wall2 collides.
    val world  = World[String](64f)
    val player = Item[String]("player")
    world.add(player, 0, 0, 1, 1)
    val wall1 = Item[String]("wall1")
    world.add(wall1, 3, 0, 1, 1)
    val wall2 = Item[String]("wall2")
    world.add(wall2, 2, 0, 1, 1)

    val skipWall1: CollisionFilter = new CollisionFilter {
      override def filter(item: Item[?], other: Nullable[Item[?]]): Nullable[Response] =
        if (other.isDefined && (other.get eq wall1)) Nullable.Null else Response.slide
    }

    val cols = Collisions()
    world.project(player, 0, 0, 1, 1, 5, 0, skipWall1, cols)

    assertEquals(cols.size, 1)
    assertEquals(cols.get(0).get.other.get, wall2)
  }

  // ---------------------------------------------------------------------------
  // Response geometric math — Response.java slide/bounce/touch/cross
  // ---------------------------------------------------------------------------

  // Helper: build the one collision produced by projecting player->goal against a single wall.
  private def firstCollision(
    world:  World[String],
    player: Item[String],
    px:     Float,
    py:     Float,
    pw:     Float,
    ph:     Float,
    goalX:  Float,
    goalY:  Float
  ): Collision = {
    val cols = Collisions()
    world.project(player, px, py, pw, ph, goalX, goalY, CollisionFilter.defaultFilter, cols)
    cols.get(0).get
  }

  test("Response.bounce reflects across X normal -> goal (-1,0) (Response.java:82-108)") {
    // player(0,0,1,1) -> goal(5,0), wall(3,0,1,1). Collision: touch=(2,0), normal=(-1,0), move=(5,0).
    // bounce: bnx=goalX-tch.x=3; normal.x(-1)!=0 => bnx=-3; bx=2-3=-1, by=0.
    val world  = World[String](64f)
    val player = Item[String]("player")
    world.add(player, 0, 0, 1, 1)
    val wall = Item[String]("wall")
    world.add(wall, 3, 0, 1, 1)

    val col    = firstCollision(world, player, 0, 0, 1, 1, 5, 0)
    val result = Response.Result()
    Response.bounce.response(world, col, 0, 0, 1, 1, 5, 0, CollisionFilter.defaultFilter, result)

    // Mutation that makes bounce use slide math (sx=goalX) would yield goalX=5, not -1.
    assertEqualsFloat(result.goalX, -1.0f, eps)
    assertEqualsFloat(result.goalY, 0.0f, eps)
    // Re-projecting from touch (2,0) moving left to (-1,0) away from the wall -> no new collisions.
    assertEquals(result.projectedCollisions.size, 0)
  }

  test("Response.slide keeps goalY because normal.x != 0 (Response.java:38-61)") {
    // player(0,0,1,1) -> goal(5,2), wall(3,0,1,1). Collision: touch=(2,0.8), normal=(-1,0).
    // slide: normal.x(-1)!=0 => sy=goalY=2; sx stays touch.x=2. New goal (2,2).
    val world  = World[String](64f)
    val player = Item[String]("player")
    world.add(player, 0, 0, 1, 1)
    val wall = Item[String]("wall")
    world.add(wall, 3, 0, 1, 1)

    val col = firstCollision(world, player, 0, 0, 1, 1, 5, 2)
    // pin the underlying touch point the response consumes
    assertEqualsFloat(col.touch.x, 2.0f, eps)
    assertEqualsFloat(col.touch.y, 0.8f, eps)
    assertEquals(col.normal.x, -1)

    val result = Response.Result()
    Response.slide.response(world, col, 0, 0, 1, 1, 5, 2, CollisionFilter.defaultFilter, result)

    // Mutation flipping the normal.x==0 test would set sx=goalX=5 instead of sy=goalY.
    assertEqualsFloat(result.goalX, 2.0f, eps)
    assertEqualsFloat(result.goalY, 2.0f, eps)
  }

  test("Response.touch sets goal exactly to touch point and clears collisions (Response.java:63-70)") {
    val world  = World[String](64f)
    val player = Item[String]("player")
    world.add(player, 0, 0, 1, 1)
    val wall = Item[String]("wall")
    world.add(wall, 3, 0, 1, 1)

    val col    = firstCollision(world, player, 0, 0, 1, 1, 5, 0)
    val result = Response.Result()
    Response.touch.response(world, col, 0, 0, 1, 1, 5, 0, CollisionFilter.defaultFilter, result)

    assertEqualsFloat(result.goalX, 2.0f, eps) // touch.x
    assertEqualsFloat(result.goalY, 0.0f, eps) // touch.y
    assertEquals(result.projectedCollisions.size, 0)
  }

  test("Response.cross passes through to the original goal (Response.java:72-80)") {
    val world  = World[String](64f)
    val player = Item[String]("player")
    world.add(player, 0, 0, 1, 1)
    val wall = Item[String]("wall")
    world.add(wall, 3, 0, 1, 1)

    val crossFilter: CollisionFilter = new CollisionFilter {
      override def filter(item: Item[?], other: Nullable[Item[?]]): Nullable[Response] = Response.cross
    }
    val col    = firstCollision(world, player, 0, 0, 1, 1, 5, 0)
    val result = Response.Result()
    Response.cross.response(world, col, 0, 0, 1, 1, 5, 0, crossFilter, result)

    // cross keeps the original goal (no clamping to the touch point).
    assertEqualsFloat(result.goalX, 5.0f, eps)
    assertEqualsFloat(result.goalY, 0.0f, eps)
  }

  // ---------------------------------------------------------------------------
  // querySegment / querySegmentWithCoords — World.java:169-201, 525-560
  // ---------------------------------------------------------------------------

  test("querySegment returns hits sorted by distance along the segment (World.java:199 sort)") {
    // Horizontal segment (-1,0.5)->(5,0.5) crossing itemA(0,0,1,1) then itemB(3,0,1,1).
    // ti1: itemA = (-1)/(-6)=0.1667 ; itemB = (-4)/(-6)=0.6667. weightComparator sorts ascending.
    val world = World[String](64f)
    val itemA = Item[String]("A")
    world.add(itemA, 0, 0, 1, 1)
    val itemB = Item[String]("B")
    world.add(itemB, 3, 0, 1, 1)

    val items = ArrayBuffer.empty[Item[?]]
    world.querySegment(-1, 0.5f, 5, 0.5f, CollisionFilter.defaultFilter, items)

    assertEquals(items.size, 2)
    // Mutation that returns hits unsorted/reversed would put B before A here.
    assertEquals(items(0), itemA)
    assertEquals(items(1), itemB)
  }

  test("querySegmentWithCoords pins entry/exit ti fractions and coords (World.java:542-560)") {
    val world = World[String](64f)
    val itemA = Item[String]("A")
    world.add(itemA, 0, 0, 1, 1)
    val itemB = Item[String]("B")
    world.add(itemB, 3, 0, 1, 1)

    val infos = ArrayBuffer.empty[ItemInfo]
    world.querySegmentWithCoords(-1, 0.5f, 5, 0.5f, CollisionFilter.defaultFilter, infos)

    assertEquals(infos.size, 2)
    val a = infos(0)
    val b = infos(1)
    assertEquals(a.item, itemA)
    assertEquals(b.item, itemB)

    // dx=6, dy=0. itemA enters left edge x=0 (ti1=1/6), exits right edge x=1 (ti2=1/3).
    assertEqualsFloat(a.ti1, 1f / 6f, eps)
    assertEqualsFloat(a.ti2, 1f / 3f, eps)
    assertEqualsFloat(a.x1, 0.0f, eps)
    assertEqualsFloat(a.y1, 0.5f, eps)
    assertEqualsFloat(a.x2, 1.0f, eps)
    assertEqualsFloat(a.y2, 0.5f, eps)

    // itemB enters left edge x=3 (ti1=2/3), exits right edge x=4 (ti2=5/6).
    assertEqualsFloat(b.ti1, 2f / 3f, eps)
    assertEqualsFloat(b.ti2, 5f / 6f, eps)
    assertEqualsFloat(b.x1, 3.0f, eps)
    assertEqualsFloat(b.x2, 4.0f, eps)
    assertEqualsFloat(b.y1, 0.5f, eps)
    assertEqualsFloat(b.y2, 0.5f, eps)
  }

  test("querySegment filter returning null skips that item (World.java:178/210)") {
    val world = World[String](64f)
    val itemA = Item[String]("A")
    world.add(itemA, 0, 0, 1, 1)
    val itemB = Item[String]("B")
    world.add(itemB, 3, 0, 1, 1)

    val skipB: CollisionFilter = new CollisionFilter {
      override def filter(item: Item[?], other: Nullable[Item[?]]): Nullable[Response] =
        if (item eq itemB) Nullable.Null else Response.slide
    }

    val items = ArrayBuffer.empty[Item[?]]
    world.querySegment(-1, 0.5f, 5, 0.5f, skipB, items)

    assertEquals(items.size, 1)
    assertEquals(items(0), itemA)
  }
}
