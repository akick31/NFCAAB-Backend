package com.nfcaab.backend.service.scheduler

import com.nfcaab.backend.service.auth.SessionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CleanupExpiredTokensTest {
    private lateinit var sessionService: SessionService
    private lateinit var cleanupExpiredTokens: CleanupExpiredTokens

    @BeforeEach
    fun setUp() {
        sessionService = mockk()
        cleanupExpiredTokens = CleanupExpiredTokens(sessionService)
    }

    @Test
    fun `cleanUpExpiredTokens should call sessionService clearExpiredTokens`() {
        every { sessionService.clearExpiredTokens() } returns Unit

        cleanupExpiredTokens.cleanUpExpiredTokens()

        verify { sessionService.clearExpiredTokens() }
    }
}

