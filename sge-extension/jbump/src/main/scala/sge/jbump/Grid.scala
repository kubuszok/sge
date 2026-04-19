/*
 * Ported from jbump - https://github.com/tommyettinger/jbump
 * Licensed under the MIT License
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 136
 * Covenant-baseline-methods: Grid,TraverseCallback,cb,cont,cr,cx,cx1,cx2,cy,cy1,cy2,dx,dy,grid_toCell,grid_toCellRect,grid_toCellRect_cxy,grid_toWorld,grid_traverse,grid_traverseRay,grid_traverse_c1,grid_traverse_c2,grid_traverse_initStep,grid_traverse_initStepX,grid_traverse_initStepY,onTraverse,stepX,stepY,tx,ty,v
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package jbump

/** grid_traverse* methods are based on "A Fast Voxel Traversal Algorithm for Ray Tracing", by John Amanides and Andrew Woo - http://www.cse.yorku.ca/~amana/research/grid.pdf It has been modified to
  * include both cells when the ray "touches a grid corner", and with a different exit condition
  */
class Grid {

  /** Callback interface for grid traversal. */
  trait TraverseCallback {
    def onTraverse(cx: Float, cy: Float, stepX: Int, stepY: Int): Boolean
  }

  private val grid_traverse_c1        = Point()
  private val grid_traverse_c2        = Point()
  private val grid_traverse_initStepX = Point()
  private val grid_traverse_initStepY = Point()

  def grid_traverse(cellSize: Float, x1: Float, y1: Float, x2: Float, y2: Float, f: TraverseCallback): Unit = {
    Grid.grid_toCell(cellSize, x1, y1, grid_traverse_c1)
    val cx1 = grid_traverse_c1.x
    val cy1 = grid_traverse_c1.y
    Grid.grid_toCell(cellSize, x2, y2, grid_traverse_c2)
    val cx2   = grid_traverse_c2.x
    val cy2   = grid_traverse_c2.y
    val stepX = Grid.grid_traverse_initStep(cellSize, cx1, x1, x2, grid_traverse_initStepX)
    val stepY = Grid.grid_traverse_initStep(cellSize, cy1, y1, y2, grid_traverse_initStepY)
    val dx    = grid_traverse_initStepX.x
    var tx    = grid_traverse_initStepX.y
    val dy    = grid_traverse_initStepY.x
    var ty    = grid_traverse_initStepY.y
    var cx    = cx1
    var cy    = cy1

    f.onTraverse(cx, cy, stepX, stepY)

    /* The default implementation had an infinite loop problem when
    approaching the last cell in some occasions. We finish iterating
    when we are *next* to the last cell */
    var cont = true // stop iterating if TraverseCallback reports that cell coordinates are outside of the world.
    while (Math.abs(cx - cx2) + Math.abs(cy - cy2) > 1 && cont)
      if (tx < ty) {
        tx = tx + dx
        cx = cx + stepX
        cont = f.onTraverse(cx, cy, stepX, stepY)
      } else {
        // Addition: include both cells when going through corners
        if (tx == ty) {
          f.onTraverse(cx + stepX, cy, stepX, stepY)
        }
        ty = ty + dy
        cy = cy + stepY
        cont = f.onTraverse(cx, cy, stepX, stepY)
      }

    // If we have not arrived to the last cell, use it
    if (cx != cx2 || cy != cy2) {
      f.onTraverse(cx2, cy2, stepX, stepY)
    }
  }

  def grid_traverseRay(cellSize: Float, x1: Float, y1: Float, dirX: Float, dirY: Float, f: TraverseCallback): Unit = {
    Grid.grid_toCell(cellSize, x1, y1, grid_traverse_c1)
    val cx1   = grid_traverse_c1.x
    val cy1   = grid_traverse_c1.y
    val stepX = Grid.grid_traverse_initStep(cellSize, cx1, x1, x1 + dirX, grid_traverse_initStepX)
    val stepY = Grid.grid_traverse_initStep(cellSize, cy1, y1, y1 + dirY, grid_traverse_initStepY)
    val dx    = grid_traverse_initStepX.x
    var tx    = grid_traverse_initStepX.y
    val dy    = grid_traverse_initStepY.x
    var ty    = grid_traverse_initStepY.y
    var cx    = cx1
    var cy    = cy1

    f.onTraverse(cx, cy, stepX, stepY)

    var cont = true // stop iterating if TraverseCallback reports that cell coordinates are outside of the world.
    while (cont)
      if (tx < ty) {
        cx = cx + stepX
        cont = f.onTraverse(cx, cy, stepX, stepY)
        tx = tx + dx
      } else {
        // Addition: include both cells when going through corners
        if (tx == ty) {
          f.onTraverse(cx + stepX, cy, stepX, stepY)
        }
        cy = cy + stepY
        cont = f.onTraverse(cx, cy, stepX, stepY)
        ty = ty + dy
      }
  }

  private val grid_toCellRect_cxy = Point()

  def grid_toCellRect(cellSize: Float, x: Float, y: Float, w: Float, h: Float, rect: Rect): Rect = {
    Grid.grid_toCell(cellSize, x, y, grid_toCellRect_cxy)
    val cx = grid_toCellRect_cxy.x
    val cy = grid_toCellRect_cxy.y

    val cr = Math.ceil((x + w) / cellSize).toFloat
    val cb = Math.ceil((y + h) / cellSize).toFloat

    rect.set(cx, cy, cr - cx + 1, cb - cy + 1)
    rect
  }
}

object Grid {

  def grid_toWorld(cellSize: Float, cx: Float, cy: Float, point: Point): Unit =
    point.set((cx - 1) * cellSize, (cy - 1) * cellSize)

  def grid_toCell(cellSize: Float, x: Float, y: Float, point: Point): Unit =
    point.set(Math.floor(x / cellSize).toFloat + 1, Math.floor(y / cellSize).toFloat + 1)

  def grid_traverse_initStep(cellSize: Float, ct: Float, t1: Float, t2: Float, point: Point): Int = {
    val v = t2 - t1
    if (v > 0) {
      point.set(cellSize / v, ((ct + v) * cellSize - t1) / v)
      1
    } else if (v < 0) {
      point.set(-cellSize / v, ((ct + v - 1) * cellSize - t1) / v)
      -1
    } else {
      point.set(Float.MaxValue, Float.MaxValue)
      0
    }
  }
}
