package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.enums.team.Conference
import com.nfcaab.backend.enums.team.Subdivision
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.GameStats
import com.nfcaab.backend.repositories.ConferenceStatsRepository
import com.nfcaab.backend.repositories.SeasonStatsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConferenceStatsServiceTest {
    private lateinit var conferenceStatsService: ConferenceStatsService
    private val conferenceStatsRepository: ConferenceStatsRepository = mockk()
    private val seasonStatsRepository: SeasonStatsRepository = mockk()

    @BeforeEach
    fun setup() {
        conferenceStatsService = ConferenceStatsService(
            conferenceStatsRepository,
            seasonStatsRepository,
        )
    }

    @Test
    fun `test updateConferenceStatsAfterGame creates new conference stats`() {
        val game = Game().apply {
            id = 1
            season = 1
            subdivision = Subdivision.NFCAAB
        }

        val gameStats = GameStats().apply {
            gameId = 1
            team = "Team A"
        }

        every { conferenceStatsRepository.findBySubdivisionAndConferenceAndSeasonNumber(any(), any(), any()) } returns null
        every { seasonStatsRepository.findBySeasonNumberOrderByTeamAsc(1) } returns emptyList()
        every { conferenceStatsRepository.save(any()) } returns mockk()

        conferenceStatsService.updateConferenceStatsAfterGame(game, gameStats, Conference.ACC)

        verify { conferenceStatsRepository.save(any()) }
    }
}

