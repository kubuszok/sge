package sge.port

/** The values of the libGDX policy that more than one step of [[LibgdxLadder]] reads. */
object LibgdxPolicy {

  /** `Class#getResource` answers a `java.net.URL`, which Scala Native's javalib lacks (its linker: `Unknown type java.net.URL`, from `FileHandle.exists`); the twin `getResourceAsStream` it has.
    * libGDX's only use of the URL is an existence probe (`!= null`), so the probe is respelled and the stream closed — sge's hand port's own spelling. `PortabilityCheck` still counts the site on
    * Scala.js, which has neither method and answers through its FileHandle platform row.
    */
  val ClasspathProbeCalls: Map[String, String] = Map(
    "java.lang.Class#getResource(String)" ->
      "({{ val bpResource = {recv}.getResourceAsStream({arg0}); if (bpResource != null) bpResource.close(); bpResource }})"
  )

  /** `Matrix4`'s `static final` scratch instances (`quat`, `l_vez`, `tmpMat`, …): java shares one per class across every thread, so two threads composing matrices corrupt each other's temporaries;
    * `ThreadConfinedStaticsTransform` confines each to the reading thread.
    */
  val ThreadConfinedScratch: Set[String] =
    Set("quat", "quat2", "l_vez", "l_vex", "l_vey", "tmpVec", "tmpMat", "right", "tmpForward", "tmpUp").map(f => s"com.badlogic.gdx.math.Matrix4#$f")

  /** Which pairs collapse to a plain `var`/`val` instead of a `def` pair — `def-pair` is the default for everything not named here. The phase refuses a mismatch rather than picking (a counted
    * `idiom(refused)` row). Declared even for PERMANENT refusals so the run's denominator stays honest. `MapLayer#opacity` deliberately absent: its getter is computed, never a stored value.
    */
  def beanPropertyTargets: Map[String, balticporter.transform.BeanPropertyTransform.Target] = {
    import balticporter.transform.BeanPropertyTransform.Target
    Map(
      // -- `var`: a get/set pair, where a public `var` is exactly the surface java published
      "com.badlogic.gdx.graphics.profiling.GLProfiler#listener" -> Target.Var,
      "com.badlogic.gdx.maps.MapLayer#name" -> Target.Var,
      "com.badlogic.gdx.maps.MapLayer#parallaxX" -> Target.Var,
      "com.badlogic.gdx.maps.MapLayer#parallaxY" -> Target.Var,
      "com.badlogic.gdx.maps.MapLayer#visible" -> Target.Var,
      "com.badlogic.gdx.maps.MapObject#color" -> Target.Var,
      "com.badlogic.gdx.maps.MapObject#name" -> Target.Var,
      "com.badlogic.gdx.maps.MapObject#opacity" -> Target.Var,
      "com.badlogic.gdx.maps.MapObject#visible" -> Target.Var,
      "com.badlogic.gdx.maps.objects.PolygonMapObject#polygon" -> Target.Var,
      "com.badlogic.gdx.maps.objects.PolylineMapObject#polyline" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#bold" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#fontFamily" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#horizontalAlign" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#italic" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#kerning" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#pixelSize" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#rotation" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#strikeout" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#text" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#underline" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#verticalAlign" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextMapObject#wrap" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextureMapObject#originX" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextureMapObject#originY" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextureMapObject#rotation" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextureMapObject#scaleX" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextureMapObject#scaleY" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextureMapObject#textureRegion" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextureMapObject#x" -> Target.Var,
      "com.badlogic.gdx.maps.objects.TextureMapObject#y" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#region" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#repeatX" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#repeatY" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#x" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#y" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.TiledMapTileSet#name" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject#flipHorizontally" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject#flipVertically" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject#tile" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.renderers.HexagonalTiledMapRenderer#hexSideLength" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.renderers.HexagonalTiledMapRenderer#staggerAxisX" -> Target.Var,
      "com.badlogic.gdx.maps.tiled.renderers.HexagonalTiledMapRenderer#staggerIndexEven" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#button" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#tapCount" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#tapSquareSize" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#dragTime" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.DragListener#button" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.DragListener#tapSquareSize" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.Selection#multiple" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.Selection#programmaticChangeEvents" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.Selection#required" -> Target.Var,
      "com.badlogic.gdx.scenes.scene2d.utils.Selection#toggle" -> Target.Var,
      // -- `val`: a get-only property over storage the declaration fills and nothing reassigns
      "com.badlogic.gdx.maps.Map#layers" -> Target.Val,
      "com.badlogic.gdx.maps.Map#properties" -> Target.Val,
      "com.badlogic.gdx.maps.MapLayer#objects" -> Target.Val,
      "com.badlogic.gdx.maps.MapLayer#properties" -> Target.Val,
      "com.badlogic.gdx.maps.MapObject#properties" -> Target.Val,
      "com.badlogic.gdx.math.collision.OrientedBoundingBox#bounds" -> Target.Val,
      "com.badlogic.gdx.math.collision.OrientedBoundingBox#vertices" -> Target.Val,
      // -- and the get-only ones the engine REFUSES under `MutableStorage`, declared ANYWAY:
      //    the port's answer for the whole collapsible population then lives in one place,
      //    and the lane says WHY each of these is still a `def` pair rather than saying
      //    nothing about it. `idiom(refused)` is a DENOMINATOR and not a work list, which is
      //    what makes a permanent refusal belong in it.
      "com.badlogic.gdx.graphics.Cubemap#cubemapData" -> Target.Val,
      "com.badlogic.gdx.graphics.Texture#textureData" -> Target.Val,
      "com.badlogic.gdx.graphics.g2d.SpriteCache#customShader" -> Target.Val,
      "com.badlogic.gdx.graphics.profiling.GLProfiler#enabled" -> Target.Val,
      "com.badlogic.gdx.maps.objects.CircleMapObject#circle" -> Target.Val,
      "com.badlogic.gdx.maps.objects.EllipseMapObject#ellipse" -> Target.Val,
      "com.badlogic.gdx.maps.objects.PointMapObject#point" -> Target.Val,
      "com.badlogic.gdx.maps.objects.RectangleMapObject#rectangle" -> Target.Val,
      "com.badlogic.gdx.maps.objects.TextMapObject#rectangle" -> Target.Val,
      "com.badlogic.gdx.maps.tiled.TiledMapTileSet#properties" -> Target.Val,
      "com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile#frameTiles" -> Target.Val,
      "com.badlogic.gdx.math.Polygon#rotation" -> Target.Val,
      "com.badlogic.gdx.math.Polygon#vertices" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.ui.List#cullingArea" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#fadeScrollBars" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#overscrollDistance" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#variableSizeKnobs" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.ui.SelectBox#clickListener" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable#name" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#pressed" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#pressedButton" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#pressedPointer" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#touchDownX" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#touchDownY" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#currentDragActor" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#currentPayload" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#currentSource" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.DragListener#dragging" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable#align" -> Target.Val,
      "com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable#scale" -> Target.Val
    )
  }

  /** the harvested pairs. The key is the emitted property in the upstream namespace (the package rename runs last); the value names the accessors explicitly, because a hand port's names are not
    * always bean-derivable (`getDragActor` -> `currentDragActor`) and a never-fired report needs them as data.
    */
  def beanPropertyPairs: Map[String, String] = Map(
    // -- com.badlogic.gdx.audio --
    "com.badlogic.gdx.audio.AudioDevice#latency" -> "getLatency",
    "com.badlogic.gdx.audio.Music#playing" -> "isPlaying",
    "com.badlogic.gdx.audio.Music#looping" -> "isLooping/setLooping",
    "com.badlogic.gdx.audio.Music#volume" -> "getVolume/setVolume",
    "com.badlogic.gdx.audio.Music#position" -> "getPosition/setPosition",
    // -- com.badlogic.gdx.Graphics -- The four GL accessors are here for a MECHANICAL reason:
    // [[globalsToContext]]'s member map re-points `Gdx.gl20` at the path `graphics.gl20`, and a
    // path segment is an IDENTIFIER — without these four the five `gl*` statics would have
    // nowhere to go. (`Gdx.gl` aliases `gl20` upstream and maps to the same path, so four pairs
    // serve five statics.)
    "com.badlogic.gdx.Graphics#gl20" -> "getGL20/setGL20",
    "com.badlogic.gdx.Graphics#gl30" -> "getGL30/setGL30",
    "com.badlogic.gdx.Graphics#gl31" -> "getGL31/setGL31",
    "com.badlogic.gdx.Graphics#gl32" -> "getGL32/setGL32",
    // -- com.badlogic.gdx.graphics --
    "com.badlogic.gdx.graphics.Cubemap#cubemapData" -> "getCubemapData",
    // `isManaged` is declared ABSTRACT on `GLTexture` and the phase renames the whole override
    // COMPONENT, so the interface entry covers `Cubemap`, `Texture` and `TextureArray` — three
    // per-implementor entries said the same thing three times.
    "com.badlogic.gdx.graphics.GLTexture#managed" -> "isManaged",
    "com.badlogic.gdx.graphics.Texture#textureData" -> "getTextureData",
    "com.badlogic.gdx.graphics.VertexAttribute#key" -> "getKey",
    "com.badlogic.gdx.graphics.VertexAttributes#mask" -> "getMask",
    "com.badlogic.gdx.graphics.VertexAttributes#maskWithSizePacked" -> "getMaskWithSizePacked",
    // -- com.badlogic.gdx.graphics.g2d --
    "com.badlogic.gdx.graphics.g2d.SpriteCache#customShader" -> "getCustomShader",
    // -- com.badlogic.gdx.graphics.profiling --
    "com.badlogic.gdx.graphics.profiling.GLProfiler#listener" -> "getListener/setListener",
    "com.badlogic.gdx.graphics.profiling.GLProfiler#enabled" -> "isEnabled",
    // -- com.badlogic.gdx.maps --
    "com.badlogic.gdx.maps.Map#layers" -> "getLayers",
    "com.badlogic.gdx.maps.Map#properties" -> "getProperties",
    "com.badlogic.gdx.maps.MapLayer#name" -> "getName/setName",
    "com.badlogic.gdx.maps.MapLayer#visible" -> "isVisible/setVisible",
    "com.badlogic.gdx.maps.MapLayer#objects" -> "getObjects",
    "com.badlogic.gdx.maps.MapLayer#properties" -> "getProperties",
    "com.badlogic.gdx.maps.MapLayer#parallaxX" -> "getParallaxX/setParallaxX",
    "com.badlogic.gdx.maps.MapLayer#parallaxY" -> "getParallaxY/setParallaxY",
    "com.badlogic.gdx.maps.MapLayer#opacity" -> "getOpacity/setOpacity",
    "com.badlogic.gdx.maps.MapLayer#combinedTintColor" -> "getCombinedTintColor",
    "com.badlogic.gdx.maps.MapLayer#tintColor" -> "getTintColor/setTintColor",
    "com.badlogic.gdx.maps.MapLayer#offsetX" -> "getOffsetX/setOffsetX",
    "com.badlogic.gdx.maps.MapLayer#offsetY" -> "getOffsetY/setOffsetY",
    "com.badlogic.gdx.maps.MapLayer#renderOffsetX" -> "getRenderOffsetX",
    "com.badlogic.gdx.maps.MapLayer#renderOffsetY" -> "getRenderOffsetY",
    "com.badlogic.gdx.maps.MapLayer#parent" -> "getParent/setParent",
    "com.badlogic.gdx.maps.MapObject#name" -> "getName/setName",
    "com.badlogic.gdx.maps.MapObject#color" -> "getColor/setColor",
    "com.badlogic.gdx.maps.MapObject#opacity" -> "getOpacity/setOpacity",
    "com.badlogic.gdx.maps.MapObject#visible" -> "isVisible/setVisible",
    "com.badlogic.gdx.maps.MapObject#properties" -> "getProperties",
    // -- com.badlogic.gdx.maps.objects --
    "com.badlogic.gdx.maps.objects.CircleMapObject#circle" -> "getCircle",
    "com.badlogic.gdx.maps.objects.EllipseMapObject#ellipse" -> "getEllipse",
    "com.badlogic.gdx.maps.objects.PointMapObject#point" -> "getPoint",
    "com.badlogic.gdx.maps.objects.PolygonMapObject#polygon" -> "getPolygon/setPolygon",
    "com.badlogic.gdx.maps.objects.PolylineMapObject#polyline" -> "getPolyline/setPolyline",
    "com.badlogic.gdx.maps.objects.RectangleMapObject#rectangle" -> "getRectangle",
    "com.badlogic.gdx.maps.objects.TextMapObject#rectangle" -> "getRectangle",
    "com.badlogic.gdx.maps.objects.TextMapObject#rotation" -> "getRotation/setRotation",
    "com.badlogic.gdx.maps.objects.TextMapObject#text" -> "getText/setText",
    "com.badlogic.gdx.maps.objects.TextMapObject#pixelSize" -> "getPixelSize/setPixelSize",
    "com.badlogic.gdx.maps.objects.TextMapObject#fontFamily" -> "getFontFamily/setFontFamily",
    "com.badlogic.gdx.maps.objects.TextMapObject#bold" -> "isBold/setBold",
    "com.badlogic.gdx.maps.objects.TextMapObject#italic" -> "isItalic/setItalic",
    "com.badlogic.gdx.maps.objects.TextMapObject#underline" -> "isUnderline/setUnderline",
    "com.badlogic.gdx.maps.objects.TextMapObject#strikeout" -> "isStrikeout/setStrikeout",
    "com.badlogic.gdx.maps.objects.TextMapObject#kerning" -> "isKerning/setKerning",
    "com.badlogic.gdx.maps.objects.TextMapObject#wrap" -> "isWrap/setWrap",
    "com.badlogic.gdx.maps.objects.TextMapObject#horizontalAlign" -> "getHorizontalAlign/setHorizontalAlign",
    "com.badlogic.gdx.maps.objects.TextMapObject#verticalAlign" -> "getVerticalAlign/setVerticalAlign",
    "com.badlogic.gdx.maps.objects.TextureMapObject#x" -> "getX/setX",
    "com.badlogic.gdx.maps.objects.TextureMapObject#y" -> "getY/setY",
    "com.badlogic.gdx.maps.objects.TextureMapObject#originX" -> "getOriginX/setOriginX",
    "com.badlogic.gdx.maps.objects.TextureMapObject#originY" -> "getOriginY/setOriginY",
    "com.badlogic.gdx.maps.objects.TextureMapObject#scaleX" -> "getScaleX/setScaleX",
    "com.badlogic.gdx.maps.objects.TextureMapObject#scaleY" -> "getScaleY/setScaleY",
    "com.badlogic.gdx.maps.objects.TextureMapObject#rotation" -> "getRotation/setRotation",
    "com.badlogic.gdx.maps.objects.TextureMapObject#textureRegion" -> "getTextureRegion/setTextureRegion",
    // -- com.badlogic.gdx.maps.tiled --
    "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#region" -> "getTextureRegion/setTextureRegion",
    "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#x" -> "getX/setX",
    "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#y" -> "getY/setY",
    "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#repeatX" -> "isRepeatX/setRepeatX",
    "com.badlogic.gdx.maps.tiled.TiledMapImageLayer#repeatY" -> "isRepeatY/setRepeatY",
    "com.badlogic.gdx.maps.tiled.TiledMapTileSet#name" -> "getName/setName",
    "com.badlogic.gdx.maps.tiled.TiledMapTileSet#properties" -> "getProperties",
    // `TiledMapTile` DECLARES all seven, and the phase renames the whole override component, so
    // one entry each covers `AnimatedTiledMapTile` and `StaticTiledMapTile` — thirteen
    // per-implementor entries said the same thing twice over.
    "com.badlogic.gdx.maps.tiled.TiledMapTile#id" -> "getId/setId",
    "com.badlogic.gdx.maps.tiled.TiledMapTile#blendMode" -> "getBlendMode/setBlendMode",
    "com.badlogic.gdx.maps.tiled.TiledMapTile#textureRegion" -> "getTextureRegion/setTextureRegion",
    "com.badlogic.gdx.maps.tiled.TiledMapTile#offsetX" -> "getOffsetX/setOffsetX",
    "com.badlogic.gdx.maps.tiled.TiledMapTile#offsetY" -> "getOffsetY/setOffsetY",
    "com.badlogic.gdx.maps.tiled.TiledMapTile#properties" -> "getProperties",
    "com.badlogic.gdx.maps.tiled.TiledMapTile#objects" -> "getObjects",
    // -- com.badlogic.gdx.maps.tiled.objects --
    "com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject#flipHorizontally" -> "isFlipHorizontally/setFlipHorizontally",
    "com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject#flipVertically" -> "isFlipVertically/setFlipVertically",
    "com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject#tile" -> "getTile/setTile",
    // -- com.badlogic.gdx.maps.tiled.renderers --
    "com.badlogic.gdx.maps.tiled.renderers.HexagonalTiledMapRenderer#staggerAxisX" -> "isStaggerAxisX/setStaggerAxisX",
    "com.badlogic.gdx.maps.tiled.renderers.HexagonalTiledMapRenderer#staggerIndexEven" -> "isStaggerIndexEven/setStaggerIndexEven",
    "com.badlogic.gdx.maps.tiled.renderers.HexagonalTiledMapRenderer#hexSideLength" -> "getHexSideLength/setHexSideLength",
    // -- com.badlogic.gdx.maps.tiled.tiles --
    "com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile#currentFrameIndex" -> "getCurrentFrameIndex",
    "com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile#currentFrame" -> "getCurrentFrame",
    "com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile#animationIntervals" -> "getAnimationIntervals/setAnimationIntervals",
    "com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile#frameTiles" -> "getFrameTiles",
    // -- com.badlogic.gdx.math --
    "com.badlogic.gdx.math.Polygon#vertices" -> "getVertices",
    "com.badlogic.gdx.math.Polygon#transformedVertices" -> "getTransformedVertices",
    "com.badlogic.gdx.math.Polygon#vertexCount" -> "getVertexCount",
    "com.badlogic.gdx.math.Polygon#boundingRectangle" -> "getBoundingRectangle",
    "com.badlogic.gdx.math.Polygon#rotation" -> "getRotation",
    // -- com.badlogic.gdx.math.collision --
    "com.badlogic.gdx.math.collision.OrientedBoundingBox#vertices" -> "getVertices",
    "com.badlogic.gdx.math.collision.OrientedBoundingBox#bounds" -> "getBounds",
    // -- com.badlogic.gdx.scenes.scene2d.ui --
    "com.badlogic.gdx.scenes.scene2d.ui.List#cullingArea" -> "getCullingArea",
    "com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#scrollX" -> "getScrollX",
    "com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#scrollY" -> "getScrollY",
    "com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#overscrollDistance" -> "getOverscrollDistance",
    "com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#fadeScrollBars" -> "getFadeScrollBars",
    "com.badlogic.gdx.scenes.scene2d.ui.ScrollPane#variableSizeKnobs" -> "getVariableSizeKnobs",
    "com.badlogic.gdx.scenes.scene2d.ui.SelectBox#clickListener" -> "getClickListener",
    // -- com.badlogic.gdx.scenes.scene2d.utils --
    "com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable#name" -> "getName",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#tapSquareSize" -> "getTapSquareSize/setTapSquareSize",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#tapCount" -> "getTapCount/setTapCount",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#button" -> "getButton/setButton",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#pressedButton" -> "getPressedButton",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#pressedPointer" -> "getPressedPointer",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#pressed" -> "isPressed",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#over" -> "isOver",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#touchDownX" -> "getTouchDownX",
    "com.badlogic.gdx.scenes.scene2d.utils.ClickListener#touchDownY" -> "getTouchDownY",
    "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#currentDragActor" -> "getDragActor",
    "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#currentPayload" -> "getDragPayload",
    "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#currentSource" -> "getDragSource",
    "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#dragTime" -> "getDragTime/setDragTime",
    "com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop#dragging" -> "isDragging",
    "com.badlogic.gdx.scenes.scene2d.utils.DragListener#tapSquareSize" -> "getTapSquareSize/setTapSquareSize",
    "com.badlogic.gdx.scenes.scene2d.utils.DragListener#button" -> "getButton/setButton",
    "com.badlogic.gdx.scenes.scene2d.utils.DragListener#dragDistance" -> "getDragDistance",
    "com.badlogic.gdx.scenes.scene2d.utils.DragListener#dragging" -> "isDragging",
    "com.badlogic.gdx.scenes.scene2d.utils.Drawable#leftWidth" -> "getLeftWidth/setLeftWidth",
    "com.badlogic.gdx.scenes.scene2d.utils.Drawable#rightWidth" -> "getRightWidth/setRightWidth",
    "com.badlogic.gdx.scenes.scene2d.utils.Drawable#topHeight" -> "getTopHeight/setTopHeight",
    "com.badlogic.gdx.scenes.scene2d.utils.Drawable#bottomHeight" -> "getBottomHeight/setBottomHeight",
    "com.badlogic.gdx.scenes.scene2d.utils.Drawable#minWidth" -> "getMinWidth/setMinWidth",
    "com.badlogic.gdx.scenes.scene2d.utils.Drawable#minHeight" -> "getMinHeight/setMinHeight",
    "com.badlogic.gdx.scenes.scene2d.utils.Selection#lastSelected" -> "getLastSelected",
    "com.badlogic.gdx.scenes.scene2d.utils.Selection#toggle" -> "getToggle/setToggle",
    "com.badlogic.gdx.scenes.scene2d.utils.Selection#multiple" -> "getMultiple/setMultiple",
    "com.badlogic.gdx.scenes.scene2d.utils.Selection#required" -> "getRequired/setRequired",
    "com.badlogic.gdx.scenes.scene2d.utils.Selection#disabled" -> "isDisabled/setDisabled",
    "com.badlogic.gdx.scenes.scene2d.utils.Selection#programmaticChangeEvents" -> "getProgrammaticChangeEvents/setProgrammaticChangeEvents",
    "com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable#scale" -> "getScale",
    "com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable#align" -> "getAlign"
  )

  /** libGDX's `Align` constants as sge's opaque `Align` (the injected `Align.scala`); with the derive step on, the seeds come from the hand-written reference and the field hints are dropped. */
  val AlignSpec: balticporter.tir.OpaqueSpec =
    balticporter.tir.OpaqueSpec(
      fqn = "com.badlogic.gdx.utils.Align",
      target = balticporter.tir.OpaqueSpec.Target.Existing(
        typeFqn = "sge.utils.Align",
        wrapName = "apply",
        unwrapName = "toInt"
      ),
      hints = Set(
        // 13 fields typed `int align` / `int alignment` / `int columnAlign` / `int rowAlign` /
        // `int labelAlign` / `int lineAlign` across the scene2d UI types. Each is a seed; the
        // propagation discovers every getter, setter, and parameter reachable from them.
        "com.badlogic.gdx.scenes.scene2d.ui.Image#align",
        "com.badlogic.gdx.scenes.scene2d.ui.Label#labelAlign",
        "com.badlogic.gdx.scenes.scene2d.ui.Label#lineAlign",
        "com.badlogic.gdx.scenes.scene2d.ui.List#alignment",
        "com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup#align",
        "com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup#columnAlign",
        "com.badlogic.gdx.scenes.scene2d.ui.Table#align",
        "com.badlogic.gdx.scenes.scene2d.ui.SelectBox#alignment",
        "com.badlogic.gdx.scenes.scene2d.ui.Container#align",
        "com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup#align",
        "com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup#rowAlign",
        "com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable#align",
        "com.badlogic.gdx.scenes.scene2d.actions.MoveToAction#alignment"
      ),
      underlying = balticporter.tir.OpaqueSpec.Primitive.Int,
      // also every slot the reference spells `Align` that no field seed reaches — `GlyphLayout.setText`'s
      // `halign`, `BitmapFont.draw`'s, which java types as plain `int` parameters (sge ISS-770)
      derive = true,
      // a `@Null Integer` slot the nullability step wrapped (`Cell#align`), which the reference spells `Nullable[Align]`
      carriers = Set("lowlevel.Nullable")
    )
}
