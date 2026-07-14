package com.nfcaab.backend.service.atbat

import com.nfcaab.backend.dto.AtBatOutcome
import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.model.Ranges
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.service.game.GameService
import com.nfcaab.backend.service.game.GameLifecycleService
import com.nfcaab.backend.service.player.PlayerService
import com.nfcaab.backend.service.scorebug.ScorebugService
import com.nfcaab.backend.service.stats.GameStatsService
import com.nfcaab.backend.util.EncryptionUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AtBatResolutionServiceTest {
    private lateinit var atBatRepository: AtBatRepository
    private lateinit var encryptionUtils: EncryptionUtils
    private lateinit var gameService: GameService
    private lateinit var gameLifecycleService: GameLifecycleService
    private lateinit var gameStatsService: GameStatsService
    private lateinit var rangesService: RangesService
    private lateinit var scorebugService: ScorebugService
    private lateinit var playerService: PlayerService
    private lateinit var baseRunningService: BaseRunningService
    private lateinit var atBatResolutionService: AtBatResolutionService

    @BeforeEach
    fun setUp() {
        atBatRepository = mockk()
        encryptionUtils = mockk()
        gameService = mockk()
        gameLifecycleService = mockk()
        gameStatsService = mockk()
        rangesService = mockk()
        scorebugService = mockk()
        playerService = mockk()
        baseRunningService = mockk()
        atBatResolutionService =
            AtBatResolutionService(
                atBatRepository,
                encryptionUtils,
                gameService,
                gameLifecycleService,
                gameStatsService,
                rangesService,
                scorebugService,
                playerService,
                baseRunningService,
            )
    }

    @Test
    fun `resolveSwing should resolve a strikeout and re-encrypt both numbers`() {
        val game =
            Game().apply {
                id = 1
                currentAtBatId = 9
                inningHalf = Game.InningHalf.TOP
                homeTeam = "Team A"
                awayTeam = "Team B"
                homeBatterLineupSpot = 1
                awayBatterLineupSpot = 1
                gameStatus = Game.GameStatus.IN_PROGRESS
            }

        val pendingAtBat =
            AtBat().apply {
                id = 9
                gameId = 1
                battingTeam = "Team B"
                pitchingTeam = "Team A"
                batterUniformNumber = 7
                pitcherNumberSubmission = "encrypted-pitcher-42"
            }

        val batter = Player().apply { batterArchetype = Player.BatterArchetype.NEUTRAL }
        val pitcher = Player().apply { pitcherArchetype = Player.PitcherArchetype.NEUTRAL }

        val rangeResult = mockk<Ranges>()
        every { rangeResult.result } returns Game.Scenario.STRIKEOUT

        val outcome =
            AtBatOutcome(
                actualResult = Game.ActualResult.STRIKEOUT,
                outs = 1,
                runsScored = 0,
                homeScore = 0,
                awayScore = 0,
                runnerOnFirstAfter = null,
                runnerOnSecondAfter = null,
                runnerOnThirdAfter = null,
                baseConditionAfter = Game.BaseCondition.EMPTY,
            )

        every { gameService.getDifference(55, 42) } returns 5
        every { playerService.getPlayerByNumberAndTeam("Team B", 7) } returns batter
        every { playerService.getPlayerByNumberAndTeam("Team A", 42) } returns pitcher
        every {
            rangesService.getResult(SubmissionType.SWING, Player.BatterArchetype.NEUTRAL, Player.PitcherArchetype.NEUTRAL, 5)
        } returns rangeResult
        every { gameService.getBaseCondition(null, null, null) } returns Game.BaseCondition.EMPTY
        every {
            baseRunningService.resolveOutcome(Game.Scenario.STRIKEOUT, 0, Game.InningHalf.TOP, Game.BaseCondition.EMPTY, null, null, null, 0, 0)
        } returns outcome
        every { gameLifecycleService.updateGameValues(game, outcome) } returns game
        every { scorebugService.generateScorebug(game) } returns mockk()
        every { atBatRepository.getAllAtBatsByGameId(1) } returns emptyList()
        every { gameStatsService.updateGameStats(game, emptyList()) } returns emptyList()
        every { encryptionUtils.encrypt("55") } returns "encrypted-batter-55"
        every { encryptionUtils.encrypt("42") } returns "re-encrypted-pitcher-42"
        every { atBatRepository.save(any()) } answers { firstArg() }

        val result = atBatResolutionService.resolveSwing(pendingAtBat, game, SubmissionType.SWING, 55, "42")

        assertNotNull(result)
        assertEquals("encrypted-batter-55", result.batterNumberSubmission)
        assertEquals("re-encrypted-pitcher-42", result.pitcherNumberSubmission)
        assertNotEquals("55", result.batterNumberSubmission)
        assertNotEquals("42", result.pitcherNumberSubmission)
    }

    @Test
    fun `resolveSteal re-encrypts both numbers instead of persisting them as plaintext`() {
        val pendingAtBat =
            AtBat().apply {
                id = 9
                gameId = 1
                battingTeam = "Team B"
                pitchingTeam = "Team A"
                batterUniformNumber = 7
                pitcherNumberSubmission = "encrypted-pitcher-42"
            }

        val runner = Player().apply { batterArchetype = Player.BatterArchetype.SPEEDY }
        val pitcher = Player().apply { pitcherArchetype = Player.PitcherArchetype.NEUTRAL }

        every { gameService.getDifference(17, 42) } returns 5
        every { playerService.getPlayerByNumberAndTeam("Team B", 7) } returns runner
        every { playerService.getPlayerByNumberAndTeam("Team A", 42) } returns pitcher
        every {
            baseRunningService.resolveSteal(Player.BatterArchetype.SPEEDY, Player.PitcherArchetype.NEUTRAL, 5)
        } returns Game.Scenario.STEAL_SUCCESS
        every { encryptionUtils.encrypt("17") } returns "encrypted-batter-17"
        every { encryptionUtils.encrypt("42") } returns "re-encrypted-pitcher-42"
        every { atBatRepository.save(any()) } answers { firstArg() }

        val result = atBatResolutionService.resolveSteal(pendingAtBat, SubmissionType.STEAL, 17, "42")

        assertNotNull(result)
        assertEquals("encrypted-batter-17", result.batterNumberSubmission)
        assertEquals("re-encrypted-pitcher-42", result.pitcherNumberSubmission)
        assertNotEquals("17", result.batterNumberSubmission)
        assertNotEquals("42", result.pitcherNumberSubmission)
        assertEquals(Game.ActualResult.SINGLE, result.actualResult)
        verify { encryptionUtils.encrypt("17") }
        verify { encryptionUtils.encrypt("42") }
    }

    @Test
    fun `resolveSteal marks a failed steal as a strikeout`() {
        val pendingAtBat =
            AtBat().apply {
                id = 9
                gameId = 1
                battingTeam = "Team B"
                pitchingTeam = "Team A"
                batterUniformNumber = 7
                pitcherNumberSubmission = "encrypted-pitcher-42"
            }

        val runner = Player().apply { batterArchetype = Player.BatterArchetype.SPEEDY }
        val pitcher = Player().apply { pitcherArchetype = Player.PitcherArchetype.NEUTRAL }

        every { gameService.getDifference(17, 42) } returns 400
        every { playerService.getPlayerByNumberAndTeam("Team B", 7) } returns runner
        every { playerService.getPlayerByNumberAndTeam("Team A", 42) } returns pitcher
        every {
            baseRunningService.resolveSteal(Player.BatterArchetype.SPEEDY, Player.PitcherArchetype.NEUTRAL, 400)
        } returns Game.Scenario.STEAL_ATTEMPT
        every { encryptionUtils.encrypt(any()) } returns "encrypted"
        every { atBatRepository.save(any()) } answers { firstArg() }

        val result = atBatResolutionService.resolveSteal(pendingAtBat, SubmissionType.STEAL, 17, "42")

        assertEquals(Game.ActualResult.STRIKEOUT, result.actualResult)
    }
}
