/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/GLVersion.java
 * Original authors: See AUTHORS file
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Type enum moved to companion; pattern match instead of if-else chain; TAG field removed
 *   Idiom: split packages
 *   Idiom: Gdx.app.log/error -> scribe.info/error; NONE case retains original constructor args instead of empty strings
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import java.util.regex.Pattern

import scribe._

import sge.Application

class GLVersion(
  appType:        Application.ApplicationType,
  versionString:  String,
  vendorString:   String,
  rendererString: String
) {

  private var majorVersion:   Int = scala.compiletime.uninitialized
  private var minorVersion:   Int = scala.compiletime.uninitialized
  private var releaseVersion: Int = scala.compiletime.uninitialized

  private val versionStringVal:  String = versionString
  private val vendorStringVal:   String = vendorString
  private val rendererStringVal: String = rendererString

  private val `type`: GLVersion.Type = appType match {
    case Application.ApplicationType.Android => GLVersion.Type.GLES
    case Application.ApplicationType.iOS     => GLVersion.Type.GLES
    case Application.ApplicationType.Desktop => GLVersion.Type.OpenGL
    case Application.ApplicationType.Applet  => GLVersion.Type.OpenGL
    case Application.ApplicationType.WebGL   => GLVersion.Type.WebGL
    case _                                   => GLVersion.Type.NONE
  }

  // Initialize version numbers based on type
  `type` match {
    case GLVersion.Type.GLES =>
      // OpenGL<space>ES<space><version number><space><vendor-specific information>.
      extractVersion("OpenGL ES (\\d(\\.\\d){0,2})", versionString)
    case GLVersion.Type.WebGL =>
      // WebGL<space><version number><space><vendor-specific information>
      extractVersion("WebGL (\\d(\\.\\d){0,2})", versionString)
    case GLVersion.Type.OpenGL =>
      // <version number><space><vendor-specific information>
      extractVersion("(\\d(\\.\\d){0,2})", versionString)
    case _ =>
      majorVersion = -1
      minorVersion = -1
      releaseVersion = -1
  }

  private def extractVersion(patternString: String, versionString: String): Unit = {
    val pattern = Pattern.compile(patternString)
    val matcher = pattern.matcher(versionString)
    val found   = matcher.find()
    if (found) {
      val result      = matcher.group(1)
      val resultSplit = result.split("\\.")
      majorVersion = parseInt(resultSplit(0), 2)
      minorVersion = if (resultSplit.length < 2) 0 else parseInt(resultSplit(1), 0)
      releaseVersion = if (resultSplit.length < 3) 0 else parseInt(resultSplit(2), 0)
    } else {
      scribe.info(s"Invalid version string: $versionString")
      majorVersion = 2
      minorVersion = 0
      releaseVersion = 0
    }
  }

  /** Forgiving parsing of gl major, minor and release versions as some manufacturers don't adhere to spec * */
  private def parseInt(v: String, defaultValue: Int): Int =
    try
      Integer.parseInt(v)
    catch {
      case _: NumberFormatException =>
        scribe.error(s"Error parsing number: $v, assuming: $defaultValue")
        defaultValue
    }

  /** @return
    *   what {@link Type} of GL implementation this application has access to, e.g. {@link Type#OpenGL} or {@link Type#GLES}
    */
  def getType(): GLVersion.Type = `type`

  /** @return the major version of current GL connection. -1 if running headless */
  def getMajorVersion(): Int = majorVersion

  /** @return the minor version of the current GL connection. -1 if running headless */
  def getMinorVersion(): Int = minorVersion

  /** @return the release version of the current GL connection. -1 if running headless */
  def getReleaseVersion(): Int = releaseVersion

  /** @return The version string as reported by `glGetString(GL_VERSION)` */
  def getVersionString(): String = versionStringVal

  /** @return the vendor string associated with the current GL connection */
  def getVendorString(): String = vendorStringVal

  /** @return
    *   the name of the renderer associated with the current GL connection. This name is typically specific to a particular configuration of a hardware platform.
    */
  def getRendererString(): String = rendererStringVal

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
    majorVersion > testMajorVersion || (majorVersion == testMajorVersion && minorVersion >= testMinorVersion)

  /** @return a string with the current GL connection data */
  def getDebugVersionString(): String =
    s"Type: ${`type`}\nVersion: $majorVersion:$minorVersion:$releaseVersion\nVendor: $vendorStringVal\nRenderer: $rendererStringVal"
}

object GLVersion {
  enum Type {
    case OpenGL, GLES, WebGL, NONE
  }
}
