/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/tools/texturepacker/TexturePackerFileProcessor.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: com.badlogic.gdx.tools -> sge.tools
 *   Convention: Java collections -> Scala collections
 *   Idiom: Scala 3 enums, boundary/break for control flow, kindlings Json AST for settings parsing
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 391
 * Covenant-baseline-methods: TexturePackerFileProcessor,atlasExtension,atlasExtensionQ,countOnly,defaultSettings,deleteOutput,dirToSettings,i,ignoreDirs,merge,n,newSettings,newTexturePacker,pack,packCount,packFileName,packer,process,processDir,processFile,processFiles,progress,result,root,rootSettings,settingsFile,settingsFiles,settingsProcessor,this
 * Covenant-source-reference: com/badlogic/gdx/tools/texturepacker/TexturePackerFileProcessor.java
 * Covenant-verified: 2026-04-19
 */
package sge
package tools
package texturepacker

import sge.utils.{ Json, readFromStream }
import sge.utils.given
import sge.graphics.Pixmap.Format
import sge.graphics.Texture.{ TextureFilter, TextureWrap }
import sge.tools.texturepacker.TexturePacker.{ ProgressListener, Resampling, Settings }
import sge.utils.Nullable

import java.io.{ File, FileInputStream, IOException }
import java.util.regex.{ Matcher, Pattern }
import scala.collection.mutable.{ ArrayBuffer, HashMap }
import scala.util.boundary
import scala.util.boundary.break

/** @author
  *   Nathan Sweet
  */
class TexturePackerFileProcessor(
  val defaultSettings: Settings,
  packFileNameArg:     String,
  val progress:        Nullable[ProgressListener]
) extends FileProcessor {

  private val dirToSettings: HashMap[File, Settings] = HashMap.empty
  private var packFileName:  String                  = packFileNameArg
  private var root:          Nullable[File]          = Nullable.empty
  var ignoreDirs:            ArrayBuffer[File]       = ArrayBuffer.empty
  var countOnly:             Boolean                 = false
  var packCount:             Int                     = 0

  {
    if (packFileName.toLowerCase().endsWith(defaultSettings.atlasExtension.toLowerCase()))
      packFileName = packFileName.substring(0, packFileName.length - defaultSettings.atlasExtension.length)

    setFlattenOutput(true)
    addInputSuffix(".png", ".jpg", ".jpeg")

    // Sort input files by name to avoid platform-dependent atlas output changes.
    setComparator(Ordering.by[File, String](_.getName()))
  }

  def this() =
    this(Settings(), "pack.atlas", Nullable.empty)

  override def process(inputFile: File, outputRoot: File): ArrayBuffer[FileProcessor.Entry] = {
    root = Nullable(inputFile)

    // Collect pack.json setting files.
    val settingsFiles     = ArrayBuffer.empty[File]
    val settingsProcessor = new FileProcessor() {
      override protected def processFile(inputFile: FileProcessor.Entry): Unit =
        inputFile.inputFile.foreach(f => settingsFiles += f)
    }
    settingsProcessor.addInputRegex("pack\\.json")
    settingsProcessor.process(inputFile, null) // @nowarn("msg=deprecated") null outputRoot when no output needed
    // Sort parent first.
    settingsFiles.sortInPlace()(using Ordering.by(_.toString.length))
    for (settingsFile <- settingsFiles) {
      // Find first parent with settings, or use defaults.
      var settings: Nullable[Settings] = Nullable.empty
      var parent = settingsFile.getParentFile()
      boundary {
        while (true) {
          if (parent.equals(root.get)) break()
          parent = parent.getParentFile()
          dirToSettings.get(parent).foreach { s =>
            settings = Nullable(newSettings(s))
            break()
          }
        }
      }
      if (settings.isEmpty) settings = Nullable(newSettings(defaultSettings))
      // Merge settings from current directory.
      merge(settings.get, settingsFile)
      dirToSettings.put(settingsFile.getParentFile(), settings.get)
    }

    // Count the number of texture packer invocations.
    countOnly = true
    super.process(inputFile, outputRoot)
    countOnly = false

    // Do actual processing.
    progress.foreach(_.start(1))
    val result = super.process(inputFile, outputRoot)
    progress.foreach(_.end())
    result
  }

  def merge(settings: Settings, settingsFile: File): Unit =
    try {
      val stream = new FileInputStream(settingsFile)
      try {
        val json = readFromStream[Json](stream)
        json match {
          case Json.Obj(obj) =>
            val fields = obj.fields.toMap
            fields.get("pot").foreach { case Json.Bool(v) => settings.pot = v; case _ => }
            fields.get("multipleOfFour").foreach { case Json.Bool(v) => settings.multipleOfFour = v; case _ => }
            fields.get("paddingX").foreach { case Json.Num(v) => settings.paddingX = v.toDouble.map(_.toInt).getOrElse(0); case _ => }
            fields.get("paddingY").foreach { case Json.Num(v) => settings.paddingY = v.toDouble.map(_.toInt).getOrElse(0); case _ => }
            fields.get("edgePadding").foreach { case Json.Bool(v) => settings.edgePadding = v; case _ => }
            fields.get("duplicatePadding").foreach { case Json.Bool(v) => settings.duplicatePadding = v; case _ => }
            fields.get("rotation").foreach { case Json.Bool(v) => settings.rotation = v; case _ => }
            fields.get("minWidth").foreach { case Json.Num(v) => settings.minWidth = v.toDouble.map(_.toInt).getOrElse(0); case _ => }
            fields.get("minHeight").foreach { case Json.Num(v) => settings.minHeight = v.toDouble.map(_.toInt).getOrElse(0); case _ => }
            fields.get("maxWidth").foreach { case Json.Num(v) => settings.maxWidth = v.toDouble.map(_.toInt).getOrElse(0); case _ => }
            fields.get("maxHeight").foreach { case Json.Num(v) => settings.maxHeight = v.toDouble.map(_.toInt).getOrElse(0); case _ => }
            fields.get("square").foreach { case Json.Bool(v) => settings.square = v; case _ => }
            fields.get("stripWhitespaceX").foreach { case Json.Bool(v) => settings.stripWhitespaceX = v; case _ => }
            fields.get("stripWhitespaceY").foreach { case Json.Bool(v) => settings.stripWhitespaceY = v; case _ => }
            fields.get("alphaThreshold").foreach { case Json.Num(v) => settings.alphaThreshold = v.toDouble.map(_.toInt).getOrElse(0); case _ => }
            fields.get("filterMin").foreach {
              case Json.Str(v) =>
                try settings.filterMin = TextureFilter.valueOf(v)
                catch { case _: Exception => }
              case _ =>
            }
            fields.get("filterMag").foreach {
              case Json.Str(v) =>
                try settings.filterMag = TextureFilter.valueOf(v)
                catch { case _: Exception => }
              case _ =>
            }
            fields.get("wrapX").foreach {
              case Json.Str(v) =>
                try settings.wrapX = TextureWrap.valueOf(v)
                catch { case _: Exception => }
              case _ =>
            }
            fields.get("wrapY").foreach {
              case Json.Str(v) =>
                try settings.wrapY = TextureWrap.valueOf(v)
                catch { case _: Exception => }
              case _ =>
            }
            fields.get("format").foreach {
              case Json.Str(v) =>
                try settings.format = Format.valueOf(v)
                catch { case _: Exception => }
              case _ =>
            }
            fields.get("alias").foreach { case Json.Bool(v) => settings.alias = v; case _ => }
            fields.get("outputFormat").foreach { case Json.Str(v) => settings.outputFormat = v; case _ => }
            fields.get("jpegQuality").foreach { case Json.Num(v) => settings.jpegQuality = v.toDouble.map(_.toFloat).getOrElse(0f); case _ => }
            fields.get("ignoreBlankImages").foreach { case Json.Bool(v) => settings.ignoreBlankImages = v; case _ => }
            fields.get("fast").foreach { case Json.Bool(v) => settings.fast = v; case _ => }
            fields.get("debug").foreach { case Json.Bool(v) => settings.debug = v; case _ => }
            fields.get("silent").foreach { case Json.Bool(v) => settings.silent = v; case _ => }
            fields.get("combineSubdirectories").foreach { case Json.Bool(v) => settings.combineSubdirectories = v; case _ => }
            fields.get("ignore").foreach { case Json.Bool(v) => settings.ignore = v; case _ => }
            fields.get("flattenPaths").foreach { case Json.Bool(v) => settings.flattenPaths = v; case _ => }
            fields.get("premultiplyAlpha").foreach { case Json.Bool(v) => settings.premultiplyAlpha = v; case _ => }
            fields.get("useIndexes").foreach { case Json.Bool(v) => settings.useIndexes = v; case _ => }
            fields.get("bleed").foreach { case Json.Bool(v) => settings.bleed = v; case _ => }
            fields.get("bleedIterations").foreach { case Json.Num(v) => settings.bleedIterations = v.toDouble.map(_.toInt).getOrElse(0); case _ => }
            fields.get("limitMemory").foreach { case Json.Bool(v) => settings.limitMemory = v; case _ => }
            fields.get("grid").foreach { case Json.Bool(v) => settings.grid = v; case _ => }
            fields.get("scale").foreach {
              case Json.Arr(vs) =>
                settings.scale = vs.collect { case Json.Num(v) => v.toDouble.map(_.toFloat).getOrElse(0f) }.toArray
              case _ =>
            }
            fields.get("scaleSuffix").foreach {
              case Json.Arr(vs) =>
                settings.scaleSuffix = vs.collect { case Json.Str(v) => v }.toArray
              case _ =>
            }
            fields.get("scaleResampling").foreach {
              case Json.Arr(vs) =>
                settings.scaleResampling = vs.collect { case Json.Str(v) =>
                  try Resampling.valueOf(v)
                  catch { case _: Exception => Resampling.bicubic }
                }.toArray
              case _ =>
            }
            fields.get("atlasExtension").foreach { case Json.Str(v) => settings.atlasExtension = v; case _ => }
            fields.get("prettyPrint").foreach { case Json.Bool(v) => settings.prettyPrint = v; case _ => }
            fields.get("legacyOutput").foreach { case Json.Bool(v) => settings.legacyOutput = v; case _ => }
          case _ => // Empty file or non-object.
        }
      } finally stream.close()
    } catch {
      case ex: Exception =>
        throw new RuntimeException("Error reading settings file: " + settingsFile, ex)
    }

  override def processFiles(files: Array[File], outputRoot: File): ArrayBuffer[FileProcessor.Entry] = {
    // Delete pack file and images.
    if (countOnly && outputRoot != null && outputRoot.exists()) deleteOutput(outputRoot) // @nowarn("msg=deprecated") outputRoot can be null
    super.processFiles(files, outputRoot)
  }

  protected def deleteOutput(outputRoot: File): Unit = {
    // Load root settings to get scale.
    val settingsFile = new File(root.get, "pack.json")
    var rootSettings = defaultSettings
    if (settingsFile.exists()) {
      rootSettings = newSettings(rootSettings)
      merge(rootSettings, settingsFile)
    }

    val atlasExtension =
      if (rootSettings.atlasExtension == null) "" else rootSettings.atlasExtension // @nowarn("msg=deprecated") null check for safety
    val atlasExtensionQ = Pattern.quote(atlasExtension)

    var i = 0
    val n = rootSettings.scale.length
    while (i < n) {
      val deleteProcessor = new FileProcessor() {
        override protected def processFile(inputFile: FileProcessor.Entry): Unit =
          inputFile.inputFile.foreach(_.delete())
      }
      deleteProcessor.setRecursive(false)

      val packFile           = new File(rootSettings.getScaledPackFileName(packFileName, i))
      val scaledPackFileName = packFile.getName()

      var prefix   = packFile.getName()
      val dotIndex = prefix.lastIndexOf('.')
      if (dotIndex != -1) prefix = prefix.substring(0, dotIndex)
      deleteProcessor.addInputRegex("(?i)" + prefix + "-?\\d*\\.(png|jpg|jpeg)")
      deleteProcessor.addInputRegex("(?i)" + prefix + atlasExtensionQ)

      val dir = packFile.getParent()
      if (dir == null) // @nowarn("msg=deprecated") getParent returns null
        deleteProcessor.process(outputRoot, null) // @nowarn("msg=deprecated") null outputRoot
      else if (new File(outputRoot.getPath() + "/" + dir).exists())
        deleteProcessor.process(new File(outputRoot.getPath() + "/" + dir), null) // @nowarn("msg=deprecated") null outputRoot

      i += 1
    }
  }

  override protected def processDir(inputDir: FileProcessor.Entry, files: ArrayBuffer[FileProcessor.Entry]): Unit = {
    if (ignoreDirs.contains(inputDir.inputFile.get)) {}
    else {
      // Find first parent with settings, or use defaults.
      var settings: Nullable[Settings] = Nullable.empty
      var parent:   Nullable[File]     = inputDir.inputFile
      boundary {
        while (true) {
          parent.foreach { p =>
            dirToSettings.get(p).foreach { s =>
              settings = Nullable(s)
              break()
            }
          }
          if (parent.isEmpty || parent.exists(_.equals(root.get))) break()
          parent = parent.map(_.getParentFile())
        }
      }
      if (settings.isEmpty) settings = Nullable(defaultSettings)

      val s = settings.get
      if (!s.ignore) {
        var effectiveFiles = files
        if (s.combineSubdirectories) {
          // Collect all files under subdirectories except those with a pack.json file.
          val thisInputDir = inputDir
          val thisSelf     = this
          effectiveFiles = new FileProcessor(this) {
            override protected def processDir(entryDir: FileProcessor.Entry, dirFiles: ArrayBuffer[FileProcessor.Entry]): Unit = {
              var file = entryDir.inputFile
              boundary {
                while (file.isDefined && !file.exists(_.equals(thisInputDir.inputFile.get))) {
                  if (new File(file.get, "pack.json").exists()) {
                    dirFiles.clear()
                    break()
                  }
                  file = file.map(_.getParentFile())
                }
              }
              if (!thisSelf.countOnly) entryDir.inputFile.foreach(f => thisSelf.ignoreDirs += f)
            }

            override protected def processFile(entry: FileProcessor.Entry): Unit =
              addProcessedFile(entry)
          }.process(inputDir.inputFile.get, null) // @nowarn("msg=deprecated") null outputRoot

        }

        if (effectiveFiles.nonEmpty) {
          if (countOnly) {
            packCount += 1
          } else {
            // Sort by name using numeric suffix, then alpha.
            val digitSuffix = Pattern.compile("(.*?)(\\d+)$")
            effectiveFiles.sortInPlace()(using
              Ordering.fromLessThan { (entry1: FileProcessor.Entry, entry2: FileProcessor.Entry) =>
                var full1  = entry1.inputFile.map(_.getName()).getOrElse("")
                var dotIdx = full1.lastIndexOf('.')
                if (dotIdx != -1) full1 = full1.substring(0, dotIdx)

                var full2 = entry2.inputFile.map(_.getName()).getOrElse("")
                dotIdx = full2.lastIndexOf('.')
                if (dotIdx != -1) full2 = full2.substring(0, dotIdx)

                var name1 = full1
                var name2 = full2
                var num1  = 0
                var num2  = 0

                var matcher = digitSuffix.matcher(full1)
                if (matcher.matches()) {
                  try {
                    num1 = Integer.parseInt(matcher.group(2))
                    name1 = matcher.group(1)
                  } catch { case _: Exception => }
                }
                matcher = digitSuffix.matcher(full2)
                if (matcher.matches()) {
                  try {
                    num2 = Integer.parseInt(matcher.group(2))
                    name2 = matcher.group(1)
                  } catch { case _: Exception => }
                }
                val cmp = name1.compareTo(name2)
                if (cmp != 0 || num1 == num2) cmp < 0
                else (num1 - num2) < 0
              }
            )

            // Pack.
            if (!s.silent) {
              val pathStr =
                try inputDir.inputFile.map(_.getCanonicalPath()).getOrElse("")
                catch { case _: IOException => inputDir.inputFile.map(_.getAbsolutePath()).getOrElse("") }
              System.out.println("Reading: " + pathStr)
            }
            progress.foreach { prog =>
              prog.start(1f / packCount)
              var inputPath: Nullable[String] = Nullable.empty
              try {
                val rootPath = root.get.getCanonicalPath()
                val ip       = inputDir.inputFile.map(_.getCanonicalPath()).getOrElse("")
                if (ip.startsWith(rootPath)) {
                  val rp   = rootPath.replace('\\', '/')
                  var path = ip.substring(rp.length).replace('\\', '/')
                  if (path.startsWith("/")) path = path.substring(1)
                  inputPath = Nullable(path)
                }
              } catch { case _: IOException => }
              val finalPath =
                if (inputPath.isEmpty || inputPath.exists(_.isEmpty))
                  inputDir.inputFile.map(_.getName()).getOrElse("")
                else inputPath.get
              prog.message = finalPath
            }
            val packer = newTexturePacker(root.get, s)
            for (file <- effectiveFiles)
              file.inputFile.foreach(f => packer.addImage(f))
            pack(packer, inputDir)
            progress.foreach(_.end())
          }
        }
      }
    }
  }

  protected def pack(packer: TexturePacker, inputDir: FileProcessor.Entry): Unit =
    inputDir.outputDir.foreach { outDir =>
      packer.pack(outDir, packFileName)
    }

  protected def newTexturePacker(root: File, settings: Settings): TexturePacker = {
    val packer = TexturePacker(root, settings)
    progress.foreach(p => packer.setProgressListener(p))
    packer
  }

  protected def newSettings(settings: Settings): Settings =
    Settings(settings)
}
