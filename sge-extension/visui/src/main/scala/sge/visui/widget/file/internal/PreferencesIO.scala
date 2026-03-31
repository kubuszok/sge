/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package visui
package widget
package file
package internal

import sge.files.FileHandle
import sge.utils.{ DynamicArray, Nullable }

/** @author Kotcrab */
class PreferencesIO(prefsName: String)(using Sge) {

  private val favoritesKeyName: String      = "favorites"
  private val recentDirKeyName: String      = "recentDirectories"
  private val lastDirKeyName:   String      = "lastDirectory"
  private val prefs:            Preferences = Sge().application.getPreferences(prefsName)

  def this()(using Sge) = this(PreferencesIO.defaultPrefsName)

  checkIfUsingDefaultName()

  def checkIfUsingDefaultName(): Unit =
    if (PreferencesIO.defaultPrefsName == PreferencesIO.VIS_DEFAULT_PREFS_NAME) {
      System.err.println("VisUI: Warning, using default preferences file name for file chooser! (see FileChooser.setDefaultPrefsName(String))") // @nowarn -- simple logging fallback
    }

  def loadFavorites(): DynamicArray[FileHandle] = loadFileArray(favoritesKeyName)

  def saveFavorites(favorites: DynamicArray[FileHandle]): Unit =
    saveFileArray(favoritesKeyName, favorites)

  def loadRecentDirectories(): DynamicArray[FileHandle] = loadFileArray(recentDirKeyName)

  def saveRecentDirectories(recentDirs: DynamicArray[FileHandle]): Unit =
    saveFileArray(recentDirKeyName, recentDirs)

  def loadLastDirectory(): Nullable[FileHandle] = {
    val data = prefs.getString(lastDirKeyName, "") // @nowarn -- empty string as default
    if (data.isEmpty) Nullable.empty
    else {
      val parts = data.split("\\|", 2)
      if (parts.length == 2) Nullable(toFileHandle(parts(0), parts(1)))
      else Nullable.empty
    }
  }

  def saveLastDirectory(file: FileHandle): Unit = {
    prefs.putString(lastDirKeyName, file.fileType.toString + "|" + file.path)
    prefs.flush()
  }

  private def loadFileArray(key: String): DynamicArray[FileHandle] = {
    val data = prefs.getString(key, "") // @nowarn -- empty string as default
    if (data.isEmpty) DynamicArray[FileHandle]()
    else {
      val result  = DynamicArray[FileHandle]()
      val entries = data.split(";;")
      for (entry <- entries) {
        val parts = entry.split("\\|", 2)
        if (parts.length == 2) {
          result.add(toFileHandle(parts(0), parts(1)))
        }
      }
      result
    }
  }

  private def saveFileArray(key: String, files: DynamicArray[FileHandle]): Unit = {
    val sb = new StringBuilder()
    var i  = 0
    while (i < files.size) {
      if (i > 0) sb.append(";;")
      val file = files(i)
      sb.append(file.fileType.toString)
      sb.append("|")
      sb.append(file.path)
      i += 1
    }
    prefs.putString(key, sb.toString())
    prefs.flush()
  }

  private def toFileHandle(typeName: String, path: String): FileHandle =
    typeName match {
      case "Absolute"  => Sge().files.absolute(path)
      case "Classpath" => Sge().files.classpath(path)
      case "External"  => Sge().files.external(path)
      case "Internal"  => Sge().files.internal(path)
      case "Local"     => Sge().files.local(path)
      case _           => Sge().files.absolute(path) // fallback
    }
}

object PreferencesIO {
  private val VIS_DEFAULT_PREFS_NAME: String = "com.kotcrab.vis.ui.widget.file.filechooser_favorites"
  var defaultPrefsName:               String = VIS_DEFAULT_PREFS_NAME

  def setDefaultPrefsName(prefsName: String): Unit = {
    require(prefsName != null, "prefsName can't be null") // @nowarn -- Java interop boundary
    defaultPrefsName = prefsName
  }
}
