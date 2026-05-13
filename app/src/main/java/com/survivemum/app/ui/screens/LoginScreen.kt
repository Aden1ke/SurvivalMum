package com.survivemum.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.survivemum.app.data.SurviveMumDatabase
import com.survivemum.app.navigation.Screen
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController, userType: String) {

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var pin          by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading    by remember { mutableStateOf(false) }

    // Issue 4: the keypad is the ONLY input — no soft keyboard, no text field.
    // Letters are structurally impossible because the buttons only emit digits.
    val keyRows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("⌫", "0", "✓")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SurviveMum",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text("The Silent Guardian",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Show which account type is being logged into
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (userType == "TBA") "Signing in as Birth Attendant"
            else "Signing in as Mother / Caregiver",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(48.dp))
        Text("Enter your PIN",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(24.dp))

        // PIN dots
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < pin.length) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                )
            }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(40.dp))

        // Keypad
        keyRows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                row.forEach { key ->
                    val isConfirm = key == "✓"
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (isConfirm) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = {
                                when (key) {
                                    "⌫" -> {
                                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                        errorMessage = ""
                                    }
                                    "✓" -> {
                                        if (pin.length < 4) {
                                            errorMessage = "Enter all 4 digits"
                                            return@TextButton
                                        }
                                        isLoading = true
                                        scope.launch {
                                            try {
                                                val db = SurviveMumDatabase.getDatabase(context)

                                                // Issue 5 fix: look up user by userType AND pin
                                                // so a TBA pin never unlocks a mother account
                                                // and vice versa.
                                                val user = db.userDao().getUserByType(userType)

                                                if (user != null && user.pinHash == pin) {
                                                    db.userDao().updateLastLogin(
                                                        user.userId,
                                                        System.currentTimeMillis().toString()
                                                    )
                                                    isLoading = false
                                                    navController.navigate(
                                                        Screen.HomeDashboard.go(user.userType)
                                                    ) {
                                                        // Clear entire auth stack
                                                        popUpTo(Screen.UserType.route) {
                                                            inclusive = true
                                                        }
                                                    }
                                                } else {
                                                    isLoading = false
                                                    errorMessage = "Wrong PIN. Try again."
                                                    pin = ""
                                                }
                                            } catch (e: Exception) {
                                                isLoading = false
                                                errorMessage = "Error: ${e.message}"
                                            }
                                        }
                                    }
                                    else -> {
                                        // Issue 4: only digits 0-9 are on the keypad.
                                        // No text field = no soft keyboard = no letters possible.
                                        if (pin.length < 4) {
                                            pin += key
                                            errorMessage = ""
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isLoading && isConfirm) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    key,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConfirm) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        TextButton(
            onClick = { navController.navigate(Screen.Signup.go(userType)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("New user? Create account",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                textAlign = TextAlign.Center)
        }
    }
}

// ── Required UserDao addition ─────────────────────────────────────────────────
// Add this query to UserDao.kt so login is scoped to userType:
//
// @Query("SELECT * FROM users WHERE userType = :userType AND isActive = 1 ORDER BY lastLoginAt DESC LIMIT 1")
// suspend fun getUserByType(userType: String): UserEntity?
//
// Without this, getCurrentUser() returns whichever user logged in last regardless
// of type, which is what caused the TBA PIN to enter the mother's account.
// ─────────────────────────────────────────────────────────────────────────────