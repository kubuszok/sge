# Whole-codebase re-review — 0.1.0 release readiness (2026-07-16)

Eight-agent review sweep (core runtime, 2D graphics, 3D+glTF, UI+text, gameplay
libs/extensions, platform backends, release engineering, open-issue triage)
through the lens of: ship a 0.1.0, then build a first real 2D desktop game.

**Verdict:** the engine is structurally complete and unusually faithful — the
port itself is not the blocker. What blocks 0.1.0 is (a) three
release-engineering gaps that would make a tag today unpublishable or
unusable, (b) a small cluster of first-game-path code bugs concentrated in
text rendering and desktop fullscreen, and (c) two confidence gaps: the
scene2d/font path and the Tiled file-loading path have never been exercised
end-to-end by anything in the repo.

---

## A. Release-engineering blockers (independent of code quality)

1. **Published `sge` POM depends on SNAPSHOT native providers.**
   `project/Versions.scala:45` pins `nativeComponents =
   "0.1.2-33-gcf10406-SNAPSHOT"` (Central snapshots repo only, ephemeral).
   Latest released provider (0.1.2) lacks the Windows Native import-lib/DLL
   fixes. → Cut **sge-native-providers 0.1.3** (also resolves ISS-673) and pin.
2. **`sge-build` is unpublished with no publish path.**
   `docs/getting-started.md:27` and the project template tell users to
   `addSbtPlugin("com.kubuszok" % "sge-build" % ...)` — nothing resolvable
   exists. `sge-build/build.sbt` has no publishTo/Sonatype/PGP wiring and
   `release.yml:168` runs `ci-release` only in the root build. → Add publish
   config + tag-gated publish step.
3. **Javadoc artifacts disabled on every module** (`build.sbt:178-182`,
   `packageDoc / publishArtifact := false`): Central Portal *release*
   validation rejects this (snapshots skip validation — current snapshot
   publishing gives false confidence). → Re-enable scaladoc or publish stub
   javadoc JARs.
4. **Published `sge` JAR contents vary with the release runner's Android SDK.**
   Android classes are compiled only when `hasAndroidSdk`
   (`build.sbt:249-252`) but merged into the JVM JAR via mappings;
   `release.yml`'s publish job (macos-latest) sets up no Android SDK, and no
   gate checks JAR contents. → Inspect a current `sge_3` snapshot for
   `sge/platform/android` classes; provision SDK or add a package-content gate.
5. **Verify kindlings 0.3.1 is fully on Central** (local caches show only
   `kindlings-jsoniter-derivation_3` at 0.3.1; other artifacts/platform rows
   unverified). Two-minute check before tagging.
6. **Doc pins**: getting-started + template hardcode `0.1.0-SNAPSHOT`;
   `README.md:56` says "sbt 1.12+" but the plugin is sbt2-only; CLAUDE.md says
   Scala 3.8.3 vs plugin's 3.8.4; CLAUDE.md claims freetype has a JS axis
   (dropped, ISS-553).

Solid already: tag-driven release pipeline with validation gate, complete POM
metadata, LICENSE/NOTICE/THIRD-PARTY-LICENSES covering all 17 upstream
projects, real getting-started tutorial, demos consume sge exactly as an
external user would.

## B. Code blockers on the first-game path

Text rendering (worst cluster — three independent bugs):

1. **ISS-584 (open)** — `BitmapFontCache.scala:342,366`: 3-arg
   `setText`/`addText` pass `halign = 0` with a false "Align.left = 0" comment
   (`Align.left = 1<<3`); `GlyphLayout.alignRuns` treats bit-unset as
   right-align, so plain `font.draw(batch, str, x, y)` renders text
   right-aligned at x.
2. **NEW** — `Label.scala:156-164,188-191,249-251` and `Skin.scala:227-231`
   assign `font.data.scaleX/scaleY` directly instead of calling
   `BitmapFontData.setScale` (ported intact at `BitmapFont.scala:710-728` but
   never called): lineHeight/capHeight/descent/padding never rescale — any
   scaled Label or skin `scaledSize` font wraps/aligns/sizes wrong.
3. **NEW** — `Button.scala:152-196`: the original's drawable fall-through
   became closed if/else nesting — a checked button without `checkedOver`
   renders `up` instead of `checked` (default-skin common case); `focused`
   fallback also skipped. (`TextButton.scala:76-97` shows the correct cascade
   pattern.)

Desktop backend:

4. **Fullscreen cluster** — `DesktopApplication.scala:394` never consumes
   `config.fullscreenMode` at window creation (silently windowed);
   `DesktopDisplayMode.toDisplayMode` (`DesktopDisplayMode.scala:41`) drops
   `monitorHandle` so `setFullscreenMode` can only target the current monitor;
   `DesktopApplicationConfig.scala:239` pre-launch monitor/display-mode
   queries still "deferred" although the windowing FFI now exists. Three
   compounding gaps; no supported way to start fullscreen.
5. **NEW** — `MiniaudioMusic.scala:85`: `Music.onComplete` never fires on
   desktop (JVM+Native) — `fireOnComplete()` has zero callers; JS and Android
   both wire it. "Next track / back to menu when music ends" hangs on the
   primary platform.
6. **ISS-693 (open)** — desktop `LogPlatform.debug()` throws
   `RuntimeException("Stub!")` when android.jar is on the classpath;
   AssetManager init crashes.
7. **Wayland risk** — `WindowingOpsJvm.scala:381` (+ Native twin): no Wayland
   branch in `getNativeWindowHandle` and no `GLFW_PLATFORM` X11 init hint; if
   the provider GLFW 3.4 build enables Wayland, every Linux-Wayland launch
   dies. One-line preventive hint.
8. **ISS-572 (open, critical)** — `GLFrameBuffer.scala:302` CCE on first FBO
   use; likely fixed by lls 0.3.0 but open-unverified and the harness FBO
   check is still excluded. Re-verify and close (or fix).
9. **Dead config surface** — `DesktopApplicationConfig` fields `glEmulation`,
   `transparentFramebuffer`, `debug`, `debugStream`, `errorStream`,
   `maxNetThreads`, GLES version knobs are never read; window `title` empty
   fallback unported. Fix or delete before the API freezes.

Extensions on the 2D path:

10. **ISS-642 (open, critical)** — textra `Parser.scala` look-behind regex
    breaks TextraField/TypingLabel construction on JS/Native → textra is
    de-facto JVM-only; fix or document as JVM-only for 0.1.0.
11. **JVM gamepad stub** — `GlfwControllerBackend.scala:52` always returns
    `Disconnected` on JVM desktop ("pending Panama downcall wiring") while
    Native and browser work. Fix or document.
12. **Cursor API dead on every platform** — desktop/browser `newCursor`
    return `Nullable.empty` ("deferred"), Android `setCursor` no-ops. Fix or
    release-note.

## C. Confidence gaps (nothing in the repo proves these paths)

- **Zero of 11 demos use Stage, Skin, or BitmapFont** — demo "text" is
  colored rectangles. The scene2d/menu/HUD path has never run in an app here;
  that is exactly how B.2/B.3 survived a "pass" audit. → Add one scene2d menu
  demo (catches B.1–B.3 in one shot).
- **Zero `.tmx`/`.tmj` fixtures in the repo**; the only tiled-loader test
  covers color parsing, and the tile-world demo constructs its map
  programmatically. XML/JSON parse, base64+zlib decode, tileset resolution,
  AssetManager integration for real Tiled files: all unexercised. → One
  end-to-end fixture test.
- **No default font ships** (`BitmapFont()` no-arg ctor / lsans-15 unported,
  undocumented despite full-port covenant on the file).
- Android CI excludes TOUCH_DISPATCH and LIFECYCLE from pass/fail — the two
  subsystems a real game exercises hardest.

## D. Ship-as-experimental for 0.1.0

- **gltf**: ISS-626 (real-world files exceed jsoniter buffer), ISS-625
  (exporter CCE), ISS-605 (unexpanded `#include`), ISS-622 (.pfx codec
  registration), ISS-749 (exporter reflection breaks JS/Native linking), and
  NEW: `SgeNativeProviderPlugin.scala:28-44` resource-embed patterns omit
  `**.glsl/gltf/glb/bin`, so the PBR shader classpath load fails in native
  binaries (core g3d dodged this by embedding GLSL as constants). Core g3d
  itself is complete and safe to ship normally.
- **textra**: ISS-642 + the Font.scala partial-port block (no
  underline/strikethrough/shadow/outline effects — ISS-710/712/713/714/720).
- **AI extension**: `Random` decorator never runs its child
  (`Random.scala:59`, ISS-614), parsed `wait` never elapses (ISS-615).

## E. API inconsistencies to settle before the API freezes

1. **Duplicate Nullable**: `sge-extension/jbump/.../util/Nullable.scala`
   reimplements `lowlevel.Nullable` with a different surface (incl. an
   implicit auto-wrap Conversion). Collapse onto lowlevel before 0.1.0.
2. **Null-idiom trifecta**: `Nullable[A]` (core), raw `null` +
   `compiletime.uninitialized` hiding nullability in non-Nullable fields
   (scene2d/visui style classes, gltf SceneManager, graphs public API,
   `Input.inputProcessor` orNull), and `Option` (parse/AST layers, platform
   ops). State the rule ("Option at parse layer, Nullable at API layer, never
   raw null in public signatures") and sweep the violators
   (`SceneManager.getFirstDirectionalLight`, `DirectedGraph:46`,
   jbump `World.scala:350` null into non-Nullable `CollisionFilter`).
3. **`Align` opaque type unused at API boundaries**: all font/layout params
   are raw `halign: Int` — the untyped seam where ISS-584 slipped in. Typing
   them makes the bug class unrepresentable.
4. **Accessor-style split**: core is propertyized; textra/visui/gltf keep
   `getX()/setX()` (gltf `SceneManager` has no-logic pairs violating the
   stated rule; `VisUI.scala:127-137` exposes both forms).
5. **Unsupported-capability signaling is ad hoc**: UnsupportedOperationException
   vs `SgeError.InvalidInput` vs silent no-op/empty (per platform, per
   feature). Adopt one convention (e.g. `SgeError.Unsupported` + capability
   probes) — silent failure is the current worst case (cursors, browser
   `exit()`, Native `getTextInput` silently "cancels").
6. **Exception-type drift**: raw RuntimeException/ISE/IAE instead of the
   documented `SgeError` mapping across maps loaders, ai PriorityQueue,
   graphs — games have no single error type to catch on asset load.
7. **Stale "deferred"/stub comments that now mislead**: monitor queries
   (FFI exists), WebGL20 pixmap-upload TODO (implemented), `Pixmap.scala:13`
   "stubs" header (implemented), `RemoteInput` postRunnable TODO (wired),
   ShapeRenderer scaladoc still using renamed `rect(...)`. Sweep before tag.
8. **Music trait lacks `duration`** (exists only on MiniaudioMusic) — a
   portable game can't query track length.

## F. Issues-DB hygiene (from the 124-open triage)

- Likely stale, verify-and-close: ISS-608 (VisUI readers all wired now),
  ISS-748 (describes its own fix), ISS-574 (superseded by ISS-739?),
  ISS-583/585/591/599b (post-scalafmt-campaign fixpoint), ISS-575 (folds into
  ISS-572).
- Stale skip-policy entries: `PhysicsOpsJs(.3d)` whitelisted as "stub by
  design" but now full 1424/1512-LOC implementations — the whitelist would
  mask a regression. jbump `World.scala:350` whitelisted as "Java interop"
  but is a genuine API bug (mislabeled).
- Physics (Rapier2D, 2D+3D) verified genuinely complete on all three
  platforms — the old "stub" reputation is obsolete.

## G. Suggested 0.1.0 cut line

**Fix before tag:** A.1–A.6; B.1–B.8 (B.2/B.3 are one root-cause fix + one
method rewrite; B.7 is one init hint); D-flag gltf/textra/ai as experimental;
E.1 (Nullable dedup) because it freezes API; C's two proofs (menu demo,
tiled fixture test).

**Explicitly OK to ship with known issues (release-noted):** cursor API,
JVM gamepads, dead config knobs (if deleted rather than fixed), textra
effects, gltf cluster, Android live-wallpaper, net/http hardening cluster,
remaining fidelity/test-gap backlog.

**First-game feasibility:** after the B+C fixes, a keyboard/mouse 2D desktop
game (sprites, shapes, particles, tiled map, scene2d menu/HUD, music/sfx) is
realistic on JVM desktop with no known blockers; browser follows once
ISS-580 (KTX/FileInputStream JS linking) is addressed.
