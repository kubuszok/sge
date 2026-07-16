/*
 * SGE - Scala Game Engine
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * End-to-end fixture tests for ISS-767: the Tiled FILE-LOADING path was never
 * exercised (no .tmx/.tmj fixtures in the repo; the only loader test covered
 * colour parsing, and the tile-world demo builds maps programmatically).
 *
 * These suites drive the REAL TmxMapLoader / TmjMapLoader over hand-written
 * .tmx / .tmj fixtures written to a temp directory, covering the encoding
 * matrix that had zero coverage:
 *
 *   - TMX: external tileset (.tsx) + gzip-compressed base64 layer + CSV layer
 *          + object layer with a typed custom property.
 *   - TMJ: external tileset (.tsj) + zlib-compressed base64 layer + CSV layer
 *          (native JSON int array) + object layer with a typed custom property.
 *
 * A tiny 32x32 PNG tileset (2x2 tiles of 16px, generated inline via ImageIO,
 * mirroring AndroidRuntimeTest's inline-PNG idiom) is loaded through a headless
 * Sge (NoopGraphics -> NoopGL20), so full Texture creation and tile slicing run
 * without a GPU. Loading is JVM-only: java.util.zip gzip/deflate and the
 * desktop image decode path do not exist on the Scala.js linker, so this suite
 * lives in scalajvm alongside the sibling TmxTileIdsEofRedSuite.
 */
package sge
package maps
package tiled

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files => JFiles, Path }
import java.util.zip.{ DeflaterOutputStream, GZIPOutputStream }

import sge.assets.loaders.FileHandleResolver
import sge.files.DesktopFiles
import sge.noop.NoopGraphics

class TiledMapFileLoadingSuite extends munit.FunSuite {

  private given Sge = SgeTestFixture.testSge(graphics = new NoopGraphics(), files = DesktopFiles())

  /** Row-major 4x4 gids; each of tileset gids 1..4 appears exactly four times, so every cell is filled. */
  private val gids: Array[Int]     = Array(1, 2, 3, 4, 4, 3, 2, 1, 1, 1, 2, 2, 3, 3, 4, 4)
  private val expectedSorted: List[Int] = gids.toList.sorted

  /** Little-endian 4-byte-per-gid packing, exactly what the base64 layer decoders read back. */
  private def pack(ids: Array[Int]): Array[Byte] = {
    val out = new Array[Byte](ids.length * 4)
    var i   = 0
    while (i < ids.length) {
      val g = ids(i)
      out(i * 4) = (g & 0xff).toByte
      out(i * 4 + 1) = ((g >> 8) & 0xff).toByte
      out(i * 4 + 2) = ((g >> 16) & 0xff).toByte
      out(i * 4 + 3) = ((g >> 24) & 0xff).toByte
      i += 1
    }
    out
  }

  private def gzipBase64(ids: Array[Int]): String = {
    val bos = new ByteArrayOutputStream()
    val gz  = new GZIPOutputStream(bos)
    gz.write(pack(ids))
    gz.close()
    java.util.Base64.getEncoder.encodeToString(bos.toByteArray)
  }

  private def zlibBase64(ids: Array[Int]): String = {
    val bos = new ByteArrayOutputStream()
    val df  = new DeflaterOutputStream(bos)
    df.write(pack(ids))
    df.close()
    java.util.Base64.getEncoder.encodeToString(bos.toByteArray)
  }

  /** 32x32 RGBA PNG (2x2 grid of 16px tiles) written to `dir/tiles.png` via ImageIO. */
  private def writeTilesetPng(dir: Path): Unit = {
    val img = new java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    var y   = 0
    while (y < 32) {
      var x = 0
      while (x < 32) {
        img.setRGB(x, y, 0xff000000 | ((x * 8) << 16) | ((y * 8) << 8))
        x += 1
      }
      y += 1
    }
    val out = dir.resolve("tiles.png").toFile
    javax.imageio.ImageIO.write(img, "png", out)
  }

  private def write(dir: Path, name: String, content: String): Path = {
    val p = dir.resolve(name)
    JFiles.write(p, content.getBytes(StandardCharsets.UTF_8))
    p
  }

  /** Collects the sorted gids of every populated cell — invariant under the loader's Y-flip, which only reorders rows. */
  private def layerGids(layer: TiledMapTileLayer): List[Int] = {
    val buf = scala.collection.mutable.ListBuffer[Int]()
    var y   = 0
    while (y < layer.height) {
      var x = 0
      while (x < layer.width) {
        layer.getCell(x, y).foreach(cell => cell.tile.foreach(t => buf += t.id))
        x += 1
      }
      y += 1
    }
    buf.toList.sorted
  }

  private def tileLayer(map: TiledMap, name: String): TiledMapTileLayer =
    map.layers.get(name).getOrElse(fail(s"missing layer '$name'")) match {
      case l: TiledMapTileLayer => l
      case other                => fail(s"layer '$name' is ${other.getClass.getSimpleName}, expected TiledMapTileLayer")
    }

  test("TMX: external tileset + gzip + CSV layers + object layer with properties load end-to-end") {
    val dir = JFiles.createTempDirectory("sge-tmx-fixture")
    writeTilesetPng(dir)

    write(
      dir,
      "tileset.tsx",
      """<?xml version="1.0" encoding="UTF-8"?>
        |<tileset version="1.10" name="tiles" tilewidth="16" tileheight="16" tilecount="4" columns="2">
        | <image source="tiles.png" width="32" height="32"/>
        |</tileset>""".stripMargin
    )

    val mapFile = write(
      dir,
      "map.tmx",
      s"""<?xml version="1.0" encoding="UTF-8"?>
         |<map version="1.10" orientation="orthogonal" renderorder="right-down" width="4" height="4" tilewidth="16" tileheight="16" infinite="0">
         | <properties>
         |  <property name="author" value="sge-test"/>
         | </properties>
         | <tileset firstgid="1" source="tileset.tsx"/>
         | <layer id="1" name="gzip_layer" width="4" height="4">
         |  <data encoding="base64" compression="gzip">${gzipBase64(gids)}</data>
         | </layer>
         | <layer id="2" name="csv_layer" width="4" height="4">
         |  <data encoding="csv">
         |${gids.mkString(",")}
         |</data>
         | </layer>
         | <objectgroup id="3" name="objects">
         |  <object id="1" name="spawn" type="marker" x="16" y="32" width="8" height="8">
         |   <properties>
         |    <property name="hp" type="int" value="100"/>
         |   </properties>
         |  </object>
         | </objectgroup>
         |</map>""".stripMargin
    )

    val loader = TmxMapLoader(FileHandleResolver.Absolute())
    val map    = loader.load(mapFile.toAbsolutePath.toString)

    try {
      // Map-level properties
      assertEquals(map.properties.getAs[Integer]("width").get.intValue(), 4)
      assertEquals(map.properties.getAs[Integer]("height").get.intValue(), 4)
      assertEquals(map.properties.getAs[Integer]("tilewidth").get.intValue(), 16)
      assertEquals(map.properties.getAs[Integer]("tileheight").get.intValue(), 16)
      assertEquals(map.properties.getAs[String]("author").getOrElse(fail("missing author prop")), "sge-test")

      // External tileset was parsed and sliced into 4 tiles (gids 1..4)
      assert(map.tileSets.getTile(1).isDefined, "tileset gid 1 missing (external .tsx not loaded)")
      assert(map.tileSets.getTile(4).isDefined, "tileset gid 4 missing (tile slicing incomplete)")

      // gzip and CSV layers decode to the same gid multiset
      assertEquals(layerGids(tileLayer(map, "gzip_layer")), expectedSorted)
      assertEquals(layerGids(tileLayer(map, "csv_layer")), expectedSorted)

      // Object layer + typed custom property
      val objects = map.layers.get("objects").getOrElse(fail("missing objects layer")).objects
      assertEquals(objects.count, 1)
      val spawn = objects.get("spawn").getOrElse(fail("missing 'spawn' object"))
      assertEquals(spawn.properties.getAs[Integer]("hp").get.intValue(), 100)
    } finally map.close()
  }

  test("TMJ: external tileset + zlib + CSV layers + object layer with properties load end-to-end") {
    val dir = JFiles.createTempDirectory("sge-tmj-fixture")
    writeTilesetPng(dir)

    write(
      dir,
      "tileset.tsj",
      """{ "name": "tiles", "tilewidth": 16, "tileheight": 16, "tilecount": 4, "columns": 2,
        |  "image": "tiles.png", "imagewidth": 32, "imageheight": 32 }""".stripMargin
    )

    val mapFile = write(
      dir,
      "map.tmj",
      s"""{
         |  "type": "map",
         |  "version": "1.10",
         |  "orientation": "orthogonal",
         |  "renderorder": "right-down",
         |  "width": 4, "height": 4, "tilewidth": 16, "tileheight": 16,
         |  "infinite": false,
         |  "properties": [ { "name": "author", "type": "string", "value": "sge-test" } ],
         |  "tilesets": [ { "firstgid": 1, "source": "tileset.tsj" } ],
         |  "layers": [
         |    { "type": "tilelayer", "id": 1, "name": "zlib_layer", "width": 4, "height": 4,
         |      "encoding": "base64", "compression": "zlib", "data": "${zlibBase64(gids)}" },
         |    { "type": "tilelayer", "id": 2, "name": "csv_layer", "width": 4, "height": 4,
         |      "data": [${gids.mkString(",")}] },
         |    { "type": "objectgroup", "id": 3, "name": "objects",
         |      "objects": [ { "id": 1, "name": "spawn", "type": "marker", "x": 16, "y": 32, "width": 8, "height": 8,
         |        "properties": [ { "name": "hp", "type": "int", "value": 100 } ] } ] }
         |  ]
         |}""".stripMargin
    )

    val loader = TmjMapLoader(FileHandleResolver.Absolute())
    val map    = loader.load(mapFile.toAbsolutePath.toString)

    try {
      // Map-level properties
      assertEquals(map.properties.getAs[Integer]("width").get.intValue(), 4)
      assertEquals(map.properties.getAs[Integer]("height").get.intValue(), 4)
      assertEquals(map.properties.getAs[Integer]("tilewidth").get.intValue(), 16)
      assertEquals(map.properties.getAs[Integer]("tileheight").get.intValue(), 16)
      assertEquals(map.properties.getAs[String]("author").getOrElse(fail("missing author prop")), "sge-test")

      // External tileset was parsed and sliced into 4 tiles (gids 1..4)
      assert(map.tileSets.getTile(1).isDefined, "tileset gid 1 missing (external .tsj not loaded)")
      assert(map.tileSets.getTile(4).isDefined, "tileset gid 4 missing (tile slicing incomplete)")

      // zlib base64 and native CSV int-array layers decode to the same gid multiset
      assertEquals(layerGids(tileLayer(map, "zlib_layer")), expectedSorted)
      assertEquals(layerGids(tileLayer(map, "csv_layer")), expectedSorted)

      // Object layer + typed custom property
      val objects = map.layers.get("objects").getOrElse(fail("missing objects layer")).objects
      assertEquals(objects.count, 1)
      val spawn = objects.get("spawn").getOrElse(fail("missing 'spawn' object"))
      assertEquals(spawn.properties.getAs[Integer]("hp").get.intValue(), 100)
    } finally map.close()
  }
}
