package com.quakescope.ui.screens

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.HomeRepairService
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import com.quakescope.R

@Composable
fun WikiScreen() {
    val sections = rememberWikiSections()
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 720

    var menuExpanded by rememberSaveable { mutableStateOf(!isCompact) }
    LaunchedEffect(isCompact) {
        if (!isCompact) {
            menuExpanded = true
        }
    }

    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    if (sections.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(id = R.string.wiki_header_subtitle))
            }
        }
        return
    }
    selectedIndex = selectedIndex.coerceIn(sections.indices)

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isCompact) {
            val listState = rememberLazyListState()
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    sections.forEachIndexed { index, section ->
                        NavigationRailItem(
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index },
                            icon = { Icon(imageVector = section.icon, contentDescription = null) },
                            label = {
                                Text(
                                    text = stringResource(section.title),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 24.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                sectionContent(
                    modifier = Modifier.weight(1f),
                    section = sections[selectedIndex],
                    listState = listState
                )
            }
        } else {
            val listState = rememberLazyListState()
            val topBarHeight = 56.dp
            val topBarOffset = topBarHeight + 8.dp
            var showTopBar by rememberSaveable { mutableStateOf(true) }

            LaunchedEffect(listState, menuExpanded) {
                var previousIndex = 0
                var previousOffset = 0
                snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                    .collect { (index, offset) ->
                        if (menuExpanded) {
                            showTopBar = true
                        } else {
                            when {
                                index == 0 && offset <= 8 -> showTopBar = true
                                index > previousIndex || (index == previousIndex && offset > previousOffset + 8) -> showTopBar = false
                                index < previousIndex || (index == previousIndex && offset < previousOffset - 8) -> showTopBar = true
                            }
                        }
                        previousIndex = index
                        previousOffset = offset
                    }
            }

            val contentTopPadding = if (showTopBar || menuExpanded) topBarOffset else 12.dp

            sectionContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = contentTopPadding),
                section = sections[selectedIndex],
                listState = listState
            )

            AnimatedVisibility(
                visible = showTopBar || menuExpanded,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(topBarHeight)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Menu,
                                contentDescription = stringResource(id = R.string.wiki_toggle_menu),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.wiki_header_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(40.dp))
                    }
                }
            }

            if (menuExpanded) {
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = topBarOffset)
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .clickable(
                            indication = null,
                            interactionSource = interactionSource
                        ) {
                            menuExpanded = false
                        }
                )
                CompactTopicMenu(
                    sections = sections,
                    selectedIndex = selectedIndex,
                    onSelect = { index ->
                        selectedIndex = index
                        menuExpanded = false
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = topBarOffset + 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun sectionContent(
    modifier: Modifier,
    section: WikiSection,
    listState: LazyListState
) {
    val expandedIds = rememberSaveable(
        section.id,
        saver = listSaver<SnapshotStateList<String>, String>(
            save = { stateList -> stateList.toList() },
            restore = { saved -> saved.toMutableStateList() }
        )
    ) {
        mutableStateListOf<String>().apply {
            section.subsections.firstOrNull()?.let { add(it.id) }
        }
    }
    val uriHandler = LocalUriHandler.current
    val resources = remember(section.id) {
        listOf(
            WikiResource(R.string.wiki_resource_ready_label, "https://www.ready.gov/earthquakes"),
            WikiResource(R.string.wiki_resource_usgs_label, "https://earthquake.usgs.gov/earthquakes/map/"),
            WikiResource(R.string.wiki_resource_redcross_label, "https://www.redcross.org/get-help/how-to-prepare-for-emergencies/types-of-emergencies/earthquake.html")
        )
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            WikiHeroCard()
        }
        item {
            Text(
                text = stringResource(section.title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(section.summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(section.subsections) { subsection ->
            val isExpanded = expandedIds.contains(subsection.id)
            WikiAccordion(
                subsection = subsection,
                expanded = isExpanded,
                onToggle = {
                    if (isExpanded) {
                        expandedIds.remove(subsection.id)
                    } else {
                        expandedIds.add(subsection.id)
                    }
                }
            )
        }
        item {
            Text(
                text = stringResource(R.string.wiki_footer_resources_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                resources.forEach { resource ->
                    AssistChip(
                        onClick = { uriHandler.openUri(resource.url) },
                        label = { Text(stringResource(resource.label)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun WikiHeroCard(
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.wiki_header_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = stringResource(R.string.wiki_header_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.wiki_call_to_action),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun CompactTopicMenu(
    sections: List<WikiSection>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 10.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sections.forEachIndexed { index, section ->
                val selected = selectedIndex == index
                val labelColor = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                val background = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .background(background, RoundedCornerShape(18.dp))
                        .clickable { onSelect(index) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = section.icon,
                        contentDescription = null,
                        tint = labelColor
                    )
                    Text(
                        text = stringResource(id = section.title),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = labelColor
                    )
                }
            }
        }
    }
}

@Composable
private fun WikiAccordion(
    subsection: WikiSubsection,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = subsection.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(subsection.title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = stringResource(subsection.description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val bulletPoints = stringArrayResource(subsection.bulletPoints)
                    bulletPoints.forEach { point ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class WikiSection(
    val id: String,
    @StringRes val title: Int,
    @StringRes val summary: Int,
    val icon: ImageVector,
    val subsections: List<WikiSubsection>
)

private data class WikiSubsection(
    val id: String,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @ArrayRes val bulletPoints: Int,
    val icon: ImageVector
)

private data class WikiResource(
    @StringRes val label: Int,
    val url: String
)

@Composable
private fun rememberWikiSections(): List<WikiSection> = remember {
    listOf(
        WikiSection(
            id = "basics",
            title = R.string.wiki_section_basics_title,
            summary = R.string.wiki_section_basics_summary,
            icon = Icons.Outlined.Info,
            subsections = listOf(
                WikiSubsection(
                    id = "basics_origin",
                    title = R.string.wiki_basics_sub_origin_title,
                    description = R.string.wiki_basics_sub_origin_body,
                    bulletPoints = R.array.wiki_basics_sub_origin_points,
                    icon = Icons.Outlined.Waves
                ),
                WikiSubsection(
                    id = "basics_scale",
                    title = R.string.wiki_basics_sub_scale_title,
                    description = R.string.wiki_basics_sub_scale_body,
                    bulletPoints = R.array.wiki_basics_sub_scale_points,
                    icon = Icons.Outlined.Info
                ),
                WikiSubsection(
                    id = "basics_signals",
                    title = R.string.wiki_basics_sub_signals_title,
                    description = R.string.wiki_basics_sub_signals_body,
                    bulletPoints = R.array.wiki_basics_sub_signals_points,
                    icon = Icons.Outlined.Explore
                )
            )
        ),
        WikiSection(
            id = "before",
            title = R.string.wiki_section_before_title,
            summary = R.string.wiki_section_before_summary,
            icon = Icons.Outlined.HomeRepairService,
            subsections = listOf(
                WikiSubsection(
                    id = "before_home",
                    title = R.string.wiki_before_sub_home_title,
                    description = R.string.wiki_before_sub_home_body,
                    bulletPoints = R.array.wiki_before_sub_home_points,
                    icon = Icons.Outlined.Inventory2
                ),
                WikiSubsection(
                    id = "before_plan",
                    title = R.string.wiki_before_sub_plan_title,
                    description = R.string.wiki_before_sub_plan_body,
                    bulletPoints = R.array.wiki_before_sub_plan_points,
                    icon = Icons.Outlined.FamilyRestroom
                ),
                WikiSubsection(
                    id = "before_drills",
                    title = R.string.wiki_before_sub_drills_title,
                    description = R.string.wiki_before_sub_drills_body,
                    bulletPoints = R.array.wiki_before_sub_drills_points,
                    icon = Icons.Outlined.AutoAwesome
                )
            )
        ),
        WikiSection(
            id = "during",
            title = R.string.wiki_section_during_title,
            summary = R.string.wiki_section_during_summary,
            icon = Icons.Outlined.HealthAndSafety,
            subsections = listOf(
                WikiSubsection(
                    id = "during_indoor",
                    title = R.string.wiki_during_sub_indoor_title,
                    description = R.string.wiki_during_sub_indoor_body,
                    bulletPoints = R.array.wiki_during_sub_indoor_points,
                    icon = Icons.Outlined.HomeRepairService
                ),
                WikiSubsection(
                    id = "during_outdoor",
                    title = R.string.wiki_during_sub_outdoor_title,
                    description = R.string.wiki_during_sub_outdoor_body,
                    bulletPoints = R.array.wiki_during_sub_outdoor_points,
                    icon = Icons.Outlined.Explore
                ),
                WikiSubsection(
                    id = "during_special",
                    title = R.string.wiki_during_sub_special_title,
                    description = R.string.wiki_during_sub_special_body,
                    bulletPoints = R.array.wiki_during_sub_special_points,
                    icon = Icons.Outlined.HealthAndSafety
                )
            )
        ),
        WikiSection(
            id = "after",
            title = R.string.wiki_section_after_title,
            summary = R.string.wiki_section_after_summary,
            icon = Icons.Outlined.Info,
            subsections = listOf(
                WikiSubsection(
                    id = "after_check",
                    title = R.string.wiki_after_sub_check_title,
                    description = R.string.wiki_after_sub_check_body,
                    bulletPoints = R.array.wiki_after_sub_check_points,
                    icon = Icons.Outlined.Inventory2
                ),
                WikiSubsection(
                    id = "after_help",
                    title = R.string.wiki_after_sub_help_title,
                    description = R.string.wiki_after_sub_help_body,
                    bulletPoints = R.array.wiki_after_sub_help_points,
                    icon = Icons.Outlined.HealthAndSafety
                ),
                WikiSubsection(
                    id = "after_stayinformed",
                    title = R.string.wiki_after_sub_stayinformed_title,
                    description = R.string.wiki_after_sub_stayinformed_body,
                    bulletPoints = R.array.wiki_after_sub_stayinformed_points,
                    icon = Icons.Outlined.Explore
                )
            )
        ),
        WikiSection(
            id = "kit",
            title = R.string.wiki_section_kit_title,
            summary = R.string.wiki_section_kit_summary,
            icon = Icons.Outlined.Inventory2,
            subsections = listOf(
                WikiSubsection(
                    id = "kit_basic",
                    title = R.string.wiki_kit_sub_basic_title,
                    description = R.string.wiki_kit_sub_basic_body,
                    bulletPoints = R.array.wiki_kit_sub_basic_points,
                    icon = Icons.Outlined.Inventory2
                ),
                WikiSubsection(
                    id = "kit_docs",
                    title = R.string.wiki_kit_sub_docs_title,
                    description = R.string.wiki_kit_sub_docs_body,
                    bulletPoints = R.array.wiki_kit_sub_docs_points,
                    icon = Icons.Outlined.Info
                ),
                WikiSubsection(
                    id = "kit_maintenance",
                    title = R.string.wiki_kit_sub_maintenance_title,
                    description = R.string.wiki_kit_sub_maintenance_body,
                    bulletPoints = R.array.wiki_kit_sub_maintenance_points,
                    icon = Icons.Outlined.AutoAwesome
                )
            )
        )
    )
}


