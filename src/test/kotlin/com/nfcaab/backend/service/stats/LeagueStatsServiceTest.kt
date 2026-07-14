package com.nfcaab.backend.service.stats

import com.nfcaab.backend.enums.team.Subdivision
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.GameStats
import com.nfcaab.backend.repositories.LeagueStatsRepository
import com.nfcaab.backend.repositories.SeasonStatsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LeagueStatsServiceTest {
    private lateinit var leagueStatsService: LeagueStatsService
    private val leagueStatsRepository: LeagueStatsRepository = mockk()
    private val seasonStatsRepository: SeasonStatsRepository = mockk()

    @BeforeEach
    fun setup() {
        leagueStatsService = LeagueStatsService(
            leagueStatsRepository,
            seasonStatsRepository,
        )
    }

    @Test
    fun `test updateLeagueStatsAfterGame creates new league stats`() {
        val game = Game().apply {
            id = 1
            season = 1
            subdivision = Subdivision.NFCAAB
        }

        val gameStats = GameStats().apply {
            gameId = 1
            team = "Team A"
        }

        every { leagueStatsRepository.findBySubdivisionAndSeasonNumber(any(), any()) } returns null
        every { seasonStatsRepository.findBySeasonNumberOrderByTeamAsc(1) } returns emptyList()
        every { leagueStatsRepository.save(any()) } returns mockk()

        leagueStatsService.updateLeagueStatsAfterGame(game, gameStats)

        verify { leagueStatsRepository.save(any()) }
    }
}

