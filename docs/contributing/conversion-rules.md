# Java to Scala Conversion Rules

When asked to convert a file from Java to Scala, **start by planning before executing**.

## Planning Rules

1. Use `re-scale build compile` to compile and get errors/warnings.
2. Use `context7` MCP to look up library documentation.
3. If you need to make code compilable as an intermediate step, **comment out** problematic code
   instead of removing it. Leave a note for yourself.
4. **Do NOT use braceless syntax.** Use braces `{}` for `trait`, `class`, `enum` and method definitions.
5. **Do NOT remove comments.** Leave them unchanged.
6. **Do NOT use flat package names.** Instead of `package a.b.c` use:

```scala
package a
package b
package c
```

This automatically imports all definitions from packages `a`, `a.b` and `a.b.c`.

## Conversion Procedure

### Step 0: Add license header

Add the license header as the first thing in the file, before the `package` declaration.
See [code-style.md](code-style.md) for the full template. Extract `@author` tags from
the original Java file for the `Original authors` line. Include the original source path.

### Step 1: Find Java syntax

Find all Scala files which still have Java syntax in them.

### Step 2: Rewrite declarations

Rewrite Java method declarations to Scala declarations and Java types into Scala types.

### Step 3: Move static members

Move static classes, enums and methods of a class to a companion object (create it if needed).

### Step 4: Move nested types from interfaces

Move all nested classes and enums from interfaces to companion objects (create them if needed).

### Step 5: Rewrite implementations

Rewrite method implementations from Java to Scala, applying these adjustments:

| # | When you see... | Replace with... |
|---|----------------|-----------------|
| a | `ByteArray`, `CharArray`, `FloatArray`, `IntArray`, `LongArray`, `ShortArray` | `DynamicArray` of the respective type |
| b | `ObjectMap`, `IntMap`, `LongMap`, `IntIntMap`, `IntFloatMap`, `ObjectIntMap`, `ObjectFloatMap`, `ObjectLongMap` | `ObjectMap[K, V]` of the respective types |
| b2 | `ArrayMap`, `IdentityMap` | `ArrayMap[K, V]` of the respective types |
| c | `ObjectSet`, `IntSet` | `ObjectSet[A]` of the respective type |
| d | `BooleanArray`, `Bits` | `mutable.BitSet` |
| e | `OrderedSet` | `OrderedSet[A]` of the respective type |
| e2 | `OrderedMap` | `OrderedMap[K, V]` of the respective types |
| f | `GdxException` / `GdxRuntimeException` | One of `SgeError`s |
| g | Global `Gdx` | Implicit `Sge` (via `given`/`using`) — see **Sge propagation** below |
| h | Uninitialized `var` (`= _`) | `= scala.compiletime.uninitialized` |
| i | Nullable argument/return type | `Nullable[A]` — see [nullable-guide.md](nullable-guide.md) |
| j | `math.methodName` | Check if it should be `Math.methodName` (java.lang.Math) |
| k | `return` keyword | `scala.util.boundary`/`break` — see [control-flow-guide.md](control-flow-guide.md) |
| l | `java.util.Comparator` | `given Ordering` |
| m | `SnapshotArray` | `DynamicArray` — modifications between `begin()`/`end()` on a copy, replace after `end()` |
| n | `Disposable` / `dispose()` | `AutoCloseable` / `close()` |
| o | Assignment to `Collections.allocateIterators` | Remove the statement |
| p | `matrix.val` | `matrix.values` |
| q | `AsyncTask<T>` / `call()` | `() => T` / `def apply()` |
| r | `AsyncResult<T>` | `scala.concurrent.Future[T]` (adjust for `Future`s and `ExecutionContext`s) |
| s | `ApplicationAdapter` / `InputAdapter` / `ScreenAdapter` | `Application` / `Input` / `Screen` |

### Sge propagation

LibGDX uses global static `Gdx.graphics`, `Gdx.input`, etc. SGE replaces these with
`(using Sge)` context parameters. **Never leave a TODO for missing Sge context.**

Add `(using Sge)` to the **class constructor**, not individual methods. Within a
single application, every class uses the same Sge instance — it's effectively a
per-application singleton passed explicitly instead of via globals. This enables
parallel testing and multiple application instances without global state conflicts.

Key patterns:
- `Gdx.graphics.getGL20()` → `Sge().graphics.getGL20()`
- `Gdx.input.isKeyPressed(k)` → `Sge().input.isKeyPressed(k)`
- Classes using `Sge()` calls need `(using Sge)` on their **constructor**
- Subclasses inherit: `class Button()(using Sge) extends Table()`
- Secondary constructors also need `(using Sge)`:
  `def this(skin: Skin)(using Sge) = { this(); ... }`
- Methods do NOT need `(using Sge)` — it's available from the class level
- Example hierarchy: `Actor()(using Sge)` → `Group` → `WidgetGroup` → `Table` → `Button`

### Step 6: Verify compilation

Use `re-scale build compile --all` to ensure the conversion compiles on all platforms
(JVM, JS, Native). All 4 platforms are baseline — changes must be non-regressing.

### Step 7: Fix issues

Fix **both** errors and warnings.

### Step 8: Run tests

Run `re-scale test unit --all` to verify the conversion doesn't break existing tests
on any platform. If a test reveals a pre-existing bug, fix the bug in the source
code — never patch the test to avoid it.
