
package gaming.xplay.ui.presentation

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import gaming.xplay.ui.theme.VibrantRed
import gaming.xplay.ui.theme.LightText
import gaming.xplay.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val phoneNumber by authViewModel.phoneNumber.collectAsStateWithLifecycle()
    val verificationCode by authViewModel.verificationCode.collectAsStateWithLifecycle()
    val isCodeSent by authViewModel.isCodeSent.collectAsStateWithLifecycle()
    val error by authViewModel.error.collectAsStateWithLifecycle()
    val loginSuccess by authViewModel.loginSuccess.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            navController.navigate("home") {
                // Prevent going back to login screen
                popUpTo("login") { inclusive = true }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "XPLAY",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = VibrantRed,
                    fontSize = 50.sp
                )
            )
            Spacer(modifier = Modifier.height(48.dp))

            if (!isCodeSent) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = authViewModel::onPhoneNumberChanged,
                    label = { Text("Enter your number") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = VibrantRed,
                        unfocusedIndicatorColor = LightText.copy(alpha = 0.5f),
                        cursorColor = VibrantRed,
                        focusedLabelColor = VibrantRed,
                        unfocusedLabelColor = LightText.copy(alpha = 0.7f)
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { authViewModel.sendVerificationCode(context as Activity) },
                    enabled = phoneNumber.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantRed)
                ) {
                    Text("GET CODE", color = LightText)
                }
            } else {
                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = authViewModel::onVerificationCodeChanged,
                    label = { Text("Enter verification code") },
                    singleLine = true,
                     colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = VibrantRed,
                        unfocusedIndicatorColor = LightText.copy(alpha = 0.5f),
                        cursorColor = VibrantRed,
                        focusedLabelColor = VibrantRed,
                        unfocusedLabelColor = LightText.copy(alpha = 0.7f)
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { authViewModel.verifyCode() },
                    enabled = verificationCode.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantRed)
                ) {
                    Text("VERIFY & PLAY", color = LightText)
                }
            }

            error?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
