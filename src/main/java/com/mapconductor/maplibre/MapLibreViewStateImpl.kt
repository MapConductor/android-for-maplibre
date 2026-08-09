package com.mapconductor.maplibre

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.mapconductor.compose.map.BaseMapViewSaver
import com.mapconductor.core.map.MapCameraPosition
import com.mapconductor.core.map.MapCameraPositionInterface
import com.mapconductor.core.map.MapViewState
import com.mapconductor.core.map.MapViewStateInterface
import java.util.UUID
import android.os.Bundle

interface MapLibreViewStateInterface : MapViewStateInterface<MapLibreMapDesignTypeInterface>

class MapLibreViewState(
    mapDesignType: MapLibreMapDesignTypeInterface,
    override val id: String,
    cameraPosition: MapCameraPosition = MapCameraPosition.Default,
) : MapViewState<MapLibreMapDesignTypeInterface>(cameraPosition),
    MapLibreViewStateInterface {
    private var controller: MapLibreViewControllerInterface? = null
    private var _mapDesignType: MapLibreMapDesignTypeInterface = mapDesignType

    override var mapDesignType: MapLibreMapDesignTypeInterface
        set(value) {
            _mapDesignType = value
            this.controller?.setMapDesignType(value)
        }
        get() = _mapDesignType

    internal fun setController(controller: MapLibreViewControllerInterface) {
        this.controller = controller
        attachController(controller)
    }

    internal fun onMapDesignTypeChange(value: MapLibreMapDesignTypeInterface) {
        _mapDesignType = value
    }

    /** 戻り型をこのプロバイダのホルダーへ絞る（アプリが `?.map` を取れる形を保つため）。 */
    override fun getMapViewHolder(): MapLibreMapViewHolderInterface? =
        super.getMapViewHolder() as? MapLibreMapViewHolderInterface

    internal fun updateCameraPosition(cameraPosition: MapCameraPosition) {
        setCameraPositionInternal(cameraPosition)
    }
}

class MapLibreMapViewSaver : BaseMapViewSaver<MapLibreViewState>() {
    override fun saveMapDesign(
        state: MapLibreViewState,
        bundle: Bundle,
    ) {
        bundle.putString("styleJsonURL", state.mapDesignType.styleJsonURL)
    }

    override fun createState(
        stateId: String,
        mapDesignBundle: Bundle?,
        cameraPosition: MapCameraPosition,
    ): MapLibreViewState =
        MapLibreViewState(
            id = stateId,
            mapDesignType =
                MapLibreDesign(
                    id =
                        mapDesignBundle?.getString("id")
                            ?: MapLibreDesign.OsmBright.id,
                    styleJsonURL =
                        mapDesignBundle?.getString("styleJsonURL")
                            ?: MapLibreDesign.OsmBright.styleJsonURL,
                ),
            cameraPosition = cameraPosition,
        )

    override fun getStateId(state: MapLibreViewState): String = state.id
}

@Composable
fun rememberMapLibreMapViewState(
    mapDesign: MapLibreMapDesignTypeInterface = MapLibreDesign.DemoTiles,
    cameraPosition: MapCameraPositionInterface = MapCameraPosition.Default,
): MapLibreViewState {
    val stateId by rememberSaveable {
        val uuid = UUID.randomUUID().toString()
        mutableStateOf(uuid)
    }
    val state =
        rememberSaveable(
            stateSaver = MapLibreMapViewSaver().createSaver(),
        ) {
            mutableStateOf(
                MapLibreViewState(
                    id = stateId,
                    mapDesignType = mapDesign,
                    cameraPosition = MapCameraPosition.from(cameraPosition),
                ),
            )
        }

    return state.value
}
