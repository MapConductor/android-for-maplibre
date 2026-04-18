# MapLibrePolylineController

The `MapLibrePolylineController` is the primary controller for managing and displaying polylines on
a MapLibre map.

## Signature

```kotlin
class MapLibrePolylineController(
    override val renderer: MapLibrePolylineOverlayRenderer,
    polylineManager: PolylineManagerInterface<MapLibreActualPolyline> = renderer.polylineManager,
) : PolylineController<MapLibreActualPolyline>(polylineManager, renderer)
```

## Description

This class serves as the main entry point for developers to interact with polylines within a
MapLibre environment. It integrates the generic polyline management logic from the base
`PolylineController` with the MapLibre-specific rendering capabilities provided by
`MapLibrePolylineOverlayRenderer`. Use this controller to add, remove, update, and manage the
lifecycle of polylines on the map.

## Parameters

This section describes the parameters for the `MapLibrePolylineController` constructor.

- `renderer`
    - Type: `MapLibrePolylineOverlayRenderer`
    - Description: **Required.** The renderer responsible for drawing and managing polyline overlays
                   on the MapLibre map.
- `polylineManager`
    - Type: `PolylineManagerInterface<MapLibreActualPolyline>`
    - Description: *Optional.* The manager for the underlying polyline data structures. By default,
                   it uses the `polylineManager` instance from the provided `renderer`.

## Example

The following example demonstrates how to initialize the `MapLibrePolylineController` and use it to
add a polyline to the map.

```kotlin
// MapLibrePolylineController is typically created internally by MapLibreMapView.
// The following shows how the controller is used once obtained.

// Assume 'mapLibreMapView' is an initialized MapLibreMapView composable providing
// a MapLibreViewController via its state.

// Add a polyline via PolylineState in the Compose content lambda:
MapLibreMapView(state = mapState) {
    Polyline(
        state = rememberPolylineState(
            id = "route-1",
            points = listOf(
                GeoPoint(37.7749, -122.4194), // San Francisco
                GeoPoint(34.0522, -118.2437)  // Los Angeles
            ),
            strokeColor = Color.Red,
            strokeWidth = 8.dp
        )
    )
}
```