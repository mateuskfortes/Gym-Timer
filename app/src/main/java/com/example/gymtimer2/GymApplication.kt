@file:Suppress("detekt:DocumentationOverPrivateFunction")

package com.example.gymtimer2

import android.app.Application
import org.maplibre.android.MapLibre
import org.maplibre.android.module.http.HttpRequestUtil
import com.example.gymtimer2.data.AppContainer
import com.example.gymtimer2.player.MusicPlayerManager
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

/** Cria dependências compartilhadas e configura as requisições de tiles OSM. */
class GymApplication : Application() {

    lateinit var container: AppContainer
    lateinit var musicPlayerManager: MusicPlayerManager

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        musicPlayerManager = MusicPlayerManager(this)
        initializeMap()
    }

    /** Configura o MapLibre e um cache HTTP com User-Agent do aplicativo. */
    private fun initializeMap() {
        MapLibre.getInstance(this)
        val cache = Cache(File(cacheDir, MAP_CACHE_DIRECTORY), MAP_CACHE_SIZE_BYTES)
        val userAgent = "GymTimer2/${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"
        val client = OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor { chain ->
                val request = chain.request()
                if (request.url.host == OSM_TILE_HOST) {
                    chain.proceed(
                        request.newBuilder()
                            .header("User-Agent", userAgent)
                            .build()
                    )
                } else {
                    chain.proceed(request)
                }
            }
            .build()
        HttpRequestUtil.setOkHttpClient(client)
    }

    private companion object {
        const val MAP_CACHE_DIRECTORY = "run-map-cache"
        const val MAP_CACHE_SIZE_BYTES = 64L * 1024L * 1024L
        const val OSM_TILE_HOST = "tile.openstreetmap.org"
    }
}
