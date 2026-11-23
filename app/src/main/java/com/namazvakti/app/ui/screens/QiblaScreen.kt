package com.namazvakti.app.ui.screens

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.namazvakti.app.domain.QiblaCalculator
import com.namazvakti.app.model.LocationInfo
import com.namazvakti.app.ui.theme.EmeraldLight
import com.namazvakti.app.ui.theme.EmeraldPrimary
import com.namazvakti.app.ui.theme.GoldAccent
import com.namazvakti.app.ui.theme.GoldSoft
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun QiblaScreen(location: LocationInfo) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val qiblaBearing = remember(location.latitude, location.longitude) {
        QiblaCalculator.bearingDegrees(location.latitude, location.longitude)
    }
    val distanceKm = remember(location.latitude, location.longitude) {
        QiblaCalculator.distanceKm(location.latitude, location.longitude)
    }
    val cardinal = remember(qiblaBearing) {
        QiblaCalculator.cardinalDirectionTr(qiblaBearing)
    }
    val magneticDeclination = remember(location.latitude, location.longitude) {
        runCatching {
            GeomagneticField(
                location.latitude.toFloat(),
                location.longitude.toFloat(),
                0f,
                System.currentTimeMillis()
            ).declination.toDouble()
        }.getOrDefault(0.0)
    }

    var trueHeading by remember { mutableDoubleStateOf(0.0) }
    var sensorAccuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) }
    var hasCompassSensor by remember { mutableStateOf(true) }

    DisposableEffect(context, magneticDeclination) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotationSensor == null && (accelSensor == null || magSensor == null)) {
            hasCompassSensor = false
            return@DisposableEffect onDispose {}
        }

        hasCompassSensor = true
        val rotMatrix = FloatArray(9)
        val orient = FloatArray(3)
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasGeo = false
        var smoothSin = 0.0
        var smoothCos = 1.0
        var initialized = false

        fun updateHeading(magneticAzimuthDeg: Double) {
            val trueDeg = ((magneticAzimuthDeg + magneticDeclination) % 360.0 + 360.0) % 360.0
            val rad = Math.toRadians(trueDeg)
            if (!initialized) {
                smoothSin = sin(rad)
                smoothCos = cos(rad)
                initialized = true
            } else {
                val alpha = 0.14
                smoothSin = smoothSin * (1.0 - alpha) + sin(rad) * alpha
                smoothCos = smoothCos * (1.0 - alpha) + cos(rad) * alpha
            }
            val filtered = (Math.toDegrees(atan2(smoothSin, smoothCos)) + 360.0) % 360.0
            trueHeading = filtered
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                        SensorManager.getOrientation(rotMatrix, orient)
                        val az = Math.toDegrees(orient[0].toDouble())
                        updateHeading(az)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasGravity = true
                        if (hasGeo && SensorManager.getRotationMatrix(rotMatrix, null, gravity, geomagnetic)) {
                            SensorManager.getOrientation(rotMatrix, orient)
                            updateHeading(Math.toDegrees(orient[0].toDouble()))
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        hasGeo = true
                        if (hasGravity && SensorManager.getRotationMatrix(rotMatrix, null, gravity, geomagnetic)) {
                            SensorManager.getOrientation(rotMatrix, orient)
                            updateHeading(Math.toDegrees(orient[0].toDouble()))
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                sensorAccuracy = accuracy
            }
        }

        if (rotationSensor != null) {
            sm.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sm.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            sm.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose { sm.unregisterListener(listener) }
    }

    val angleDiff = QiblaCalculator.signedAngleDifference(trueHeading, qiblaBearing)
    val isAligned = hasCompassSensor && abs(angleDiff) <= 3.0

    LaunchedEffect(isAligned) {
        if (isAligned) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Kıble Pusulası",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${location.displayTitle} • ${location.country}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Alignment Status Pill
        Surface(
            shape = RoundedCornerShape(50),
            color = if (isAligned) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isAligned) Icons.Rounded.CheckCircle else Icons.Rounded.Explore,
                    contentDescription = null,
                    tint = if (isAligned) GoldSoft else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                val hint = when {
                    !hasCompassSensor -> "Bu cihazda manyetik pusula sensörü bulunamadı"
                    isAligned -> "Kıble Yönündesiniz!"
                    angleDiff > 0 -> "Telefonu ${angleDiff.roundToInt()}° sağa çevirin"
                    else -> "Telefonu ${abs(angleDiff).roundToInt()}° sola çevirin"
                }
                Text(
                    text = hint,
                    color = if (isAligned) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Compass Canvas
        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            QiblaCompassCanvas(
                trueHeadingDeg = if (hasCompassSensor) trueHeading.toFloat() else 0f,
                qiblaBearingDeg = qiblaBearing.toFloat(),
                isAligned = isAligned
            )
        }

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompassStatCard(
                modifier = Modifier.weight(1f),
                title = "Kıble Açısı",
                value = "${qiblaBearing.roundToInt()}° $cardinal"
            )
            CompassStatCard(
                modifier = Modifier.weight(1f),
                title = "Kâbe'ye Mesafe",
                value = String.format(Locale("tr", "TR"), "%,.0f km", distanceKm)
            )
            CompassStatCard(
                modifier = Modifier.weight(1f),
                title = "Mevcut Yön",
                value = if (hasCompassSensor) "${trueHeading.roundToInt()}°" else "--"
            )
        }

        // Calibration Hint Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                val accuracyLabel = when (sensorAccuracy) {
                    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "Pusula hassasiyeti: Yüksek."
                    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Pusula hassasiyeti: Orta."
                    else -> "Pusula hassasiyeti düşük! Telefonu havada 8 şeklinde çevirerek kalibre edin."
                }
                Text(
                    text = "$accuracyLabel Telefonu düz tutun ve metal/mıknatıslı kılıflardan uzaklaştırın. Gerçek kuzey sapması (${String.format(Locale.US, "%+.1f°", magneticDeclination)}) otomatik düzeltilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompassStatCard(modifier: Modifier = Modifier, title: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QiblaCompassCanvas(
    trueHeadingDeg: Float,
    qiblaBearingDeg: Float,
    isAligned: Boolean
) {
    val ringColor = if (isAligned) EmeraldLight else GoldAccent
    val dialBg = MaterialTheme.colorScheme.surface
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 14.dp.toPx()

        // Fixed top reference triangle (where top of phone points)
        val topMarker = Path().apply {
            moveTo(center.x, 4.dp.toPx())
            lineTo(center.x - 8.dp.toPx(), 16.dp.toPx())
            lineTo(center.x + 8.dp.toPx(), 16.dp.toPx())
            close()
        }
        drawPath(topMarker, color = ringColor)

        // Outer compass circle
        drawCircle(color = dialBg, radius = radius, center = center)
        drawCircle(
            color = ringColor,
            radius = radius,
            center = center,
            style = Stroke(width = if (isAligned) 5.dp.toPx() else 3.dp.toPx())
        )
        drawCircle(
            color = primaryColor.copy(alpha = 0.14f),
            radius = radius * 0.72f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Rotating compass dial (-trueHeadingDeg so North stays pointing toward physical North)
        rotate(degrees = -trueHeadingDeg, pivot = center) {
            // Degree ticks (every 5°)
            for (deg in 0 until 360 step 5) {
                val rad = deg * PI / 180.0
                val isMajor = deg % 90 == 0
                val isMedium = deg % 30 == 0
                val innerR = when {
                    isMajor -> radius - 18.dp.toPx()
                    isMedium -> radius - 13.dp.toPx()
                    else -> radius - 8.dp.toPx()
                }
                val start = Offset(
                    center.x + (innerR * sin(rad)).toFloat(),
                    center.y - (innerR * cos(rad)).toFloat()
                )
                val end = Offset(
                    center.x + ((radius - 3.dp.toPx()) * sin(rad)).toFloat(),
                    center.y - ((radius - 3.dp.toPx()) * cos(rad)).toFloat()
                )
                drawLine(
                    color = if (deg == 0) Color(0xFFD94A3D) else tickColor.copy(alpha = if (isMajor) 0.9f else 0.45f),
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Cardinal letters K, D, G, B
            val paint = android.graphics.Paint().apply {
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 15.dp.toPx()
                isFakeBoldText = true
                isAntiAlias = true
            }
            val labelR = radius - 31.dp.toPx()
            val cardinals = listOf("K" to 0.0, "D" to 90.0, "G" to 180.0, "B" to 270.0)
            for ((label, deg) in cardinals) {
                val rad = deg * PI / 180.0
                paint.color = if (label == "K") android.graphics.Color.rgb(217, 74, 61)
                else android.graphics.Color.rgb(90, 110, 102)
                val x = center.x + (labelR * sin(rad)).toFloat()
                val y = center.y - (labelR * cos(rad)).toFloat() + 5.dp.toPx()
                drawContext.canvas.nativeCanvas.drawText(label, x, y, paint)
            }

            // Qibla needle & Kaaba icon rotated to qiblaBearingDeg relative to North
            rotate(degrees = qiblaBearingDeg, pivot = center) {
                // Qibla arrow shaft
                val needlePath = Path().apply {
                    moveTo(center.x, center.y - radius * 0.72f)
                    lineTo(center.x - 10.dp.toPx(), center.y)
                    lineTo(center.x, center.y + radius * 0.22f)
                    lineTo(center.x + 10.dp.toPx(), center.y)
                    close()
                }
                drawPath(
                    path = needlePath,
                    color = if (isAligned) EmeraldLight else GoldAccent
                )

                // Stylized Kaaba cube at tip of Qibla direction
                val kaabaSize = 22.dp.toPx()
                val kaabaTopLeft = Offset(
                    center.x - kaabaSize / 2f,
                    center.y - radius * 0.88f
                )
                drawRect(
                    color = Color(0xFF1A1A1A),
                    topLeft = kaabaTopLeft,
                    size = Size(kaabaSize, kaabaSize)
                )
                // Gold Kiswah band
                drawRect(
                    color = GoldSoft,
                    topLeft = Offset(kaabaTopLeft.x, kaabaTopLeft.y + kaabaSize * 0.28f),
                    size = Size(kaabaSize, kaabaSize * 0.18f)
                )
            }
        }

        // Center pivot jewel
        drawCircle(color = EmeraldPrimary, radius = 8.dp.toPx(), center = center)
        drawCircle(color = GoldSoft, radius = 3.5.dp.toPx(), center = center)
    }
}
