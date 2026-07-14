package com.nfcaab.backend.controllers

import com.nfcaab.backend.model.Schedule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.nfcaab.backend.service.schedule.ScheduleService

class ScheduleControllerTest {
    private lateinit var scheduleService: ScheduleService
    private lateinit var scheduleController: ScheduleController

    @BeforeEach
    fun setUp() {
        scheduleService = mockk()
        scheduleController = ScheduleController(scheduleService)
    }

    @Test
    fun `getTeamOpponent should return opponent team name`() {
        val team = "Team A"
        val expectedOpponent = "Team B"

        every { scheduleService.getTeamOpponent(team) } returns expectedOpponent

        val result = scheduleController.getTeamOpponent(team)

        assertEquals(expectedOpponent, result)
        verify { scheduleService.getTeamOpponent(team) }
    }

    @Test
    fun `getScheduleBySeasonAndTeam should return schedule list`() {
        val season = 2024
        val team = "Team A"
        val schedules = listOf(
            Schedule().apply {
                this.season = season
                this.homeTeam = team
                this.awayTeam = "Team B"
            }
        )

        every { scheduleService.getScheduleBySeasonAndTeam(season, team) } returns schedules

        val result = scheduleController.getScheduleBySeasonAndTeam(season, team)

        assertEquals(schedules, result)
        verify { scheduleService.getScheduleBySeasonAndTeam(season, team) }
    }
}

