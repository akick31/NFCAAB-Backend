package com.nfcaab.backend.service.auth

import com.nfcaab.backend.repositories.SessionRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date

class SessionServiceTest {
    private lateinit var sessionRepository: SessionRepository
    private lateinit var sessionService: SessionService
    private val secretKey = "test-secret-key-for-jwt-token-generation-and-validation-purposes-only"

    @BeforeEach
    fun setUp() {
        sessionRepository = mockk()
        sessionService = SessionService(sessionRepository, secretKey)
    }

    @Test
    fun `generateToken should create valid token`() {
        val userId = 123L
        val token = sessionService.generateToken(userId)

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
    }

    @Test
    fun `validateToken should return true for valid token`() {
        val userId = 123L
        val token = sessionService.generateToken(userId)

        val isValid = sessionService.validateToken(token)

        assertTrue(isValid)
    }

    @Test
    fun `validateToken should return false for invalid token`() {
        val invalidToken = "invalid.token.here"

        val isValid = sessionService.validateToken(invalidToken)

        assertFalse(isValid)
    }

    @Test
    fun `blacklistUserSession should call repository`() {
        val userId = 123L
        val token = sessionService.generateToken(userId)
        val expirationDate = Date(System.currentTimeMillis() + 3600 * 1000)

        every { sessionRepository.blacklistUserSession(any(), any(), any()) } returns Unit

        sessionService.blacklistUserSession(token)

        verify { sessionRepository.blacklistUserSession(any(), any(), any()) }
    }

    @Test
    fun `isSessionBlacklisted should call repository`() {
        val token = "test.token"
        val isBlacklisted = false

        every { sessionRepository.isSessionBlacklisted(token) } returns isBlacklisted

        val result = sessionService.isSessionBlacklisted(token)

        assertEquals(isBlacklisted, result)
        verify { sessionRepository.isSessionBlacklisted(token) }
    }

    @Test
    fun `clearExpiredTokens should call repository`() {
        every { sessionRepository.clearExpiredTokens() } returns Unit

        sessionService.clearExpiredTokens()

        verify { sessionRepository.clearExpiredTokens() }
    }
}

