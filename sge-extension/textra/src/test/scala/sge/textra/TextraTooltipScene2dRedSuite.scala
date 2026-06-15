/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Red tests for ISS-670 (milestone 4/4, the LAST, of the ISS-524
 * textra<->scene2d integration): TextraTooltip and TypingTooltip must be real
 * scene2d Tooltips (which ARE InputListeners) managing a real Container, not
 * standalone classes wrapping a hand-rolled ContainerProxy.
 *
 * Original semantics (TextraTypist, upstream commit
 * 3fe5c930acc9d66cb0ab1a29751e44591c18e2c4 — the commit the textra headers
 * reference; original-src/textratypist/src/main/java/com/github/tommyettinger/
 * textra/{TextraTooltip,TypingTooltip}.java):
 *
 *   - TextraTooltip.java:33 `public class TextraTooltip extends Tooltip<TextraLabel>`
 *     — i.e. com.badlogic.gdx.scenes.scene2d.ui.Tooltip, which extends
 *     com.badlogic.gdx.scenes.scene2d.InputListener. So a TextraTooltip IS a
 *     Tooltip<TextraLabel> and IS an InputListener: it carries the inherited
 *     `manager` (TooltipManager) and `container` (Container<TextraLabel>), and
 *     can be registered on an actor as a real listener so it shows on hover.
 *   - TypingTooltip.java:35 `public class TypingTooltip extends Tooltip<TypingLabel>`
 *     — likewise a Tooltip<TypingLabel> and an InputListener.
 *   - TextraTooltip.java:78-84 the canonical ctor calls `super(null, manager)`,
 *     then `setActor(newLabel(...))` and
 *     `getContainer().width(style.wrapWidth).background(style.background)` — i.e.
 *     the TextraLabel is installed into the REAL Container the Tooltip
 *     superclass manages, and getContainer() returns that REAL Container, whose
 *     getActor() is the TextraLabel.
 *   - Tooltip.java:64-69 `setActor(T)`/`getActor()` delegate to
 *     `container.setActor(...)`/`container.getActor()` — so the actor lives
 *     inside the real Container.
 *
 * The port at the red commit declares both as standalone classes with NO
 * scene2d base (TextraTooltip.scala:31 `class TextraTooltip(...)(using Sge) {`,
 * TypingTooltip.scala:36 `class TypingTooltip(...) extends TextraTooltip(...)`),
 * hand-rolls the label as a private field, and returns a bespoke
 * `TextraTooltip.ContainerProxy` from getContainer (TextraTooltip.scala:75/110)
 * INSTEAD of the real Container<TextraLabel> the Tooltip superclass would hold.
 * There is no inherited `manager` and the tooltip is not an InputListener, so it
 * cannot be added to an actor as a hover listener.
 *
 * What these tests pin (each grounded in real scene2d types; trivially-green
 * stubs are avoided):
 *   1. is-a Tooltip / InputListener (compile-level): a TextraTooltip value is
 *      usable where a sge.scenes.scene2d.ui.Tooltip[TextraLabel] and a
 *      sge.scenes.scene2d.InputListener are required; likewise TypingTooltip as
 *      a Tooltip[TypingLabel]. This does NOT compile at the red commit
 *      (standalone classes) — a compile-failure RED, the strongest possible
 *      proof the type relationship is absent.
 *   2. real Container: getContainer returns a real
 *      sge.scenes.scene2d.ui.Container[TextraLabel] (NOT a ContainerProxy), and
 *      that container's actor (Container.actor / upstream getActor()) is the
 *      tooltip's TextraLabel.
 *   3. real TooltipManager + real listener: the tooltip is bound to a real
 *      sge.scenes.scene2d.ui.TooltipManager (the inherited `manager`), and being
 *      an InputListener it can be added to an actor and appears in that actor's
 *      listeners.
 *   4. getActor: tooltip.getActor returns the contained TextraLabel.
 *
 * Harness: a headless Sge (NoopGraphics/NoopAudio/NoopInput, no GL) is enough
 * to construct the tooltips and their TextraLabel/Container/TooltipManager. The
 * tooltip tests exercise Actor/Container/TooltipManager directly and never
 * build or render a Stage. This suite lives under the `sge` package so any
 * `private[sge]` scene2d members it needs are visible.
 *
 * These tests are written by the reproducer agent and MUST NOT be modified by
 * the fixer: they encode the original TextraTypist semantics (TextraTooltip IS
 * a scene2d Tooltip with a real Container + TooltipManager), not the port's
 * standalone shape.
 */
package sge
package textra

import lowlevel.Nullable

import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }
import sge.files.{ FileHandle, FileType }
import sge.scenes.scene2d.{ Actor, InputListener }
import sge.scenes.scene2d.ui.{ Container, Tooltip, TooltipManager }

class TextraTooltipScene2dRedSuite extends munit.FunSuite {

  // --- Headless Sge fixture (NoopGraphics gives width/height with no GL) ---

  private object StubApplication extends Application {
    def applicationListener:                                  ApplicationListener         = throw new UnsupportedOperationException
    def graphics:                                             Graphics                    = throw new UnsupportedOperationException
    def audio:                                                Audio                       = throw new UnsupportedOperationException
    def input:                                                Input                       = throw new UnsupportedOperationException
    def files:                                                Files                       = throw new UnsupportedOperationException
    def net:                                                  Net                         = throw new UnsupportedOperationException
    def applicationType:                                      Application.ApplicationType = Application.ApplicationType.HeadlessDesktop
    def version:                                              Int                         = 0
    def javaHeap:                                             Long                        = 0L
    def nativeHeap:                                           Long                        = 0L
    def getPreferences(name:              String):            Preferences                 = throw new UnsupportedOperationException
    def clipboard:                                            sge.utils.Clipboard         = throw new UnsupportedOperationException
    def postRunnable(runnable:            Runnable):          Unit                        = ()
    def exit():                                               Unit                        = ()
    def addLifecycleListener(listener:    LifecycleListener): Unit                        = ()
    def removeLifecycleListener(listener: LifecycleListener): Unit                        = ()
  }

  private object NoNet extends Net {
    import Net.*
    def httpClient:                                                                                         net.SgeHttpClient = net.SgeHttpClient.noop()
    def newServerSocket(protocol: Protocol, hostname: String, port: Int, hints: sge.net.ServerSocketHints): net.ServerSocket  = throw new UnsupportedOperationException
    def newServerSocket(protocol: Protocol, port:     Int, hints:   sge.net.ServerSocketHints):             net.ServerSocket  = throw new UnsupportedOperationException
    def newClientSocket(protocol: Protocol, host:     String, port: Int, hints: sge.net.SocketHints):       net.Socket        = throw new UnsupportedOperationException
    def openURI(URI:              String):                                                                  Boolean           = false
  }

  private object NoFiles extends Files {
    def getFileHandle(path: String, fileType: FileType): FileHandle = throw new UnsupportedOperationException
    def classpath(path:     String):                     FileHandle = throw new UnsupportedOperationException
    def internal(path:      String):                     FileHandle = throw new UnsupportedOperationException
    def external(path:      String):                     FileHandle = throw new UnsupportedOperationException
    def absolute(path:      String):                     FileHandle = throw new UnsupportedOperationException
    def local(path:         String):                     FileHandle = throw new UnsupportedOperationException
    def externalStoragePath:                             String     = ""
    def isExternalStorageAvailable:                      Boolean    = false
    def localStoragePath:                                String     = ""
    def isLocalStorageAvailable:                         Boolean    = false
  }

  private def headlessSge(): Sge =
    Sge(StubApplication, new NoopGraphics(), new NoopAudio(), NoFiles, new NoopInput(), NoNet)

  /** A minimal textra TextTooltipStyle with a label font, sufficient to construct a TextraTooltip/TypingTooltip. */
  private def newStyle()(using Sge): Styles.TextTooltipStyle = {
    val style      = new Styles.TextTooltipStyle()
    val labelStyle = new Styles.LabelStyle()
    labelStyle.font = Nullable(new Font())
    style.label = Nullable(labelStyle)
    style.wrapWidth = 100f
    style
  }

  // --- 1. is-a Tooltip / InputListener (compile-level) ---------------------

  test(
    "ISS-670: a TextraTooltip is a scene2d.ui.Tooltip[TextraLabel] and an InputListener (TextraTooltip.java:33 `extends Tooltip<TextraLabel>`)"
  ) {
    given Sge   = headlessSge()
    val tooltip = new TextraTooltip(Nullable("hi"), newStyle())

    // These widenings only compile if TextraTooltip extends Tooltip[TextraLabel]
    // (which extends InputListener). At the red commit TextraTooltip is a
    // standalone class, so this suite does not compile — a compile-failure RED.
    val asTooltip:  Tooltip[TextraLabel] = tooltip
    val asListener: InputListener        = tooltip
    assert(asTooltip eq tooltip, "a TextraTooltip value must BE a Tooltip[TextraLabel], not wrap one")
    assert(asListener eq tooltip, "a TextraTooltip value must BE an InputListener (Tooltip extends InputListener), not wrap one")
  }

  test(
    "ISS-670: a TypingTooltip is a scene2d.ui.Tooltip[TypingLabel] and an InputListener (TypingTooltip.java:35 `extends Tooltip<TypingLabel>`)"
  ) {
    given Sge   = headlessSge()
    val tooltip = new TypingTooltip(Nullable("hi"), newStyle())

    val asTooltip:  Tooltip[TypingLabel] = tooltip
    val asListener: InputListener        = tooltip
    assert(asTooltip eq tooltip, "a TypingTooltip value must BE a Tooltip[TypingLabel], not wrap one")
    assert(asListener eq tooltip, "a TypingTooltip value must BE an InputListener, not wrap one")
  }

  // --- 2. real Container ---------------------------------------------------

  test(
    "ISS-670: getContainer returns a real Container[TextraLabel] (NOT a ContainerProxy) holding the tooltip's TextraLabel (TextraTooltip.java:78-84)"
  ) {
    given Sge   = headlessSge()
    val tooltip = new TextraTooltip(Nullable("contained"), newStyle())

    // At the red commit getContainer returns TextraTooltip.ContainerProxy, so
    // this typed binding does not compile (and, were it to, ContainerProxy has
    // no `actor` accessor and is not a Container).
    // The `: Container[TextraLabel]` ascription compile-proves it is a real
    // Container (not a ContainerProxy); assert its actor is the tooltip's label.
    val container: Container[TextraLabel] = tooltip.getContainer
    assert(
      container.actor.exists(_ eq tooltip.getActor),
      "the Container's actor (upstream getContainer().getActor()) must be the tooltip's TextraLabel"
    )
  }

  // --- 3. real TooltipManager + real listener ------------------------------

  test(
    "ISS-670: a TextraTooltip is bound to a real TooltipManager (the inherited `manager`, TextraTooltip.java:78 super(null, manager))"
  ) {
    given Sge   = headlessSge()
    val tooltip = new TextraTooltip(Nullable("mgr"), newStyle())

    // At the red commit there is no inherited `manager` field (standalone class).
    val manager: TooltipManager = tooltip.manager
    assert(
      manager eq TooltipManager.instance,
      "the tooltip's manager is the per-Sge TooltipManager singleton (Tooltip secondary ctor uses TooltipManager.instance)"
    )
  }

  test(
    "ISS-670: a TextraTooltip can be added to an actor as a real listener (it IS an InputListener) and appears in that actor's listeners"
  ) {
    given Sge   = headlessSge()
    val tooltip = new TextraTooltip(Nullable("hover"), newStyle())
    val target  = new Actor()

    assert(!target.listeners.exists(_ eq tooltip), "before wiring the tooltip is not a listener of the target actor")
    // addListener accepts an EventListener; a TextraTooltip is only acceptable
    // here if it IS an InputListener (Tooltip extends InputListener).
    target.addListener(tooltip)
    assert(
      target.listeners.exists(_ eq tooltip),
      "once added the tooltip appears in the target actor's listeners (shows on hover, real InputListener)"
    )
  }

  // --- 4. getActor ---------------------------------------------------------

  test("ISS-670: getActor returns the contained TextraLabel (TextraTooltip.java:80-81 setActor(newLabel(...)); getActor())") {
    given Sge   = headlessSge()
    val tooltip = new TextraTooltip(Nullable("label"), newStyle())

    // The `: TextraLabel` ascription compile-proves getActor returns the label.
    val label: TextraLabel = tooltip.getActor
    assert(
      tooltip.getContainer.actor.exists(_ eq label),
      "getActor must be exactly the actor held inside the real Container (no duplicate hand-rolled label)"
    )
  }
}
