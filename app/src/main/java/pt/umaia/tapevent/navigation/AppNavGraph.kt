package pt.umaia.tapevent.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import pt.umaia.tapevent.core.model.Roles
import pt.umaia.tapevent.pages.approvals.ApprovalsScreen
import pt.umaia.tapevent.pages.createevent.CreateEventScreen
import pt.umaia.tapevent.pages.home.HomeAction
import pt.umaia.tapevent.pages.home.HomePlaceholderScreen
import pt.umaia.tapevent.pages.login.LoginScreen

object Routes {
    const val LOGIN = "login"
    const val STUDENT_HOME = "student/discover"
    const val ORGANIZER_HOME = "organizer/dashboard"
    const val CREATE_EVENT = "organizer/create-event"
    const val ADMIN_HOME = "admin/approvals"

    fun homeFor(role: String): String = when (role) {
        Roles.ORGANIZER -> ORGANIZER_HOME
        Roles.ADMIN -> ADMIN_HOME
        else -> STUDENT_HOME
    }
}

@Composable
fun AppNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val signOut: () -> Unit = {
        Firebase.auth.signOut()
        navController.navigate(Routes.LOGIN) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN,
        modifier = modifier
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onSignedIn = { role ->
                    navController.navigate(Routes.homeFor(role)) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.STUDENT_HOME) {
            HomePlaceholderScreen(title = "Discover (student)", onSignOut = signOut)
        }
        composable(Routes.ORGANIZER_HOME) {
            HomePlaceholderScreen(
                title = "Dashboard (organizer)",
                onSignOut = signOut,
                actions = listOf(
                    HomeAction("Create event") { navController.navigate(Routes.CREATE_EVENT) }
                )
            )
        }
        composable(Routes.CREATE_EVENT) {
            CreateEventScreen(
                onBack = { navController.popBackStack() },
                onSubmitted = { navController.popBackStack() }
            )
        }
        composable(Routes.ADMIN_HOME) {
            ApprovalsScreen(onSignOut = signOut)
        }
    }
}
