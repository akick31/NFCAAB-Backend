package com.nfcaab.backend.dto.website

import com.nfcaab.backend.model.User

data class LoginResponse(
    val userId: Long,
    val role: User.Role,
)

