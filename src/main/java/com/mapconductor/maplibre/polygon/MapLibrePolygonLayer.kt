package com.mapconductor.maplibre.polygon

import com.mapconductor.core.polygon.PolygonEntityInterface
import com.mapconductor.maplibre.MapLibreActualPolygon
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection

class MapLibrePolygonLayer(
    val sourceId: String,
    val layerId: String,
) {
    object Prop {
        const val FILL_COLOR = "fillColor"
        const val Z_INDEX = "zIndex"
    }

    val source: GeoJsonSource =
        GeoJsonSource(
            sourceId,
            FeatureCollection.fromFeatures(emptyList()),
        )

    // A fresh native layer on every read. MapLibre releases a Layer's core
    // object into the style it is added to, and the Java object can never be
    // added again ("Cannot add layer twice") -- so the one built for the
    // first style was silently refused by every style loaded after it, and
    // a design change lost the overlays. Only `setupStyle` reads this, once
    // per style load.
    val layer: FillLayer
        get() =
            FillLayer(layerId, sourceId).apply {
            setProperties(
                fillColor(get(Prop.FILL_COLOR)),
            )
        }

    fun draw(
        entities: List<PolygonEntityInterface<MapLibreActualPolygon>>,
        style: org.maplibre.android.maps.Style,
    ) {
        val features: List<Feature> =
            entities
                .sortedBy { it.state.zIndex }
                .flatMap { it.polygon }

        val styleSource =
            try {
                style.getSource(sourceId)
            } catch (e: IllegalStateException) {
                null
            }

        if (styleSource is GeoJsonSource) {
            try {
                styleSource.setGeoJson(FeatureCollection.fromFeatures(features))
                return
            } catch (_: IllegalStateException) {
                // fall through to fallback
            }
        }
        // Fallback to local source instance if style source is unavailable
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }
}
