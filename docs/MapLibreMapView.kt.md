# MapLibreMapView

## `MapLibreMapView`

A Jetpack Compose component for displaying an interactive MapLibre map. This is the primary entry
point for integrating MapLibre into a Compose application.

This composable manages the map's lifecycle, state, camera position, and user interactions. Map
overlays such as markers, polylines, and polygons are added as children within the trailing
`content` lambda, which provides a `MapLibreMapViewScope`.

### Signature
```kotlin
@Composable
fun MapLibreMapView(
    state: MapLibreViewState,
    modifier: Modifier = Modifier,
    markerTiling: MarkerTilingOptions? = null,
    sdkInitialize: (suspend (android.content.Context) -> Boolean)? = null,
    onMapLoaded: OnMapLoadedHandler? = null,
    onMapClick: OnMapEventHandler? = null,
    onCameraMoveStart: OnCameraMoveHandler? = null,
    onCameraMove: OnCameraMoveHandler? = null,
    onCameraMoveEnd: OnCameraMoveHandler? = null,
    content: (@Composable MapLibreMapViewScope.() -> Unit)? = null,
)
```

### Description
This function sets up the MapLibre map view and binds it to the provided `MapLibreViewState`. It
handles the initialization of the MapLibre SDK, map style loading, and camera updates. Event
listeners for map interactions like clicks and camera movements can be provided.

### Parameters
- `state`
    - Type: `MapLibreViewState`
    - Description: The state object that manages the map's properties, such as camera position and
                   map style. See `rememberMapLibreViewState`.
- `modifier`
    - Type: `Modifier`
    - Description: A `Modifier` to be applied to the map container.
- `markerTiling`
    - Type: `MarkerTilingOptions?`
    - Description: Optional configuration for marker tiling and clustering. Defaults to `null`.
- `sdkInitialize`
    - Type: `(suspend (Context) -> Boolean)?`
    - Description: An optional suspend lambda to perform custom initialization of the MapLibre SDK.
                   If `null`, the default `MapLibre.getInstance(context)` is used.
- `onMapLoaded`
    - Type: `OnMapLoadedHandler?`
    - Description: A callback invoked when the map and its style have been fully loaded and the map
                   is ready for interaction.
- `onMapClick`
    - Type: `OnMapEventHandler?`
    - Description: A callback invoked when the user clicks on the map. It receives the `GeoPoint` of
                   the click location.
- `onCameraMoveStart`
    - Type: `OnCameraMoveHandler?`
    - Description: A callback invoked when the camera starts moving.
- `onCameraMove`
    - Type: `OnCameraMoveHandler?`
    - Description: A callback invoked repeatedly while the camera is moving.
- `onCameraMoveEnd`
    - Type: `OnCameraMoveHandler?`
    - Description: A callback invoked when the camera movement has finished.
- `content`
    - Type: `(@Composable MapLibreMapViewScope.() -> Unit)?`
    - Description: A trailing lambda where you can declare map overlays like `Marker`, `Polyline`,
                   `Polygon`, and `Circle` within the `MapLibreMapViewScope`.

### Example
Here is a basic example of how to use `MapLibreMapView` in a Composable function.

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mapconductor.core.features.GeoPoint
import com.mapconductor.core.map.MapCameraPosition
import com.mapconductor.maplibre.MapLibreDesign
import com.mapconductor.maplibre.MapLibreMapView
import com.mapconductor.maplibre.rememberMapLibreMapViewState

@Composable
fun MyMapScreen() {
    // Create and remember the map's state
    val mapState = rememberMapLibreMapViewState(
        mapDesign = MapLibreDesign.OsmBright,
        cameraPosition = MapCameraPosition(
            position = GeoPoint(37.7749, -122.4194), // San Francisco
            zoom = 12.0
        )
    )

    MapLibreMapView(
        state = mapState,
        modifier = Modifier.fillMaxSize(),
        onMapLoaded = {
            println("Map is fully loaded and ready.")
        },
        onMapClick = { cameraPosition ->
            println("Map clicked, camera at: $cameraPosition")
        }
    ) {
        // Add map overlays within this scope
    }
}
```

---

## `MapLibreMapView` (Deprecated)

**Deprecated:** Use the primary `MapLibreMapView` composable instead. The event handlers for map
overlays (`onMarkerClick`, `onPolylineClick`, etc.) have been moved to their respective state
objects (e.g., `rememberMarkerState(onClick = { ... })`). This provides a more granular and
Compose-idiomatic way to handle events.

### Signature
```kotlin
@Deprecated("Use CircleState/PolylineState/PolygonState onClick instead.")
@Composable
fun MapLibreMapView(
    state: MapLibreViewState,
    modifier: Modifier = Modifier,
    markerTiling: MarkerTilingOptions? = null,
    sdkInitialize: (suspend (android.content.Context) -> Boolean)? = null,
    onMapLoaded: OnMapLoadedHandler? = null,
    onMapClick: OnMapEventHandler? = null,
    onCameraMoveStart: OnCameraMoveHandler? = null,
    onCameraMove: OnCameraMoveHandler? = null,
    onCameraMoveEnd: OnCameraMoveHandler? = null,
    onMarkerClick: OnMarkerEventHandler?,
    onMarkerDragStart: OnMarkerEventHandler? = null,
    onMarkerDrag: OnMarkerEventHandler? = null,
    onMarkerDragEnd: OnMarkerEventHandler? = null,
    onMarkerAnimateStart: OnMarkerEventHandler? = null,
    onMarkerAnimateEnd: OnMarkerEventHandler? = null,
    onPolylineClick: OnPolylineEventHandler? = null,
    onCircleClick: OnCircleEventHandler? = null,
    onPolygonClick: OnPolygonEventHandler? = null,
    content: (@Composable MapLibreMapViewScope.() -> Unit)? = null,
)
```

### Description
This deprecated version of `MapLibreMapView` provides global event handlers for all markers,
polylines, circles, and polygons on the map. For new implementations, it is strongly recommended to
use the primary `MapLibreMapView` and handle click events on the state objects of individual
overlays (e.g., `rememberMarkerState`, `rememberPolylineState`).

### Parameters
- `state`
    - Type: `MapLibreViewState`
    - Description: The state object that manages the map's properties.
- `modifier`
    - Type: `Modifier`
    - Description: A `Modifier` to be applied to the map container.
- `markerTiling`
    - Type: `MarkerTilingOptions?`
    - Description: Optional configuration for marker tiling and clustering.
- `sdkInitialize`
    - Type: `(suspend (Context) -> Boolean)?`
    - Description: Optional lambda for custom SDK initialization.
- `onMapLoaded`
    - Type: `OnMapLoadedHandler?`
    - Description: Callback invoked when the map is fully loaded.
- `onMapClick`
    - Type: `OnMapEventHandler?`
    - Description: Callback invoked when the user clicks on the map.
- `onCameraMoveStart`
    - Type: `OnCameraMoveHandler?`
    - Description: Callback invoked when the camera starts moving.
- `onCameraMove`
    - Type: `OnCameraMoveHandler?`
    - Description: Callback invoked while the camera is moving.
- `onCameraMoveEnd`
    - Type: `OnCameraMoveHandler?`
    - Description: Callback invoked when the camera movement has finished.
- `onMarkerClick`
    - Type: `OnMarkerEventHandler?`
    - Description: **Deprecated.** A global callback for marker click events. Use the `onClick`
                   parameter in `rememberMarkerState` instead.
- `onMarkerDragStart`
    - Type: `OnMarkerEventHandler?`
    - Description: **Deprecated.** A global callback for marker drag start events. Use the
                   `onDragStart` parameter in `rememberMarkerState` instead.
- `onMarkerDrag`
    - Type: `OnMarkerEventHandler?`
    - Description: **Deprecated.** A global callback for marker drag events. Use the `onDrag`
                   parameter in `rememberMarkerState` instead.
- `onMarkerDragEnd`
    - Type: `OnMarkerEventHandler?`
    - Description: **Deprecated.** A global callback for marker drag end events. Use the `onDragEnd`
                   parameter in `rememberMarkerState` instead.
- `onMarkerAnimateStart`
    - Type: `OnMarkerEventHandler?`
    - Description: **Deprecated.** A global callback for marker animation start events.
- `onMarkerAnimateEnd`
    - Type: `OnMarkerEventHandler?`
    - Description: **Deprecated.** A global callback for marker animation end events.
- `onPolylineClick`
    - Type: `OnPolylineEventHandler?`
    - Description: **Deprecated.** A global callback for polyline click events. Use the `onClick`
                   parameter in `rememberPolylineState` instead.
- `onCircleClick`
    - Type: `OnCircleEventHandler?`
    - Description: **Deprecated.** A global callback for circle click events. Use the `onClick`
                   parameter in `rememberCircleState` instead.
- `onPolygonClick`
    - Type: `OnPolygonEventHandler?`
    - Description: **Deprecated.** A global callback for polygon click events. Use the `onClick`
                   parameter in `rememberPolygonState` instead.
- `content`
    - Type: `(@Composable MapLibreMapViewScope.() -> Unit)?`
    - Description: A trailing lambda for declaring map overlays.
