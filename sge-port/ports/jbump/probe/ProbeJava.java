import com.dongbat.jbump.*;
import com.dongbat.jbump.util.*;
import java.util.ArrayList;

/** The UPSTREAM half of jbump's differential probe — the same scenario against the ORIGINAL Java, so
  * that `just jbump-measure` can diff the two transcripts line for line.
  *
  * This file is the AUTHORITY: it is compiled against `../sge/original-src/jbump/jbump/src` and its
  * output is what the port's transcript has to equal. Nothing here asserts anything, because an
  * assertion would encode a belief about jbump instead of jbump's behaviour. See `Probe.scala` for
  * why a library with no test suite gets a probe at all.
  *
  * Keep the two files in LOCKSTEP: same scenario, same order, same `println` text. A line added on
  * one side and not the other is reported by the diff, which is the intended failure mode. */
public class ProbeJava {

  static String join(ArrayList<Item> xs) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < xs.size(); i++) { if (i > 0) sb.append(","); sb.append(String.valueOf(xs.get(i).userData)); }
    return sb.toString();
  }

  public static void main(String[] args) {
    // ---- 1 ----
    BooleanArray b = new BooleanArray();
    System.out.println("BooleanArray().items=" + b.items.length + " size=" + b.size + " ordered=" + b.ordered);
    for (int i = 0; i < 40; i++) b.add(i % 3 == 0);
    System.out.println("BooleanArray grown: size=" + b.size + " cap=" + b.items.length + " get(0)=" + b.get(0) + " get(39)=" + b.get(39));
    b.removeIndex(0); b.swap(0, 1);
    System.out.println("BooleanArray after removeIndex+swap: size=" + b.size + " get(0)=" + b.get(0) + " get(1)=" + b.get(1));

    IntArray ia = new IntArray();
    for (int i = 0; i < 40; i++) ia.add(i * 7);
    ia.swap(2, 5);
    System.out.println("IntArray: size=" + ia.size + " cap=" + ia.items.length + " [2]=" + ia.get(2) + " [5]=" + ia.get(5));

    FloatArray fa = new FloatArray(false, 3);
    for (int i = 0; i < 10; i++) fa.add(i / 4f);
    System.out.println("FloatArray: size=" + fa.size + " cap=" + fa.items.length + " [9]=" + fa.get(9));

    // ---- 2 ----
    IntIntMap m = new IntIntMap(4, 0.8f);
    for (int i = 0; i < 200; i++) m.put(i * 3, i);
    System.out.println("IntIntMap: size=" + m.size + " get(0)=" + m.get(0, -1) + " get(597)=" + m.get(597, -1) + " get(4)=" + m.get(4, -1));
    System.out.println("IntIntMap containsKey(300)=" + m.containsKey(300) + " containsValue(100)=" + m.containsValue(100));
    System.out.println("IntIntMap remove(300)=" + m.remove(300, -1) + " -> size=" + m.size + " containsKey=" + m.containsKey(300));
    long sum = 0L;
    IntIntMap.Entries it = m.entries();
    while (it.hasNext()) { IntIntMap.Entry e = it.next(); sum += (long) e.key * 31 + e.value; }
    System.out.println("IntIntMap entry fold=" + sum);
    System.out.println("IntIntMap toString.length=" + m.toString().length() + " head=" + m.toString().substring(0, 1));
    IntIntMap m2 = new IntIntMap(m);
    System.out.println("IntIntMap copy: size=" + m2.size + " equals=" + m2.equals(m) + " hashCode==" + (m2.hashCode() == m.hashCode()));

    // ---- 3 ----
    System.out.println("MathUtils sin(PI/2)=" + MathUtils.sin(MathUtils.PI / 2) + " cos(0)=" + MathUtils.cos(0f));
    System.out.println("MathUtils nextPowerOfTwo(100)=" + MathUtils.nextPowerOfTwo(100) + " clamp=" + MathUtils.clamp(9, 0, 5)
      + " floor(-1.5)=" + MathUtils.floor(-1.5f) + " ceil(1.2)=" + MathUtils.ceil(1.2f) + " round(1.5)=" + MathUtils.round(1.5f));
    MathUtils.random.setSeed(42L);
    System.out.println("MathUtils random(10)=" + MathUtils.random(10) + "," + MathUtils.random(10) + " sign=" + Extra.sign(-3f)
      + " nearest=" + Extra.nearest(1f, 0f, 4f) + " DELTA=" + Extra.DELTA);

    // ---- 4 ----
    Point p1 = new Point(1.5f, -2.5f), p2 = new Point(1.5f, -2.5f);
    System.out.println("Point equals=" + p1.equals(p2) + " sameHash=" + (p1.hashCode() == p2.hashCode()) + " toString=" + p1.toString());
    IntPoint q1 = new IntPoint(3, 4), q2 = new IntPoint(3, 4);
    System.out.println("IntPoint equals=" + q1.equals(q2) + " hash=" + q1.hashCode() + " toString=" + q1.toString());
    Item<String> i1 = new Item<String>("x");
    System.out.println("Item equals-self=" + i1.equals(i1) + " equals-other=" + i1.equals(new Item<String>("x")));

    // ---- 5 ----
    Grid g = new Grid();
    final Point pt = new Point();
    Grid.grid_toCell(64f, 100f, 200f, pt); System.out.println("grid_toCell=" + pt);
    Grid.grid_toWorld(64f, 2f, 4f, pt);    System.out.println("grid_toWorld=" + pt);
    final StringBuilder visits = new StringBuilder();
    g.grid_traverse(64f, 0f, 0f, 200f, 130f, new Grid.TraverseCallback() {
      public boolean onTraverse(float cx, float cy, int sx, int sy) { visits.append("(" + cx + "," + cy + ")"); return true; }
    });
    System.out.println("grid_traverse=" + visits);

    // ---- 6 ----
    Collisions cs = new Collisions();
    Item<String> wall = new Item<String>("wall");
    cs.add(false, 0.9f, 1f, 2f, -1, 0, 3f, 4f, 10f, 11f, 12f, 13f, 20f, 21f, 22f, 23f, wall, wall, Response.slide);
    cs.add(false, 0.1f, 5f, 6f,  0, 1, 7f, 8f, 30f, 31f, 32f, 33f, 40f, 41f, 42f, 43f, wall, wall, Response.touch);
    cs.add(false, 0.5f, 9f, 0f,  1, 0, 1f, 2f, 50f, 51f, 52f, 53f, 60f, 61f, 62f, 63f, wall, wall, Response.cross);
    Collisions copy = new Collisions(cs);
    System.out.println("Collisions copy: size=" + copy.size() + " src=" + cs.size());
    copy.clear();
    System.out.println("Collisions after copy.clear(): copy=" + copy.size() + " src=" + cs.size() + " (independence)");
    cs.sort();
    StringBuilder tis = new StringBuilder(), mvs = new StringBuilder();
    for (int i = 0; i < cs.size(); i++) { if (i > 0) { tis.append(","); mvs.append(","); } tis.append(cs.get(i).ti); mvs.append(cs.get(i).move.x); }
    System.out.println("Collisions sorted tis=" + tis);
    System.out.println("Collisions sorted moveX=" + mvs);
    System.out.println("Collisions compare(0,1)=" + cs.compare(0, 1) + " isEmpty=" + cs.isEmpty());
    cs.remove(0); System.out.println("Collisions after remove(0): size=" + cs.size() + " ti0=" + cs.get(0).ti);

    // ---- 7 ----
    World<String> w = new World<String>(1f);
    Item<String> player = new Item<String>("player");
    w.add(player, 0f, 0f, 1f, 1f);
    for (int x = 2; x < 6; x++) w.add(new Item<String>("wall" + x), (float) x, 0f, 1f, 1f);
    System.out.println("World: items=" + w.countItems() + " cells=" + w.countCells() + " cellSize=" + w.getCellSize() + " tileMode=" + w.isTileMode());
    Response.Result res = w.move(player, 10f, 0f, CollisionFilter.defaultFilter);
    System.out.println("World move -> (" + res.goalX + "," + res.goalY + ") cols=" + res.projectedCollisions.size());
    Rect r0 = w.getRect(player); System.out.println("World player rect=" + r0.x + "," + r0.y + "," + r0.w + "," + r0.h);
    ArrayList<Item> buf = new ArrayList<Item>();
    w.queryRect(0f, 0f, 6f, 1f, CollisionFilter.defaultFilter, buf);
    System.out.println("World queryRect=" + buf.size());
    w.queryPoint(3.5f, 0.5f, CollisionFilter.defaultFilter, buf);
    System.out.println("World queryPoint=" + buf.size() + " -> " + join(buf));
    w.querySegment(0.5f, 0.5f, 5.5f, 0.5f, CollisionFilter.defaultFilter, buf);
    System.out.println("World querySegment=" + buf.size() + " -> " + join(buf));
    ArrayList<ItemInfo> infos = new ArrayList<ItemInfo>();
    w.querySegmentWithCoords(0.5f, 0.5f, 5.5f, 0.5f, CollisionFilter.defaultFilter, infos);
    StringBuilder si = new StringBuilder();
    for (int i = 0; i < infos.size(); i++) { if (i > 0) si.append(","); si.append(String.valueOf(infos.get(i).item.userData)).append("@").append(infos.get(i).x1); }
    System.out.println("World querySegmentWithCoords=" + si);
    w.queryRay(0.5f, 0.5f, 1f, 0f, CollisionFilter.defaultFilter, buf);
    System.out.println("World queryRay=" + buf.size());
    ArrayList<Cell> cells = new ArrayList<Cell>();
    w.getCellsTouchedBySegment(0f, 0f, 5f, 0f, cells);
    System.out.println("World cellsTouchedBySegment=" + cells.size());
    w.update(player, 1.5f, 0f); System.out.println("World after update: rect.x=" + w.getRect(player).x);
    w.remove(player);           System.out.println("World after remove: items=" + w.countItems() + " hasItem=" + w.hasItem(player));
    w.reset();                  System.out.println("World after reset: items=" + w.countItems() + " cells=" + w.countCells());

    // ---- 8 ----
    String[] names = { "slide", "touch", "cross", "bounce" };
    Response[] rs   = { Response.slide, Response.touch, Response.cross, Response.bounce };
    for (int k = 0; k < names.length; k++) {
      final Response r = rs[k];
      World<String> ww = new World<String>(1f);
      Item<String> m1 = new Item<String>("m"); ww.add(m1, 0f, 0f, 1f, 1f);
      Item<String> mo = new Item<String>("o"); ww.add(mo, 3f, 0f, 1f, 1f);
      Response.Result out = ww.move(m1, 5f, 0f, new CollisionFilter() {
        public Response filter(Item i, Item o) { return r; }
      });
      System.out.println("Response." + names[k] + " -> (" + out.goalX + "," + out.goalY + ") cols=" + out.projectedCollisions.size());
    }

    // ---- 9 ----
    Rect rr = new Rect(0f, 0f, 2f, 2f);
    System.out.println("Rect containsPoint=" + Rect.rect_containsPoint(0f, 0f, 2f, 2f, 1f, 1f)
      + " intersecting=" + Rect.rect_isIntersecting(0f, 0f, 2f, 2f, 1f, 1f, 2f, 2f)
      + " sqDist=" + Rect.rect_getSquareDistance(0f, 0f, 2f, 2f, 5f, 5f, 2f, 2f));
    RectHelper rh = new RectHelper();
    Collision col = rh.rect_detectCollision(0f, 0f, 1f, 1f, 0.5f, 0f, 1f, 1f, 0.5f, 0f);
    System.out.println("rect_detectCollision overlaps=" + col.overlaps + " ti=" + col.ti + " touch=" + col.touch + " normal=" + col.normal);
  }
}
