package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.enums.team.Subdivision
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.GameStats
import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.repositories.GameStatsRepository
import com.nfcaab.backend.repositories.GameRepository
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.repositories.TeamRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameStatsServiceTest {
    private lateinit var gameStatsService: GameStatsService
    private val gameStatsRepository: GameStatsRepository = mockk()
    private val gameRepository: GameRepository = mockk()
    private val atBatRepository: AtBatRepository = mockk()
    private val teamRepository: TeamRepository = mockk()
    private val seasonStatsService: SeasonStatsService = mockk()
    private val conferenceStatsService: ConferenceStatsService = mockk()
    private val leagueStatsService: LeagueStatsService = mockk()

    @BeforeEach
    fun setup() {
        gameStatsService = GameStatsService(
            gameStatsRepository,
            gameRepository,
            atBatRepository,
            teamRepository,
            seasonStatsService,
            conferenceStatsService,
            leagueStatsService,
        )
    }

    @Test
    fun `test createGameStats`() {
        val game = Game().apply {
            id = 1
            homeTeam = "Team A"
            awayTeam = "Team B"
            season = 1
            week = 1
            subdivision = Subdivision.NFCAAB
            gameStatus = Game.GameStatus.IN_PROGRESS
            gameType = Game.GameType.SCRIMMAGE
        }

        every { teamRepository.getTeamByName("Team A") } returns mockk()
        every { teamRepository.getTeamByName("Team B") } returns mockk()
        every { gameStatsRepository.save(any()) } returns mockk()

        val result = gameStatsService.createGameStats(game)

        assertEquals(2, result.size)
        verify { gameStatsRepository.save(any()) }
    }

    @Test
    fun `test updateGameStats calculates batting stats correctly`() {
        val game = Game().apply {
            id = 1
            homeTeam = "Team A"
            awayTeam = "Team B"
            homeScore = 5
            awayScore = 3
        }

        val atBats = listOf(
            AtBat().apply {
                battingTeam = "Team A"
                actualResult = Game.ActualResult.SINGLE
                runsScored = 1
            },
            AtBat().apply {
                battingTeam = "Team A"
                actualResult = Game.ActualResult.HOME_RUN
                runsScored = 1
            },
        )

        val gameStats = GameStats().apply {
            gameId = 1
            team = "Team A"
        }

        every { gameStatsRepository.getGameStatsByIdAndTeam(1, "Team A") } returns gameStats
        every { gameStatsRepository.getGameStatsByIdAndTeam(1, "Team B") } returns GameStats().apply {
            gameId = 1
            team = "Team B"
        }
        every { gameStatsRepository.save(any()) } returns mockk()

        val result = gameStatsService.updateGameStats(game, atBats)

        assertEquals(2, result.size)
        verify { gameStatsRepository.save(any()) }
    }
}

