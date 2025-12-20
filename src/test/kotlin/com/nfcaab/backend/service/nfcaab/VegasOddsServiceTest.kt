package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.GameStats
import com.nfcaab.backend.model.Team
import com.nfcaab.backend.repositories.GameRepository
import com.nfcaab.backend.repositories.GameStatsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VegasOddsServiceTest {
    private lateinit var gameRepository: GameRepository
    private lateinit var gameStatsRepository: GameStatsRepository
    private lateinit var teamService: TeamService
    private lateinit var vegasOddsService: VegasOddsService

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        gameStatsRepository = mockk()
        teamService = mockk()
        vegasOddsService = VegasOddsService(gameRepository, gameStatsRepository)
    }

    @Test
    fun `calculateVegasOdds should calculate correct spreads`() {
        val homeTeam = Team().apply {
            name = "Home Team"
            currentElo = 1600.0
        }
        val awayTeam = Team().apply {
            name = "Away Team"
            currentElo = 1400.0
        }

        val result = vegasOddsService.calculateVegasOdds(homeTeam, awayTeam)

        assertEquals(homeTeam.name, result.homeTeam)
        assertEquals(awayTeam.name, result.awayTeam)
        assertEquals(homeTeam.currentElo, result.homeElo)
        assertEquals(awayTeam.currentElo, result.awayElo)
        assertEquals(-result.homeSpread, result.awaySpread)
    }

    @Test
    fun `getVegasOddsByTeams should return odds when teams exist`() {
        val homeTeamName = "Home Team"
        val awayTeamName = "Away Team"
        val homeTeam = Team().apply {
            name = homeTeamName
            currentElo = 1600.0
        }
        val awayTeam = Team().apply {
            name = awayTeamName
            currentElo = 1400.0
        }

        every { teamService.getTeamByName(homeTeamName) } returns homeTeam
        every { teamService.getTeamByName(awayTeamName) } returns awayTeam

        val result = vegasOddsService.getVegasOddsByTeams(homeTeamName, awayTeamName, teamService)

        assertEquals(200, result.statusCodeValue)
        assertEquals(homeTeamName, result.body?.homeTeam)
        assertEquals(awayTeamName, result.body?.awayTeam)
        verify { teamService.getTeamByName(homeTeamName) }
        verify { teamService.getTeamByName(awayTeamName) }
    }

    @Test
    fun `getVegasOddsByElo should calculate spreads from ELO`() {
        val homeElo = 1600.0
        val awayElo = 1400.0

        val result = vegasOddsService.getVegasOddsByElo(homeElo, awayElo)

        assertEquals(200, result.statusCodeValue)
        assertEquals(homeElo, result.body?.homeElo ?: 0.0)
        assertEquals(awayElo, result.body?.awayElo ?: 0.0)
        result.body?.let {
            assertEquals(-it.homeSpread, it.awaySpread)
        }
    }

    @Test
    fun `updateSpreadsForSeasonAndWeek should update spreads for all games`() {
        val season = 2024
        val week = 1
        val games = listOf(
            Game().apply {
                id = 1
                homeTeam = "Home Team"
                awayTeam = "Away Team"
            }
        )
        val gameStats = listOf(
            GameStats().apply {
                gameId = 1
                team = "Home Team"
            },
            GameStats().apply {
                gameId = 1
                team = "Away Team"
            }
        )

        every { gameRepository.getGamesBySeasonAndWeek(season, week) } returns games
        every { gameStatsRepository.findBySeasonOrderByGameIdAsc(season) } returns gameStats

        val result = vegasOddsService.updateSpreadsForSeasonAndWeek(season, week)

        assertEquals(200, result.statusCodeValue)
        assertEquals(season, result.body?.season)
        assertEquals(week, result.body?.week)
        verify { gameRepository.getGamesBySeasonAndWeek(season, week) }
        verify { gameStatsRepository.findBySeasonOrderByGameIdAsc(season) }
    }
}

