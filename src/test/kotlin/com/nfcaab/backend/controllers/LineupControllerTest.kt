package com.nfcaab.backend.controllers

import com.nfcaab.backend.dto.requests.LineupSubmissionRequest
import com.nfcaab.backend.model.GameLineup
import com.nfcaab.backend.service.nfcaab.LineupService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LineupControllerTest {
    private lateinit var lineupService: LineupService
    private lateinit var lineupController: LineupController

    @BeforeEach
    fun setUp() {
        lineupService = mockk()
        lineupController = LineupController(lineupService)
    }

    @Test
    fun `submitLineup should return list of GameLineup entries`() {
        val request = LineupSubmissionRequest(
            gameId = 1,
            team = "Team A",
            batters = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
            pitcher = 10,
        )

        val expectedLineups = listOf(
            GameLineup().apply {
                id = 1
                gameId = "1"
                team = "Team A"
            },
            GameLineup().apply {
                id = 2
                gameId = "1"
                team = "Team A"
            },
        )

        every { lineupService.saveLineup(request) } returns expectedLineups

        val result = lineupController.submitLineup(request)

        assertEquals(expectedLineups, result)
        verify { lineupService.saveLineup(request) }
    }
}

