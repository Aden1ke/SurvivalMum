package com.survivemum.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.survivemum.app.ui.theme.SurviveMumTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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

    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var facilityName by remember { mutableStateOf("") }
    var community by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("en") }

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
            .background(MaterialTheme.colorScheme.background)  // CHANGED
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = if (isTBA) "TBA Registration" else "Mother Registration",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground  // CHANGED
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isTBA)
                "Create your account to monitor patients"
            else
                "Create your account to monitor your pregnancy",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant  // CHANGED
        )

        Spacer(modifier = Modifier.height(32.dp))

        SurviveMumTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = "Full Name",
            placeholder = "Enter your full name"
        )

        Spacer(modifier = Modifier.height(16.dp))

        SurviveMumTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = "Phone Number (optional)",
            placeholder = "+234...",
            keyboardType = KeyboardType.Phone
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isTBA) {
            SurviveMumTextField(
                value = facilityName,
                onValueChange = { facilityName = it },
                label = "Health Facility Name",
                placeholder = "e.g. Kano North PHC"
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        SurviveMumTextField(
            value = community,
            onValueChange = { community = it },
            label = "Community / LGA",
            placeholder = "e.g. Kano North LGA"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Language",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground  // CHANGED
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box {
            OutlinedButton(
                onClick = { showLanguageDropdown = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground  // CHANGED
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

        Text(
            text = "Create a 4-digit PIN",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground  // CHANGED
        )
        Text(
            text = "Used to protect your patient data",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant  // CHANGED
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
                focusedBorderColor = MaterialTheme.colorScheme.primary,      // CHANGED
                unfocusedBorderColor = MaterialTheme.colorScheme.outline      // CHANGED
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

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
                focusedBorderColor = MaterialTheme.colorScheme.primary,      // CHANGED
                unfocusedBorderColor = MaterialTheme.colorScheme.outline      // CHANGED
            )
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,  // CHANGED
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (validate()) {
                    isLoading = true
                    scope.launch {
                        try {
                            val db = SurviveMumDatabase.getDatabase(context)
                            val userId = UUID.randomUUID().toString()
                            val now = System.currentTimeMillis().toString()

                            val user = UserEntity(
                                userId = userId,
                                fullName = fullName,
                                phoneNumber = phoneNumber.ifBlank { null },
                                facilityName = facilityName,
                                community = community,
                                language = selectedLanguage,
                                pinHash = pin,
                                userType = userType,
                                createdAt = now,
                                lastLoginAt = now,
                                isActive = 1
                            )

                            db.userDao().insertUser(user)

                            val prefs = PreferenceEntity(
                                userId = userId,
                                language = selectedLanguage,
                                userType = userType,
                                updatedAt = now
                            )

                            db.preferenceDao().savePreferences(prefs)

                            isLoading = false

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
                containerColor = MaterialTheme.colorScheme.primary  // CHANGED
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Create Account",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CENTERED — Already have an account
        TextButton(
            onClick = { navController.navigate("login/$userType") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Already have an account? Log in",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}