package com.quakescope.ui

import android.os.Bundle
import android.app.Activity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.messaging.FirebaseMessaging
import com.quakescope.R
import com.quakescope.domain.usecase.UpdateDeviceTokenUseCase
import com.quakescope.ui.navigation.BottomNavBar
import com.quakescope.ui.navigation.Navigation
import com.quakescope.ui.navigation.Screen
import com.quakescope.ui.screens.FilterSheet
import com.quakescope.ui.theme.QuakeScopeTheme
import com.quakescope.ui.viewmodel.ListViewModel
import com.quakescope.ui.viewmodel.MapViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var updateDeviceTokenUseCase: UpdateDeviceTokenUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuakeScopeTheme {
                MainScreen(
                    onTokenRegistered = { token ->
                        lifecycleScope.launch {
                            try {
                                updateDeviceTokenUseCase(token)
                            } catch (ex: Exception) {
                                Log.w("MainActivity", "Failed to sync FCM token", ex)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    onTokenRegistered: (String) -> Unit = {}
) {
    val navController = rememberNavController()
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDisclaimer by remember { mutableStateOf(true) }
    val listViewModel: ListViewModel = hiltViewModel()
    val mapViewModel: MapViewModel = hiltViewModel()
    val activity = LocalContext.current as? Activity

    LaunchedEffect(Unit) {
        mapViewModel.setFilterState(listViewModel.filterState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity?.let {
                val permissionGranted = ActivityCompat.checkSelfPermission(
                    it,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!permissionGranted) {
                    ActivityCompat.requestPermissions(
                        it,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_NOTIFICATION_PERMISSION
                    )
                }
            }
        }
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("FCM", "Token: $token")
                    if (!token.isNullOrBlank()) {
                        onTokenRegistered(token)
                    }
                } else {
                    Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                }
            }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = { BottomNavBar(navController) },
        floatingActionButton = {
            if (currentRoute == Screen.Map.route || currentRoute == Screen.List.route) {
                FloatingActionButton(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = stringResource(id = R.string.filter))
                }
            }
        }
    ) { innerPadding ->
        Navigation(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            listViewModel = listViewModel,
            mapViewModel = mapViewModel
        )

        if (showFilterSheet) {
            FilterSheet(
                onDismiss = { showFilterSheet = false },
                onSaveFilters = {
                    listViewModel.refresh()
                    mapViewModel.refresh()
                    showFilterSheet = false
                },
                onManualRefresh = {
                    listViewModel.refresh()
                    mapViewModel.refresh()
                },
                listViewModel = listViewModel
            )
        }
    }

    if (showDisclaimer) {
        DisclaimerDialog(
            onAccept = { showDisclaimer = false },
            onCloseApp = { activity?.finishAffinity() }
        )
    }
}
@Composable
private fun DisclaimerDialog(
    onAccept: () -> Unit,
    onCloseApp: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(
                text = stringResource(id = R.string.disclaimer_title),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = stringResource(id = R.string.disclaimer_body_primary))
                Text(
                    text = stringResource(id = R.string.disclaimer_body_secondary),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text(text = stringResource(id = R.string.disclaimer_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onCloseApp) {
                Text(text = stringResource(id = R.string.disclaimer_close))
            }
        }
    )
}

private const val REQUEST_NOTIFICATION_PERMISSION = 1001
