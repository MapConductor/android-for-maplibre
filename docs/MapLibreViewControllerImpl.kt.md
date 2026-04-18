# MapLibreViewController

## Class: `MapLibreViewController`

The `MapLibreViewController` is the primary controller for managing and interacting with a MapLibre
map instance. It serves as a bridge between your application logic and the map view, handling the
lifecycle and rendering of various map overlays such as markers, polylines, polygons, and more. It
also processes user interactions like clicks, drags, and camera movements.

### Signature

```kotlin
class MapLibreViewController(
    override val holder: MapLibreMapViewHolderInterface,
    private val markerController: MapLibreMarkerController,
    private val polylineController: MapLibrePolylineController,
    private val polygonController: MapLibrePolygonConductor,
    private val groundImageController: MapLibreGroundImageController,
    private val circleController: MapLibreCircleController,
    private val rasterLayerController: MapLibreRasterLayerController,
    override val coroutine: CoroutineScope = CoroutineScope(Dispatchers.Main),
    val backCoroutine: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : BaseMapViewController(), MapLibreViewControllerInterface, ...
```

### Constructor

Initializes a new instance of the `MapLibreViewController`.

#### Parameters

- `holder`
    - Type: `MapLibreMapViewHolderInterface`
    - Description: The view holder that contains the `MapLibreMap` instance.
- `markerController`
    - Type: `MapLibreMarkerController`
    - Description: The controller responsible for managing markers.
- `polylineController`
    - Type: `MapLibrePolylineController`
    - Description: The controller responsible for managing polylines.
- `polygonController`
    - Type: `MapLibrePolygonConductor`
    - Description: The controller responsible for managing polygons.
- `groundImageController`
    - Type: `MapLibreGroundImageController`
    - Description: The controller responsible for managing ground images.
- `circleController`
    - Type: `MapLibreCircleController`
    - Description: The controller responsible for managing circles.
- `rasterLayerController`
    - Type: `MapLibreRasterLayerController`
    - Description: The controller responsible for managing raster tile layers.
- `coroutine`
    - Type: `CoroutineScope`
    - Description: The coroutine scope for UI-related operations. Defaults to
                   `CoroutineScope(Dispatchers.Main)`.
- `backCoroutine`
    - Type: `CoroutineScope`
    - Description: The coroutine scope for background operations. Defaults to
                   `CoroutineScope(Dispatchers.Default)`.

---

## Methods

### Camera Control

#### moveCamera

Instantly moves the map camera to a specified position.

**Signature**
```kotlin
fun moveCamera(position: MapCameraPosition)
```

**Parameters**
- `position`
    - Type: `MapCameraPosition`
    - Description: The target camera position to move to.

**Example**
```kotlin
val newPosition = MapCameraPosition(
    position = GeoPoint(40.7128, -74.0060), // New York City
    zoom = 12.0
)
mapViewController.moveCamera(newPosition)
```

#### animateCamera

Animates the map camera to a new position over a specified duration.

**Signature**
```kotlin
fun animateCamera(position: MapCameraPosition, duration: Long)
```

**Parameters**
- `position`
    - Type: `MapCameraPosition`
    - Description: The target camera position to animate to.
- `duration`
    - Type: `Long`
    - Description: The duration of the animation in milliseconds.

**Example**
```kotlin
val targetPosition = MapCameraPosition(
    position = GeoPoint(34.0522, -118.2437), // Los Angeles
    zoom = 10.0
)
mapViewController.animateCamera(targetPosition, duration = 2000L) // 2-second animation
```

---

### Map Style

#### setMapDesignType

Sets the visual style of the map. This operation reloads the map style from the URL provided by the
`MapLibreMapDesignTypeInterface` and re-applies all existing overlays.

**Signature**
```kotlin
fun setMapDesignType(value: MapLibreMapDesignTypeInterface)
```

**Parameters**
- `value`
    - Type: `MapLibreMapDesignTypeInterface`
    - Description: The new map design to apply.

#### getStyleInstance

Gets the current MapLibre `Style` instance. This is useful for advanced, direct manipulation of the
map's style, sources, and layers if the standard controller methods are insufficient.

**Signature**
```kotlin
fun getStyleInstance(): Style?
```

**Returns**
- Type: `Style?`
    - Description: The current `Style` object, or `null` if the style has not been loaded yet.

#### setMapDesignTypeChangeListener

Registers a listener that is invoked when the map design type changes.

**Signature**
```kotlin
fun setMapDesignTypeChangeListener(listener: MapLibreDesignTypeChangeHandler)
```

**Parameters**
- `listener`
    - Type: `MapLibreDesignTypeChangeHandler`
    - Description: A lambda function `(MapLibreMapDesignTypeInterface) -> Unit` that will be called
                   with the new map design.

---

### Overlay Management

#### clearOverlays

Asynchronously removes all markers, polylines, polygons, circles, ground images, and raster layers
from the map.

**Signature**
```kotlin
suspend fun clearOverlays()
```

#### Composition Methods

These methods add a collection of map elements to the map. They are efficient for adding multiple
items at once, for example, during initial map setup.

    - Description: Adds a list of markers to the map.
    - Description: Adds a list of polylines to the map.
    - Description: Adds a list of polygons to the map.
    - Description: Adds a list of circles to the map.
    - Description: Adds a list of ground images to the map.
    - Description: Adds a list of raster layers to the map.

**Example**
```kotlin
val markers = listOf(
    MarkerState(id = "marker1", position = GeoPoint(48.8584, 2.2945)),
    MarkerState(id = "marker2", position = GeoPoint(48.8606, 2.3376))
)
mapViewController.compositionMarkers(markers)
```

#### Update Methods

These methods update the properties of a single, existing map element, identified by the `id` within
its state object.

    - Description: Updates an existing marker.
    - Description: Updates an existing polyline.
    - Description: Updates an existing polygon.
    - Description: Updates an existing circle.
    - Description: Updates an existing ground image.
    - Description: Updates an existing raster layer.

**Example**
```kotlin
// Assuming a marker with id "marker1" already exists
val updatedMarker = MarkerState(
    id = "marker1",
    position = GeoPoint(48.8584, 2.2945),
    alpha = 0.5f // Make it semi-transparent
)
mapViewController.updateMarker(updatedMarker)
```

#### "Has" Methods

These methods check if a specific map element exists on the map, identified by the `id` in its state
object.

    - Description: Checks if the marker exists.
    - Description: Checks if the polyline exists.
    - Description: Checks if the polygon exists.
    - Description: Checks if the circle exists.
    - Description: Checks if the ground image exists.
    - Description: Checks if the raster layer exists.

**Example**
```kotlin
val markerToCheck = MarkerState(id = "marker1", position = GeoPoint(0.0, 0.0))
if (mapViewController.hasMarker(markerToCheck)) {
    println("Marker 'marker1' is on the map.")
}
```

---

### Advanced Customization

These methods are for advanced use cases that require custom rendering or event handling logic for
markers.

#### createMarkerRenderer

A factory method to create a specialized `MarkerOverlayRendererInterface` for a custom marker
rendering strategy.

**Signature**
```kotlin
fun createMarkerRenderer(
    strategy: MarkerRenderingStrategyInterface<MapLibreActualMarker>
): MarkerOverlayRendererInterface<MapLibreActualMarker>
```

**Parameters**
- `strategy`
    - Type: `MarkerRenderingStrategyInterface<MapLibreActualMarker>`
    - Description: The custom rendering strategy that defines how markers are managed and displayed.

**Returns**
- Type: `MarkerOverlayRendererInterface<MapLibreActualMarker>`
    - Description: A new renderer instance tailored to the provided strategy.

#### createMarkerEventController

A factory method to create a `MarkerEventControllerInterface` that links a custom strategy
controller with its renderer.

**Signature**
```kotlin
fun createMarkerEventController(
    controller: StrategyMarkerController<MapLibreActualMarker>,
    renderer: MarkerOverlayRendererInterface<MapLibreActualMarker>
): MarkerEventControllerInterface<MapLibreActualMarker>
```

**Parameters**
- `controller`
    - Type: `StrategyMarkerController<MapLibreActualMarker>`
    - Description: The custom strategy controller.
- `renderer`
    - Type: `MarkerOverlayRendererInterface<MapLibreActualMarker>`
    - Description: The renderer created for the strategy.

**Returns**
- Type: `MarkerEventControllerInterface<MapLibreActualMarker>`
    - Description: A new event controller that bridges the strategy controller and its renderer.

#### registerMarkerEventController

Registers a custom marker event controller with the main view controller. This allows custom marker
types to receive click and drag events through the main event pipeline.

**Signature**
```kotlin
fun registerMarkerEventController(controller: MarkerEventControllerInterface<MapLibreActualMarker>)
```

**Parameters**
- `controller`
    - Type: `MarkerEventControllerInterface<MapLibreActualMarker>`
    - Description: The event controller to register.

---

### Deprecated Event Listeners

The following methods for setting event listeners are deprecated. It is recommended to set event
callbacks directly on the respective state objects (e.g., `MarkerState.onClick`,
`PolylineState.onClick`).

