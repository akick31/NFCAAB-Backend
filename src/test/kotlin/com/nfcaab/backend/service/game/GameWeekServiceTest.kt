package com.nfcaab.backend.service.game

import com.nfcaab.backend.dto.requests.StartRequest
import com.nfcaab.backend.enums.team.Subdivision
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Schedule
import com.nfcaab.backend.service.schedule.ScheduleService
import com.nfcaab.backend.util.NoGameFoundException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameWeekServiceTest {
    private lateinit var scheduleService: ScheduleService
    private lateinit var gameLifecycleService: GameLifecycleService
    private lateinit var gameWeekService: GameWeekService

    @BeforeEach
    fun setUp() {
        scheduleService = mockk()
        gameLifecycleService = mockk()
        gameWeekService = GameWeekService(scheduleService, gameLifecycleService)
    }

    private fun scheduleEntry() =
        Schedule().apply {
            season = 2026
            week = 3
            subdivision = Subdivision.NFCAAB
            homeTeam = "Home Team"
            awayTeam = "Away Team"
            gameType = Game.GameType.OUT_OF_CONFERENCE
        }

    @Test
    fun `startWeek should start every scheduled game and mark it started`() =
        runBlocking {
            val schedule = scheduleEntry()
            val startedGame = Game().apply { id = 1; homeTeam = "Home Team"; awayTeam = "Away Team" }

            every { scheduleService.getGamesToStartBySeasonAndWeek(2026, 3) } returns listOf(schedule)
            coEvery { gameLifecycleService.startGame(any<StartRequest>(), 3) } returns startedGame
            every { scheduleService.markGameAsStarted(schedule) } returns Unit

            val result = gameWeekService.startWeek(2026, 3)

            assertEquals(listOf(startedGame), result)
            verify { scheduleService.markGameAsStarted(schedule) }
        }

    @Test
    fun `startWeek should skip a game that fails to start and continue with the rest`() =
        runBlocking {
            val failingSchedule = scheduleEntry()
            val okSchedule = scheduleEntry().apply { homeTeam = "Third Team" }
            val startedGame = Game().apply { id = 2; homeTeam = "Third Team"; awayTeam = "Away Team" }

            every { scheduleService.getGamesToStartBySeasonAndWeek(2026, 3) } returns listOf(failingSchedule, okSchedule)
            coEvery { gameLifecycleService.startGame(match { it.homeTeam == "Home Team" }, 3) } throws RuntimeException("boom")
            coEvery { gameLifecycleService.startGame(match { it.homeTeam == "Third Team" }, 3) } returns startedGame
            every { scheduleService.markGameAsStarted(okSchedule) } returns Unit

            val result = gameWeekService.startWeek(2026, 3)

            assertEquals(listOf(startedGame), result)
            verify(exactly = 0) { scheduleService.markGameAsStarted(failingSchedule) }
        }

    @Test
    fun `startWeek should throw when no games are scheduled`() =
        runBlocking {
            every { scheduleService.getGamesToStartBySeasonAndWeek(2026, 3) } returns null

            assertThrows(NoGameFoundException::class.java) {
                runBlocking { gameWeekService.startWeek(2026, 3) }
            }
        }
}
