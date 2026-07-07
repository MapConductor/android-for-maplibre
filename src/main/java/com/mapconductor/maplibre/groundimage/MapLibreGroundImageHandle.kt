package com.mapconductor.maplibre.groundimage

data class MapLibreGroundImageHandle(
    val sourceId: String,
    val layerId: String,
    val applied: AppliedGroundImage,
)

data class AppliedGroundImage(
    val bounds: Int,
    val image: Int,
    val opacity: Int,
)
