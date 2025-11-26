package gaming.xplay.presentation.model

import gaming.xplay.data.model.Player
import gaming.xplay.data.model.rankings

data class PlayerSearchResult(
    val player: Player,
    val ranking: rankings?
)
