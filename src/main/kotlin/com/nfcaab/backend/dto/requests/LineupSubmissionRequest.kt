package com.nfcaab.backend.dto.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class LineupSubmissionRequest(
    @JsonProperty("token") val token: String,
    @JsonProperty("batters") val batters: List<Int>,
    @JsonProperty("pitcher") val pitcher: Int,
)
