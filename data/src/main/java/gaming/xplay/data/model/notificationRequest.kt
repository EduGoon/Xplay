package gaming.xplay.data.model

data class NotificationRequest(
    val targetUserId: String,
    val title: String,
    val body: String,
    val requestId: String = java.util.UUID.randomUUID().toString()
)
