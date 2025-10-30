package com.quakescope.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.South
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.quakescope.R
import com.quakescope.domain.model.Earthquake
import com.quakescope.domain.model.EarthquakePair
import com.quakescope.ui.theme.EarthquakeColors
import com.quakescope.ui.viewmodel.ListViewModel
import com.quakescope.ui.viewmodel.SharedViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ListScreen(
    listViewModel: ListViewModel = hiltViewModel(),
    sharedViewModel: SharedViewModel
) {
    val lazyPagingItems = listViewModel.earthquakePairs.collectAsLazyPagingItems()
    val selectedPair by sharedViewModel.selectedPair.collectAsState()

    LazyColumn {
        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey { pair -> pair.real?.id ?: pair.estimated?.id ?: "" },
            contentType = lazyPagingItems.itemContentType { "earthquakePair" }
        ) { index ->
            val pair = lazyPagingItems[index]
            pair?.let {
                EarthquakePairItem(
                    pair = it,
                    isSelected = it == selectedPair,
                    onItemClick = { sharedViewModel.selectPair(it) }
                )
            }
        }
    }
}

@Composable
fun EarthquakePairItem(
    pair: EarthquakePair,
    isSelected: Boolean,
    onItemClick: () -> Unit
) {
    val pairId = pair.real?.earthquakeId ?: pair.estimated?.earthquakeId ?: "N/A"
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }
    val hasReal = pair.real != null
    val hasEstimated = pair.estimated != null
    val containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)

    Card(
        onClick = onItemClick,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 12.dp else 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.id),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = pairId,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (hasReal && hasEstimated) {
                        StatusChip(
                            text = stringResource(id = R.string.paired_badge),
                            icon = Icons.Filled.Link,
                            color = EarthquakeColors.PairLink
                        )
                    }
                    if (isSelected) {
                        StatusChip(
                            text = stringResource(id = R.string.selected_badge),
                            icon = Icons.Filled.RadioButtonChecked,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            pair.real?.let {
                EarthquakeSegment(
                    label = stringResource(id = R.string.real_earthquake),
                    accentColor = EarthquakeColors.Real,
                    earthquake = it
                )
            }

            if (hasReal && hasEstimated) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))
            }

            pair.estimated?.let {
                EarthquakeSegment(
                    label = stringResource(id = R.string.estimated_earthquake),
                    accentColor = EarthquakeColors.Estimated,
                    earthquake = it
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EarthquakeSegment(
    label: String,
    accentColor: Color,
    earthquake: Earthquake
) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy 'at' HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }
    val formattedTime by remember(earthquake.time) {
        derivedStateOf { formatter.format(earthquake.time) }
    }

    val magnitudeText = stringResource(id = R.string.magnitude_chip, earthquake.magnitude)
    val depthText = stringResource(id = R.string.depth_chip, earthquake.depth)
    val locationText = stringResource(id = R.string.location_chip, earthquake.latitude, earthquake.longitude)
    val timeText = stringResource(id = R.string.time_chip, formattedTime)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier
                    .size(12.dp),
                shape = CircleShape,
                color = accentColor
            ) {}
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chipBackground = accentColor.copy(alpha = 0.16f)
            MetricChip(
                icon = Icons.Filled.Bolt,
                text = magnitudeText,
                iconTint = accentColor,
                backgroundColor = chipBackground,
                contentColor = accentColor
            )
            MetricChip(
                icon = Icons.Filled.South,
                text = depthText,
                iconTint = accentColor,
                backgroundColor = chipBackground,
                contentColor = accentColor
            )
            MetricChip(
                icon = Icons.Filled.LocationOn,
                text = locationText,
                iconTint = accentColor,
                backgroundColor = chipBackground,
                contentColor = accentColor
            )
            MetricChip(
                icon = Icons.Filled.Schedule,
                text = timeText,
                iconTint = accentColor,
                backgroundColor = chipBackground,
                contentColor = accentColor
            )
        }
    }
}

@Composable
private fun MetricChip(
    icon: ImageVector,
    text: String,
    iconTint: Color,
    backgroundColor: Color,
    contentColor: Color
) {
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.24f),
        contentColor = color,
        shape = RoundedCornerShape(50),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}
