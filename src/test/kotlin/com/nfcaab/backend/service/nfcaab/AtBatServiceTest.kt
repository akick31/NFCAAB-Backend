package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.util.EncryptionUtils
import com.nfcaab.backend.util.PitcherNumberSubmissionNotFound
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
        atBatService = AtBatService(
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

        val game = Game().apply {
            id = gameId
            numAtBat = 0
            inningHalf = Game.InningHalf.TOP
            homeTeam = "Team A"
            awayTeam = "Team B"
            gameStatus = Game.GameStatus.IN_PROGRESS
        }

        val encryptedNumber = "encrypted42"
        val savedAtBat = AtBat().apply {
            id = 1
            gameId = gameId
            pitcherNumberSubmission = encryptedNumber
        }

        every { gameService.getGameById(gameId) } returns game
        every { encryptionUtils.encrypt(pitcherNumberSubmission.toString()) } returns encryptedNumber
        every { atBatRepository.save(any()) } returns savedAtBat

        val result = atBatService.pitchingNumberSubmitted(
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
    fun `batterNumberSubmitted should process batter number and determine outcome`() {
        val gameId = 1
        val batterSubmitter = "batterUser"
        val batterNumberSubmission = 55
        val submissionType = SubmissionType.SWING

        val game = Game().apply {
            id = gameId
            numAtBat = 1
            inningHalf = Game.InningHalf.TOP
            homeTeam = "Team A"
            awayTeam = "Team B"
            gameStatus = Game.GameStatus.IN_PROGRESS
        }

        val atBat = AtBat().apply {
            id = 1
            gameId = gameId
            pitcherNumberSubmission = "encrypted42"
        }

        every { gameService.getGameById(gameId) } returns game
        every { atBatRepository.getAtBatByGameIdAndPitchNumber(gameId, game.numAtBat) } returns atBat
        every { encryptionUtils.decrypt(atBat.pitcherNumberSubmission) } returns "42"
        every { rangesService.getAtBatOutcome(any(), any(), any(), any(), any()) } returns mockk<com.nfcaab.backend.dto.AtBatOutcome>()
        every { atBatRepository.save(any()) } returns atBat
        every { gameService.updateGame(any()) } returns game
        every { gameStatsService.updateGameStats(any()) } returns mockk()
        every { scorebugService.generateScorebug(any()) } returns mockk()

        val result = atBatService.batterNumberSubmitted(
            gameId,
            batterSubmitter,
            batterNumberSubmission,
            submissionType,
        )

        assertNotNull(result)
        verify { atBatRepository.getAtBatByGameIdAndPitchNumber(gameId, game.numAtBat) }
        verify { encryptionUtils.decrypt(atBat.pitcherNumberSubmission) }
    }

    @Test
    fun `steal resolution re-encrypts both numbers instead of persisting them as plaintext`() {
        val gameId = 1
        val batterSubmitter = "batterUser"
        val batterNumberSubmission = 17
        val submissionType = SubmissionType.STEAL

        val game =
            Game().apply {
                id = gameId
                currentAtBatId = 9
                inningHalf = Game.InningHalf.TOP
                homeTeam = "Team A"
                awayTeam = "Team B"
                gameStatus = Game.GameStatus.IN_PROGRESS
            }

        val pendingAtBat =
            AtBat().apply {
                id = 9
                gameId = gameId
                battingTeam = "Team B"
                pitcherNumberSubmission = "encrypted-pitcher-42"
            }

        val stealResult = mockk<com.nfcaab.backend.model.Ranges>()
        every { stealResult.result } returns Game.Scenario.STEAL_SUCCESS

        every { gameService.getGameById(gameId) } returns game
        every { atBatRepository.getAtBatById(9) } returns pendingAtBat
        every { encryptionUtils.decrypt("encrypted-pitcher-42") } returns "42"
        every { gameService.getDifference(batterNumberSubmission, 42) } returns 5
        every { rangesService.getStealResult(5) } returns stealResult
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
