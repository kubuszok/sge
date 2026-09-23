/** The PORT half of jbump's differential probe. Its twin is `ProbeJava.java` beside it, and
  * `just jbump-measure` runs both and diffs the transcripts line for line. */
object Probe:

  def main(args: Array[String]): Unit =
    import sge.jbump.*
    import sge.jbump.util.*

    // ---- 1. the primitive arrays: promoted-nilary replay, growth, removeIndex, swap ----
    val b = new BooleanArray()
    println("BooleanArray().items=" + b.items.length + " size=" + b.size + " ordered=" + b.ordered)
    for i <- 0 until 40 do b.add(i % 3 == 0)
    println("BooleanArray grown: size=" + b.size + " cap=" + b.items.length + " get(0)=" + b.get(0) + " get(39)=" + b.get(39))
    b.removeIndex(0); b.swap(0, 1)
    println("BooleanArray after removeIndex+swap: size=" + b.size + " get(0)=" + b.get(0) + " get(1)=" + b.get(1))

    val ia = new IntArray()
    for i <- 0 until 40 do ia.add(i * 7)
    ia.swap(2, 5)
    println("IntArray: size=" + ia.size + " cap=" + ia.items.length + " [2]=" + ia.get(2) + " [5]=" + ia.get(5))

    val fa = new FloatArray(false, 3)
    for i <- 0 until 10 do fa.add(i / 4f)
    println("FloatArray: size=" + fa.size + " cap=" + fa.items.length + " [9]=" + fa.get(9))

    // ---- 2. IntIntMap: size++ as a value, break/continue, resize, iteration ----
    val m = new IntIntMap(4, 0.8f)
    for i <- 0 until 200 do m.put(i * 3, i)
    println("IntIntMap: size=" + m.size + " get(0)=" + m.get(0, -1) + " get(597)=" + m.get(597, -1) + " get(4)=" + m.get(4, -1))
    println("IntIntMap containsKey(300)=" + m.containsKey(300) + " containsValue(100)=" + m.containsValue(100))
    println("IntIntMap remove(300)=" + m.remove(300, -1) + " -> size=" + m.size + " containsKey=" + m.containsKey(300))
    var sum = 0L
    val it = m.entries()
    while it.hasNext() do { val e = it.next(); sum += e.key.toLong * 31 + e.value }
    println("IntIntMap entry fold=" + sum)
    println("IntIntMap toString.length=" + m.toString.length + " head=" + m.toString.take(1))
    val m2 = new IntIntMap(m)
    println("IntIntMap copy: size=" + m2.size + " equals=" + m2.equals(m) + " hashCode==" + (m2.hashCode == m.hashCode))

    // ---- 3. MathUtils: the sine table's static initialiser, and the clash-renamed statics ----
    println("MathUtils sin(PI/2)=" + MathUtils.sin(MathUtils.PI / 2) + " cos(0)=" + MathUtils.cos(0f))
    println("MathUtils nextPowerOfTwo(100)=" + MathUtils.nextPowerOfTwo(100) + " clamp=" + MathUtils.clamp(9, 0, 5) +
      " floor(-1.5)=" + MathUtils.floor(-1.5f) + " ceil(1.2)=" + MathUtils.ceil(1.2f) + " round(1.5)=" + MathUtils.round(1.5f))
    MathUtils.random$field.setSeed(42L)
    println("MathUtils random(10)=" + MathUtils.random(10) + "," + MathUtils.random(10) + " sign=" + Extra.sign(-3f) +
      " nearest=" + Extra.nearest(1f, 0f, 4f) + " DELTA=" + Extra.DELTA)

    // ---- 4. Point / IntPoint: equals + hashCode, which World uses as HashMap KEYS ----
    val p1 = new Point(1.5f, -2.5f); val p2 = new Point(1.5f, -2.5f)
    println("Point equals=" + p1.equals(p2) + " sameHash=" + (p1.hashCode == p2.hashCode) + " toString=" + p1.toString)
    val q1 = new IntPoint(3, 4); val q2 = new IntPoint(3, 4)
    println("IntPoint equals=" + q1.equals(q2) + " hash=" + q1.hashCode + " toString=" + q1.toString)
    val i1 = new Item[String]("x")
    println("Item equals-self=" + i1.equals(i1) + " equals-other=" + i1.equals(new Item[String]("x")))

    // ---- 5. Grid traversal ----
    val g = new Grid()
    val pt = new Point()
    Grid.grid_toCell(64f, 100f, 200f, pt); println("grid_toCell=" + pt)
    Grid.grid_toWorld(64f, 2f, 4f, pt);    println("grid_toWorld=" + pt)
    val visits = new StringBuilder
    g.grid_traverse(64f, 0f, 0f, 200f, 130f, new Grid.TraverseCallback {
      def onTraverse(cx: Float, cy: Float, sx: Int, sy: Int): Boolean = { visits.append("(" + cx + "," + cy + ")"); true }
    })
    println("grid_traverse=" + visits)

    // ---- 6. Collisions: the copy constructor, sort(), and the Comparator the hand port dropped ----
    val cs = new Collisions()
    val wall = new Item[String]("wall")
    cs.add(false, 0.9f, 1f, 2f, -1, 0, 3f, 4f, 10f, 11f, 12f, 13f, 20f, 21f, 22f, 23f, wall, wall, Response.slide)
    cs.add(false, 0.1f, 5f, 6f,  0, 1, 7f, 8f, 30f, 31f, 32f, 33f, 40f, 41f, 42f, 43f, wall, wall, Response.touch)
    cs.add(false, 0.5f, 9f, 0f,  1, 0, 1f, 2f, 50f, 51f, 52f, 53f, 60f, 61f, 62f, 63f, wall, wall, Response.cross)
    val copy = new Collisions(cs)
    println("Collisions copy: size=" + copy.size() + " src=" + cs.size())
    copy.clear()
    println("Collisions after copy.clear(): copy=" + copy.size() + " src=" + cs.size() + " (independence)")
    cs.sort()
    println("Collisions sorted tis=" + (0 until cs.size()).map(i => cs.get(i).ti).mkString(","))
    println("Collisions sorted moveX=" + (0 until cs.size()).map(i => cs.get(i).move.x).mkString(","))
    println("Collisions compare(0,1)=" + cs.compare(0, 1) + " isEmpty=" + cs.isEmpty())
    cs.remove(0); println("Collisions after remove(0): size=" + cs.size() + " ti0=" + cs.get(0).ti)

    // ---- 7. World: the whole point of the library ----
    val w = new World[String](1f)
    val player = new Item[String]("player")
    w.add(player, 0f, 0f, 1f, 1f)
    for x <- 2 until 6 do w.add(new Item[String]("wall" + x), x.toFloat, 0f, 1f, 1f)
    println("World: items=" + w.countItems() + " cells=" + w.countCells() + " cellSize=" + w.getCellSize() + " tileMode=" + w.isTileMode())
    val res = w.move(player, 10f, 0f, CollisionFilter.defaultFilter)
    println("World move -> (" + res.goalX + "," + res.goalY + ") cols=" + res.projectedCollisions.size())
    val r0 = w.getRect(player); println("World player rect=" + r0.x + "," + r0.y + "," + r0.w + "," + r0.h)
    // queryRect / queryPoint take the port's retyped list — build it as the port declares it
    val buf = scala.collection.mutable.ArrayBuffer.empty[Item[?]]
    w.queryRect(0f, 0f, 6f, 1f, CollisionFilter.defaultFilter, buf)
    println("World queryRect=" + buf.size)
    w.queryPoint(3.5f, 0.5f, CollisionFilter.defaultFilter, buf)
    println("World queryPoint=" + buf.size + " -> " + buf.map(x => String.valueOf(x.userData)).mkString(","))
    w.querySegment(0.5f, 0.5f, 5.5f, 0.5f, CollisionFilter.defaultFilter, buf)
    println("World querySegment=" + buf.size + " -> " + buf.map(x => String.valueOf(x.userData)).mkString(","))
    val infos = scala.collection.mutable.ArrayBuffer.empty[ItemInfo]
    w.querySegmentWithCoords(0.5f, 0.5f, 5.5f, 0.5f, CollisionFilter.defaultFilter, infos)
    println("World querySegmentWithCoords=" + infos.map(i => String.valueOf(i.item.userData) + "@" + i.x1).mkString(","))
    w.queryRay(0.5f, 0.5f, 1f, 0f, CollisionFilter.defaultFilter, buf)
    println("World queryRay=" + buf.size)
    val cells = scala.collection.mutable.ArrayBuffer.empty[Cell]
    w.getCellsTouchedBySegment(0f, 0f, 5f, 0f, cells)
    println("World cellsTouchedBySegment=" + cells.size)
    w.update(player, 1.5f, 0f); println("World after update: rect.x=" + w.getRect(player).x)
    w.remove(player);           println("World after remove: items=" + w.countItems() + " hasItem=" + w.hasItem(player))
    w.reset();                  println("World after reset: items=" + w.countItems() + " cells=" + w.countCells())

    // ---- 8. Response.bounce / cross / touch through the interface CONSTANTS ----
    val w2 = new World[String](1f)
    val a = new Item[String]("a"); w2.add(a, 0f, 0f, 1f, 1f)
    val bItem = new Item[String]("b"); w2.add(bItem, 3f, 0f, 1f, 1f)
    for (name, r) <- List(("slide", Response.slide), ("touch", Response.touch), ("cross", Response.cross), ("bounce", Response.bounce)) do
      val ww = new World[String](1f)
      val m1 = new Item[String]("m"); ww.add(m1, 0f, 0f, 1f, 1f)
      val m2 = new Item[String]("o"); ww.add(m2, 3f, 0f, 1f, 1f)
      val out = ww.move(m1, 5f, 0f, new CollisionFilter { def filter(i: Item[?], o: Item[?]): Response = r })
      println("Response." + name + " -> (" + out.goalX + "," + out.goalY + ") cols=" + out.projectedCollisions.size())

    // ---- 9. RectHelper / Rect statics ----
    val rr = new Rect(0f, 0f, 2f, 2f)
    println("Rect containsPoint=" + Rect.rect_containsPoint(0f, 0f, 2f, 2f, 1f, 1f) +
      " intersecting=" + Rect.rect_isIntersecting(0f, 0f, 2f, 2f, 1f, 1f, 2f, 2f) +
      " sqDist=" + Rect.rect_getSquareDistance(0f, 0f, 2f, 2f, 5f, 5f, 2f, 2f))
    val rh = new RectHelper()
    val col = rh.rect_detectCollision(0f, 0f, 1f, 1f, 0.5f, 0f, 1f, 1f, 0.5f, 0f)
    println("rect_detectCollision overlaps=" + col.overlaps + " ti=" + col.ti + " touch=" + col.touch + " normal=" + col.normal)
