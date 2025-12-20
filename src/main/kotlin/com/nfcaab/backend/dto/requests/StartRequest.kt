package com.nfcaab.backend.dto.requests

import com.fasterxml.jackson.annotation.JsonProperty
import com.nfcaab.backend.model.Game.GameType
import com.nfcaab.backend.enums.team.Subdivision

data class StartRequest(
    @JsonProperty("subdivision") val subdivision: Subdivision,
    @JsonProperty("homeTeam") val homeTeam: String,
    @JsonProperty("awayTeam") val awayTeam: String,
    @JsonProperty("gameType") val gameType: GameType,
    @JsonProperty("seriesGameNumber") val seriesGameNumber: Int,
)

