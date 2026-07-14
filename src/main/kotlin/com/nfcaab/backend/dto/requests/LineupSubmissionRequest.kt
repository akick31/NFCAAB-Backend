package com.nfcaab.backend.dto.requests

import com.fasterxml.jackson.annotation.JsonProperty
import com.nfcaab.backend.model.Player

data class BatterSubmission(
    @JsonProperty("uniformNumber") val uniformNumber: Int,
    @JsonProperty("position") val position: Player.Position,
)

data class LineupSubmissionRequest(
    @JsonProperty("token") val token: String,
    @JsonProperty("batters") val batters: List<BatterSubmission>,
    @JsonProperty("pitcher") val pitcher: Int,
)
