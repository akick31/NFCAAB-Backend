package com.nfcaab.backend.service.stats

import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.repositories.BatterGameStatsRepository
import com.nfcaab.backend.repositories.PitcherGameStatsRepository
import com.nfcaab.backend.repositories.RunEventRepository
import com.nfcaab.backend.service.player.PlayerService
import com.nfcaab.backend.util.PlayerNotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlayerGameStatsServiceTest {
    private lateinit var batterGameStatsRepository: BatterGameStatsRepository
    private lateinit var pitcherGameStatsRepository: PitcherGameStatsRepository
    private lateinit var runEventRepository: RunEventRepository
    private lateinit var playerService: PlayerService
    private lateinit var playerGameStatsService: PlayerGameStatsService

    @BeforeEach
    fun setUp() {
        batterGameStatsRepository = mockk()
        pitcherGameStatsRepository = mockk()
        runEventRepository = mockk()
        playerService = mockk()
        playerGameStatsService =
            PlayerGameStatsService(batterGameStatsRepository, pitcherGameStatsRepository, runEventRepository, playerService)

        every { playerService.getPlayerByNumberAndTeam(any(), any()) } throws PlayerNotFoundException("not found")
        every { runEventRepository.countByGameIdAndScoringPlayerUniformNumberAndScoringTeam(any(), any(), any()) } returns 0
        every { runEventRepository.countByGameIdAndChargedPitcherUniformNumberAndChargedPitcherTeam(any(), any(), any()) } returns 0
        every { batterGameStatsRepository.findByGameIdAndUniformNumberAndTeam(any(), any(), any()) } returns null
        every { pitcherGameStatsRepository.findByGameIdAndUniformNumberAndTeam(any(), any(), any()) } returns null
    }

    private fun theGame() =
        com.nfcaab.backend.model.Game().apply {
            id = 1
            homeTeam = "Home"
            awayTeam = "Away"
        }

    private fun pitchingAtBat(
        actualResult: ActualResult,
        lineupSpot: Int = 1,
    ) = AtBat().apply {
        gameId = 1
        homeTeam = "Home"
        awayTeam = "Away"
        battingTeam = "Away"
        pitchingTeam = "Home"
        pitcherUniformNumber = 10
        batterUniformNumber = 5
        this.lineupSpot = lineupSpot
        this.actualResult = actualResult
    }

    @Test
    fun `updatePlayerGameStats counts a double play as two outs and a fielders choice as one out toward innings pitched`() {
        val atBats = listOf(pitchingAtBat(ActualResult.DOUBLE_PLAY), pitchingAtBat(ActualResult.FIELDERS_CHOICE))
        val savedPitcherStats = slot<com.nfcaab.backend.model.PitcherGameStats>()
        every { pitcherGameStatsRepository.save(capture(savedPitcherStats)) } answers { firstArg() }
        every { batterGameStatsRepository.save(any()) } answers { firstArg() }

        playerGameStatsService.updatePlayerGameStats(theGame(), atBats)

        assertEquals("1.0", savedPitcherStats.captured.inningsPitched)
    }

    @Test
    fun `updatePlayerGameStats counts a sacrifice fly, sacrifice bunt, and caught stealing as one out each`() {
        val atBats =
            listOf(
                pitchingAtBat(ActualResult.SACRIFICE_FLY),
                pitchingAtBat(ActualResult.SACRIFICE_BUNT),
                pitchingAtBat(ActualResult.CAUGHT_STEALING),
            )
        val savedPitcherStats = slot<com.nfcaab.backend.model.PitcherGameStats>()
        every { pitcherGameStatsRepository.save(capture(savedPitcherStats)) } answers { firstArg() }
        every { batterGameStatsRepository.save(any()) } answers { firstArg() }

        playerGameStatsService.updatePlayerGameStats(theGame(), atBats)

        assertEquals("1.0", savedPitcherStats.captured.inningsPitched)
    }
}
