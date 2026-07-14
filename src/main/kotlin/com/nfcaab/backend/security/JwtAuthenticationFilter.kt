package com.nfcaab.backend.security

import com.nfcaab.backend.service.auth.SessionService
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private const val API_KEY_HEADER = "X-NFCAAB-Api-Key"

class JwtAuthenticationFilter(
    private val sessionService: SessionService,
    private val botApiKey: String,
) : OncePerRequestFilter() {
    public override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val apiKey = request.getHeader(API_KEY_HEADER)
        if (apiKey != null && apiKey == botApiKey) {
            setAuthentication("nfcaab-umpire", "ROLE_SERVICE")
            filterChain.doFilter(request, response)
            return
        }

        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.removePrefix("Bearer ")
            if (sessionService.validateToken(token) && !sessionService.isSessionBlacklisted(token)) {
                val userId = sessionService.extractUserIdFromToken(token)
                setAuthentication(userId.toString(), "ROLE_USER")
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun setAuthentication(
        principal: String,
        role: String,
    ) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, listOf(SimpleGrantedAuthority(role)))
    }
}
