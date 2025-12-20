package com.nfcaab.backend.dto.response

data class UserValidationResponse(
    var discordIdExists: Boolean,
    var discordTagExists: Boolean,
    var usernameExists: Boolean,
    var emailExists: Boolean,
)

