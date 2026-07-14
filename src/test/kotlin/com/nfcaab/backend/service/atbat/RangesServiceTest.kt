package com.nfcaab.backend.service.atbat

import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Player.BatterArchetype
import com.nfcaab.backend.model.Player.PitcherArchetype
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
        val batterArchetype = BatterArchetype.POWER
        val pitcherArchetype = PitcherArchetype.STRIKEOUT
        val difference = 5
        val range = Ranges().apply { result = Game.Scenario.SINGLE }

        every {
            rangesRepository.getNormalResult(submissionType, batterArchetype, pitcherArchetype, difference.toString())
        } returns range

        val result = rangesService.getResult(submissionType, batterArchetype, pitcherArchetype, difference)

        assertEquals(range, result)
        verify {
            rangesRepository.getNormalResult(submissionType, batterArchetype, pitcherArchetype, difference.toString())
        }
    }

    @Test
    fun `getResult should throw exception when not found`() {
        val submissionType = SubmissionType.SWING
        val batterArchetype = BatterArchetype.POWER
        val pitcherArchetype = PitcherArchetype.STRIKEOUT
        val difference = 5

        every {
            rangesRepository.getNormalResult(submissionType, batterArchetype, pitcherArchetype, difference.toString())
        } returns null

        assertThrows(ResultNotFoundException::class.java) {
            rangesService.getResult(submissionType, batterArchetype, pitcherArchetype, difference)
        }
    }

    @Test
    fun `getBuntResult should call getResult with BUNT submission type and the given archetypes`() {
        val batterArchetype = BatterArchetype.CONTACT
        val pitcherArchetype = PitcherArchetype.CONTROL
        val difference = 5
        val range = Ranges().apply { result = Game.Scenario.SINGLE }

        every {
            rangesRepository.getNormalResult(SubmissionType.BUNT, batterArchetype, pitcherArchetype, difference.toString())
        } returns range

        val result = rangesService.getBuntResult(batterArchetype, pitcherArchetype, difference)

        assertEquals(range, result)
        verify {
            rangesRepository.getNormalResult(SubmissionType.BUNT, batterArchetype, pitcherArchetype, difference.toString())
        }
    }

    @Test
    fun `getStealResult should call getResult with STEAL submission type and the given archetypes`() {
        val batterArchetype = BatterArchetype.SPEEDY
        val pitcherArchetype = PitcherArchetype.NEUTRAL
        val difference = 5
        val range = Ranges().apply { result = Game.Scenario.STEAL_SUCCESS }

        every {
            rangesRepository.getNormalResult(SubmissionType.STEAL, batterArchetype, pitcherArchetype, difference.toString())
        } returns range

        val result = rangesService.getStealResult(batterArchetype, pitcherArchetype, difference)

        assertEquals(range, result)
        verify {
            rangesRepository.getNormalResult(SubmissionType.STEAL, batterArchetype, pitcherArchetype, difference.toString())
        }
    }
}
