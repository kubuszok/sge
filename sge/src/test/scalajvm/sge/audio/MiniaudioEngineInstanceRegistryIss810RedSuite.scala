// SGE — RED reproducer for ISS-810 (MiniaudioEngine soundInstances/musicInstances are mutated across
// threads without synchronization).
//
// MiniaudioEngine tracks live instances in two BARE scala.collection.mutable.ArrayBuffers
// (MiniaudioEngine.scala:54-55): `musicInstances` and `soundInstances`. They are mutated from more
// than one thread by design — a loader thread creates sounds/music (newSound += soundInstances,
// MiniaudioEngine.scala:86; newMusic += musicInstances, :117) while the render/update thread iterates
// musicInstances (update(), :143-147) and instance close() removes from them (forgetSound/forgetMusic,
// :200-204). ArrayBuffer is not thread-safe: its addOne does `array(size0) = elem; size0 += 1` with no
// atomicity, so two racing appends can overwrite the same slot (losing an element), leave a written
// slot unaccounted, or throw ArrayIndexOutOfBoundsException while the backing array is being grown.
//
// This is the same class of defect the Pool free-list (ISS-603) and PoolManager map (ISS-803) were
// fixed for, and the prescribed remedy is the same: guard every mutation/read of these collections
// (dedicated lock / synchronized), per the Pool-threading precedent.
//
// A wall-clock timing assertion would be flaky, so this suite uses the ISS-803 bounded-iteration
// barrier-stress shape (PoolManagerConcurrencyIss803Suite): each iteration releases N loader threads
// on a common CyclicBarrier so their appends race, then checks the registry's INTEGRITY — every
// instance that newSound/newMusic reported as created (returned without throwing, so its `+=` "ran")
// must still be present, observed here as the number of native dispose calls the engine issues when it
// tears the registry down in close() (MiniaudioEngine.scala:150-160, which snapshots the buffer and
// closes each survivor). A corrupted, unsynchronized buffer loses survivors (dispose count < created)
// or throws during the append/teardown; a guarded buffer keeps count == created with no exceptions.
//
// It FAILS today (bare ArrayBuffers). Guarding the collections with a lock flips it green. Written by
// the reproducer agent; MUST NOT be modified by the fixer.

package sge
package audio

import munit.FunSuite
import sge.files.{ FileHandle, FileType }
import sge.platform.AudioOps

import java.util.concurrent.{ ConcurrentLinkedQueue, CyclicBarrier }
import java.util.concurrent.atomic.{ AtomicInteger, AtomicLong }
import scala.concurrent.duration.{ Duration, DurationInt }

class MiniaudioEngineInstanceRegistryIss810RedSuite extends FunSuite {

  override def munitTimeout: Duration = 120.seconds

  /** Headless fake miniaudio FFI: hands out a UNIQUE nonzero handle for every sound/music created (so the engine takes its real, non-`noDevice` code paths) and counts the native dispose calls the
    * engine issues when it tears the registry down. No native library involved.
    */
  final private class CountingAudioOps extends AudioOps {
    private val nextHandle = new AtomicLong(1L)
    val disposeSoundCount  = new AtomicInteger(0)
    val disposeMusicCount  = new AtomicInteger(0)
    private def fresh(): Long = nextHandle.getAndIncrement()

    override def initEngine(simultaneousSources: Int, bufferSize: Int, bufferCount: Int): Long = 1L
    override def shutdownEngine(engineHandle:    Long):                                   Unit = ()
    override def updateEngine(engineHandle:      Long):                                   Unit = ()

    override def createSound(engineHandle:       Long, pcmData: Array[Byte], channels: Int, bitDepth: Int, sampleRate: Int):     Long = fresh()
    override def disposeSound(soundHandle:       Long):                                                                          Unit = { disposeSoundCount.incrementAndGet(); () }
    override def playSound(soundHandle:          Long, volume:  Float, pitch:          Float, pan:    Float, loop:     Boolean): Long = 1L
    override def stopSound(instanceId:           Long):                                                                          Unit = ()
    override def pauseSound(instanceId:          Long):                                                                          Unit = ()
    override def resumeSound(instanceId:         Long):                                                                          Unit = ()
    override def stopAllInstances(soundHandle:   Long):                                                                          Unit = ()
    override def pauseAllInstances(soundHandle:  Long):                                                                          Unit = ()
    override def resumeAllInstances(soundHandle: Long):                                                                          Unit = ()
    override def setSoundVolume(instanceId:      Long, volume:  Float):                                                          Unit = ()
    override def setSoundPitch(instanceId:       Long, pitch:   Float):                                                          Unit = ()
    override def setSoundPan(instanceId:         Long, pan:     Float, volume:         Float):                                   Unit = ()
    override def setSoundLooping(instanceId:     Long, looping: Boolean):                                                        Unit = ()

    override def createMusic(engineHandle:     Long, filePath: String):               Long    = fresh()
    override def disposeMusic(musicHandle:     Long):                                 Unit    = { disposeMusicCount.incrementAndGet(); () }
    override def playMusic(musicHandle:        Long):                                 Unit    = ()
    override def pauseMusic(musicHandle:       Long):                                 Unit    = ()
    override def stopMusic(musicHandle:        Long):                                 Unit    = ()
    override def isMusicPlaying(musicHandle:   Long):                                 Boolean = false
    override def getMusicVolume(musicHandle:   Long):                                 Float   = 1f
    override def setMusicVolume(musicHandle:   Long, volume:   Float):                Unit    = ()
    override def setMusicPitch(musicHandle:    Long, pitch:    Float):                Unit    = ()
    override def setMusicPan(musicHandle:      Long, pan:      Float, volume: Float): Unit    = ()
    override def isMusicLooping(musicHandle:   Long):                                 Boolean = false
    override def setMusicLooping(musicHandle:  Long, looping:  Boolean):              Unit    = ()
    override def setMusicPosition(musicHandle: Long, position: Float):                Unit    = ()
    override def getMusicPosition(musicHandle: Long):                                 Float   = 0f
    override def getMusicDuration(musicHandle: Long):                                 Float   = 1f

    override def createAudioDevice(engineHandle:     Long, sampleRate: Int, isMono:         Boolean):          Long = 9L
    override def disposeAudioDevice(deviceHandle:    Long):                                                    Unit = ()
    override def writeAudioDevice(deviceHandle:      Long, data:       Array[Byte], offset: Int, length: Int): Unit = ()
    override def setAudioDeviceVolume(deviceHandle:  Long, volume:     Float):                                 Unit = ()
    override def pauseAudioDevice(deviceHandle:      Long):                                                    Unit = ()
    override def resumeAudioDevice(deviceHandle:     Long):                                                    Unit = ()
    override def getAudioDeviceLatency(deviceHandle: Long):                                                    Int  = 0

    override def getAvailableOutputDevices(engineHandle: Long):                     Array[String] = Array.empty[String]
    override def switchOutputDevice(engineHandle:        Long, deviceName: String): Boolean       = true
  }

  /** A tiny NON-RIFF file: MiniaudioEngine.newSound reads its bytes and, since it is not a WAV, takes the trivial format-fallback path (no WavInputStream) — keeping per-append work minimal so the
    * threads reach `soundInstances += ...` as close together as possible.
    */
  private def nonWavFile(): FileHandle = {
    val tmp = java.io.File.createTempFile("sge-iss810-", ".bin")
    tmp.deleteOnExit()
    val out = new java.io.FileOutputStream(tmp)
    try out.write(Array.fill[Byte](12)(0))
    finally out.close()
    new FileHandle(tmp, FileType.Absolute)
  }

  /** An existing file usable as a music path: newMusic resolves via file.file (exists → absolute path, no classpath extraction) and hands it to the fake createMusic.
    */
  private def musicFile(): FileHandle = {
    val tmp = java.io.File.createTempFile("sge-iss810-", ".ogg")
    tmp.deleteOnExit()
    new FileHandle(tmp, FileType.Absolute)
  }

  private def describe(t: Throwable): String = s"${t.getClass.getName}: ${t.getMessage}"

  test("ISS-810 (RED): concurrent newSound appends must not corrupt the soundInstances registry") {
    val loaders    = 16
    val iterations = 300
    var iter       = 0
    while (iter < iterations) {
      val ops     = new CountingAudioOps
      val engine  = MiniaudioEngine(ops)
      val file    = nonWavFile()
      val barrier = new CyclicBarrier(loaders)
      val created = new AtomicInteger(0)
      val errors  = new ConcurrentLinkedQueue[Throwable]()

      val threads = (0 until loaders).map { _ =>
        new Thread(() =>
          try {
            barrier.await()
            engine.newSound(file) // returns non-null: its `soundInstances += sound` has run
            created.incrementAndGet()
            ()
          } catch { case t: Throwable => errors.add(t); () }
        )
      }
      threads.foreach(_.start())
      threads.foreach(_.join(30000))

      // Tear the registry down: close() snapshots soundInstances and closes each survivor, so the
      // dispose count == number of instances actually present in the buffer.
      try engine.close()
      catch { case t: Throwable => errors.add(t) }

      val disposed = ops.disposeSoundCount.get()
      val firstErr = Option(errors.peek()).map(describe).getOrElse("none")
      assert(
        errors.isEmpty && disposed == created.get(),
        s"iteration $iter: unsynchronized soundInstances corrupted by $loaders concurrent newSound appends — " +
          s"created=${created.get()} but engine disposed only $disposed on teardown, ${errors.size} exception(s) " +
          s"(first: $firstErr). Bare ArrayBuffer at MiniaudioEngine.scala:55; append at :86 — guard it (ISS-810)."
      )
      iter += 1
    }
  }

  test("ISS-810 (RED): concurrent newMusic appends must not corrupt the musicInstances registry") {
    val loaders    = 16
    val iterations = 300
    var iter       = 0
    while (iter < iterations) {
      val ops     = new CountingAudioOps
      val engine  = MiniaudioEngine(ops)
      val file    = musicFile()
      val barrier = new CyclicBarrier(loaders)
      val created = new AtomicInteger(0)
      val errors  = new ConcurrentLinkedQueue[Throwable]()

      val threads = (0 until loaders).map { _ =>
        new Thread(() =>
          try {
            barrier.await()
            engine.newMusic(file) // returns non-null: its `musicInstances += music` has run
            created.incrementAndGet()
            ()
          } catch { case t: Throwable => errors.add(t); () }
        )
      }
      threads.foreach(_.start())
      threads.foreach(_.join(30000))

      try engine.close()
      catch { case t: Throwable => errors.add(t) }

      val disposed = ops.disposeMusicCount.get()
      val firstErr = Option(errors.peek()).map(describe).getOrElse("none")
      assert(
        errors.isEmpty && disposed == created.get(),
        s"iteration $iter: unsynchronized musicInstances corrupted by $loaders concurrent newMusic appends — " +
          s"created=${created.get()} but engine disposed only $disposed on teardown, ${errors.size} exception(s) " +
          s"(first: $firstErr). Bare ArrayBuffer at MiniaudioEngine.scala:54; append at :117 — guard it (ISS-810)."
      )
      iter += 1
    }
  }
}
