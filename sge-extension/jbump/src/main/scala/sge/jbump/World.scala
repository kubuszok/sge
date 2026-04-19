/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 784
 * Covenant-baseline-methods: World,add,addItemToCell,add_c,cell,cellMap,cellMaxX,cellMaxY,cellMinX,cellMinY,ch,check,check_cols,check_projectedCols,check_result,check_visited,cl,cols,countCells,countItems,ct,currentGoalX,currentGoalY,cw,cx,cy,dictItemsInCellRect,dx,dy,filter,getCells,getCellsTouchedByRay,getCellsTouchedBySegment,getCellsTouchedBySegment_visited,getDictItemsInCellRect,getInfoAboutItemsTouchedByRay,getInfoAboutItemsTouchedBySegment,getItems,getRect,getRects,grid,h,h1,hasItem,i,info_cells,info_normalX,info_normalY,info_ti,info_visited,infos,move,nonEmptyCells,onTraverse,outerFilter,project,project_c,project_dictItemsInCellRect,project_visited,projectedCols,pt,queryPoint,queryRay,queryRayWithCoords,queryRect,querySegment,querySegmentWithCoords,query_c,query_dictItemsInCellRect,query_infos,query_point,rect,rectHelper,rects,remove,removeItemFromCell,remove_c,reset,result,tb,th,tileMode,tl,toCell,toWorld,tr,tt,tw,update,update_c1,update_c2,visited,visitedFilter,w,w1,x,x1,y,y1
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package jbump

import scala.language.implicitConversions

import sge.jbump.util.Nullable

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/** Main API. AABB collision world. Generic over item type E. */
class World[E](val cellSize: Float = 64f) {

  private val cellMap       = mutable.HashMap.empty[Point, Cell]
  private val nonEmptyCells = mutable.HashSet.empty[Cell]
  private var cellMinX      = 0f
  private var cellMinY      = 0f
  private var cellMaxX      = 0f
  private var cellMaxY      = 0f
  private val grid          = Grid()
  private val rectHelper    = RectHelper()
  var tileMode: Boolean = true

  private def addItemToCell(item: Item[E], cx: Float, cy: Float): Unit = {
    val pt   = Point(cx, cy)
    val cell = cellMap.getOrElseUpdate(
      pt, {
        val c = Cell()
        if (cx < cellMinX) cellMinX = cx
        if (cy < cellMinY) cellMinY = cy
        if (cx > cellMaxX) cellMaxX = cx
        if (cy > cellMaxY) cellMaxY = cy
        c
      }
    )
    nonEmptyCells.add(cell)
    if (!cell.items.contains(item)) {
      cell.items.add(item)
      cell.itemCount = cell.itemCount + 1
    }
  }

  private def removeItemFromCell(item: Item[?], cx: Float, cy: Float): Boolean = {
    val pt = Point(cx, cy)
    cellMap.get(pt) match {
      case None       => false
      case Some(cell) =>
        if (!cell.items.contains(item)) {
          false
        } else {
          cell.items.remove(item)
          cell.itemCount = cell.itemCount - 1
          if (cell.itemCount == 0) {
            nonEmptyCells.remove(cell)
          }
          true
        }
    }
  }

  private def getDictItemsInCellRect(
    cl:     Float,
    ct:     Float,
    cw:     Float,
    ch:     Float,
    result: mutable.LinkedHashSet[Item[?]]
  ): mutable.LinkedHashSet[Item[?]] = {
    result.clear()
    val pt = Point(cl, ct)
    var cy = ct
    while (cy < ct + ch) {
      pt.y = cy
      pt.x = cl
      var cx = cl
      while (cx < cl + cw) {
        pt.x = cx
        cellMap.get(pt).foreach { cell =>
          if (cell.itemCount > 0) { // no cell.itemCount > 1 because tunneling
            result.addAll(cell.items)
          }
        }
        cx += 1
      }
      cy += 1
    }
    result
  }

  private val getCellsTouchedBySegment_visited = ArrayBuffer.empty[Cell]

  def getCellsTouchedBySegment(x1: Float, y1: Float, x2: Float, y2: Float, result: ArrayBuffer[Cell]): ArrayBuffer[Cell] = {
    result.clear()
    getCellsTouchedBySegment_visited.clear()
    val visited = getCellsTouchedBySegment_visited
    val pt      = Point(x1, y1)
    grid.grid_traverse(
      cellSize,
      x1,
      y1,
      x2,
      y2,
      new grid.TraverseCallback {
        override def onTraverse(cx: Float, cy: Float, stepX: Int, stepY: Int): Boolean =
          // stop if cell coordinates are outside of the world.
          if (
            (stepX == -1 && cx < cellMinX) || (stepX == 1 && cx > cellMaxX)
            || (stepY == -1 && cy < cellMinY) || (stepY == 1 && cy > cellMaxY)
          ) {
            false
          } else {
            pt.x = cx
            pt.y = cy
            cellMap.get(pt) match {
              case None       => true
              case Some(cell) =>
                if (visited.contains(cell)) {
                  true
                } else {
                  visited += cell
                  result += cell
                  true
                }
            }
          }
      }
    )
    result
  }

  def getCellsTouchedByRay(
    originX: Float,
    originY: Float,
    dirX:    Float,
    dirY:    Float,
    result:  ArrayBuffer[Cell]
  ): ArrayBuffer[Cell] = {
    result.clear()
    getCellsTouchedBySegment_visited.clear()
    val visited = getCellsTouchedBySegment_visited
    val pt      = Point(originX, originY)
    grid.grid_traverseRay(
      cellSize,
      originX,
      originY,
      dirX,
      dirY,
      new grid.TraverseCallback {
        override def onTraverse(cx: Float, cy: Float, stepX: Int, stepY: Int): Boolean =
          // stop if cell coordinates are outside of the world.
          if (
            (stepX == -1 && cx < cellMinX) || (stepX == 1 && cx > cellMaxX)
            || (stepY == -1 && cy < cellMinY) || (stepY == 1 && cy > cellMaxY)
          ) {
            false
          } else {
            pt.x = cx
            pt.y = cy
            cellMap.get(pt) match {
              case None       => true
              case Some(cell) =>
                if (visited.contains(cell)) {
                  true
                } else {
                  visited += cell
                  result += cell
                  true
                }
            }
          }
      }
    )
    result
  }

  private val info_cells   = ArrayBuffer.empty[Cell]
  private val info_ti      = Point()
  private val info_normalX = IntPoint()
  private val info_normalY = IntPoint()
  private val info_visited = ArrayBuffer.empty[Item[?]]

  private def getInfoAboutItemsTouchedBySegment(
    x1:     Float,
    y1:     Float,
    x2:     Float,
    y2:     Float,
    filter: Nullable[CollisionFilter],
    infos:  ArrayBuffer[ItemInfo]
  ): ArrayBuffer[ItemInfo] = {
    info_visited.clear()
    infos.clear()
    getCellsTouchedBySegment(x1, y1, x2, y2, info_cells)

    for (cell <- info_cells)
      for (item <- cell.items)
        if (!info_visited.contains(item)) {
          info_visited += item
          if (filter.isEmpty || filter.get.filter(item, Nullable.Null).isDefined) {
            val rect = rects(item)
            val l    = rect.x
            val t    = rect.y
            val w    = rect.w
            val h    = rect.h

            if (Rect.rect_getSegmentIntersectionIndices(l, t, w, h, x1, y1, x2, y2, 0, 1, info_ti, info_normalX, info_normalY)) {
              val ti1 = info_ti.x
              val ti2 = info_ti.y
              if ((0 < ti1 && ti1 < 1) || (0 < ti2 && ti2 < 1)) {
                Rect.rect_getSegmentIntersectionIndices(
                  l,
                  t,
                  w,
                  h,
                  x1,
                  y1,
                  x2,
                  y2,
                  -Float.MaxValue,
                  Float.MaxValue,
                  info_ti,
                  info_normalX,
                  info_normalY
                )
                val tii0 = info_ti.x
                val tii1 = info_ti.y
                infos += ItemInfo(item, ti1, ti2, Math.min(tii0, tii1))
              }
            }
          }
        }
    infos.sortInPlace()(using ItemInfo.weightComparator)
    infos
  }

  private def getInfoAboutItemsTouchedByRay(
    originX: Float,
    originY: Float,
    dirX:    Float,
    dirY:    Float,
    filter:  Nullable[CollisionFilter],
    infos:   ArrayBuffer[ItemInfo]
  ): ArrayBuffer[ItemInfo] = {
    info_visited.clear()
    infos.clear()
    getCellsTouchedByRay(originX, originY, dirX, dirY, info_cells)

    for (cell <- info_cells)
      for (item <- cell.items)
        if (!info_visited.contains(item)) {
          info_visited += item
          if (filter.isEmpty || filter.get.filter(item, Nullable.Null).isDefined) {
            val rect = rects(item)
            val l    = rect.x
            val t    = rect.y
            val w    = rect.w
            val h    = rect.h

            if (
              Rect.rect_getSegmentIntersectionIndices(
                l,
                t,
                w,
                h,
                originX,
                originY,
                originX + dirX,
                originY + dirY,
                0,
                Float.MaxValue,
                info_ti,
                info_normalX,
                info_normalY
              )
            ) {
              val ti1 = info_ti.x
              val ti2 = info_ti.y
              infos += ItemInfo(item, ti1, ti2, Math.min(ti1, ti2))
            }
          }
        }
    infos.sortInPlace()(using ItemInfo.weightComparator)
    infos
  }

  def project(
    item:       Nullable[Item[?]],
    x:          Float,
    y:          Float,
    w:          Float,
    h:          Float,
    goalX:      Float,
    goalY:      Float,
    collisions: Collisions
  ): Collisions =
    project(item, x, y, w, h, goalX, goalY, CollisionFilter.defaultFilter, collisions)

  private val project_visited             = ArrayBuffer.empty[Item[?]]
  private val project_c                   = Rect()
  private val project_dictItemsInCellRect = mutable.LinkedHashSet.empty[Item[?]]

  def project(
    item:       Nullable[Item[?]],
    x:          Float,
    y:          Float,
    w:          Float,
    h:          Float,
    goalX:      Float,
    goalY:      Float,
    filter:     CollisionFilter,
    collisions: Collisions
  ): Collisions = {
    collisions.clear()
    val visited = project_visited
    visited.clear()
    if (item.isDefined) {
      visited += item.get
    }

    /* This could probably be done with less cells using a polygon raster over the cells instead of a
    bounding rect of the whole movement. Conditional to building a queryPolygon method */
    val tl = Math.min(goalX, x)
    val tt = Math.min(goalY, y)
    val tr = Math.max(goalX + w, x + w)
    val tb = Math.max(goalY + h, y + h)

    val tw = tr - tl
    val th = tb - tt

    grid.grid_toCellRect(cellSize, tl, tt, tw, th, project_c)
    val cl                  = project_c.x
    val ct                  = project_c.y
    val cw                  = project_c.w
    val ch                  = project_c.h
    val dictItemsInCellRect = getDictItemsInCellRect(cl, ct, cw, ch, project_dictItemsInCellRect)
    for (other <- dictItemsInCellRect)
      if (!visited.contains(other)) {
        visited += other
        val response = filter.filter(item.getOrElse(null.asInstanceOf[Item[?]]), other)
        if (response.isDefined) {
          val o   = getRect(other)
          val ox  = o.x
          val oy  = o.y
          val ow  = o.w
          val oh  = o.h
          val col = rectHelper.rect_detectCollision(x, y, w, h, ox, oy, ow, oh, goalX, goalY)

          if (col.isDefined) {
            val c = col.get
            collisions.add(
              c.overlaps,
              c.ti,
              c.move.x,
              c.move.y,
              c.normal.x,
              c.normal.y,
              c.touch.x,
              c.touch.y,
              c.itemRect.x,
              c.itemRect.y,
              c.itemRect.w,
              c.itemRect.h,
              c.otherRect.x,
              c.otherRect.y,
              c.otherRect.w,
              c.otherRect.h,
              item,
              other,
              response
            )
          }
        }
      }
    if (tileMode) {
      collisions.sort()
    }
    collisions
  }

  private val rects = mutable.HashMap.empty[Item[?], Rect]

  def getRect(item: Item[?]): Rect = rects(item)

  def getItems: collection.Set[Item[?]] = rects.keySet

  def getRects: Iterable[Rect] = rects.values

  def getCells: Iterable[Cell] = cellMap.values

  def countCells: Int = cellMap.size

  def hasItem(item: Item[?]): Boolean = rects.contains(item)

  def countItems: Int = rects.size

  def toWorld(cx: Float, cy: Float, result: Point): Point = {
    Grid.grid_toWorld(cellSize, cx, cy, result)
    result
  }

  def toCell(x: Float, y: Float, result: Point): Point = {
    Grid.grid_toCell(cellSize, x, y, result)
    result
  }

  private val add_c = Rect()

  def add(item: Item[E], x: Float, y: Float, w: Float, h: Float): Item[E] =
    if (rects.contains(item)) {
      item
    } else {
      rects.put(item, Rect(x, y, w, h))
      grid.grid_toCellRect(cellSize, x, y, w, h, add_c)
      val cl = add_c.x
      val ct = add_c.y
      val cw = add_c.w
      val ch = add_c.h
      var cy = ct
      while (cy < ct + ch) {
        var cx = cl
        while (cx < cl + cw) {
          addItemToCell(item, cx, cy)
          cx += 1
        }
        cy += 1
      }
      item
    }

  private val remove_c = Rect()

  def remove(item: Item[?]): Unit = {
    val rect = getRect(item)
    val x    = rect.x
    val y    = rect.y
    val w    = rect.w
    val h    = rect.h

    rects.remove(item)
    grid.grid_toCellRect(cellSize, x, y, w, h, remove_c)
    val cl = remove_c.x
    val ct = remove_c.y
    val cw = remove_c.w
    val ch = remove_c.h

    var cy = ct
    while (cy < ct + ch) {
      var cx = cl
      while (cx < cl + cw) {
        removeItemFromCell(item, cx, cy)
        cx += 1
      }
      cy += 1
    }
  }

  def reset(): Unit = {
    rects.clear()
    cellMap.clear()
    nonEmptyCells.clear()
  }

  def update(item: Item[?], x2: Float, y2: Float): Unit = {
    val rect = getRect(item)
    update(item, x2, y2, rect.w, rect.h)
  }

  private val update_c1 = Rect()
  private val update_c2 = Rect()

  def update(item: Item[?], x2: Float, y2: Float, w2: Float, h2: Float): Unit = {
    val rect = getRect(item)
    val x1   = rect.x
    val y1   = rect.y
    val w1   = rect.w
    val h1   = rect.h
    if (x1 != x2 || y1 != y2 || w1 != w2 || h1 != h2) {

      val c1 = grid.grid_toCellRect(cellSize, x1, y1, w1, h1, update_c1)
      val c2 = grid.grid_toCellRect(cellSize, x2, y2, w2, h2, update_c2)

      val cl1 = c1.x; val ct1 = c1.y; val cw1 = c1.w; val ch1 = c1.h
      val cl2 = c2.x; val ct2 = c2.y; val cw2 = c2.w; val ch2 = c2.h

      if (cl1 != cl2 || ct1 != ct2 || cw1 != cw2 || ch1 != ch2) {
        val cr1 = cl1 + cw1 - 1
        val cb1 = ct1 + ch1 - 1
        val cr2 = cl2 + cw2 - 1
        val cb2 = ct2 + ch2 - 1

        var cy = ct1
        while (cy <= cb1) {
          val cyOut = cy < ct2 || cy > cb2
          var cx    = cl1
          while (cx <= cr1) {
            if (cyOut || cx < cl2 || cx > cr2) {
              removeItemFromCell(item, cx, cy)
            }
            cx += 1
          }
          cy += 1
        }

        cy = ct2
        while (cy <= cb2) {
          val cyOut = cy < ct1 || cy > cb1
          var cx    = cl2
          while (cx <= cr2) {
            if (cyOut || cx < cl1 || cx > cr1) {
              addItemToCell(item.asInstanceOf[Item[E]], cx, cy)
            }
            cx += 1
          }
          cy += 1
        }
      }
      rect.set(x2, y2, w2, h2)
    }
  }

  private val check_visited       = ArrayBuffer.empty[Item[?]]
  private val check_cols          = Collisions()
  private val check_projectedCols = Collisions()
  private val check_result        = Response.Result()

  def check(item: Item[E], goalX: Float, goalY: Float, filter: CollisionFilter): Response.Result = {
    val visited = check_visited
    visited.clear()
    visited += item

    val outerFilter = filter
    val visitedFilter: CollisionFilter = new CollisionFilter {
      override def filter(filterItem: Item[?], other: Nullable[Item[?]]): Nullable[Response] =
        if (other.isDefined && visited.contains(other.get)) {
          Nullable.Null
        } else {
          outerFilter.filter(filterItem, other)
        }
    }

    val rect = getRect(item)
    val x    = rect.x
    val y    = rect.y
    val w    = rect.w
    val h    = rect.h
    val cols = check_cols
    cols.clear()
    var currentGoalX  = goalX
    var currentGoalY  = goalY
    var projectedCols = project(item, x, y, w, h, currentGoalX, currentGoalY, filter, check_projectedCols)
    val result        = check_result
    while (projectedCols != null && !projectedCols.isEmpty) {
      val col = projectedCols.get(0).get
      cols.add(
        col.overlaps,
        col.ti,
        col.move.x,
        col.move.y,
        col.normal.x,
        col.normal.y,
        col.touch.x,
        col.touch.y,
        col.itemRect.x,
        col.itemRect.y,
        col.itemRect.w,
        col.itemRect.h,
        col.otherRect.x,
        col.otherRect.y,
        col.otherRect.w,
        col.otherRect.h,
        col.item,
        col.other,
        col.`type`
      )

      visited += col.other.get

      val response = col.`type`.get
      response.response(this, col, x, y, w, h, currentGoalX, currentGoalY, visitedFilter, result)
      currentGoalX = result.goalX
      currentGoalY = result.goalY
      projectedCols = result.projectedCollisions
    }

    result.set(currentGoalX, currentGoalY)
    result.projectedCollisions.clear()
    var i = 0
    while (i < cols.size) {
      result.projectedCollisions.add(cols.get(i).get)
      i += 1
    }
    result
  }

  def move(item: Item[E], goalX: Float, goalY: Float, filter: CollisionFilter): Response.Result = {
    val result = check(item, goalX, goalY, filter)
    update(item, result.goalX, result.goalY)
    result
  }

  private val query_c                   = Rect()
  private val query_dictItemsInCellRect = mutable.LinkedHashSet.empty[Item[?]]

  /** A collision check of items that intersect the given rectangle.
    * @param filter
    *   Defines what items will be checked for collision. "item" is the Item checked for collision. "other" is null.
    * @param items
    *   An empty list that will be filled with the Item instances that collide with the rectangle.
    */
  def queryRect(
    x:      Float,
    y:      Float,
    w:      Float,
    h:      Float,
    filter: CollisionFilter,
    items:  ArrayBuffer[Item[?]]
  ): ArrayBuffer[Item[?]] = {
    items.clear()
    grid.grid_toCellRect(cellSize, x, y, w, h, query_c)
    val cl                  = query_c.x
    val ct                  = query_c.y
    val cw                  = query_c.w
    val ch                  = query_c.h
    val dictItemsInCellRect = getDictItemsInCellRect(cl, ct, cw, ch, query_dictItemsInCellRect)

    for (item <- dictItemsInCellRect) {
      val rect = rects(item)
      if (filter.filter(item, Nullable.Null).isDefined && Rect.rect_isIntersecting(x, y, w, h, rect.x, rect.y, rect.w, rect.h)) {
        items += item
      }
    }
    items
  }

  private val query_point = Point()

  /** A collision check of items that intersect the given point.
    * @param filter
    *   Defines what items will be checked for collision. "item" is the Item checked for collision. "other" is null.
    * @param items
    *   An empty list that will be filled with the Item instances that collide with the point.
    */
  def queryPoint(x: Float, y: Float, filter: CollisionFilter, items: ArrayBuffer[Item[?]]): ArrayBuffer[Item[?]] = {
    items.clear()
    toCell(x, y, query_point)
    val cx                  = query_point.x
    val cy                  = query_point.y
    val dictItemsInCellRect = getDictItemsInCellRect(cx, cy, 1, 1, query_dictItemsInCellRect)

    for (item <- dictItemsInCellRect) {
      val rect = rects(item)
      if (filter.filter(item, Nullable.Null).isDefined && Rect.rect_containsPoint(rect.x, rect.y, rect.w, rect.h, x, y)) {
        items += item
      }
    }
    items
  }

  /** A collision check of items that intersect the given line segment.
    * @param filter
    *   Defines what items will be checked for collision. "item" is the Item checked for collision. "other" is null.
    * @param items
    *   An empty list that will be filled with the Item instances that intersect the segment.
    */
  private val query_infos = ArrayBuffer.empty[ItemInfo]

  def querySegment(
    x1:     Float,
    y1:     Float,
    x2:     Float,
    y2:     Float,
    filter: CollisionFilter,
    items:  ArrayBuffer[Item[?]]
  ): ArrayBuffer[Item[?]] = {
    items.clear()
    val infos = getInfoAboutItemsTouchedBySegment(x1, y1, x2, y2, filter, query_infos)
    for (info <- infos)
      items += info.item
    items
  }

  /** A collision check of items that intersect the given line segment. Returns more details about where the collision occurs compared to querySegment.
    * @param filter
    *   Defines what items will be checked for collision. "item" is the Item checked for collision. "other" is null
    * @param infos
    *   An empty list that will be filled with the collision information.
    */
  def querySegmentWithCoords(
    x1:     Float,
    y1:     Float,
    x2:     Float,
    y2:     Float,
    filter: CollisionFilter,
    infos:  ArrayBuffer[ItemInfo]
  ): ArrayBuffer[ItemInfo] = {
    infos.clear()
    val result = getInfoAboutItemsTouchedBySegment(x1, y1, x2, y2, filter, infos)
    val dx     = x2 - x1
    val dy     = y2 - y1

    for (info <- result) {
      val ti1 = info.ti1
      val ti2 = info.ti2

      info.weight = 0
      info.x1 = x1 + dx * ti1
      info.y1 = y1 + dy * ti1
      info.x2 = x1 + dx * ti2
      info.y2 = y1 + dy * ti2
    }
    result
  }

  /** A collision check of items that intersect the given ray.
    * @param originX
    *   The x-origin of the ray.
    * @param originY
    *   The y-origin of the ray.
    * @param dirX
    *   The x component of the vector that defines the angle of the ray.
    * @param dirY
    *   The y component of the vector that defines the angle of the ray.
    * @param filter
    *   Defines what items will be checked for collision. "item" is the Item checked for collision. "other" is null.
    * @param items
    *   An empty list that will be filled with the Item instances that intersect the ray.
    */
  def queryRay(
    originX: Float,
    originY: Float,
    dirX:    Float,
    dirY:    Float,
    filter:  CollisionFilter,
    items:   ArrayBuffer[Item[?]]
  ): ArrayBuffer[Item[?]] = {
    items.clear()
    val infos = getInfoAboutItemsTouchedByRay(originX, originY, dirX, dirY, filter, query_infos)
    for (info <- infos)
      items += info.item
    items
  }

  /** A collision check of items that intersect the given ray. Returns more details about where the collision occurs compared to queryRay.
    * @param originX
    *   The x-origin of the ray.
    * @param originY
    *   The y-origin of the ray.
    * @param dirX
    *   The x component of the vector that defines the angle of the ray.
    * @param dirY
    *   The y component of the vector that defines the angle of the ray.
    * @param filter
    *   Defines what items will be checked for collision. "item" is the Item checked for collision. "other" is null
    * @param infos
    *   An empty list that will be filled with the collision information.
    */
  def queryRayWithCoords(
    originX: Float,
    originY: Float,
    dirX:    Float,
    dirY:    Float,
    filter:  CollisionFilter,
    infos:   ArrayBuffer[ItemInfo]
  ): ArrayBuffer[ItemInfo] = {
    infos.clear()
    val result = getInfoAboutItemsTouchedByRay(originX, originY, dirX, dirY, filter, infos)

    for (info <- result) {
      val ti1 = info.ti1
      val ti2 = info.ti2

      info.weight = 0
      info.x1 = originX + dirX * ti1
      info.y1 = originY + dirY * ti1
      info.x2 = originX + dirX * ti2
      info.y2 = originY + dirY * ti2
    }
    result
  }
}
