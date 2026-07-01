// ISS-679 — global-exposing wrapper for @dimforge/rapier2d-compat.
//
// The SGE browser packaging is bundler-free (ModuleKind.NoModule): a shipped
// game is a plain index.html + main.js with no module loader. The physics JS
// backend (PhysicsExtension.scala) obtains the Rapier module from the global
// `window.RAPIER` in the browser. rapier2d-compat ships only ESM/CJS builds and
// no ready global/UMD build, so we bundle one here: this wrapper re-exports the
// (WASM-inlined) compat namespace onto `window.RAPIER`, and esbuild bundles it
// into a single self-contained IIFE (rapier2d-compat.umd.js). See README.md for
// the exact regeneration command.
import * as RAPIER from "@dimforge/rapier2d-compat";
window.RAPIER = RAPIER;
