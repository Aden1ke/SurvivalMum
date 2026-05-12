package com.survivemum.app

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
import com.survivemum.app.security.SecurityModule
import com.survivemum.app.ui.theme.SurvivalMumTheme

class MainActivity : ComponentActivity() {

    /**
     * Container for the security layer — audit log, safety screener,
     * alert dispatcher, battery monitor, model router.
     *
     * Initialized once in onCreate and held for the activity's lifetime.
     * Eventually this should move to an Application subclass so it survives
     * configuration changes, but for the hackathon Activity-scoped is fine.
     */
    private lateinit var securityModule: SecurityModule

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wire up the security layer before anything else.
        // This creates the Room database, starts the connectivity listener,
        // and prepares the alert dispatcher to receive queued alerts.
        securityModule = SecurityModule(applicationContext)

        enableEdgeToEdge()
        setContent {
            SurvivalMumTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SurvivalMumTheme {
        Greeting("Android")
    }
}