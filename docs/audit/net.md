# Audit: sge.net

Audited: 12/12 files | Pass: 8 | Minor: 1 | Major: 3
Last updated: 2026-03-03

---

### HttpParametersUtils.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/net/HttpParametersUtils.scala` |
| Java source(s) | `com/badlogic/gdx/net/HttpParametersUtils.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods and fields match.
**Convention changes**: Java `final class` -> `object`; `Map<String,String>` -> `mutable.Map`
**Issues**: None

---

### HttpRequestHeader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/net/HttpRequestHeader.scala` |
| Java source(s) | `com/badlogic/gdx/net/HttpRequestHeader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 31 string constants match exactly.
**Convention changes**: Java interface with `static final` -> `object` with `val`
**Issues**: None

---

### HttpResponseHeader.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/net/HttpResponseHeader.scala` |
| Java source(s) | `com/badlogic/gdx/net/HttpResponseHeader.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 36 string constants match exactly.
**Issues**: None

---

### NetJavaServerSocketImpl.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/net/NetJavaServerSocketImpl.scala` |
| Java source(s) | `com/badlogic/gdx/net/NetJavaServerSocketImpl.java` |
| Status | major_issues |
| Tested | No |

**Completeness**: All methods present.
**Issues**:
- `major`: **Double initialization bug** — secondary constructor delegates to primary (which calls `initializeServer`), then calls `initializeServer` again. Java delegates 3-arg to 4-arg so init runs once.
- `minor`: Raw `null` assignment in `close()` without `@nowarn`

---

### NetJavaSocketImpl.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/net/NetJavaSocketImpl.scala` |
| Java source(s) | `com/badlogic/gdx/net/NetJavaSocketImpl.java` |
| Status | major_issues |
| Tested | No |

**Completeness**: All methods present.
**Issues**:
- `major`: **Secondary constructor creates throwaway socket** — delegates to primary (which creates + connects socket), then overwrites `this.socket` with the passed-in socket. Java's 2-arg constructor directly assigns.
- `minor`: Raw `null` assignments without `@nowarn`

---

### ServerSocket.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/net/ServerSocket.scala` |
| Java source(s) | `com/badlogic/gdx/net/ServerSocket.java` |
| Status | pass |
| Tested | No |

**Completeness**: All methods ported. `Disposable` -> `AutoCloseable`.
**Issues**: None

---

### ServerSocketHints.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/net/ServerSocketHints.scala` |
| Java source(s) | `com/badlogic/gdx/net/ServerSocketHints.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 7 fields with correct defaults.
**Issues**: None

---

### Socket.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/net/Socket.scala` |
| Java source(s) | `com/badlogic/gdx/net/Socket.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 4 methods. `Disposable` -> `AutoCloseable`.
**Issues**: None

---

### SocketHints.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/net/SocketHints.scala` |
| Java source(s) | `com/badlogic/gdx/net/SocketHints.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 11 fields with correct defaults.
**Issues**: None

---

### HttpRequestBuilder.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/net/HttpRequestBuilder.scala` |
| Java source(s) | `com/badlogic/gdx/net/HttpRequestBuilder.java` |
| Status | major_issues |
| Tested | No |

**Completeness**: Structure matches but key methods are stubbed out.
**Convention changes**: `method()` takes `Net.HttpMethod` enum instead of `String`
**Issues**:
- `major`: `newRequest()` body commented out — builder cannot create requests
- `major`: Companion `json` field commented out; `jsonContent()` uses placeholder
- `minor`: Raw `null` in `build()` without `@nowarn`

---

### HttpStatus.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/net/HttpStatus.scala` |
| Java source(s) | `com/badlogic/gdx/net/HttpStatus.java` |
| Status | pass |
| Tested | No |

**Completeness**: All 40 status code constants match.
**Convention changes**: Java class with `int statusCode` -> `opaque type HttpStatus = Int` with extension methods
**Issues**: None

---

### NetJavaImpl.scala

| Field | Value |
|-------|-------|
| SGE path | `core/src/main/scala/sge/net/NetJavaImpl.scala` |
| Java source(s) | `com/badlogic/gdx/net/NetJavaImpl.java` |
| Status | pass |
| Tested | No |

**Completeness**: All public + private methods ported. Inner `HttpClientResponse` fully functional.
**Renames**: `sendHttpRequest` listener param -> `Option[HttpResponseListener]`
**Issues**: None
