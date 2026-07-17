// SGE — RED reproducer for ISS-781 (Music trait lacks `duration`).
//
// `duration` exists only on the desktop concrete class MiniaudioMusic (MiniaudioMusic.scala:95),
// not on the portable `Music` trait (Music.scala). A portable game holding a `Music` reference
// therefore cannot query track length without an unsafe, platform-specific downcast — the point
// of the trait is defeated. The fix is to promote `duration` onto the `Music` trait (with
// per-platform impls, or a Nullable result where a backend cannot report it).
//
// The RED assertion is expressed reflectively because `(m: Music).duration` cannot even be written
// until the method exists on the trait — a source reference would fail to COMPILE and take the
// whole test module down with it. Reflecting on the trait's interface lets the module compile and
// fail cleanly at runtime until `duration` is promoted.

package sge
package audio

import munit.FunSuite
import sge.files.{ FileHandle, FileType }

class MusicDurationIss781RedSuite extends FunSuite {

  private def newMusic(): Music = {
    val eng = MiniaudioEngine(new WaveDFakeAudioOps)
    val tmp = java.io.File.createTempFile("sge-iss781-", ".ogg")
    tmp.deleteOnExit()
    eng.newMusic(new FileHandle(tmp, FileType.Absolute))
  }

  test("sanity (GREEN): the desktop concrete class MiniaudioMusic already reports duration") {
    newMusic() match {
      case m: MiniaudioMusic => assert(m.duration.toFloatSeconds >= 0f, "concrete impl exposes duration")
      case other => fail(s"expected MiniaudioMusic, got ${other.getClass}")
    }
  }

  test("ISS-781 (RED): the portable Music trait exposes duration") {
    val declaresDuration = classOf[Music].getMethods.exists(_.getName == "duration")
    assert(
      declaresDuration,
      "Music trait must declare `duration` so a portable game can query track length through the " +
        "trait without a platform-specific downcast (currently duration lives only on MiniaudioMusic)"
    )
  }
}
