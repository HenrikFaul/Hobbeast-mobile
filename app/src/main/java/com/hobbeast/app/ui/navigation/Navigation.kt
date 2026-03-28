package com.hobbeast.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.navigation.compose.*
import com.hobbeast.app.ui.auth.*
import com.hobbeast.app.ui.checkin.CheckInScreen
import com.hobbeast.app.ui.createevent.*
import com.hobbeast.app.ui.discovery.DiscoveryScreen
import com.hobbeast.app.ui.eventdetail.EventDetailScreen
import com.hobbeast.app.ui.organizer.*
import com.hobbeast.app.ui.profile.ProfileScreen
import com.hobbeast.app.ui.retention.MyPlansScreen
import com.hobbeast.app.ui.tripplanning.TripPlanningScreen
import com.hobbeast.app.ui.settings.SettingsScreen
import com.hobbeast.app.ui.venue.VenueProfileScreen

// ─── Route definitions ────────────────────────────────────────────────────────

sealed class Screen(val route: String) {
    object Login          : Screen("login")
    object Register       : Screen("register")
    object Onboarding     : Screen("onboarding")

    // Bottom nav destinations
    object Discovery      : Screen("discovery")
    object MyPlans        : Screen("my_plans")
    object Profile        : Screen("profile")
    object OrganizerHub   : Screen("organizer")

    // Detail screens
    object EventDetail    : Screen("event/{eventId}") {
        fun createRoute(id: String) = "event/$id"
    }
    object CreateEvent    : Screen("create_event")
    object EditEvent      : Screen("edit_event/{eventId}") {
        fun createRoute(id: String) = "edit_event/$id"
    }
    object Venue          : Screen("venue/{venueId}") {
        fun createRoute(id: String) = "venue/$id"
    }

    // Organizer sub-screens
    object Attendees      : Screen("attendees/{eventId}") {
        fun createRoute(id: String) = "attendees/$id"
    }
    object CheckIn        : Screen("checkin/{eventId}") {
        fun createRoute(id: String) = "checkin/$id"
    }
    object Messaging      : Screen("messaging/{eventId}") {
        fun createRoute(id: String) = "messaging/$id"
    }
    object Analytics      : Screen("analytics/{eventId}") {
        fun createRoute(id: String) = "analytics/$id"
    }
    object Settings       : Screen("settings")

    object TripPlanning   : Screen("trip/{eventId}") {
        fun createRoute(id: String) = "trip/$id"
    }
}

// Bottom nav items
private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Discovery,    "Felfedezés", Icons.Outlined.Explore,       Icons.Filled.Explore),
    BottomNavItem(Screen.MyPlans,      "Terveim",    Icons.Outlined.EventNote,      Icons.Filled.EventNote),
    BottomNavItem(Screen.OrganizerHub, "Organizer",  Icons.Outlined.Dashboard,      Icons.Filled.Dashboard),
    BottomNavItem(Screen.Profile,      "Profil",     Icons.Outlined.AccountCircle,  Icons.Filled.AccountCircle),
)

// ─── Main scaffold with bottom nav ───────────────────────────────────────────

@Composable
fun HobbeastMainScaffold(startDestination: String = Screen.Login.route) {
    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    val showBottomNav = currentRoute in listOf(
        Screen.Discovery.route, Screen.MyPlans.route,
        Screen.OrganizerHub.route, Screen.Profile.route,
    )

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(Screen.Discovery.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.icon,
                                    contentDescription = item.label,
                                )
                            },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        HobbeastNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

// ─── Nav host ────────────────────────────────────────────────────────────────

@Composable
fun HobbeastNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { slideInHorizontally { it / 3 } + fadeIn() },
        exitTransition  = { slideOutHorizontally { -it / 3 } + fadeOut() },
        popEnterTransition  = { slideInHorizontally { -it / 3 } + fadeIn() },
        popExitTransition   = { slideOutHorizontally { it / 3 } + fadeOut() },
    ) {

        // ─── Auth ─────────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Discovery.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onRegister = { navController.navigate(Screen.Register.route) },
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Discovery.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }

        // ─── Bottom nav screens ───────────────────────────────────────────────
        composable(Screen.Discovery.route) {
            DiscoveryScreen(
                onEventClick = { navController.navigate(Screen.EventDetail.createRoute(it)) },
                onCreateEvent = { navController.navigate(Screen.CreateEvent.route) },
                onProfile = { navController.navigate(Screen.Profile.route) },
                onOrganizerMode = { navController.navigate(Screen.OrganizerHub.route) },
            )
        }
        composable(Screen.MyPlans.route) {
            MyPlansScreen(
                onBack = { navController.popBackStack() },
                onEventClick = { navController.navigate(Screen.EventDetail.createRoute(it)) },
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                },
            )
        }

        // ─── Event screens ────────────────────────────────────────────────────
        composable(Screen.EventDetail.route, listOf(navArgument("eventId") { type = NavType.StringType })) { back ->
            val eventId = back.arguments!!.getString("eventId")!!
            EventDetailScreen(
                eventId = eventId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Screen.EditEvent.createRoute(eventId)) },
                onTripPlanning = { navController.navigate(Screen.TripPlanning.createRoute(eventId)) },
                onCheckIn = { navController.navigate(Screen.CheckIn.createRoute(eventId)) },
            )
        }
        composable(Screen.CreateEvent.route) {
            CreateEventScreen(
                onBack = { navController.popBackStack() },
                onCreated = { id ->
                    navController.navigate(Screen.EventDetail.createRoute(id)) {
                        popUpTo(Screen.CreateEvent.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.EditEvent.route, listOf(navArgument("eventId") { type = NavType.StringType })) { back ->
            EditEventScreen(
                eventId = back.arguments!!.getString("eventId")!!,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        // ─── Venue ────────────────────────────────────────────────────────────
        composable(Screen.Venue.route, listOf(navArgument("venueId") { type = NavType.StringType })) { back ->
            VenueProfileScreen(
                venueId = back.arguments!!.getString("venueId")!!,
                onBack = { navController.popBackStack() },
                onEventClick = { navController.navigate(Screen.EventDetail.createRoute(it)) },
            )
        }

        // ─── Organizer ────────────────────────────────────────────────────────
        composable(Screen.OrganizerHub.route) {
            OrganizerDashboardScreen(
                onBack = { navController.popBackStack() },
                onEventClick = { navController.navigate(Screen.EventDetail.createRoute(it)) },
                onAttendees  = { navController.navigate(Screen.Attendees.createRoute(it)) },
                onCheckIn    = { navController.navigate(Screen.CheckIn.createRoute(it)) },
                onMessaging  = { navController.navigate(Screen.Messaging.createRoute(it)) },
                onAnalytics  = { navController.navigate(Screen.Analytics.createRoute(it)) },
            )
        }
        composable(Screen.Attendees.route,  listOf(navArgument("eventId") { type = NavType.StringType })) { back ->
            AttendeeManagementScreen(eventId = back.arguments!!.getString("eventId")!!, onBack = { navController.popBackStack() })
        }
        composable(Screen.CheckIn.route,    listOf(navArgument("eventId") { type = NavType.StringType })) { back ->
            CheckInScreen(eventId = back.arguments!!.getString("eventId")!!, onBack = { navController.popBackStack() })
        }
        composable(Screen.Messaging.route,  listOf(navArgument("eventId") { type = NavType.StringType })) { back ->
            MessagingScreen(eventId = back.arguments!!.getString("eventId")!!, onBack = { navController.popBackStack() })
        }
        composable(Screen.Analytics.route,  listOf(navArgument("eventId") { type = NavType.StringType })) { back ->
            AnalyticsScreen(eventId = back.arguments!!.getString("eventId")!!, onBack = { navController.popBackStack() })
        }

        // ─── Trip planning ────────────────────────────────────────────────────
        composable(Screen.TripPlanning.route, listOf(navArgument("eventId") { type = NavType.StringType })) { back ->
            TripPlanningScreen(eventId = back.arguments!!.getString("eventId")!!, onBack = { navController.popBackStack() })
        }
    }
}
