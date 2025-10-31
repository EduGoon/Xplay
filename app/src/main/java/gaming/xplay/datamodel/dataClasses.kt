package gaming.xplay.datamodel

data class Player(
    val playerid: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val CountyOfResidence: String = ""
)

data class Game(
    val gameid: String,
    val name: String,
)

data class Match(
    val matchid: String,
    val player1: Player,
    val player2: Player,
    val score: String,
    val winner: Player,
)

data class rankings(
    val id: String,
    val playerid: String,
    val gameid: String,
    val XPpoints: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
)
