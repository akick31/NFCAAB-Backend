package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Player.Archetype
import com.nfcaab.backend.model.Ranges
import com.nfcaab.backend.repositories.RangesRepository
import com.nfcaab.backend.util.ResultNotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RangesServiceTest {
    private lateinit var rangesRepository: RangesRepository
    private lateinit var rangesService: RangesService

    @BeforeEach
    fun setUp() {
        rangesRepository = mockk()
        rangesService = RangesService(rangesRepository)
    }

    @Test
    fun `getResult should return range when found`() {
        val submissionType = SubmissionType.SWING
        val batterArchetype = Archetype.POWER
        val pitcherArchetype = Archetype.CONTROL
        val difference = 5
        val range = Ranges().apply {
            result = "SINGLE"
        }

        every {
            rangesRepository.getNormalResult(
                submissionType,
                batterArchetype,
                pitcherArchetype,
                difference.toString()
            )
        } returns range

        val result = rangesService.getResult(submissionType, batterArchetype, pitcherArchetype, difference)

        assertEquals(range, result)
        verify {
            rangesRepository.getNormalResult(
                submissionType,
                batterArchetype,
                pitcherArchetype,
                difference.toString()
            )
        }
    }

    @Test
    fun `getResult should throw exception when not found`() {
        val submissionType = SubmissionType.SWING
        val batterArchetype = Archetype.POWER
        val pitcherArchetype = Archetype.CONTROL
        val difference = 5

        every {
            rangesRepository.getNormalResult(
                submissionType,
                batterArchetype,
                pitcherArchetype,
                difference.toString()
            )
        } returns null

        assertThrows(ResultNotFoundException::class.java) {
            rangesService.getResult(submissionType, batterArchetype, pitcherArchetype, difference)
        }
    }

    @Test
    fun `getBuntResult should call getResult with BUNT submission type`() {
        val difference = 5
        val range = Ranges().apply {
            result = "SAC_BUNT"
        }

        every {
            rangesRepository.getNormalResult(
                SubmissionType.BUNT,
                Archetype.NEUTRAL,
                Archetype.NEUTRAL,
                difference.toString()
            )
        } returns range

        val result = rangesService.getBuntResult(difference)

        assertEquals(range, result)
        verify {
            rangesRepository.getNormalResult(
                SubmissionType.BUNT,
                Archetype.NEUTRAL,
                Archetype.NEUTRAL,
                difference.toString()
            )
        }
    }

    @Test
    fun `getStealResult should call getResult with STEAL submission type`() {
        val difference = 5
        val range = Ranges().apply {
            result = "STOLEN_BASE"
        }

        every {
            rangesRepository.getNormalResult(
                SubmissionType.STEAL,
                Archetype.SPEEDY,
                Archetype.NEUTRAL,
                difference.toString()
            )
        } returns range

        val result = rangesService.getStealResult(difference)

        assertEquals(range, result)
        verify {
            rangesRepository.getNormalResult(
                SubmissionType.STEAL,
                Archetype.SPEEDY,
                Archetype.NEUTRAL,
                difference.toString()
            )
        }
    }
}

