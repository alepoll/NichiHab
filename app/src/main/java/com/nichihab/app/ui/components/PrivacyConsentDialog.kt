package com.nichihab.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyConsentDialog(onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Must accept to continue */ },
        title = {
            Text(
                "Data Usage & Privacy",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "NichiHab requires certain permissions and data to function as an effective app-blocker and productivity tool:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    "• Photos & Camera: Sent to Google's AI service (Gemini) to verify your task completions. Photos are processed in real-time and are NOT stored.",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    "• Installed Apps: We need to see your installed apps so you can select which distracting apps to block.",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    "• App Usage (Accessibility Service): We monitor when you open an app to enforce your selected blocks during focus sessions.",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    "By continuing, you agree to our Privacy Policy and the processing of this data.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("I Agree")
            }
        },
        dismissButton = null,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}
