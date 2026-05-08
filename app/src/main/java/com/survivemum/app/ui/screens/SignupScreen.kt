package com.survivemum.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.survivemum.app.data.PreferenceEntity
import com.survivemum.app.data.SurviveMumDatabase
import com.survivemum.app.data.UserEntity
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun SignupScreen(navController: NavController, userType: String) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Form fields
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var facilityName by remember { mutableStateOf("") }
    var community by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("en") }

    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showLanguageDropdown by remember { mutableStateOf(false) }

    val isTBA = userType == "TBA"

    val languages = mapOf(
        "en" to "English",
        "yo" to "Yoruba",
        "ha" to "Hausa",
        "ig" to "Igbo",
        "pcm" to "Nigerian Pidgin"
    )

    // Validation
    fun validate(): Boolean {
        return when {
            fullName.isBlank() -> {
                errorMessage = "Please enter your full name"
                false
            }
            pin.length != 4 -> {
                errorMessage = "PIN must be exactly 4 digits"
                false
            }
            pin != confirmPin -> {
                errorMessage = "PINs do not match"
                false
            }
            !pin.all { it.isDigit() } -> {
                errorMessage = "PIN must contain only numbers"
                false
            }
            else -> {
                errorMessage = ""
                true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDFEFE))
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {

        Spacer(modifier = Modifier.height(40.dp))

        // Header
        Text(
            text = if (isTBA) "TBA Registration" else "Mother Registration",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isTBA)
                "Create your account to monitor patients"
            else
                "Create your account to monitor your pregnancy",
            fontSize = 14.sp,
            color = Color(0xFF717D7E)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Full Name
        SurviveMumTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = "Full Name",
            placeholder = "Enter your full name"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Phone Number
        SurviveMumTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = "Phone Number (optional)",
            placeholder = "+234...",
            keyboardType = KeyboardType.Phone
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Facility — TBA only
        if (isTBA) {
            SurviveMumTextField(
                value = facilityName,
                onValueChange = { facilityName = it },
                label = "Health Facility Name",
                placeholder = "e.g. Kano North PHC"
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Community
        SurviveMumTextField(
            value = community,
            onValueChange = { community = it },
            label = "Community / LGA",
            placeholder = "e.g. Kano North LGA"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Language Selector
        Text(
            text = "Language",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1A2E)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box {
            OutlinedButton(
                onClick = { showLanguageDropdown = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF1A1A2E)
                )
            ) {
                Text(
                    text = languages[selectedLanguage] ?: "English",
                    modifier = Modifier.weight(1f)
                )
                Text(text = "▼", fontSize = 12.sp)
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
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PIN
        Text(
            text = "Create a 4-digit PIN",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1A2E)
        )
        Text(
            text = "Used to protect your patient data",
            fontSize = 12.sp,
            color = Color(0xFF717D7E)
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4) pin = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("••••") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFC0392B),
                unfocusedBorderColor = Color(0xFFDDDDDD)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Confirm PIN
        OutlinedTextField(
            value = confirmPin,
            onValueChange = { if (it.length <= 4) confirmPin = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Confirm PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFC0392B),
                unfocusedBorderColor = Color(0xFFDDDDDD)
            )
        )

        // Error Message
        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = Color(0xFFC0392B),
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Create Account Button
        Button(
            onClick = {
                if (validate()) {
                    isLoading = true
                    scope.launch {
                        try {
                            val db = SurviveMumDatabase.getDatabase(context)

                            val userId = UUID.randomUUID().toString()
                            val now = System.currentTimeMillis().toString()

                            // Save user to database
                            val user = UserEntity(
                                userId = userId,
                                fullName = fullName,
                                phoneNumber = phoneNumber.ifBlank { null },
                                facilityName = facilityName,
                                community = community,
                                language = selectedLanguage,
                                pinHash = pin, // BE-2 will hash this properly
                                userType = userType,
                                createdAt = now,
                                lastLoginAt = now,
                                isActive = 1
                            )

                            db.userDao().insertUser(user)

                            // Save preferences
                            val prefs = PreferenceEntity(
                                userId = userId,
                                language = selectedLanguage,
                                userType = userType,
                                updatedAt = now
                            )

                            db.preferenceDao().savePreferences(prefs)

                            isLoading = false

                            // Navigate to home
                            navController.navigate("home/$userType") {
                                popUpTo("usertype") { inclusive = true }
                            }

                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Error creating account: ${e.message}"
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFC0392B)
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Create Account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Already have account
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Already have an account? ",
                color = Color(0xFF717D7E),
                fontSize = 14.sp
            )
            TextButton(
                onClick = { navController.navigate("login/$userType") },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Log in",
                    color = Color(0xFFC0392B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// Reusable text field component
@Composable
fun SurviveMumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1A2E)
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = Color(0xFFBDC3C7)) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFC0392B),
                unfocusedBorderColor = Color(0xFFDDDDDD),
                focusedLabelColor = Color(0xFFC0392B)
            )
        )
    }
}
