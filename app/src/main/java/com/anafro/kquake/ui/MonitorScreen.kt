package com.anafro.kquake.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anafro.kquake.location.locationUpdates
import com.anafro.kquake.sensor.MotionReading
import com.anafro.kquake.sensor.Vector3
import com.anafro.kquake.sensor.accelerationSamples
import com.anafro.kquake.sensor.hasAccelerometer
import com.anafro.kquake.sensor.toMotionReadings
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.sample
import java.time.Instant
import java.util.Locale

/** The sensors deliver ~100 updates/s; the screen only needs a few. */
private const val UI_REFRESH_MS = 100L

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@Composable
fun MonitorScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AccelerometerCard()
        LocationCard()
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun AccelerometerCard() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }

    SectionCard(title = "Accelerometer") {
        if (!sensorManager.hasAccelerometer()) {
            Text("This device has no accelerometer.")
            return@SectionCard
        }

        val readings = remember(sensorManager) {
            sensorManager.accelerationSamples().toMotionReadings().sample(UI_REFRESH_MS)
        }
        val reading by readings.collectAsStateWithLifecycle(initialValue = null)
        reading?.let { MotionDetails(it) } ?: Text("Waiting for samples…")
    }
}

@Composable
private fun MotionDetails(reading: MotionReading) {
    LabeledValue("Sample rate", "%.1f Hz".fmt(reading.sampleRateHz))
    VectorRow("Raw (m/s²)", reading.raw)
    VectorRow("Dynamic (m/s²)", reading.dynamic)
    LabeledValue("|Dynamic|", "%.4f m/s²  (%.5f g)".fmt(reading.dynamic.magnitude, reading.dynamic.magnitude / SensorManager.GRAVITY_EARTH))
}

@Composable
private fun LocationCard() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants -> hasPermission = grants.values.any { it } }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(LOCATION_PERMISSIONS)
    }

    SectionCard(title = "Location") {
        if (!hasPermission) {
            Text("Location is needed to place shaking reports on the map.")
            Button(onClick = { permissionLauncher.launch(LOCATION_PERMISSIONS) }) {
                Text("Grant location access")
            }
            return@SectionCard
        }

        val locationManager = remember { context.getSystemService(LocationManager::class.java) }
        val locations = remember(locationManager) {
            @SuppressLint("MissingPermission") // Checked by hasPermission above.
            val updates = locationManager.locationUpdates()
            updates
        }
        val location by locations.collectAsStateWithLifecycle(initialValue = null)
        location?.let { LocationDetails(it) } ?: Text("Waiting for a fix… (is location turned on?)")
    }
}

@Composable
private fun LocationDetails(location: Location) {
    LabeledValue("Latitude", "%.6f°".fmt(location.latitude))
    LabeledValue("Longitude", "%.6f°".fmt(location.longitude))
    LabeledValue("Accuracy", if (location.hasAccuracy()) "±%.0f m".fmt(location.accuracy) else "—")
    LabeledValue("Altitude", if (location.hasAltitude()) "%.0f m".fmt(location.altitude) else "—")
    LabeledValue("Provider", location.provider ?: "—")
    LabeledValue("Fix time (UTC)", Instant.ofEpochMilli(location.time).toString())
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun VectorRow(label: String, v: Vector3) {
    LabeledValue(label, "x %+8.4f  y %+8.4f  z %+8.4f".fmt(v.x, v.y, v.z))
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
    }
}

private fun String.fmt(vararg args: Any): String = String.format(Locale.ROOT, this, *args)

private fun Context.hasLocationPermission(): Boolean = LOCATION_PERMISSIONS.any {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}
