/*
 * SGE Gauntlet — maps: real .tmj file loading end-to-end (ISS-796, mirrors ISS-767 suite).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet
package probes

import sge.assets.loaders.FileHandleResolver
import sge.maps.tiled.TmjMapLoader

import java.nio.file.{ Files => JFiles }
import scala.collection.mutable.ListBuffer

/** Drives the REAL [[TmjMapLoader]] over a hand-written .tmj (Tiled JSON) fixture: an external .tsj tileset (generated PNG image), a zlib+base64 layer, a native JSON int-array (CSV) layer, and an
  * object layer with a typed custom property. Asserts the decoded map/layer/tile/property content. Non-GPU: Texture creation runs through NoopGraphics headlessly (the image decode + java.util.zip
  * inflate are JVM-only, so this probe lives in scalajvm alongside the net probes).
  */
object TmjLoadProbe extends FeatureProbe {

  override def id: String = "maps/tmj-load"

  override def area: String = "maps"

  override def requiresGpu: Boolean = false

  override def frames: Int = 1

  private val checks = ListBuffer.empty[Check]

  override def init(ctx: ProbeContext): Unit =
    checks.clear()

  override def render(ctx: ProbeContext, frame: Int): Unit = {
    given Sge = ctx.sgeCtx
    val dir   = JFiles.createTempDirectory("sge-gauntlet-tmj")
    TiledFixtures.writeTilesetPng(dir)

    TiledFixtures.write(
      dir,
      "tileset.tsj",
      """{ "name": "tiles", "tilewidth": 16, "tileheight": 16, "tilecount": 4, "columns": 2,
        |  "image": "tiles.png", "imagewidth": 32, "imageheight": 32 }""".stripMargin
    )

    val mapFile = TiledFixtures.write(
      dir,
      "map.tmj",
      s"""{
         |  "type": "map",
         |  "version": "1.10",
         |  "orientation": "orthogonal",
         |  "renderorder": "right-down",
         |  "width": 4, "height": 4, "tilewidth": 16, "tileheight": 16,
         |  "infinite": false,
         |  "properties": [ { "name": "author", "type": "string", "value": "sge-gauntlet" } ],
         |  "tilesets": [ { "firstgid": 1, "source": "tileset.tsj" } ],
         |  "layers": [
         |    { "type": "tilelayer", "id": 1, "name": "zlib_layer", "width": 4, "height": 4,
         |      "encoding": "base64", "compression": "zlib", "data": "${TiledFixtures.zlibBase64(TiledFixtures.Gids)}" },
         |    { "type": "tilelayer", "id": 2, "name": "csv_layer", "width": 4, "height": 4,
         |      "data": [${TiledFixtures.Gids.mkString(",")}] },
         |    { "type": "objectgroup", "id": 3, "name": "objects",
         |      "objects": [ { "id": 1, "name": "spawn", "type": "marker", "x": 16, "y": 32, "width": 8, "height": 8,
         |        "properties": [ { "name": "hp", "type": "int", "value": 100 } ] } ] }
         |  ]
         |}""".stripMargin
    )

    val loader = TmjMapLoader(FileHandleResolver.Absolute())
    val map    = loader.load(mapFile.toAbsolutePath.toString)
    try {
      // Map-level properties parsed from JSON fields and the properties array.
      checks += Check.eq("map-width", 4, map.properties.getAs[Integer]("width").fold(-1)(_.intValue()))
      checks += Check.eq("map-tileheight", 16, map.properties.getAs[Integer]("tileheight").fold(-1)(_.intValue()))
      checks += Check.eq("map-author-property", "sge-gauntlet", map.properties.getAs[String]("author").getOrElse("<missing>"))

      // External .tsj tileset parsed and sliced into 4 tiles (gids 1..4).
      checks += Check.cond("external-tileset-gid1", map.tileSets.getTile(1).isDefined, "gid 1 present", map.tileSets.getTile(1).isDefined.toString)
      checks += Check.cond("external-tileset-gid4", map.tileSets.getTile(4).isDefined, "gid 4 present", map.tileSets.getTile(4).isDefined.toString)

      // zlib+base64 and native JSON int-array layers both decode to the same gid multiset.
      val zlib = TiledFixtures.tileLayer(map, "zlib_layer").fold[List[Int]](Nil)(TiledFixtures.layerGids)
      checks += Check.eq("zlib-layer-gids", TiledFixtures.ExpectedSorted, zlib)
      val csv = TiledFixtures.tileLayer(map, "csv_layer").fold[List[Int]](Nil)(TiledFixtures.layerGids)
      checks += Check.eq("csv-layer-gids", TiledFixtures.ExpectedSorted, csv)

      // Object layer + typed custom property.
      val objects = map.layers.get("objects").fold(0)(_.objects.count)
      checks += Check.eq("object-count", 1, objects)
      val hp = map.layers.get("objects").fold(-1)(l => l.objects.get("spawn").fold(-1)(_.properties.getAs[Integer]("hp").fold(-1)(_.intValue())))
      checks += Check.eq("object-typed-property-hp", 100, hp)
    } finally map.close()
  }

  override def verify(ctx: ProbeContext): List[Check] =
    checks.toList
}
