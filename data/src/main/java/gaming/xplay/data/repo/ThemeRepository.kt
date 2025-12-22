package gaming.xplay.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import gaming.xplay.data.model.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemeRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme")

    val theme: Flow<Theme> = context.dataStore.data.map {
        Theme.valueOf(it[themeKey] ?: Theme.SYSTEM.name)
    }

    suspend fun setTheme(theme: Theme) {
        context.dataStore.edit {
            it[themeKey] = theme.name
        }
    }
}