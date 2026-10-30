package it.palsoftware.pastiera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import it.palsoftware.pastiera.ui.theme.PastieraTheme
import it.palsoftware.pastiera.ui.theme.rememberQ25ThemeMode

class LanguagesActivity : LocalizedComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PastieraTheme(themeMode = rememberQ25ThemeMode()) {
                LanguagesScreen(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

}
