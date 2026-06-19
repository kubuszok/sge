/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

import sge.utils.Seconds

/** ISS-557 (clause 3): AD-003 claims Screen/ApplicationListener collapse the LibGDX Adapter pattern into a single trait WITH default (no-op) method bodies, so a user only overrides the methods they
  * care about.
  *
  * Reference: docs/improvements/api-design.md:26-37 (AD-003 "single trait with default methods", Status: implemented).
  *
  * But today the trait methods are ABSTRACT:
  *   - sge/src/main/scala/sge/ApplicationListener.scala:36-58 (create/resize/render/ pause/resume/dispose all abstract)
  *   - sge/src/main/scala/sge/Screen.scala:34-55 (show/render/resize/pause/resume/hide/ close all abstract)
  *
  * This suite is RED today because a minimal implementation that overrides ONLY the essential lifecycle method does NOT compile — the remaining methods have no default bodies, violating AD-003.
  */
class AdapterDefaultsIss557RedSuite extends munit.FunSuite {

  test("ISS-557 minimal ApplicationListener overriding only render() compiles via defaults") {
    // Per AD-003, the other lifecycle methods (create/resize/pause/resume/dispose)
    // must have default no-op bodies. If they are abstract this fails to compile.
    val listener: ApplicationListener = new ApplicationListener {
      override def render(): Unit = ()
    }
    listener.create()
    listener.render()
    listener.resize(Pixels(800), Pixels(600))
    listener.pause()
    listener.resume()
    listener.dispose()
    assert(listener != null)
  }

  test("ISS-557 minimal Screen overriding only render() compiles via defaults") {
    // Per AD-003, the other methods (show/resize/pause/resume/hide/close) must have
    // default no-op bodies. If they are abstract this fails to compile.
    val screen: Screen = new Screen {
      override def render(delta: Seconds): Unit = ()
    }
    screen.show()
    screen.render(Seconds(0.016f))
    screen.resize(Pixels(800), Pixels(600))
    screen.pause()
    screen.resume()
    screen.hide()
    screen.close()
    assert(screen != null)
  }
}
