# Verification Checklist

Use this checklist when verifying an AI-converted file. Track status via
`sge-dev db migration` and `sge-dev db audit`.

## License Header

- [ ] File has license header as the first thing (before `package`)
- [ ] `Original source` path matches the LibGDX file(s) this was ported from
- [ ] `Original authors` extracted from `@author` tags (or "See AUTHORS file")
- [ ] `Scala port Copyright 2024-2026 Mateusz Kubuszok` line present

## Compilation

- [ ] File compiles without errors via `sge-dev build compile`
- [ ] File compiles without warnings (warnings are fatal)

## Completeness

- [ ] All public methods from the LibGDX source are present
- [ ] All constants/enums from the LibGDX source are present
- [ ] No methods have been accidentally omitted or stubbed out
- [ ] Comments from the original source are preserved

## Scala Idioms

- [ ] No `return` keyword — uses `boundary`/`break` instead
- [ ] No direct `null` checks — uses `Nullable[A]` patterns
- [ ] No Java-style `void` / `boolean` / `public` / `static` keywords
- [ ] Uses `given`/`using` instead of `java.util.Comparator`
- [ ] Uses `AutoCloseable`/`close()` instead of `Disposable`/`dispose()`
- [ ] Uses `scala.compiletime.uninitialized` for uninitialized vars
- [ ] Braces `{}` used for all class/trait/method definitions (no braceless syntax)
- [ ] Split package declarations (not flat `package a.b.c`)

## Type Mappings

- [ ] LibGDX collections replaced with Scala equivalents (see [type-mappings.md](type-mappings.md))
- [ ] `GdxException` replaced with `SgeError`
- [ ] `Gdx` global replaced with implicit `Sge`
- [ ] `math.method` calls verified (java.lang.Math vs sge.math package)

## Testing

- [ ] Compiles on all platforms: `sge-dev build compile --all`
- [ ] Tests pass on all platforms: `sge-dev test unit --all`
- [ ] If a test reveals a pre-existing bug in the codebase, fix the bug — never patch the test to work around it

## Status Progression

After verification, update status via `sge-dev db audit set <file> pass`.
See `/audit-file` and `/audit-package` skills for the full audit workflow.
