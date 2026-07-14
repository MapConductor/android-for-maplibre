package com.mapconductor.maplibre.marker

import com.mapconductor.core.marker.MarkerEntityInterface
import com.mapconductor.maplibre.MapLibreActualMarker
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconOffset
import org.maplibre.android.style.layers.PropertyFactory.iconTranslateAnchor
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection

open class MarkerLayer(
    open val sourceId: String,
    open val layerId: String,
) {
    val layer =
        SymbolLayer(layerId, sourceId).apply {
            setProperties(
                iconImage(get(MapLibreMarkerOverlayRenderer.Prop.ICON_ID)),
                // iconSize(get(MapLibreMarkerOverlayRenderer.Prop.SCALE)),
                iconAllowOverlap(true),
                iconIgnorePlacement(true),
                PropertyFactory.symbolSortKey(get(MapLibreMarkerOverlayRenderer.Prop.Z_INDEX)),
                iconAnchor(MapLibreMarkerOverlayRenderer.IconAnchor.TOP_LEFT),
                iconTranslateAnchor(MapLibreMarkerOverlayRenderer.IconTranslateAnchor.MAP),
                // Each feature always carries icon-offset in properties; use it directly
                iconOffset(get(MapLibreMarkerOverlayRenderer.Prop.ICON_ANCHOR)),
            )
        }

    val source: GeoJsonSource =
        GeoJsonSource(
            sourceId,
            FeatureCollection.fromFeatures(emptyList<MapLibreActualMarker>()),
        )

    // GeoJsonSource() starts out empty, so the first draw() call has nothing to clear.
    // @Volatile because callers on a different thread (e.g. MapLibreMarkerOverlayRenderer.
    // onPostProcess() on its ingest thread) need to read this without hopping onto the
    // thread that writes it, to decide whether that hop is even necessary in the first place.
    @Volatile
    private var lastDrawnEmpty = true

    // Lets a caller on any thread check, before paying for a dispatcher hop onto the thread
    // that owns the style, whether draw() would actually have anything to do for this set of
    // entities. Mirrors the emptiness check draw() itself performs.
    fun wouldSkipDraw(entities: List<MarkerEntityInterface<Feature>>): Boolean =
        lastDrawnEmpty && entities.none { it.visible && it.marker != null }

    fun draw(
        entities: List<MarkerEntityInterface<Feature>>,
        style: org.maplibre.android.maps.Style,
    ) {
        val visibleEntities = entities.filter { it.visible && it.marker != null }
        val features = visibleEntities.mapNotNull { it.marker }

        // setGeoJson() always forces MapLibre GL Native to re-tile and invalidate the source's
        // render pass, even when the data is identical to what's already there. When tiling is
        // active, onPostProcess() calls draw() with an empty list on every ingest regardless of
        // whether anything actually changed, so an empty-to-empty call here is pure waste -
        // worst of all, it lands right after a large marker ingest, when the heap is already
        // under GC pressure from that ingest's allocations.
        if (features.isEmpty() && lastDrawnEmpty) return

        val collection = FeatureCollection.fromFeatures(features)

        try {
            // Always update the source attached to the current style
            var styleSource = style.getSourceAs<GeoJsonSource>(sourceId)
            if (styleSource == null) {
                // Source might not be attached yet (e.g., after style reload). Try to attach ours.
                try {
                    style.addSource(source)
                } catch (_: Exception) {
                    // ignore if already added or style busy
                }
                styleSource = style.getSourceAs(sourceId)
            }
            styleSource?.setGeoJson(collection)
            lastDrawnEmpty = features.isEmpty()
        } catch (e: Exception) {
            android.util.Log.w("MapLibre", "Failed to update marker source: ${e.message}")
        }
    }
}
