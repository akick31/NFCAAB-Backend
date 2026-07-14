package com.nfcaab.backend.security

import com.nfcaab.backend.service.auth.SessionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.SecurityContextHolder
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JwtAuthenticationFilterTest {
    private lateinit var sessionService: SessionService
    private lateinit var filter: JwtAuthenticationFilter
    private val botApiKey = "test-bot-api-key"

    @BeforeEach
    fun setUp() {
        sessionService = mockk()
        filter = JwtAuthenticationFilter(sessionService, botApiKey)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `request with no api key or bearer token stays unauthenticated`() {
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>()
        val chain = mockk<FilterChain>(relaxed = true)
        every { request.getHeader("X-NFCAAB-Api-Key") } returns null
        every { request.getHeader("Authorization") } returns null

        filter.doFilterInternal(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
        verify { chain.doFilter(request, response) }
    }

    @Test
    fun `request with a wrong api key stays unauthenticated`() {
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>()
        val chain = mockk<FilterChain>(relaxed = true)
        every { request.getHeader("X-NFCAAB-Api-Key") } returns "not-the-real-key"
        every { request.getHeader("Authorization") } returns null

        filter.doFilterInternal(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `request with the correct api key is authenticated as the service role`() {
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>()
        val chain = mockk<FilterChain>(relaxed = true)
        every { request.getHeader("X-NFCAAB-Api-Key") } returns botApiKey

        filter.doFilterInternal(request, response, chain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertEquals(true, authentication?.isAuthenticated)
        assertEquals("ROLE_SERVICE", authentication?.authorities?.first()?.authority)
    }

    @Test
    fun `request with a valid non-blacklisted bearer token is authenticated as the user role`() {
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>()
        val chain = mockk<FilterChain>(relaxed = true)
        every { request.getHeader("X-NFCAAB-Api-Key") } returns null
        every { request.getHeader("Authorization") } returns "Bearer valid-token"
        every { sessionService.validateToken("valid-token") } returns true
        every { sessionService.isSessionBlacklisted("valid-token") } returns false
        every { sessionService.extractUserIdFromToken("valid-token") } returns 42L

        filter.doFilterInternal(request, response, chain)

        val authentication = SecurityContextHolder.getContext().authentication
        assertEquals(true, authentication?.isAuthenticated)
        assertEquals("42", authentication?.principal)
        assertEquals("ROLE_USER", authentication?.authorities?.first()?.authority)
    }

    @Test
    fun `request with a blacklisted bearer token stays unauthenticated`() {
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>()
        val chain = mockk<FilterChain>(relaxed = true)
        every { request.getHeader("X-NFCAAB-Api-Key") } returns null
        every { request.getHeader("Authorization") } returns "Bearer blacklisted-token"
        every { sessionService.validateToken("blacklisted-token") } returns true
        every { sessionService.isSessionBlacklisted("blacklisted-token") } returns true

        filter.doFilterInternal(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `request with an expired or invalid bearer token stays unauthenticated`() {
        val request = mockk<HttpServletRequest>()
        val response = mockk<HttpServletResponse>()
        val chain = mockk<FilterChain>(relaxed = true)
        every { request.getHeader("X-NFCAAB-Api-Key") } returns null
        every { request.getHeader("Authorization") } returns "Bearer expired-token"
        every { sessionService.validateToken("expired-token") } returns false

        filter.doFilterInternal(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }
}
