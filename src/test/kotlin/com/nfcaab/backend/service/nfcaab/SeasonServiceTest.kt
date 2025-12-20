package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.model.Season
import com.nfcaab.backend.repositories.SeasonRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SeasonServiceTest {
    private lateinit var seasonRepository: SeasonRepository
    private lateinit var seasonService: SeasonService

    @BeforeEach
    fun setUp() {
        seasonRepository = mockk()
        seasonService = SeasonService(seasonRepository)
    }

    @Test
    fun `startSeason should create new season`() {
        val expectedResult = "Season started successfully"

        every { seasonRepository.findTopByOrderBySeasonNumberDesc() } returns null
        every { seasonRepository.save(any()) } returns mockk<Season>()

        val result = seasonService.startSeason()

        assertEquals(expectedResult, result)
        verify { seasonRepository.save(any()) }
    }

    @Test
    fun `getCurrentSeason should return current season number`() {
        val season = Season().apply {
            seasonNumber = 2024
            currentWeek = 5
        }

        every { seasonRepository.findTopByOrderBySeasonNumberDesc() } returns season

        val result = seasonService.getCurrentSeason()

        assertEquals(2024, result)
        verify { seasonRepository.findTopByOrderBySeasonNumberDesc() }
    }

    @Test
    fun `getCurrentWeek should return current week number`() {
        val season = Season().apply {
            seasonNumber = 2024
            currentWeek = 5
        }

        every { seasonRepository.findTopByOrderBySeasonNumberDesc() } returns season

        val result = seasonService.getCurrentWeek()

        assertEquals(5, result)
        verify { seasonRepository.findTopByOrderBySeasonNumberDesc() }
    }
}

