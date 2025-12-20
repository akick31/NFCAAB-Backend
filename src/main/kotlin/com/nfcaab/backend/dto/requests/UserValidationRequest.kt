package com.nfcaab.backend.dto.requests

data class UserValidationRequest(
    var discordId: String,
    var discordTag: String,
    var username: String,
    var email: String,
)

