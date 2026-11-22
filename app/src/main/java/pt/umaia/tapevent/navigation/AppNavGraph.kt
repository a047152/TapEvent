package pt.umaia.tapevent.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import pt.umaia.tapevent.core.model.Roles
import pt.umaia.tapevent.pages.approvals.ApprovalsScreen
import pt.umaia.tapevent.pages.createevent.CreateEventScreen
import pt.umaia.tapevent.pages.discover.DiscoverScreen
import pt.umaia.tapevent.pages.eventdetails.EventDetailsScreen
import pt.umaia.tapevent.pages.ticket.TicketScreen
import pt.umaia.tapevent.pages.home.HomeAction
import pt.umaia.tapevent.pages.home.HomePlaceholderScreen
import pt.umaia.tapevent.pages.login.LoginScreen
import pt.umaia.tapevent.pages.myevents.MyEventsScreen

object Routes {
    const val LOGIN = "login"
    const val STUDENT_HOME = "student/discover"
    const val EVENT_DETAILS = "student/event/{eventId}"
    const val TICKET = "student/ticket/{eventId}"
    const val MY_EVENTS = "student/my-events"
    const val ORGANIZER_HOME = "organizer/dashboard"
    const val CREATE_EVENT = "organizer/create-event"
    const val ADMIN_HOME = "admin/approvals"

    fun eventDetails(eventId: String) = "student/event/$eventId"
    fun ticket(eventId: String) = "student/ticket/$eventId"

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
            DiscoverScreen(
                onEventClick = { event -> navController.navigate(Routes.eventDetails(event.id)) },
                onSignOut = signOut
            )
        }
        composable(Routes.MY_EVENTS) {
            MyEventsScreen(
                onOpenTicket = { id -> navController.navigate(Routes.ticket(id)) },
                onOpenEvent = { id -> navController.navigate(Routes.eventDetails(id)) },
                onGiveFeedback = { /* TODO (next page): open the Feedback screen */ },
                onBrowseEvents = {
                    navController.navigate(Routes.STUDENT_HOME) {
                        popUpTo(Routes.STUDENT_HOME) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Routes.EVENT_DETAILS,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId").orEmpty()
            EventDetailsScreen(
                eventId = eventId,
                onBack = { navController.popBackStack() },
                onViewTicket = { id -> navController.navigate(Routes.ticket(id)) }
            )
        }
        composable(
            route = Routes.TICKET,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId").orEmpty()
            TicketScreen(
                eventId = eventId,
                onBack = { navController.popBackStack() },
                // Back to the event page, which now shows "Garantir vaga grátis" again
                onCancelled = { navController.popBackStack() },
                onBrowseEvents = {
                    navController.navigate(Routes.STUDENT_HOME) {
                        popUpTo(Routes.STUDENT_HOME) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.ORGANIZER_HOME) {
            HomePlaceholderScreen(
                title = "Painel (organizador)",
                onSignOut = signOut,
                actions = listOf(
                    HomeAction("Criar evento") { navController.navigate(Routes.CREATE_EVENT) }
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
