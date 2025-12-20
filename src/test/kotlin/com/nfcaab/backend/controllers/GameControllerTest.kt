package com.nfcaab.backend.controllers

import com.nfcaab.backend.enums.team.Subdivision
import com.nfcaab.backend.dto.requests.StartRequest
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.service.nfcaab.GameService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

class GameControllerTest {
    private lateinit var gameController: GameController
    private val gameService: GameService = mockk()

    @BeforeEach
    fun setup() {
        gameController = GameController(gameService)
    }

    @Test
    fun `test startGame`() = runBlocking {
        val startRequest = StartRequest(
            subdivision = Subdivision.NFCAAB,
            homeTeam = "Team A",
            awayTeam = "Team B",
            gameType = Game.GameType.SCRIMMAGE,
            seriesGameNumber = 1,
        )
        val game = Game().apply {
            id = 1
            homeTeam = "Team A"
            awayTeam = "Team B"
        }

        coEvery { gameService.startSingleGame(any(), any()) } returns game

        val result = gameController.startGame(startRequest, null)

        assertNotNull(result.body)
        assertEquals(1, result.body?.id)
    }

    @Test
    fun `test endAllGames`() {
        val games = listOf(
            Game().apply { id = 1 },
            Game().apply { id = 2 },
        )

        every { gameService.endAllGames() } returns games

        val result = gameController.endAllGames()

        assertNotNull(result.body)
        assertEquals(2, result.body?.size)
    }

    @Test
    fun `test getGameByGameId`() {
        val game = Game().apply {
            id = 1
            homeTeam = "Team A"
            awayTeam = "Team B"
        }

        every { gameService.getGameById(1) } returns game

        val result = gameController.getGameByGameId(1)

        assertNotNull(result.body)
        assertEquals(1, result.body?.id)
    }
}

