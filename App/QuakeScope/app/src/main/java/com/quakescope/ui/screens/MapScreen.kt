package com.quakescope.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.quakescope.R
import com.quakescope.domain.model.Earthquake
import com.quakescope.ui.theme.EarthquakeColors
import com.quakescope.ui.viewmodel.MapViewModel
import com.quakescope.ui.viewmodel.SharedViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MapScreen(
    mapViewModel: MapViewModel = hiltViewModel(),
    sharedViewModel: SharedViewModel
) {
    val earthquakePairs by mapViewModel.earthquakePairs.collectAsState()
    val selectedPair by sharedViewModel.selectedPair.collectAsState()
    val selectedEarthquake by sharedViewModel.selectedEarthquake.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 0f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { sharedViewModel.clearSelection() }
        ) {
            earthquakePairs.forEach { pair ->
                val isSelected = selectedPair == pair
                val real = pair.real
                val estimated = pair.estimated

                if (real != null && estimated != null) {
                    Polyline(
                        points = listOf(
                            LatLng(real.latitude, real.longitude),
                            LatLng(estimated.latitude, estimated.longitude)
                        ),
                        color = if (isSelected) EarthquakeColors.PairLink else EarthquakeColors.PairLink.copy(alpha = 0.7f),
                        width = if (isSelected) 4f else 2f,
                        pattern = listOf(Dash(20f), Gap(10f))
                    )
                }

                real?.let { realQuake ->
                    val isEarthquakeSelected = selectedEarthquake?.id == realQuake.id
                    val position = LatLng(realQuake.latitude, realQuake.longitude)
                    val markerState = rememberMarkerState(position = position)
                    val baseAlpha = when {
                        isEarthquakeSelected -> 0.65f
                        isSelected -> 0.45f
                        else -> 0.25f
                    }
                    val strokeWidth = when {
                        isEarthquakeSelected -> 6f
                        isSelected -> 4f
                        else -> 2f
                    }
                    Circle(
                        center = position,
                        radius = realQuake.radiusKm * 1000,
                        fillColor = EarthquakeColors.Real.copy(alpha = baseAlpha),
                        strokeColor = if (isEarthquakeSelected) Color.White else EarthquakeColors.Real,
                        strokeWidth = strokeWidth,
                        clickable = true,
                        onClick = { sharedViewModel.selectEarthquake(pair, realQuake) }
                    )
                    if (isEarthquakeSelected) {
                        Circle(
                            center = position,
                            radius = realQuake.radiusKm * 1000 * 1.25,
                            fillColor = EarthquakeColors.Real.copy(alpha = 0.12f),
                            strokeWidth = 0f
                        )
                        MarkerInfoWindowContent(
                            state = markerState,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                            alpha = 0f
                        ) { _ ->
                            EarthquakeInfoBubble(earthquake = realQuake, accentColor = EarthquakeColors.Real)
                        }
                        LaunchedEffect(selectedEarthquake?.id) {
                            markerState.showInfoWindow()
                        }
                    }
                }

                estimated?.let { estimatedQuake ->
                    val isEarthquakeSelected = selectedEarthquake?.id == estimatedQuake.id
                    val position = LatLng(estimatedQuake.latitude, estimatedQuake.longitude)
                    val markerState = rememberMarkerState(position = position)
                    val baseAlpha = when {
                        isEarthquakeSelected -> 0.65f
                        isSelected -> 0.45f
                        else -> 0.25f
                    }
                    val strokeWidth = when {
                        isEarthquakeSelected -> 6f
                        isSelected -> 4f
                        else -> 2f
                    }
                    Circle(
                        center = position,
                        radius = estimatedQuake.radiusKm * 1000,
                        fillColor = EarthquakeColors.Estimated.copy(alpha = baseAlpha),
                        strokeColor = if (isEarthquakeSelected) Color.White else EarthquakeColors.Estimated,
                        strokeWidth = strokeWidth,
                        clickable = true,
                        onClick = { sharedViewModel.selectEarthquake(pair, estimatedQuake) }
                    )
                    if (isEarthquakeSelected) {
                        Circle(
                            center = position,
                            radius = estimatedQuake.radiusKm * 1000 * 1.25,
                            fillColor = EarthquakeColors.Estimated.copy(alpha = 0.12f),
                            strokeWidth = 0f
                        )
                        MarkerInfoWindowContent(
                            state = markerState,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            alpha = 0f
                        ) { _ ->
                            EarthquakeInfoBubble(earthquake = estimatedQuake, accentColor = EarthquakeColors.Estimated)
                        }
                        LaunchedEffect(selectedEarthquake?.id) {
                            markerState.showInfoWindow()
                        }
                    }
                }
            }
        }

        MapLegend(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun EarthquakeInfoBubble(
    earthquake: Earthquake,
    accentColor: Color
) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }
    val formattedTime = formatter.format(earthquake.time)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier.size(10.dp),
                    shape = CircleShape,
                    color = accentColor,
                    content = {}
                )
                Text(
                    text = earthquake.earthquakeId,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.magnitude_value, earthquake.magnitude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(id = R.string.depth_value, earthquake.depth),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(id = R.string.location_value, earthquake.latitude, earthquake.longitude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.map_legend_title),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendEntry(color = EarthquakeColors.Real, label = stringResource(id = R.string.map_legend_real))
                LegendEntry(color = EarthquakeColors.Estimated, label = stringResource(id = R.string.map_legend_estimated))
            }
        }
    }
}

@Composable
private fun LegendEntry(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = CircleShape,
            color = color,
            content = {}
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
