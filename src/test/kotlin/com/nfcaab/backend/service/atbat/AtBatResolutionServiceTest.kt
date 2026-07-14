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
    private lateinit var hitLocationService: HitLocationService
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
        hitLocationService = mockk()
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
                hitLocationService,
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
            hitLocationService.determine(Game.Scenario.STRIKEOUT, Player.BatterArchetype.NEUTRAL, 55, 5)
        } returns HitLocation(null, null, null)
        every {
            baseRunningService.resolveOutcome(
                Game.Scenario.STRIKEOUT,
                0,
                Game.InningHalf.TOP,
                Game.BaseCondition.EMPTY,
                null,
                null,
                null,
                0,
                0,
                null,
                batter,
                SubmissionType.SWING,
            )
        } returns outcome
        every { gameLifecycleService.updateGameValues(game, outcome) } returns game
        every { scorebugService.generateScorebug(game) } returns mockk()
        every { atBatRepository.getAllAtBatsByGameId(1) } returns emptyList()
        every { gameStatsService.updateGameStats(game, emptyList()) } returns emptyList()
        every {
            hitLocationService.buildFieldingNotation(Game.ActualResult.STRIKEOUT, null, null)
        } returns null
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
    fun `resolveIntentionalWalk places the batter on first`() {
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

        val batter = Player().apply { uniformNumber = 7; batterArchetype = Player.BatterArchetype.NEUTRAL }

        val outcome =
            AtBatOutcome(
                actualResult = Game.ActualResult.WALK,
                outs = 0,
                runsScored = 0,
                homeScore = 0,
                awayScore = 0,
                runnerOnFirstAfter = batter,
                runnerOnSecondAfter = null,
                runnerOnThirdAfter = null,
                baseConditionAfter = Game.BaseCondition.FIRST,
            )

        every { gameService.getDifference(55, 42) } returns 5
        every { playerService.getPlayerByNumberAndTeam("Team B", 7) } returns batter
        every { gameService.getBaseCondition(null, null, null) } returns Game.BaseCondition.EMPTY
        every {
            baseRunningService.resolveOutcome(
                Game.Scenario.WALK,
                0,
                Game.InningHalf.TOP,
                Game.BaseCondition.EMPTY,
                null,
                null,
                null,
                0,
                0,
                null,
                batter,
            )
        } returns outcome
        every { gameLifecycleService.updateGameValues(game, outcome) } returns game
        every { scorebugService.generateScorebug(game) } returns mockk()
        every { atBatRepository.getAllAtBatsByGameId(1) } returns emptyList()
        every { gameStatsService.updateGameStats(game, emptyList()) } returns emptyList()
        every { encryptionUtils.encrypt("55") } returns "encrypted-batter-55"
        every { encryptionUtils.encrypt("42") } returns "re-encrypted-pitcher-42"
        every { atBatRepository.save(any()) } answers { firstArg() }

        val result = atBatResolutionService.resolveIntentionalWalk(pendingAtBat, game, SubmissionType.SWING, 55, "42")

        assertEquals(Game.ActualResult.WALK, result.actualResult)
        assertEquals(7, result.runnerOnFirstAfter)
    }

    private fun stealGame() =
        Game().apply {
            id = 1
            currentAtBatId = 9
            inningHalf = Game.InningHalf.TOP
            homeTeam = "Team A"
            awayTeam = "Team B"
            homeBatterLineupSpot = 1
            awayBatterLineupSpot = 1
            gameStatus = Game.GameStatus.IN_PROGRESS
            runnerOnFirst = 12
        }

    private fun stealAtBat() =
        AtBat().apply {
            id = 9
            gameId = 1
            battingTeam = "Team B"
            pitchingTeam = "Team A"
            batterUniformNumber = 7
            runnerOnFirst = 12
            pitcherNumberSubmission = "encrypted-pitcher-42"
        }

    @Test
    fun `resolveSteal advances the lead runner and re-encrypts both numbers on a successful steal`() {
        val game = stealGame()
        val pendingAtBat = stealAtBat()
        val runner = Player().apply { uniformNumber = 12; batterArchetype = Player.BatterArchetype.SPEEDY }
        val pitcher = Player().apply { pitcherArchetype = Player.PitcherArchetype.NEUTRAL }
        val outcome =
            AtBatOutcome(
                actualResult = Game.ActualResult.STOLEN_BASE,
                outs = 0,
                runsScored = 0,
                homeScore = 0,
                awayScore = 0,
                runnerOnFirstAfter = null,
                runnerOnSecondAfter = runner,
                runnerOnThirdAfter = null,
                baseConditionAfter = Game.BaseCondition.SECOND,
            )

        every { gameService.getDifference(17, 42) } returns 5
        every { playerService.getPlayerByNumberAndTeam("Team B", 12) } returns runner
        every { playerService.getPlayerByNumberAndTeam("Team A", 42) } returns pitcher
        every { baseRunningService.leadRunner(runner, null, null) } returns runner
        every {
            baseRunningService.resolveSteal(Player.BatterArchetype.SPEEDY, Player.PitcherArchetype.NEUTRAL, 5)
        } returns Game.Scenario.STEAL_SUCCESS
        every {
            baseRunningService.resolveStealOutcome(Game.Scenario.STEAL_SUCCESS, 0, Game.InningHalf.TOP, runner, null, null, 0, 0)
        } returns outcome
        every { gameLifecycleService.updateGameValues(game, outcome, false) } returns game
        every { scorebugService.generateScorebug(game) } returns mockk()
        every { atBatRepository.getAllAtBatsByGameId(1) } returns emptyList()
        every { gameStatsService.updateGameStats(game, emptyList()) } returns emptyList()
        every { encryptionUtils.encrypt("17") } returns "encrypted-batter-17"
        every { encryptionUtils.encrypt("42") } returns "re-encrypted-pitcher-42"
        every { atBatRepository.save(any()) } answers { firstArg() }

        val result = atBatResolutionService.resolveSteal(pendingAtBat, game, SubmissionType.STEAL, 17, "42")

        assertNotNull(result)
        assertEquals("encrypted-batter-17", result.batterNumberSubmission)
        assertEquals("re-encrypted-pitcher-42", result.pitcherNumberSubmission)
        assertNotEquals("17", result.batterNumberSubmission)
        assertNotEquals("42", result.pitcherNumberSubmission)
        assertEquals(Game.ActualResult.STOLEN_BASE, result.actualResult)
        assertEquals(12, result.runnerOnSecondAfter)
        verify { gameLifecycleService.updateGameValues(game, outcome, false) }
    }

    @Test
    fun `resolveSteal records a caught stealing when the attempt fails`() {
        val game = stealGame()
        val pendingAtBat = stealAtBat()
        val runner = Player().apply { uniformNumber = 12; batterArchetype = Player.BatterArchetype.SPEEDY }
        val pitcher = Player().apply { pitcherArchetype = Player.PitcherArchetype.NEUTRAL }
        val outcome =
            AtBatOutcome(
                actualResult = Game.ActualResult.CAUGHT_STEALING,
                outs = 1,
                runsScored = 0,
                homeScore = 0,
                awayScore = 0,
                runnerOnFirstAfter = null,
                runnerOnSecondAfter = null,
                runnerOnThirdAfter = null,
                baseConditionAfter = Game.BaseCondition.EMPTY,
            )

        every { gameService.getDifference(17, 42) } returns 400
        every { playerService.getPlayerByNumberAndTeam("Team B", 12) } returns runner
        every { playerService.getPlayerByNumberAndTeam("Team A", 42) } returns pitcher
        every { baseRunningService.leadRunner(runner, null, null) } returns runner
        every {
            baseRunningService.resolveSteal(Player.BatterArchetype.SPEEDY, Player.PitcherArchetype.NEUTRAL, 400)
        } returns Game.Scenario.STEAL_ATTEMPT
        every {
            baseRunningService.resolveStealOutcome(Game.Scenario.STEAL_ATTEMPT, 0, Game.InningHalf.TOP, runner, null, null, 0, 0)
        } returns outcome
        every { gameLifecycleService.updateGameValues(game, outcome, false) } returns game
        every { scorebugService.generateScorebug(game) } returns mockk()
        every { atBatRepository.getAllAtBatsByGameId(1) } returns emptyList()
        every { gameStatsService.updateGameStats(game, emptyList()) } returns emptyList()
        every { encryptionUtils.encrypt(any()) } returns "encrypted"
        every { atBatRepository.save(any()) } answers { firstArg() }

        val result = atBatResolutionService.resolveSteal(pendingAtBat, game, SubmissionType.STEAL, 17, "42")

        assertEquals(Game.ActualResult.CAUGHT_STEALING, result.actualResult)
        verify { gameLifecycleService.updateGameValues(game, outcome, false) }
    }
}
