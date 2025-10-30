package com.quakescope.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.quakescope.R
import com.quakescope.domain.model.EarthquakeType
import com.quakescope.domain.model.SortOption
import com.quakescope.domain.model.SortOptionRules
import com.quakescope.ui.viewmodel.ListViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    onDismiss: () -> Unit,
    onSaveFilters: () -> Unit,
    onManualRefresh: () -> Unit,
    listViewModel: ListViewModel,
) {
    val filterState by listViewModel.filterState.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            val setSortOption: (SortOption) -> Unit = listViewModel::setSortOption
            val setLimit: (Int) -> Unit = listViewModel::setLimit

            FilterSectionCard(
                title = stringResource(id = R.string.filter_section_scope_title),
                description = stringResource(id = R.string.filter_section_scope_description)
            ) {
                EarthquakeTypeRadioButtons(
                    currentType = filterState.earthquakeType,
                    onTypeSelected = listViewModel::setEarthquakeType
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            FilterSectionCard(
                title = stringResource(id = R.string.filter_section_ranges_title),
                description = if (filterState.earthquakeType == EarthquakeType.PAIRS) {
                    stringResource(id = R.string.filter_section_ranges_pairs_description)
                } else {
                    stringResource(id = R.string.filter_section_ranges_single_description)
                }
            ) {
                if (filterState.earthquakeType == EarthquakeType.PAIRS) {
                    val setRealMagnitudeRange: (ClosedFloatingPointRange<Float>) -> Unit = listViewModel::setRealMagnitudeRange
                    val setEstimatedMagnitudeRange: (ClosedFloatingPointRange<Float>) -> Unit = listViewModel::setEstimatedMagnitudeRange
                    val setRealDepthRange: (ClosedFloatingPointRange<Float>) -> Unit = listViewModel::setRealDepthRange
                    val setEstimatedDepthRange: (ClosedFloatingPointRange<Float>) -> Unit = listViewModel::setEstimatedDepthRange

                    RangeInputFields(
                        label = stringResource(id = R.string.real_magnitude),
                        range = filterState.realMagnitudeRange,
                        minAllowed = 4f,
                        maxAllowed = 10f,
                        onRangeChange = setRealMagnitudeRange
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    RangeInputFields(
                        label = stringResource(id = R.string.estimated_magnitude),
                        range = filterState.estimatedMagnitudeRange,
                        minAllowed = 4f,
                        maxAllowed = 10f,
                        onRangeChange = setEstimatedMagnitudeRange
                    )
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    RangeInputFields(
                        label = stringResource(id = R.string.real_depth_km),
                        range = filterState.realDepthRange,
                        minAllowed = 0f,
                        maxAllowed = 70f,
                        onRangeChange = setRealDepthRange
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    RangeInputFields(
                        label = stringResource(id = R.string.estimated_depth_km),
                        range = filterState.estimatedDepthRange,
                        minAllowed = 0f,
                        maxAllowed = 70f,
                        onRangeChange = setEstimatedDepthRange
                    )
                } else {
                    val setMagnitudeRange: (ClosedFloatingPointRange<Float>) -> Unit = listViewModel::setMagnitudeRange
                    val setDepthRange: (ClosedFloatingPointRange<Float>) -> Unit = listViewModel::setDepthRange

                    RangeInputFields(
                        label = stringResource(id = R.string.magnitude),
                        range = filterState.magnitudeRange,
                        minAllowed = 4f,
                        maxAllowed = 10f,
                        onRangeChange = setMagnitudeRange
                    )
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    RangeInputFields(
                        label = stringResource(id = R.string.depth_km),
                        range = filterState.depthRange,
                        minAllowed = 0f,
                        maxAllowed = 70f,
                        onRangeChange = setDepthRange
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            FilterSectionCard(
                title = stringResource(id = R.string.filter_section_sort_title),
                description = stringResource(id = R.string.filter_section_sort_description)
            ) {
                SortDropdown(
                    currentOption = filterState.sortOption,
                    availableOptions = SortOptionRules.availableFor(filterState.earthquakeType),
                    onOptionSelected = setSortOption
                )
                Spacer(modifier = Modifier.height(16.dp))
                LimitDropdown(
                    currentLimit = filterState.limit,
                    onLimitSelected = setLimit
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            FilterSectionCard(
                title = stringResource(id = R.string.filter_section_actions_title)
            ) {
                Text(
                    text = stringResource(id = R.string.filter_section_actions_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onManualRefresh,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(id = R.string.refresh_now))
                    }
                    Button(
                        onClick = {
                            onSaveFilters()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(id = R.string.save_filters))
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeInputFields(
    label: String,
    range: ClosedFloatingPointRange<Float>,
    minAllowed: Float,
    maxAllowed: Float,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    var minText by remember { mutableStateOf(formatRangeValue(range.start)) }
    var maxText by remember { mutableStateOf(formatRangeValue(range.endInclusive)) }

    LaunchedEffect(range.start, range.endInclusive) {
        minText = formatRangeValue(range.start)
        maxText = formatRangeValue(range.endInclusive)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        RangeSummaryChip(
            summary = stringResource(
                id = R.string.range_summary_label,
                formatRangeValue(range.start),
                formatRangeValue(range.endInclusive)
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = minText,
                onValueChange = { newValue ->
                    minText = sanitizeInput(newValue)
                    updateRangeIfValid(minText, maxText, minAllowed, maxAllowed, onRangeChange)
                },
                label = { Text(stringResource(id = R.string.min_value_label)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                value = maxText,
                onValueChange = { newValue ->
                    maxText = sanitizeInput(newValue)
                    updateRangeIfValid(minText, maxText, minAllowed, maxAllowed, onRangeChange)
                },
                label = { Text(stringResource(id = R.string.max_value_label)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
    }
}

private fun sanitizeInput(value: String): String = value.replace(',', '.')

@Composable
private fun RangeSummaryChip(summary: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Text(
            text = summary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private fun updateRangeIfValid(
    minText: String,
    maxText: String,
    minAllowed: Float,
    maxAllowed: Float,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    val minValue = minText.toFloatOrNull() ?: return
    val maxValue = maxText.toFloatOrNull() ?: return
    val clampedMin = minValue.coerceIn(minAllowed, maxAllowed)
    val clampedMax = maxValue.coerceIn(minAllowed, maxAllowed)
    if (clampedMin <= clampedMax) {
        onRangeChange(clampedMin..clampedMax)
    }
}

private fun formatRangeValue(value: Float): String {
    return if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortDropdown(
    currentOption: SortOption,
    availableOptions: List<SortOption>,
    onOptionSelected: (SortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = stringResource(id = R.string.sort_by)
    val displayOption = remember(currentOption, availableOptions) {
        availableOptions.firstOrNull { it == currentOption } ?: availableOptions.firstOrNull() ?: SortOption.TIME_DESC
    }
    val currentLabel = stringResource(id = sortOptionLabelRes(displayOption))
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()

    if (isPressed) {
        expanded = true
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { textFieldSize = it.size }
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = currentLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                },
                singleLine = true,
                interactionSource = interactionSource
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(with(density) { textFieldSize.width.toDp() })
            ) {
                availableOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(id = sortOptionLabelRes(option))) },
                        onClick = {
                            expanded = false
                            onOptionSelected(option)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LimitDropdown(
    currentLimit: Int,
    onLimitSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = stringResource(id = R.string.limit_label)
    val currentLabel = stringResource(id = R.string.limit_option, currentLimit)
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()

    if (isPressed) {
        expanded = true
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { textFieldSize = it.size }
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = currentLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                },
                singleLine = true,
                interactionSource = interactionSource
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(with(density) { textFieldSize.width.toDp() })
            ) {
                LIMIT_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.limit_option, option)) },
                        onClick = {
                            expanded = false
                            onLimitSelected(option)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun sortOptionLabelRes(option: SortOption): Int = when (option) {
    SortOption.TIME_DESC -> R.string.sort_time_desc
    SortOption.TIME_ASC -> R.string.sort_time_asc
    SortOption.MAGNITUDE_DESC -> R.string.sort_magnitude_desc
    SortOption.MAGNITUDE_ASC -> R.string.sort_magnitude_asc
    SortOption.DEPTH_DESC -> R.string.sort_depth_desc
    SortOption.DEPTH_ASC -> R.string.sort_depth_asc

    SortOption.REAL_TIME_DESC -> R.string.sort_real_time_desc
    SortOption.REAL_TIME_ASC -> R.string.sort_real_time_asc
    SortOption.REAL_MAGNITUDE_DESC -> R.string.sort_real_magnitude_desc
    SortOption.REAL_MAGNITUDE_ASC -> R.string.sort_real_magnitude_asc
    SortOption.REAL_DEPTH_DESC -> R.string.sort_real_depth_desc
    SortOption.REAL_DEPTH_ASC -> R.string.sort_real_depth_asc

    SortOption.ESTIMATED_TIME_DESC -> R.string.sort_estimated_time_desc
    SortOption.ESTIMATED_TIME_ASC -> R.string.sort_estimated_time_asc
    SortOption.ESTIMATED_MAGNITUDE_DESC -> R.string.sort_estimated_magnitude_desc
    SortOption.ESTIMATED_MAGNITUDE_ASC -> R.string.sort_estimated_magnitude_asc
    SortOption.ESTIMATED_DEPTH_DESC -> R.string.sort_estimated_depth_desc
    SortOption.ESTIMATED_DEPTH_ASC -> R.string.sort_estimated_depth_asc
}

private val LIMIT_OPTIONS = listOf(20, 50, 100, 200)

@Composable
private fun EarthquakeTypeRadioButtons(
    currentType: EarthquakeType,
    onTypeSelected: (EarthquakeType) -> Unit
) {
    val types = EarthquakeType.values()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        types.forEach { type ->
            val selected = currentType == type
            val label = when (type) {
                EarthquakeType.PAIRS -> stringResource(id = R.string.pairs)
                EarthquakeType.REAL -> stringResource(id = R.string.real)
                EarthquakeType.ESTIMATED -> stringResource(id = R.string.estimated)
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable { onTypeSelected(type) },
                shape = RoundedCornerShape(24.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                border = if (selected) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                },
                tonalElevation = if (selected) 4.dp else 0.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterSectionCard(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
