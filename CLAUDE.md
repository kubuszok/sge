# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SGE2 is a Scala port of LibGDX, a cross-platform game development framework. This is an active conversion project porting Java/Kotlin LibGDX APIs to idiomatic Scala 3 code. The conversion follows a systematic approach documented in PROGRESS.md.

## Build System

This project uses SBT (Scala Build Tool):

- **Build**: `sbt compile`
- **Format code**: `sbt scalafmt` (scalafmt plugin is configured)
- **Clean**: `sbt clean`
- **Run**: No main class defined yet - this is a library project

Configuration:
- Scala version: 3.7.1
- Compiler flags include `-deprecation`, `-feature`, `-no-indent`, `-rewrite`, `-Xfatal-warnings`

## Project Structure

```
core/src/main/scala/sge/          # Main SGE library code
├── *.scala                       # Core engine classes (Application, Graphics, Audio, etc.)
├── assets/                       # Asset management system
├── audio/                        # Audio subsystem  
├── files/                        # File handling
├── graphics/                     # Graphics rendering
│   ├── g2d/                     # 2D graphics (sprites, batches, etc.)
│   ├── glutils/                 # OpenGL utilities
│   └── profiling/               # Graphics profiling tools
├── input/                        # Input handling
├── math/                         # Mathematical utilities
│   └── collision/               # Collision detection
├── net/                          # Networking
├── scenes/scene2d/              # 2D scene graph (partial port)
└── utils/                        # Utilities
    ├── compression/             # LZMA compression
    └── viewport/                # Viewport management
```

## Architecture

The project follows LibGDX's modular architecture with these key patterns:

1. **Core Engine Access**: The main `Sge` object provides access to all subsystems (graphics, audio, files, input, net)
2. **Scala-First Design**: Uses Scala 3 features like opaque types, given/using context, and idiomatic collections
3. **Package Mapping**: `com.badlogic.gdx.*` → `sge.*` with some structural changes documented in CHANGES.md

## Conversion Status

This is an ongoing port from LibGDX. Key conversion details:

- Most core packages have initial AI-generated conversions
- Manual verification and Scala idiomatization is pending for most modules
- Some LibGDX classes are intentionally skipped in favor of Scala stdlib (collections, JSON, XML, async)
- The `g3d` (3D graphics) package is not yet started

Refer to PROGRESS.md for detailed conversion status of each package.

## Code Conventions

- Use Scala 3 syntax (no curly braces for simple expressions due to `-no-indent`)
- Prefer opaque types over case classes where appropriate  
- Use `given`/`using` for context passing
- Follow existing naming patterns in the codebase
- Maintain LibGDX API compatibility where possible while making it idiomatic Scala

## Java to Scala migration plan

When asked to convert some file from Java to Scala, start by planning before executing the plan.

During planning conform to the following rules:

1. The MCP config can be found in `.vscode/mcp.json`:
    - Use `sge2-metals` MCP to compile the project and obtain the informations about errors and warnings.
    - Use `context7` MCP to obtain information about other libraries.
2. Do NOT call `sbt` directly! If you need to make code compilable as an intermediate step, do not remove the code, comment it out instead and leave a note for yourself.
3. Do NOT use braceless-syntax! Use braces `{}` for `trait`, `class`, `enum` and method definitions.
4. Do NOT remove comments! Leave them unchanged.
5. Do NOT use flat package names! Instead of `package a.b.c` use

    ```scala
    package a
    package b
    package c
    ```

    ! It automatically imports all the definitions from the packages `a`, `a.b` and `a.b.c`, and they don't need to be imported with `import`.

Conversion should be performed as follows:

1. find all Scala files which have Java syntax in them
2. rewrite Java method declarations to Scala declarations and Java types into Scala types
3. move static classes, enums and methods of a class to companion object (create it if needed)
4. move all nested classes and enums from interface to compatnion object (create it if needed)
5. rewrite methods implementations from Java to Scala, with these adjustments:
  a. when code uses `ByteArray`, `CharArray`, `FloatArray`, `IntArray`, `LongArray`, `ShortArray`, use `DynamicArray` of the respective type instead
  b. when code uses `ArrayMap`, `IdentityMap`, `IntFloatMap`, `IntIntMap`, `IntMap`, `LongMap`, `ObjectFloatMap`, `ObjectLongMap`, `ObjectIntMap`, `ObjectMap` use `ArrayMap` of the respective type instead
  c. when code uses `ObjectSet`, use `Set` of the respective type instead
  d. when code uses `BooleanArray`, `Bits` or `IntSet`, use `BitSet` of the respective type instead
  e. when code uses `OrderedSet`, use `SortedSet` of the respective type instead
  f. when code uses `GdxException`, use one of `SgeError`s instead
  g. when code uses global `Gdx`, use implicit `Sge` instead
  h. when having uninitialized `var`, use `scala.compiletime.uninitialized` instead of `_`
  i. when seeing a type that can be `null` as an argument/returned value, use `sde.utils.Nullable` instead of `Option` - see `Nullable[A]` usage
  j. when seeing `math.methodName` consider it might be `Math.methodName` instead of a method in `math` package
  k. when after rewrite there is still a `return` rewrite the code to use `scala.util.boundary` - see Removing `return`, `break` and `continue`
  l. when seeing something that is `java.util.Comparator` or could be, convert it to `given Ordering`
  m. when seeing `SnapshotArray` use `ArrayBuffer`, and where modifications between `begin()` and `end()` are made on a copy and after `end()` modified copy replaces the original
  n. when seeing `Disposable`, replace it with `AutoCloseable` and `dispose()` method with `close()` method
  o. when seeing value assigned to `Collections.allocateIterators`, remove this statement
  p. when seeing `matrix.val`, use `matrix.values`
  q. when seeing `AsyncTask<T>`, use `() => T` and rename `call()` to `def apply()`
  r. when seeing `AsyncResult<t>`, use `scala.concurrent.Future[T]` instead (adjust code for `Future`s and `ExecutorContext`s)
  s. when seeing `ApplicationAdapter` use `Application`, when `InputAdapter` use `Input`, when `ScreenAdapter` use `Screen`
6. use `sge2-metals` MCP to ensure that conversion is successful
7. fix both errors and warnings

## `Nullable[A]` usage

1. replace `if (nullable == null) value else nullable.asInstanceOf[A]` with `nullable.getOrElse(value)`
1. replace `if (nullable == null) throw error else { val a = nullable.asInstanceOf[A]; ... }` with `nullable.fold(throw error)(a => ...)`
2. replace `if (nullable == null) thunk else { val a = nullable.asInstanceOf[A]; ... }` with `nullable.fold(thunk)(a => ...)`
3. replace `if (nullable != null) { val a = nullable.asInstanceOf[A]; ... }` (no else) with `nullable.foreach { a => ... }`
3. replace `nullable != null` or `!(nullable == null)` with `nullable.isDefined`
4. replace `nullable == null` with `nullable.isEmpty`
5. replace `nullable = null.asInstanceOf[Nullable[A]]`, `nullable = _` and `nullable = scala.compiletime.uninitialized` with `nullable = Nullable.empty`

In general:

- initialize value using `Nullable.empty` instead of `null` and `Nullable(value)` when it's non-null or unknown
- use `fold` when there is branching handling both cases of `null` and non-`null`
  - use `getOrElse` instead if non-`null` only returns unchanged value and `null` returns a single value
- use `foreach` if there is no branch handling `null`

## Removing `return`, `break` and `continue`

This project does not allow `return`, `break` and `continue`, when seeing one rewrite the code

1. replace

```scala
def method = {
  // code
  return value
  // code
}
```

with 

```scala
def method = scala.util.boundary {
  // code
  scala.util.boundary.break(value)
  // code
}
```

2. replace

```java
while (cond) {
  // code
  break;
}
```

with

```scala
scala.util.boundary {
  while (cond) {
    // code
    scala.util.boundary.break()
  }
}
```

3. replace

```java
while (cond) {
  // code
  continue;
}
```

with

```scala
while (cond) {
  scala.util.boundary {
    // code
    scala.util.boundary.break()
  }
}
```
