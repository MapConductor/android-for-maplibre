package com.mapconductor.maplibre

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.mapconductor.core.InternalMapConductorApi
import com.mapconductor.core.map.StyleMutationTarget
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.HeatmapLayer
import org.maplibre.android.style.layers.LayoutPropertyValue
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PaintPropertyValue
import org.maplibre.android.style.layers.SymbolLayer

/**
 * Changes a loaded MapLibre style in place.
 *
 * The seven calls [StyleMutationTarget] asks for, against MapLibre's style
 * API. Everything above them — what a mutation means, which can be skipped,
 * keeping one bad property from costing the rest — is
 * `applyStyleMutations` in core, shared with every other provider.
 *
 * Specific to MapLibre on Android, despite the shape being shared. Unlike
 * the browser -- where MapLibre, Mapbox and the MapTiler SDK answer to the
 * same method names and one applier serves all three -- the Android SDKs
 * share nothing: Mapbox is `com.mapbox.maps`, and MapTiler Android is
 * `com.maptiler.maptilersdk`, not MapLibre at all. Each gets its own
 * adapter, and core keeps the decisions.
 *
 * ## Why values arrive as text
 *
 * A style property's value is a literal or an expression tree, and MapLibre
 * wants the two as different Java types — a `String`/`Float`/`Boolean`, or
 * an `Expression` built from a Gson tree. Text is what crossed the FFI
 * boundary from the compiler, and this is where it becomes one or the
 * other.
 */
@InternalMapConductorApi
internal class MapLibreStyleMutationTarget(
    private val style: () -> Style?,
    /**
     * Whether the style on screen is the document these deltas are for.
     *
     * Not `style() != null`: a vector style hands the map a new document
     * and applies the deltas compiled for it straight after, and in between
     * the map still has the one being replaced. See
     * `MapLibreViewController.isShowingRequestedStyle`.
     */
    private val ready: () -> Boolean,
) : StyleMutationTarget {
    override val isReady: Boolean get() = ready()

    override fun hasLayer(layerId: String): Boolean = style()?.getLayer(layerId) != null

    override fun setPaint(
        layerId: String,
        key: String,
        valueJson: String,
    ) {
        val layer = style()?.getLayer(layerId) ?: return
        layer.setProperties(PaintPropertyValue(key, valueFor(valueJson)))
    }

    override fun setLayout(
        layerId: String,
        key: String,
        valueJson: String,
    ) {
        val layer = style()?.getLayer(layerId) ?: return
        layer.setProperties(LayoutPropertyValue(key, valueFor(valueJson)))
    }

    override fun setFilter(
        layerId: String,
        filterJson: String,
    ) {
        // `setFilter` is declared on each layer type rather than on `Layer`,
        // so there is no way to write this once. A layer type that cannot
        // be filtered (background, raster, hillshade) throws, which the
        // caller reports as not applied -- correct, because it cannot be.
        val layer = style()?.getLayer(layerId) ?: return
        // `setFilter` takes an expression, never null, so "no filter" is
        // the one that matches everything.
        val expression =
            JsonParser.parseString(filterJson).takeIf { !it.isJsonNull }?.let(::expressionOf)
                ?: Expression.literal(true)
        when (layer) {
            is FillLayer -> layer.setFilter(expression)
            is LineLayer -> layer.setFilter(expression)
            is SymbolLayer -> layer.setFilter(expression)
            is CircleLayer -> layer.setFilter(expression)
            is FillExtrusionLayer -> layer.setFilter(expression)
            is HeatmapLayer -> layer.setFilter(expression)
            else -> throw UnsupportedOperationException("${layer.javaClass.simpleName} has no filter")
        }
    }

    override fun setZoomRange(
        layerId: String,
        minZoom: Float,
        maxZoom: Float,
    ) {
        val layer = style()?.getLayer(layerId) ?: return
        layer.minZoom = minZoom
        layer.maxZoom = maxZoom
    }

    override fun addLayer(
        layerId: String,
        layerJson: String,
        beforeId: String?,
    ) {
        val target = style() ?: return
        // Already there: the style was reloaded and the whole set is going
        // back on. `addLayer` would throw on the duplicate id.
        if (target.getLayer(layerId) != null) return
        // MapLibre builds layers from typed constructors; there is no "add
        // this JSON" call. The compiler only ever adds one kind of layer --
        // a background under a style that has none -- so that is the one
        // kind handled, and anything else is refused rather than guessed at.
        val layer = JsonParser.parseString(layerJson).asJsonObject
        if (layer.get("type")?.asString != "background") {
            throw UnsupportedOperationException("only a background layer can be added here")
        }
        val background = BackgroundLayer(layerId)
        layer.getAsJsonObject("paint")?.let { paint ->
            val properties =
                paint.entrySet().map { (key, value) -> PaintPropertyValue(key, valueFor(value)) }
            if (properties.isNotEmpty()) background.setProperties(*properties.toTypedArray())
        }
        if (beforeId != null && target.getLayer(beforeId) != null) {
            target.addLayerBelow(background, beforeId)
        } else {
            target.addLayer(background)
        }
    }

    /** MapLibre answers `false` for a layer it does not have; nothing to guard. */
    override fun removeLayer(layerId: String) {
        style()?.removeLayer(layerId)
    }

    private fun valueFor(json: String): Any = valueFor(JsonParser.parseString(json))

    /**
     * A JSON node as the Java value MapLibre wants.
     *
     * Arrays are expressions; everything else is the literal it looks like.
     * A number arrives as `Float` because that is what every numeric paint
     * property is declared as — handing it a `Double` makes the native
     * setter reject the whole property.
     */
    private fun valueFor(node: JsonElement): Any =
        when {
            node.isJsonArray -> expressionOf(node)
            node.isJsonNull ->
                // Nothing to set. The compiler fills in the spec default for
                // the properties it knows, so this is a raw key it did not,
                // and refusing is better than writing something invented.
                throw UnsupportedOperationException("no value, and no default to put back")
            node.isJsonObject -> throw UnsupportedOperationException("an object is not a style value")
            else -> {
                val primitive = node.asJsonPrimitive
                when {
                    primitive.isBoolean -> primitive.asBoolean
                    primitive.isNumber -> primitive.asFloat
                    else -> primitive.asString
                }
            }
        }

    private fun expressionOf(node: JsonElement): Expression =
        Expression.Converter.convert(node as? JsonArray ?: node.asJsonArray)
}
