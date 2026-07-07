/*
 * SGE — ISS-733 red reproducer.
 *
 * Pins the ORIGINAL public logging API of AssetManager that the port dropped:
 *   original-src/libgdx .../assets/AssetManager.java
 *     :87   Logger log = new Logger("AssetManager", Application.LOG_NONE);
 *     :747  public Logger getLogger () { return log; }
 *     :751  public void setLogger (Logger logger) { log = logger; }
 *
 * The port (sge/assets/AssetManager.scala ~:77-82) replaced these with a
 * `private object log` that forwards UNCONDITIONALLY to `utils.Log.*`, so:
 *   (a) there is NO public accessor to read the AssetManager's Logger,
 *   (b) there is NO public setter to inject/silence a Logger, and
 *   (c) the default is NOT LOG_NONE — the original AssetManager is SILENT by
 *       default (Application.LOG_NONE == 0), the port logs at utils.Log's level.
 *
 * These assertions MUST FAIL on current code (accessors absent / not silent
 * by default). Reflection-based so the suite compiles even though the
 * public accessor + Logger type do not yet exist — the failure is a clean
 * runtime assertion pointing at the missing public API.
 */
package sge
package assets

import munit.FunSuite

import sge.assets.loaders.FileHandleResolver
import sge.files.{ FileHandle, FileType }

class AssetManagerLoggerIss733Suite extends FunSuite {

  private class StubResolver extends FileHandleResolver {
    override def resolve(fileName: String): FileHandle =
      FileHandle(new java.io.File(fileName), FileType.Absolute)
  }

  private def makeManager(): AssetManager = {
    given Sge = SgeTestFixture.testSge()
    AssetManager(StubResolver(), defaultLoaders = false)
  }

  /** Public accessor: Java `getLogger` OR Scala property `logger`. */
  private def accessorMethod: Option[java.lang.reflect.Method] =
    classOf[AssetManager].getMethods
      .find(m => (m.getName == "getLogger" || m.getName == "logger") && m.getParameterCount == 0 && m.getReturnType != java.lang.Void.TYPE)

  /** Public setter: Java `setLogger(x)` OR Scala property setter `logger_=`. */
  private def setterMethod: Option[java.lang.reflect.Method] =
    classOf[AssetManager].getMethods
      .find(m => (m.getName == "setLogger" || m.getName == "logger_$eq") && m.getParameterCount == 1)

  // (a) AssetManager.java:747 — `public Logger getLogger ()`
  test("ISS-733: AssetManager exposes a public logger/getLogger accessor (AssetManager.java:747)") {
    assert(
      accessorMethod.isDefined,
      "AssetManager has no public `logger`/`getLogger` accessor — the port's `private object log` (AssetManager.scala ~:77) " +
        "dropped `public Logger getLogger()` (AssetManager.java:747). Callers can no longer read the AssetManager's Logger."
    )
  }

  // (b) AssetManager.java:751 — `public void setLogger (Logger logger)`
  test("ISS-733: AssetManager exposes a public setLogger/logger_= setter (AssetManager.java:751)") {
    assert(
      setterMethod.isDefined,
      "AssetManager has no public `setLogger`/`logger_=` setter — the port dropped `public void setLogger(Logger)` " +
        "(AssetManager.java:751). Callers can no longer inject a Logger nor silence the AssetManager."
    )
  }

  // (c) AssetManager.java:87 — `new Logger("AssetManager", Application.LOG_NONE)` => silent (level == 0) by default.
  test("ISS-733: AssetManager's default Logger level is LOG_NONE / silent (AssetManager.java:87)") {
    val acc = accessorMethod.getOrElse(
      fail(
        "cannot verify the LOG_NONE default: no public `logger`/`getLogger` accessor exists. " +
          "Original AssetManager.java:87 defaults to `new Logger(\"AssetManager\", Application.LOG_NONE)` (level 0 == silent); " +
          "the port forwards unconditionally to utils.Log with no per-instance level and no way to silence it."
      )
    )
    val manager = makeManager()
    try {
      val logger = acc.invoke(manager)
      assert(logger != null, "getLogger returned null; expected a Logger defaulting to LOG_NONE (AssetManager.java:87).")
      // Level accessor on the restored Logger: `getLevel` (Java) or `level` (Scala property).
      val levelMethod = logger.getClass.getMethods
        .find(m => (m.getName == "getLevel" || m.getName == "level") && m.getParameterCount == 0)
        .getOrElse(fail("restored Logger exposes no level/getLevel accessor to verify the LOG_NONE default (AssetManager.java:87)."))
      val level = levelMethod.invoke(logger).asInstanceOf[Int]
      assertEquals(
        level,
        0,
        "default AssetManager Logger level must be LOG_NONE (0) so the manager is SILENT by default (AssetManager.java:87); " +
          s"got $level."
      )
    } finally
      manager.close()
  }
}
