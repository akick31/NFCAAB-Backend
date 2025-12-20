package com.nfcaab.backend.dto.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class LineupSubmissionRequest(
    @JsonProperty("gameId") val gameId: Int,
    @JsonProperty("team") val team: String,
    @JsonProperty("batters") val batters: List<Int>, // Uniform numbers for spots 1-9
    @JsonProperty("pitcher") val pitcher: Int, // Uniform number for pitcher
)

