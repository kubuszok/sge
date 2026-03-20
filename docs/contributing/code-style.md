# Code Style Guide

## License Header

Every ported Scala file **must** have a license header as the first thing in the file,
before the `package` declaration. The header links back to the original LibGDX source
and preserves attribution.

### Template

```scala
/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/<path>/<ClassName>.java
 * Original authors: <authors from @author tags, or "See AUTHORS file">
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
```

### Rules

- **`Original source`**: The path relative to `libgdx/gdx/src/`. For merged files, list
  all original sources on separate lines.
- **`Original authors`**: Extract from `@author` Javadoc tags in the original file.
  If none are present, use `See AUTHORS file` (the AUTHORS file lists Mario Zechner
  and Nathan Sweet as the copyright holders).
- **Copyright year range**: Starts at `2025` (project inception), ends at current year.
- **Lowercase `copyright`**: Use `copyright` (not `Copyright`) in the header.
- **For new SGE-only files** (no LibGDX equivalent): Use just the Scala port copyright,
  no "Ported from" line.

### Example (merged file)

```scala
/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/math/Vector.java
 *                  com/badlogic/gdx/math/Vector2.java
 *                  com/badlogic/gdx/math/Vector3.java
 *                  com/badlogic/gdx/math/Vector4.java
 * Original authors: badlogicgames@gmail.com, Xoppa
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
```

## Scala 3 Syntax Rules

### Braces required

Always use braces `{}` for `trait`, `class`, `enum`, and method definitions.
The compiler flag `-no-indent` enforces this.

```scala
// Correct:
class Foo {
  def bar: Int = {
    42
  }
}

// Wrong (braceless):
class Foo:
  def bar: Int =
    42
```

### Split package declarations

Never use flat package names. Always split them:

```scala
// Correct:
package sge
package graphics
package g2d

// Wrong:
package sge.graphics.g2d
```

Split packages automatically import all definitions from parent packages (`sge`, `sge.graphics`,
and `sge.graphics.g2d`), removing the need for explicit imports.

## Naming Conventions

- Follow existing naming patterns in the codebase
- Maintain LibGDX API compatibility where possible while making it idiomatic Scala
- Prefer Scala 3 features: opaque types over case classes, `given`/`using` over implicits

## Comments

**Never remove comments** during conversion. Leave them unchanged from the original source.

## Scalafmt

The project uses scalafmt for formatting. Run `sge-dev build fmt` after changes.
Configuration is in `.scalafmt.conf`.

## Compiler Flags

Core flags: `-deprecation`, `-feature`, `-no-indent`, `-rewrite`, `-Werror`

Linter flags (all enabled):
- `-Wimplausible-patterns` — warn on pattern matches that can never match
- `-Wrecurse-with-default` — warn on recursive calls with default arguments
- `-Wenum-comment-discard` — warn when comments between enum cases are discarded
- `-Wunused:imports,privates,locals,patvars,nowarn` — warn on unused symbols

All warnings are fatal (`-Werror`) — treat them as errors. Use `@nowarn("id=E198")`
to suppress specific warnings where justified.
