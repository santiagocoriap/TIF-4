package com.quakescope.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.quakescope.R

sealed class Screen(val route: String, @StringRes val title: Int, val icon: ImageVector) {
    object Map : Screen("map", R.string.map, Icons.Default.Map)
    object List : Screen("list", R.string.list, Icons.Default.List)
    object Wiki : Screen("wiki", R.string.wiki, Icons.Default.MenuBook)
    object Settings : Screen("settings", R.string.settings, Icons.Default.Settings)
}

val items = listOf(
    Screen.Map,
    Screen.List,
    Screen.Wiki,
    Screen.Settings
)

@Composable
fun BottomNavBar(navController: NavController) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = stringResource(id = screen.title)) },
                label = { Text(stringResource(id = screen.title)) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
