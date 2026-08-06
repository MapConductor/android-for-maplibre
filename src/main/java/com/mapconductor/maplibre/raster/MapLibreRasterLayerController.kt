package com.mapconductor.maplibre.raster

import com.mapconductor.core.raster.RasterLayerController
import com.mapconductor.core.raster.RasterLayerEntity
import com.mapconductor.core.raster.RasterLayerManager
import com.mapconductor.core.raster.RasterLayerManagerInterface
import com.mapconductor.core.raster.RasterLayerOverlayRendererInterface
import com.mapconductor.core.raster.RasterLayerState

class MapLibreRasterLayerController(
    rasterLayerManager: RasterLayerManagerInterface<MapLibreRasterLayerHandle> = RasterLayerManager(),
    renderer: MapLibreRasterLayerOverlayRenderer,
) : RasterLayerController<MapLibreRasterLayerHandle>(rasterLayerManager, renderer) {
    /**
     * ヘッダはレイヤを足す**前に**登録する。あとからだと最初のタイル要求が
     * ヘッダ無しで飛び、認証が要るサーバでは初回だけ 401 になる。
     */
    override suspend fun add(data: List<RasterLayerState>) {
        MapLibreRasterHeaderInjector.apply(data, this)
        super.add(data)
    }

    override suspend fun update(state: RasterLayerState) {
        val merged = rasterLayerManager.allEntities().map { if (it.state.id == state.id) state else it.state }
        MapLibreRasterHeaderInjector.apply(merged, this)
        super.update(state)
    }

    override suspend fun clear() {
        MapLibreRasterHeaderInjector.remove(this)
        super.clear()
    }

    override fun destroy() {
        MapLibreRasterHeaderInjector.remove(this)
        super.destroy()
    }

    suspend fun reapplyStyle() {
        val states = rasterLayerManager.allEntities().map { it.state }
        if (states.isEmpty()) return
        val addParams =
            states.map { state ->
                object : RasterLayerOverlayRendererInterface.AddParamsInterface {
                    override val state: RasterLayerState = state
                }
            }
        val layers = renderer.onAdd(addParams)
        layers.forEachIndexed { index, layer ->
            layer?.let {
                rasterLayerManager.registerEntity(
                    RasterLayerEntity(
                        layer = it,
                        state = states[index],
                    ),
                )
            }
        }
        renderer.onPostProcess()
    }
}
