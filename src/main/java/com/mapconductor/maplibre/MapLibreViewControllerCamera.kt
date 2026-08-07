package com.mapconductor.maplibre

import androidx.compose.ui.geometry.Offset
import com.mapconductor.core.features.GeoRectBounds
import com.mapconductor.core.map.CameraRestriction
import com.mapconductor.core.map.MapCameraPosition
import com.mapconductor.core.map.MapCameraPositionInterface
import com.mapconductor.core.map.VisibleRegion
import com.mapconductor.maplibre.zoom.ZoomAltitudeConverter
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.constants.MapLibreConstants
import kotlinx.coroutines.launch

/**
 * カメラ位置の読み取りと初回通知。
 *
 * MapLibre のズームは 256px タイル基準で、MapConductor の論理ズーム（Google 基準）とは
 * 1 段ずれる。読むときも書くときも [ZoomAltitudeConverter] を通す。
 */
internal fun MapLibreViewController.readLogicalCameraPosition(): MapCameraPosition =
    MapLibreCameraStateSnapshot(
        cameraPosition = holder.map.cameraPosition,
        logicalTiltHint = lastLogicalCameraPosition?.tilt,
    ).toMapCameraPosition()

internal fun MapLibreViewController.getMapCameraPosition(camera: MapCameraPositionInterface): MapCameraPosition? {
    val mapWidth = holder.mapView.width.toFloat()
    val mapHeight = holder.mapView.height.toFloat()
    val nearLeft =
        holder.fromScreenOffsetSync(
            Offset(0.0f, mapHeight),
        ) ?: return null
    val nearRight =
        holder.fromScreenOffsetSync(
            Offset(mapWidth, mapHeight),
        ) ?: return null
    val farLeft =
        holder.fromScreenOffsetSync(
            Offset(0.0f, 0.0f),
        ) ?: return null
    val farRight =
        holder.fromScreenOffsetSync(
            Offset(mapWidth, 0.0f),
        ) ?: return null

    val bounds = GeoRectBounds()
    bounds.extend(nearLeft)
    bounds.extend(nearRight)
    bounds.extend(farLeft)
    bounds.extend(farRight)
    val visibleRegion =
        VisibleRegion(
            bounds = bounds,
            nearLeft = nearLeft,
            nearRight = nearRight,
            farLeft = farLeft,
            farRight = farRight,
        )
    val mapCameraPosition =
        MapCameraPosition.from(camera).copy(
            visibleRegion = visibleRegion,
        )
    return mapCameraPosition
}

internal fun MapLibreViewController.handleMoveCamera(position: MapCameraPosition) {
    lastLogicalCameraPosition = position
    mainCoroutine.launch {
        val cameraPos = position.toCameraPosition()
        val cameraUpdate =
            CameraUpdateFactory
                .newCameraPosition(cameraPos)
        holder.map.moveCamera(cameraUpdate)
        emitCameraMoveEnd(position)
    }
}

internal fun MapLibreViewController.handleAnimateCamera(
    position: MapCameraPosition,
    duration: Long,
) {
    lastLogicalCameraPosition = position
    mainCoroutine.launch {
        val cameraPos = position.toCameraPosition()
        val cameraUpdate =
            CameraUpdateFactory
                .newCameraPosition(cameraPos)
        holder.map.animateCamera(cameraUpdate, duration.toInt())
        emitCameraMoveEnd(position)
    }
}

internal fun MapLibreViewController.handleFitBounds(
    bounds: GeoRectBounds,
    padding: Int,
) {
    val latLngBounds = bounds.toLatLngBounds() ?: return
    val cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds, padding)
    mainCoroutine.launch {
        holder.map.moveCamera(cameraUpdate)
        emitCameraMoveEnd(readLogicalCameraPosition())
    }
}

internal fun MapLibreViewController.handleCameraRestriction(restriction: CameraRestriction?) {
    mainCoroutine.launch {
        holder.map.setLatLngBoundsForCameraTarget(restriction?.bounds?.toLatLngBounds())
        // 統一ズーム（Google 準拠）を MapLibre ズームへ変換して適用。
        // preference は解除 API が無いため、未指定時は既定の下限/上限を渡す。
        holder.map.setMinZoomPreference(
            restriction
                ?.minZoom
                ?.let { ZoomAltitudeConverter.googleZoomToMaplibreZoom(it) }
                ?: MapLibreConstants.MINIMUM_ZOOM.toDouble(),
        )
        holder.map.setMaxZoomPreference(
            restriction
                ?.maxZoom
                ?.let { ZoomAltitudeConverter.googleZoomToMaplibreZoom(it) }
                ?: MapLibreConstants.MAXIMUM_ZOOM.toDouble(),
        )
    }
}
