package com.mapconductor.maplibre

import android.graphics.PointF
import com.mapconductor.core.features.GeoPoint
import com.mapconductor.core.features.GeoPointInterface
import com.mapconductor.core.map.MapViewHolderInterface
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

interface MapLibreMapViewHolderInterface : MapViewHolderInterface<MapView, MapLibreMap> {
    fun getController(): MapLibreViewController?
}

internal class MapLibreMapViewHolder(
    override val mapView: MapView,
    override val map: MapLibreMap,
) : MapLibreMapViewHolderInterface {
    private var controller: MapLibreViewController? = null

    fun setController(ctrl: MapLibreViewController) {
        controller = ctrl
    }

    override fun getController(): MapLibreViewController? = controller

    override fun toScreenOffset(position: GeoPointInterface): PointF? {
        val pixel =
            map.projection.toScreenLocation(GeoPoint.from(position).toLatLng())
        return PointF(pixel.x, pixel.y)
    }

    override fun fromScreenOffsetSync(offset: PointF): GeoPoint? =
        map.projection.fromScreenLocation(PointF(offset.x, offset.y)).toGeoPoint()

    override suspend fun fromScreenOffset(offset: PointF): GeoPoint? = fromScreenOffsetSync(offset)
}
