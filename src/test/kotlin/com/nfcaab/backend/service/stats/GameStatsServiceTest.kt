package com.nfcaab.backend.service.stats

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
                homeTeam = "Team A"
                awayTeam = "Team B"
                battingTeam = "Team A"
                actualResult = Game.ActualResult.SINGLE
                runsScored = 1
            },
            AtBat().apply {
                homeTeam = "Team A"
                awayTeam = "Team B"
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

    @Test
    fun `updateGameStats counts a double play as two outs and a fielders choice as one out toward innings pitched`() {
        val game = Game().apply {
            id = 1
            homeTeam = "Team A"
            awayTeam = "Team B"
        }

        val atBats = listOf(
            AtBat().apply {
                homeTeam = "Team A"
                awayTeam = "Team B"
                battingTeam = "Team B"
                pitchingTeam = "Team A"
                actualResult = Game.ActualResult.DOUBLE_PLAY
            },
            AtBat().apply {
                homeTeam = "Team A"
                awayTeam = "Team B"
                battingTeam = "Team B"
                pitchingTeam = "Team A"
                actualResult = Game.ActualResult.FIELDERS_CHOICE
            },
        )

        val teamAStats = GameStats().apply { gameId = 1; team = "Team A" }
        val teamBStats = GameStats().apply { gameId = 1; team = "Team B" }

        every { gameStatsRepository.getGameStatsByIdAndTeam(1, "Team A") } returns teamAStats
        every { gameStatsRepository.getGameStatsByIdAndTeam(1, "Team B") } returns teamBStats
        every { gameStatsRepository.save(any()) } returns mockk()

        gameStatsService.updateGameStats(game, atBats)

        assertEquals(1.0, teamAStats.inningsPitched)
    }

    @Test
    fun `updateGameStats reports a largest lead and deficit of zero rather than negative for a one-sided game`() {
        val game = Game().apply {
            id = 1
            homeTeam = "Team A"
            awayTeam = "Team B"
            homeScore = 1
            awayScore = 3
        }

        val atBats = listOf(
            AtBat().apply { homeTeam = "Team A"; awayTeam = "Team B"; homeScore = 0; awayScore = 1 },
            AtBat().apply { homeTeam = "Team A"; awayTeam = "Team B"; homeScore = 1; awayScore = 3 },
        )

        val teamAStats = GameStats().apply { gameId = 1; team = "Team A" }
        val teamBStats = GameStats().apply { gameId = 1; team = "Team B" }

        every { gameStatsRepository.getGameStatsByIdAndTeam(1, "Team A") } returns teamAStats
        every { gameStatsRepository.getGameStatsByIdAndTeam(1, "Team B") } returns teamBStats
        every { gameStatsRepository.save(any()) } returns mockk()

        gameStatsService.updateGameStats(game, atBats)

        assertEquals(0, teamAStats.largestLead)
        assertEquals(2, teamAStats.largestDeficit)
        assertEquals(2, teamBStats.largestLead)
        assertEquals(0, teamBStats.largestDeficit)
    }
}

