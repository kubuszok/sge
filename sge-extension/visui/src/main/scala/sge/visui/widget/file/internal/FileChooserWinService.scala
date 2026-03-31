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

import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.{ InvocationTargetException, Method }
import java.util.concurrent.{ ExecutorService, Executors }

import scala.collection.mutable

import sge.utils.{ DynamicArray, Nullable, ObjectMap }
import sge.visui.util.OsUtils

/** Used to get system drive name. Only used on Windows.
  * @author
  *   Kotcrab
  */
class FileChooserWinService(using Sge) {
  private val nameCache: ObjectMap[File, String]                                  = ObjectMap[File, String]()
  private val listeners: mutable.HashMap[File, FileChooserWinService.ListenerSet] = mutable.HashMap.empty
  private val pool:      ExecutorService                                          = Executors.newFixedThreadPool(3, new ServiceThreadFactory("SystemDisplayNameGetter"))

  private var shellFolderSupported:            Boolean          = false
  private var getShellFolderMethod:            Nullable[Method] = Nullable.empty
  private var getShellFolderDisplayNameMethod: Nullable[Method] = Nullable.empty

  {
    try {
      val shellFolderClass = Class.forName("sun.awt.shell.ShellFolder")
      getShellFolderMethod = Nullable(shellFolderClass.getMethod("getShellFolder", classOf[File]))
      getShellFolderDisplayNameMethod = Nullable(shellFolderClass.getMethod("getDisplayName"))
      shellFolderSupported = true
    } catch {
      case _: ClassNotFoundException => () // ShellFolder not supported on current JVM, ignoring
      case _: NoSuchMethodException  => ()
    }

    val roots = File.listRoots()
    for (root <- roots) processRoot(root)
  }

  private def processRoot(root: File): Unit =
    pool.execute(new Runnable {
      override def run(): Unit = processResult(root, getSystemDisplayName(root))
    })

  private def processResult(root: File, name: Nullable[String]): Unit =
    Sge().application.postRunnable(
      new Runnable {
        override def run(): Unit = {
          if (name.isDefined) nameCache.put(root, name.get)
          else nameCache.put(root, root.toString)

          listeners.get(root).foreach(_.notifyListeners(name))
        }
      }
    )

  def addListener(root: File, listener: FileChooserWinService.RootNameListener): Unit = {
    val cachedName = nameCache.get(root)
    if (cachedName.isDefined) {
      listener.setRootName(cachedName.get)
    } else {
      val set = listeners.getOrElseUpdate(root, new FileChooserWinService.ListenerSet())
      set.add(listener)
      processRoot(root)
    }
  }

  private def getSystemDisplayName(f: File): Nullable[String] =
    if (!shellFolderSupported) Nullable.empty
    else {
      try {
        val shellFolder = getShellFolderMethod.get.invoke(null, f) // @nowarn -- Java interop boundary
        var name        = getShellFolderDisplayNameMethod.get.invoke(shellFolder).asInstanceOf[String]
        if (name == null || name.isEmpty) name = f.getPath // @nowarn -- Java interop boundary
        Nullable(name)
      } catch {
        case _: InvocationTargetException => Nullable.empty
        case _: IllegalAccessException    => Nullable.empty
      }
    }
}

object FileChooserWinService {
  private var _instance: Nullable[FileChooserWinService] = Nullable.empty

  def getInstance(using Sge): Nullable[FileChooserWinService] = synchronized {
    if (!OsUtils.isWindows) Nullable.empty
    else {
      if (_instance.isEmpty) _instance = Nullable(new FileChooserWinService())
      _instance
    }
  }

  trait RootNameListener {
    def setRootName(newName: String): Unit
  }

  private class ListenerSet {
    val list: DynamicArray[WeakReference[RootNameListener]] = DynamicArray[WeakReference[RootNameListener]]()

    def add(listener: RootNameListener): Unit = list.add(new WeakReference[RootNameListener](listener))

    def notifyListeners(newName: Nullable[String]): Unit = {
      var i = list.size - 1
      while (i >= 0) {
        val listener = list(i).get() // @nowarn -- Java interop boundary, weak reference
        if (listener == null) { // @nowarn -- Java interop boundary, weak reference
          list.removeIndex(i)
        } else {
          listener.setRootName(if (newName.isDefined) newName.get else "")
        }
        i -= 1
      }
    }
  }
}
