package com.mapconductor.maplibre.marker

import android.graphics.Bitmap
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mapconductor.core.ResourceProvider
import com.mapconductor.core.calculateZIndex
import com.mapconductor.core.features.GeoPoint
import com.mapconductor.core.marker.AbstractMarkerOverlayRenderer
import com.mapconductor.core.marker.BitmapIcon
import com.mapconductor.core.marker.DefaultMarkerIcon
import com.mapconductor.core.marker.MarkerEntityInterface
import com.mapconductor.core.marker.MarkerIconInterface
import com.mapconductor.core.marker.MarkerManager
import com.mapconductor.core.marker.MarkerOverlayRendererInterface
import com.mapconductor.maplibre.MapLibreActualMarker
import com.mapconductor.maplibre.MapLibreMapViewHolderInterface
import com.mapconductor.maplibre.toPoint
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class MapLibreMarkerOverlayRenderer(
    holder: MapLibreMapViewHolderInterface,
    val markerManager: MarkerManager<MapLibreActualMarker>,
    val markerLayer: MarkerLayer,
    val dragLayer: MarkerDragLayer,
    coroutine: CoroutineScope = CoroutineScope(Dispatchers.Main),
) : AbstractMarkerOverlayRenderer<MapLibreMapViewHolderInterface, MapLibreActualMarker>(
        holder = holder,
        coroutine = coroutine,
    ) {
    private val iconRefCounter: MutableMap<String, Int> = mutableMapOf()
    private val pendingStyleImageRemovals: MutableMap<String, Long> = mutableMapOf()
    private val iconBitmapCache: MutableMap<String, Bitmap> = mutableMapOf()
    private val defaultMarkerIcon: BitmapIcon = DefaultMarkerIcon().toBitmapIcon()

    object Prop {
        const val ICON_ID = "icon_id"
        const val DEFAULT_MARKER_ID = "default"
        const val SCALE = "scale"
        const val ICON_ANCHOR = "icon-offset"
        const val Z_INDEX = "zIndex"
    }

    object IconAnchor {
        const val CENTER = "center"
        const val LEFT = "left"
        const val RIGHT = "right"
        const val BOTTOM = "bottom"
        const val TOP_LEFT = "top-left"
        const val TOP_RIGHT = "top-right"
        const val BOTTOM_LEFT = "bottom-left"
        const val BOTTOM_RIGHT = "bottom-right"
    }

    object IconTranslateAnchor {
        const val MAP = "map"
        const val VIEWPORT = "viewport"
    }

    init {
        val style = holder.map.style
        if (style != null) {
            style.addImage(Prop.DEFAULT_MARKER_ID, defaultMarkerIcon.bitmap)
        } else {
            holder.map.getStyle { style ->
                style.addImage(Prop.DEFAULT_MARKER_ID, defaultMarkerIcon.bitmap)
            }
        }
    }

    // Ensure marker images exist on the given style (used after style reload).
    fun ensureDefaultIcon(style: org.maplibre.android.maps.Style) {
        ensureStyleImages(style)
    }

    fun ensureStyleImages(style: org.maplibre.android.maps.Style) {
        try {
            style.addImage(Prop.DEFAULT_MARKER_ID, defaultMarkerIcon.bitmap)
        } catch (_: Exception) {
        }

        markerManager.allEntities().forEach { entity ->
            entity.state.icon?.let { icon ->
                val iconKey = icon.hashCode().toString()
                val bitmap = iconBitmapCache[iconKey] ?: icon.toBitmapIcon().bitmap
                try {
                    style.addImage(iconKey, bitmap, false)
                } catch (_: Exception) {
                }
                iconBitmapCache[iconKey] = bitmap
            }
        }
    }

    private fun decrementIconRef(iconKey: String) {
        if (iconKey == Prop.DEFAULT_MARKER_ID) return
        val next = (iconRefCounter[iconKey] ?: 0) - 1
        if (next <= 0) {
            iconRefCounter.remove(iconKey)
            // MapLibre can render one frame behind GeoJSON source updates during marker animation.
            // Keep images briefly after last use so icon-image references never point at a missing image.
            pendingStyleImageRemovals[iconKey] = System.currentTimeMillis() + STYLE_IMAGE_REMOVAL_GRACE_MS
        } else {
            iconRefCounter[iconKey] = next
        }
    }

    private fun incrementIconRef(iconKey: String) {
        if (iconKey == Prop.DEFAULT_MARKER_ID) return
        pendingStyleImageRemovals.remove(iconKey)
        iconRefCounter[iconKey] = (iconRefCounter[iconKey] ?: 0) + 1
    }

    override fun setMarkerPosition(
        markerEntity: MarkerEntityInterface<MapLibreActualMarker>,
        position: GeoPoint,
    ) {
        val entities = markerManager.allEntities()
        val props = (markerEntity.marker?.properties() ?: JsonObject()).deepCopy()
        val previousZIndex =
            props
                .get(Prop.Z_INDEX)
                ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
                ?.asInt
        props.addProperty(
            Prop.Z_INDEX,
            markerEntity.state.zIndex
                ?: previousZIndex
                ?: calculateZIndex(position),
        )
        val feature =
            Feature.fromGeometry(
                position.toPoint(),
                props,
                "marker-${markerEntity.state.id}",
            )
        markerEntity.marker = feature
        val features =
            entities.map {
                if (it.state.id == markerEntity.state.id) {
                    feature
                } else {
                    it.marker
                }
            }
        // Execute directly instead of launching a new coroutine
        holder.map.style?.let { style ->
            val styleSource = style.getSourceAs<org.maplibre.android.style.sources.GeoJsonSource>(markerLayer.sourceId)
            styleSource?.setGeoJson(FeatureCollection.fromFeatures(features))
        }
    }

    override suspend fun onAdd(
        data: List<MarkerOverlayRendererInterface.AddParamsInterface>,
    ): List<MapLibreActualMarker?> =
        withContext(Dispatchers.Main) {
            val style = holder.getController()?.getStyleInstance() ?: holder.map.style
            if (style == null) {
                return@withContext emptyList()
            }

            data.forEach {
                it.state.icon?.let { icon ->
                    val iconKey = icon.hashCode().toString()
                    try {
                        style.addImage(iconKey, it.bitmapIcon.bitmap, false)
                    } catch (_: Exception) {
                    }
                    iconBitmapCache[iconKey] = it.bitmapIcon.bitmap
                    if (!iconRefCounter.contains(iconKey)) iconRefCounter[iconKey] = 0
                }
            }

            data.map {
            val featureId = "marker-${it.state.id}"
            val position = GeoPoint.from(it.state.position).toPoint()
            val properties =
                JsonObject().apply {
                    if (it.state.icon != null) {
                        it.state.icon?.let { icon ->
                            val iconKey = icon.hashCode().toString()
                            incrementIconRef(iconKey)
                            addProperty(Prop.ICON_ID, iconKey)
                            // icon offset property
                            add(Prop.ICON_ANCHOR, createIconOffset(icon))
                        }
                    } else {
                        addProperty(Prop.ICON_ID, Prop.DEFAULT_MARKER_ID)
                        add(Prop.ICON_ANCHOR, getDefaultIconOffsetProperty())
                    }
                    // We don't use the MapLibre SDK's scaling system
                    // addProperty(Prop.SCALE, 1.0)
                    addProperty(Prop.Z_INDEX, it.state.zIndex ?: calculateZIndex(it.state.position))
                }
            Feature.fromGeometry(position, properties, featureId)
        }
    }

    private fun getDefaultIconOffsetProperty(): JsonArray = createIconOffset(defaultMarkerIcon)

    private fun createIconOffset(icon: BitmapIcon): JsonArray =
        JsonArray().apply {
            add(-(icon.size.width * icon.anchor.x) / ResourceProvider.getDensity())
            add(-(icon.size.height * icon.anchor.y) / ResourceProvider.getDensity())
        }

    private fun createIconOffset(icon: MarkerIconInterface): JsonArray = createIconOffset(icon.toBitmapIcon())

    override suspend fun onRemove(data: List<MarkerEntityInterface<MapLibreActualMarker>>) {
        withContext(Dispatchers.Main) {
            data.forEach { entity ->
                val iconKey =
                    entity.marker
                        ?.properties()
                        ?.get(Prop.ICON_ID)
                        ?.asString
                        ?: entity.state.icon
                            ?.hashCode()
                            ?.toString()
                if (iconKey != null) {
                    decrementIconRef(iconKey)
                }
            }
        }
    }

    fun drawDragLayer() {
        coroutine.launch {
            val style = holder.getController()?.getStyleInstance() ?: holder.map.style
            if (style != null) {
                dragLayer.draw(style)
            }
        }
    }

    fun redraw() {
        val entities = markerManager.allEntities()
        // Get style from controller to use the same instance
        val style = holder.getController()?.getStyleInstance() ?: holder.map.style
        style?.let { s ->
            coroutine.launch {
                markerLayer.draw(entities, s)
            }
        }
    }

    override suspend fun onPostProcess() {
        withContext(Dispatchers.Main) {
            val style = holder.getController()?.getStyleInstance() ?: holder.map.style ?: return@withContext
            markerLayer.draw(markerManager.allEntities(), style)
            yield()

            val now = System.currentTimeMillis()
            val expired =
                pendingStyleImageRemovals
                    .asSequence()
                    .filter { (_, deadline) -> deadline <= now }
                    .map { (key, _) -> key }
                    .toList()

            expired.forEach { iconKey ->
                if (iconRefCounter.containsKey(iconKey)) {
                    pendingStyleImageRemovals.remove(iconKey)
                    return@forEach
                }
                try {
                    style.removeImage(iconKey)
                } catch (_: Exception) {
                } finally {
                    pendingStyleImageRemovals.remove(iconKey)
                    iconBitmapCache.remove(iconKey)
                }
            }
        }
    }

    override suspend fun onChange(
        data: List<MarkerOverlayRendererInterface.ChangeParamsInterface<MapLibreActualMarker>>,
    ): List<MapLibreActualMarker?> =
        withContext(Dispatchers.Main) {
            val style = holder.getController()?.getStyleInstance() ?: holder.map.style

            data.map { params ->
            val prevFinger = params.prev.fingerPrint
            val currFinger = params.current.fingerPrint
            val prevProperties = params.prev.marker?.properties()

            val properties =
                JsonObject().apply {
                    // No additional scaling needed - bitmap is created with device density
                    // and Bitmap.density is set to prevent MapLibre's automatic scaling
                    // addProperty(Prop.SCALE, 1.0)
                    if (currFinger.icon == prevFinger.icon) {
                        addProperty(
                            Prop.ICON_ID,
                            prevProperties?.get(Prop.ICON_ID)?.asString ?: Prop.DEFAULT_MARKER_ID,
                        )

                        add(
                            Prop.ICON_ANCHOR,
                            prevProperties?.get(Prop.ICON_ANCHOR) ?: getDefaultIconOffsetProperty(),
                        )
                    } else {
                        val prevIconKey =
                            prevProperties
                                ?.get(Prop.ICON_ID)
                                ?.asString
                                ?: params.prev.state.icon
                                    ?.hashCode()
                                    ?.toString()
                                ?: Prop.DEFAULT_MARKER_ID
                        decrementIconRef(prevIconKey)

                        if (currFinger.icon == null) {
                            addProperty(Prop.ICON_ID, Prop.DEFAULT_MARKER_ID)
                            add(Prop.ICON_ANCHOR, getDefaultIconOffsetProperty())
                        } else {
                            params.current.state.icon?.let { icon ->
                                // icon id
                                val iconKey = icon.hashCode().toString()
                                try {
                                    style?.addImage(iconKey, params.bitmapIcon.bitmap, false)
                                } catch (_: Exception) {
                                }
                                iconBitmapCache[iconKey] = params.bitmapIcon.bitmap
                                if (!iconRefCounter.contains(iconKey)) iconRefCounter[iconKey] = 0
                                incrementIconRef(iconKey)
                                addProperty(Prop.ICON_ID, iconKey)
                                add(Prop.ICON_ANCHOR, createIconOffset(icon))
                            }
                        }
                    }
                    addProperty(
                        Prop.Z_INDEX,
                        resolveZIndexForChange(
                            current = params.current,
                            prev = params.prev,
                            prevProperties = prevProperties,
                        ),
                    )
                }

            val position =
                GeoPoint.from(params.current.state.position).toPoint()
            val featureId = "marker-${params.current.state.id}"
            Feature.fromGeometry(position, properties, featureId)
        }
    }

    private fun resolveZIndexForChange(
        current: MarkerEntityInterface<MapLibreActualMarker>,
        prev: MarkerEntityInterface<MapLibreActualMarker>,
        prevProperties: JsonObject?,
    ): Int {
        current.state.zIndex?.let { return it }

        if (current.fingerPrint.zIndex == prev.fingerPrint.zIndex) {
            prevProperties
                ?.get(Prop.Z_INDEX)
                ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
                ?.asInt
                ?.let { return it }
        }

        return calculateZIndex(current.state.position)
    }
}

private const val STYLE_IMAGE_REMOVAL_GRACE_MS: Long = 1500L
