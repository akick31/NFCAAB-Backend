package com.nfcaab.porygon.dto.response

import com.nfcaab.porygon.model.Game

data class ScorebugResponse(
    val gameId: Int,
    val scorebug: ByteArray?,
    val homeTeam: String,
    val awayTeam: String,
    val status: Game.GameStatus?,
)

