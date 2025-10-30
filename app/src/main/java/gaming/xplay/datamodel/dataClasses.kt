package gaming.xplay.datamodel

data class User(
    val uid: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val CountyOfResidence: String = "",
    val XplayPoints: Int = 0,
    val ranking: Int = 0,
    var wins: Int = 0,
    var draws: Int = 0,
    var losses: Int = 0
) {
    val gamesPlayed: Int
        get() = wins + draws + losses

    fun recordWin() {
        wins++
    }

    fun recordDraw() {
        draws++
    }

    fun recordLoss() {
        losses++
    }
}
