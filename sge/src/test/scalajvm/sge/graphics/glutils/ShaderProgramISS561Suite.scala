/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Test coverage for ISS-561 (batch F): glutils ShaderProgram — a core class
 * that had zero unit tests. ShaderProgram is a GL state machine: its
 * constructor compiles two shaders and links a program, then uniform/attribute
 * accessors drive further GL calls with a location cache.
 *
 * Every expected behaviour below is hand-traced from the original
 * com/badlogic/gdx/graphics/glutils/ShaderProgram.java (original-src/libgdx).
 * Java line numbers cited refer to that file:
 *   - constructor (lines 156-173): compileShaders(...) then, iff isCompiled(),
 *     fetchAttributes()/fetchUniforms()/addManagedShader.
 *   - compileShaders (183-199): vertexShaderHandle = loadShader(GL_VERTEX_SHADER,
 *     ...) (184); fragmentShaderHandle = loadShader(GL_FRAGMENT_SHADER, ...) (185);
 *     if either is -1 -> isCompiled = false (187-190); else program =
 *     linkProgram(createProgram()) (192); program == -1 -> isCompiled = false
 *     (193-196); else isCompiled = true (198).
 *   - loadShader (201-225): gl.glCreateShader(type) (205); if 0 -> -1 (206);
 *     glShaderSource (208); glCompileShader (209); glGetShaderiv(shader,
 *     GL_COMPILE_STATUS, intbuf) (210); compiled = intbuf.get(0) (212); if 0 ->
 *     glGetShaderInfoLog + append to log + return -1 (213-222).
 *   - createProgram (227-231): glCreateProgram() (229); != 0 ? program : -1 (230).
 *   - linkProgram (233-257): glAttachShader(program, vertexShaderHandle) (237);
 *     glAttachShader(program, fragmentShaderHandle) (238); glLinkProgram (239);
 *     glGetProgramiv(program, GL_LINK_STATUS, intbuf) (245); linked = intbuf.get(0)
 *     (246); if 0 -> log = glGetProgramInfoLog + return -1 (247-254).
 *   - fetchUniforms / fetchAttributes (Scala port lines 857-895, mirroring Java):
 *     glGetProgramiv(program, GL_ACTIVE_UNIFORMS/ATTRIBUTES, params) then loop
 *     `numX` times. Returning 0 here keeps both loops empty so construction
 *     succeeds with no enumeration side effects.
 *   - isCompiled (277-279): returns the isCompiled flag.
 *   - fetchUniformLocation(name, pedantic) (297-310): if `uniforms.get(name, -2)
 *     == -2` (uncached) -> location = glGetUniformLocation(program, name) (302)
 *     and uniforms.put(name, location) (307). On a SECOND call the cached value
 *     is returned WITHOUT calling glGetUniformLocation again. (Port:
 *     ShaderProgram.scala lines 233-246 via an ObjectMap cache.)
 *   - setUniformi(name, value) (316-321): checkManaged(); location =
 *     fetchUniformLocation(name) (319); glUniform1i(location, value) (320).
 *   - bind() (Java 670-674 / port 753-757): checkManaged(); glUseProgram(program).
 *   - dispose()/close() (Java 1004-1014 / port 767-774): glUseProgram(0);
 *     glDeleteShader(vertex); glDeleteShader(fragment); glDeleteProgram(program).
 *
 * Headless strategy: a recording GL20 below extends GL20, delegates everything
 * to NoopGL20 via `export`, and overrides ONLY the handful of state-machine
 * entry points. It returns success values so a ShaderProgram constructs cleanly:
 * distinct nonzero shader handles, a nonzero program handle, GL_TRUE for compile
 * and link status, 0 active uniforms/attributes, and a fixed uniform location.
 * Each interesting call is appended to `calls` so we can pin the exact ORDER and
 * the exact CACHING behaviour.
 *
 * Mutations these tests catch (campaign requirement):
 *   - Drop the uniform-location cache (always call glGetUniformLocation in
 *     setUniformi): the "uniform location is fetched ONCE" test fails because
 *     glGetUniformLocation would be recorded twice instead of once.
 *   - isCompiled ignores link status (e.g. always returns true, or the
 *     `program == -1 -> false` branch is removed): the "link status GL_FALSE ->
 *     not compiled" test fails because compiled would be true.
 *   - bind() uses the wrong handle (e.g. glUseProgram(0) instead of
 *     glUseProgram(program)): the bind test fails because the recorded argument
 *     would be 0, not the program handle 20.
 */
package sge
package graphics
package glutils

import java.nio.IntBuffer

import sge.noop.{ NoopGL20, NoopGraphics }

class ShaderProgramISS561Suite extends munit.FunSuite {

  // --- Fixed handles the recording GL hands back -----------------------------
  private val VertexShaderHandle   = 11
  private val FragmentShaderHandle = 12
  private val ProgramHandle        = 20
  private val FixedUniformLocation = 5
  private val FixedAttribLocation  = 3

  /** A single recorded GL call: the GL function name plus the integer arguments we care about pinning (handle / location / value).
    */
  final private case class Call(name: String, args: List[Int])

  /** Recording GL20 that lets a ShaderProgram construct successfully.
    *
    * @param compileStatus
    *   value written into the buffer by glGetShaderiv(GL_COMPILE_STATUS) — GL_TRUE(1) to make shaders compile, GL_FALSE(0) to fail them.
    * @param linkStatus
    *   value written into the buffer by glGetProgramiv(GL_LINK_STATUS) — GL_TRUE(1) to link, GL_FALSE(0) to fail linking.
    */
  final private class RecordingGL20(compileStatus: Int = GL20.GL_TRUE, linkStatus: Int = GL20.GL_TRUE) extends GL20 {

    val calls: scala.collection.mutable.ListBuffer[Call] = scala.collection.mutable.ListBuffer.empty

    // Delegate every un-overridden method to the no-op implementation. The
    // overridden glGetShaderiv/glGetProgramiv/glCreateShader/... below shadow
    // the exported ones.
    private val underlying: GL20 = NoopGL20
    export underlying.{
      glAttachShader as _,
      glCompileShader as _,
      glCreateProgram as _,
      glCreateShader as _,
      glDeleteProgram as _,
      glDeleteShader as _,
      glGetAttribLocation as _,
      glGetProgramInfoLog as _,
      glGetProgramiv as _,
      glGetShaderInfoLog as _,
      glGetShaderiv as _,
      glGetUniformLocation as _,
      glLinkProgram as _,
      glUniform1i as _,
      glUseProgram as _,
      *
    }

    private var nextShaderHandle = VertexShaderHandle

    override def glCreateShader(`type`: ShaderType): Int = {
      calls += Call("glCreateShader", List(`type`.toInt))
      val handle = nextShaderHandle
      nextShaderHandle = FragmentShaderHandle // first call -> 11 (vertex), second -> 12 (fragment)
      handle
    }

    override def glCompileShader(shader: Int): Unit =
      calls += Call("glCompileShader", List(shader))

    override def glGetShaderiv(shader: Int, pname: Int, params: IntBuffer): Unit = {
      calls += Call("glGetShaderiv", List(shader, pname))
      if (pname == GL20.GL_COMPILE_STATUS) params.put(0, compileStatus)
    }

    override def glGetShaderInfoLog(shader: Int): String = "SHADER-COMPILE-FAILED"

    override def glCreateProgram(): Int = {
      calls += Call("glCreateProgram", Nil)
      ProgramHandle
    }

    override def glAttachShader(program: Int, shader: Int): Unit =
      calls += Call("glAttachShader", List(program, shader))

    override def glLinkProgram(program: Int): Unit =
      calls += Call("glLinkProgram", List(program))

    override def glGetProgramiv(program: Int, pname: Int, params: IntBuffer): Unit = {
      calls += Call("glGetProgramiv", List(program, pname))
      if (pname == GL20.GL_LINK_STATUS) params.put(0, linkStatus)
      // GL_ACTIVE_UNIFORMS / GL_ACTIVE_ATTRIBUTES -> 0 (empty enumeration). The
      // fetch loops then run zero iterations and never touch the buffer again.
      else if (pname == GL20.GL_ACTIVE_UNIFORMS || pname == GL20.GL_ACTIVE_ATTRIBUTES) params.put(0, 0)
    }

    override def glGetProgramInfoLog(program: Int): String = "PROGRAM-LINK-FAILED"

    override def glGetUniformLocation(program: Int, name: String): Int = {
      calls += Call("glGetUniformLocation", List(program))
      FixedUniformLocation
    }

    override def glGetAttribLocation(program: Int, name: String): Int = {
      calls += Call("glGetAttribLocation", List(program))
      FixedAttribLocation
    }

    override def glUseProgram(program: Int): Unit =
      calls += Call("glUseProgram", List(program))

    override def glUniform1i(location: Int, x: Int): Unit =
      calls += Call("glUniform1i", List(location, x))

    override def glDeleteShader(shader: Int): Unit =
      calls += Call("glDeleteShader", List(shader))

    override def glDeleteProgram(program: Int): Unit =
      calls += Call("glDeleteProgram", List(program))
  }

  private def makeSge(glImpl: GL20): Sge =
    SgeTestFixture.testSge(graphics = new NoopGraphics() {
      override def gl20: GL20 = glImpl
    })

  /** Construct a ShaderProgram and return (program, recordingGL). pedantic is forced off so a fixed/non-(-1) uniform location is never rejected — the cache behaviour is what we exercise, not pedantic
    * validation.
    */
  private def makeShader(gl: RecordingGL20): ShaderProgram = {
    given Sge = makeSge(gl)
    ShaderProgram.pedantic = false
    new ShaderProgram("void main(){}", "void main(){}")
  }

  // --- isCompiled true + exact compile/link call sequence ---------------------

  test("ISS561: a valid ShaderProgram reports compiled == true and emits the exact compile->link GL sequence") {
    val gl     = new RecordingGL20()
    val shader = makeShader(gl)

    assertEquals(shader.compiled, true, "compile + link both succeed -> isCompiled is true (Java line 198, isCompiled() line 277)")
    assertEquals(shader.handle, ProgramHandle, "handle returns the linked program (Java program field, port line 978)")

    // The state machine, in order (Java lines 184-185 loadShader x2, then 192
    // createProgram -> linkProgram). Within loadShader: glCreateShader,
    // glCompileShader, glGetShaderiv(GL_COMPILE_STATUS). Within link:
    // glCreateProgram, glAttachShader x2, glLinkProgram,
    // glGetProgramiv(GL_LINK_STATUS).
    val expected = List(
      Call("glCreateShader", List(ShaderType.Vertex.toInt)),
      Call("glCompileShader", List(VertexShaderHandle)),
      Call("glGetShaderiv", List(VertexShaderHandle, GL20.GL_COMPILE_STATUS)),
      Call("glCreateShader", List(ShaderType.Fragment.toInt)),
      Call("glCompileShader", List(FragmentShaderHandle)),
      Call("glGetShaderiv", List(FragmentShaderHandle, GL20.GL_COMPILE_STATUS)),
      Call("glCreateProgram", Nil),
      Call("glAttachShader", List(ProgramHandle, VertexShaderHandle)),
      Call("glAttachShader", List(ProgramHandle, FragmentShaderHandle)),
      Call("glLinkProgram", List(ProgramHandle)),
      Call("glGetProgramiv", List(ProgramHandle, GL20.GL_LINK_STATUS))
    )
    // After link, fetchAttributes/fetchUniforms each enumerate via
    // glGetProgramiv(GL_ACTIVE_ATTRIBUTES/UNIFORMS) returning 0 -> no further
    // recorded calls of interest. Verify the prefix is exactly the sequence.
    assertEquals(
      gl.calls.take(expected.size).toList,
      expected,
      "compile->link state machine must run in exactly this order with these handles"
    )

    // The two shader handles attached are the two distinct created handles.
    assertEquals(VertexShaderHandle != FragmentShaderHandle, true)
  }

  // --- compile status GL_FALSE -> not compiled + log captured -----------------

  test("ISS561: a shader whose compile status is GL_FALSE reports compiled == false and captures the info log") {
    // glGetShaderiv writes GL_FALSE -> loadShader returns -1 (Java line 221) ->
    // compileShaders sets isCompiled = false (line 188) and never links.
    val gl     = new RecordingGL20(compileStatus = GL20.GL_FALSE)
    val shader = makeShader(gl)

    assertEquals(
      shader.compiled,
      false,
      "compile status GL_FALSE -> loadShader returns -1 -> isCompiled false (Java lines 213-221, 187-189)"
    )
    assert(
      shader.log.contains("SHADER-COMPILE-FAILED"),
      s"the captured glGetShaderInfoLog must be in the log; got: ${shader.log}"
    )
    assert(
      shader.log.contains("Vertex shader"),
      s"failing the vertex shader records the 'Vertex shader' prefix (Java line 218); got: ${shader.log}"
    )

    // No program is ever created when a shader fails to compile.
    assert(
      !gl.calls.exists(_.name == "glCreateProgram"),
      "a failed compile must short-circuit before createProgram (Java lines 187-190)"
    )
    assert(!gl.calls.exists(_.name == "glLinkProgram"), "a failed compile must never reach glLinkProgram")
  }

  // --- link status GL_FALSE -> not compiled -----------------------------------

  test("ISS561: a program whose link status is GL_FALSE reports compiled == false (link status is honored)") {
    // Shaders compile (GL_TRUE) but link status is GL_FALSE -> linkProgram
    // returns -1 (Java line 253) -> program == -1 -> isCompiled false (line 195).
    val gl     = new RecordingGL20(linkStatus = GL20.GL_FALSE)
    val shader = makeShader(gl)

    assertEquals(
      shader.compiled,
      false,
      "link status GL_FALSE -> linkProgram returns -1 -> isCompiled false (Java lines 247-253, 193-196)"
    )
    assert(
      shader.log.contains("PROGRAM-LINK-FAILED"),
      s"the program info log must be captured on link failure (Java line 251); got: ${shader.log}"
    )
    // The shaders WERE compiled and the link WAS attempted before the status
    // check failed.
    assert(gl.calls.exists(_.name == "glLinkProgram"), "linking is attempted before the link-status check")
  }

  // --- bind() emits glUseProgram(program) -------------------------------------

  test("ISS561: bind() emits glUseProgram with the linked program handle") {
    val gl     = new RecordingGL20()
    val shader = makeShader(gl)
    gl.calls.clear() // drop construction noise; isolate bind()'s call

    shader.bind()

    assertEquals(
      gl.calls.toList,
      List(Call("glUseProgram", List(ProgramHandle))),
      "bind() must call glUseProgram(program) with the linked handle 20, not 0 or any other value (Java lines 670-674)"
    )
  }

  // --- uniform location cache: glGetUniformLocation fetched exactly ONCE -------

  test("ISS561: setUniformi fetches the uniform location once and caches it; a second call does NOT re-fetch") {
    val gl     = new RecordingGL20()
    val shader = makeShader(gl)
    gl.calls.clear()

    // First setUniformi("u", 7): uncached -> glGetUniformLocation(program, "u")
    // returns 5, cached, then glUniform1i(5, 7) (Java lines 301-307, 319-320).
    shader.setUniformi("u", 7)
    assertEquals(
      gl.calls.toList,
      List(
        Call("glGetUniformLocation", List(ProgramHandle)),
        Call("glUniform1i", List(FixedUniformLocation, 7))
      ),
      "first setUniformi must fetch the location once (glGetUniformLocation) then glUniform1i(location=5, value=7)"
    )

    gl.calls.clear()

    // Second setUniformi("u", 9): the location is already cached -> NO
    // glGetUniformLocation, only glUniform1i(5, 9) (Java line 301 guard:
    // uniforms.get(name,-2) != -2 so the fetch is skipped).
    shader.setUniformi("u", 9)
    assertEquals(
      gl.calls.toList,
      List(Call("glUniform1i", List(FixedUniformLocation, 9))),
      "second setUniformi must reuse the cached location (NO glGetUniformLocation) and glUniform1i(location=5, value=9)"
    )
    assert(
      !gl.calls.exists(_.name == "glGetUniformLocation"),
      "dropping the uniform-location cache would call glGetUniformLocation a second time — this asserts it does not"
    )
  }

  // --- dispose/close emits glDeleteProgram(program) ---------------------------

  test("ISS561: close() emits glUseProgram(0), deletes both shaders, then glDeleteProgram(program) in order") {
    val gl     = new RecordingGL20()
    val shader = makeShader(gl)
    gl.calls.clear()

    shader.close()

    // Java dispose() (port close() lines 767-773): glUseProgram(0);
    // glDeleteShader(vertex); glDeleteShader(fragment); glDeleteProgram(program).
    assertEquals(
      gl.calls.toList,
      List(
        Call("glUseProgram", List(0)),
        Call("glDeleteShader", List(VertexShaderHandle)),
        Call("glDeleteShader", List(FragmentShaderHandle)),
        Call("glDeleteProgram", List(ProgramHandle))
      ),
      "close() must unbind (glUseProgram(0)), delete both shaders, then delete the program 20 in this exact order"
    )
  }
}
