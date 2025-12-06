package gaming.xplay.data.model

data class Club(
    val clubId: String = "",
    val clubName: String = "",
    val adminId: String = "",
    val members: Int = 0,
    val imageUrl: String = "",
    val memberIds: List<String> = emptyList(),
    val pendingMemberIds: List<String> = emptyList(),
    val tournaments: List<String> = emptyList()
)
