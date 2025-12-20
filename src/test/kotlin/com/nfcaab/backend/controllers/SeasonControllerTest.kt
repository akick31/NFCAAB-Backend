package com.nfcaab.backend.controllers

import com.nfcaab.backend.service.nfcaab.SeasonService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SeasonControllerTest {
    private lateinit var seasonService: SeasonService
    private lateinit var seasonController: SeasonController

    @BeforeEach
    fun setUp() {
        seasonService = mockk()
        seasonController = SeasonController(seasonService)
    }

    @Test
    fun `startSeason should call service`() {
        every { seasonService.startSeason() } returns Unit

        seasonController.startSeason()

        verify { seasonService.startSeason() }
    }

    @Test
    fun `getCurrentSeason should return current season`() {
        val expectedSeason = com.nfcaab.backend.model.Season().apply {
            seasonNumber = 2024
        }

        every { seasonService.getCurrentSeason() } returns expectedSeason

        val result = seasonController.getCurrentSeason()

        assertEquals(expectedSeason, result)
        verify { seasonService.getCurrentSeason() }
    }

    @Test
    fun `getCurrentWeek should return current week number`() {
        val expectedWeek = 5

        every { seasonService.getCurrentWeek() } returns expectedWeek

        val result = seasonController.getCurrentWeek()

        assertEquals(expectedWeek, result)
        verify { seasonService.getCurrentWeek() }
    }
}

