# MapLibreMapViewScope

## Signature

```kotlin
class MapLibreMapViewScope : MapViewScope()
```

## Description

`MapLibreMapViewScope` is a specialized scope class for the MapLibre implementation within the
MapConductor framework. It extends the base `MapViewScope`, inheriting all common map
functionalities.

This scope is the context for map configuration and control when using MapLibre as the provider.
It is provided as the receiver within the `content` lambda of `MapLibreMapView`, where overlay
composables are called.

## Example

`MapLibreMapViewScope` is provided as the receiver within the `content` lambda of `MapLibreMapView`.
Overlay composables are called within this scope.

```kotlin
MapLibreMapView(
    state = mapState,
    modifier = Modifier.fillMaxSize(),
) {
    // 'this' is MapLibreMapViewScope
    // Add overlays using composables from MapViewScope here.
    Marker(state = MarkerState(id = "marker-1", position = GeoPoint(35.681236, 139.767125)))
}
```
