/* Copyright 2025-2026 Mateusz Kubuszok / Licensed under Apache 2.0 */
package sge
package utils

import java.util.concurrent.{ ConcurrentLinkedQueue, CyclicBarrier }

/** ISS-803 red: PoolManager's `typePools` was a bare `scala.collection.mutable.Map` mutated/read without synchronization, while the surrounding `Pool` was made internally thread-safe
  * (ISS-603/ISS-797). Concurrent `addPool`/`obtain` on distinct types races the shared HashMap: crossing the load-factor resize threshold while several threads mutate the table can lose entries or
  * throw (NPE / index errors) from inside HashMap internals.
  *
  * The suite starts N threads on a common barrier, each registering AND obtaining a DISTINCT type into one shared PoolManager. N is chosen to cross the default HashMap resize threshold (load factor
  * 0.75 of capacity 16 → ~12 entries) so a resize happens mid-mutation. A corrupted map surfaces as either a thrown exception (collected) or a lost registration (`hasPool` false afterwards). With the
  * monitor guard both are impossible.
  */
class PoolManagerConcurrencyIss803Suite extends munit.FunSuite {

  // 20 distinct marker classes → 20 distinct Class keys, enough concurrent inserts to force two HashMap resizes
  // (thresholds at ~12 and ~24 for capacities 16→32) while every thread is still mutating the shared table.
  final private class C00
  final private class C01
  final private class C02
  final private class C03
  final private class C04
  final private class C05
  final private class C06
  final private class C07
  final private class C08
  final private class C09
  final private class C10
  final private class C11
  final private class C12
  final private class C13
  final private class C14
  final private class C15
  final private class C16
  final private class C17
  final private class C18
  final private class C19

  // Each entry registers a distinct type and immediately obtains from it, stressing both map mutation and reads.
  private val registrars: Vector[(PoolManager => Unit, Class[?])] = Vector(
    ((pm: PoolManager) => { pm.addPool[C00](() => new C00()); pm.obtain[C00]; () }, classOf[C00]),
    ((pm: PoolManager) => { pm.addPool[C01](() => new C01()); pm.obtain[C01]; () }, classOf[C01]),
    ((pm: PoolManager) => { pm.addPool[C02](() => new C02()); pm.obtain[C02]; () }, classOf[C02]),
    ((pm: PoolManager) => { pm.addPool[C03](() => new C03()); pm.obtain[C03]; () }, classOf[C03]),
    ((pm: PoolManager) => { pm.addPool[C04](() => new C04()); pm.obtain[C04]; () }, classOf[C04]),
    ((pm: PoolManager) => { pm.addPool[C05](() => new C05()); pm.obtain[C05]; () }, classOf[C05]),
    ((pm: PoolManager) => { pm.addPool[C06](() => new C06()); pm.obtain[C06]; () }, classOf[C06]),
    ((pm: PoolManager) => { pm.addPool[C07](() => new C07()); pm.obtain[C07]; () }, classOf[C07]),
    ((pm: PoolManager) => { pm.addPool[C08](() => new C08()); pm.obtain[C08]; () }, classOf[C08]),
    ((pm: PoolManager) => { pm.addPool[C09](() => new C09()); pm.obtain[C09]; () }, classOf[C09]),
    ((pm: PoolManager) => { pm.addPool[C10](() => new C10()); pm.obtain[C10]; () }, classOf[C10]),
    ((pm: PoolManager) => { pm.addPool[C11](() => new C11()); pm.obtain[C11]; () }, classOf[C11]),
    ((pm: PoolManager) => { pm.addPool[C12](() => new C12()); pm.obtain[C12]; () }, classOf[C12]),
    ((pm: PoolManager) => { pm.addPool[C13](() => new C13()); pm.obtain[C13]; () }, classOf[C13]),
    ((pm: PoolManager) => { pm.addPool[C14](() => new C14()); pm.obtain[C14]; () }, classOf[C14]),
    ((pm: PoolManager) => { pm.addPool[C15](() => new C15()); pm.obtain[C15]; () }, classOf[C15]),
    ((pm: PoolManager) => { pm.addPool[C16](() => new C16()); pm.obtain[C16]; () }, classOf[C16]),
    ((pm: PoolManager) => { pm.addPool[C17](() => new C17()); pm.obtain[C17]; () }, classOf[C17]),
    ((pm: PoolManager) => { pm.addPool[C18](() => new C18()); pm.obtain[C18]; () }, classOf[C18]),
    ((pm: PoolManager) => { pm.addPool[C19](() => new C19()); pm.obtain[C19]; () }, classOf[C19])
  )

  test("ISS-803: concurrent addPool/obtain on distinct types does not corrupt the typePools map") {
    val iterations = 400
    var iter       = 0
    while (iter < iterations) {
      val pm      = PoolManager()
      val barrier = new CyclicBarrier(registrars.size)
      val errors  = new ConcurrentLinkedQueue[Throwable]()
      val threads = registrars.map { case (register, _) =>
        new Thread(new Runnable {
          def run(): Unit =
            try {
              barrier.await()
              register(pm)
            } catch {
              case e: Throwable =>
                errors.add(e)
                ()
            }
        })
      }
      threads.foreach(_.start())
      threads.foreach(_.join())

      assert(
        errors.isEmpty,
        s"iteration $iter: concurrent addPool/obtain threw ${errors.size} exception(s); first: ${Option(errors.peek()).map(_.toString).getOrElse("<none>")}"
      )
      registrars.foreach { case (_, clazz) =>
        assert(pm.hasPool(clazz), s"iteration $iter: registration for $clazz was lost — map corrupted by concurrent mutation")
      }
      iter += 1
    }
  }
}
