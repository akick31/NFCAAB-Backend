package com.nfcaab.backend.controllers

import com.nfcaab.backend.dto.requests.StartRequest
import com.nfcaab.backend.enums.team.Subdivision
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.service.game.GameSpecificationService.GameSort
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import com.nfcaab.backend.service.game.GameSpecificationService
import com.nfcaab.backend.service.game.GameService
import com.nfcaab.backend.service.game.GameLifecycleService
import com.nfcaab.backend.service.game.GameWeekService

class GameControllerTest {
    private lateinit var gameController: GameController
    private val gameService: GameService = mockk()
    private val gameLifecycleService: GameLifecycleService = mockk()
    private val gameWeekService: GameWeekService = mockk()

    @BeforeEach
    fun setup() {
        gameController = GameController(gameService, gameLifecycleService, gameWeekService)
    }

    @Test
    fun `startGame should return created game`() = runBlocking {
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

        coEvery { gameLifecycleService.startSingleGame(any(), any()) } returns game

        val result = gameController.startGame(startRequest, null)

        assertNotNull(result.body)
        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertEquals(1, result.body?.id)
        coVerify { gameLifecycleService.startSingleGame(startRequest, null) }
    }

    @Test
    fun `startWeek should return list of games`() = runBlocking {
        val season = 2024
        val week = 1
        val games = listOf(
            Game().apply { id = 1 },
            Game().apply { id = 2 },
        )

        coEvery { gameWeekService.startWeek(season, week) } returns games

        val result = gameController.startWeek(season, week)

        assertEquals(HttpStatus.CREATED, result.statusCode)
        assertNotNull(result.body)
        assertEquals(2, result.body?.size)
        coVerify { gameWeekService.startWeek(season, week) }
    }

    @Test
    fun `getGameByRequestMessageId should return game`() {
        val requestMessageId = "msg123"
        val game = Game().apply {
            id = 1
            this.requestMessageId = requestMessageId
        }

        every { gameService.getGameByRequestMessageId(requestMessageId) } returns game

        val result = gameController.getGameByRequestMessageId(requestMessageId)

        assertNotNull(result.body)
        assertEquals(1, result.body?.id)
        verify { gameService.getGameByRequestMessageId(requestMessageId) }
    }

    @Test
    fun `getGameByGameId should return game`() {
        val gameId = 1
        val game = Game().apply {
            id = gameId
            homeTeam = "Team A"
            awayTeam = "Team B"
        }

        every { gameService.getGameById(gameId) } returns game

        val result = gameController.getGameByGameId(gameId)

        assertNotNull(result.body)
        assertEquals(gameId, result.body?.id)
        verify { gameService.getGameById(gameId) }
    }

    @Test
    fun `getAllOngoingGames should return list of games`() {
        val games = listOf(
            Game().apply { id = 1 },
            Game().apply { id = 2 },
        )

        every { gameService.getAllOngoingGames() } returns games

        val result = gameController.getAllOngoingGames()

        assertNotNull(result.body)
        assertEquals(2, result.body?.size)
        verify { gameService.getAllOngoingGames() }
    }

    @Test
    fun `getFilteredGames should return paginated games`() {
        val pageable = mockk<Pageable>()
        val mockPage = PageImpl(listOf(Game().apply { id = 1 }))
        every {
            gameService.getFilteredGames(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                pageable,
            )
        } returns mockPage

        val result = gameController.getFilteredGames(
            null,
            null,
            GameSort.CLOSEST_TO_END,
            null,
            null,
            null,
            pageable,
        )

        assertNotNull(result.body)
        assertEquals(1, result.body?.totalElements)
        verify {
            gameService.getFilteredGames(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                pageable,
            )
        }
    }

    @Test
    fun `getGameByPlatformId should return game`() {
        val platformId = 1234UL
        val game = Game().apply {
            id = 1
            gameThreadId = platformId.toString()
        }

        every { gameService.getGameByPlatformId(platformId) } returns game

        val result = gameController.getGameByPlatformId(platformId)

        assertNotNull(result.body)
        assertEquals(1, result.body?.id)
        verify { gameService.getGameByPlatformId(platformId) }
    }

    @Test
    fun `endGameByChannelId should return ended game`() {
        val channelId = 1234UL
        val game = Game().apply {
            id = 1
            gameStatus = Game.GameStatus.FINAL
        }

        every { gameLifecycleService.endSingleGame(channelId) } returns game

        val result = gameController.endGameByChannelId(channelId)

        assertNotNull(result.body)
        assertEquals(Game.GameStatus.FINAL, result.body?.gameStatus)
        verify { gameLifecycleService.endSingleGame(channelId) }
    }

    @Test
    fun `endGameByGameId should return ended game`() {
        val gameId = 1
        val game = Game().apply {
            id = gameId
            gameStatus = Game.GameStatus.FINAL
        }

        every { gameLifecycleService.endSingleGameByGameId(gameId) } returns game

        val result = gameController.endGameByGameId(gameId)

        assertNotNull(result.body)
        assertEquals(Game.GameStatus.FINAL, result.body?.gameStatus)
        verify { gameLifecycleService.endSingleGameByGameId(gameId) }
    }

    @Test
    fun `endAllGames should return list of ended games`() {
        val games = listOf(
            Game().apply {
                id = 1
                gameStatus = Game.GameStatus.FINAL
            },
            Game().apply {
                id = 2
                gameStatus = Game.GameStatus.FINAL
            },
        )

        every { gameLifecycleService.endAllGames() } returns games

        val result = gameController.endAllGames()

        assertNotNull(result.body)
        assertEquals(2, result.body?.size)
        verify { gameLifecycleService.endAllGames() }
    }

    @Test
    fun `restartGame should return restarted game`() = runBlocking {
        val channelId = 1234UL
        val game = Game().apply {
            id = 1
            gameStatus = Game.GameStatus.PREGAME
        }

        coEvery { gameLifecycleService.restartGame(channelId) } returns game

        val result = gameController.restartGame(channelId)

        assertNotNull(result.body)
        assertEquals(Game.GameStatus.PREGAME, result.body?.gameStatus)
        coVerify { gameLifecycleService.restartGame(channelId) }
    }

    @Test
    fun `deleteGame should return true when successful`() {
        val channelId = 1234UL

        every { gameLifecycleService.deleteOngoingGame(channelId) } returns true

        val result = gameController.deleteGame(channelId)

        assertEquals(ResponseEntity.ok(true), result)
        verify { gameLifecycleService.deleteOngoingGame(channelId) }
    }

    @Test
    fun `updateRequestMessageId should return updated game`() {
        val gameId = 1
        val requestMessageId = "msg123"
        val game = Game().apply {
            id = gameId
            this.requestMessageId = requestMessageId
        }

        every { gameService.updateRequestMessageId(gameId, requestMessageId) } returns game

        val result = gameController.updateRequestMessageId(gameId, requestMessageId)

        assertNotNull(result.body)
        assertEquals(requestMessageId, result.body?.requestMessageId)
        verify { gameService.updateRequestMessageId(gameId, requestMessageId) }
    }

    @Test
    fun `updateLastMessageTimestamp should return updated game`() {
        val gameId = 1
        val game = Game().apply {
            id = gameId
        }

        every { gameService.updateLastMessageTimestamp(gameId) } returns game

        val result = gameController.updateLastMessageTimestamp(gameId)

        assertNotNull(result.body)
        assertEquals(gameId, result.body?.id)
        verify { gameService.updateLastMessageTimestamp(gameId) }
    }

    @Test
    fun `markCloseGamePinged should return no content`() {
        val gameId = 1
        every { gameService.markCloseGamePinged(gameId) } just Runs

        val result = gameController.markCloseGamePinged(gameId)

        assertEquals(ResponseEntity.noContent().build<Void>(), result)
        verify { gameService.markCloseGamePinged(gameId) }
    }

    @Test
    fun `markUpsetAlertPinged should return no content`() {
        val gameId = 1
        every { gameService.markUpsetAlertPinged(gameId) } just Runs

        val result = gameController.markUpsetAlertPinged(gameId)

        assertEquals(ResponseEntity.noContent().build<Void>(), result)
        verify { gameService.markUpsetAlertPinged(gameId) }
    }

    @Test
    fun `updateGame should return updated game`() {
        val game = Game().apply {
            id = 1
            homeTeam = "Team A"
            awayTeam = "Team B"
        }

        every { gameService.updateGame(game) } returns game

        val result = gameController.updateGame(game)

        assertNotNull(result.body)
        assertEquals(1, result.body?.id)
        verify { gameService.updateGame(game) }
    }

    @Test
    fun `subCoachIntoGame should return updated game`() {
        val gameId = 1
        val team = "Team A"
        val discordId = "discord123"
        val game = Game().apply {
            id = gameId
            homeTeam = team
        }

        every { gameLifecycleService.subCoachIntoGame(gameId, team, discordId) } returns game

        val result = gameController.subCoachIntoGame(gameId, team, discordId)

        assertNotNull(result.body)
        assertEquals(team, result.body?.homeTeam)
        verify { gameLifecycleService.subCoachIntoGame(gameId, team, discordId) }
    }
}
