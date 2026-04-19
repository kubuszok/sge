# Port vs original-src audit log

**Purpose:** Living document for human and agent findings when comparing SGE Scala ports to sources under `original-src/`. Append new entries at the end (newest dated section last).
Do not delete prior entries; strikethrough or mark **RESOLVED** if fixed.

**How to append**

- Add a `## YYYY-MM-DD` section (or sub-bullet under an existing open investigation).
- For each file: `path/to/Ported.scala` (or `.java` stub) → cite `original-src/.../Original.java` if applicable.
- Tag: **MISSING** (no port), **STUB**, **SIMPLIFIED**, **SKIPPED**, **WRONG**, **UNVERIFIED** (not yet diffed).
- One or two sentences: what diverges from the original (missing methods, empty bodies, platform branch, etc.).

**Scope note:** Core LibGDX (`com.badlogic.gdx` framework classes such as `Graphics`, `SpriteBatch`, etc.)
are **not** present under `original-src/` in this repository. The `sge/` core module is ported from LibGDX
but cannot be line-compared here without adding upstream sources. This log focuses on what *is* in `original-src/`
plus high-level gaps called out from port comments in `sge/`.

---

## 2026-04-18 — Baseline inventory (automated + spot checks)

### Repository limitation: core LibGDX originals absent

- **UNVERIFIED / external reference:** Entirety of `sge/src/main/scala/sge/**` (core engine) vs standard LibGDX
  — originals not vendored in `original-src/`. Use upstream LibGDX tree for file-by-file parity work.

### `original-src` bundles vs SGE extension modules (expected mapping)

| original-src tree | SGE module | Notes |
|-------------------|------------|--------|
| `gdx-ai/gdx-ai/src` | `sge-extension/ai` | 167 `.java` files under main `src` (incl. `gdx/emu`); port uses `sge.ai` |
| `ashley/ashley/src` | `sge-extension/ecs` | Ashley ECS → `sge.ecs` |
| `anim8-gdx/src/main/java` | `sge-extension/anim8` | |
| `colorful-gdx/colorful/src/main/java` (and related) | `sge-extension/colorful` | Multiple subprojects in upstream |
| `gdx-controllers/*/src/main/java` | `sge-extension/controllers` | JVM/Android/JS/Native splits in SGE |
| `gdx-gltf/gltf/src/main/java` | `sge-extension/gltf` | Large surface; includes demo/ibl in upstream |
| `gdx-vfx/gdx-vfx/**/src` | `sge-extension/vfx` | core + effects modules |
| `jbump/jbump/src` | `sge-extension/jbump` | |
| `libgdx-screenmanager/src/main/java` | `sge-extension/screens` | |
| `noise4j/src/main/java` | `sge-extension/noise` | Package `sge.noise` |
| `simple-graphs/src/main/java` | `sge-extension/graphs` | |
| `textratypist/src/main/java` | `sge-extension/textra` | Ignore `textratypist/versions/**` duplicates for “canonical” unless port tracks a specific release |
| `vis-ui/ui/src/main/java` | `sge-extension/visui` | |

### Core `sge/` — known gaps called out in source (not a full diff)

These are **SIMPLIFIED** or **STUB** markers found via comment scan; each needs a full compare against LibGDX upstream:

- **STUB / deferred JSON:** `sge/src/main/scala/sge/graphics/g3d/particles/Emitter.scala` — `Json.Serializable` not implemented.
- **STUB / deferred JSON:** `sge/src/main/scala/sge/graphics/g3d/particles/ParticleController.scala` — JSON read/write not implemented.
- **STUB / deferred JSON:** `sge/src/main/scala/sge/graphics/g3d/particles/ParticleControllerComponent.scala`
- **STUB / deferred JSON:** `sge/src/main/scala/sge/graphics/g3d/particles/ParallelArray.scala`
- **STUB / deferred JSON:** `sge/src/main/scala/sge/graphics/g3d/particles/emitters/RegularEmitter.scala`
- **FIXME / incomplete:** `sge/src/main/scala/sge/graphics/g3d/shaders/DefaultShader.scala` — throws for some attribute masks; multiple UV-mapping FIXMEs; location caching FIXME.
- **FIXME / incomplete:** `sge/src/main/scala/sge/graphics/g3d/Model.scala` — notes ignored uvScaling/uvTranslation; FIXME on lookup maps.
- **FIXME / incomplete:** `sge/src/main/scala/sge/graphics/g3d/utils/DefaultRenderableSorter.scala` — “implement better sorting algorithm”.
- **FIXME / incomplete:** `sge/src/main/scala/sge/graphics/g3d/decals/SimpleOrthoGroupStrategy.scala` — sort by material not done.
- **FIXME stub (parity with Java):** `DirectionalLightsAttribute.scala`, `SpotLightsAttribute.scala`, `PointLightsAttribute.scala` — `compareTo` uses light count vs full Java semantics (documented in file headers).
- **Platform stub:** `sge/src/main/scala/sge/platform/Gdx2dOps.scala` — JS/Native noted as stub paths in module comments.

### Unported or non-library content under `original-src` (expected)

The following are **SKIPPED** by design for “library port” scope (demos, tests, old versions, docs):

- `**/tests/**`, `**/test/**`, `**/benchmarks/**`, `**/demo/**`, `**/demos/**`, `**/versions/**`, `**/docs/apidocs/**`, Android/HTML launchers, Gradle build files.

Agents: when claiming **MISSING** port, exclude these paths unless the team explicitly wants demo/test parity.

---

## Open: per-extension file-by-file diff

*Agents: continue below with `re-scale enforce compare --port … --source …` results and manual review.*

### Tooling (this checkout)

- **`re-scale enforce shortcuts`** — full scan for heuristic markers (unsupported-op, fixme, stub-comment, etc.):
  **185 hits in 70 files** under `sge/src/main/scala` + `sge-extension/*/src/main/scala`. Raw listing:
  `agents/shortcuts_scan.txt` (regenerate: `re-scale enforce shortcuts --src sge/src/main/scala 'sge-extension/*/src/main/scala'`).
  Spot-check on ai/textra/gltf/vfx/visui only: **4 hits in 3 files** — `agents/shortcuts_extensions_subset.txt` (includes a **FIXME** in `HierarchicalPathFinder.scala`: “the break below is wrong”).
- **`re-scale enforce compare`** — cross-language **member name** diff (Java getters vs Scala `def` names produce false “Missing getX” noise). Treat as a hint only; verify bodies manually. Example run log: `agents/sample_compare_timepiece.txt`.

### Verified divergence examples (manual original read)

- **SIMPLIFIED** — `sge-extension/ai/.../Timepiece.scala` (merged from `Timepiece.java` + `DefaultTimepiece.java`):
  Original `DefaultTimepiece` clamps `deltaTime` with configurable `maxDeltaTime` (constructors `()` and `(float maxDeltaTime)`).
  Port’s `DefaultTimepiece` has no `maxDeltaTime` field and assigns raw `delta` — behavior differs when frames spike (pause / tab switch / debugger).
- **MISSING surface (noise4j)** — `original-src/noise4j/src/.../array/Object2dArray.java` and `Array2D.java` have **no** `sge/noise/**` counterparts by name; only `Int2dArray` is ported. If public API parity with noise4j matters, these types are absent (may be intentional if unused by SGE).
- **RESTRUCTURED (needs method-level audit)** — `simple-graphs` → `sge-extension/graphs`: fewer top-level Scala files; Java types such as `VertexCollection`, `NodeCollection`, `Internals`, `Errors`, `Array` are not present as named types — logic may be folded into `Graph` / `internal/*` or Scala collections.

### Basename inventory snapshots (not method parity)

| Upstream main sources | Count | SGE area | Notes |
|----------------------|-------|----------|--------|
| `original-src/gdx-ai/gdx-ai/src/**/*.java` | 167 | `sge-extension/ai` | Many files merged/renamed;1:1 basename map fails |
| `original-src/ashley/ashley/src/**/*.java` | 21 | `sge-extension/ecs` | 21 Scala files, matching leaf names (still **UNVERIFIED** at method level) |
| `original-src/jbump/jbump/src/**/*.java` | 19 | `sge-extension/jbump` | 15 Scala files; `Extra.java` helpers live in `util/MathUtils.scala`; util `*Array.java` types likely replaced — **UNVERIFIED** |
| `original-src/noise4j/src/**/*.java` | 12 | `sge-extension/noise` | 10 Scala files; missing `Object2dArray`, `Array2D` as named ports (see above) |
| `original-src/simple-graphs/src/main/java/**/*.java` | 29 | `sge-extension/graphs` | 25 Scala files; structural reshuffle (see above) |

### Entire subtrees in `original-src` with no SGE module (library scope)

Treat as **NOT PORTED** unless covered elsewhere under another name:

- **Demos / samples:** `gdx-gltf/demo`, `gdx-vfx/demo`, `colorful-gdx/demos`, `libgdx-screenmanager/src/example`, most `textratypist/versions/**` app shells.
- **Tests / benches:** `**/tests/**`, `**/test/**`, `ashley/benchmarks`, `anim8-gdx/src/test`, etc.
- **Tooling / composer:** `gdx-gltf/ibl-composer` (separate app).
- **Platform launchers:** Android/HTML/LWJGL launcher classes across extensions.
- **Generated docs:** `**/docs/apidocs/**`.

### Core `sge/` parity

**Cannot be checked against `original-src`** in this repo — vendor LibGDX core sources or point compare at a local LibGDX clone.
- **Shortcut-heavy hotspots** (from `shortcuts_scan.txt`): `ParticleEffectCodecs.scala` (many `UnsupportedOperationException` branches), `FileHandles.scala`, `AssetManager.scala`, `Mesh.scala`, `DefaultShader.scala`, `ScrollPane`/`Container`/`SplitPane` (explicit `UnsupportedOperationException` for deprecated widget APIs), `SgeHttpClient.scala` (noop backend), etc. Each entry is a **review queue**, not proof of wrong behavior (some match upstream throws).

---

### Agent checklist for a single ported file

1. Locate original under `original-src/...` (correct module; skip demos/tests unless tasked).
2. If basename differs, read port header comments for “Merged with …”.
3. Run `re-scale enforce compare --port <scala> --source <java>`; ignore false positives from getter/setter naming.
4. Diff bodies for branches, edge cases, and field defaults (see Timepiece example).
5. Append finding here with tag **MISSING** / **STUB** / **SIMPLIFIED** / **WRONG** / **OK**.
