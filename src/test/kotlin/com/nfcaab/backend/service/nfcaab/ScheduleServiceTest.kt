package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.model.Schedule
import com.nfcaab.backend.model.Season
import com.nfcaab.backend.repositories.ScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ScheduleServiceTest {
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var seasonService: SeasonService
    private lateinit var scheduleService: ScheduleService

    @BeforeEach
    fun setUp() {
        scheduleRepository = mockk()
        seasonService = mockk()
        scheduleService = ScheduleService(seasonService, scheduleRepository)
    }

    @Test
    fun `getTeamOpponent should return opponent team name`() {
        val team = "Team A"
        val expectedOpponent = "Team B"
        val season = Season().apply { seasonNumber = 2024 }

        every { seasonService.getCurrentSeason() } returns season
        every { seasonService.getCurrentWeek() } returns 1
        every { scheduleRepository.getTeamOpponent(2024, 1, team) } returns expectedOpponent

        val result = scheduleService.getTeamOpponent(team)

        assertEquals(expectedOpponent, result)
        verify { scheduleRepository.getTeamOpponent(2024, 1, team) }
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

        every { scheduleRepository.getScheduleBySeasonAndTeam(season, team) } returns listOf(schedule)

        val result = scheduleService.getScheduleBySeasonAndTeam(season, team)

        assertEquals(listOf(schedule), result)
        verify { scheduleRepository.getScheduleBySeasonAndTeam(season, team) }
    }
}

