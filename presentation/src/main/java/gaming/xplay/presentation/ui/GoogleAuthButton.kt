package gaming.xplay.presentation.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import gaming.xplay.presentation.R

@Composable
fun GoogleAuthButton(
    onClick: () -> Unit,
    text: String = "Sign in with Google",
    isLoading: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .height(50.dp)
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google logo",
                modifier = Modifier
                    .size(24.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(text = if (isLoading) "Signing in..." else text)
    }
}