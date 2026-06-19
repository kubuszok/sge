# Getting Started: Your First SGE Game

This guide walks you through building a minimal SGE game from scratch using the
**`Game` / `Screen`** application pattern, and running it on **desktop (JVM)**,
**browser (Scala.js)**, and **Android**.

SGE is a cross-platform Scala 3 port of [LibGDX](https://libgdx.com/). If you
have used LibGDX, most of the API will look familiar — the biggest difference is
that SGE has no global `Gdx.*` statics. Instead, the application context is
passed explicitly as a `(using Sge)` parameter. See
[The `(using Sge)` idiom](#the-using-sge-idiom) below.

> The 11 bundled [demos](../demos/) drive their scenes through a thin
> `DemoScene` launcher abstraction. This guide instead shows the lower-level
> `Game` / `Screen` path directly, so you can see exactly how SGE wires an
> `ApplicationListener` to per-screen lifecycle callbacks.

## 1. Project setup with the SGE sbt plugin

SGE ships an sbt plugin (`sge-build`) that configures the Scala 3 compiler
flags, the JVM/JS/Native cross-build (`projectMatrix`), and packaging — roughly
100 lines of boilerplate you would otherwise write by hand.

### `project/plugins.sbt`

```scala
addSbtPlugin("com.kubuszok" % "sge-build" % "0.1.0-SNAPSHOT")
```

### `build.sbt`

The canonical starting `build.sbt` lives in the plugin resources at
[`sge-build/src/main/resources/sge-template-build.sbt`](../sge-build/src/main/resources/sge-template-build.sbt).
Copy it into your project root:

```scala
// Example build.sbt for an SGE game project.
// With sbt-sge plugin, this replaces ~100 lines of boilerplate.
//
// In project/plugins.sbt, add:
//   addSbtPlugin("com.kubuszok" % "sge-build" % "0.1.0-SNAPSHOT")

import sge.sbt.SgePlugin

lazy val game = (projectMatrix in file("game"))
  .enablePlugins(SgePlugin)
  .settings(
    name := "my-sge-game",
    organization := "com.example",
    // Uncomment to add extensions:
    // sgeExtensions := Set(SgeExtension.Noise, SgeExtension.FreeType),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .jvmPlatform()
  .jsPlatform()
  .nativePlatform()
```

`enablePlugins(SgePlugin)` applies the SGE compiler flags and dependencies.
`.jvmPlatform()`, `.jsPlatform()`, and `.nativePlatform()` add one cross-build
axis each — drop a line if you do not target that platform. (Android is added
separately via the Android build tasks; see
[Android](#android-current-state) below.)

## The `(using Sge)` idiom

In LibGDX, you reach graphics, audio, input, files, and networking through
global statics: `Gdx.graphics`, `Gdx.input`, `Gdx.audio`, `Gdx.files`,
`Gdx.net`. SGE replaces all of these with a single immutable context value,
`Sge`, passed explicitly as a `using` (implicit) parameter:

```scala
final case class Sge private[sge] (
  application: Application,
  graphics:    Graphics,
  audio:       Audio,
  files:       Files,
  input:       Input,
  net:         Net
)
```

(from [`sge/src/main/scala/sge/Sge.scala`](../sge/src/main/scala/sge/Sge.scala))

Wherever LibGDX wrote `Gdx.graphics`, SGE writes `Sge().graphics` (the
`Sge.apply()` summons the in-scope context) — or, if you bind the parameter by
name, `sge.graphics`:

```scala
def render(delta: Seconds)(using sge: Sge): Unit = {
  val input = sge.input
  // ...
}
```

**Propagate the context through constructors.** The convention is to add
`(using Sge)` to your class constructor so every method can summon it:

```scala
class MyGame()(using Sge) extends Game {
  // every method in here can call Sge() or accept (using Sge)
}
```

If you forget to thread the context through, the compiler tells you exactly what
to do. `Sge` carries an `@implicitNotFound` message:

```
No given `Sge` is in scope. `Sge` is this application's context — graphics,
audio, input, files, net — passed explicitly via `(using Sge)` (it replaces
LibGDX's global `Gdx.*`). Add a `(using Sge)` parameter to the enclosing class
constructor or method, propagating the `Sge` your `Game`/`ApplicationListener`
already receives.
```

The `Sge` value is created by the platform launcher and handed to your
`ApplicationListener` factory — you never construct it yourself. The sections
below show exactly where it comes from on each platform.

## 2. The `Game` / `Screen` model

SGE's `Game` is an `ApplicationListener` that delegates every lifecycle callback
to the currently active `Screen`. This lets you split a game into a menu screen,
a play screen, a settings screen, and so on, and switch between them by
assigning to one field.

### `Game`

From [`sge/src/main/scala/sge/Game.scala`](../sge/src/main/scala/sge/Game.scala):

```scala
abstract class Game()(using Sge) extends ApplicationListener {
  private var _screen: Nullable[Screen] = Nullable.empty

  def screen: Nullable[Screen] = _screen

  def screen_=(newScreen: Nullable[Screen]): Unit = {
    _screen.foreach(_.hide())
    _screen = newScreen
    _screen.foreach { s =>
      s.show()
      s.resize(Sge().graphics.width, Sge().graphics.height)
    }
  }

  override def dispose(): Unit = _screen.foreach(_.hide())
  override def pause():   Unit = _screen.foreach(_.pause())
  override def resume():  Unit = _screen.foreach(_.resume())
  override def render():  Unit = _screen.foreach(_.render(Sge().graphics.deltaTime))
  override def resize(width: Pixels, height: Pixels): Unit =
    _screen.foreach(_.resize(width, height))
}
```

Key points:

- The active screen is a `Nullable[Screen]` (SGE's null-safe opaque type from
  `lowlevel.Nullable` — there is no `null` in SGE).
- Assigning `game.screen = myScreen` (the `screen_=` setter) calls `hide()` on
  the **old** screen, then `show()` and `resize(...)` on the **new** one. That
  is the entire transition mechanism.
- `Game.render()` forwards the per-frame delta as
  `Sge().graphics.deltaTime` (a `Seconds`).

### `Screen`

From [`sge/src/main/scala/sge/Screen.scala`](../sge/src/main/scala/sge/Screen.scala):

```scala
trait Screen extends AutoCloseable {
  def show():   Unit = {}
  def render(delta: Seconds): Unit            // the only abstract method
  def resize(width: Pixels, height: Pixels): Unit = {}
  def pause():  Unit = {}
  def resume(): Unit = {}
  def hide():   Unit = {}
  override def close(): Unit = {}
}
```

`render(delta: Seconds)` is the **only** abstract method — `show`, `resize`,
`pause`, `resume`, `hide`, and `close` all have default no-op bodies, so you
override only what you need. (`close()` comes from `AutoCloseable`; SGE renamed
LibGDX's `dispose()` to `close()` on `Screen`.)

The signature types are real SGE types, not `Int`/`Float`:

- `Seconds` is `opaque type Seconds = Float` (from
  [`sge/src/main/scala/sge/utils/Seconds.scala`](../sge/src/main/scala/sge/utils/Seconds.scala)).
  Call `.toFloat` to get a plain float for arithmetic.
- `Pixels` is `opaque type Pixels = Int` (from
  [`sge/src/main/scala/sge/Pixels.scala`](../sge/src/main/scala/sge/Pixels.scala)).
  Construct one with `Pixels(640)`.

### A minimal Game with a menu → play transition

This is a complete, API-accurate example. The menu screen waits for the player
to press <kbd>Space</kbd> (or tap the screen), then assigns a new screen on the
`Game`, which triggers the `hide()`/`show()` handshake described above.

```scala
package com.example.game

import sge.{ Game, Input, Pixels, Screen, Sge }
import sge.utils.Seconds
import sge.utils.ScreenUtils

/** The root application object. The launcher hands us an `Sge` context;
  * we thread it into the screens we create. */
class MyGame()(using Sge) extends Game {
  override def create(): Unit =
    // Set the first screen. The setter calls show()/resize() for us.
    screen = MenuScreen(this)
}

/** Title screen: press Space (or tap) to start. */
class MenuScreen(game: MyGame)(using sge: Sge) extends Screen {

  override def render(delta: Seconds): Unit = {
    // Dark blue background.
    ScreenUtils.clear(0.05f, 0.05f, 0.1f, 1f)

    // isKeyJustPressed / justTouched fire once per press, not every frame.
    if (sge.input.isKeyJustPressed(Input.Keys.SPACE) || sge.input.justTouched()) {
      // Assigning a new screen hides this one and shows the next.
      game.screen = PlayScreen(game)
    }
  }
}

/** The actual game screen. */
class PlayScreen(game: MyGame)(using sge: Sge) extends Screen {

  private var elapsed: Float = 0f

  override def show(): Unit =
    elapsed = 0f

  override def render(delta: Seconds): Unit = {
    elapsed += delta.toFloat

    // Animate the clear colour over time as a stand-in for real rendering.
    val pulse = 0.25f + 0.25f * scala.math.sin(elapsed).toFloat
    ScreenUtils.clear(pulse, 0.1f, 0.2f, 1f)

    // Press Escape to go back to the menu.
    if (sge.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
      game.screen = MenuScreen(game)
    }
  }

  override def resize(width: Pixels, height: Pixels): Unit =
    () // update your viewport here

  override def close(): Unit =
    () // release textures, ShapeRenderers, etc. here
}
```

Notes on the API used above:

- `ScreenUtils.clear(r, g, b, a)` takes `(using Sge)` implicitly — see
  [`sge/src/main/scala/sge/utils/ScreenUtils.scala`](../sge/src/main/scala/sge/utils/ScreenUtils.scala).
- `Input.Keys.SPACE` / `Input.Keys.ESCAPE` are key constants;
  `input.isKeyJustPressed(key)` and `input.justTouched()` return `Boolean`
  (see [`sge/src/main/scala/sge/Input.scala`](../sge/src/main/scala/sge/Input.scala)).
- For real drawing you would typically create a `ShapeRenderer` or
  `SpriteBatch` in `show()`/`create()` and close it in `close()`. See the
  [Pong demo](../demos/pong/src/main/scala/demos/pong/PongGame.scala) for a
  fully worked `ShapeRenderer` example (note: it uses the `DemoScene`
  abstraction, not `Game`/`Screen`, but the rendering calls are identical).

## 3. Running per platform

Each platform has its own entry point. The launcher constructs the `Sge`
context and passes your `Game` to it via a context function of type
`Sge ?=> ApplicationListener` — i.e. "give me an `ApplicationListener` once an
`Sge` is in scope". You write `new MyGame()` and the compiler supplies the
`(using Sge)` from the launcher.

### Desktop (JVM)

On the JVM, `DesktopApplicationFactory.apply` loads GLFW, ANGLE, and miniaudio
via Panama FFM and runs the app, blocking until the window closes (see
[`sge/src/main/scalajvm/sge/DesktopApplicationFactory.scala`](../sge/src/main/scalajvm/sge/DesktopApplicationFactory.scala)).

Put this in a desktop source root (`src/main/scaladesktop/` for shared JVM +
Native code, or `src/main/scalajvm/` for JVM-only):

```scala
package com.example.game

import sge.{ ApplicationListener, DesktopApplicationConfig, DesktopApplicationFactory, Sge }

object DesktopMain {
  def main(args: Array[String]): Unit = {
    val config = DesktopApplicationConfig()
    config.title = "My SGE Game"
    config.windowWidth = 800
    config.windowHeight = 600
    config.foregroundFPS = 60

    // `Sge ?=> ApplicationListener`: MyGame's (using Sge) is filled in by the launcher.
    val app: Sge ?=> ApplicationListener = new MyGame()
    DesktopApplicationFactory(app, config)
  }
}
```

`DesktopApplicationConfig` exposes `title`, `windowWidth`, `windowHeight`
(inherited from `DesktopWindowConfig`) and `foregroundFPS`. Run it with:

```sh
sbt --client 'game/run'
```

Scala Native uses the **same** `scaladesktop` source — the platform-specific
`DesktopApplicationFactory` is resolved at link time. Run the Native binary
with `sbt --client 'gameNative/run'`.

### Browser (Scala.js)

In the browser, `BrowserApplication` creates a WebGL canvas and drives the app
via `requestAnimationFrame`. Put this in `src/main/scalajs/`:

```scala
package com.example.game

import sge.{ ApplicationListener, BrowserApplication, BrowserApplicationConfig, Sge }

object BrowserMain {
  def main(args: Array[String]): Unit = {
    // width/height in CSS pixels; 0 = fill available space.
    val config = new BrowserApplicationConfig(800, 600)
    val app: Sge ?=> ApplicationListener = new MyGame()
    new BrowserApplication(app, config)
  }
}
```

Link it to JavaScript and load `main.js` from an HTML page that calls
`BrowserMain.main`:

```sh
sbt --client 'gameJS/fastLinkJS'   # output under target/js-3/fastLinkJS/main.js
```

(See [`demos/shared/.../BrowserLauncher.scala`](../demos/shared/src/main/scalajs/demos/shared/BrowserLauncher.scala)
for the pattern this mirrors.)

### Android (current state)

Android support exists and the demos ship as signed APKs, **but a reusable
library launcher Activity is not published yet**. Today, each game hand-copies
an Android `Activity` that wires up the GL surface, the renderer bridge, touch
events, and the SGE lifecycle. The reference implementation is
[`demos/shared/.../AndroidLauncher.scala`](../demos/shared/src/main/scala-android/demos/shared/AndroidLauncher.scala)
(`AndroidLauncherActivity`), and a concrete game's Activity is as small as:

```scala
package demos.pong

import demos.shared.AndroidLauncherActivity

class AndroidMain extends AndroidLauncherActivity {
  override def scene = PongGame
}
```

The `AndroidLauncherActivity` base class creates the
`AndroidApplication`, sets up the `GLSurfaceView` and renderer, calls
`app.initializeSge()` and `listener.create()` in the correct order, and
forwards touch events to `AndroidInput`. Until that base class ships in the
library, you would copy `AndroidLauncherActivity` into your own project and
point its `scene`/listener at your `MyGame`.

> Shipping `AndroidLauncherActivity` (plus its renderer bridge) as part of the
> library — so games no longer copy ~160 lines of lifecycle wiring — is tracked
> as open issue **ISS-554**. Treat the Android instructions above as the
> current, demo-driven state rather than a finished library API.

## Where to go next

- [README — Quick Start](../README.md#quick-start): building, testing, and
  running the bundled demos.
- [docs/contributing/setup.md](contributing/setup.md): full toolchain setup.
- The [Pong demo](../demos/pong/): a complete worked game (input, viewport,
  `ShapeRenderer`) running on all four platforms.
