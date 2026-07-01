# Vendored `rapier2d-compat` global build (ISS-679)

SGE browser packaging is **bundler-free** (`ModuleKind.NoModule`): a shipped
game is a plain `index.html` + `main.js`, with no module loader. The physics JS
backend obtains the Rapier module from a browser global:

```scala
// sge-extension/physics/src/main/scalajs/sge/platform/PhysicsExtension.scala
if (js.typeOf(js.Dynamic.global.RAPIER) != "undefined") js.Dynamic.global.RAPIER
else js.Dynamic.global.require("@dimforge/rapier2d-compat")
```

`@dimforge/rapier2d-compat` ships only ESM/CJS builds and **no ready
global/UMD build**, so a shipped browser game would find neither `window.RAPIER`
nor a CommonJS `require`, and physics would fail to load. This directory vendors
a single self-contained IIFE that exposes `window.RAPIER`, which
`SgePackaging.packageBrowserTask` injects (via `<script>` before `main.js`) when
`sgeBrowserIncludeRapier2d := true`.

The `rapier2d-compat` variant **inlines its WASM as base64**, so the produced
`rapier2d-compat.umd.js` is a single self-contained file — no separate `.wasm`
to serve.

## Files

- `wrapper.mjs` — re-exports the compat namespace onto `window.RAPIER`.
- `package.json` — pins `@dimforge/rapier2d-compat` `0.19.3` (matches the sge
  root `package.json`) + the esbuild dev dependency.
- `rapier2d-compat.umd.js` — **the committed build artifact** shipped by the
  bundler-free packaging. Regenerate with the command below.

## Regeneration

From this directory:

```sh
npm install
npx esbuild wrapper.mjs --bundle --format=iife --outfile=rapier2d-compat.umd.js
```

`npm install` writes a local `node_modules/` — do **not** commit it. Only commit
`rapier2d-compat.umd.js` (plus `wrapper.mjs`, `package.json`, this README).

When bumping the Rapier version, update it in both this `package.json` and the
sge root `package.json`, then rerun the two commands above.

## Scope

This handles **2D physics** (`rapier2d`) only, per ISS-679. The 3D physics
extension (`@dimforge/rapier3d-compat`) needs the same treatment; that is
tracked as a follow-up under ISS-677.
