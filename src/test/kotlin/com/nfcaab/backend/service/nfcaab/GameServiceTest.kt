package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.dto.requests.StartRequest
import com.nfcaab.backend.enums.team.Subdivision
import com.nfcaab.backend.enums.team.TeamSide
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.BaseCondition
import com.nfcaab.backend.model.Game.GameStatus
import com.nfcaab.backend.model.Game.GameType
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.model.Team
import com.nfcaab.backend.model.User
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.repositories.GameRepository
import com.nfcaab.backend.service.discord.DiscordService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification

class GameServiceTest {
    private lateinit var gameRepository: GameRepository
    private lateinit var atBatRepository: AtBatRepository
    private lateinit var teamService: TeamService
    private lateinit var discordService: DiscordService
    private lateinit var userService: UserService
    private lateinit var gameStatsService: GameStatsService
    private lateinit var seasonService: SeasonService
    private lateinit var scheduleService: ScheduleService
    private lateinit var gameSpecificationService: GameSpecificationService
    private lateinit var lineupService: LineupService
    private lateinit var gameService: GameService

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        atBatRepository = mockk()
        teamService = mockk()
        discordService = mockk()
        userService = mockk()
        gameStatsService = mockk()
        seasonService = mockk()
        scheduleService = mockk()
        gameSpecificationService = mockk()
        lineupService = mockk()
        gameService = GameService(
            gameRepository,
            atBatRepository,
            teamService,
            discordService,
            userService,
            gameStatsService,
            seasonService,
            scheduleService,
            gameSpecificationService,
            lineupService
        )
    }

    @Test
    fun `getDifference should calculate correct difference`() {
        val difference1 = gameService.getDifference(50, 30)
        assertEquals(20, difference1)

        val difference2 = gameService.getDifference(30, 50)
        assertEquals(20, difference2)

        val difference3 = gameService.getDifference(950, 50)
        assertEquals(100, difference3)
    }

    @Test
    fun `getBaseCondition should return correct condition`() {
        val player1 = Player().apply { uniformNumber = 1 }
        val player2 = Player().apply { uniformNumber = 2 }
        val player3 = Player().apply { uniformNumber = 3 }

        val condition1 = gameService.getBaseCondition(null, null, null)
        assertEquals(BaseCondition.EMPTY, condition1)

        val condition2 = gameService.getBaseCondition(player1, null, null)
        assertEquals(BaseCondition.FIRST, condition2)

        val condition3 = gameService.getBaseCondition(null, player2, null)
        assertEquals(BaseCondition.SECOND, condition3)

        val condition4 = gameService.getBaseCondition(null, null, player3)
        assertEquals(BaseCondition.THIRD, condition4)

        val condition5 = gameService.getBaseCondition(player1, player2, null)
        assertEquals(BaseCondition.FIRST_SECOND, condition5)

        val condition6 = gameService.getBaseCondition(player1, null, player3)
        assertEquals(BaseCondition.FIRST_THIRD, condition6)

        val condition7 = gameService.getBaseCondition(null, player2, player3)
        assertEquals(BaseCondition.SECOND_THIRD, condition7)

        val condition8 = gameService.getBaseCondition(player1, player2, player3)
        assertEquals(BaseCondition.BASED_LOADED, condition8)
    }

    @Test
    fun `getGameById should return game when found`() {
        val gameId = 1
        val game = Game().apply {
            id = gameId
            homeTeam = "Home Team"
            awayTeam = "Away Team"
        }

        every { gameRepository.getGameById(gameId) } returns game

        val result = gameService.getGameById(gameId)

        assertEquals(game, result)
        verify { gameRepository.getGameById(gameId) }
    }

    @Test
    fun `getGameByRequestMessageId should return game when found`() {
        val requestMessageId = "123456"
        val game = Game()
        game.id = 1
        game.requestMessageId = requestMessageId

        every { gameRepository.getGameByRequestMessageId(requestMessageId) } returns game

        val result = gameService.getGameByRequestMessageId(requestMessageId)

        assertEquals(game, result)
        verify { gameRepository.getGameByRequestMessageId(requestMessageId) }
    }

    @Test
    fun `getAllGames should return list of games`() {
        val games = listOf(
            Game().apply { id = 1 },
            Game().apply { id = 2 }
        )

        every { gameRepository.getAllGames() } returns games

        val result = gameService.getAllGames()

        assertEquals(games, result)
        verify { gameRepository.getAllGames() }
    }

    @Test
    fun `findExpiredTimers should return games with expired timers`() {
        val games = listOf(
            Game().apply { id = 1 }
        )

        every { gameRepository.findExpiredTimers() } returns games

        val result = gameService.findExpiredTimers()

        assertEquals(games, result)
        verify { gameRepository.findExpiredTimers() }
    }

    @Test
    fun `findGamesToWarn should return games to warn`() {
        val games = listOf(
            Game().apply { id = 1 }
        )

        every { gameRepository.findGamesToWarn() } returns games

        val result = gameService.findGamesToWarn()

        assertEquals(games, result)
        verify { gameRepository.findGamesToWarn() }
    }

    @Test
    fun `updateGameAsWarned should call repository`() {
        val gameId = 1

        every { gameRepository.updateGameAsWarned(gameId) } returns Unit

        gameService.updateGameAsWarned(gameId)

        verify { gameRepository.updateGameAsWarned(gameId) }
    }

    @Test
    fun `markCloseGamePinged should call repository`() {
        val gameId = 1

        every { gameRepository.markCloseGamePinged(gameId) } returns Unit

        gameService.markCloseGamePinged(gameId)

        verify { gameRepository.markCloseGamePinged(gameId) }
    }

    @Test
    fun `markUpsetAlertPinged should call repository`() {
        val gameId = 1

        every { gameRepository.markUpsetAlertPinged(gameId) } returns Unit

        gameService.markUpsetAlertPinged(gameId)

        verify { gameRepository.markUpsetAlertPinged(gameId) }
    }

    @Test
    fun `getFilteredGames should return filtered games`() {
        val filters = emptyList<GameSpecificationService.GameFilter>()
        val category: GameSpecificationService.GameCategory? = null
        val conference: String? = null
        val season: Int? = null
        val week: Int? = null
        val sort = GameSpecificationService.GameSort.CLOSEST_TO_END
        val pageable: Pageable = mockk()
        val games = listOf(Game().apply { id = 1 })
        val page = PageImpl(games)

        every { pageable.pageNumber } returns 0
        every { pageable.pageSize } returns 25
        every {
            gameSpecificationService.createSpecification(any(), any(), any(), any(), any())
        } returns mockk<Specification<Game>>()
        every { gameSpecificationService.createSort(any()) } returns emptyList()
        every { gameRepository.findAll(any<Specification<Game>>(), any<Pageable>()) } returns page

        val result = gameService.getFilteredGames(filters, category, conference, season, week, sort, pageable)

        assertEquals(page, result)
        verify { gameRepository.findAll(any<Specification<Game>>(), any<Pageable>()) }
    }

    @Test
    fun `saveGame should save and return game`() {
        val game = Game().apply {
            id = 1
            homeTeam = "Home Team"
        }

        every { gameRepository.save(game) } returns game

        val result = gameService.saveGame(game)

        assertEquals(game, result)
        verify { gameRepository.save(game) }
    }

    @Test
    fun `getGamesWithTeams should return games for teams`() {
        val team1 = Team().apply { name = "Team 1" }
        val team2 = Team().apply { name = "Team 2" }
        val teams = listOf(team1, team2)
        val season = 2024
        val week = 1
        val game1 = Game().apply {
            id = 1
            homeTeam = "Team 1"
        }
        val game2 = Game().apply {
            id = 2
            homeTeam = "Team 2"
        }

        every { gameRepository.getGamesByTeamSeasonAndWeek("Team 1", season, week) } returns game1
        every { gameRepository.getGamesByTeamSeasonAndWeek("Team 2", season, week) } returns game2

        val result = gameService.getGamesWithTeams(teams, season, week)

        assertEquals(2, result.size)
        verify { gameRepository.getGamesByTeamSeasonAndWeek("Team 1", season, week) }
        verify { gameRepository.getGamesByTeamSeasonAndWeek("Team 2", season, week) }
    }
}

