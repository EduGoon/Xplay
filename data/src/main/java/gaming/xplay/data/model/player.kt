package gaming.xplay.data.model

data class Player(
    val uid: String = "",
    val name: String? = null,
    val email: String? = null,
    val profilePictureUrl: String? = null,
    val isFirstTime: Boolean = true,
    val isClubOwner: Boolean = false,
    val clubs: List<String> = emptyList(),
    val currentBadge: String? = null,
    val unlockedBadges: List<String> = emptyList()
)
