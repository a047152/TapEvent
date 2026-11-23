package pt.umaia.tapevent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import pt.umaia.tapevent.navigation.AppNavGraph
import pt.umaia.tapevent.navigation.StudentBottomBar
import pt.umaia.tapevent.ui.theme.TapEventTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TapEventTheme {
                // Goes up on every sign-out. key() then rebuilds everything inside it:
                // a new NavController, new ViewModels, no saved tab state from the last user.
                var session by rememberSaveable { mutableIntStateOf(0) }

                key(session) {
                    val navController = rememberNavController()
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = { StudentBottomBar(navController) }
                    ) { innerPadding ->
                        AppNavGraph(
                            onSignOut = { session++ },
                            navController = navController,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
