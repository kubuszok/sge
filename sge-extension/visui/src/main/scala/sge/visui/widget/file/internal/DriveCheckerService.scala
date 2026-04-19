/*
 * Ported from VisUI - https://github.com/kotcrab/vis-ui
 * Original authors: Kotcrab
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 114
 * Covenant-baseline-methods: DriveCheckerListener,DriveCheckerService,ListenerSet,RootMode,_instance,add,addListener,addListenerInternal,getInstance,list,notifyListeners,pool,processResults,processRoot,readableListeners,readableRoots,rootMode,roots,run,writableListeners,writableRoots
 * Covenant-source-reference: com/kotcrab/vis/ui/widget/file/internal/DriveCheckerService.java
 * Covenant-verified: 2026-04-19
 */
package sge
package visui
package widget
package file
package internal

import java.io.File
import java.util.concurrent.{ ExecutorService, Executors }

import scala.collection.mutable

import sge.utils.DynamicArray

/** Used to check whether file system root is readable or writeable.
  * @author
  *   Kotcrab
  */
class DriveCheckerService(using Sge) {
  private val pool: ExecutorService = Executors.newFixedThreadPool(3, new ServiceThreadFactory("DriveStatusChecker"))

  private val readableRoots: DynamicArray[File] = DynamicArray[File]()
  private val writableRoots: DynamicArray[File] = DynamicArray[File]()

  private val readableListeners: mutable.HashMap[File, DriveCheckerService.ListenerSet] = mutable.HashMap.empty
  private val writableListeners: mutable.HashMap[File, DriveCheckerService.ListenerSet] = mutable.HashMap.empty

  {
    val roots = File.listRoots()
    for (root <- roots) processRoot(root)
  }

  private def processRoot(root: File): Unit =
    pool.execute(new Runnable {
      override def run(): Unit = processResults(root, root.canRead, root.canWrite)
    })

  private def processResults(root: File, readable: Boolean, writable: Boolean): Unit =
    Sge().application.postRunnable(
      new Runnable {
        override def run(): Unit = {
          if (readable) {
            readableRoots.add(root)
            readableListeners.get(root).foreach(_.notifyListeners(root, DriveCheckerService.RootMode.READABLE))
          }
          if (writable) {
            writableRoots.add(root)
            writableListeners.get(root).foreach(_.notifyListeners(root, DriveCheckerService.RootMode.WRITABLE))
          }
        }
      }
    )

  def addListener(root: File, mode: DriveCheckerService.RootMode, listener: DriveCheckerService.DriveCheckerListener): Unit =
    mode match {
      case DriveCheckerService.RootMode.READABLE =>
        addListenerInternal(root, mode, listener, readableRoots, readableListeners)
      case DriveCheckerService.RootMode.WRITABLE =>
        addListenerInternal(root, mode, listener, writableRoots, writableListeners)
    }

  private def addListenerInternal(
    root:        File,
    mode:        DriveCheckerService.RootMode,
    listener:    DriveCheckerService.DriveCheckerListener,
    cachedRoots: DynamicArray[File],
    listeners:   mutable.HashMap[File, DriveCheckerService.ListenerSet]
  ): Unit =
    if (cachedRoots.contains(root)) {
      listener.rootMode(root, mode)
    } else {
      val set = listeners.getOrElseUpdate(root, new DriveCheckerService.ListenerSet())
      set.add(listener)
      processRoot(root)
    }
}

object DriveCheckerService {
  private var _instance: DriveCheckerService = scala.compiletime.uninitialized

  def getInstance(using Sge): DriveCheckerService = synchronized {
    if (_instance == null) _instance = new DriveCheckerService() // @nowarn -- Java interop boundary
    _instance
  }

  enum RootMode {
    case READABLE, WRITABLE
  }

  trait DriveCheckerListener {
    def rootMode(root: File, mode: RootMode): Unit
  }

  class ListenerSet {
    val list: DynamicArray[DriveCheckerListener] = DynamicArray[DriveCheckerListener]()

    def add(listener: DriveCheckerListener): Unit = list.add(listener)

    def notifyListeners(root: File, mode: RootMode): Unit = {
      var i = 0
      while (i < list.size) {
        list(i).rootMode(root, mode)
        i += 1
      }
      list.clear()
    }
  }
}
