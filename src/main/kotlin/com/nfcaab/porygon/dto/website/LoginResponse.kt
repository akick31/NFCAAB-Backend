package com.nfcaab.porygon.dto.website

import com.nfcaab.porygon.model.User

data class LoginResponse(
    val token: String,
    val userId: Long,
    val role: User.Role,
)

