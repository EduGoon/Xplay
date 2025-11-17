package gaming.xplay.presentation.ui.State

sealed class NotificationState {
    object Idle : NotificationState()
    object Sending : NotificationState()
    data class Success(val accepted: Boolean) : NotificationState()
    data class Error(val message: String) : NotificationState()
    object Timeout : NotificationState()
}
