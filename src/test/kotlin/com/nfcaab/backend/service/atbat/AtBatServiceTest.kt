package com.nfcaab.backend.service.atbat

import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.service.game.GameService
import com.nfcaab.backend.service.game.GameLifecycleService
import com.nfcaab.backend.util.EncryptionUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AtBatServiceTest {
    private lateinit var atBatRepository: AtBatRepository
    private lateinit var encryptionUtils: EncryptionUtils
    private lateinit var gameService: GameService
    private lateinit var gameLifecycleService: GameLifecycleService
    private lateinit var atBatResolutionService: AtBatResolutionService
    private lateinit var buntService: BuntService
    private lateinit var atBatService: AtBatService

    @BeforeEach
    fun setUp() {
        atBatRepository = mockk()
        encryptionUtils = mockk()
        gameService = mockk()
        gameLifecycleService = mockk()
        atBatResolutionService = mockk()
        buntService = mockk()
        atBatService =
            AtBatService(
                atBatRepository,
                encryptionUtils,
                gameService,
                gameLifecycleService,
                atBatResolutionService,
                buntService,
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
        every { gameLifecycleService.updateWithPitcherNumberSubmission(any(), any()) } returns Unit
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
    fun `batterNumberSubmitted should dispatch a SWING submission to AtBatResolutionService`() {
        val gameId = 1
        val game = Game().apply { id = gameId; currentAtBatId = 9 }
        val pendingAtBat = AtBat().apply { id = 9; pitcherNumberSubmission = "encrypted-pitcher-42" }
        val resolvedAtBat = AtBat().apply { id = 9; atBatFinished = true }

        every { gameService.getGameById(gameId) } returns game
        every { atBatRepository.getAtBatById(9) } returns pendingAtBat
        every { encryptionUtils.decrypt("encrypted-pitcher-42") } returns "42"
        every {
            atBatResolutionService.resolveSwing(pendingAtBat, game, SubmissionType.SWING, 55, "42")
        } returns resolvedAtBat

        val result = atBatService.batterNumberSubmitted(gameId, "batterUser", 55, SubmissionType.SWING)

        assertEquals(resolvedAtBat, result)
        verify { atBatResolutionService.resolveSwing(pendingAtBat, game, SubmissionType.SWING, 55, "42") }
    }

    @Test
    fun `batterNumberSubmitted should dispatch a BUNT submission to BuntService`() {
        val gameId = 1
        val game = Game().apply { id = gameId; currentAtBatId = 9 }
        val pendingAtBat = AtBat().apply { id = 9; pitcherNumberSubmission = "encrypted-pitcher-42" }
        val resolvedAtBat = AtBat().apply { id = 9; atBatFinished = true }

        every { gameService.getGameById(gameId) } returns game
        every { atBatRepository.getAtBatById(9) } returns pendingAtBat
        every { encryptionUtils.decrypt("encrypted-pitcher-42") } returns "42"
        every {
            buntService.resolveBunt(pendingAtBat, game, SubmissionType.BUNT, 55, "42")
        } returns resolvedAtBat

        val result = atBatService.batterNumberSubmitted(gameId, "batterUser", 55, SubmissionType.BUNT)

        assertEquals(resolvedAtBat, result)
        verify { buntService.resolveBunt(pendingAtBat, game, SubmissionType.BUNT, 55, "42") }
    }

    @Test
    fun `batterNumberSubmitted should dispatch a STEAL submission to AtBatResolutionService`() {
        val gameId = 1
        val game = Game().apply { id = gameId; currentAtBatId = 9 }
        val pendingAtBat = AtBat().apply { id = 9; pitcherNumberSubmission = "encrypted-pitcher-42" }
        val resolvedAtBat = AtBat().apply { id = 9; atBatFinished = true }

        every { gameService.getGameById(gameId) } returns game
        every { atBatRepository.getAtBatById(9) } returns pendingAtBat
        every { encryptionUtils.decrypt("encrypted-pitcher-42") } returns "42"
        every {
            atBatResolutionService.resolveSteal(pendingAtBat, SubmissionType.STEAL, 17, "42")
        } returns resolvedAtBat

        val result = atBatService.batterNumberSubmitted(gameId, "batterUser", 17, SubmissionType.STEAL)

        assertEquals(resolvedAtBat, result)
        verify { atBatResolutionService.resolveSteal(pendingAtBat, SubmissionType.STEAL, 17, "42") }
    }

    @Test
    fun `batterNumberSubmitted should dispatch an intentional walk to AtBatResolutionService`() {
        val gameId = 1
        val game = Game().apply { id = gameId; currentAtBatId = 9 }
        val pendingAtBat =
            AtBat().apply {
                id = 9
                pitcherNumberSubmission = "encrypted-pitcher-42"
                submissionType = SubmissionType.INTENTIONAL_WALK
            }
        val resolvedAtBat = AtBat().apply { id = 9; atBatFinished = true }

        every { gameService.getGameById(gameId) } returns game
        every { atBatRepository.getAtBatById(9) } returns pendingAtBat
        every { encryptionUtils.decrypt("encrypted-pitcher-42") } returns "42"
        every {
            atBatResolutionService.resolveIntentionalWalk(pendingAtBat, game, SubmissionType.SWING, 55, "42")
        } returns resolvedAtBat

        val result = atBatService.batterNumberSubmitted(gameId, "batterUser", 55, SubmissionType.SWING)

        assertEquals(resolvedAtBat, result)
        verify { atBatResolutionService.resolveIntentionalWalk(pendingAtBat, game, SubmissionType.SWING, 55, "42") }
    }
}
