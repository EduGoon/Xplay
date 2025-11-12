package gaming.xplay.ui.presentation

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import gaming.xplay.viewmodel.AuthViewModel

@Composable
fun LoginScreen(authViewModel: AuthViewModel) {
    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "Welcome to Xplay",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                "Sign in to continue",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 64.dp)
            )
            SocialAuthButtons(authViewModel)

        }
    }
}

@Composable
private fun SocialAuthButtons(authViewModel: AuthViewModel) {
    val signInState by authViewModel.signInState.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current


    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode != Activity.RESULT_OK) {
                Log.d("GoogleSignIn", "Sign-in cancelled")
                isLoading = false
                return@rememberLauncherForActivityResult
            }
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                authViewModel.signInWithGoogle(idToken)
            } else {
                Log.e("GoogleSignIn", "Missing idToken")
                isLoading = false
            }
        } catch (e: ApiException) {
            Log.e("GoogleSignIn", "Google API error (${e.statusCode})", e)
            isLoading = false
        } catch (t: Throwable) {
            Log.e("GoogleSignIn", "Unhandled launcher error", t)
            isLoading = false
        }
    }


    GoogleAuthButton(
        onClick = {
            isLoading = true
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("219219111613-g5u92aa14eoru26tq7ph5kepe0ndg0d2.apps.googleusercontent.com")
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            googleSignInClient.signOut().addOnCompleteListener {
                launcher.launch(googleSignInClient.signInIntent)
            }
        },
        isLoading = isLoading
    )

    if (signInState == false) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Sign-in failed. Please try again.",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
