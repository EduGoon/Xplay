package gaming.xplay.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gaming.xplay.repo.NotificationRepository
import gaming.xplay.repo.NotificationRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NotificationState {
    object Idle : NotificationState()
    object Sending : NotificationState()
    data class Success(val accepted: Boolean) : NotificationState()
    data class Error(val message: String) : NotificationState()
    object Timeout : NotificationState()
}

class NotificationViewModel(
    private val repository: NotificationRepository = NotificationRepository()
) : ViewModel() {

    private val _notificationState = MutableStateFlow<NotificationState>(NotificationState.Idle)
    val notificationState: StateFlow<NotificationState> = _notificationState.asStateFlow()

    /**
     * Send notification to another user
     * Call this from your UI
     */
    fun sendNotificationToUser(
        targetUserId: String,
        title: String,
        body: String,
        timeoutSeconds: Long = 30
    ) {
        viewModelScope.launch {
            _notificationState.value = NotificationState.Sending

            val request = NotificationRequest(
                targetUserId = targetUserId,
                title = title,
                body = body
            )

            val feedback = repository.sendNotificationAndAwaitFeedback(
                request = request,
                timeoutSeconds = timeoutSeconds
            )

            _notificationState.value = when (feedback) {
                true -> NotificationState.Success(accepted = true)
                false -> NotificationState.Success(accepted = false)
                null -> NotificationState.Timeout
            }
        }
    }

    fun resetState() {
        _notificationState.value = NotificationState.Idle
    }
}