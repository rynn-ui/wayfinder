package com.roadguardian.app.ui.map

import android.content.Context
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import java.io.File

interface MapTileProvider {
    val name: String
    val attribution: String
    fun getTileSource(): ITileSource
    fun configure(context: Context)
}

class OpenStreetMapTileProvider(
    private val customUserAgent: String = "WayFinder-Android/1.0 (Road Condition Intelligence)"
) : MapTileProvider {

    override val name: String = "OpenStreetMap Standard"
    override val attribution: String = "© OpenStreetMap contributors"

    override fun getTileSource(): ITileSource = TileSourceFactory.MAPNIK

    override fun configure(context: Context) {
        val config = Configuration.getInstance()
        config.userAgentValue = customUserAgent

        val osmCacheDir = File(context.cacheDir, "osmdroid")
        if (!osmCacheDir.exists()) {
            osmCacheDir.mkdirs()
        }
        config.osmdroidBasePath = osmCacheDir
        config.osmdroidTileCache = File(osmCacheDir, "tiles")
        config.tileFileSystemCacheMaxBytes = 50L * 1024L * 1024L
        config.tileFileSystemCacheTrimBytes = 40L * 1024L * 1024L
    }
}
