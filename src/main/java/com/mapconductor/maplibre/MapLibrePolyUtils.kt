package com.mapconductor.maplibre

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.google.gson.JsonObject
import com.mapconductor.core.features.GeoPoint
import com.mapconductor.core.features.GeoPointInterface
import com.mapconductor.core.geometry.buildUnwrappedPolygonRings
import com.mapconductor.core.geometry.buildUnwrappedPolylinePath
import com.mapconductor.core.geometry.closeRing
import com.mapconductor.maplibre.polygon.MapLibrePolygonLayer
import com.mapconductor.maplibre.polyline.MapLibrePolylineLayer
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Polygon as GLPolygon

internal fun createMapLibreLines(
    id: String,
    points: List<GeoPointInterface>,
    geodesic: Boolean,
    strokeColor: Color,
    strokeWidth: Dp,
    zIndex: Int = 0,
): List<Feature> {
    // unwrap 座標の単一パス。MapLibre GL は ±180 超の経度を扱えるため分割不要（継ぎ目が出ない）。
    val path = buildUnwrappedPolylinePath(points, geodesic)
    if (path.size < 2) return emptyList()
    val pts = path.map { GeoPoint.from(it).toPoint() }
    val fid = "polyline-$id-0"
    return listOf(
        Feature.fromGeometry(
            LineString.fromLngLats(pts),
            JsonObject().apply {
                addProperty(MapLibrePolylineLayer.Prop.STROKE_COLOR, strokeColor.toMapLibreColorString())
                addProperty(MapLibrePolylineLayer.Prop.STROKE_WIDTH, strokeWidth.value)
                addProperty("zIndex", zIndex)
                addProperty("id", fid)
            },
            fid,
        ),
    )
}

fun Color.toMapLibreColorString(): String {
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()
    val alpha = this.alpha
    return "rgba($red, $green, $blue, $alpha)"
}

internal fun createMapLibrePolygons(
    id: String,
    points: List<GeoPointInterface>,
    holes: List<List<GeoPointInterface>> = emptyList(),
    geodesic: Boolean,
    fillColor: Color,
    zIndex: Int,
): List<Feature> {
    // unwrap 座標の外周 1 リング + 全穴。MapLibre GL は ±180 超の経度を扱えるため分割不要で、
    // ±180 跨ぎのポリゴンでも穴を保持できる。
    val polygonRings = buildUnwrappedPolygonRings(points, holes, geodesic)
    val outer = polygonRings.outerRings.firstOrNull() ?: return emptyList()
    val holeRings =
        polygonRings.holeRings.mapNotNull { hole ->
            val closed = closeRing(hole.map { GeoPoint.from(it).toPoint() })
            if (closed.size < 4) null else closed
        }

    val closed = closeRing(outer.map { GeoPoint.from(it).toPoint() })
    val fid = "polygon-$id-0"
    return listOf(
        Feature.fromGeometry(
            GLPolygon.fromLngLats(listOf(closed) + holeRings),
            JsonObject().apply {
                addProperty(MapLibrePolygonLayer.Prop.FILL_COLOR, fillColor.toMapLibreColorString())
                addProperty("zIndex", zIndex)
                addProperty("id", fid)
            },
            fid,
        ),
    )
}
