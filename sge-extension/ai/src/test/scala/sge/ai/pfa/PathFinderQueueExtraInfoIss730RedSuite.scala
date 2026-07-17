package sge
package ai
package pfa

import sge.ai.DefaultTimepiece
import sge.ai.msg.Telegram
import sge.ai.msg.Telegraph
import lowlevel.Nullable

/** Red suite for ISS-730 clause c4 (wave 2026-07-17-F, territory Z).
  *
  * `PathFinderQueue.handleMessage` must dereference the telegram's request payload
  * UNCONDITIONALLY, matching the original, rather than guarding it inside a
  * `Nullable.foreach` that silently swallows a payload-less telegram.
  *
  * Original contract:
  *   - com/badlogic/gdx/ai/pfa/PathFinderQueue.java:71-81 —
  *     {{{
  *     public boolean handleMessage (Telegram telegram) {
  *       PathFinderRequest<N> pfr = (PathFinderRequest<N>)telegram.extraInfo;
  *       pfr.client = telegram.sender;      // :75, unconditional
  *       pfr.status = SEARCH_NEW;
  *       pfr.statusChanged = true;
  *       pfr.executionFrames = 0;
  *       requestQueue.store(pfr);
  *       return true;
  *     }
  *     }}}
  *     The request is read straight from `telegram.extraInfo`; a telegram that
  *     carries no request is a protocol error and the original fails fast (NPE on
  *     the `pfr.client = ...` dereference).
  *
  * The port (PathFinderQueue.scala:85-95) wraps the whole body in
  * `msg.extraInfo.foreach { info => ... }`, so a telegram with an empty
  * `extraInfo` is SILENTLY dropped: `handleMessage` returns `true` and nothing is
  * enqueued, hiding the malformed message instead of surfacing it. The faithful
  * mapping dereferences the payload unconditionally (e.g. `msg.extraInfo.get`),
  * preserving the original's fail-fast on a payload-less telegram.
  *
  * NOTE (do not weaken existing suites): the "stale directed client on re-enqueue"
  * behavior that clause c4 references is already covered and GREEN via
  * `PathFinderBroadcastDispatchRedSuite` (ISS-729). This suite pins only the
  * residual `extraInfo`-guard divergence.
  *
  * These assertions encode the ORIGINAL semantics and MUST NOT be weakened.
  */
class PathFinderQueueExtraInfoIss730RedSuite extends munit.FunSuite {

  private val ResponseCode = 7304

  /** A path finder that is never actually run: `handleMessage` only touches the
    * request payload and the internal queue, never the finder.
    */
  private final class UnusedPathFinder extends PathFinder[String] {
    override def searchConnectionPath(startNode: String, endNode: String, heuristic: Heuristic[String], outPath: GraphPath[Connection[String]]): Boolean =
      throw new IllegalStateException("path finder must not be invoked by handleMessage")
    override def searchNodePath(startNode: String, endNode: String, heuristic: Heuristic[String], outPath: GraphPath[String]): Boolean =
      throw new IllegalStateException("path finder must not be invoked by handleMessage")
    override def search(request: PathFinderRequest[String], timeToRun: Long): Boolean =
      throw new IllegalStateException("path finder must not be invoked by handleMessage")
  }

  private final class RecordingClient extends Telegraph {
    override def handleMessage(msg: Telegram): Boolean = true
  }

  private def newQueue(): PathFinderQueue[String] =
    new PathFinderQueue[String](new UnusedPathFinder(), new DefaultTimepiece())

  private def telegram(sender: Nullable[Telegraph], extraInfo: Nullable[Any]): Telegram = {
    val t = new Telegram()
    t.sender = sender
    t.message = ResponseCode
    t.extraInfo = extraInfo
    t
  }

  test("ISS-730 c4 green control: a well-formed request telegram is enqueued") {
    val queue   = newQueue()
    val request = new PathFinderRequest[String]()
    queue.handleMessage(telegram(sender = Nullable(new RecordingClient()), extraInfo = Nullable(request)))
    assertEquals(queue.size, 1, "a telegram carrying a PathFinderRequest must be stored, matching PathFinderQueue.java:79")
  }

  test("ISS-730 c4: a payload-less telegram (empty extraInfo) must NOT be silently swallowed") {
    val queue = newQueue()
    // PathFinderQueue.java:74-75 dereferences telegram.extraInfo unconditionally,
    // so a payload-less telegram fails fast (NPE). The port's `msg.extraInfo.foreach`
    // guard swallows it: handleMessage returns true and nothing is enqueued.
    intercept[NullPointerException] {
      queue.handleMessage(telegram(sender = Nullable(new RecordingClient()), extraInfo = Nullable.empty))
    }
  }
}
