package com.nfcaab.backend.controllers

import com.nfcaab.backend.dto.requests.BatterSubmission
import com.nfcaab.backend.dto.requests.LineupSubmissionRequest
import com.nfcaab.backend.model.GameLineup
import com.nfcaab.backend.model.LineupToken
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.model.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import com.nfcaab.backend.service.lineup.LineupService
import com.nfcaab.backend.service.lineup.LineupTokenService
import com.nfcaab.backend.service.user.UserService

class LineupControllerTest {
    private lateinit var lineupService: LineupService
    private lateinit var lineupTokenService: LineupTokenService
    private lateinit var userService: UserService
    private lateinit var lineupController: LineupController

    @BeforeEach
    fun setUp() {
        lineupService = mockk()
        lineupTokenService = mockk()
        userService = mockk()
        lineupController = LineupController(lineupService, lineupTokenService, userService)
    }

    private fun fullLineupBatters() =
        listOf(
            BatterSubmission(1, Player.Position.CATCHER),
            BatterSubmission(2, Player.Position.FIRST_BASE),
            BatterSubmission(3, Player.Position.SECOND_BASE),
            BatterSubmission(4, Player.Position.THIRD_BASE),
            BatterSubmission(5, Player.Position.SHORTSTOP),
            BatterSubmission(6, Player.Position.LEFT_FIELD),
            BatterSubmission(7, Player.Position.CENTER_FIELD),
            BatterSubmission(8, Player.Position.RIGHT_FIELD),
            BatterSubmission(9, Player.Position.DESIGNATED_HITTER),
        )

    @Test
    fun `submitLineup should return list of GameLineup entries`() {
        val request =
            LineupSubmissionRequest(
                token = "test-token",
                batters = fullLineupBatters(),
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

    @Test
    fun `getMyActiveLineupTokens should return active tokens for the caller's team`() {
        val authentication = mockk<Authentication>()
        val user = User().apply { team = "Team A" }
        val tokens = listOf(LineupToken().apply { token = "test-token" })

        every { authentication.name } returns "7"
        every { userService.getUserById(7L) } returns user
        every { lineupTokenService.getActiveTokensForTeam("Team A") } returns tokens

        val result = lineupController.getMyActiveLineupTokens(authentication)

        assertEquals(tokens, result)
        verify { lineupTokenService.getActiveTokensForTeam("Team A") }
    }

    @Test
    fun `getMyActiveLineupTokens should return empty list when the caller has no team`() {
        val authentication = mockk<Authentication>()
        val user = User().apply { team = null }

        every { authentication.name } returns "7"
        every { userService.getUserById(7L) } returns user

        val result = lineupController.getMyActiveLineupTokens(authentication)

        assertEquals(emptyList<LineupToken>(), result)
    }
}
