/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/Application.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java interface -> Scala trait; ApplicationType uses Scala 3 enum
 *   Idiom: split packages; logging decoupled to scribe library (was Application.log/error/debug)
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 136
 * Covenant-baseline-methods: Application,ApplicationType,addLifecycleListener,applicationListener,applicationType,audio,clipboard,exit,files,getPreferences,graphics,input,javaHeap,nativeHeap,net,postRunnable,removeLifecycleListener,version
 * Covenant-source-reference: com/badlogic/gdx/Application.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 7263ab22efbda67e02d7180e9b4dfeb01160c061
 */
package sge

import sge.utils.Clipboard

/** <p> An <code>Application</code> is the main entry point of your project. It sets up a window and rendering surface and manages the different aspects of your application, namely {@link Graphics} ,
  * {@link Audio} , {@link Input} and {@link Files} . Think of an Application being equivalent to Swing's <code>JFrame</code> or Android's <code>Activity</code>. </p>
  *
  * <p> An application can be an instance of any of the following: <ul> <li>a desktop application (see <code>JglfwApplication</code> found in gdx-backends-jglfw.jar)</li> <li>an Android application
  * (see <code>AndroidApplication</code> found in gdx-backends-android.jar)</li> <li>a HTML5 application (see <code>GwtApplication</code> found in gdx-backends-gwt.jar)</li> <li>an iOS application
  * (see <code>IOSApplication</code> found in gdx-backends-robovm.jar)</li> </ul> Each application class has it's own startup and initialization methods. Please refer to their documentation for more
  * information. </p>
  *
  * <p> While game programmers are used to having a main loop, libGDX employs a different concept to accommodate the event based nature of Android applications a little more. You application logic
  * must be implemented in a {@link ApplicationListener} which has methods that get called by the Application when the application is created, resumed, paused, disposed or rendered. As a developer you
  * will simply implement the ApplicationListener interface and fill in the functionality accordingly. The ApplicationListener is provided to a concrete Application instance as a parameter to the
  * constructor or another initialization method. Please refer to the documentation of the Application implementations for more information. Note that the ApplicationListener can be provided to any
  * Application implementation. This means that you only need to write your program logic once and have it run on different platforms by passing it to a concrete Application implementation. </p>
  *
  * <p> The Application interface provides you with a set of modules for graphics, audio, input and file i/o. </p>
  *
  * <p> {@link Graphics} offers you various methods to output visuals to the screen. This is achieved via OpenGL ES 2.0 or 3.0 depending on what's available an the platform. On the desktop the
  * features of OpenGL ES 2.0 and 3.0 are emulated via desktop OpenGL. On Android the functionality of the Java OpenGL ES bindings is used. </p>
  *
  * <p> {@link Audio} offers you various methods to output and record sound and music. This is achieved via the Java Sound API on the desktop. On Android the Android media framework is used. </p>
  *
  * <p> {@link Input} offers you various methods to poll user input from the keyboard, touch screen, mouse and accelerometer. Additionally you can implement an {@link InputProcessor} and use it with
  * {@link Input#setInputProcessor(InputProcessor)} to receive input events. </p>
  *
  * <p> {@link Files} offers you various methods to access internal and external files. An internal file is a file that is stored near your application. On Android internal files are equivalent to
  * assets. On the desktop the classpath is first scanned for the specified file. If that fails then the root directory of your application is used for a look up. External files are resources you
  * create in your application and write to an external storage. On Android external files reside on the SD-card, on the desktop external files are written to a users home directory. If you know what
  * you are doing you can also specify absolute file names. Absolute filenames are not portable, so take great care when using this feature. </p>
  *
  * <p> {@link Net} offers you various methods to perform network operations, such as performing HTTP requests, or creating server and client sockets for more elaborate network programming. </p>
  *
  * <p> The <code>Application</code> also has a set of methods that you can use to query specific information such as the operating system the application is currently running on and so forth. This
  * allows you to have operating system dependent code paths. It is however not recommended to use these facilities. </p>
  *
  * <p> The <code>Application</code> also has a simple logging method which will print to standard out on the desktop and to logcat on Android. </p>
  *
  * @author
  *   mzechner
  */
trait Application {

  /** @return the {@link ApplicationListener} instance */
  def applicationListener: ApplicationListener

  /** @return the {@link Graphics} instance */
  def graphics: Graphics

  /** @return the {@link Audio} instance */
  def audio: Audio

  /** @return the {@link Input} instance */
  def input: Input

  /** @return the {@link Files} instance */
  def files: Files

  /** @return the {@link Net} instance */
  def net: Net

  /** @return what {@link ApplicationType} this application has, e.g. Android or Desktop */
  def applicationType: Application.ApplicationType

  /** @return the Android API level on Android, the major OS version on iOS (5, 6, 7, ..), or 0 on the desktop. */
  def version: Int

  /** @return the Java heap memory use in bytes */
  def javaHeap: Long

  /** @return the Native heap memory use in bytes */
  def nativeHeap: Long

  /** Returns the {@link Preferences} instance of this Application. It can be used to store application settings across runs.
    * @param name
    *   the name of the preferences, must be useable as a file name.
    * @return
    *   the preferences.
    */
  def getPreferences(name: String): Preferences

  def clipboard: Clipboard

  /** Posts a {@link Runnable} on the main loop thread.
    *
    * In a multi-window application, the {@linkplain Gdx#graphics} and {@linkplain Gdx#input} values may be unpredictable at the time the Runnable is executed. If graphics or input are needed, they
    * can be copied to a variable to be used in the Runnable. For example: <p> <code> final Graphics graphics = Gdx.graphics;
    *
    * @param runnable
    *   the runnable.
    */
  def postRunnable(runnable: Runnable): Unit

  /** Schedule an exit from the application. On android, this will cause a call to pause() and dispose() some time in the future, it will not immediately finish your application. On iOS this should be
    * avoided in production as it breaks Apples guidelines
    */
  def exit(): Unit

  /** Adds a new {@link LifecycleListener} to the application. This can be used by extensions to hook into the lifecycle more easily. The {@link ApplicationListener} methods are sufficient for
    * application level development.
    * @param listener
    */
  def addLifecycleListener(listener: LifecycleListener): Unit

  /** Removes the {@link LifecycleListener} .
    * @param listener
    */
  def removeLifecycleListener(listener: LifecycleListener): Unit
}

object Application {

  /** Enumeration of possible {@link Application} types
    *
    * @author
    *   mzechner
    */
  enum ApplicationType {
    case Android, Desktop, HeadlessDesktop, Applet, WebGL, iOS
  }
}
