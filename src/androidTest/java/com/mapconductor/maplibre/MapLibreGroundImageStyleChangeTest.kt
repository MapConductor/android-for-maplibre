package com.mapconductor.maplibre

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mapconductor.compose.groundimage.GroundImage
import com.mapconductor.core.features.GeoPoint
import com.mapconductor.core.features.GeoRectBounds
import com.mapconductor.core.groundimage.GroundImageState
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.maps.Style
import java.io.File

/**
 * A ground image survives the map design changing under it.
 *
 * Replacing the style throws away every source and layer added at runtime, so
 * the SDK has to put the image back into the new style. iOS kept the handle it
 * had made for the old style and skipped an unchanged image (ios-for-maplibre
 * #12); this checks the Android path — `setMapDesignType` → `setStyle` →
 * `setupStyle` — on a real MapView loading two local styles.
 */
@RunWith(AndroidJUnit4::class)
class MapLibreGroundImageStyleChangeTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var first: MapLibreDesign
    private lateinit var second: MapLibreDesign

    @Before
    fun writeStyles() {
        val dir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        fun style(name: String): MapLibreDesign {
            val file = File(dir, "mc-groundimage-$name.json")
            file.writeText(
                """{"version": 8, "name": "$name", "sources": {},
                   "layers": [{"id": "background", "type": "background"}]}""",
            )
            return MapLibreDesign(id = name, styleJsonURL = "file://${file.absolutePath}")
        }
        first = style("first")
        second = style("second")
    }

    private fun groundImage(): GroundImageState {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        return GroundImageState(
            bounds = GeoRectBounds(GeoPoint(35.6, 139.6), GeoPoint(35.8, 139.9)),
            image = BitmapDrawable(resources, bitmap),
            id = "tokyo",
        )
    }

    private fun currentStyle(state: MapLibreViewState): Style? {
        var style: Style? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            style = state.getMapViewHolder()?.map?.style?.takeIf { it.isFullyLoaded }
        }
        return style
    }

    private fun hasGroundImage(style: Style?): Boolean {
        var found = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            found = style != null &&
                style.getSource("mc-gimg-src-tokyo") != null &&
                style.getLayer("mc-gimg-lyr-tokyo") != null
        }
        return found
    }

    private fun styleNamed(state: MapLibreViewState, design: MapLibreDesign): Style? {
        var style: Style? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            style = state.getMapViewHolder()?.map?.style
                ?.takeIf { it.isFullyLoaded && it.uri == design.styleJsonURL }
        }
        return style
    }

    private var shown by mutableStateOf(true)

    private fun show(state: MapLibreViewState, image: GroundImageState) {
        compose.setContent {
            MapLibreMapView(state = state, modifier = Modifier.fillMaxSize()) {
                if (shown) GroundImage(image)
            }
        }
    }

    @Test
    fun anUnchangedGroundImageIsInTheNewStyle() {
        val state = MapLibreViewState(mapDesignType = first, id = "unchanged")
        show(state, groundImage())
        compose.waitUntil(TIMEOUT) { hasGroundImage(styleNamed(state, first)) }
        val before = currentStyle(state)

        compose.runOnUiThread { state.mapDesignType = second }
        compose.waitUntil(TIMEOUT) { styleNamed(state, second) != null }
        val after = currentStyle(state)
        assertNotSame("the design change replaced the style", before, after)
        compose.waitUntil(TIMEOUT) { hasGroundImage(styleNamed(state, second)) }
        assertTrue("drawn again in the new style, with nothing about it changed", hasGroundImage(after))
    }

    @Test
    fun theSameDesignSetAgainKeepsTheImage() {
        val state = MapLibreViewState(mapDesignType = first, id = "same")
        show(state, groundImage())
        compose.waitUntil(TIMEOUT) { hasGroundImage(styleNamed(state, first)) }

        compose.runOnUiThread { state.mapDesignType = first }
        // Give a reload, if one is started, time to land before checking.
        Thread.sleep(SETTLE_MS)
        compose.waitUntil(TIMEOUT) { hasGroundImage(styleNamed(state, first)) }
    }

    @Test
    fun aGroundImageRemovedAfterTheChangeLeavesTheNewStyle() {
        val state = MapLibreViewState(mapDesignType = first, id = "removed")
        show(state, groundImage())
        compose.waitUntil(TIMEOUT) { hasGroundImage(styleNamed(state, first)) }

        compose.runOnUiThread { state.mapDesignType = second }
        compose.waitUntil(TIMEOUT) { hasGroundImage(styleNamed(state, second)) }

        compose.runOnUiThread { shown = false }
        compose.waitUntil(TIMEOUT) {
            val style = styleNamed(state, second)
            style != null && !hasGroundImage(style)
        }
    }

    private companion object {
        const val TIMEOUT = 20_000L
        const val SETTLE_MS = 1_500L
    }
}
