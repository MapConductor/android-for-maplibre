package com.mapconductor.maplibre.polygon

import com.mapconductor.core.polygon.AbstractPolygonOverlayRenderer
import com.mapconductor.core.polygon.PolygonEntityInterface
import com.mapconductor.core.polygon.PolygonManagerInterface
import com.mapconductor.core.polygon.PolygonState
import com.mapconductor.maplibre.MapLibreActualPolygon
import com.mapconductor.maplibre.MapLibreMapViewHolderInterface
import com.mapconductor.maplibre.createMapLibrePolygons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * MapLibre Polygon Overlay Renderer
 *
 * 使用 GeoJSON hole polygons（与 React SDK 相同的方式）而不是 tile-based masking
 */
class MapLibrePolygonOverlayRenderer(
    val layer: MapLibrePolygonLayer,
    val polygonManager: PolygonManagerInterface<MapLibreActualPolygon>,
    override val holder: MapLibreMapViewHolderInterface,
    override val coroutine: CoroutineScope = CoroutineScope(Dispatchers.Main),
) : AbstractPolygonOverlayRenderer<MapLibreActualPolygon>() {

    override suspend fun onRemove(data: List<PolygonEntityInterface<MapLibreActualPolygon>>) {
        coroutine.launch {
            holder.map.style?.let {
                layer.draw(getAllPolygonEntities(), it)
            }
        }
    }

    override suspend fun onPostProcess() {
        val polygons = getAllPolygonEntities()

        holder.map.style?.let {
            coroutine.launch {
                layer.draw(polygons, it)
            }
        }
    }

    override suspend fun removePolygon(entity: PolygonEntityInterface<MapLibreActualPolygon>) {
        // 不单独删除，通过 onPostProcess 重绘所有多边形
    }

    override suspend fun createPolygon(state: PolygonState): MapLibreActualPolygon? {
        val features = createMapLibrePolygons(
            id = state.id,
            points = state.points,
            holes = state.holes,
            geodesic = state.geodesic,
            fillColor = state.fillColor,
            zIndex = state.zIndex,
        )

        if (features.isEmpty()) {
            return null
        }
        return features  // MapLibreActualPolygon = List<Feature>
    }

    override suspend fun updatePolygonProperties(
        polygon: MapLibreActualPolygon,
        current: PolygonEntityInterface<MapLibreActualPolygon>,
        prev: PolygonEntityInterface<MapLibreActualPolygon>,
    ): MapLibreActualPolygon? {
        val finger = current.fingerPrint
        val prevFinger = prev.fingerPrint

        if (finger != prevFinger) {
            return createPolygon(current.state)
        }
        return polygon
    }

    private fun getAllPolygonEntities(): List<PolygonEntityInterface<MapLibreActualPolygon>> =
        polygonManager.allEntities()
}
