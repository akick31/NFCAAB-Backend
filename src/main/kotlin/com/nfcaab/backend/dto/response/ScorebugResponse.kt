package com.nfcaab.backend.dto.response

import com.nfcaab.backend.model.Game

data class ScorebugResponse(
    val gameId: Int,
    val scorebug: ByteArray?,
    val homeTeam: String,
    val awayTeam: String,
    val status: Game.GameStatus?,
)

