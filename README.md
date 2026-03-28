# MapLibre SDK for MapConductor Android

## Description

MapConductor provides a unified API for Android Jetpack Compose.
You can use MapLibre  with Jetpack Compose, but you can also switch to other Maps SDKs (such as Mapbox, HERE, and so on), anytimes.

Even you use the wrapper API, but you can still access to the native MapLibre view if you want.

## Usage

```kotlin
@Composable
fun MapView(modifier: Modififer = Modififer) {
    val center = GeoPoint.fromLatLong(
        latitude = 21.382314,
        longitude = -157.933097,
    )
    val initCameraPosition = MapCameraPosition(
        position = center,
        zoom = 12.0,
    )

    val state = rememberMapLibreMapViewState(
        cameraPosition = initCameraPosition,
        mapDesignType = MapLibreDesign.MapTilerBasicEn,
    )

    val markerState = MarkerState(
        position = center,
        icon = DefaultMarkerIcon(label = "Hello, World!"),
        onClick = {
            it.animate(MarkerAnimation.Bounce)
        },
    )

    MapLibreMapView(
        modifier = modifier,
        state = state,
    ) {
        Marker(
            state = markerState,
        )
    }
}
```


