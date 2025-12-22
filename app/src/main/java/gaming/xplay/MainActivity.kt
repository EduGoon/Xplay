package gaming.xplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.google.firebase.FirebaseApp
import dagger.hilt.android.AndroidEntryPoint
import gaming.xplay.data.model.Theme
import gaming.xplay.presentation.ui.MainApp
import gaming.xplay.presentation.viewmodel.ThemeViewModel
import gaming.xplay.theme.XplayTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            val theme by themeViewModel.theme.collectAsState()
            val useDarkTheme = when (theme) {
                Theme.LIGHT -> false
                Theme.DARK -> true
                Theme.SYSTEM -> isSystemInDarkTheme()
            }

            XplayTheme(darkTheme = useDarkTheme) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Use the default web client ID from the google-services.json file
                    val webClientId = getString(R.string.default_web_client_id)
                    MainApp(webClientId = webClientId)
                }
            }
        }
    }
}
