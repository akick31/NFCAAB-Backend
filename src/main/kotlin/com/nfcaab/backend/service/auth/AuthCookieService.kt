package com.nfcaab.backend.service.auth

import com.nfcaab.backend.security.AUTH_COOKIE_NAME
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component
class AuthCookieService(
    private val sessionService: SessionService,
    @Value("\${server.servlet.context-path}") private val contextPath: String,
    @Value("\${cookie.domain:}") private val cookieDomain: String,
    @Value("\${cookie.secure:true}") private val cookieSecure: Boolean,
) {
    fun readAuthCookie(request: HttpServletRequest): String? =
        request.cookies?.firstOrNull { it.name == AUTH_COOKIE_NAME }?.value

    fun issueAuthCookie(
        response: HttpServletResponse,
        userId: Long,
    ) {
        val token = sessionService.generateToken(userId)
        val cookie =
            ResponseCookie.from(AUTH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path(contextPath)
                .maxAge(Duration.ofHours(1))
                .sameSite("Lax")
                .apply { if (cookieDomain.isNotBlank()) domain(cookieDomain) }
                .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    fun clearAuthCookie(response: HttpServletResponse) {
        val cookie =
            ResponseCookie.from(AUTH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path(contextPath)
                .maxAge(Duration.ZERO)
                .sameSite("Lax")
                .apply { if (cookieDomain.isNotBlank()) domain(cookieDomain) }
                .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }
}
