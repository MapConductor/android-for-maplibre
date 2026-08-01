package com.mapconductor.maplibre.circle

import com.google.gson.JsonObject
import com.mapconductor.core.calculateZIndex
import com.mapconductor.core.circle.AbstractCircleOverlayRenderer
import com.mapconductor.core.circle.CircleEntityInterface
import com.mapconductor.core.circle.CircleManagerInterface
import com.mapconductor.core.circle.CircleState
import com.mapconductor.core.geometry.circleToRing
import com.mapconductor.core.geometry.closeRing
import com.mapconductor.maplibre.MapLibreActualCircle
import com.mapconductor.maplibre.MapLibreMapViewHolderInterface
import com.mapconductor.maplibre.toMapLibreColorString
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapLibreCircleOverlayRenderer(
    val layer: MapLibreCircleLayer,
    val circleManager: CircleManagerInterface<MapLibreActualCircle>,
    override val holder: MapLibreMapViewHolderInterface,
    override val coroutine: CoroutineScope = CoroutineScope(Dispatchers.Main),
) : AbstractCircleOverlayRenderer<MapLibreActualCircle>() {
    override suspend fun createCircle(state: CircleState): MapLibreActualCircle? = buildFeature(state)

    override suspend fun updateCircleProperties(
        circle: MapLibreActualCircle,
        current: CircleEntityInterface<MapLibreActualCircle>,
        prev: CircleEntityInterface<MapLibreActualCircle>,
    ): MapLibreActualCircle? = buildFeature(current.state)

    override suspend fun removeCircle(entity: CircleEntityInterface<MapLibreActualCircle>) {
        // Remove by redrawing remaining; nothing to do here
    }

    override suspend fun onPostProcess() {
        redraw()
    }

    /** Reapply the current composition after a MapLibre style is created or replaced. */
    fun redraw() {
        val circles = circleManager.allEntities()
        holder.map.style?.let { style ->
            coroutine.launch { layer.draw(circles, style) }
        }
    }

    private fun buildFeature(state: CircleState): Feature =
        Feature.fromGeometry(
            createCirclePolygon(state),
            JsonObject().apply {
                addProperty(MapLibreCircleLayer.Prop.FILL_COLOR, state.fillColor.toMapLibreColorString())
                addProperty(MapLibreCircleLayer.Prop.STROKE_COLOR, state.strokeColor.toMapLibreColorString())
                addProperty(MapLibreCircleLayer.Prop.STROKE_WIDTH, state.strokeWidth.value)
                addProperty(MapLibreCircleLayer.Prop.Z_INDEX, state.zIndex ?: calculateZIndex(state.center))
            },
            "circle-${state.id}",
        )

    /**
     * コア共通の [circleToRing] でリングを生成する。リングは中心経度まわりに連続化
     * （unwrap）されており、MapLibre GL は ±180 を超える経度を扱えるため、±180 を跨ぐ円も
     * 分割せず 1 枚の Polygon として描画できる（子午線の継ぎ目が出ない）。
     */
    private fun createCirclePolygon(state: CircleState): Polygon {
        val ring = circleToRing(state.center, state.radiusMeters, state.geodesic)
        val closed = closeRing(ring.map { Point.fromLngLat(it.longitude, it.latitude) })
        return Polygon.fromLngLats(listOf(closed))
    }
}
