package pt.umaia.tapevent.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

private data class BottomTab(val route: String, val label: String, val icon: ImageVector)

/** Student tabs. */
private val STUDENT_TABS = listOf(
    BottomTab(Routes.STUDENT_HOME, "Descobrir", Icons.Filled.Search),
    BottomTab(Routes.MY_EVENTS, "Meus", Icons.Filled.DateRange),
    BottomTab(Routes.IDEAS, "Propor", Icons.Filled.ThumbUp),
    BottomTab(Routes.FRIENDS, "Amigos", Icons.Filled.Person),
    BottomTab(Routes.ALERTS, "Avisos", Icons.Filled.Notifications)
)

/** Shown only on the student tab screens (not on Login, Event or Ticket). */
@Composable
fun StudentBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    if (STUDENT_TABS.none { it.route == currentRoute }) return

    NavigationBar {
        STUDENT_TABS.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    navController.navigate(tab.route) {
                        // One copy of each tab on the back stack; Back from a tab returns to Discover
                        popUpTo(Routes.STUDENT_HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) }
            )
        }
    }
}
