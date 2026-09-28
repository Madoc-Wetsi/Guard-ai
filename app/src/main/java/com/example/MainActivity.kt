package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.CounselingSupportScreen
import com.example.ui.screens.EmergencySosScreen
import com.example.ui.screens.IncidentDetailScreen
import com.example.ui.screens.ReportIncidentScreen
import com.example.ui.screens.StudentHomeScreen
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CampusGuardTheme
import com.example.ui.theme.SafetyBluePrimary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.CampusSafetyViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CampusSafetyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CampusGuardTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: CampusSafetyViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val selectedIncidentId by viewModel.selectedIncidentId.collectAsStateWithLifecycle()
    val activeAlerts by viewModel.activeAlerts.collectAsStateWithLifecycle()

    val showBottomBar = currentScreen != AppScreen.EMERGENCY_SOS

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("main_bottom_nav"),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                ) {
                    // Home
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.HOME,
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        icon = {
                            Icon(imageVector = Icons.Default.Home, contentDescription = "Home")
                        },
                        label = { Text("Home", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SafetyBluePrimary,
                            selectedTextColor = SafetyBluePrimary,
                            indicatorColor = SafetyBluePrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_tab_home")
                    )

                    // Report Incident
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.REPORT_INCIDENT,
                        onClick = { viewModel.navigateTo(AppScreen.REPORT_INCIDENT) },
                        icon = {
                            Icon(imageVector = Icons.Default.ReportProblem, contentDescription = "Report")
                        },
                        label = { Text("Report", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SafetyBluePrimary,
                            selectedTextColor = SafetyBluePrimary,
                            indicatorColor = SafetyBluePrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_tab_report")
                    )

                    // Counseling & Support
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.COUNSELING_SUPPORT,
                        onClick = { viewModel.navigateTo(AppScreen.COUNSELING_SUPPORT) },
                        icon = {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = "Support")
                        },
                        label = { Text("Support", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SafetyBluePrimary,
                            selectedTextColor = SafetyBluePrimary,
                            indicatorColor = SafetyBluePrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_tab_counseling")
                    )

                    // Dispatch Command Hub
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.ADMIN_DASHBOARD,
                        onClick = { viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD) },
                        icon = {
                            if (activeAlerts.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = AlertRed) {
                                            Text("${activeAlerts.size}", color = Color.White)
                                        }
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = "Dispatch")
                                }
                            } else {
                                Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = "Dispatch")
                            }
                        },
                        label = { Text("Dispatch", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SafetyBluePrimary,
                            selectedTextColor = SafetyBluePrimary,
                            indicatorColor = SafetyBluePrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_tab_dispatch")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            AppScreen.HOME -> {
                StudentHomeScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.REPORT_INCIDENT -> {
                ReportIncidentScreen(
                    viewModel = viewModel,
                    onNavigateBack = { viewModel.navigateBack() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.INCIDENT_DETAIL -> {
                selectedIncidentId?.let { id ->
                    IncidentDetailScreen(
                        incidentId = id,
                        viewModel = viewModel,
                        onNavigateBack = { viewModel.navigateBack() },
                        modifier = Modifier.padding(innerPadding)
                    )
                } ?: run {
                    StudentHomeScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
            AppScreen.EMERGENCY_SOS -> {
                EmergencySosScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.ADMIN_DASHBOARD -> {
                AdminDashboardScreen(
                    viewModel = viewModel,
                    onNavigateBack = { viewModel.navigateBack() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.COUNSELING_SUPPORT -> {
                CounselingSupportScreen(
                    viewModel = viewModel,
                    onNavigateBack = { viewModel.navigateBack() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
