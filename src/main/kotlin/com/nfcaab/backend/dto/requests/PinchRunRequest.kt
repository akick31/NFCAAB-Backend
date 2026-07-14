package com.nfcaab.backend.dto.requests

import com.fasterxml.jackson.annotation.JsonProperty
import com.nfcaab.backend.model.Game

data class PinchRunRequest(
    @JsonProperty("team") val team: String,
    @JsonProperty("base") val base: Game.Base,
    @JsonProperty("incomingUniformNumber") val incomingUniformNumber: Int,
)
