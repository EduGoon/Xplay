package gaming.xplay.ui.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import gaming.xplay.R

@Composable
fun GoogleAuthButton(
    onClick: () -> Unit,
    text: String = "Sign in with Google",
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(50.dp)
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.Black
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
