package com.mapconductor.maplibre

import com.mapconductor.core.controller.OverlayKind
import com.mapconductor.maplibre.marker.MapLibreMarkerOverlayRenderer
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import android.util.Log
import kotlinx.coroutines.launch

/**
 * スタイル（レイヤーとソース）の組み立て。
 *
 * MapLibre はスタイルが読み込み直されるたびにレイヤーが全部消えるため、ここは
 * 何度呼ばれても同じ状態になるように書いてある（`ensureGeoJsonSource` /
 * [MapLibreOverlayStacking.add] はいずれも既存があれば作らない）。層の位置は
 * [MapLibreOverlayStacking] が帯の順（ラスター < GroundImage < ポリゴン < サークル <
 * ポリライン < マーカー）で決める。
 *
 * ポリゴンの z 順は**レイヤーを z 値ごとに用意して**表現する。MapLibre には
 * フィーチャー単位の z 指定が無く、レイヤーの並び順だけが順序を決めるため。
 */
internal fun MapLibreViewController.ensureGeoJsonSource(
    style: Style,
    sourceId: String,
) {
    if (style.getSource(sourceId) != null) return
    try {
        style.addSource(GeoJsonSource(sourceId))
    } catch (e: Exception) {
        Log.w("MapLibre", "Failed to add source: $sourceId (${e.message})")
    }
}

// 層の位置は MapLibreOverlayStacking が決める。以下 2 つは公開 API の互換のために残している（SDK 内では未使用）。
internal fun MapLibreViewController.addLayerSafely(
    style: Style,
    layer: Layer,
    layerId: String,
) {
    if (style.getLayer(layerId) != null) return
    try {
        style.addLayer(layer)
    } catch (e: Exception) {
        if (style.getLayer(layerId) == null) {
            Log.w("MapLibre", "Failed to add layer: $layerId (${e.message})")
        }
    }
}

internal fun MapLibreViewController.addLayerAboveSafely(
    style: Style,
    layer: Layer,
    layerId: String,
    aboveId: String,
) {
    if (style.getLayer(layerId) != null) return
    try {
        style.addLayerAbove(layer, aboveId)
    } catch (_: Exception) {
        if (style.getLayer(layerId) != null) return
        try {
            style.addLayer(layer)
        } catch (e2: Exception) {
            if (style.getLayer(layerId) == null) {
                Log.w("MapLibre", "Failed to add layer: $layerId (${e2.message})")
            }
        }
    }
}

internal fun MapLibreViewController.setupStyle(style: Style) {
    // Store the style instance for future use
    styleInstance = style

    // Log existing layers
    // val topLayerId = style.layers.lastOrNull()?.id

    // Ensure default icon image exists on this style
    (markerController.renderer as MapLibreMarkerOverlayRenderer).ensureDefaultIcon(style)

    // Polygon sources only (layers will be added per zIndex)
    ensureGeoJsonSource(style, polygonController.renderer.outline.layer.sourceId)
    ensureGeoJsonSource(style, polygonController.renderer.fill.layer.sourceId)

    // 層の位置は MapLibreOverlayStacking が帯の順で決める（追加の順に依らない）。
    val stacking = MapLibreOverlayStacking
    val map = holder.map
    val markerRenderer = markerController.renderer as MapLibreMarkerOverlayRenderer

    ensureGeoJsonSource(style, circleController.renderer.layer.sourceId)
    stacking.add(map, style, circleController.renderer.layer.layer, stacking.key(OverlayKind.Circle))
    // 線は塗りの直後（同じ鍵なら後から入れたものが上）。
    stacking.add(map, style, circleController.renderer.layer.strokeLayer, stacking.key(OverlayKind.Circle))

    ensureGeoJsonSource(style, polylineController.renderer.layer.sourceId)
    stacking.add(map, style, polylineController.renderer.layer.layer, stacking.key(OverlayKind.Polyline))

    ensurePolygonZLayers(style)

    ensureGeoJsonSource(style, markerRenderer.markerLayer.sourceId)
    stacking.add(map, style, markerRenderer.markerLayer.layer, stacking.key(OverlayKind.Marker))
    markerRenderer.redraw()

    ensureGeoJsonSource(style, markerRenderer.dragLayer.sourceId)
    stacking.add(map, style, markerRenderer.dragLayer.layer, stacking.MARKER_DRAG)
    markerRenderer.redraw()

    markerEventControllers
        .map { it.renderer as MapLibreMarkerOverlayRenderer }
        .filter { it != markerController.renderer }
        .forEach { renderer ->
            renderer.ensureDefaultIcon(style)
            ensureGeoJsonSource(style, renderer.markerLayer.sourceId)
            stacking.add(map, style, renderer.markerLayer.layer, stacking.key(OverlayKind.Marker))
            ensureGeoJsonSource(style, renderer.dragLayer.sourceId)
            stacking.add(map, style, renderer.dragLayer.layer, stacking.MARKER_DRAG)
            renderer.redraw()
            renderer.drawDragLayer()
        }

    // Force redraw after adding layers
    (markerController.renderer as MapLibreMarkerOverlayRenderer).redraw()
    circleController.renderer.redraw()
    polylineController.renderer.redraw()
    // The polygon sources are new and empty on every style; the data has to
    // be written into them again, like the markers' and the polylines'.
    polygonController.renderer.fill.redraw()
    polygonController.renderer.outline.redraw()
    mainCoroutine.launch {
        groundImageController.reapplyStyle()
        rasterLayerController.reapplyStyle()
    }
    // Last, and after the overlays: anything a vector style adjustment put
    // on the previous style went with it, and this is the only moment the
    // new one is known to be complete.
    styleLoadedListener?.invoke()
}

internal fun MapLibreViewController.ensurePolygonZLayers(style: Style) {
    val fillSourceId = polygonController.renderer.fill.layer.sourceId
    val outlineSourceId = polygonController.renderer.outline.layer.sourceId

    val zSet =
        polygonController.polygonManager
            .allEntities()
            .map { it.state.zIndex }
            .toSet()

    // Remove stale z-indexed layers we previously created
    val toRemove = polygonZLayers.subtract(zSet)
    toRemove.forEach { z ->
        val fillId = "polygon-fill-layer-$z"
        val outlineId = "polygon-outline-layer-$z"
        MapLibreOverlayStacking.remove(holder.map, style, outlineId)
        MapLibreOverlayStacking.remove(holder.map, style, fillId)
    }

    val zList = zSet.toList().sorted()
    zList.forEach { z ->
        val fillId = "polygon-fill-layer-$z"
        val outlineId = "polygon-outline-layer-$z"

        if (style.getLayer(fillId) == null) {
            val fill =
                FillLayer(fillId, fillSourceId).apply {
                    setFilter(
                        Expression.eq(
                            Expression
                                .get("zIndex"),
                            Expression
                                .literal(z),
                        ),
                    )
                    setProperties(
                        PropertyFactory.fillColor(
                            Expression
                                .get("fillColor"),
                        ),
                    )
                }
            MapLibreOverlayStacking.add(holder.map, style, fill, MapLibreOverlayStacking.key(OverlayKind.Polygon, z))
        }

        if (style.getLayer(outlineId) == null) {
            val outline =
                LineLayer(outlineId, outlineSourceId).apply {
                    setFilter(
                        Expression.eq(
                            Expression
                                .get("zIndex"),
                            Expression
                                .literal(z),
                        ),
                    )
                    setProperties(
                        PropertyFactory
                            .lineJoin(Property.LINE_JOIN_ROUND),
                        PropertyFactory
                            .lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineColor(
                            Expression
                                .get("strokeColor"),
                        ),
                        PropertyFactory.lineWidth(
                            Expression
                                .get("strokeWidth"),
                        ),
                    )
                }
            // 塗りの直後（同じ鍵なら後から入れたものが上）。
            MapLibreOverlayStacking.add(holder.map, style, outline, MapLibreOverlayStacking.key(OverlayKind.Polygon, z))
        }
    }
    polygonZLayers.clear()
    polygonZLayers.addAll(zSet)
}
