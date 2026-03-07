/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

class DesktopInterfacesTest extends munit.FunSuite {

  // ---- DesktopAudio ----

  test("DesktopAudio extends Audio and AutoCloseable") {
    // Verify type hierarchy at compile time
    val _: Audio         = null.asInstanceOf[DesktopAudio]
    val _: AutoCloseable = null.asInstanceOf[DesktopAudio]
  }

  test("DesktopAudio can be implemented with update and close") {
    var updated = false
    var closed  = false
    val desktopAudio: DesktopAudio = new DesktopAudio {
      override def update(): Unit = updated = true
      override def close():  Unit = closed = true

      // Audio trait stubs
      override def newAudioDevice(samplingRate:         Int, isMono: Boolean):   _root_.sge.audio.AudioDevice   = ???
      override def newAudioRecorder(samplingRate:       Int, isMono: Boolean):   _root_.sge.audio.AudioRecorder = ???
      override def newSound(fileHandle:                 files.FileHandle):       _root_.sge.audio.Sound         = ???
      override def newMusic(file:                       files.FileHandle):       _root_.sge.audio.Music         = ???
      override def switchOutputDevice(deviceIdentifier: utils.Nullable[String]): Boolean                        = false
      override def getAvailableOutputDevices:                                    Array[String]                  = Array.empty
    }

    desktopAudio.update()
    assert(updated)
    desktopAudio.close()
    assert(closed)
  }

  // ---- DesktopInput ----

  test("DesktopInput extends Input and AutoCloseable") {
    val _: Input         = null.asInstanceOf[DesktopInput]
    val _: AutoCloseable = null.asInstanceOf[DesktopInput]
  }

  // ---- DesktopApplicationBase ----

  test("DesktopApplicationBase extends Application") {
    val _: Application = null.asInstanceOf[DesktopApplicationBase]
  }

  test("DesktopApplicationBase declares createAudio and createInput") {
    // Compile-time check — the trait has these abstract methods
    val base = null.asInstanceOf[DesktopApplicationBase]
    val _: DesktopApplicationConfig => DesktopAudio = base.createAudio
    val _: DesktopWindow => DesktopInput            = base.createInput
  }
}
