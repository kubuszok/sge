# Dependency Replacements

Tracking document for potential library replacements in SGE. Each entry evaluates
a hand-rolled implementation against a mature cross-platform Scala library.

## Current Dependencies

| Library | Version | Purpose | Cross-Platform |
|---------|---------|---------|---------------|
| jsoniter-scala-core | 2.38.9 | JSON parsing | JVM/JS/Native |
| kindlings-jsoniter-json | snapshot | JSON tree model | JVM/JS/Native |
| scala-xml | 2.3.0 | XML parsing | JVM/JS/Native |
| munit | 1.2.3 | Testing | JVM/JS/Native |

---

## B1: TimeUtils — delegate to `scala-java-time`

**Current:** `sge.utils.TimeUtils` (62 LOC) wraps `System.nanoTime()`/`currentTimeMillis()`.

**Library:** `io.github.cquiroz %%% "scala-java-time" % "2.6.0"`

**Platform Support:**
- JVM: delegates to `java.time` (built-in)
- JS: pure Scala.js implementation of `java.time`
- Native: pure Scala Native implementation of `java.time`
- Android: native `java.time` from API 26+, desugaring for older

**What it provides:**
- `java.time.Duration`, `Instant`, `Clock`
- Proper timezone handling via `java.time.ZoneId`
- ISO-8601 parsing/formatting
- Thread-safe, immutable value types

**Approach:** Keep `TimeUtils` as a facade with opaque types (`Millis`, `Nanos`)
that delegate to `java.time.Instant.now()` / `Duration.between()` internally.
No API change for consumers — just type safety + proper time library underneath.

**Assessment:** Recommended. Low risk, high value, well-maintained library.

**Status:** TODO — annotated in TimeUtils.scala

---

## B2: Logger — consider `scribe`

**Current:** `sge.utils.Logger` (63 LOC) delegates to `Application.log/debug/error`.

**Library:** `com.outr %%% "scribe" % "3.17.0"`

**Platform Support:**
- JVM: full-featured (file output, log rotation, etc.)
- JS: console output
- Native: console output

**What it provides:**
- Zero external dependencies
- Compile-time optimization (macro-based)
- Structured logging, MDC context
- Pluggable output handlers
- Log level filtering

**Trade-off:** Logger is intentionally a thin facade over Application's logging.
Replacing with scribe would **decouple logging from the Application lifecycle**.
This is both a benefit (logging works before Application init) and a cost
(loses the LibGDX pattern where Application controls log output).

**Assessment:** Evaluate — the user should decide if decoupling logging from
Application is wanted. If yes, scribe is the best cross-platform option.

**Status:** TODO — annotated in Logger.scala for evaluation

---

## B3: Timer — redesign with Gears

**Current:** `sge.utils.Timer` (341 LOC) uses raw Java `Thread` + `synchronized`.

**Library:** `ch.epfl.lamp %%% "gears" % "0.2.0-RC3"`

**Platform Support:**
- JVM: virtual threads (JDK 21+ required)
- JS: JSPI (JavaScript Promise Integration) — experimental
- Native: supported

**What it provides:**
- Structured concurrency (`Async` context)
- `.await` on futures/channels
- `Channel[T]` for inter-task communication
- `Timer.sleep(duration)` for delays
- Automatic cancellation via structured scopes

**Why needed:** Timer's current `Thread` + `synchronized` model:
- Won't compile on Scala.js (no `java.lang.Thread`)
- Won't work on Scala Native without JVM thread emulation
- Uses blocking `wait()`/`notifyAll()` patterns

**Risk:** Gears is 0.2.0-RC3 (experimental). API may change before 1.0.

**Note on WASM:** Scala.js 1.20.2 has experimental Wasm backend but it does NOT
enable blocking/Await — it's a performance optimization only. Gears with JSPI
is the correct JS async approach.

**Approach:** Redesign Timer as a Gears-based scheduler. Public API
(`scheduleTask`, `delay`, `interval`) preserved; implementation switches
from threads to async.

**Assessment:** Necessary for JS/Native targets. Gears is the only cross-platform
structured concurrency library for Scala 3. Accept RC status risk.

**Status:** TODO — annotated in Timer.scala

---

## B4: Compression — REMOVED

**Former:** ~1000 LOC in `sge.utils.compression` (full LZMA SDK port, 13 files).

**Removed** (2026-03-03): Dead code — never called by LibGDX or SGE. No built-in
LZMA on any target platform (JVM/JS/Native/Android). Multiple porting bugs
(constructor body not called, infinite loops). Originally for `.g3db` binary
format support which was already deferred.

**If needed later:** Use platform-adapter pattern (like `BufferOps`):
- JVM: Apache Commons Compress
- JS: lzma-js via facade
- Native: Rust binding via C ABI

**Status:** Done — package deleted, 13 files reclassified as `skipped`.

---

## B5: JsonValue → jsoniter-scala derivation

**Current:** 6 consumer files manually parse JSON via `JsonValue` linked-list tree
navigation — constructing a `JsonValue` tree from a file, then walking it with
`.get("key")`, `.child`, `.next`, `.asFloat()`, `.asFloatArray()`, etc. to populate
typed data classes. This is the same pattern Java LibGDX uses (`JsonValue` was its
only JSON abstraction), but Scala has a much better option available.

**Library:** `com.github.plokhotnyuk.jsoniter-scala %%% "jsoniter-scala-macros"`
(already a transitive dependency via kindlings-jsoniter-json)

**What it provides:**
- Compile-time codec derivation for `case class` / `enum` / sealed trait hierarchies
- Zero-allocation streaming decode — no intermediate tree
- Cross-platform: JVM/JS/Native (already proven in SGE's `JsonReader`)

**The pattern to eliminate:**

```scala
// CURRENT: manual JsonValue tree walking (~200 LOC in G3dModelLoader alone)
val meshes = json.get("meshes")
meshes.foreach { mesh =>
  val id = mesh.getString("id")
  val verts = mesh.get("vertices").asFloatArray()
  // ... 20 more fields, nested loops for parts/materials/nodes/animations
}

// TARGET: define data classes, derive codecs, decode directly
final case class G3dModel(meshes: Array[G3dMesh], materials: Array[G3dMaterial], ...)
given JsonValueCodec[G3dModel] = JsonCodecMaker.make
val model = readFromStream(fileHandle.read())(using codec)
```

**Files to migrate (6 consumers):**

| File | What it parses | Complexity |
|------|---------------|------------|
| `graphics/g3d/loader/G3dModelLoader.scala` | `.g3dj` 3D models (meshes, materials, nodes, animations) | High (~300 LOC of JSON walking) |
| `maps/tiled/BaseTmjMapLoader.scala` | `.tmj` Tiled maps (layers, objects, tilesets, properties) | High (~500 LOC of JSON walking) |
| `maps/tiled/BaseTiledMapLoader.scala` | `.tiled-project` files (class property definitions) | Medium (~100 LOC) |
| `maps/tiled/TmjMapLoader.scala` | Delegates to BaseTmjMapLoader; stores root `JsonValue` | Low (indirect) |
| `maps/tiled/AtlasTmjMapLoader.scala` | Delegates to BaseTmjMapLoader; stores root `JsonValue` | Low (indirect) |
| `scenes/scene2d/ui/Skin.scala` | `.skin` UI theme files (styles, fonts, colors, drawables) | High (~200 LOC, uses reflection) |

**Infrastructure to retire after migration:**

| File | Role | Keep/Remove |
|------|------|-------------|
| `utils/JsonValue.scala` | Mutable linked-list JSON tree | Remove (no consumers) |
| `utils/JsonReader.scala` | Parses JSON into `JsonValue` tree | Remove (replaced by direct codec decode) |
| `utils/BaseJsonReader.scala` | Trait returning `JsonValue` | Remove (no consumers) |

**Benefits:**
- **~800 LOC eliminated** across 6 consumer files (manual tree walking → derived codecs)
- **~600 LOC eliminated** in infrastructure (JsonValue + JsonReader + BaseJsonReader)
- **Type safety**: malformed JSON caught at decode time with clear error messages
- **Performance**: streaming decode, no intermediate tree allocation
- **Skin.scala**: eliminates Java reflection dependency (the main JS/Native blocker)
- **Compile-time verification**: codec derivation catches missing/mistyped fields

**Approach:**
1. Define `case class` hierarchies matching each JSON format (G3dModel, TmjMap, SkinData)
2. Derive `JsonValueCodec[T]` for each via `JsonCodecMaker.make`
3. Replace `reader.parse(handle)` + manual walking with `readFromStream(handle.read())`
4. Keep any post-parse validation logic (e.g. duplicate mesh part ID check)
5. Remove `JsonValue`, `JsonReader`, `BaseJsonReader` once all consumers migrated

**Risk:** Low — jsoniter-scala is already a dependency, codecs are derived at
compile time (no runtime reflection), and the case class approach works on all
platforms. The main effort is defining accurate case class hierarchies for each
JSON format.

**Assessment:** Strongly recommended. High value (eliminates ~1400 LOC, removes
reflection dependency, enables JS/Native for Skin), low risk (library already in use).

**Status:** TODO — annotated in all 6 consumer files

---

## B6: Collections, Math, XML — keep as-is

**Collections** (`ObjectMap`, `DynamicArray`, `ObjectSet`, `ArrayMap`, `OrderedMap`, `OrderedSet`):
Performance-critical, MkArray-backed, no boxing. No Scala library provides
equivalent unboxed game-optimized collections. Keep.

**Math** (`MathUtils`, `Interpolation`, `RandomXS128`):
Game-optimized lookup tables (sin/cos), specialized RNG. No replacement
would be faster. Keep.

**XML**: Already uses scala-xml (cross-platform). Done.

**JSON**: See B5 — `JsonValue` tree walking should be replaced with jsoniter-scala
codec derivation. The parsing library (jsoniter-scala) is already in use; what
changes is eliminating the intermediate `JsonValue` tree in favor of direct decode
into typed case classes.

---

## C: Redundancy Analysis

### C1: TimeUtils vs java.time

With `scala-java-time` + opaque `Millis`/`Nanos`:
- `nanosToMillis`/`millisToNanos` become extension methods with type conversions
- `timeSinceNanos`/`timeSinceMillis` use `Duration.between()`
- TimeUtils kept as compatibility facade

### C2: Logger vs scribe

If scribe adopted:
- Logger becomes thin bridge: `Logger.debug(msg) -> scribe.debug(msg)`
- Or removed entirely in favor of direct scribe calls
- Decision depends on Application lifecycle coupling

### C3: Timer vs Gears

Timer redesigned as Gears-based scheduler:
- `scheduleTask` -> `Async.group { Timer.sleep(delay); task.run() }`
- `repeatCount` -> `Async.group { while(count > 0) { sleep(interval); run() } }`
- TimerThread eliminated entirely

### C4: ComparableTimSort vs Ordering

Already tracked (commit 5de91d1): merge ComparableTimSort with TimSort
using `Ordering[T]`, eliminating all `asInstanceOf` casts.

### C5: JsonValue tree → jsoniter-scala codec derivation

See B5 for full details. When complete:
- `JsonValue.scala`, `JsonReader.scala`, `BaseJsonReader.scala` — removed
- `JsonValueTest.scala` — removed
- 6 consumer files lose ~800 LOC of manual tree walking
- `Skin.scala` no longer needs Java reflection for style field population
- Total: ~1400 LOC eliminated
