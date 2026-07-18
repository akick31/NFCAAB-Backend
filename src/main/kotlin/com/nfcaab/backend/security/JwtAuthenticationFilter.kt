package com.nfcaab.backend.security

import com.nfcaab.backend.service.auth.SessionService
import com.nfcaab.backend.service.user.UserService
import com.nfcaab.backend.util.Logger
import com.nfcaab.backend.util.UserNotFoundException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private const val API_KEY_HEADER = "X-NFCAAB-Api-Key"
const val AUTH_COOKIE_NAME = "nfcaab_session"

class JwtAuthenticationFilter(
    private val sessionService: SessionService,
    private val userService: UserService,
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

        val token = resolveToken(request)
        if (token != null && sessionService.validateToken(token) && !sessionService.isSessionBlacklisted(token)) {
            val userId = sessionService.extractUserIdFromToken(token)
            try {
                val role = userService.getUserById(userId).role
                setAuthentication(userId.toString(), "ROLE_${role.name}")
            } catch (e: UserNotFoundException) {
                Logger.warn("Valid token for deleted user $userId, treating as unauthenticated")
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.removePrefix("Bearer ")
        }
        return request.cookies?.firstOrNull { it.name == AUTH_COOKIE_NAME }?.value
    }

    private fun setAuthentication(
        principal: String,
        role: String,
    ) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, listOf(SimpleGrantedAuthority(role)))
    }
}
