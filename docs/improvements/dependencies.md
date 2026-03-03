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

## B4: Compression — keep, platform-adapt later

**Current:** ~1000 LOC in `sge.utils.compression` (full LZMA implementation).

**Assessment:** Only needed for `.g3db` binary format (already deferred via
`UBJsonReader`). No cross-platform LZMA library exists for Scala 3.

**If needed later:** Use platform-adapter pattern (like `BufferOps`):
- JVM: Apache Commons Compress
- JS: lzma-js via facade
- Native: Rust binding via C ABI

**Status:** No action — revisit if `.g3db` support is needed.

---

## B5: Collections, Math, JSON/XML — keep as-is

**Collections** (`ObjectMap`, `DynamicArray`, `ObjectSet`, `ArrayMap`, `OrderedMap`, `OrderedSet`):
Performance-critical, MkArray-backed, no boxing. No Scala library provides
equivalent unboxed game-optimized collections. Keep.

**Math** (`MathUtils`, `Interpolation`, `RandomXS128`):
Game-optimized lookup tables (sin/cos), specialized RNG. No replacement
would be faster. Keep.

**JSON/XML**: Already uses jsoniter-scala + scala-xml (both cross-platform). Done.

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
