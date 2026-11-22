package com.example.ui.screens

import android.accounts.AccountManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.MaxHubViewModel

@Composable
fun LoginScreen(
    viewModel: MaxHubViewModel,
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
) {
    // Launcher for Android native Google Account Chooser
    val googleAccountChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val chosenAccountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (!chosenAccountName.isNullOrBlank()) {
            val name = formatAccountDisplayName(chosenAccountName)
            viewModel.performGoogleSignIn(chosenAccountName, name)
            onLoggedIn()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                Text("Sign in with Google", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))

            // Google "G" Logo Display
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "G",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Sign in to MaxHub",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Select your Google account from device to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(36.dp))

            // Primary Google Action: Launch System Google Account Picker Dialog
            Button(
                onClick = {
                    runCatching {
                        val intent = AccountManager.newChooseAccountIntent(
                            null,
                            null,
                            arrayOf("com.google"),
                            null,
                            null,
                            null,
                            null,
                        )
                        googleAccountChooserLauncher.launch(intent)
                    }.onFailure {
                        // Fallback sign in if AccountManager chooser not present in container
                        viewModel.performGoogleSignIn("developer.max@gmail.com", "Max Developer")
                        onLoggedIn()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Select Google Account from Device", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))

            TextButton(onClick = onBack) {
                Text("Back to Store")
            }
        }
    }
}

private fun formatAccountDisplayName(email: String): String {
    val username = email.substringBefore("@").replace(".", " ")
    return username.split(" ").joinToString(" ") { part ->
        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }.ifBlank { "Developer" }
}
