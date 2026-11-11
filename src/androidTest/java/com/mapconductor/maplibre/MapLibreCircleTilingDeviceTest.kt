package com.mapconductor.maplibre

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mapconductor.compose.circle.Circles
import com.mapconductor.core.circle.CIRCLE_TILE_LAYER_ID
import com.mapconductor.core.circle.CircleState
import com.mapconductor.core.features.GeoPoint
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises bulk registration through the real map host, not just the core tiler. */
@RunWith(AndroidJUnit4::class)
class MapLibreCircleTilingDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun circlesSwitchBetweenTilesAndNativeAndSurviveStyleReload() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fun design(name: String): MapLibreDesign {
            val file = File(context.cacheDir, "circle-device-$name.json")
            file.writeText("""{"version":8,"name":"$name","sources":{},"layers":[{"id":"background","type":"background"}]}""")
            return MapLibreDesign(id = name, styleJsonURL = "file://${file.absolutePath}")
        }
        val first = design("first")
        val second = design("second")
        val state = MapLibreViewState(mapDesignType = first, id = "circle-device")
        val many = (0..2000).map { i ->
            CircleState(center = GeoPoint(35.6 + i * 0.00001, 139.7), radiusMeters = 20.0, id = "circle-$i")
        }
        var circles by mutableStateOf(many)
        compose.setContent {
            MapLibreMapView(state = state, modifier = Modifier.fillMaxSize()) { Circles(circles) }
        }
        fun tiled(design: String): Boolean {
            var result = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val style = state.getMapViewHolder()?.map?.style
                result = style?.isFullyLoaded == true && style.uri == (if (design == "first") first.styleJsonURL else second.styleJsonURL) &&
                    style.layers.any { it.id.contains(CIRCLE_TILE_LAYER_ID) }
            }
            return result
        }
        compose.waitUntil(30_000) { tiled("first") }
        compose.runOnUiThread { state.mapDesignType = second }
        compose.waitUntil(30_000) { tiled("second") }
        compose.runOnUiThread { circles = many.take(1) }
        compose.waitUntil(30_000) {
            var native = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val style = state.getMapViewHolder()?.map?.style
                native = style?.isFullyLoaded == true && style.layers.none { it.id.contains(CIRCLE_TILE_LAYER_ID) }
            }
            native
        }
        compose.runOnUiThread { circles = many }
        compose.waitUntil(30_000) { tiled("second") }
        assertTrue(tiled("second"))
        compose.runOnUiThread { circles = emptyList() }
        compose.waitUntil(30_000) { !tiled("second") }
    }
}
