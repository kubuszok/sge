# Scaladoc Style Guide

How to write API documentation (scaladoc / class- and member-level comments) for
SGE. This complements [code-style.md](code-style.md), which covers license headers
and formatting. The license header and its `Migration notes:` / `Covenant:` blocks
are **not** scaladoc and are governed there; this guide is about the `/** ... */`
documentation that a user of the API reads in their IDE.

SGE is a game-engine API, not a tutorial. Keep docs tight: **1–6 lines per member,
class docs ≤ 10 lines.** A false or stale doc is worse than no doc — every claim you
write is verified against the code.

## The seven rules

### 1. Class-level doc for every public type

Every public type gets a class-level scaladoc: one sentence naming its role, then how
it fits the getting-started path (2–4 sentences max). Say how a user reaches an
instance — most engine services are reached through the application context
`[[sge.Sge]]` (e.g. `Sge().graphics`, `Sge().input`).

```scala
/** Encapsulates communication with the graphics processor: querying the framebuffer,
  * timing, display modes, and the GL bindings used to draw.
  *
  * Reached through the application context as `Sge().graphics`. The GL bindings it
  * exposes ([[sge.graphics.GL20]] and, when available, [[sge.graphics.GL30]]) are the
  * lowest level most games touch directly.
  */
trait Graphics { ... }
```

### 2. Nullable semantics are explicit

Any member that returns or takes a `lowlevel.Nullable` documents what the empty case
**means** — not just that it can be empty. Prefer "empty when …" phrasing over the
word "null".

```scala
/** The GL30 bindings, empty when the backend/config did not request an OpenGL ES 3.0
  * context (check [[gl30Available]] first).
  */
def gl30: Nullable[GL30]
```

### 3. `(using Sge)` members name the context, never "global"

`Sge` is the per-application context that replaces LibGDX's global `Gdx.*`. When a
member takes `(using Sge)`, or when you mention how a service is reached, link
[[sge.Sge]] and describe it as the application context passed explicitly. Never call
it a "global" or a "singleton lookup" — it is threaded through explicitly.

### 4. LibGDX heritage note for renamed / relocated types

A type or member that was renamed or moved from LibGDX gets an `@note` line pointing at
the real original path, so a migrating user can find it. Use the actual path from
`original-src/libgdx/` (the same path recorded in `Covenant-source-reference`).

```scala
/** @note LibGDX: `com.badlogic.gdx.Gdx` — the global static holder became this
  *       explicitly-passed context.
  */
```

Do not add a heritage note to a type whose name and package match LibGDX unchanged.

### 5. Honest platform-gap notes

Where behavior genuinely differs per platform (JS / Native / Android capability
differences), add an `@note Platform:` line. State what actually happens — never
promise behavior the code does not implement. If you find an existing doc comment that
overclaims relative to the code, **fix it to the truth** (these are the
"worse-than-no-doc" cases). Describe an unsupported path by its observable effect
(e.g. "returns an empty array on platforms without the API", or "signals
`SgeError.Unsupported`") rather than with placeholder vocabulary.

### 6. No stale examples

Any code example in scaladoc must use **current** SGE names and be
compile-plausible: `Sge().net.httpClient`, not `sge.net.httpClient`; the current
method spelling, not a renamed-away one. Verify each identifier in an example against
the code before writing it. Preserved LibGDX Java-syntax examples inside migrated
javadoc may stay as historical reference, but any example you add is Scala and current.

### 7. Scaladoc adds, it never removes migration history

The no-comment-removal rule (see code-style.md) is absolute. Original-source comments,
`@author` attributions, and `Migration notes:` blocks are preserved. Scaladoc you add
sits alongside them; it does not replace them. Correcting a **stale or false** claim in
an existing doc comment is not removal — it is rule 5/6 — but delete nothing that is
merely historical.

## Mechanics

- Link with scaladoc `[[fully.qualified.Name]]` or `[[member]]`. Migrated javadoc may
  still contain `{@link X}`; that renders as literal text and is harmless — do not
  churn it into `[[ ]]` wholesale, only where you are already editing for truth.
- Keep `@param` / `@return` only where they add information beyond the prose. Do not
  pad every getter with an empty `@return`.
- Documentation is comment-only. A doc change must never alter an executable token: if
  you strip the comments, the diff is empty.
- Do not touch `Covenant-*` header lines. Adding docs changes a file's line count;
  the covenant baseline is re-stamped separately at merge time.
</content>
</invoke>
