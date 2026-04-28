/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/GLVersion.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Type enum moved to companion; pattern match instead of if-else chain; TAG field removed
 *   Idiom: split packages
 *   Idiom: Gdx.app.log -> Log.info, Gdx.app.error -> Log.error; NONE case sets vendorString/rendererString to "" as Java does
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 135
 * Covenant-baseline-methods: GLVersion,Type,_majorVersion,_minorVersion,_releaseVersion,_rendererString,_type,_vendorString,_versionString,debugVersionString,extractVersion,found,glType,isVersionEqualToOrHigher,majorVersion,matcher,minorVersion,parseInt,pattern,releaseVersion,rendererString,vendorString,versionString
 * Covenant-source-reference: com/badlogic/gdx/graphics/glutils/GLVersion.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 8c73ac5833142bf408f9b6f979b1672efb89fa36
 */
package sge
package graphics
package glutils

import java.util.regex.Pattern

import sge.Application

class GLVersion(
  appType:         Application.ApplicationType,
  initialVersion:  String,
  initialVendor:   String,
  initialRenderer: String
) {

  private var _majorVersion:   Int = scala.compiletime.uninitialized
  private var _minorVersion:   Int = scala.compiletime.uninitialized
  private var _releaseVersion: Int = scala.compiletime.uninitialized

  private val _versionString: String = initialVersion

  private val _type: GLVersion.Type = appType match {
    case Application.ApplicationType.Android => GLVersion.Type.GLES
    case Application.ApplicationType.iOS     => GLVersion.Type.GLES
    case Application.ApplicationType.Desktop => GLVersion.Type.OpenGL
    case Application.ApplicationType.Applet  => GLVersion.Type.OpenGL
    case Application.ApplicationType.WebGL   => GLVersion.Type.WebGL
    case _                                   => GLVersion.Type.NONE
  }

  // Initialize version numbers based on type; NONE case overrides vendor/renderer to "" as Java does
  private val _vendorString:   String = if (_type == GLVersion.Type.NONE) "" else initialVendor
  private val _rendererString: String = if (_type == GLVersion.Type.NONE) "" else initialRenderer

  _type match {
    case GLVersion.Type.GLES =>
      // OpenGL<space>ES<space><version number><space><vendor-specific information>.
      extractVersion("OpenGL ES (\\d(\\.\\d){0,2})", initialVersion)
    case GLVersion.Type.WebGL =>
      // WebGL<space><version number><space><vendor-specific information>
      extractVersion("WebGL (\\d(\\.\\d){0,2})", initialVersion)
    case GLVersion.Type.OpenGL =>
      // <version number><space><vendor-specific information>
      extractVersion("(\\d(\\.\\d){0,2})", initialVersion)
    case _ =>
      _majorVersion = -1
      _minorVersion = -1
      _releaseVersion = -1
  }

  private def extractVersion(patternString: String, versionString: String): Unit = {
    val pattern = Pattern.compile(patternString)
    val matcher = pattern.matcher(versionString)
    val found   = matcher.find()
    if (found) {
      val result      = matcher.group(1)
      val resultSplit = result.split("\\.")
      _majorVersion = parseInt(resultSplit(0), 2)
      _minorVersion = if (resultSplit.length < 2) 0 else parseInt(resultSplit(1), 0)
      _releaseVersion = if (resultSplit.length < 3) 0 else parseInt(resultSplit(2), 0)
    } else {
      utils.Log.info(s"Invalid version string: $versionString")
      _majorVersion = 2
      _minorVersion = 0
      _releaseVersion = 0
    }
  }

  /** Forgiving parsing of gl major, minor and release versions as some manufacturers don't adhere to spec * */
  private def parseInt(v: String, defaultValue: Int): Int =
    try
      Integer.parseInt(v)
    catch {
      case _: NumberFormatException =>
        utils.Log.error(s"Error parsing number: $v, assuming: $defaultValue")
        defaultValue
    }

  /** What {@link Type} of GL implementation this application has access to, e.g. {@link Type#OpenGL} or {@link Type#GLES} */
  def glType: GLVersion.Type = _type

  /** The major version of current GL connection. -1 if running headless */
  def majorVersion: Int = _majorVersion

  /** The minor version of the current GL connection. -1 if running headless */
  def minorVersion: Int = _minorVersion

  /** The release version of the current GL connection. -1 if running headless */
  def releaseVersion: Int = _releaseVersion

  /** The version string as reported by `glGetString(GL_VERSION)` */
  def versionString: String = _versionString

  /** The vendor string associated with the current GL connection */
  def vendorString: String = _vendorString

  /** The name of the renderer associated with the current GL connection. This name is typically specific to a particular configuration of a hardware platform. */
  def rendererString: String = _rendererString

  /** Checks to see if the current GL connection version is higher, or equal to the provided test versions.
    *
    * @param testMajorVersion
    *   the major version to test against
    * @param testMinorVersion
    *   the minor version to test against
    * @return
    *   true if the current version is higher or equal to the test version
    */
  def isVersionEqualToOrHigher(testMajorVersion: Int, testMinorVersion: Int): Boolean =
    _majorVersion > testMajorVersion || (_majorVersion == testMajorVersion && _minorVersion >= testMinorVersion)

  /** A string with the current GL connection data */
  def debugVersionString: String =
    s"Type: ${_type}\nVersion: ${_majorVersion}:${_minorVersion}:${_releaseVersion}\nVendor: $_vendorString\nRenderer: $_rendererString"
}

object GLVersion {
  enum Type {
    case OpenGL, GLES, WebGL, NONE
  }
}
