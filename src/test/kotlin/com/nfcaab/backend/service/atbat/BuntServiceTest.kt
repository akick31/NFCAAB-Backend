package com.nfcaab.backend.service.atbat

import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Game
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BuntServiceTest {
    private lateinit var atBatResolutionService: AtBatResolutionService
    private lateinit var buntService: BuntService

    @BeforeEach
    fun setUp() {
        atBatResolutionService = mockk()
        buntService = BuntService(atBatResolutionService)
    }

    @Test
    fun `resolveBunt should delegate to AtBatResolutionService resolveSwing`() {
        val atBat = AtBat().apply { id = 9 }
        val game = Game().apply { id = 1 }
        val resolvedAtBat = AtBat().apply { id = 9; atBatFinished = true }

        every {
            atBatResolutionService.resolveSwing(atBat, game, SubmissionType.BUNT, 55, "42")
        } returns resolvedAtBat

        val result = buntService.resolveBunt(atBat, game, SubmissionType.BUNT, 55, "42")

        assertEquals(resolvedAtBat, result)
        verify { atBatResolutionService.resolveSwing(atBat, game, SubmissionType.BUNT, 55, "42") }
    }
}
