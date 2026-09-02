package com.roadguardian.app.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.roadguardian.app.domain.model.HazardSeverity
import com.roadguardian.app.domain.model.RoadHazard
import com.roadguardian.app.location.LocationState
import com.roadguardian.app.traffic.model.WazeJamSeverity
import com.roadguardian.app.traffic.model.WazeTrafficIncident
import com.roadguardian.app.traffic.model.WazeTrafficType
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

@Composable
fun OsmMapView(
    hazards: List<RoadHazard>,
    locationState: LocationState,
    routePoints: List<GeoPoint> = emptyList(),
    selectedHazard: RoadHazard? = null,
    onHazardSelected: (RoadHazard) -> Unit = {},
    trafficIncidents: List<WazeTrafficIncident> = emptyList(),
    isTrafficLayerVisible: Boolean = true,
    selectedTrafficIncident: WazeTrafficIncident? = null,
    onTrafficIncidentSelected: (WazeTrafficIncident) -> Unit = {},
    tileProvider: MapTileProvider = remember { OpenStreetMapTileProvider() },
    isFollowUser: Boolean = false,
    isFollowHeading: Boolean = false,
    onUserPan: () -> Unit = {},
    onMapReady: ((MapViewControls) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    if (isPreview) {
        return
    }

    var hasCenteredOnFirstFix by remember { mutableStateOf(locationState.isAvailable && locationState.latitude != 0.0) }
    var currentOrientation by remember { mutableFloatStateOf(0f) }

    val initialTarget = remember {
        when {
            selectedHazard != null && selectedHazard.deviceLatitude != 0.0 ->
                GeoPoint(selectedHazard.deviceLatitude, selectedHazard.deviceLongitude)
            locationState.isAvailable && locationState.latitude != 0.0 ->
                GeoPoint(locationState.latitude, locationState.longitude)
            hazards.isNotEmpty() && hazards.first().deviceLatitude != 0.0 ->
                GeoPoint(hazards.first().deviceLatitude, hazards.first().deviceLongitude)
            else -> GeoPoint(26.4499, 80.3319)
        }
    }

    val mapView = remember {
        tileProvider.configure(context)
        MapView(context).apply {
            setTileSource(tileProvider.getTileSource())
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(17.0)
            controller.setCenter(initialTarget)
            isTilesScaledToDpi = true
            minZoomLevel = 4.0
            maxZoomLevel = 20.0

            val rotationOverlay = RotationGestureOverlay(this).apply {
                isEnabled = true
            }
            overlays.add(rotationOverlay)

            val compassOverlay = CompassOverlay(context, InternalCompassOrientationProvider(context), this).apply {
                enableCompass()
            }
            overlays.add(compassOverlay)

            overlays.add(object : Overlay() {
                override fun onTouchEvent(event: MotionEvent?, mapView: MapView?): Boolean {
                    if (event != null && (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_SCROLL)) {
                        onUserPan()
                    }
                    return false
                }
            })
        }
    }

    fun instantCenter(latitude: Double, longitude: Double, zoom: Double = 17.0) {
        if (latitude == 0.0 && longitude == 0.0) return
        val target = GeoPoint(latitude, longitude)
        mapView.controller.setZoom(zoom)
        mapView.controller.setCenter(target)
    }

    fun safeAnimateTo(latitude: Double, longitude: Double, zoom: Double = 17.0) {
        if (latitude == 0.0 && longitude == 0.0) return
        val target = GeoPoint(latitude, longitude)
        if (mapView.width > 0 && mapView.height > 0) {
            mapView.controller.setZoom(zoom)
            mapView.controller.animateTo(target)
        } else {
            instantCenter(latitude, longitude, zoom)
        }
    }

    LaunchedEffect(locationState.isAvailable, locationState.latitude, locationState.longitude, hazards.size) {
        if (!hasCenteredOnFirstFix) {
            if (locationState.isAvailable && locationState.latitude != 0.0) {
                hasCenteredOnFirstFix = true
                instantCenter(locationState.latitude, locationState.longitude, 17.0)
            } else if (hazards.isNotEmpty()) {
                val target = hazards.maxByOrNull { it.timestamp } ?: hazards.first()
                if (target.deviceLatitude != 0.0) {
                    hasCenteredOnFirstFix = true
                    instantCenter(target.deviceLatitude, target.deviceLongitude, 17.0)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val controls = MapViewControls(
            zoomIn = { mapView.controller.zoomIn() },
            zoomOut = { mapView.controller.zoomOut() },
            resetNorth = {
                mapView.mapOrientation = 0.0f
                currentOrientation = 0.0f
            },
            animateToLocation = { lat, lon, zoom ->
                safeAnimateTo(lat, lon, zoom)
            },
            getOrientation = {
                mapView.mapOrientation
            }
        )
        onMapReady?.invoke(controls)
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(locationState.isAvailable, locationState.latitude, locationState.longitude, isFollowUser) {
        if (locationState.isAvailable && locationState.latitude != 0.0 && isFollowUser) {
            instantCenter(locationState.latitude, locationState.longitude, 17.0)
        }
    }

    LaunchedEffect(locationState.bearing, isFollowHeading) {
        if (isFollowHeading && locationState.bearing != null) {
            mapView.mapOrientation = -locationState.bearing
            currentOrientation = mapView.mapOrientation
        }
    }

    var userMarker by remember { mutableStateOf<Marker?>(null) }

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize(),
        update = { map ->
            val nonDynamicOverlays = map.overlays.filter { it !is Marker && it !is Polyline }
            map.overlays.clear()
            map.overlays.addAll(nonDynamicOverlays)

            if (isTrafficLayerVisible) {
                for (incident in trafficIncidents) {
                    if (incident.jamPolyline.size >= 2) {
                        val jamLine = Polyline(map).apply {
                            setPoints(incident.jamPolyline)
                            outlinePaint.color = when (incident.severity) {
                                WazeJamSeverity.STANDSTILL -> Color.rgb(153, 27, 27)
                                WazeJamSeverity.HEAVY -> Color.rgb(239, 68, 68)
                                WazeJamSeverity.MODERATE -> Color.rgb(245, 158, 11)
                                WazeJamSeverity.FREE_FLOW -> Color.rgb(16, 185, 129)
                            }
                            outlinePaint.strokeWidth = 14.0f
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                        }
                        map.overlays.add(jamLine)
                    }
                }
            }

            if (routePoints.isNotEmpty()) {
                val polyline = Polyline(map).apply {
                    setPoints(routePoints)
                    outlinePaint.color = Color.rgb(52, 211, 153)
                    outlinePaint.strokeWidth = 10.0f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                }
                map.overlays.add(polyline)
            }

            if (isTrafficLayerVisible) {
                for (incident in trafficIncidents) {
                    if (incident.latitude == 0.0 && incident.longitude == 0.0) continue
                    val isSelected = incident.id == selectedTrafficIncident?.id
                    val trafficMarker = Marker(map).apply {
                        position = GeoPoint(incident.latitude, incident.longitude)
                        title = incident.type.displayName
                        snippet = "${incident.streetName} • ${incident.formattedDelay}"
                        icon = getCachedTrafficMarkerIcon(context, incident, isSelected)
                        setAnchor(Marker.ANCHOR_CENTER, 0.90f)
                        setOnMarkerClickListener { _, _ ->
                            onTrafficIncidentSelected(incident)
                            true
                        }
                    }
                    map.overlays.add(trafficMarker)
                }
            }

            for (hazard in hazards) {
                if (hazard.deviceLatitude == 0.0 && hazard.deviceLongitude == 0.0) continue
                val isSelected = hazard.id == selectedHazard?.id
                val marker = Marker(map).apply {
                    position = GeoPoint(hazard.deviceLatitude, hazard.deviceLongitude)
                    title = hazard.displayTitle
                    snippet = "${hazard.severity.uppercase()} • ${(hazard.confidence * 100).toInt()}% • ${hazard.confirmationCount} reports"
                    icon = getCachedMarkerIcon(context, hazard.hazardSeverity, isSelected)
                    setAnchor(Marker.ANCHOR_CENTER, 0.90f)
                    setOnMarkerClickListener { _, _ ->
                        onHazardSelected(hazard)
                        true
                    }
                }
                map.overlays.add(marker)
            }

            if (locationState.isAvailable && locationState.latitude != 0.0) {
                val marker = userMarker ?: Marker(map).also { userMarker = it }
                marker.position = GeoPoint(locationState.latitude, locationState.longitude)
                marker.icon = getCachedUserLocationIcon(context, locationState.bearing)
                marker.setAnchor(Marker.ANCHOR_CENTER, 0.85f)
                marker.setOnMarkerClickListener { _, _ -> true }
                map.overlays.add(marker)
            }

            map.invalidate()
        }
    )
}

private val hazardIconCache = mutableMapOf<String, BitmapDrawable>()
private val trafficIconCache = mutableMapOf<String, BitmapDrawable>()
private var userIconCache: BitmapDrawable? = null

private fun getCachedMarkerIcon(context: Context, severity: HazardSeverity, isSelected: Boolean): BitmapDrawable {
    val key = "${severity.name}_$isSelected"
    return hazardIconCache.getOrPut(key) {
        createMarkerIcon(context, severity, isSelected)
    }
}

private fun getCachedTrafficMarkerIcon(context: Context, incident: WazeTrafficIncident, isSelected: Boolean): BitmapDrawable {
    val label = when (incident.type) {
        WazeTrafficType.JAM -> if (incident.delayMinutes > 0) "${incident.delayMinutes}m" else "JAM"
        WazeTrafficType.ACCIDENT -> "ACC"
        WazeTrafficType.ROAD_CLOSED -> "X"
        else -> "!"
    }
    val key = "${incident.type.name}_${incident.severity.name}_${label}_$isSelected"
    return trafficIconCache.getOrPut(key) {
        createTrafficMarkerIcon(context, incident, isSelected)
    }
}

private fun getCachedUserLocationIcon(context: Context, bearing: Float?): BitmapDrawable {
    var cached = userIconCache
    if (cached == null) {
        cached = createUserLocationIcon(context, bearing)
        userIconCache = cached
    }
    return cached
}

data class MapViewControls(
    val zoomIn: () -> Unit = {},
    val zoomOut: () -> Unit = {},
    val resetNorth: () -> Unit = {},
    val animateToLocation: (Double, Double, Double) -> Unit = { _, _, _ -> },
    val getOrientation: () -> Float = { 0f }
)

private fun createTrafficMarkerIcon(
    context: Context,
    incident: WazeTrafficIncident,
    isSelected: Boolean
): BitmapDrawable {
    val width = if (isSelected) 68 else 54
    val height = if (isSelected) 84 else 68
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = width / 2.0f
    val tipY = height - 10.0f
    val bulbCenterY = (height * 0.36f)
    val bulbRadius = width * 0.34f

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val shadowY = height - 6.0f
        val rx = if (isSelected) 22.0f else 16.0f
        val ry = if (isSelected) 7.0f else 5.0f
        shader = RadialGradient(
            cx, shadowY, rx,
            intArrayOf(Color.argb(130, 0, 0, 0), Color.argb(50, 0, 0, 0), Color.TRANSPARENT),
            floatArrayOf(0.0f, 0.55f, 1.0f),
            Shader.TileMode.CLAMP
        )
    }
    val shadowRect = RectF(
        cx - (if (isSelected) 22.0f else 16.0f),
        height - 12.0f,
        cx + (if (isSelected) 22.0f else 16.0f),
        height - 2.0f
    )
    canvas.drawOval(shadowRect, shadowPaint)

    val pinPath = Path().apply {
        val leftX = cx - bulbRadius
        val rightX = cx + bulbRadius
        moveTo(cx, tipY)
        cubicTo(
            cx - (bulbRadius * 0.6f), tipY - (height * 0.28f),
            leftX, bulbCenterY + (bulbRadius * 0.4f),
            leftX, bulbCenterY
        )
        arcTo(
            RectF(leftX, bulbCenterY - bulbRadius, rightX, bulbCenterY + bulbRadius),
            180f,
            180f,
            false
        )
        cubicTo(
            rightX, bulbCenterY + (bulbRadius * 0.4f),
            cx + (bulbRadius * 0.6f), tipY - (height * 0.28f),
            cx, tipY
        )
        close()
    }

    val (topColor, midColor, darkColor) = when (incident.type) {
        WazeTrafficType.JAM -> when (incident.severity) {
            WazeJamSeverity.STANDSTILL -> Triple(Color.rgb(185, 28, 28), Color.rgb(153, 27, 27), Color.rgb(127, 29, 29))
            WazeJamSeverity.HEAVY -> Triple(Color.rgb(239, 68, 68), Color.rgb(220, 38, 38), Color.rgb(185, 28, 28))
            WazeJamSeverity.MODERATE -> Triple(Color.rgb(245, 158, 11), Color.rgb(217, 119, 6), Color.rgb(180, 83, 9))
            WazeJamSeverity.FREE_FLOW -> Triple(Color.rgb(16, 185, 129), Color.rgb(5, 150, 105), Color.rgb(4, 120, 87))
        }
        WazeTrafficType.ACCIDENT -> Triple(Color.rgb(225, 29, 72), Color.rgb(190, 18, 60), Color.rgb(136, 19, 55))
        WazeTrafficType.ROAD_CLOSED -> Triple(Color.rgb(107, 114, 128), Color.rgb(75, 85, 99), Color.rgb(55, 65, 81))
        else -> Triple(Color.rgb(51, 204, 255), Color.rgb(0, 176, 255), Color.rgb(2, 132, 199))
    }

    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            cx, 4.0f,
            cx, tipY,
            intArrayOf(topColor, midColor, darkColor),
            floatArrayOf(0.0f, 0.45f, 1.0f),
            Shader.TileMode.CLAMP
        )
        style = Paint.Style.FILL
    }
    canvas.drawPath(pinPath, bodyPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = if (isSelected) 3.0f else 2.0f
        color = Color.WHITE
    }
    canvas.drawPath(pinPath, strokePaint)

    val discRadius = bulbRadius * 0.52f
    val innerDiscPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, bulbCenterY, discRadius, innerDiscPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = midColor
        textSize = if (isSelected) 17.0f else 14.0f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    val labelText = when (incident.type) {
        WazeTrafficType.JAM -> if (incident.delayMinutes > 0) "${incident.delayMinutes}m" else "JAM"
        WazeTrafficType.ACCIDENT -> "ACC"
        WazeTrafficType.ROAD_CLOSED -> "X"
        else -> "!"
    }

    val textY = bulbCenterY - ((textPaint.descent() + textPaint.ascent()) / 2.0f)
    canvas.drawText(labelText, cx, textY, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createMarkerIcon(context: Context, severity: HazardSeverity, isSelected: Boolean): BitmapDrawable {
    val width = if (isSelected) 74 else 60
    val height = if (isSelected) 96 else 78
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = width / 2.0f
    val tipY = height - 12.0f
    val bulbCenterY = (height * 0.36f)
    val bulbRadius = width * 0.34f

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        val shadowY = height - 8.0f
        val rx = if (isSelected) 24.0f else 18.0f
        val ry = if (isSelected) 8.0f else 6.0f
        shader = RadialGradient(
            cx, shadowY, rx,
            intArrayOf(Color.argb(140, 0, 0, 0), Color.argb(60, 0, 0, 0), Color.TRANSPARENT),
            floatArrayOf(0.0f, 0.55f, 1.0f),
            Shader.TileMode.CLAMP
        )
    }
    val shadowRect = RectF(
        cx - (if (isSelected) 24.0f else 18.0f),
        height - 14.0f,
        cx + (if (isSelected) 24.0f else 18.0f),
        height - 2.0f
    )
    canvas.drawOval(shadowRect, shadowPaint)

    if (isSelected) {
        val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, bulbCenterY, bulbRadius * 1.6f,
                intArrayOf(Color.argb(160, 239, 68, 68), Color.TRANSPARENT),
                floatArrayOf(0.4f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, bulbCenterY, bulbRadius * 1.6f, auraPaint)
    }

    val pinPath = Path().apply {
        val leftX = cx - bulbRadius
        val rightX = cx + bulbRadius
        moveTo(cx, tipY)
        cubicTo(
            cx - (bulbRadius * 0.6f), tipY - (height * 0.28f),
            leftX, bulbCenterY + (bulbRadius * 0.4f),
            leftX, bulbCenterY
        )
        arcTo(
            RectF(leftX, bulbCenterY - bulbRadius, rightX, bulbCenterY + bulbRadius),
            180f,
            180f,
            false
        )
        cubicTo(
            rightX, bulbCenterY + (bulbRadius * 0.4f),
            cx + (bulbRadius * 0.6f), tipY - (height * 0.28f),
            cx, tipY
        )
        close()
    }

    val (topColor, midColor, darkColor) = when (severity) {
        HazardSeverity.CRITICAL -> Triple(Color.rgb(255, 69, 58), Color.rgb(225, 29, 72), Color.rgb(136, 19, 55))
        HazardSeverity.HIGH -> Triple(Color.rgb(248, 113, 113), Color.rgb(239, 68, 68), Color.rgb(153, 27, 27))
        HazardSeverity.MEDIUM -> Triple(Color.rgb(251, 146, 60), Color.rgb(234, 88, 12), Color.rgb(154, 52, 18))
        HazardSeverity.LOW -> Triple(Color.rgb(250, 204, 21), Color.rgb(202, 138, 4), Color.rgb(133, 77, 14))
    }

    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            cx, 4.0f,
            cx, tipY,
            intArrayOf(topColor, midColor, darkColor),
            floatArrayOf(0.0f, 0.45f, 1.0f),
            Shader.TileMode.CLAMP
        )
        style = Paint.Style.FILL
    }
    canvas.drawPath(pinPath, bodyPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = if (isSelected) 3.5f else 2.5f
        color = Color.WHITE
    }
    canvas.drawPath(pinPath, strokePaint)

    val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            cx, bulbCenterY - bulbRadius,
            cx, bulbCenterY,
            intArrayOf(Color.argb(180, 255, 255, 255), Color.argb(0, 255, 255, 255)),
            floatArrayOf(0.0f, 1.0f),
            Shader.TileMode.CLAMP
        )
        style = Paint.Style.FILL
    }
    val highlightRect = RectF(
        cx - (bulbRadius * 0.75f),
        bulbCenterY - (bulbRadius * 0.85f),
        cx + (bulbRadius * 0.75f),
        bulbCenterY - (bulbRadius * 0.15f)
    )
    canvas.drawOval(highlightRect, highlightPaint)

    val discRadius = bulbRadius * 0.54f
    val innerDiscPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            cx, bulbCenterY, discRadius,
            intArrayOf(Color.rgb(30, 41, 59), Color.rgb(15, 23, 42)),
            floatArrayOf(0.0f, 1.0f),
            Shader.TileMode.CLAMP
        )
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, bulbCenterY, discRadius, innerDiscPaint)

    val innerDiscStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.argb(180, 255, 255, 255)
    }
    canvas.drawCircle(cx, bulbCenterY, discRadius, innerDiscStroke)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = if (isSelected) 22.0f else 18.0f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    val textY = bulbCenterY - ((textPaint.descent() + textPaint.ascent()) / 2.0f)
    canvas.drawText("!", cx, textY, textPaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createUserLocationIcon(context: Context, bearing: Float?): BitmapDrawable {
    val size = 56
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val cx = size / 2.0f
    val cy = size / 2.0f

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            cx, cy + 4.0f, 22.0f,
            intArrayOf(Color.argb(120, 0, 0, 0), Color.TRANSPARENT),
            floatArrayOf(0.3f, 1.0f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(cx, cy + 4.0f, 22.0f, shadowPaint)

    val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 52, 211, 153)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 22.0f, pulsePaint)

    val pulseStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 52, 211, 153)
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
    }
    canvas.drawCircle(cx, cy, 22.0f, pulseStroke)

    val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            cx - 3.0f, cy - 3.0f, 14.0f,
            intArrayOf(Color.rgb(110, 231, 183), Color.rgb(16, 185, 129), Color.rgb(4, 120, 87)),
            floatArrayOf(0.0f, 0.55f, 1.0f),
            Shader.TileMode.CLAMP
        )
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 14.0f, orbPaint)

    val orbStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3.0f
    }
    canvas.drawCircle(cx, cy, 14.0f, orbStroke)

    val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 4.0f, centerDotPaint)

    return BitmapDrawable(context.resources, bitmap)
}
