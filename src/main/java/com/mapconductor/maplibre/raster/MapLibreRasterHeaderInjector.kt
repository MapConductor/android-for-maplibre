package com.mapconductor.maplibre.raster

import com.mapconductor.core.raster.RasterHeaderRuleSet
import com.mapconductor.core.raster.RasterLayerState
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.maplibre.android.module.http.HttpRequestUtil
import android.os.Build

/**
 * [RasterLayerState] の `userAgent` / `extraHeaders` を MapLibre のリクエストに載せる。
 *
 * MapLibre Android はタイル取得を OkHttp で行い、使うクライアントを
 * `HttpRequestUtil.setOkHttpClient()` で差し替えられる。ここに Interceptor 付きの
 * クライアントを渡してヘッダを差す。
 *
 * 差し込み先は**プロセス全体で 1 つ**。地図が複数あっても、`android-for-maptiler` が
 * 同居していても壊れないよう、この object は 1 つだけで、規則そのものは
 * [RasterHeaderRuleSet.shared] に置く。MapTiler 側の同等品も同じ置き場を見るので、
 * どちらのクライアントが最終的に載っていても結果が変わらない。
 *
 * ヘッダはラスタタイルの**配信ホスト宛にだけ**載せる。全リクエストに載せると、
 * ラスタレイヤを 1 枚置いただけでベースマップのスタイル取得の User-Agent まで
 * 書き換わる（`RasterLayerState.userAgent` の既定値は空ではない）。
 *
 * ios-sdk の `MapLibreRasterHeaderInjector` と同じ役割・同じ絞り方。
 */
object MapLibreRasterHeaderInjector {
    /**
     * 差し替え用クライアント。
     *
     * MapLibre の既定クライアント（`HttpRequestImpl.DEFAULT_CLIENT`）は
     * `OkHttpClient.Builder().dispatcher(...).build()` で、Dispatcher の
     * `maxRequestsPerHost` だけを変えている。差し替えでその調整を失わないよう、
     * 同じ設定を再現したうえで Interceptor を足す。
     */
    private val client: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            .dispatcher(
                Dispatcher().apply {
                    maxRequestsPerHost = if (Build.VERSION.SDK_INT >= 21) 20 else 10
                },
            ).addInterceptor(HeaderInterceptor())
            .build()
    }

    /** 自分がフックを差しているか。他所が差したものを勝手に外さないための目印。 */
    private var installed = false

    /** 登録元 1 つ分の規則を差し替え、必要に応じてフックを着脱する。 */
    @Synchronized
    fun apply(
        states: List<RasterLayerState>,
        owner: Any,
    ) {
        RasterHeaderRuleSet.shared.setRules(RasterHeaderRuleSet.makeRules(states), owner)
        syncClient()
    }

    /** 登録元 1 つ分の規則を外す。 */
    @Synchronized
    fun remove(owner: Any) {
        RasterHeaderRuleSet.shared.removeRules(owner)
        syncClient()
    }

    /**
     * 規則が無いときはフックを外して MapLibre 既定の挙動に戻す。
     * 付けっぱなしにすると、ヘッダを一切指定していないアプリにも余計な処理が挟まる。
     */
    private fun syncClient() {
        if (RasterHeaderRuleSet.shared.isEmpty) {
            if (installed) {
                // null を渡すと MapLibre は DEFAULT_CLIENT に戻す。
                HttpRequestUtil.setOkHttpClient(null)
                installed = false
            }
        } else if (!installed) {
            HttpRequestUtil.setOkHttpClient(client)
            installed = true
        }
    }

    private class HeaderInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val rule =
                RasterHeaderRuleSet.shared.headersFor(original.url.toString())
                    ?: return chain.proceed(original)

            val builder = original.newBuilder()
            rule.userAgent?.let { builder.header("User-Agent", it) }
            rule.extraHeaders.forEach { (key, value) -> builder.header(key, value) }
            return chain.proceed(builder.build())
        }
    }
}
