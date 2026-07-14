package com.nfcaab.backend.service.schedule

import com.nfcaab.backend.model.Season
import com.nfcaab.backend.repositories.SeasonRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.nfcaab.backend.service.player.PlayerService

class SeasonServiceTest {
    private lateinit var seasonRepository: SeasonRepository
    private lateinit var playerService: PlayerService
    private lateinit var seasonService: SeasonService

    @BeforeEach
    fun setUp() {
        seasonRepository = mockk()
        playerService = mockk()
        seasonService = SeasonService(seasonRepository, playerService)
    }

    @Test
    fun `startSeason should create new season`() {
        every { seasonRepository.getPreviousSeason() } returns null
        every { seasonRepository.save(any()) } returns mockk<Season>()

        seasonService.startSeason()

        verify { seasonRepository.getPreviousSeason() }
        verify { seasonRepository.save(any()) }
    }

    @Test
    fun `startSeason should roll over player eligibility when a previous season exists`() {
        val previousSeason = Season().apply { seasonNumber = 2024 }
        every { seasonRepository.getPreviousSeason() } returns previousSeason
        every { seasonRepository.save(any()) } returns mockk<Season>()
        every { playerService.rolloverEligibilityForNewSeason() } returns emptyList()

        seasonService.startSeason()

        verify { playerService.rolloverEligibilityForNewSeason() }
    }

    @Test
    fun `getCurrentSeason should return current season`() {
        val season = Season().apply {
            seasonNumber = 2024
            currentWeek = 5
        }

        every { seasonRepository.getCurrentSeason() } returns season

        val result = seasonService.getCurrentSeason()

        assertEquals(season, result)
        assertEquals(2024, result.seasonNumber)
        verify { seasonRepository.getCurrentSeason() }
    }

    @Test
    fun `getCurrentWeek should return current week number`() {
        val season = Season().apply {
            seasonNumber = 2024
            currentWeek = 5
        }

        every { seasonRepository.getCurrentSeason() } returns season

        val result = seasonService.getCurrentWeek()

        assertEquals(5, result)
        verify { seasonRepository.getCurrentSeason() }
    }
}

