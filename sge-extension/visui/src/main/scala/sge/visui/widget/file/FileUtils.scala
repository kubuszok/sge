/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 144
 * Covenant-baseline-methods: FILE_MODIFIED_DATE_COMPARATOR,FILE_NAME_COMPARATOR,FILE_SIZE_COMPARATOR,FileUtils,UNITS,compare,dirToShow,directoriesList,filesList,isValidFileName,ordering,readableFileSize,showDirInExplorer,sortFiles,toFileHandle
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/file/FileUtils.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 820300c86a1bd907404217195a9987e5c66d2220
 */
package sge
package visui
package widget
package file

import java.io.File
import java.text.DecimalFormat
import java.util.Comparator

import sge.files.FileHandle
import sge.utils.DynamicArray
import sge.visui.util.OsUtils

/** File related utils. Note that FileUtils are not available on GWT.
  * @author
  *   Kotcrab
  */
object FileUtils {

  private val UNITS: Array[String] = Array("B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB")

  /** Sorts file by names ignoring upper case */
  val FILE_NAME_COMPARATOR: Comparator[FileHandle] = new Comparator[FileHandle] {
    override def compare(f1: FileHandle, f2: FileHandle): Int =
      f1.name.toLowerCase.compareTo(f2.name.toLowerCase)
  }

  /** Sorts file by modified date then by name. */
  val FILE_MODIFIED_DATE_COMPARATOR: Comparator[FileHandle] = new Comparator[FileHandle] {
    override def compare(f1: FileHandle, f2: FileHandle): Int = {
      val l1 = f1.lastModified()
      val l2 = f2.lastModified()
      if (l1 > l2) 1 else if (l1 == l2) FILE_NAME_COMPARATOR.compare(f1, f2) else -1
    }
  }

  /** Sorts file by their size then by name. */
  val FILE_SIZE_COMPARATOR: Comparator[FileHandle] = new Comparator[FileHandle] {
    override def compare(f1: FileHandle, f2: FileHandle): Int = {
      val l1 = f1.length()
      val l2 = f2.length()
      if (l1 > l2) -1 else if (l1 == l2) FILE_NAME_COMPARATOR.compare(f1, f2) else 1
    }
  }

  /** Converts byte file size to human readable, eg: 500 becomes 500 B, 1024 becomes 1 KB. Max supported unit is yottabyte (YB).
    * @param size
    *   file size in bytes.
    * @return
    *   human readable file size.
    */
  def readableFileSize(size: Long): String =
    if (size <= 0) "0 B"
    else {
      val digitGroups = (Math.log10(size.toDouble) / Math.log10(1024)).toInt
      new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)).replace(",", ".") + " " + UNITS(digitGroups)
    }

  /** Sorts file list, using this rules: directories first, sorted by names ignoring uppercase, then files sorted by names ignoring uppercase. */
  def sortFiles(files: scala.Array[FileHandle]): DynamicArray[FileHandle] =
    sortFiles(files, FILE_NAME_COMPARATOR)

  /** Sorts file list, using this rules: directories first, sorted using provided comparator, then files sorted using provided comparator. */
  def sortFiles(files: scala.Array[FileHandle], comparator: Comparator[FileHandle]): DynamicArray[FileHandle] =
    sortFiles(files, comparator, descending = false)

  /** Sorts file list, using this rules: directories first, sorted using provided comparator, then files sorted using provided comparator. */
  def sortFiles(files: scala.Array[FileHandle], comparator: Comparator[FileHandle], descending: Boolean): DynamicArray[FileHandle] = {
    val directoriesList = DynamicArray[FileHandle]()
    val filesList       = DynamicArray[FileHandle]()

    for (f <- files)
      if (f.isDirectory()) directoriesList.add(f)
      else filesList.add(f)

    val ordering = Ordering.comparatorToOrdering(using comparator)
    directoriesList.sort(ordering)
    filesList.sort(ordering)

    if (descending) {
      directoriesList.reverse()
      filesList.reverse()
    }

    directoriesList.addAll(filesList) // combine lists
    directoriesList
  }

  /** Checks whether given name is valid for current user OS. */
  def isValidFileName(name: String): Boolean =
    try {
      var n = name
      if (OsUtils.isWindows) {
        if (n.contains(">") || n.contains("<")) false
        else {
          n = n.toLowerCase // Windows is case insensitive
          new File(n).getCanonicalFile.getName.equals(n)
        }
      } else {
        new File(n).getCanonicalFile.getName.equals(n)
      }
    } catch {
      case _: Exception => false
    }

  /** Converts [[File]] to absolute [[FileHandle]]. */
  def toFileHandle(file: File)(using Sge): FileHandle =
    Sge().files.absolute(file.getAbsolutePath)

  /** Shows given directory in system explorer window. */
  def showDirInExplorer(dir: FileHandle): Unit = {
    val dirToShow = if (dir.isDirectory()) dir.file else dir.parent().file

    try {
      // Using reflection to avoid importing AWT desktop which would trigger Android Lint errors
      // This is desktop only, rarely called, performance drop is negligible
      val desktopClass = Class.forName("java.awt.Desktop")
      val desktop      = desktopClass.getMethod("getDesktop").invoke(null) // @nowarn -- Java interop boundary
      try
        // browseFileDirectory was introduced in JDK 9
        desktopClass.getMethod("browseFileDirectory", classOf[File]).invoke(desktop, dirToShow)
      catch {
        case e: (NoSuchMethodException | java.lang.reflect.InvocationTargetException) =>
          // browseFileDirectory throws UnsupportedOperationException on some platforms
          e match {
            case ite: java.lang.reflect.InvocationTargetException if !ite.getCause.isInstanceOf[UnsupportedOperationException] =>
              throw ite
            case _ => ()
          }
          desktopClass.getMethod("open", classOf[File]).invoke(desktop, dirToShow)
      }
    } catch {
      case e: Exception =>
        System.err.println("VisUI: Can't open file " + dirToShow.getPath + ": " + e.getMessage) // @nowarn -- fallback logging
    }
  }
}
