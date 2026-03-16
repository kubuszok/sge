# SGE Demos

Ten feature demos for the SGE (Scala Game Engine), each exercising different
engine subsystems. Nine demos run on all three platforms (JVM, Scala.js, Scala
Native); Net Chat is JVM-only (XmlReader depends on a SAX parser only available
on JVM). No asset files — everything is procedurally generated.

## Running

```bash
cd demos
sbt --client '<project>/run'          # JVM
sbt --client '<project>JS/fastLinkJS' # Browser (JS)
sbt --client '<project>Native/run'    # Native
```

## Releasing

```bash
cd demos
sbt --client releaseAll               # Build all 9 cross-platform demos + Android APKs
sbt --client releaseNetChat           # Build Net Chat separately (JVM only)
sbt --client collectReleases          # Collect all artifacts into target/releases/
```

`releaseAll` builds JVM distributions (all 6 platforms), browser packages,
native binaries, and Android APKs for all 9 cross-platform demos.

`collectReleases` gathers all artifacts (JVM archives, browser packages,
native binaries, and APKs) from each demo's `target/` into a single
`demos/target/releases/` directory.

## Demos

| Demo | Description | Interactive? | SGE features tested |
|------|-------------|-------------|---------------------|
| **Pong** | Classic Pong with two paddles, bouncing ball, 7-segment score display | **Yes** — W/S or arrow keys move left paddle; touch/click left half to drag paddle. Right paddle is AI | `ShapeRenderer`, `FitViewport`, `MathUtils` (lerp, clamp, cos/sin), `Interpolation`, keyboard + touch input |
| **Space Shooter** | Vertical scrolling shooter with player ship, bullets, enemies, and parallax starfield | **Yes** — arrow keys / A/D to move, Space to fire | `DynamicArray`, `Pool`, `ObjectSet`, `WindowedMean`, `FloatCounter`, `Sort`, `ShapeRenderer`, `FitViewport` |
| **Curve Playground** | Bezier + CatmullRom curves (left), ConvexHull / Delaunay / EarClipping geometry (right), interpolation easing gallery (bottom), Bresenham line (top-right) | **Yes** — drag 4 curve control points (left); drag 8 geometry points (right); scrub T-value across interpolation gallery (bottom); R to randomize geometry | `Bezier`, `CatmullRomSpline`, `ConvexHull`, `DelaunayTriangulator`, `EarClippingTriangulator`, `GeometryUtils`, `Bresenham2`, `Interpolation` (8 easing functions), `ShapeRenderer` |
| **Viewport Gallery** | Same test pattern rendered through 6 viewport types in a 3x2 grid: Screen, Fit, Fill, Stretch, Extend, Scaling | **No** — animated automatically (hue shift + pulsing circle) | `ScreenViewport`, `FitViewport`, `FillViewport`, `StretchViewport`, `ExtendViewport`, `ScalingViewport`, `HdpiUtils`, `ShapeRenderer`, scissor test, split-screen rendering |
| **Shader Lab** | Fullscreen quad with procedural checkerboard texture and 3 switchable GLSL fragment effects (wave distortion, grayscale blend, color inversion) | **Yes** — Tab to cycle effects | `ShaderProgram` (custom GLSL), `Mesh` (manual vertex/index buffers), `Pixmap` (procedural texture), `Texture`, `GLProfiler` (draw call counting), `ScreenViewport` |
| **3D Viewer** | Five procedural 3D shapes (box, sphere, cylinder, cone, capsule) with directional + point lighting, orbiting camera, billboard decals, floor grid | **Yes** — arrow keys orbit camera, Tab toggles auto-rotation | `ModelBuilder`, `ModelBatch`, `ModelInstance`, `PerspectiveCamera`, `Environment`, `DirectionalLight`, `PointLight`, `Material`, `ColorAttribute`, `DecalBatch`, `CameraGroupStrategy`, `ShapeRenderer` (3D lines) |
| **Tile World** | Top-down tile map with 4 terrain types (grass, water, sand, stone), a walking character sprite, scrolling camera | **Yes** — arrow keys / WASD to move character; water tiles block movement | `TiledMap`, `TiledMapTileLayer`, `TiledMapTileSet`, `StaticTiledMapTile`, `OrthogonalTiledMapRenderer`, `SpriteBatch`, `Pixmap` (procedural tileset + sprite), `OrthographicCamera` (smooth follow), `FitViewport`, tile collision |
| **Particle Show** | Manual particle system with 3 emitter modes (explosion, fountain, rain), gravity, fade-out, particle count bar | **Yes** — 1/2/3 keys switch modes; click/touch spawns burst at cursor | `DynamicArray`, `Pool` + `Poolable`, `SpriteBatch`, `Pixmap`, `Texture`, `ShapeRenderer` (HUD bar), `FitViewport` |
| **Hex Tactics** | Hex grid tactics mini-game: 4 terrain types (plains, forest, mountain, water), 2 teams (red/blue) with 2 units each, turn-based movement | **Yes** — click to select unit, click adjacent passable hex to move; Tab advances turn; R regenerates map | `ArrayMap`, `OrderedMap`, `ObjectMap`, `ShapeRenderer` (hex rendering via triangles), `FitViewport`, `Vector2`/`Vector3`, hex coordinate math |
| **Net Chat** | Network/serialization utilities showcase: XML parsing, TextFormatter, TimeUtils, Clipboard — visualized as 4 animated info cards. **JVM only** (XmlReader depends on SAX parser) | **Yes** — Tab cycles XML samples, R re-parses, C copies to clipboard, V checks clipboard | `XmlReader`, `TextFormatter`, `TimeUtils` (millis, nanos), `Clipboard`, `ShapeRenderer`, `ScreenViewport` |

## Architecture

All demos share a common `DemoScene` trait (`shared/`) and `SingleSceneApp`
wrapper that bridges `DemoScene` lifecycle to SGE's `ApplicationListener`.
Platform launchers (JVM main, JS entry, Native main) instantiate
`SingleSceneApp(MyDemoGame)`.

No demos use asset files — all textures, tile maps, models, and sprites are
built procedurally from `Pixmap`, `ModelBuilder`, or `ShapeRenderer` at
runtime.
