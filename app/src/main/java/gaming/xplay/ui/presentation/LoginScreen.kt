package gaming.xplay.ui.presentation

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import gaming.xplay.datamodel.UiState
import gaming.xplay.ui.theme.LightText
import gaming.xplay.ui.theme.VibrantRed
import gaming.xplay.viewmodel.AuthViewModel

// TODO: Move this to a secure place and reference it from string resources
private const val WEB_CLIENT_ID = "219219111613-g5u92aa14eoru26tq7ph5kepe0ndg0d2.apps.googleusercontent.com"

@Composable
fun LoginScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val uiState by authViewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account?.idToken != null) {
                    authViewModel.signInWithGoogle(account.idToken!!)
                } else {
                    Log.w("LoginScreen", "Google sign-in successful but idToken is null.")
                }
            } catch (e: ApiException) {
                Log.e("LoginScreen", "Google sign-in failed with ApiException", e)
            }
        } else {
            Log.w("LoginScreen", "Google sign-in flow was cancelled or failed.")
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
            Spacer(modifier = Modifier.height(64.dp))

            when (uiState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(color = VibrantRed)
                }
                is UiState.Success -> {
                    // Show the sign-in button only when the user is not authenticated
                    if ((uiState as UiState.Success<*>).data == null) {
                        Button(
                            onClick = {
                                val gso =
                                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken(WEB_CLIENT_ID)
                                        .requestEmail()
                                        .build()
                                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VibrantRed)
                        ) {
                            Text("Sign in with Google", color = LightText)
                        }
                    }
                }
                is UiState.Error -> {
                    val error = (uiState as UiState.Error).message
                    Text(
                        text = error,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
