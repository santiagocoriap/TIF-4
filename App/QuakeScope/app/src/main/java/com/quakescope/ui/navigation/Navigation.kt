package com.quakescope.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.quakescope.ui.screens.ListScreen
import com.quakescope.ui.screens.MapScreen
import com.quakescope.ui.screens.SettingsScreen
import com.quakescope.ui.screens.WikiScreen
import com.quakescope.ui.viewmodel.ListViewModel
import com.quakescope.ui.viewmodel.MapViewModel
import com.quakescope.ui.viewmodel.SharedViewModel

@Composable
fun Navigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    listViewModel: ListViewModel,
    mapViewModel: MapViewModel
) {
    val sharedViewModel: SharedViewModel = hiltViewModel()
    NavHost(navController, startDestination = Screen.Map.route, modifier = modifier) {
        composable(Screen.Map.route) { MapScreen(mapViewModel = mapViewModel, sharedViewModel = sharedViewModel) }
        composable(Screen.List.route) { ListScreen(listViewModel = listViewModel, sharedViewModel = sharedViewModel) }
        composable(Screen.Wiki.route) { WikiScreen() }
        composable(Screen.Settings.route) { SettingsScreen() }
    }
}
