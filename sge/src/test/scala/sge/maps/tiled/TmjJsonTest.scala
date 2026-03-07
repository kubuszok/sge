package sge
package maps
package tiled

import com.github.plokhotnyuk.jsoniter_scala.core.{ readFromString, writeToString }

class TmjJsonTest extends munit.FunSuite {

  // ---------------------------------------------------------------------------
  // TmjMapJson
  // ---------------------------------------------------------------------------

  test("TmjMapJson round-trip") {
    val original = TmjMapJson(
      orientation = Some("orthogonal"),
      width = 10,
      height = 10,
      tilewidth = 32,
      tileheight = 32,
      properties = List(
        TmjPropertyJson(name = "author", tpe = "string", value = sge.utils.Json.Str("test"))
      ),
      tilesets = List(
        TmjTilesetRefJson(firstgid = 1, source = Some("tileset.tsj"))
      ),
      layers = List(
        TmjLayerJson(name = "ground", tpe = "tilelayer", width = 10, height = 10)
      )
    )

    val json     = writeToString(original)
    val restored = readFromString[TmjMapJson](json)
    assertEquals(restored, original)
  }

  test("parse: minimal orthogonal map with tile layer") {
    val json =
      """{
        |  "orientation": "orthogonal",
        |  "width": 4,
        |  "height": 4,
        |  "tilewidth": 16,
        |  "tileheight": 16,
        |  "tilesets": [
        |    {"firstgid": 1, "source": "terrain.tsj"}
        |  ],
        |  "layers": [
        |    {
        |      "name": "ground",
        |      "type": "tilelayer",
        |      "width": 4,
        |      "height": 4,
        |      "data": [1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4],
        |      "opacity": 1.0,
        |      "visible": true
        |    }
        |  ]
        |}""".stripMargin

    val m = readFromString[TmjMapJson](json)
    assertEquals(m.orientation, Some("orthogonal"))
    assertEquals(m.width, 4)
    assertEquals(m.height, 4)
    assertEquals(m.tilewidth, 16)
    assertEquals(m.tileheight, 16)
    assertEquals(m.tilesets.size, 1)
    assertEquals(m.tilesets.head.firstgid, 1)
    assertEquals(m.tilesets.head.source, Some("terrain.tsj"))
    assertEquals(m.layers.size, 1)
    assertEquals(m.layers.head.name, "ground")
    assertEquals(m.layers.head.tpe, "tilelayer")
    assertEquals(m.layers.head.width, 4)
    assertEquals(m.layers.head.height, 4)
    assert(m.layers.head.data.isDefined)
  }

  test("parse: base64-encoded tile layer") {
    val json =
      """{
        |  "width": 2, "height": 2, "tilewidth": 16, "tileheight": 16,
        |  "layers": [{
        |    "name": "bg",
        |    "type": "tilelayer",
        |    "width": 2, "height": 2,
        |    "encoding": "base64",
        |    "data": "AQAAAAMAAAACAAAABAAAAA=="
        |  }]
        |}""".stripMargin

    val layer = readFromString[TmjMapJson](json).layers.head
    assertEquals(layer.encoding, Some("base64"))
    // data should be a Json.Str for base64
    assert(layer.data.isDefined)
  }

  test("parse: object group layer with various object types") {
    val json =
      """{
        |  "width": 10, "height": 10, "tilewidth": 16, "tileheight": 16,
        |  "layers": [{
        |    "name": "objects",
        |    "type": "objectgroup",
        |    "objects": [
        |      {"id": 1, "name": "spawn", "type": "PlayerSpawn", "x": 32.0, "y": 64.0, "width": 16.0, "height": 16.0},
        |      {"id": 2, "name": "zone", "x": 0.0, "y": 0.0, "ellipse": true, "width": 48.0, "height": 48.0},
        |      {"id": 3, "name": "path", "x": 10.0, "y": 10.0,
        |       "polyline": [{"x": 0, "y": 0}, {"x": 50, "y": 0}, {"x": 50, "y": 50}]},
        |      {"id": 4, "name": "marker", "x": 100.0, "y": 100.0, "point": true},
        |      {"id": 5, "name": "tile_obj", "gid": 42, "x": 0, "y": 0, "width": 16, "height": 16}
        |    ]
        |  }]
        |}""".stripMargin

    val layer = readFromString[TmjMapJson](json).layers.head
    assertEquals(layer.tpe, "objectgroup")
    assertEquals(layer.objects.size, 5)

    val spawn = layer.objects(0)
    assertEquals(spawn.id, 1)
    assertEquals(spawn.name, Some("spawn"))
    assertEquals(spawn.tpe, Some("PlayerSpawn"))
    assertEquals(spawn.x, Some(32.0f))
    assertEquals(spawn.y, Some(64.0f))
    assertEquals(spawn.width, Some(16.0f))
    assertEquals(spawn.height, Some(16.0f))

    val ellipse = layer.objects(1)
    assertEquals(ellipse.ellipse, Some(true))

    val polyline = layer.objects(2)
    assertEquals(polyline.polyline.size, 3)
    assertEquals(polyline.polyline(0), TmjPointJson(0f, 0f))
    assertEquals(polyline.polyline(1), TmjPointJson(50f, 0f))
    assertEquals(polyline.polyline(2), TmjPointJson(50f, 50f))

    val point = layer.objects(3)
    assertEquals(point.point, Some(true))

    val tileObj = layer.objects(4)
    assertEquals(tileObj.gid, Some(42L))
  }

  test("parse: image layer") {
    val json =
      """{
        |  "width": 10, "height": 10, "tilewidth": 16, "tileheight": 16,
        |  "layers": [{
        |    "name": "background",
        |    "type": "imagelayer",
        |    "image": "sky.png",
        |    "offsetx": 0,
        |    "offsety": -100,
        |    "opacity": 0.8,
        |    "repeatx": 1,
        |    "repeaty": 0
        |  }]
        |}""".stripMargin

    val layer = readFromString[TmjMapJson](json).layers.head
    assertEquals(layer.tpe, "imagelayer")
    assertEquals(layer.image, Some("sky.png"))
    assertEquals(layer.offsety, -100f)
    assertEquals(layer.opacity, 0.8f)
    assertEquals(layer.repeatx, 1)
    assertEquals(layer.repeaty, 0)
  }

  test("parse: group layer with nested layers (recursive)") {
    val json =
      """{
        |  "width": 10, "height": 10, "tilewidth": 16, "tileheight": 16,
        |  "layers": [{
        |    "name": "group1",
        |    "type": "group",
        |    "layers": [
        |      {"name": "inner_tiles", "type": "tilelayer", "width": 10, "height": 10},
        |      {"name": "inner_group", "type": "group", "layers": [
        |        {"name": "deep", "type": "objectgroup"}
        |      ]}
        |    ]
        |  }]
        |}""".stripMargin

    val group = readFromString[TmjMapJson](json).layers.head
    assertEquals(group.tpe, "group")
    assertEquals(group.layers.size, 2)
    assertEquals(group.layers(0).name, "inner_tiles")
    assertEquals(group.layers(1).name, "inner_group")
    assertEquals(group.layers(1).layers.size, 1)
    assertEquals(group.layers(1).layers.head.name, "deep")
  }

  test("parse: layer with parallax and tint") {
    val json =
      """{
        |  "width": 10, "height": 10, "tilewidth": 16, "tileheight": 16,
        |  "layers": [{
        |    "name": "clouds",
        |    "type": "tilelayer",
        |    "width": 10, "height": 10,
        |    "parallaxx": 0.5,
        |    "parallaxy": 0.3,
        |    "tintcolor": "#80ffffff",
        |    "visible": false
        |  }]
        |}""".stripMargin

    val layer = readFromString[TmjMapJson](json).layers.head
    assertEquals(layer.parallaxx, 0.5f)
    assertEquals(layer.parallaxy, 0.3f)
    assertEquals(layer.tintcolor, Some("#80ffffff"))
    assertEquals(layer.visible, false)
  }

  test("parse: properties with various types") {
    val json =
      """{
        |  "width": 1, "height": 1, "tilewidth": 16, "tileheight": 16,
        |  "properties": [
        |    {"name": "title", "type": "string", "value": "My Map"},
        |    {"name": "difficulty", "type": "int", "value": 3},
        |    {"name": "gravity", "type": "float", "value": 9.81},
        |    {"name": "dark", "type": "bool", "value": true}
        |  ]
        |}""".stripMargin

    val props = readFromString[TmjMapJson](json).properties
    assertEquals(props.size, 4)
    assertEquals(props(0).name, "title")
    assertEquals(props(0).tpe, "string")
    assertEquals(props(1).name, "difficulty")
    assertEquals(props(1).tpe, "int")
    assertEquals(props(3).name, "dark")
    assertEquals(props(3).tpe, "bool")
  }

  test("parse: hexagonal map with stagger settings") {
    val json =
      """{
        |  "orientation": "hexagonal",
        |  "width": 8, "height": 8,
        |  "tilewidth": 32, "tileheight": 32,
        |  "hexsidelength": 16,
        |  "staggeraxis": "y",
        |  "staggerindex": "odd",
        |  "backgroundcolor": "#ff336699"
        |}""".stripMargin

    val m = readFromString[TmjMapJson](json)
    assertEquals(m.orientation, Some("hexagonal"))
    assertEquals(m.hexsidelength, 16)
    assertEquals(m.staggeraxis, Some("y"))
    assertEquals(m.staggerindex, Some("odd"))
    assertEquals(m.backgroundcolor, Some("#ff336699"))
  }

  test("parse: text object") {
    val json =
      """{
        |  "width": 10, "height": 10, "tilewidth": 16, "tileheight": 16,
        |  "layers": [{
        |    "name": "labels",
        |    "type": "objectgroup",
        |    "objects": [{
        |      "id": 1, "name": "sign", "x": 50, "y": 50,
        |      "text": {
        |        "text": "Hello World",
        |        "fontfamily": "Arial",
        |        "pixelSize": 24,
        |        "halign": "center",
        |        "valign": "top",
        |        "bold": true,
        |        "italic": false,
        |        "wrap": true,
        |        "color": "#ff000000"
        |      }
        |    }]
        |  }]
        |}""".stripMargin

    val obj = readFromString[TmjMapJson](json).layers.head.objects.head
    assert(obj.text.isDefined)
    val txt = obj.text.get
    assertEquals(txt.text, Some("Hello World"))
    assertEquals(txt.fontfamily, Some("Arial"))
    assertEquals(txt.pixelSize, Some(24))
    assertEquals(txt.halign, Some("center"))
    assertEquals(txt.valign, Some("top"))
    assertEquals(txt.bold, Some(true))
    assertEquals(txt.italic, Some(false))
    assertEquals(txt.wrap, Some(true))
    assertEquals(txt.color, Some("#ff000000"))
  }

  test("parse: unknown fields are silently ignored") {
    val json =
      """{
        |  "width": 1, "height": 1, "tilewidth": 16, "tileheight": 16,
        |  "renderorder": "right-down",
        |  "infinite": false,
        |  "nextlayerid": 5,
        |  "nextobjectid": 10,
        |  "compressionlevel": -1
        |}""".stripMargin

    val m = readFromString[TmjMapJson](json)
    assertEquals(m.width, 1)
  }

  // ---------------------------------------------------------------------------
  // TmjTilesetRefJson
  // ---------------------------------------------------------------------------

  test("TmjTilesetRefJson round-trip") {
    val original = TmjTilesetRefJson(
      firstgid = 1,
      name = Some("terrain"),
      image = Some("terrain.png"),
      imagewidth = 256,
      imageheight = 256,
      tilewidth = 32,
      tileheight = 32,
      spacing = 2,
      margin = 1,
      tileoffset = Some(TmjTileOffsetJson(x = 0, y = -16)),
      tiles = List(
        TmjTileJson(id = 0, tpe = Some("grass")),
        TmjTileJson(id = 1,
                    animation = List(
                      TmjAnimFrameJson(tileid = 1, duration = 200),
                      TmjAnimFrameJson(tileid = 2, duration = 200)
                    )
        )
      )
    )

    val json     = writeToString(original)
    val restored = readFromString[TmjTilesetRefJson](json)
    assertEquals(restored, original)
  }

  test("parse: external tileset reference (firstgid + source only)") {
    val json = """{"firstgid": 1, "source": "tileset.tsj"}"""
    val ts   = readFromString[TmjTilesetRefJson](json)
    assertEquals(ts.firstgid, 1)
    assertEquals(ts.source, Some("tileset.tsj"))
    assertEquals(ts.name, None)
    assertEquals(ts.image, None)
  }

  test("parse: inline tileset with tiles and animation") {
    val json =
      """{
        |  "firstgid": 1,
        |  "name": "water",
        |  "image": "water.png",
        |  "imagewidth": 128,
        |  "imageheight": 32,
        |  "tilewidth": 32,
        |  "tileheight": 32,
        |  "spacing": 0,
        |  "margin": 0,
        |  "tileoffset": {"x": 0, "y": -8},
        |  "tiles": [
        |    {"id": 0, "type": "water_still",
        |     "animation": [
        |       {"tileid": 0, "duration": 250},
        |       {"tileid": 1, "duration": 250},
        |       {"tileid": 2, "duration": 250},
        |       {"tileid": 3, "duration": 250}
        |     ]},
        |    {"id": 4, "probability": "0.5",
        |     "properties": [
        |       {"name": "depth", "type": "float", "value": 1.5}
        |     ]}
        |  ]
        |}""".stripMargin

    val ts = readFromString[TmjTilesetRefJson](json)
    assertEquals(ts.name, Some("water"))
    assertEquals(ts.imagewidth, 128)
    assertEquals(ts.tileoffset, Some(TmjTileOffsetJson(0, -8)))
    assertEquals(ts.tiles.size, 2)

    val tile0 = ts.tiles(0)
    assertEquals(tile0.tpe, Some("water_still"))
    assertEquals(tile0.animation.size, 4)
    assertEquals(tile0.animation(0).tileid, 0)
    assertEquals(tile0.animation(0).duration, 250)

    val tile4 = ts.tiles(1)
    assertEquals(tile4.id, 4)
    assertEquals(tile4.probability, Some("0.5"))
    assertEquals(tile4.properties.size, 1)
    assertEquals(tile4.properties.head.name, "depth")
  }

  test("parse: tile with collision object group") {
    val json =
      """{
        |  "firstgid": 1,
        |  "name": "blocks",
        |  "tilewidth": 16, "tileheight": 16,
        |  "tiles": [{
        |    "id": 0,
        |    "objectgroup": {
        |      "objects": [
        |        {"id": 1, "x": 0, "y": 0, "width": 16, "height": 16}
        |      ]
        |    }
        |  }]
        |}""".stripMargin

    val tile = readFromString[TmjTilesetRefJson](json).tiles.head
    assert(tile.objectgroup.isDefined)
    assertEquals(tile.objectgroup.get.objects.size, 1)
    assertEquals(tile.objectgroup.get.objects.head.width, Some(16f))
  }

  test("parse: tile with per-tile image") {
    val json =
      """{
        |  "firstgid": 1,
        |  "name": "collection",
        |  "tilewidth": 32, "tileheight": 32,
        |  "tiles": [
        |    {"id": 0, "image": "tree.png"},
        |    {"id": 1, "image": "rock.png"}
        |  ]
        |}""".stripMargin

    val tiles = readFromString[TmjTilesetRefJson](json).tiles
    assertEquals(tiles(0).image, Some("tree.png"))
    assertEquals(tiles(1).image, Some("rock.png"))
  }

  // ---------------------------------------------------------------------------
  // TmjTemplateJson
  // ---------------------------------------------------------------------------

  test("TmjTemplateJson round-trip") {
    val original = TmjTemplateJson(
      tpe = "template",
      `object` = TmjObjectJson(
        id = 0,
        name = Some("enemy"),
        tpe = Some("Enemy"),
        width = Some(32f),
        height = Some(32f),
        gid = Some(100L)
      )
    )

    val json     = writeToString(original)
    val restored = readFromString[TmjTemplateJson](json)
    assertEquals(restored, original)
  }

  test("parse: template with polygon object") {
    val json =
      """{
        |  "type": "template",
        |  "object": {
        |    "id": 0,
        |    "name": "trigger",
        |    "type": "TriggerZone",
        |    "x": 0, "y": 0,
        |    "polygon": [
        |      {"x": 0, "y": 0},
        |      {"x": 100, "y": 0},
        |      {"x": 100, "y": 50},
        |      {"x": 0, "y": 50}
        |    ],
        |    "properties": [
        |      {"name": "script", "type": "string", "value": "on_enter.lua"}
        |    ]
        |  }
        |}""".stripMargin

    val tmpl = readFromString[TmjTemplateJson](json)
    assertEquals(tmpl.tpe, "template")
    val obj = tmpl.`object`
    assertEquals(obj.name, Some("trigger"))
    assertEquals(obj.tpe, Some("TriggerZone"))
    assertEquals(obj.polygon.size, 4)
    assertEquals(obj.polygon(2), TmjPointJson(100f, 50f))
    assertEquals(obj.properties.size, 1)
  }

  test("parse: template with text object") {
    val json =
      """{
        |  "type": "template",
        |  "object": {
        |    "id": 0,
        |    "name": "label",
        |    "text": {"text": "Hello", "fontfamily": "sans-serif", "pixelSize": 16}
        |  }
        |}""".stripMargin

    val obj = readFromString[TmjTemplateJson](json).`object`
    assert(obj.text.isDefined)
    assertEquals(obj.text.get.text, Some("Hello"))
    assertEquals(obj.text.get.fontfamily, Some("sans-serif"))
    assertEquals(obj.text.get.pixelSize, Some(16))
  }

  test("parse: object with template reference") {
    val json =
      """{
        |  "width": 10, "height": 10, "tilewidth": 16, "tileheight": 16,
        |  "layers": [{
        |    "name": "objs",
        |    "type": "objectgroup",
        |    "objects": [{
        |      "id": 7,
        |      "template": "templates/enemy.tj",
        |      "x": 128, "y": 256
        |    }]
        |  }]
        |}""".stripMargin

    val obj = readFromString[TmjMapJson](json).layers.head.objects.head
    assertEquals(obj.id, 7)
    assertEquals(obj.template, Some("templates/enemy.tj"))
    assertEquals(obj.x, Some(128f))
    assertEquals(obj.y, Some(256f))
  }
}
