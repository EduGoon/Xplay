package gaming.xplay.presentation.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSessionManager @Inject constructor() {

    private val _logoutEvents = MutableSharedFlow<Unit>()
    val logoutEvents = _logoutEvents.asSharedFlow()

    suspend fun onLogout() {
        _logoutEvents.emit(Unit)
    }
}
