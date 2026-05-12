package com.survivemum.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.survivemum.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController, userType: String) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val numbers = listOf(
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

        Text(
            text = "SurviveMum",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "The Silent Guardian",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Enter your PIN",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        // PIN dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < pin.length)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline
                        )
                )
            }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Number pad
        numbers.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                when (key) {
                                    "✓" -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
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
                                        if (pin.length == 4) {
                                            isLoading = true
                                            scope.launch {
                                                try {
                                                    val db = SurviveMumDatabase.getDatabase(context)
                                                    val user = db.userDao().getCurrentUser()
                                                    if (user != null && user.pinHash == pin) {
                                                        db.userDao().updateLastLogin(
                                                            user.userId,
                                                            System.currentTimeMillis().toString()
                                                        )
                                                        isLoading = false
                                                        navController.navigate("home/${user.userType}") {
                                                            popUpTo("login/$userType") {
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
                                        } else {
                                            errorMessage = "Enter all 4 digits"
                                        }
                                    }
                                    else -> {
                                        if (pin.length < 4) {
                                            pin += key
                                            errorMessage = ""
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isLoading && key == "✓") {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,  // CHANGED
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = key,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (key) {
                                        "✓" -> MaterialTheme.colorScheme.onPrimary  // CHANGED
                                        else -> MaterialTheme.colorScheme.onBackground
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        TextButton(
            onClick = { navController.navigate("signup/$userType") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "New user? Create account",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}