# `MapLibreCircleController`

A controller class that manages the lifecycle and rendering of circle overlays on a MapLibre map.

## Signature

```kotlin
class MapLibreCircleController(
    override val renderer: MapLibreCircleOverlayRenderer,
    circleManager: CircleManagerInterface<MapLibreActualCircle> = CircleManager(),
) : CircleController<MapLibreActualCircle>(circleManager, renderer)
```

## Description

The `MapLibreCircleController` is the primary entry point for developers to manage and display
circles on a MapLibre map. It orchestrates the interaction between the generic circle management
logic (`CircleManager`) and the platform-specific rendering implementation
(`MapLibreCircleOverlayRenderer`).

This controller simplifies the process of adding, removing, and updating circles by providing a
high-level API. It delegates the state management of circle objects to the `circleManager` and the
actual drawing on the map canvas to the `renderer`.

## Parameters

- `renderer`
    - Type: `MapLibreCircleOverlayRenderer`
    - Description: **Required.** The renderer instance responsible for drawing the circles onto the
                   MapLibre map.
- `circleManager`
    - Type: `CircleManagerInterface<MapLibreActualCircle>`
    - Description: *Optional.* The manager that handles the collection of circles, including their
                   properties and state. If not provided, a default `CircleManager()` instance is
                   created.

## Example

The following example demonstrates how to initialize the `MapLibreCircleController` and use it to
add and manage a circle on the map.

```kotlin
// MapLibreCircleController is typically created internally by MapLibreMapView.
// Circles are added via CircleState in the Compose content lambda:

MapLibreMapView(state = mapState) {
    Circle(
        state = rememberCircleState(
            id = "circle-1",
            center = GeoPoint(40.7128, -74.0060), // New York City
            radius = 800.0, // Radius in meters
            fillColor = Color(0x21_2196F3), // Semi-transparent blue
            strokeColor = Color.Blue,
            strokeWidth = 3.dp
        )
    )
}
```
