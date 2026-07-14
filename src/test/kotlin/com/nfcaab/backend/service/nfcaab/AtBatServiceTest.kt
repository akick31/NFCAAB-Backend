package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.dto.AtBatOutcome
import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.model.Ranges
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.util.EncryptionUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AtBatServiceTest {
    private lateinit var atBatRepository: AtBatRepository
    private lateinit var encryptionUtils: EncryptionUtils
    private lateinit var gameService: GameService
    private lateinit var gameStatsService: GameStatsService
    private lateinit var rangesService: RangesService
    private lateinit var scorebugService: ScorebugService
    private lateinit var playerService: PlayerService
    private lateinit var lineupService: LineupService
    private lateinit var atBatService: AtBatService

    @BeforeEach
    fun setUp() {
        atBatRepository = mockk()
        encryptionUtils = mockk()
        gameService = mockk()
        gameStatsService = mockk()
        rangesService = mockk()
        scorebugService = mockk()
        playerService = mockk()
        lineupService = mockk()
        atBatService =
            AtBatService(
                atBatRepository,
                encryptionUtils,
                gameService,
                gameStatsService,
                rangesService,
                scorebugService,
                playerService,
                lineupService,
            )
    }

    @Test
    fun `pitchingNumberSubmitted should create AtBat with encrypted pitcher number`() {
        val gameId = 1
        val pitcherSubmitter = "pitcherUser"
        val pitcherNumberSubmission = 42
        val submissionType: SubmissionType? = null

        val game =
            Game().apply {
                id = gameId
                numAtBat = 0
                inningHalf = Game.InningHalf.TOP
                homeTeam = "Team A"
                awayTeam = "Team B"
                gameStatus = Game.GameStatus.IN_PROGRESS
            }

        val encryptedNumber = "encrypted42"
        val savedAtBat = AtBat()
        savedAtBat.id = 1
        savedAtBat.gameId = gameId
        savedAtBat.pitcherNumberSubmission = encryptedNumber

        every { gameService.getGameById(gameId) } returns game
        every { encryptionUtils.encrypt(pitcherNumberSubmission.toString()) } returns encryptedNumber
        every { gameService.updateWithPitcherNumberSubmission(any(), any()) } returns Unit
        every { atBatRepository.save(any()) } returns savedAtBat

        val result =
            atBatService.pitchingNumberSubmitted(
                gameId,
                pitcherSubmitter,
                pitcherNumberSubmission,
                submissionType,
            )

        assertNotNull(result)
        assertEquals(encryptedNumber, result.pitcherNumberSubmission)
        verify { encryptionUtils.encrypt(pitcherNumberSubmission.toString()) }
        verify { atBatRepository.save(any()) }
    }

    @Test
    fun `batterNumberSubmitted should resolve a swing and re-encrypt both numbers`() {
        val gameId = 1
        val batterSubmitter = "batterUser"
        val batterNumberSubmission = 55
        val submissionType = SubmissionType.SWING

        val game = Game()
        game.id = gameId
        game.currentAtBatId = 9
        game.inningHalf = Game.InningHalf.TOP
        game.homeTeam = "Team A"
        game.awayTeam = "Team B"
        game.homeBatterLineupSpot = 1
        game.awayBatterLineupSpot = 1
        game.gameStatus = Game.GameStatus.IN_PROGRESS

        val pendingAtBat = AtBat()
        pendingAtBat.id = 9
        pendingAtBat.gameId = gameId
        pendingAtBat.battingTeam = "Team B"
        pendingAtBat.pitchingTeam = "Team A"
        pendingAtBat.batterUniformNumber = 7
        pendingAtBat.pitcherNumberSubmission = "encrypted-pitcher-42"

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

        every { gameService.getGameById(gameId) } returns game
        every { atBatRepository.getAtBatById(9) } returns pendingAtBat
        every { encryptionUtils.decrypt("encrypted-pitcher-42") } returns "42"
        every { gameService.getDifference(batterNumberSubmission, 42) } returns 5
        every { playerService.getPlayerByNumberAndTeam("Team B", 7) } returns batter
        every { playerService.getPlayerByNumberAndTeam("Team A", 42) } returns pitcher
        every {
            rangesService.getResult(submissionType, Player.BatterArchetype.NEUTRAL, Player.PitcherArchetype.NEUTRAL, 5)
        } returns rangeResult
        every { gameService.getBaseCondition(null, null, null) } returns Game.BaseCondition.EMPTY
        every { gameService.updateGameValues(game, outcome) } returns game
        every { scorebugService.generateScorebug(game) } returns mockk()
        every { atBatRepository.getAllAtBatsByGameId(gameId) } returns emptyList()
        every { gameStatsService.updateGameStats(game, emptyList()) } returns emptyList()
        every { encryptionUtils.encrypt(batterNumberSubmission.toString()) } returns "encrypted-batter-55"
        every { encryptionUtils.encrypt("42") } returns "re-encrypted-pitcher-42"
        every { atBatRepository.save(any()) } answers { firstArg() }

        val result =
            atBatService.batterNumberSubmitted(
                gameId,
                batterSubmitter,
                batterNumberSubmission,
                submissionType,
            )

        assertNotNull(result)
        assertEquals("encrypted-batter-55", result.batterNumberSubmission)
        assertEquals("re-encrypted-pitcher-42", result.pitcherNumberSubmission)
        assertNotEquals(batterNumberSubmission.toString(), result.batterNumberSubmission)
        assertNotEquals("42", result.pitcherNumberSubmission)
        verify { atBatRepository.getAtBatById(9) }
        verify { encryptionUtils.decrypt("encrypted-pitcher-42") }
    }

    @Test
    fun `steal resolution re-encrypts both numbers instead of persisting them as plaintext`() {
        val gameId = 1
        val batterSubmitter = "batterUser"
        val batterNumberSubmission = 17
        val submissionType = SubmissionType.STEAL

        val game = Game()
        game.id = gameId
        game.currentAtBatId = 9
        game.inningHalf = Game.InningHalf.TOP
        game.homeTeam = "Team A"
        game.awayTeam = "Team B"
        game.gameStatus = Game.GameStatus.IN_PROGRESS

        val pendingAtBat = AtBat()
        pendingAtBat.id = 9
        pendingAtBat.gameId = gameId
        pendingAtBat.battingTeam = "Team B"
        pendingAtBat.pitchingTeam = "Team A"
        pendingAtBat.batterUniformNumber = 7
        pendingAtBat.pitcherNumberSubmission = "encrypted-pitcher-42"

        val runner = Player().apply { batterArchetype = Player.BatterArchetype.SPEEDY }
        val pitcher = Player().apply { pitcherArchetype = Player.PitcherArchetype.NEUTRAL }

        val stealResult = mockk<Ranges>()
        every { stealResult.result } returns Game.Scenario.STEAL_SUCCESS

        every { gameService.getGameById(gameId) } returns game
        every { atBatRepository.getAtBatById(9) } returns pendingAtBat
        every { encryptionUtils.decrypt("encrypted-pitcher-42") } returns "42"
        every { gameService.getDifference(batterNumberSubmission, 42) } returns 5
        every { playerService.getPlayerByNumberAndTeam("Team B", 7) } returns runner
        every { playerService.getPlayerByNumberAndTeam("Team A", 42) } returns pitcher
        every {
            rangesService.getStealResult(Player.BatterArchetype.SPEEDY, Player.PitcherArchetype.NEUTRAL, 5)
        } returns stealResult
        every { encryptionUtils.encrypt(batterNumberSubmission.toString()) } returns "encrypted-batter-17"
        every { encryptionUtils.encrypt("42") } returns "re-encrypted-pitcher-42"
        every { atBatRepository.save(any()) } answers { firstArg() }

        val result =
            atBatService.batterNumberSubmitted(
                gameId,
                batterSubmitter,
                batterNumberSubmission,
                submissionType,
            )

        assertNotNull(result)
        assertEquals("encrypted-batter-17", result.batterNumberSubmission)
        assertEquals("re-encrypted-pitcher-42", result.pitcherNumberSubmission)
        assertNotEquals(batterNumberSubmission.toString(), result.batterNumberSubmission)
        assertNotEquals("42", result.pitcherNumberSubmission)
        verify { encryptionUtils.encrypt(batterNumberSubmission.toString()) }
        verify { encryptionUtils.encrypt("42") }
    }
}
