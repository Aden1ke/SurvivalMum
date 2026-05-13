package com.survivemum.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.survivemum.app.navigation.Screen
import com.survivemum.app.ui.theme.SurviveMumTextField
import kotlinx.coroutines.launch
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// SignupScreen handles three distinct userType values:
//
//   "TBA"    — birth attendant onboarding (from UserTypeScreen)
//   "mother" — mother onboarding (from UserTypeScreen)
//   "PATIENT"— a mother being registered BY a TBA (from HomeDashboard Add Patient)
//
// The key difference for "PATIENT":
//   • No account is created for the mother — she is registered as a PatientEntity
//     under the TBA's userId, NOT as a UserEntity who can log in.
//   • After saving, we popUpTo the TBA's dashboard (not UserTypeScreen)
//     so Back never leaks into the mother's session.
//   • The TBA stays logged in throughout.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SignupScreen(navController: NavController, userType: String) {

    // "PATIENT" means a TBA is registering a mother patient — not self-onboarding
    val isPatientRegistration = userType == "PATIENT"
    val isTBA                 = userType == "TBA"

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var fullName         by remember { mutableStateOf("") }
    var phoneNumber      by remember { mutableStateOf("") }
    var facilityName     by remember { mutableStateOf("") }
    var community        by remember { mutableStateOf("") }
    var pin              by remember { mutableStateOf("") }
    var confirmPin       by remember { mutableStateOf("") }
    var weeksPregnant    by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("en") }
    var isLoading        by remember { mutableStateOf(false) }
    var errorMessage     by remember { mutableStateOf("") }
    var showLangDropdown by remember { mutableStateOf(false) }

    val languages = mapOf(
        "en"  to "English",
        "yo"  to "Yoruba",
        "ha"  to "Hausa",
        "ig"  to "Igbo",
        "pcm" to "Nigerian Pidgin"
    )

    // ── Issue 4: strict validation ────────────────────────────────────────────
    fun validate(): Boolean {
        errorMessage = ""

        // Full name — no numbers allowed
        if (fullName.isBlank()) {
            errorMessage = "Please enter full name"; return false
        }
        if (fullName.any { it.isDigit() }) {
            errorMessage = "Name must not contain numbers"; return false
        }

        // Phone — Nigerian format: +234 followed by exactly 10 digits
        // Also accept 080/090/070/081 local format and convert to +234
        if (phoneNumber.isNotBlank()) {
            val cleaned = phoneNumber.trim()
            val validInternational = cleaned.matches(Regex("^\\+234[0-9]{10}$"))
            val validLocal         = cleaned.matches(Regex("^0[789][01][0-9]{8}$"))
            if (!validInternational && !validLocal) {
                errorMessage = "Phone: use +234XXXXXXXXXX or 080XXXXXXXX format"
                return false
            }
        }

        // Weeks pregnant (only for patient registration)
        if (isPatientRegistration) {
            val weeks = weeksPregnant.toIntOrNull()
            if (weeks == null || weeks !in 1..42) {
                errorMessage = "Weeks pregnant must be between 1 and 42"
                return false
            }
        }

        // PIN — only required when creating a user account (not patient registration)
        if (!isPatientRegistration) {
            if (pin.length != 4) {
                errorMessage = "PIN must be exactly 4 digits"; return false
            }
            if (!pin.all { it.isDigit() }) {
                errorMessage = "PIN must contain digits only"; return false
            }
            if (pin != confirmPin) {
                errorMessage = "PINs do not match"; return false
            }
        }

        return true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(40.dp))

        Text(
            text = when {
                isPatientRegistration -> "Register Patient"
                isTBA                 -> "TBA Registration"
                else                  -> "Mother Registration"
            },
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = when {
                isPatientRegistration -> "Add a mother to your patient list"
                isTBA                 -> "Create your account to monitor patients"
                else                  -> "Create your account to monitor your pregnancy"
            },
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // Full name — letters and spaces only enforced by filtering
        SurviveMumTextField(
            value = fullName,
            onValueChange = { input ->
                // Allow letters, spaces, hyphens, apostrophes (common in Nigerian names)
                if (input.all { it.isLetter() || it == ' ' || it == '-' || it == '\'' }) {
                    fullName = input
                }
            },
            label = "Full Name",
            placeholder = if (isPatientRegistration) "Patient's full name" else "Your full name"
        )
        Spacer(Modifier.height(16.dp))

        // Phone with format hint
        SurviveMumTextField(
            value = phoneNumber,
            onValueChange = { raw ->
                // Only allow +, digits — max 14 chars (+234XXXXXXXXXX)
                val filtered = raw.filter { it.isDigit() || it == '+' }
                if (filtered.length <= 14) phoneNumber = filtered
            },
            label = "Phone Number (optional)",
            placeholder = "+234XXXXXXXXXX or 080XXXXXXXX",
            keyboardType = KeyboardType.Phone
        )
        Spacer(Modifier.height(16.dp))

        // Facility name — TBA only
        if (isTBA) {
            SurviveMumTextField(
                value = facilityName,
                onValueChange = { facilityName = it },
                label = "Health Facility Name",
                placeholder = "e.g. Kano North PHC"
            )
            Spacer(Modifier.height(16.dp))
        }

        SurviveMumTextField(
            value = community,
            onValueChange = { community = it },
            label = "Community / LGA",
            placeholder = "e.g. Ikorodu LGA"
        )
        Spacer(Modifier.height(16.dp))

        // Weeks pregnant — only for patient registration by TBA
        if (isPatientRegistration) {
            SurviveMumTextField(
                value = weeksPregnant,
                onValueChange = { input ->
                    // Only digits, max 2 chars (1–42)
                    if (input.all { it.isDigit() } && input.length <= 2) {
                        weeksPregnant = input
                    }
                },
                label = "Weeks Pregnant",
                placeholder = "e.g. 28",
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(16.dp))
        }

        // Language picker
        Text("Language", fontSize = 14.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Box {
            OutlinedButton(
                onClick = { showLangDropdown = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground)
            ) {
                Text(languages[selectedLanguage] ?: "English",
                    modifier = Modifier.weight(1f))
                Text("▼", fontSize = 12.sp)
            }
            DropdownMenu(
                expanded = showLangDropdown,
                onDismissRequest = { showLangDropdown = false }
            ) {
                languages.forEach { (code, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = { selectedLanguage = code; showLangDropdown = false }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // PIN fields — only for user account creation, not patient registration
        if (!isPatientRegistration) {
            Text("Create a 4-digit PIN", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground)
            Text("Used to protect your data", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = pin,
                // Issue 4: only digits accepted, max 4 chars, enforced here not just in validate()
                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("••••") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) confirmPin = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Confirm PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if (!validate()) return@Button
                isLoading = true
                scope.launch {
                    try {
                        val db  = SurviveMumDatabase.getDatabase(context)
                        val now = System.currentTimeMillis().toString()

                        if (isPatientRegistration) {
                            // ── Issue 5 fix: TBA registering a patient ────────
                            // We do NOT create a UserEntity (no login account).
                            // We create a PatientEntity under the TBA's userId.
                            val tbaUser = db.userDao().getCurrentUser()
                                ?: throw Exception("No TBA session found")

                            val patientId = UUID.randomUUID().toString()
                            db.patientDao().insertPatient(
                                com.survivemum.app.data.PatientEntity(
                                    patientId      = patientId,
                                    tbaId          = tbaUser.userId,   // owned by TBA
                                    fullName       = fullName,
                                    phoneNumber    = phoneNumber.ifBlank { null },
                                    community      = community,
                                    language       = selectedLanguage,
                                    weeksPregnant  = weeksPregnant.toIntOrNull() ?: 0,
                                    riskLevel      = "LOW",
                                    createdAt      = now,
                                    updatedAt      = now
                                )
                            )

                            isLoading = false

                            // Issue 5 fix: pop back to TBA dashboard only —
                            // do NOT clear UserTypeScreen from the stack.
                            // The TBA's home is still on the stack; we just go back to it.
                            navController.navigate(Screen.HomeDashboard.go("TBA")) {
                                // Pop everything up to (but not including) the TBA dashboard
                                // so the back stack is: TBADashboard (only)
                                popUpTo(Screen.HomeDashboard.go("TBA")) {
                                    inclusive = false
                                }
                            }

                        } else {
                            // ── Normal user account creation (TBA or mother) ──
                            val userId = UUID.randomUUID().toString()
                            db.userDao().insertUser(
                                UserEntity(
                                    userId       = userId,
                                    fullName     = fullName,
                                    phoneNumber  = phoneNumber.ifBlank { null },
                                    facilityName = facilityName,
                                    community    = community,
                                    language     = selectedLanguage,
                                    pinHash      = pin,
                                    userType     = userType,
                                    createdAt    = now,
                                    lastLoginAt  = now,
                                    isActive     = 1
                                )
                            )
                            db.preferenceDao().savePreferences(
                                PreferenceEntity(
                                    userId    = userId,
                                    language  = selectedLanguage,
                                    userType  = userType,
                                    updatedAt = now
                                )
                            )

                            isLoading = false

                            // Clear entire auth stack — back from dashboard goes nowhere
                            navController.navigate(Screen.HomeDashboard.go(userType)) {
                                popUpTo(Screen.UserType.route) { inclusive = true }
                            }
                        }

                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = "Error: ${e.message}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary),
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
                    if (isPatientRegistration) "Register Patient" else "Create Account",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
            }
        }

        // "Already have an account" only makes sense for self-onboarding, not patient reg
        if (!isPatientRegistration) {
            Spacer(Modifier.height(24.dp))
            TextButton(
                onClick = { navController.navigate(Screen.Login.go(userType)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Already have an account? Log in",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}