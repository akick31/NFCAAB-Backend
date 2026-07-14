package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.dto.requests.LineupSubmissionRequest
import com.nfcaab.backend.model.GameLineup
import com.nfcaab.backend.model.LineupToken
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.repositories.GameLineupRepository
import com.nfcaab.backend.repositories.GameRepository
import com.nfcaab.backend.util.InvalidLineupException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LineupServiceTest {
    private lateinit var gameLineupRepository: GameLineupRepository
    private lateinit var playerService: PlayerService
    private lateinit var gameRepository: GameRepository
    private lateinit var lineupTokenService: LineupTokenService
    private lateinit var lineupService: LineupService

    @BeforeEach
    fun setUp() {
        gameLineupRepository = mockk()
        playerService = mockk()
        gameRepository = mockk()
        lineupTokenService = mockk()
        lineupService = LineupService(gameLineupRepository, playerService, gameRepository, lineupTokenService)
    }

    private fun tokenFor(
        gameId: Int,
        team: String,
    ) = LineupToken().apply {
        this.token = "test-token"
        this.gameId = gameId
        this.team = team
    }

    @Test
    fun `saveLineup should save valid lineup`() {
        val request =
            LineupSubmissionRequest(
                token = "test-token",
                batters = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
                pitcher = 10,
            )
        val lineupToken = tokenFor(1, "Team A")

        val player = Player().apply {
            firstName = "John"
            lastName = "Doe"
            uniformNumber = 1
            currentTeam = "Team A"
        }

        every { lineupTokenService.validateToken("test-token") } returns lineupToken
        every { gameLineupRepository.getAllLineupsByGameIdAndTeam(1, "Team A") } returns emptyList()
        every { gameLineupRepository.deleteAll(any()) } returns Unit
        every { playerService.getPlayerByNumberAndTeam(any(), any()) } returns player
        every { gameLineupRepository.save(any()) } returns mockk<GameLineup>()
        every { lineupTokenService.consumeToken(lineupToken) } returns lineupToken

        val result = lineupService.saveLineup(request)

        assertEquals(10, result.size) // 9 batters + 1 pitcher
        verify { lineupTokenService.validateToken("test-token") }
        verify { lineupTokenService.consumeToken(lineupToken) }
        verify { gameLineupRepository.save(any()) }
    }

    @Test
    fun `saveLineup should throw exception when lineup has wrong number of batters`() {
        val request =
            LineupSubmissionRequest(
                token = "test-token",
                batters = listOf(1, 2, 3, 4, 5, 6, 7, 8), // Only 8 batters
                pitcher = 10,
            )
        val lineupToken = tokenFor(1, "Team A")

        every { lineupTokenService.validateToken("test-token") } returns lineupToken

        assertThrows<InvalidLineupException> {
            lineupService.saveLineup(request)
        }
    }

    @Test
    fun `saveLineup should throw exception when pitcher is also in the batting lineup`() {
        val request =
            LineupSubmissionRequest(
                token = "test-token",
                batters = listOf(1, 2, 3, 4, 5, 6, 7, 8, 10),
                pitcher = 10,
            )
        val lineupToken = tokenFor(1, "Team A")

        every { lineupTokenService.validateToken("test-token") } returns lineupToken

        assertThrows<InvalidLineupException> {
            lineupService.saveLineup(request)
        }
    }

    @Test
    fun `saveLineup should reject a starting pitcher who has not rested enough games`() {
        val request =
            LineupSubmissionRequest(
                token = "test-token",
                batters = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
                pitcher = 10,
            )
        val lineupToken = tokenFor(5, "Team A")

        val batter = Player().apply {
            firstName = "John"
            lastName = "Doe"
            currentTeam = "Team A"
        }
        val restingStarter = Player().apply {
            firstName = "Jane"
            lastName = "Smith"
            currentTeam = "Team A"
            pitcherRole = Player.PitcherRole.STARTER
            lastStartGameId = 3
        }

        every { lineupTokenService.validateToken("test-token") } returns lineupToken
        every { playerService.getPlayerByNumberAndTeam("Team A", 10) } returns restingStarter
        every { playerService.getPlayerByNumberAndTeam("Team A", match { it != 10 }) } returns batter
        every { gameRepository.countFinishedGamesByTeamSinceGameId("Team A", 3) } returns 2

        assertThrows<InvalidLineupException> {
            lineupService.saveLineup(request)
        }
    }
}
