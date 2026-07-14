package com.nfcaab.backend.service.elo

import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Team
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.service.atbat.AtBatService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WinProbabilityServiceTest {
    private lateinit var atBatRepository: AtBatRepository
    private lateinit var atBatService: AtBatService
    private lateinit var winProbabilityService: WinProbabilityService

    @BeforeEach
    fun setUp() {
        atBatRepository = mockk()
        atBatService = mockk()
        winProbabilityService = WinProbabilityService(atBatRepository, atBatService)
    }

    private fun atBat(
        id: Int = 1,
        gameId: Int = 1,
        inning: Int = 1,
        inningHalf: Game.InningHalf = Game.InningHalf.TOP,
        outs: Int = 0,
        homeScore: Int = 0,
        awayScore: Int = 0,
        runnerOnFirst: Int? = null,
        runnerOnSecond: Int? = null,
        runnerOnThird: Int? = null,
    ) = AtBat().apply {
        this.id = id
        this.gameId = gameId
        this.inning = inning
        this.inningHalf = inningHalf
        this.outs = outs
        this.homeScore = homeScore
        this.awayScore = awayScore
        this.runnerOnFirst = runnerOnFirst
        this.runnerOnSecond = runnerOnSecond
        this.runnerOnThird = runnerOnThird
    }

    @Test
    fun `calculateWinProbability should favor the higher elo team when they are batting`() {
        val game = Game().apply { id = 1; homeTeam = "Home Team"; awayTeam = "Away Team" }
        val pa = atBat(inningHalf = Game.InningHalf.BOTTOM)

        every { atBatService.getAllAtBatsByGameId(1) } returns emptyList()

        val result = winProbabilityService.calculateWinProbability(game, pa, homeElo = 1700.0, awayElo = 1300.0)

        assertTrue(result > 0.5)
    }

    @Test
    fun `calculateWinProbability should weight elo less as the game reaches its final innings`() {
        val game = Game().apply { id = 1; homeTeam = "Home Team"; awayTeam = "Away Team" }
        val earlyGamePa = atBat(inning = 1, inningHalf = Game.InningHalf.TOP)
        val lateGamePa = atBat(id = 2, inning = 9, inningHalf = Game.InningHalf.TOP)

        every { atBatService.getAllAtBatsByGameId(1) } returns emptyList()

        val earlyResult = winProbabilityService.calculateWinProbability(game, earlyGamePa, homeElo = 1700.0, awayElo = 1300.0)
        val lateResult = winProbabilityService.calculateWinProbability(game, lateGamePa, homeElo = 1700.0, awayElo = 1300.0)

        assertTrue(kotlin.math.abs(earlyResult - 0.5) > kotlin.math.abs(lateResult - 0.5))
    }

    @Test
    fun `calculateWinProbability should favor the batting team when they lead late regardless of home or away`() {
        val game = Game().apply { id = 1; homeTeam = "Home Team"; awayTeam = "Away Team" }
        val awayBattingWithLead = atBat(inning = 9, inningHalf = Game.InningHalf.TOP, homeScore = 1, awayScore = 6)

        every { atBatService.getAllAtBatsByGameId(1) } returns emptyList()

        val result = winProbabilityService.calculateWinProbability(game, awayBattingWithLead, homeElo = 1500.0, awayElo = 1500.0)

        assertTrue(result > 0.5)
    }

    @Test
    fun `calculateWinProbability should boost the batting team when bases are loaded with fewer outs`() {
        val game = Game().apply { id = 1; homeTeam = "Home Team"; awayTeam = "Away Team" }
        val basesEmptyTwoOuts = atBat(outs = 2)
        val basesLoadedNoOuts = atBat(id = 2, outs = 0, runnerOnFirst = 1, runnerOnSecond = 2, runnerOnThird = 3)

        every { atBatService.getAllAtBatsByGameId(1) } returns emptyList()

        val emptyBasesResult = winProbabilityService.calculateWinProbability(game, basesEmptyTwoOuts, homeElo = 1500.0, awayElo = 1500.0)
        val loadedBasesResult = winProbabilityService.calculateWinProbability(game, basesLoadedNoOuts, homeElo = 1500.0, awayElo = 1500.0)

        assertTrue(loadedBasesResult > emptyBasesResult)
    }

    @Test
    fun `calculateWinProbability should set winProbability and winProbabilityAdded on the at bat`() {
        val game = Game().apply { id = 1; homeTeam = "Home Team"; awayTeam = "Away Team" }
        val pa = atBat()

        every { atBatService.getAllAtBatsByGameId(1) } returns emptyList()

        val result = winProbabilityService.calculateWinProbability(game, pa, homeElo = 1500.0, awayElo = 1500.0)

        assertEquals(result.toFloat(), pa.winProbability)
        assertEquals((result - 0.5).toFloat(), pa.winProbabilityAdded)
    }

    @Test
    fun `calculateWinProbability should compute winProbabilityAdded relative to the home team across a half inning flip`() {
        val game = Game().apply { id = 1; homeTeam = "Home Team"; awayTeam = "Away Team" }
        val previousPa = atBat(id = 1, inningHalf = Game.InningHalf.TOP).apply { winProbability = 0.6F }
        val currentPa = atBat(id = 2, inningHalf = Game.InningHalf.BOTTOM)

        every { atBatService.getAllAtBatsByGameId(1) } returns listOf(previousPa)

        winProbabilityService.calculateWinProbability(game, currentPa, homeElo = 1500.0, awayElo = 1500.0)

        val previousHomeWinProb = 1.0 - 0.6F.toDouble()
        val currentHomeWinProb = currentPa.winProbability.toDouble()
        assertEquals(currentHomeWinProb - previousHomeWinProb, currentPa.winProbabilityAdded.toDouble(), 0.0001)
    }

    @Test
    fun `updateEloRatings should raise the winner's elo and lower the loser's`() {
        val game = Game().apply { id = 1; homeTeam = "Home Team"; awayTeam = "Away Team"; homeScore = 5; awayScore = 2 }
        val homeTeam = Team().apply { name = "Home Team"; currentElo = 1500.0 }
        val awayTeam = Team().apply { name = "Away Team"; currentElo = 1500.0 }

        winProbabilityService.updateEloRatings(game, homeTeam, awayTeam)

        assertTrue(homeTeam.currentElo > 1500.0)
        assertTrue(awayTeam.currentElo < 1500.0)
        assertEquals(3000.0, homeTeam.currentElo + awayTeam.currentElo, 0.001)
    }

    @Test
    fun `getEloRatings should sort teams by descending elo`() {
        val teamA = Team().apply { id = 1; name = "Team A"; currentElo = 1400.0; overallElo = 1400.0 }
        val teamB = Team().apply { id = 2; name = "Team B"; currentElo = 1600.0; overallElo = 1600.0 }

        val result = winProbabilityService.getEloRatings(listOf(teamA, teamB))

        assertEquals("Team B", result[0].teamName)
        assertEquals("Team A", result[1].teamName)
    }

    @Test
    fun `getWinProbabilitiesForGame should flip win probability perspective for the top half`() {
        val topHalfPa = atBat(inningHalf = Game.InningHalf.TOP).apply { winProbability = 0.7F }
        val bottomHalfPa = atBat(id = 2, inningHalf = Game.InningHalf.BOTTOM).apply { winProbability = 0.7F }

        val result = winProbabilityService.getWinProbabilitiesForGame(1, listOf(topHalfPa, bottomHalfPa))

        val topPlay = result.plays.first { it.playNumber == 1 }
        val bottomPlay = result.plays.first { it.playNumber == 2 }
        assertEquals(0.3, topPlay.homeTeamWinProbability, 0.001)
        assertEquals(0.7, bottomPlay.homeTeamWinProbability, 0.001)
    }
}
