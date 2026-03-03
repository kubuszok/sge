/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g2d/TextureAtlas.java
 * Original authors: Nathan Sweet
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Renames: dispose() -> close(); ObjectSet<Texture> -> MutableSet[Texture]; @Null -> Nullable
 *   Convention: Nullable throughout; using Sge context parameter; MutableMap/MutableSet
 *   Idiom: boundary/break, Nullable, split packages
 *   TODO: Java-style getters/setters — getRegions, getTextures; AtlasSprite: getX/Y, getOriginX/Y, getWidth/Height; AtlasRegion: getRotatedPackedWidth/Height
 *   Audited: 2026-03-03
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g2d

import scala.collection.mutable.{ Map as MutableMap, Set as MutableSet }
import sge.utils.DynamicArray
import scala.util.boundary
import scala.language.implicitConversions
import sge.Sge
import sge.files.FileHandle
import sge.graphics.Texture
import sge.graphics.Pixmap.Format
import sge.graphics.Texture.{ TextureFilter, TextureWrap }
import sge.graphics.g2d.{ NinePatch, Sprite, TextureRegion }
import sge.utils.{ Nullable, SgeError, StreamUtils }

/** Loads images from texture atlases created by TexturePacker.<br> <br> A TextureAtlas must be disposed to free up the resources consumed by the backing textures.
  * @author
  *   Nathan Sweet
  */
class TextureAtlas() extends AutoCloseable {
  private val textures: MutableSet[Texture]                    = MutableSet.empty[Texture]
  private val regions:  DynamicArray[TextureAtlas.AtlasRegion] = DynamicArray[TextureAtlas.AtlasRegion]()

  /** Loads the specified pack file using FileType.Internal, using the parent directory of the pack file to find the page images.
    */
  def this(internalPackFile: String)(using Sge) = {
    this()
    val packFile = Sge().files.internal(internalPackFile)
    load(new TextureAtlas.TextureAtlasData(packFile, packFile.parent(), false))
  }

  /** Loads the specified pack file, using the parent directory of the pack file to find the page images. */
  def this(packFile: FileHandle)(using Sge) = {
    this()
    load(new TextureAtlas.TextureAtlasData(packFile, packFile.parent(), false))
  }

  /** @param flip If true, all regions loaded will be flipped for use with a perspective where 0,0 is the upper left corner. */
  def this(packFile: FileHandle, flip: Boolean)(using Sge) = {
    this()
    load(new TextureAtlas.TextureAtlasData(packFile, packFile.parent(), flip))
  }

  def this(packFile: FileHandle, imagesDir: FileHandle)(using Sge) = {
    this()
    load(new TextureAtlas.TextureAtlasData(packFile, imagesDir, false))
  }

  /** @param flip If true, all regions loaded will be flipped for use with a perspective where 0,0 is the upper left corner. */
  def this(packFile: FileHandle, imagesDir: FileHandle, flip: Boolean)(using Sge) = {
    this()
    load(new TextureAtlas.TextureAtlasData(packFile, imagesDir, flip))
  }

  def this(data: TextureAtlas.TextureAtlasData)(using Sge) = {
    this()
    load(data)
  }

  /** Adds the textures and regions from the specified texture atlas data. */
  def load(data: TextureAtlas.TextureAtlasData)(using Sge): Unit = {
    for (page <- data.pages) {
      if (page.texture.isEmpty) {
        page.texture = Nullable(
          new Texture(page.textureFile.getOrElse(throw SgeError.GraphicsError("Page has no texture file")), page.format, page.useMipMaps)
        )
      }
      page.texture.foreach { tex =>
        tex.setFilter(page.minFilter, page.magFilter)
        tex.setWrap(page.uWrap, page.vWrap)
        textures.addOne(tex)
      }
    }

    for (region <- data.regions) {
      val atlasRegion = new TextureAtlas.AtlasRegion(
        region.page.texture.getOrElse(throw SgeError.GraphicsError("Region page has no texture")),
        region.left,
        region.top,
        if (region.rotate) region.height else region.width,
        if (region.rotate) region.width else region.height
      )
      atlasRegion.index = region.index
      atlasRegion.name = region.name
      atlasRegion.offsetX = region.offsetX
      atlasRegion.offsetY = region.offsetY
      atlasRegion.originalHeight = region.originalHeight
      atlasRegion.originalWidth = region.originalWidth
      atlasRegion.rotate = region.rotate
      atlasRegion.degrees = region.degrees
      atlasRegion.names = region.names
      atlasRegion.values = region.values
      if (region.flip) atlasRegion.flip(false, true)
      regions.add(atlasRegion)
    }
  }

  /** Adds a region to the atlas. The specified texture will be disposed when the atlas is disposed. */
  def addRegion(name: String, texture: Texture, x: Int, y: Int, width: Int, height: Int): TextureAtlas.AtlasRegion = {
    textures.addOne(texture)
    val region = new TextureAtlas.AtlasRegion(texture, x, y, width, height)
    region.name = name
    regions.add(region)
    region
  }

  /** Adds a region to the atlas. The texture for the specified region will be disposed when the atlas is disposed. */
  def addRegion(name: String, textureRegion: TextureRegion): TextureAtlas.AtlasRegion = {
    textures.addOne(textureRegion.texture)
    val region = new TextureAtlas.AtlasRegion(textureRegion)
    region.name = name
    regions.add(region)
    region
  }

  /** Returns all regions in the atlas. */
  def getRegions(): Array[TextureAtlas.AtlasRegion] =
    regions.toArray

  /** Returns the first region found with the specified name. This method uses string comparison to find the region, so the result should be cached rather than calling this method multiple times.
    */
  def findRegion(name: String): Nullable[TextureAtlas.AtlasRegion] =
    Nullable.fromOption(regions.iterator.find(_.name == name))

  /** Returns the first region found with the specified name and index. This method uses string comparison to find the region, so the result should be cached rather than calling this method multiple
    * times.
    */
  def findRegion(name: String, index: Int): Nullable[TextureAtlas.AtlasRegion] =
    Nullable.fromOption(regions.iterator.find(r => r.name == name && r.index == index))

  /** Returns all regions with the specified name, ordered by smallest to largest {@link AtlasRegion#index index} . This method uses string comparison to find the regions, so the result should be
    * cached rather than calling this method multiple times.
    */
  def findRegions(name: String): Array[TextureAtlas.AtlasRegion] =
    regions.iterator.filter(_.name == name).map(r => new TextureAtlas.AtlasRegion(r)).toArray

  /** Returns all regions in the atlas as sprites. This method creates a new sprite for each region, so the result should be stored rather than calling this method multiple times.
    * @see
    *   #createSprite(String)
    */
  def createSprites(): Array[Sprite] =
    regions.iterator.map(newSprite).toArray

  /** Returns the first region found with the specified name as a sprite. If whitespace was stripped from the region when it was packed, the sprite is automatically positioned as if whitespace had not
    * been stripped. This method uses string comparison to find the region and constructs a new sprite, so the result should be cached rather than calling this method multiple times.
    */
  def createSprite(name: String): Nullable[Sprite] =
    Nullable.fromOption(regions.iterator.find(_.name == name).map(newSprite))

  /** Returns the first region found with the specified name and index as a sprite. This method uses string comparison to find the region and constructs a new sprite, so the result should be cached
    * rather than calling this method multiple times.
    * @see
    *   #createSprite(String)
    */
  def createSprite(name: String, index: Int): Nullable[Sprite] =
    Nullable.fromOption(regions.iterator.find(r => r.name == name && r.index == index).map(newSprite))

  /** Returns all regions with the specified name as sprites, ordered by smallest to largest {@link AtlasRegion#index index} . This method uses string comparison to find the regions and constructs new
    * sprites, so the result should be cached rather than calling this method multiple times.
    * @see
    *   #createSprite(String)
    */
  def createSprites(name: String): Array[Sprite] =
    regions.iterator.filter(_.name == name).map(newSprite).toArray

  private def newSprite(region: TextureAtlas.AtlasRegion): Sprite =
    if (region.packedWidth == region.originalWidth && region.packedHeight == region.originalHeight) {
      if (region.rotate) {
        val sprite = new Sprite(region)
        sprite.setBounds(0, 0, region.getRegionHeight().toFloat, region.getRegionWidth().toFloat)
        sprite.rotate90(true)
        sprite
      } else {
        new Sprite(region)
      }
    } else {
      new TextureAtlas.AtlasSprite(region)
    }

  /** Returns the first region found with the specified name as a {@link NinePatch} . The region must have been packed with ninepatch splits. This method uses string comparison to find the region and
    * constructs a new ninepatch, so the result should be cached rather than calling this method multiple times.
    */
  def createPatch(name: String): Nullable[NinePatch] =
    Nullable.fromOption(
      regions.iterator.find(_.name == name).flatMap { region =>
        val splits = region.findValue("split")
        if (splits.isEmpty) throw new IllegalArgumentException("Region does not have ninepatch splits: " + name)
        val s     = splits.getOrElse(throw new IllegalArgumentException("Region does not have ninepatch splits: " + name))
        val patch = new NinePatch(region, s(0), s(1), s(2), s(3))
        region.findValue("pad").foreach { p =>
          patch.setPadding(p(0).toFloat, p(1).toFloat, p(2).toFloat, p(3).toFloat)
        }
        Some(patch)
      }
    )

  /** @return the textures of the pages, unordered */
  def getTextures(): MutableSet[Texture] =
    textures

  /** Releases all resources associated with this TextureAtlas instance. This releases all the textures backing all TextureRegions and Sprites, which should no longer be used after calling dispose.
    */
  override def close(): Unit = {
    textures.foreach(_.close()) // AutoCloseable instead of Disposable
    textures.clear()
  }
}

object TextureAtlas {

  class TextureAtlasData {
    val pages:   DynamicArray[Page]   = DynamicArray[Page]()
    val regions: DynamicArray[Region] = DynamicArray[Region]()

    def this(packFile: FileHandle, imagesDir: FileHandle, flip: Boolean) = {
      this()
      load(packFile, imagesDir, flip)
    }

    def load(packFile: FileHandle, imagesDir: FileHandle, flip: Boolean): Unit = {
      val entry = new Array[String](5)

      val pageFields: MutableMap[String, Field[Page]] = MutableMap.empty
      pageFields.put(
        "size",
        new Field[Page] {
          override def parse(page: Page): Unit = {
            page.width = Integer.parseInt(entry(1)).toFloat
            page.height = Integer.parseInt(entry(2)).toFloat
          }
        }
      )
      pageFields.put("format",
                     new Field[Page] {
                       override def parse(page: Page): Unit =
                         page.format = Format.valueOf(entry(1))
                     }
      )
      pageFields.put(
        "filter",
        new Field[Page] {
          override def parse(page: Page): Unit = {
            page.minFilter = TextureFilter.valueOf(entry(1))
            page.magFilter = TextureFilter.valueOf(entry(2))
            page.useMipMaps = page.minFilter.isMipMap()
          }
        }
      )
      pageFields.put(
        "repeat",
        new Field[Page] {
          override def parse(page: Page): Unit = {
            if (entry(1).indexOf('x') != -1) page.uWrap = TextureWrap.Repeat
            if (entry(1).indexOf('y') != -1) page.vWrap = TextureWrap.Repeat
          }
        }
      )
      pageFields.put("pma",
                     new Field[Page] {
                       override def parse(page: Page): Unit =
                         page.pma = entry(1).equals("true")
                     }
      )

      var hasIndexes = false
      val regionFields: MutableMap[String, Field[Region]] = MutableMap.empty
      regionFields.put(
        "xy",
        new Field[Region] { // Deprecated, use bounds.
          override def parse(region: Region): Unit = {
            region.left = Integer.parseInt(entry(1))
            region.top = Integer.parseInt(entry(2))
          }
        }
      )
      regionFields.put(
        "size",
        new Field[Region] { // Deprecated, use bounds.
          override def parse(region: Region): Unit = {
            region.width = Integer.parseInt(entry(1))
            region.height = Integer.parseInt(entry(2))
          }
        }
      )
      regionFields.put(
        "bounds",
        new Field[Region] {
          override def parse(region: Region): Unit = {
            region.left = Integer.parseInt(entry(1))
            region.top = Integer.parseInt(entry(2))
            region.width = Integer.parseInt(entry(3))
            region.height = Integer.parseInt(entry(4))
          }
        }
      )
      regionFields.put(
        "offset",
        new Field[Region] { // Deprecated, use offsets.
          override def parse(region: Region): Unit = {
            region.offsetX = Integer.parseInt(entry(1)).toFloat
            region.offsetY = Integer.parseInt(entry(2)).toFloat
          }
        }
      )
      regionFields.put(
        "orig",
        new Field[Region] { // Deprecated, use offsets.
          override def parse(region: Region): Unit = {
            region.originalWidth = Integer.parseInt(entry(1))
            region.originalHeight = Integer.parseInt(entry(2))
          }
        }
      )
      regionFields.put(
        "offsets",
        new Field[Region] {
          override def parse(region: Region): Unit = {
            region.offsetX = Integer.parseInt(entry(1)).toFloat
            region.offsetY = Integer.parseInt(entry(2)).toFloat
            region.originalWidth = Integer.parseInt(entry(3))
            region.originalHeight = Integer.parseInt(entry(4))
          }
        }
      )
      regionFields.put(
        "rotate",
        new Field[Region] {
          override def parse(region: Region): Unit = {
            val value = entry(1)
            if (value.equals("true"))
              region.degrees = 90
            else if (!value.equals("false")) //
              region.degrees = Integer.parseInt(value)
            region.rotate = region.degrees == 90
          }
        }
      )
      regionFields.put(
        "index",
        new Field[Region] {
          override def parse(region: Region): Unit = {
            region.index = Integer.parseInt(entry(1))
            if (region.index != -1) hasIndexes = true
          }
        }
      )

      val reader = packFile.reader(1024)
      var line: Nullable[String] = Nullable.empty
      try {
        line = Nullable(reader.readLine())
        // Ignore empty lines before first entry.
        while (line.isDefined && line.getOrElse("").trim().length == 0)
          line = Nullable(reader.readLine())
        // Header entries.
        boundary {
          while (true) {
            if (line.fold(true)(_.trim().length == 0)) boundary.break()
            if (readEntry(entry, line.getOrElse("")) == 0) boundary.break() // Silently ignore all header fields.
            line = Nullable(reader.readLine())
          }
        }
        // Page and region entries.
        var page:   Nullable[Page]                     = Nullable.empty
        var names:  Nullable[DynamicArray[String]]     = Nullable.empty
        var values: Nullable[DynamicArray[Array[Int]]] = Nullable.empty
        boundary {
          while (true) {
            if (line.isEmpty) boundary.break()
            if (line.getOrElse("").trim().length == 0) {
              page = Nullable.empty
              line = Nullable(reader.readLine())
            } else if (page.isEmpty) {
              val p = new Page()
              p.name = line.getOrElse("")
              p.textureFile = Nullable(imagesDir.child(line.getOrElse("")))
              boundary {
                while (true) {
                  if (readEntry(entry, line = reader.readLine()) == 0) boundary.break()
                  val field = pageFields.get(entry(0))
                  if (field.isDefined) field.get.parse(p) // Silently ignore unknown page fields.
                }
              }
              pages.add(p)
              page = Nullable(p)
            } else {
              val region = new Region()
              region.page = page.getOrElse(throw SgeError.GraphicsError("Region has no page"))
              region.name = line.getOrElse("").trim()
              if (flip) region.flip = true
              boundary {
                while (true) {
                  val count = readEntry(entry, line = reader.readLine())
                  if (count == 0) boundary.break()
                  val field = regionFields.get(entry(0))
                  if (field.isDefined)
                    field.get.parse(region)
                  else {
                    if (names.isEmpty) {
                      names = Nullable(DynamicArray[String]())
                      values = Nullable(DynamicArray[Array[Int]]())
                    }
                    names.foreach(_.add(entry(0)))
                    val entryValues = new Array[Int](count)
                    for (i <- 0 until count)
                      try
                        entryValues(i) = Integer.parseInt(entry(i + 1))
                      catch {
                        case _: NumberFormatException => // Silently ignore non-integer values.
                      }
                    values.foreach(_.add(entryValues))
                  }
                }
              }
              if (region.originalWidth == 0 && region.originalHeight == 0) {
                region.originalWidth = region.width
                region.originalHeight = region.height
              }
              names.foreach { ns =>
                if (ns.size > 0) {
                  region.names = Nullable(ns.toArray)
                  values.foreach { vs => region.values = Nullable(vs.toArray) }
                  ns.clear()
                  values.foreach(_.clear())
                }
              }
              regions.add(region)
            }
          }
        }
      } catch {
        case ex: Exception =>
          throw SgeError.FileReadError(packFile, "Error reading texture atlas file: " + packFile + line.fold("")(l => "\nLine: " + l), Some(ex))
      } finally
        StreamUtils.closeQuietly(reader)

      if (hasIndexes) {
        regions.sort(
          Ordering.fromLessThan[Region] { (r1, r2) =>
            val i1 = if (r1.index == -1) Int.MaxValue else r1.index
            val i2 = if (r2.index == -1) Int.MaxValue else r2.index
            i1 < i2
          }
        )
      }
    }

    def getPages(): Array[Page] =
      pages.toArray

    def getRegions(): Array[Region] =
      regions.toArray

    private def readEntry(entry: Array[String], line: String): Int =
      boundary {
        if (Nullable(line).isEmpty) boundary.break(0)
        val trimmedLine = line.trim()
        if (trimmedLine.length == 0) boundary.break(0)
        val colon = trimmedLine.indexOf(':')
        if (colon == -1) boundary.break(0)
        entry(0) = trimmedLine.substring(0, colon).trim()
        var lastMatch = colon + 1
        for (i <- 1 to 4) {
          val comma = trimmedLine.indexOf(',', lastMatch)
          if (comma == -1) {
            entry(i) = trimmedLine.substring(lastMatch).trim()
            boundary.break(i)
          }
          entry(i) = trimmedLine.substring(lastMatch, comma).trim()
          lastMatch = comma + 1
          if (i == 4) boundary.break(4)
        }
        0
      }

    private trait Field[T] {
      def parse(obj: T): Unit
    }

    class Page {
      var name: String = scala.compiletime.uninitialized

      /** May be null if this page isn't associated with a file. In that case, {@link #texture} must be set. */
      var textureFile: Nullable[FileHandle] = Nullable.empty

      /** May be null if the texture is not yet loaded. */
      var texture:    Nullable[Texture] = Nullable.empty
      var width:      Float             = 0f
      var height:     Float             = 0f
      var useMipMaps: Boolean           = false
      var format:     Format            = Format.RGBA8888
      var minFilter:  TextureFilter     = TextureFilter.Nearest
      var magFilter:  TextureFilter     = TextureFilter.Nearest
      var uWrap:      TextureWrap       = TextureWrap.ClampToEdge
      var vWrap:      TextureWrap       = TextureWrap.ClampToEdge
      var pma:        Boolean           = false
    }

    class Region {
      var page:           Page                        = scala.compiletime.uninitialized
      var name:           String                      = scala.compiletime.uninitialized
      var left:           Int                         = 0
      var top:            Int                         = 0
      var width:          Int                         = 0
      var height:         Int                         = 0
      var offsetX:        Float                       = 0f
      var offsetY:        Float                       = 0f
      var originalWidth:  Int                         = 0
      var originalHeight: Int                         = 0
      var degrees:        Int                         = 0
      var rotate:         Boolean                     = false
      var index:          Int                         = -1
      var names:          Nullable[Array[String]]     = Nullable.empty
      var values:         Nullable[Array[Array[Int]]] = Nullable.empty
      var flip:           Boolean                     = false

      def findValue(name: String): Nullable[Array[Int]] =
        boundary {
          names.foreach { ns =>
            val vs = values.getOrElse(throw SgeError.GraphicsError("names defined but values missing"))
            for (i <- ns.indices)
              if (name.equals(ns(i))) boundary.break(Nullable(vs(i)))
          }
          Nullable.empty
        }
    }
  }

  /** Describes the region of a packed image and provides information about the original image before it was packed. */
  class AtlasRegion extends TextureRegion {

    /** The number at the end of the original image file name, or -1 if none.<br> <br> When sprites are packed, if the original file name ends with a number, it is stored as the index and is not
      * considered as part of the sprite's name. This is useful for keeping animation frames in order.
      * @see
      *   TextureAtlas#findRegions(String)
      */
    var index: Int = -1

    /** The name of the original image file, without the file's extension.<br> If the name ends with an underscore followed by only numbers, that part is excluded: underscores denote special
      * instructions to the texture packer.
      */
    var name: String = scala.compiletime.uninitialized

    /** The offset from the left of the original image to the left of the packed image, after whitespace was removed for packing.
      */
    var offsetX: Float = 0f

    /** The offset from the bottom of the original image to the bottom of the packed image, after whitespace was removed for packing.
      */
    var offsetY: Float = 0f

    /** The width of the image, after whitespace was removed for packing. */
    var packedWidth: Int = 0

    /** The height of the image, after whitespace was removed for packing. */
    var packedHeight: Int = 0

    /** The width of the image, before whitespace was removed and rotation was applied for packing. */
    var originalWidth: Int = 0

    /** The height of the image, before whitespace was removed for packing. */
    var originalHeight: Int = 0

    /** If true, the region has been rotated 90 degrees counter clockwise. */
    var rotate: Boolean = false

    /** The degrees the region has been rotated, counter clockwise between 0 and 359. Most atlas region handling deals only with 0 or 90 degree rotation (enough to handle rectangles). More advanced
      * texture packing may support other rotations (eg, for tightly packing polygons).
      */
    var degrees: Int = 0

    /** Names for name/value pairs other than the fields provided on this class, each entry corresponding to {@link #values}. */
    var names: Nullable[Array[String]] = Nullable.empty

    /** Values for name/value pairs other than the fields provided on this class, each entry corresponding to {@link #names}. */
    var values: Nullable[Array[Array[Int]]] = Nullable.empty

    def this(texture: Texture, x: Int, y: Int, width: Int, height: Int) = {
      this()
      this.texture = texture
      setRegion(x, y, width, height)
      originalWidth = width
      originalHeight = height
      packedWidth = width
      packedHeight = height
    }

    def this(region: AtlasRegion) = {
      this()
      setRegion(region)
      index = region.index
      name = region.name
      offsetX = region.offsetX
      offsetY = region.offsetY
      packedWidth = region.packedWidth
      packedHeight = region.packedHeight
      originalWidth = region.originalWidth
      originalHeight = region.originalHeight
      rotate = region.rotate
      degrees = region.degrees
      names = region.names
      values = region.values
    }

    def this(region: TextureRegion) = {
      this()
      setRegion(region)
      packedWidth = region.getRegionWidth()
      packedHeight = region.getRegionHeight()
      originalWidth = packedWidth
      originalHeight = packedHeight
    }

    override def flip(x: Boolean, y: Boolean): Unit = {
      super.flip(x, y)
      if (x) offsetX = originalWidth - offsetX - getRotatedPackedWidth()
      if (y) offsetY = originalHeight - offsetY - getRotatedPackedHeight()
    }

    /** Returns the packed width considering the {@link #rotate} value, if it is true then it returns the packedHeight, otherwise it returns the packedWidth.
      */
    def getRotatedPackedWidth(): Float =
      if (rotate) packedHeight.toFloat else packedWidth.toFloat

    /** Returns the packed height considering the {@link #rotate} value, if it is true then it returns the packedWidth, otherwise it returns the packedHeight.
      */
    def getRotatedPackedHeight(): Float =
      if (rotate) packedWidth.toFloat else packedHeight.toFloat

    def findValue(name: String): Nullable[Array[Int]] =
      boundary {
        names.foreach { ns =>
          val vs = values.getOrElse(throw SgeError.GraphicsError("names defined but values missing"))
          for (i <- ns.indices)
            if (name.equals(ns(i))) boundary.break(Nullable(vs(i)))
        }
        Nullable.empty
      }

    override def toString(): String =
      name
  }

  /** A sprite that, if whitespace was stripped from the region when it was packed, is automatically positioned as if whitespace had not been stripped.
    */
  class AtlasSprite(private var region: AtlasRegion) extends Sprite {
    private var originalOffsetX: Float = region.offsetX
    private var originalOffsetY: Float = region.offsetY

    this.region = new AtlasRegion(region)
    setRegion(region)
    setOrigin(region.originalWidth / 2f, region.originalHeight / 2f)
    val w = region.getRegionWidth()
    val h = region.getRegionHeight()
    if (region.rotate) {
      super.rotate90(true)
      super.setBounds(region.offsetX, region.offsetY, h.toFloat, w.toFloat)
    } else
      super.setBounds(region.offsetX, region.offsetY, w.toFloat, h.toFloat)
    setColor(1f, 1f, 1f, 1f)

    def this(sprite: AtlasSprite) = {
      this(sprite.region)
      originalOffsetX = sprite.originalOffsetX
      originalOffsetY = sprite.originalOffsetY
      set(sprite)
    }

    override def setPosition(x: Float, y: Float): Unit =
      super.setPosition(x + region.offsetX, y + region.offsetY)

    override def setX(x: Float): Unit =
      super.setX(x + region.offsetX)

    override def setY(y: Float): Unit =
      super.setY(y + region.offsetY)

    override def setBounds(x: Float, y: Float, width: Float, height: Float): Unit = {
      val widthRatio  = width / region.originalWidth
      val heightRatio = height / region.originalHeight
      region.offsetX = originalOffsetX * widthRatio
      region.offsetY = originalOffsetY * heightRatio
      val packedWidth  = if (region.rotate) region.packedHeight else region.packedWidth
      val packedHeight = if (region.rotate) region.packedWidth else region.packedHeight
      super.setBounds(x + region.offsetX, y + region.offsetY, packedWidth * widthRatio, packedHeight * heightRatio)
    }

    override def setSize(width: Float, height: Float): Unit =
      setBounds(getX(), getY(), width, height)

    override def setOrigin(originX: Float, originY: Float): Unit =
      super.setOrigin(originX - region.offsetX, originY - region.offsetY)

    override def setOriginCenter(): Unit =
      super.setOrigin(getWidth() / 2 - region.offsetX, getHeight() / 2 - region.offsetY)

    override def flip(x: Boolean, y: Boolean): Unit = {
      // Flip texture.
      if (region.rotate)
        super.flip(y, x)
      else
        super.flip(x, y)

      val oldOriginX = getOriginX()
      val oldOriginY = getOriginY()
      val oldOffsetX = region.offsetX
      val oldOffsetY = region.offsetY

      val widthRatio  = getWidthRatio()
      val heightRatio = getHeightRatio()

      region.offsetX = originalOffsetX
      region.offsetY = originalOffsetY
      region.flip(x, y) // Updates x and y offsets.
      originalOffsetX = region.offsetX
      originalOffsetY = region.offsetY
      region.offsetX *= widthRatio
      region.offsetY *= heightRatio

      // Update position and origin with new offsets.
      translate(region.offsetX - oldOffsetX, region.offsetY - oldOffsetY)
      setOrigin(oldOriginX, oldOriginY)
    }

    override def rotate90(clockwise: Boolean): Unit = {
      // Rotate texture.
      super.rotate90(clockwise)

      val oldOriginX = getOriginX()
      val oldOriginY = getOriginY()
      val oldOffsetX = region.offsetX
      val oldOffsetY = region.offsetY

      val widthRatio  = getWidthRatio()
      val heightRatio = getHeightRatio()

      if (clockwise) {
        region.offsetX = oldOffsetY
        region.offsetY = region.originalHeight * heightRatio - oldOffsetX - region.packedWidth * widthRatio
      } else {
        region.offsetX = region.originalWidth * widthRatio - oldOffsetY - region.packedHeight * heightRatio
        region.offsetY = oldOffsetX
      }

      // Update position and origin with new offsets.
      translate(region.offsetX - oldOffsetX, region.offsetY - oldOffsetY)
      setOrigin(oldOriginX, oldOriginY)
    }

    override def getX(): Float =
      super.getX() - region.offsetX

    override def getY(): Float =
      super.getY() - region.offsetY

    override def getOriginX(): Float =
      super.getOriginX() + region.offsetX

    override def getOriginY(): Float =
      super.getOriginY() + region.offsetY

    override def getWidth(): Float =
      super.getWidth() / region.getRotatedPackedWidth() * region.originalWidth

    override def getHeight(): Float =
      super.getHeight() / region.getRotatedPackedHeight() * region.originalHeight

    def getWidthRatio(): Float =
      super.getWidth() / region.getRotatedPackedWidth()

    def getHeightRatio(): Float =
      super.getHeight() / region.getRotatedPackedHeight()

    def getAtlasRegion(): AtlasRegion =
      region

    override def toString(): String =
      region.toString()
  }
}
