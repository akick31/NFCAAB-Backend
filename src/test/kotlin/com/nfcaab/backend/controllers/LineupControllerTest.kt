package com.nfcaab.backend.controllers

import com.nfcaab.backend.dto.requests.LineupSubmissionRequest
import com.nfcaab.backend.model.GameLineup
import com.nfcaab.backend.model.LineupToken
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.nfcaab.backend.service.lineup.LineupService
import com.nfcaab.backend.service.lineup.LineupTokenService

class LineupControllerTest {
    private lateinit var lineupService: LineupService
    private lateinit var lineupTokenService: LineupTokenService
    private lateinit var lineupController: LineupController

    @BeforeEach
    fun setUp() {
        lineupService = mockk()
        lineupTokenService = mockk()
        lineupController = LineupController(lineupService, lineupTokenService)
    }

    @Test
    fun `submitLineup should return list of GameLineup entries`() {
        val request =
            LineupSubmissionRequest(
                token = "test-token",
                batters = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
                pitcher = 10,
            )

        val expectedLineups =
            listOf(
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

    @Test
    fun `generateToken should return a lineup token for the game and team`() {
        val lineupToken =
            LineupToken().apply {
                token = "test-token"
                gameId = 1
                team = "Team A"
            }

        every { lineupTokenService.generateToken(1, "Team A") } returns lineupToken

        val result = lineupController.generateToken(1, "Team A")

        assertEquals(lineupToken, result)
        verify { lineupTokenService.generateToken(1, "Team A") }
    }

    @Test
    fun `resolveToken should return the lineup token when valid`() {
        val lineupToken =
            LineupToken().apply {
                token = "test-token"
                gameId = 1
                team = "Team A"
            }

        every { lineupTokenService.validateToken("test-token") } returns lineupToken

        val result = lineupController.resolveToken("test-token")

        assertEquals(lineupToken, result)
        verify { lineupTokenService.validateToken("test-token") }
    }
}
