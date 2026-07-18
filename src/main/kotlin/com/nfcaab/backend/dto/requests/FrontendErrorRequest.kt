package com.nfcaab.backend.dto.requests

data class FrontendErrorRequest(
    var message: String,
    var stack: String?,
    var url: String?,
)
