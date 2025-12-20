package com.nfcaab.backend.controllers

import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.service.discord.DiscordService
import com.nfcaab.backend.service.nfcaab.AtBatService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DiscordControllerTest {
    private lateinit var discordService: DiscordService
    private lateinit var atBatService: AtBatService
    private lateinit var discordController: DiscordController

    @BeforeEach
    fun setUp() {
        discordService = mockk()
        atBatService = mockk()
        discordController = DiscordController(discordService, atBatService)
    }

    @Test
    fun `submitPitcherNumber should return AtBat`() {
        val gameId = 1
        val pitcherSubmitter = "pitcherUser"
        val pitcherNumberSubmission = 42
        val submissionType: SubmissionType? = null

        val expectedAtBat = AtBat().apply {
            id = 1
            gameId = gameId
            pitcherSubmitter = pitcherSubmitter
            pitcherNumberSubmission = pitcherNumberSubmission.toString()
        }

        every {
            atBatService.pitchingNumberSubmitted(
                gameId,
                pitcherSubmitter,
                pitcherNumberSubmission,
                submissionType,
            )
        } returns expectedAtBat

        val result = discordController.submitPitcherNumber(
            gameId,
            pitcherSubmitter,
            pitcherNumberSubmission,
            submissionType,
        )

        assertNotNull(result)
        assertEquals(expectedAtBat, result)
        verify {
            atBatService.pitchingNumberSubmitted(
                gameId,
                pitcherSubmitter,
                pitcherNumberSubmission,
                submissionType,
            )
        }
    }

    @Test
    fun `submitBatterNumber should return AtBat`() {
        val gameId = 1
        val batterSubmitter = "batterUser"
        val batterNumberSubmission = 55
        val submissionType = SubmissionType.SWING

        val expectedAtBat = AtBat().apply {
            id = 1
            gameId = gameId
            batterSubmitter = batterSubmitter
            batterNumberSubmission = batterNumberSubmission
            submissionType = submissionType
        }

        every {
            atBatService.batterNumberSubmitted(
                gameId,
                batterSubmitter,
                batterNumberSubmission,
                submissionType,
            )
        } returns expectedAtBat

        val result = discordController.submitBatterNumber(
            gameId,
            batterSubmitter,
            batterNumberSubmission,
            submissionType,
        )

        assertNotNull(result)
        assertEquals(expectedAtBat, result)
        verify {
            atBatService.batterNumberSubmitted(
                gameId,
                batterSubmitter,
                batterNumberSubmission,
                submissionType,
            )
        }
    }

    @Test
    fun `submitPitcherNumber with intentional walk should return AtBat`() {
        val gameId = 1
        val pitcherSubmitter = "pitcherUser"
        val pitcherNumberSubmission = 42
        val submissionType = SubmissionType.INTENTIONAL_WALK

        val expectedAtBat = AtBat().apply {
            id = 1
            gameId = gameId
            pitcherSubmitter = pitcherSubmitter
            pitcherNumberSubmission = pitcherNumberSubmission.toString()
            this.submissionType = submissionType
        }

        every {
            atBatService.pitchingNumberSubmitted(
                gameId,
                pitcherSubmitter,
                pitcherNumberSubmission,
                submissionType,
            )
        } returns expectedAtBat

        val result = discordController.submitPitcherNumber(
            gameId,
            pitcherSubmitter,
            pitcherNumberSubmission,
            submissionType,
        )

        assertNotNull(result)
        assertEquals(expectedAtBat, result)
        verify {
            atBatService.pitchingNumberSubmitted(
                gameId,
                pitcherSubmitter,
                pitcherNumberSubmission,
                submissionType,
            )
        }
    }
}

