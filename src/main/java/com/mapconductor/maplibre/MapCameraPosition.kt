package com.mapconductor.maplibre

import com.mapconductor.core.features.GeoPoint
import com.mapconductor.core.map.MapCameraPosition
import com.mapconductor.core.map.MapCameraPositionInterface
import com.mapconductor.core.spherical.Spherical
import com.mapconductor.maplibre.zoom.ZoomAltitudeConverter
import org.maplibre.android.camera.CameraPosition
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.tan

private val converter = ZoomAltitudeConverter()
private const val NEGATIVE_TILT_TARGET_DISTANCE_SCALE = 1.83
private const val NEGATIVE_TILT_ZOOM_OFFSET_AT_MAX_TILT = -0.9

fun MapCameraPosition.toCameraPosition(): CameraPosition {
    if (tilt >= 0) {
        return CameraPosition
            .Builder()
            .target(GeoPoint.from(position).toLatLng())
            .zoom(ZoomAltitudeConverter.googleZoomToMaplibreZoom(zoom))
            .tilt(tilt)
            .bearing(bearing)
            // TODO:
//    .padding(paddings?.toEdgeInsects())
            .build()
    } else {
        // tilt < 0: MapLibre cannot represent an upward pitch directly.
        // Match the Google Maps workaround: move the ground target forward and render with abs(tilt).
        val tiltAbsDeg = abs(tilt).coerceIn(0.0, 60.0)
        val tiltAbsRad = Math.toRadians(tiltAbsDeg)
        val maplibreZoomForAltitude = ZoomAltitudeConverter.googleZoomToMaplibreZoom(zoom)
        val altitude = converter.zoomLevelToAltitude(maplibreZoomForAltitude, position.latitude, 0.0)
        val distanceForward =
            altitude *
                cos(tiltAbsRad) *
                tan(tiltAbsRad) *
                NEGATIVE_TILT_TARGET_DISTANCE_SCALE
        val target = Spherical.computeOffset(position, distanceForward, bearing)
        val adjustedZoom = zoom + NEGATIVE_TILT_ZOOM_OFFSET_AT_MAX_TILT * (tiltAbsDeg / 60.0)

        return CameraPosition
            .Builder()
            .target(target.toLatLng())
            .zoom(ZoomAltitudeConverter.googleZoomToMaplibreZoom(adjustedZoom))
            .tilt(tiltAbsDeg)
            .bearing(bearing)
            // TODO:
//    .padding(paddings?.toEdgeInsects())
            .build()
    }
}

fun MapCameraPosition.Companion.from(cameraPosition: MapCameraPositionInterface) =
    when (cameraPosition) {
        is MapCameraPosition -> cameraPosition
        else ->
            MapCameraPosition(
                position = cameraPosition.position,
                zoom = cameraPosition.zoom,
                bearing = cameraPosition.bearing,
                tilt = cameraPosition.tilt,
                visibleRegion = cameraPosition.visibleRegion,
            )
    }

fun CameraPosition.toMapCameraPosition() =
    MapCameraPosition(
        position = target?.toGeoPoint() ?: GeoPoint.fromLongLat(0.0, 0.0),
        zoom = ZoomAltitudeConverter.maplibreZoomToGoogleZoom(zoom),
        bearing = bearing ?: 0.0,
        tilt = tilt ?: 0.0,
        visibleRegion = null,
    )
