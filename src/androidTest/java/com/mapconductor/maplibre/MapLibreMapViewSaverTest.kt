package com.mapconductor.maplibre

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mapconductor.core.features.GeoPoint
import com.mapconductor.core.map.AttributionRule
import com.mapconductor.core.map.MapCameraPosition
import com.mapconductor.core.map.MapUISettings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import android.os.Bundle
import android.os.Parcel

/**
 * 回転などの構成変更で state が [MapLibreMapViewSaver] を通って作り直されても、デザイン（id・
 * attributionRules ほか）と uiSettings・カメラが落ちないこと。Bundle は実機の Parcel を一度通す。
 */
@RunWith(AndroidJUnit4::class)
class MapLibreMapViewSaverTest {
    private val rules =
        listOf(AttributionRule("© test"), AttributionRule("© ranged", minZoom = 3, maxZoom = 15))

    private val camera =
        MapCameraPosition(
            position = GeoPoint(21.38, -157.93),
            zoom = 12.0,
            bearing = 20.0,
            tilt = 10.0,
        )

    @Test
    fun keepsCustomDesignWithAttributionRules() {
        val design = MapLibreDesign.MapTilerTonerJa.copy(attributionRules = rules)
        val state = MapLibreViewState(mapDesignType = design, id = "maplibre-1", cameraPosition = camera)
        state.uiSettings = MapUISettings(rotateGesture = false, tiltGesture = false)

        val restored = roundTrip(MapLibreMapViewSaver().createSaver(), state)

        // 以前は styleJsonURL しか保存しておらず、id は osm-bright、attributionRules は空に戻っていた。
        assertEquals(design, restored.mapDesignType)
        assertCommon(restored, "maplibre-1")
    }

    @Test
    fun keepsPresetDesign() {
        val state = MapLibreViewState(mapDesignType = MapLibreDesign.OsmBrightJa, id = "maplibre-2")
        val restored = roundTrip(MapLibreMapViewSaver().createSaver(), state)
        assertEquals(MapLibreDesign.OsmBrightJa, restored.mapDesignType)
    }

    private fun assertCommon(
        restored: com.mapconductor.core.map.MapViewStateInterface<*>,
        id: String,
    ) {
        assertEquals(id, restored.id)
        assertEquals(false, restored.uiSettings.rotateGesture)
        assertEquals(false, restored.uiSettings.tiltGesture)
        assertEquals(true, restored.uiSettings.scrollGesture)
        assertEquals(12.0, restored.cameraPosition.zoom, 1e-9)
        assertEquals(20.0, restored.cameraPosition.bearing, 1e-9)
        assertEquals(21.38, restored.cameraPosition.position.latitude, 1e-9)
    }

    private fun <T : Any> roundTrip(
        saver: Saver<T, Bundle>,
        value: T,
    ): T {
        val saved = with(saver) { SaverScope { true }.save(value) }!!
        // 一度 Parcel に書いて読み戻す（プロセスが落ちたあとの復元と同じ経路）。
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(saved)
            parcel.setDataPosition(0)
            return saver.restore(parcel.readBundle(javaClass.classLoader)!!)!!
        } finally {
            parcel.recycle()
        }
    }
}
