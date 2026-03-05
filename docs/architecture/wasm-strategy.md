# WebAssembly Strategy

**Date**: 2026-03-05
**Status**: Decided (Scala.js with optional Wasm backend)

## Decision

Keep Scala.js as the primary browser target. The Scala.js Wasm backend is
an opt-in performance optimization requiring zero code changes.

## Scala.js Wasm Backend

Available since Scala.js 1.17.0 (experimental).

| Aspect | Details |
|--------|---------|
| How it works | Compiles Scala.js IR to Wasm (not Scala Native to Wasm) |
| Requires | Wasm 3.0 + Wasm GC + Exception Handling (exnref) |
| Browser support | Node.js 23+, Chrome 137+, Firefox 131+, Safari 18.4+ |
| Performance | ~30% faster for Scala-dominated computation |
| JS interop perf | Significantly slower (crossing Wasm/JS boundary) |
| Code size | ~2x larger than JS output |
| @JSExport | Not supported |
| Async | JSPI (JavaScript Promise Integration) |
| Module splitting | Not supported |

## Async/Await

JavaScript's single-threaded event loop prevents blocking/awaiting. This
affects:

- **Asset loading**: Must be fully async (fetch-based). SGE's AssetManager
  is already async.
- **Game loop**: Must use requestAnimationFrame callbacks. Standard pattern.
- **GL calls**: Synchronous in WebGL -- not affected.

The async constraint is manageable for game engines because the game loop
is inherently frame-based.

The Wasm backend uses JSPI to allow Wasm code to suspend at await points
and resume when JS Promises resolve. This is not yet available in Safari.

## GPU Access from Wasm

WebGL/WebGPU are JavaScript APIs. Even in Wasm, GPU access goes through
JS imports:

```
Wasm code --> env.glDrawArrays() --> JS import --> gl.drawArrays()
```

There is no "native" GPU path from Wasm. The browser's WebGL implementation
(often ANGLE itself in Chrome/Edge) handles the actual GPU work.

## Other Scala-to-Wasm Paths

| Path | Viability | Notes |
|------|-----------|-------|
| Scala.js Wasm | Best option | Production path, uses Wasm GC |
| Scala Native Emscripten | Theoretically possible | No one has done it, GC in linear memory |
| Scala Native WASI | Future | No browser GPU access |
| GraalVM Wasm | Not viable | native-image doesn't target Wasm |

## WebAssembly Feature Status

| Feature | Status | SGE Relevance |
|---------|--------|---------------|
| Wasm GC | Shipped (Chrome/Firefox/Safari) | Required by Scala.js Wasm |
| SIMD | Standardized | Not exposed via Scala.js |
| Threads | Standardized (SharedArrayBuffer) | Scala.js is single-threaded |
| Exception handling | Shipped (exnref) | Required by Scala.js Wasm |
| JSPI | Chrome shipped, Firefox coming | Enables sync-looking async |
| Component Model | WASI 0.2.0 stable | Not browser-focused yet |

## Recommendation

1. **Now**: Use Scala.js JS backend for browser target (works everywhere)
2. **When browser support catches up**: Enable Wasm backend as opt-in
   (zero code changes, 30% faster compute)
3. **Do not pursue**: Scala Native to Wasm path (not viable near-term)
