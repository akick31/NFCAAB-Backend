package com.nfcaab.backend.controllers

import com.nfcaab.backend.model.Schedule
import com.nfcaab.backend.service.nfcaab.ScheduleService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
    fun `getScheduleBySeasonAndTeam should return schedule`() {
        val season = 2024
        val team = "Team A"
        val schedule = Schedule().apply {
            this.season = season
            this.homeTeam = team
            this.awayTeam = "Team B"
        }

        every { scheduleService.getScheduleBySeasonAndTeam(season, team) } returns schedule

        val result = scheduleController.getScheduleBySeasonAndTeam(season, team)

        assertEquals(schedule, result)
        verify { scheduleService.getScheduleBySeasonAndTeam(season, team) }
    }
}

