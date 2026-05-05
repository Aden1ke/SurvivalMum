package com.survivemum.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * The initial entry screen for SurviveMum.
 * Its primary purpose is to bifurcate the user experience between 
 * professional community guardians (TBAs) and individual family guardians (Mothers).
 */
@Composable
fun UserTypeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        //  BRANDING SECTION 
        Text(
            text = "SurviveMum",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFC0392B) // Brand Red: Symbolic of life and urgency
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "The Silent Guardian",
            fontSize = 16.sp,
            color = Color(0xFF717D7E)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "An AI guardian for patients who cannot speak",
            fontSize = 13.sp,
            color = Color(0xFF717D7E),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(60.dp))

        //  ROLE SELECTION PROMPT 
        Text(
            text = "Who are you?",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "This helps us show you the right tools",
            fontSize = 14.sp,
            color = Color(0xFF717D7E),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        //  ROLE: TRADITIONAL BIRTH ATTENDANT (TBA) 
        // Clicking this routes to signup with the 'TBA' argument, 
        // unlocking multipatient management and clinical assessment tools.
        Button(
            onClick = { navController.navigate("signup/TBA") },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1A1A2E) // Deep Blue: Professional and Trustworthy
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "I am a Birth Attendant (TBA)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "I help mothers during pregnancy and birth",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        //  ROLE: MOTHER / CAREGIVER 
        // Clicking this routes to signup with the 'MOTHER' argument,
        // focusing the UI on personal health monitoring and newborn care.
        Button(
            onClick = { navController.navigate("signup/MOTHER") },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFC0392B) // Warm Red: Personal and Protective
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "I am a Mother or Caregiver",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "I want to monitor my own pregnancy or baby",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        //  MISSION PRIVACY FOOTER 
        // Reinforces the core value proposition: Privacy and Offline Autonomy.
        Text(
            text = "Works 100% offline\nYour data never leaves this phone",
            fontSize = 13.sp,
            color = Color(0xFF717D7E),
            textAlign = TextAlign.Center
        )
    }
}
