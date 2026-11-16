package pt.umaia.tapevent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import pt.umaia.tapevent.ui.theme.TapEventTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            Firebase.auth.useEmulator("10.0.2.2", 9099)
            Firebase.firestore.useEmulator("10.0.2.2", 8080)
        }

        enableEdgeToEdge()
        setContent {
            TapEventTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FirebaseTest(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun FirebaseTest(modifier: Modifier = Modifier) {
    var userStatus by remember { mutableStateOf("Signing in...") }
    var eventStatus by remember { mutableStateOf("Loading event...") }

    LaunchedEffect(Unit) {
        Firebase.auth.signInWithEmailAndPassword("aluno@teste.umaia.pt", "teste123")
            .addOnSuccessListener { result ->
                userStatus = "Signed in: ${result.user?.email}"

                Firebase.firestore.collection("events").limit(1).get()
                    .addOnSuccessListener { docs ->
                        val title = docs.documents.firstOrNull()?.getString("title")
                        eventStatus = "Event: ${title ?: "none"}"
                    }
                    .addOnFailureListener { eventStatus = "Firestore error: ${it.message}" }
            }
            .addOnFailureListener { userStatus = "Login error: ${it.message}" }
    }

    Column(modifier) {
        Text(userStatus)
        Text(eventStatus)
    }
}

