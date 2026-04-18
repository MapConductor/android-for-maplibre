# MapLibreViewControllerInterface

## Description

The `MapLibreViewControllerInterface` serves as the primary controller for a MapLibre-based map
view. It provides a comprehensive API for managing and interacting with various map features and
layers.

This interface aggregates functionality from several capability-based interfaces, offering a unified
way to handle map objects such as markers, polylines, polygons, circles, ground images, and raster
layers. It also includes methods specific to the MapLibre implementation for managing map styles and
designs.

It inherits from the following interfaces:
*   `MapViewControllerInterface`
*   `MarkerCapableInterface`
*   `PolylineCapableInterface`
*   `PolygonCapableInterface`
*   `CircleCapableInterface`
*   `GroundImageCapableInterface`
*   `RasterLayerCapableInterface`

---

## Methods

### setMapDesignType

#### Signature
```kotlin
fun setMapDesignType(value: MapLibreMapDesignTypeInterface)
```

#### Description
Sets or updates the visual design (style) of the map. This method allows you to dynamically change
the map's appearance by applying a new map design, such as switching between street, satellite, or
custom-styled maps.

#### Parameters
- `value`
    - Type: `MapLibreMapDesignTypeInterface`
    - Description: The new map design to be applied to the map.

#### Returns
This method does not return a value.

---

### setMapDesignTypeChangeListener

#### Signature
```kotlin
fun setMapDesignTypeChangeListener(listener: MapLibreDesignTypeChangeHandler)
```

#### Description
Registers a listener that will be invoked whenever the map's design type changes. This is useful for
performing actions in response to a style change, such as updating UI elements that depend on the
current map theme.

#### Parameters
- `listener`
    - Type: `MapLibreDesignTypeChangeHandler`
    - Description: A callback handler that will be executed when the map design is changed.

#### Returns
This method does not return a value.

---

## Example

The following example demonstrates how to use `MapLibreViewControllerInterface` to set a new map
design and listen for design changes.

```kotlin
// Assume 'mapController' is an instance of MapLibreViewControllerInterface
// and 'MyCustomMapDesign' implements MapLibreMapDesignTypeInterface.

// 1. Set a listener to get notified of map design changes
mapController.setMapDesignTypeChangeListener { newDesign ->
    // The 'newDesign' parameter is of type MapLibreMapDesignTypeInterface
    println("Map design changed to: ${newDesign.id}")
    // You can now update your app's UI or logic based on the new design
}

// 2. Apply a new map design to the map
mapController.setMapDesignType(MapLibreDesign.OsmBright)
// Console output: "Map design changed to: osm-bright"
```