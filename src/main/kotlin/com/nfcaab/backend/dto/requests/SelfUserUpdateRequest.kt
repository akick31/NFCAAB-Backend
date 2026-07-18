package com.nfcaab.backend.dto.requests

import com.fasterxml.jackson.annotation.JsonProperty

data class SelfUserUpdateRequest(
    @JsonProperty("username") val username: String? = null,
    @JsonProperty("email") val email: String? = null,
    @JsonProperty("password") val password: String? = null,
)
