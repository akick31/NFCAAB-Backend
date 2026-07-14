package com.nfcaab.backend.dto.requests

import com.fasterxml.jackson.annotation.JsonProperty
import com.nfcaab.backend.model.Player

data class LineupSubstitutionRequest(
    @JsonProperty("gameId") val gameId: Int,
    @JsonProperty("team") val team: String,
    @JsonProperty("outgoingUniformNumber") val outgoingUniformNumber: Int,
    @JsonProperty("incomingUniformNumber") val incomingUniformNumber: Int,
    @JsonProperty("incomingPosition") val incomingPosition: Player.Position,
)
