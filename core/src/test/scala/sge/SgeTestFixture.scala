/*
 * SGE - Scala Game Engine
 * Copyright 2024-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 */
package sge

import sge.noop.{ NoopAudio, NoopGraphics, NoopInput }

/** Provides a minimal [[Sge]] context for unit tests that need one (e.g. Camera, ModelLoader). */
object SgeTestFixture {

  /** Creates a test [[Sge]] with noop implementations for all subsystems. Override individual parameters to inject custom implementations for testing.
    */
  def testSge(
    application: Application = NoopApplication,
    graphics:    Graphics = new NoopGraphics(),
    audio:       Audio = new NoopAudio(),
    files:       Files = NoopFiles,
    input:       Input = new NoopInput(),
    net:         Net = NoopNet
  ): Sge = Sge(application, graphics, audio, files, input, net)

  private object NoopApplication extends Application {
    def getApplicationListener():                                                               ApplicationListener         = throw new UnsupportedOperationException
    def getGraphics():                                                                          Graphics                    = throw new UnsupportedOperationException
    def getAudio():                                                                             Audio                       = throw new UnsupportedOperationException
    def getInput():                                                                             Input                       = throw new UnsupportedOperationException
    def getFiles():                                                                             Files                       = throw new UnsupportedOperationException
    def getNet():                                                                               Net                         = throw new UnsupportedOperationException
    def log(tag:                                String, message: String):                       Unit                        = ()
    def log(tag:                                String, message: String, exception: Throwable): Unit                        = ()
    def error(tag:                              String, message: String):                       Unit                        = ()
    def error(tag:                              String, message: String, exception: Throwable): Unit                        = ()
    def debug(tag:                              String, message: String):                       Unit                        = ()
    def debug(tag:                              String, message: String, exception: Throwable): Unit                        = ()
    def setLogLevel(logLevel:                   Int):                                           Unit                        = ()
    def getLogLevel():                                                                          Int                         = Application.LOG_NONE
    def setApplicationLogger(applicationLogger: ApplicationLogger):                             Unit                        = ()
    def getApplicationLogger():                                                                 ApplicationLogger           = throw new UnsupportedOperationException
    def getType():                                                                              Application.ApplicationType = Application.ApplicationType.HeadlessDesktop
    def getVersion():                                                                           Int                         = 0
    def getJavaHeap():                                                                          Long                        = 0L
    def getNativeHeap():                                                                        Long                        = 0L
    def getPreferences(name:                    String):                                        Preferences                 = throw new UnsupportedOperationException
    def getClipboard():                                                                         sge.utils.Clipboard         = throw new UnsupportedOperationException
    def postRunnable(runnable:                  Runnable):                                      Unit                        = ()
    def exit():                                                                                 Unit                        = ()
    def addLifecycleListener(listener:          LifecycleListener):                             Unit                        = ()
    def removeLifecycleListener(listener:       LifecycleListener):                             Unit                        = ()
  }

  private object NoopFiles extends Files {
    def getFileHandle(path: String, fileType: files.FileType): files.FileHandle = throw new UnsupportedOperationException
    def classpath(path:     String):                           files.FileHandle = throw new UnsupportedOperationException
    def internal(path:      String):                           files.FileHandle = throw new UnsupportedOperationException
    def external(path:      String):                           files.FileHandle = throw new UnsupportedOperationException
    def absolute(path:      String):                           files.FileHandle = throw new UnsupportedOperationException
    def local(path:         String):                           files.FileHandle = throw new UnsupportedOperationException
    def getExternalStoragePath:                                String           = ""
    def isExternalStorageAvailable:                            Boolean          = false
    def getLocalStoragePath:                                   String           = ""
    def isLocalStorageAvailable:                               Boolean          = false
  }

  private object NoopNet extends Net {
    import Net._
    def httpClient:                                                                                     net.SgeHttpClient = net.SgeHttpClient.noop()
    def newServerSocket(protocol: Protocol, hostname: String, port: Int, hints: net.ServerSocketHints): net.ServerSocket  = throw new UnsupportedOperationException
    def newServerSocket(protocol: Protocol, port:     Int, hints:   net.ServerSocketHints):             net.ServerSocket  = throw new UnsupportedOperationException
    def newClientSocket(protocol: Protocol, host:     String, port: Int, hints: net.SocketHints):       net.Socket        = throw new UnsupportedOperationException
    def openURI(URI:              String):                                                              Boolean           = false
  }
}
