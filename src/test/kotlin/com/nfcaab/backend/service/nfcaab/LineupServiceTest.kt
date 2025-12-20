package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.dto.requests.LineupSubmissionRequest
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.GameLineup
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.repositories.GameLineupRepository
import com.nfcaab.backend.repositories.GameRepository
import com.nfcaab.backend.util.InvalidLineupException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LineupServiceTest {
    private lateinit var gameLineupRepository: GameLineupRepository
    private lateinit var playerService: PlayerService
    private lateinit var gameRepository: GameRepository
    private lateinit var lineupService: LineupService

    @BeforeEach
    fun setUp() {
        gameLineupRepository = mockk()
        playerService = mockk()
        gameRepository = mockk()
        lineupService = LineupService(gameLineupRepository, playerService, gameRepository)
    }

    @Test
    fun `saveLineup should save valid lineup`() {
        val request = LineupSubmissionRequest(
            gameId = 1,
            team = "Team A",
            batters = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
            pitcher = 10,
        )

        val game = Game().apply {
            id = 1
            homeTeam = "Team A"
            awayTeam = "Team B"
        }

        val player = Player().apply {
            firstName = "John"
            lastName = "Doe"
            uniformNumber = 1
            currentTeam = "Team A"
        }

        every { gameRepository.getGameById(request.gameId) } returns game
        every { gameLineupRepository.getAllLineupsByGameIdAndTeam(request.gameId, request.team) } returns emptyList()
        every { gameLineupRepository.deleteAll(any()) } returns Unit
        every { playerService.getPlayerByNumberAndTeam(any(), any()) } returns player
        every { gameLineupRepository.save(any()) } returns mockk<GameLineup>()

        val result = lineupService.saveLineup(request)

        assertEquals(10, result.size) // 9 batters + 1 pitcher
        verify { gameRepository.getGameById(request.gameId) }
        verify { gameLineupRepository.save(any()) }
    }

    @Test
    fun `saveLineup should throw exception when team is not part of game`() {
        val request = LineupSubmissionRequest(
            gameId = 1,
            team = "Team C",
            batters = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9),
            pitcher = 10,
        )

        val game = Game().apply {
            id = 1
            homeTeam = "Team A"
            awayTeam = "Team B"
        }

        every { gameRepository.getGameById(request.gameId) } returns game

        assertThrows<InvalidLineupException> {
            lineupService.saveLineup(request)
        }
    }

    @Test
    fun `saveLineup should throw exception when lineup has wrong number of batters`() {
        val request = LineupSubmissionRequest(
            gameId = 1,
            team = "Team A",
            batters = listOf(1, 2, 3, 4, 5, 6, 7, 8), // Only 8 batters
            pitcher = 10,
        )

        val game = Game().apply {
            id = 1
            homeTeam = "Team A"
            awayTeam = "Team B"
        }

        every { gameRepository.getGameById(request.gameId) } returns game

        assertThrows<InvalidLineupException> {
            lineupService.saveLineup(request)
        }
    }
}

