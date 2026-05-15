package com.survivemum.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.survivemum.app.data.SurviveMumDatabase
import com.survivemum.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Applies the selected locale to the app immediately
// Called after saving — restarts the activity so strings reload in new language
// ─────────────────────────────────────────────────────────────────────────────
fun applyLanguage(context: Context, languageCode: String) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    @Suppress("DEPRECATION")
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}

@Composable
fun SettingsScreen(navController: NavController) {

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedLanguage     by remember { mutableStateOf("en") }
    var selectedSensitivity  by remember { mutableStateOf("STANDARD") }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var userId               by remember { mutableStateOf("") }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var isSaved              by remember { mutableStateOf(false) }

    val languages = mapOf(
        "en"  to "English",
        "yo"  to "Yoruba",
        "ha"  to "Hausa",
        "ig"  to "Igbo",
        "pcm" to "Nigerian Pidgin"
    )

    // Load saved preferences on entry
    LaunchedEffect(Unit) {
        val db   = SurviveMumDatabase.getDatabase(context)
        val user = db.userDao().getCurrentUser()
        if (user != null) {
            userId = user.userId
            val prefs = db.preferenceDao().getPreferences(user.userId)
            if (prefs != null) {
                selectedLanguage    = prefs.language
                selectedSensitivity = prefs.monitoringSensitivity
                notificationsEnabled = prefs.notificationsEnabled == 1
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // Top Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurviveMumDark)
                .statusBarsPadding()
                .padding(20.dp)
        ) {
            Column {
                TextButton(
                    onClick = { navController.popBackStack() },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "← Back",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Language ──────────────────────────────────────────────────────
            ProfileCard(title = "Language") {
                Spacer(modifier = Modifier.height(4.dp))
                Box {
                    OutlinedButton(
                        onClick = { showLanguageDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = languages[selectedLanguage] ?: "English",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text("▼")
                    }
                    DropdownMenu(
                        expanded = showLanguageDropdown,
                        onDismissRequest = { showLanguageDropdown = false }
                    ) {
                        languages.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedLanguage = code
                                    showLanguageDropdown = false
                                    isSaved = false
                                }
                            )
                        }
                    }
                }

                // Show hint that save will restart the app
                if (!isSaved) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Saving will restart the app to apply the language.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Monitoring Sensitivity ────────────────────────────────────────
            ProfileCard(title = "Monitoring Sensitivity") {
                Text(
                    text = "Controls how quickly SurviveMum fires alerts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                listOf(
                    "LOW"      to "Fewer alerts — less sensitive",
                    "STANDARD" to "Recommended for most TBAs",
                    "HIGH"     to "More alerts — higher caution"
                ).forEach { (value, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedSensitivity == value,
                            onClick  = {
                                selectedSensitivity = value
                                isSaved = false
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = SurviveMumRed
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Notifications ─────────────────────────────────────────────────
            ProfileCard(title = "Notifications") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alert notifications",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = {
                            notificationsEnabled = it
                            isSaved = false
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SurviveMumRed
                        )
                    )
                }
            }

            // ── App Info ──────────────────────────────────────────────────────
            ProfileCard(title = "About SurviveMum") {
                ProfileRow("Version",    "1.0.0")
                ProfileRow("Model",      "Gemma 4 E4B")
                ProfileRow("Mode",       "100% Offline")
                ProfileRow("Hackathon",  "Gemma 4 Good — Google DeepMind")
            }

            // ── Save Button ───────────────────────────────────────────────────
            Button(
                onClick = {
                    scope.launch {
                        if (userId.isNotBlank()) {
                            val db = SurviveMumDatabase.getDatabase(context)

                            // Save to database
                            db.preferenceDao().updateLanguage(
                                userId,
                                selectedLanguage,
                                System.currentTimeMillis().toString()
                            )
                            db.preferenceDao().updateSensitivity(
                                userId,
                                selectedSensitivity,
                                System.currentTimeMillis().toString()
                            )

                            isSaved = true

                            // Apply language immediately and restart activity
                            // so all string resources reload in the new language
                            applyLanguage(context, selectedLanguage)
                            (context as? Activity)?.recreate()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSaved) NewbornPrimary else SurviveMumRed
                )
            ) {
                Text(
                    text = if (isSaved) "✓ Saved" else "Save Settings",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }

            // ── Sign Out ──────────────────────────────────────────────────────
            OutlinedButton(
                onClick = {
                    navController.navigate("user_type") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SurviveMumRed
                )
            ) {
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}