/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/tools/FileProcessor.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: com.badlogic.gdx.tools -> sge.tools
 *   Convention: Java collections -> Scala collections
 *   Idiom: Scala 3 enums, boundary/break for control flow
 *   Audited: 2026-03-08
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 289
 * Covenant-baseline-methods: Entry,FileProcessor,addInputRegex,addInputSuffix,addProcessedFile,allEntries,comparator,depth,dirToEntries,effectiveOutputRoot,entryComparator,flattenOutput,inputFile,inputFilter,inputRegex,outFile,outputDir,outputFile,outputFiles,outputSuffix,process,processDir,processFile,processFiles,processInternal,recursive,setComparator,setFlattenOutput,setInputFilter,setOutputSuffix,setRecursive,this,toString
 * Covenant-source-reference: com/badlogic/gdx/tools/FileProcessor.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: 79cf00af53b7f38667291fbacf544d3074a811bd
 */
package sge
package tools

import sge.utils.Nullable

import java.io.{ File, FilenameFilter }
import java.util.regex.Pattern
import scala.collection.mutable.{ ArrayBuffer, LinkedHashMap }
import scala.util.boundary
import scala.util.boundary.break

/** Collects files recursively, filtering by file name. Callbacks are provided to process files and the results are collected, either [[processFile]] or [[processDir]] can be overridden, or both. The
  * entries provided to the callbacks have the original file, the output directory, and the output file. If [[flattenOutput]] is false, the output will match the directory structure of the input.
  * @author
  *   Nathan Sweet
  */
class FileProcessor {

  var inputFilter: Nullable[FilenameFilter] = Nullable.empty

  var comparator: Ordering[File] = Ordering.by(_.getName())

  val inputRegex: ArrayBuffer[Pattern] = ArrayBuffer.empty

  var outputSuffix: Nullable[String] = Nullable.empty

  var outputFiles: ArrayBuffer[FileProcessor.Entry] = ArrayBuffer.empty

  var recursive: Boolean = true

  var flattenOutput: Boolean = false

  private val entryComparator: Ordering[FileProcessor.Entry] =
    Ordering.fromLessThan((o1, o2) => comparator.compare(o1.inputFile.get, o2.inputFile.get) < 0)

  /** Copy constructor. */
  def this(processor: FileProcessor) = {
    this()
    inputFilter = processor.inputFilter
    comparator = processor.comparator
    inputRegex.addAll(processor.inputRegex)
    outputSuffix = processor.outputSuffix
    recursive = processor.recursive
    flattenOutput = processor.flattenOutput
  }

  /** Sets the input filter. */
  def setInputFilter(inputFilter: FilenameFilter): FileProcessor = {
    this.inputFilter = Nullable(inputFilter)
    this
  }

  /** Sets the comparator for [[processDir]]. By default the files are sorted by alpha. */
  def setComparator(comparator: Ordering[File]): FileProcessor = {
    this.comparator = comparator
    this
  }

  /** Adds a case insensitive suffix for matching input files. */
  def addInputSuffix(suffixes: String*): FileProcessor = {
    for (suffix <- suffixes)
      addInputRegex("(?i).*" + Pattern.quote(suffix))
    this
  }

  def addInputRegex(regexes: String*): FileProcessor = {
    for (regex <- regexes)
      inputRegex += Pattern.compile(regex)
    this
  }

  /** Sets the suffix for output files, replacing the extension of the input file. */
  def setOutputSuffix(outputSuffix: String): FileProcessor = {
    this.outputSuffix = Nullable(outputSuffix)
    this
  }

  def setFlattenOutput(flattenOutput: Boolean): FileProcessor = {
    this.flattenOutput = flattenOutput
    this
  }

  /** Default is true. */
  def setRecursive(recursive: Boolean): FileProcessor = {
    this.recursive = recursive
    this
  }

  /** @param outputRoot
    *   May be null.
    * @see
    *   [[process(File, File)]]
    */
  def process(inputFileOrDir: String, outputRoot: Nullable[String]): ArrayBuffer[FileProcessor.Entry] = {
    val outFile: Nullable[File] = outputRoot.map(r => new File(r))
    process(new File(inputFileOrDir), outFile.getOrElse(new File("")))
  }

  /** Processes the specified input file or directory.
    * @param outputRoot
    *   May be null if there is no output from processing the files.
    * @return
    *   the processed files added with [[addProcessedFile]].
    */
  def process(inputFileOrDir: File, outputRoot: File): ArrayBuffer[FileProcessor.Entry] = {
    if (!inputFileOrDir.exists())
      throw new IllegalArgumentException("Input file does not exist: " + inputFileOrDir.getAbsolutePath())
    if (inputFileOrDir.isFile())
      processFiles(Array(inputFileOrDir), outputRoot)
    else
      processFiles(inputFileOrDir.listFiles(), outputRoot)
  }

  /** Processes the specified input files.
    * @param outputRoot
    *   May be null if there is no output from processing the files.
    * @return
    *   the processed files added with [[addProcessedFile]].
    */
  def processFiles(files: Array[File], outputRoot: File): ArrayBuffer[FileProcessor.Entry] = {
    val effectiveOutputRoot = if (outputRoot == null) new File("") else outputRoot // @nowarn("msg=deprecated") null at Java boundary
    outputFiles.clear()

    val dirToEntries = LinkedHashMap.empty[File, ArrayBuffer[FileProcessor.Entry]]
    processInternal(files, effectiveOutputRoot, effectiveOutputRoot, dirToEntries, 0)

    val allEntries = ArrayBuffer.empty[FileProcessor.Entry]
    for ((inputDir, dirEntries) <- dirToEntries) {
      dirEntries.sortInPlace()(using entryComparator)

      var newOutputDir: Nullable[File] = Nullable.empty
      if (flattenOutput)
        newOutputDir = Nullable(effectiveOutputRoot)
      else if (dirEntries.nonEmpty)
        newOutputDir = Nullable(dirEntries.head.outputDir.get)
      var outputName = inputDir.getName()
      outputSuffix.foreach { suffix =>
        outputName = outputName.replaceAll("(.*)\\..*", "$1") + suffix
      }

      val entry = FileProcessor.Entry()
      entry.inputFile = Nullable(inputDir)
      entry.outputDir = newOutputDir
      newOutputDir.foreach { nod =>
        entry.outputFile = Nullable(
          if (nod.getPath().length == 0) new File(outputName) else new File(nod, outputName)
        )
      }

      try
        processDir(entry, dirEntries)
      catch {
        case ex: Exception =>
          throw new Exception("Error processing directory: " + entry.inputFile.map(_.getAbsolutePath()).getOrElse(""), ex)
      }
      allEntries.addAll(dirEntries)
    }

    allEntries.sortInPlace()(using entryComparator)
    for (entry <- allEntries)
      try
        processFile(entry)
      catch {
        case ex: Exception =>
          throw new Exception("Error processing file: " + entry.inputFile.map(_.getAbsolutePath()).getOrElse(""), ex)
      }

    outputFiles
  }

  private def processInternal(
    files:        Array[File],
    outputRoot:   File,
    outputDir:    File,
    dirToEntries: LinkedHashMap[File, ArrayBuffer[FileProcessor.Entry]],
    depth:        Int
  ): Unit =
    if (files == null) // @nowarn("msg=deprecated") files from listFiles() can be null at Java boundary
    {}
    else {
      // Store empty entries for every directory.
      for (file <- files) {
        val dir = file.getParentFile()
        if (!dirToEntries.contains(dir))
          dirToEntries.put(dir, ArrayBuffer.empty)
      }

      for (file <- files) {
        if (file.isFile()) {
          val matchesRegex = boundary {
            if (inputRegex.nonEmpty) {
              var found = false
              for (pattern <- inputRegex)
                if (pattern.matcher(file.getName()).matches()) {
                  found = true
                }
              if (!found) break(false)
            }
            true
          }

          if (matchesRegex) {
            val dir          = file.getParentFile()
            val passesFilter = inputFilter.forall(_.accept(dir, file.getName()))

            if (passesFilter) {
              var outputName = file.getName()
              outputSuffix.foreach { suffix =>
                outputName = outputName.replaceAll("(.*)\\..*", "$1") + suffix
              }

              val entry = FileProcessor.Entry()
              entry.depth = depth
              entry.inputFile = Nullable(file)
              entry.outputDir = Nullable(outputDir)

              if (flattenOutput) {
                entry.outputFile = Nullable(new File(outputRoot, outputName))
              } else {
                entry.outputFile = Nullable(new File(outputDir, outputName))
              }

              dirToEntries(dir) += entry
            }
          }
        }
        if (recursive && file.isDirectory()) {
          val subdir =
            if (outputDir.getPath().length == 0) new File(file.getName())
            else new File(outputDir, file.getName())
          val listed = inputFilter match {
            case f if f.isDefined => file.listFiles(f.get)
            case _                => file.listFiles()
          }
          processInternal(listed, outputRoot, subdir, dirToEntries, depth + 1)
        }
      }
    }

  /** Called with each input file. */
  protected def processFile(entry: FileProcessor.Entry): Unit = {}

  /** Called for each input directory. The files will be sorted. The specified files list can be modified to change which files are processed.
    */
  protected def processDir(entryDir: FileProcessor.Entry, files: ArrayBuffer[FileProcessor.Entry]): Unit = {}

  /** This method should be called by [[processFile]] or [[processDir]] if the return value of [[process]] or [[processFiles]] should return all the processed files.
    */
  protected def addProcessedFile(entry: FileProcessor.Entry): Unit =
    outputFiles += entry
}

/** @author
  *   Nathan Sweet
  */
object FileProcessor {

  class Entry {
    var inputFile: Nullable[File] = Nullable.empty

    /** May be null. */
    var outputDir:  Nullable[File] = Nullable.empty
    var outputFile: Nullable[File] = Nullable.empty
    var depth:      Int            = 0

    def this(inputFile: File, outputFile: File) = {
      this()
      this.inputFile = Nullable(inputFile)
      this.outputFile = Nullable(outputFile)
    }

    override def toString: String =
      inputFile.map(_.toString).getOrElse("")
  }
}
