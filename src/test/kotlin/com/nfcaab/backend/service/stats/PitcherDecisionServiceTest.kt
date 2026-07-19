package com.nfcaab.backend.service.stats

import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.PitcherGameStats
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.repositories.PitcherGameStatsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PitcherDecisionServiceTest {
    private lateinit var atBatRepository: AtBatRepository
    private lateinit var pitcherGameStatsRepository: PitcherGameStatsRepository
    private lateinit var pitcherDecisionService: PitcherDecisionService

    @BeforeEach
    fun setUp() {
        atBatRepository = mockk()
        pitcherGameStatsRepository = mockk()
        pitcherDecisionService = PitcherDecisionService(atBatRepository, pitcherGameStatsRepository)
    }

    private fun game(
        homeScore: Int,
        awayScore: Int,
    ) = Game().apply {
        id = 1
        homeTeam = "Home"
        awayTeam = "Away"
        this.homeScore = homeScore
        this.awayScore = awayScore
    }

    private fun atBat(
        id: Int,
        pitchingTeam: String,
        pitcherUniformNumber: Int,
        homeScore: Int,
        awayScore: Int,
        runnerOnFirst: Int? = null,
        runnerOnSecond: Int? = null,
        runnerOnThird: Int? = null,
    ) = AtBat().apply {
        this.id = id
        gameId = 1
        this.pitchingTeam = pitchingTeam
        this.pitcherUniformNumber = pitcherUniformNumber
        this.homeScore = homeScore
        this.awayScore = awayScore
        this.runnerOnFirst = runnerOnFirst
        this.runnerOnSecond = runnerOnSecond
        this.runnerOnThird = runnerOnThird
    }

    private fun pitcherStats(
        team: String,
        uniformNumber: Int,
        inningsPitched: String,
    ) = PitcherGameStats().apply {
        this.team = team
        this.uniformNumber = uniformNumber
        this.inningsPitched = inningsPitched
    }

    @Test
    fun `computeDecisions credits the starter with the win when they hold the lead for 5 or more innings`() {
        val theGame = game(homeScore = 5, awayScore = 2)
        // Home bats top-of-order style: away pitcher (99) allows the go-ahead run on at-bat 3.
        val atBats =
            listOf(
                atBat(1, pitchingTeam = "Away", pitcherUniformNumber = 99, homeScore = 1, awayScore = 0),
                atBat(2, pitchingTeam = "Home", pitcherUniformNumber = 10, homeScore = 1, awayScore = 1),
                atBat(3, pitchingTeam = "Away", pitcherUniformNumber = 99, homeScore = 3, awayScore = 1),
                atBat(4, pitchingTeam = "Home", pitcherUniformNumber = 10, homeScore = 3, awayScore = 2),
                atBat(5, pitchingTeam = "Away", pitcherUniformNumber = 99, homeScore = 5, awayScore = 2),
            )
        val homeStarter = pitcherStats(team = "Home", uniformNumber = 10, inningsPitched = "6.0")
        val awayStarter = pitcherStats(team = "Away", uniformNumber = 99, inningsPitched = "5.0")

        every { atBatRepository.getAllAtBatsByGameId(1) } returns atBats
        every { pitcherGameStatsRepository.findByGameId(1) } returns listOf(homeStarter, awayStarter)
        every { pitcherGameStatsRepository.save(any()) } answers { firstArg() }

        pitcherDecisionService.computeDecisions(theGame)

        assertTrue(homeStarter.win)
        assertTrue(awayStarter.loss)
        assertFalse(homeStarter.save)
    }

    @Test
    fun `computeDecisions passes the win to the first qualifying reliever when the starter pitches fewer than 5 innings`() {
        val theGame = game(homeScore = 6, awayScore = 4)
        val atBats =
            listOf(
                atBat(1, pitchingTeam = "Away", pitcherUniformNumber = 21, homeScore = 4, awayScore = 0),
                atBat(2, pitchingTeam = "Home", pitcherUniformNumber = 10, homeScore = 4, awayScore = 4),
                atBat(3, pitchingTeam = "Away", pitcherUniformNumber = 21, homeScore = 6, awayScore = 4),
                atBat(4, pitchingTeam = "Home", pitcherUniformNumber = 11, homeScore = 6, awayScore = 4),
            )
        val starter = pitcherStats(team = "Home", uniformNumber = 10, inningsPitched = "2.0")
        val reliever = pitcherStats(team = "Home", uniformNumber = 11, inningsPitched = "2.0")
        val losingPitcher = pitcherStats(team = "Away", uniformNumber = 21, inningsPitched = "4.0")

        every { atBatRepository.getAllAtBatsByGameId(1) } returns atBats
        every { pitcherGameStatsRepository.findByGameId(1) } returns listOf(starter, reliever, losingPitcher)
        every { pitcherGameStatsRepository.save(any()) } answers { firstArg() }

        pitcherDecisionService.computeDecisions(theGame)

        assertFalse(starter.win)
        assertTrue(reliever.win)
        assertTrue(losingPitcher.loss)
    }

    @Test
    fun `computeDecisions awards a save to a reliever who finishes the game with a 1-3 run lead`() {
        val theGame = game(homeScore = 5, awayScore = 3)
        val atBats =
            listOf(
                atBat(1, pitchingTeam = "Away", pitcherUniformNumber = 21, homeScore = 5, awayScore = 0),
                atBat(2, pitchingTeam = "Home", pitcherUniformNumber = 10, homeScore = 5, awayScore = 3),
                atBat(3, pitchingTeam = "Home", pitcherUniformNumber = 12, homeScore = 5, awayScore = 3),
            )
        val starter = pitcherStats(team = "Home", uniformNumber = 10, inningsPitched = "8.0")
        val closer = pitcherStats(team = "Home", uniformNumber = 12, inningsPitched = "1.0")
        val losingPitcher = pitcherStats(team = "Away", uniformNumber = 21, inningsPitched = "8.0")

        every { atBatRepository.getAllAtBatsByGameId(1) } returns atBats
        every { pitcherGameStatsRepository.findByGameId(1) } returns listOf(starter, closer, losingPitcher)
        every { pitcherGameStatsRepository.save(any()) } answers { firstArg() }

        pitcherDecisionService.computeDecisions(theGame)

        assertTrue(starter.win)
        assertTrue(closer.save)
        assertFalse(closer.win)
    }

    @Test
    fun `computeDecisions credits the win to whoever is pitching when the lead is retaken after a blown lead`() {
        val theGame = game(homeScore = 4, awayScore = 3)
        val atBats =
            listOf(
                atBat(1, pitchingTeam = "Away", pitcherUniformNumber = 21, homeScore = 2, awayScore = 0),
                atBat(2, pitchingTeam = "Home", pitcherUniformNumber = 10, homeScore = 2, awayScore = 3),
                atBat(3, pitchingTeam = "Away", pitcherUniformNumber = 21, homeScore = 4, awayScore = 3),
            )
        val homePitcher = pitcherStats(team = "Home", uniformNumber = 10, inningsPitched = "6.0")
        val awayPitcher = pitcherStats(team = "Away", uniformNumber = 21, inningsPitched = "6.0")

        every { atBatRepository.getAllAtBatsByGameId(1) } returns atBats
        every { pitcherGameStatsRepository.findByGameId(1) } returns listOf(homePitcher, awayPitcher)
        every { pitcherGameStatsRepository.save(any()) } answers { firstArg() }

        pitcherDecisionService.computeDecisions(theGame)

        assertTrue(homePitcher.win)
        assertTrue(awayPitcher.loss)
    }

    @Test
    fun `computeDecisions charges the run-charging pitcher correctly across a multi-pitcher game`() {
        val theGame = game(homeScore = 3, awayScore = 2)
        val atBats =
            listOf(
                atBat(1, pitchingTeam = "Away", pitcherUniformNumber = 21, homeScore = 3, awayScore = 0, runnerOnFirst = 5),
                atBat(2, pitchingTeam = "Home", pitcherUniformNumber = 10, homeScore = 3, awayScore = 2),
            )
        val homePitcher = pitcherStats(team = "Home", uniformNumber = 10, inningsPitched = "9.0")
        val awayStarter = pitcherStats(team = "Away", uniformNumber = 21, inningsPitched = "5.0")
        val awayReliever = pitcherStats(team = "Away", uniformNumber = 22, inningsPitched = "4.0")

        every { atBatRepository.getAllAtBatsByGameId(1) } returns atBats
        every { pitcherGameStatsRepository.findByGameId(1) } returns listOf(homePitcher, awayStarter, awayReliever)
        val savedStats = slot<PitcherGameStats>()
        every { pitcherGameStatsRepository.save(capture(savedStats)) } answers { firstArg() }

        pitcherDecisionService.computeDecisions(theGame)

        assertTrue(homePitcher.win)
        assertTrue(awayStarter.loss)
        assertFalse(awayReliever.loss)
    }

    @Test
    fun `computeDecisions does not award a save for an inherited runner during a blowout`() {
        val theGame = game(homeScore = 10, awayScore = 0)
        val atBats =
            listOf(
                atBat(1, pitchingTeam = "Away", pitcherUniformNumber = 21, homeScore = 10, awayScore = 0),
                atBat(2, pitchingTeam = "Home", pitcherUniformNumber = 10, homeScore = 10, awayScore = 0),
                atBat(3, pitchingTeam = "Home", pitcherUniformNumber = 12, homeScore = 10, awayScore = 0, runnerOnFirst = 5),
            )
        val starter = pitcherStats(team = "Home", uniformNumber = 10, inningsPitched = "8.0")
        val closer = pitcherStats(team = "Home", uniformNumber = 12, inningsPitched = "0.1")
        val losingPitcher = pitcherStats(team = "Away", uniformNumber = 21, inningsPitched = "8.0")

        every { atBatRepository.getAllAtBatsByGameId(1) } returns atBats
        every { pitcherGameStatsRepository.findByGameId(1) } returns listOf(starter, closer, losingPitcher)
        every { pitcherGameStatsRepository.save(any()) } answers { firstArg() }

        pitcherDecisionService.computeDecisions(theGame)

        assertTrue(starter.win)
        assertFalse(closer.save)
    }

    @Test
    fun `computeDecisions awards a save when a reliever enters with the tying run on base`() {
        val theGame = game(homeScore = 1, awayScore = 0)
        val atBats =
            listOf(
                atBat(1, pitchingTeam = "Away", pitcherUniformNumber = 21, homeScore = 1, awayScore = 0),
                atBat(2, pitchingTeam = "Home", pitcherUniformNumber = 10, homeScore = 1, awayScore = 0),
                atBat(3, pitchingTeam = "Home", pitcherUniformNumber = 12, homeScore = 1, awayScore = 0, runnerOnFirst = 7),
            )
        val starter = pitcherStats(team = "Home", uniformNumber = 10, inningsPitched = "8.0")
        val closer = pitcherStats(team = "Home", uniformNumber = 12, inningsPitched = "0.2")
        val losingPitcher = pitcherStats(team = "Away", uniformNumber = 21, inningsPitched = "8.0")

        every { atBatRepository.getAllAtBatsByGameId(1) } returns atBats
        every { pitcherGameStatsRepository.findByGameId(1) } returns listOf(starter, closer, losingPitcher)
        every { pitcherGameStatsRepository.save(any()) } answers { firstArg() }

        pitcherDecisionService.computeDecisions(theGame)

        assertTrue(starter.win)
        assertTrue(closer.save)
    }

    @Test
    fun `computeDecisions does nothing when the game is tied`() {
        val theGame = game(homeScore = 4, awayScore = 4)

        pitcherDecisionService.computeDecisions(theGame)

        verify(exactly = 0) { pitcherGameStatsRepository.save(any()) }
    }
}
