package com.nfcaab.backend.service.stats

import com.nfcaab.backend.enums.team.Subdivision
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.GameStats
import com.nfcaab.backend.model.SeasonStats
import com.nfcaab.backend.model.Team
import com.nfcaab.backend.repositories.SeasonStatsRepository
import com.nfcaab.backend.repositories.GameStatsRepository
import com.nfcaab.backend.repositories.TeamRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SeasonStatsServiceTest {
    private lateinit var seasonStatsService: SeasonStatsService
    private val seasonStatsRepository: SeasonStatsRepository = mockk()
    private val gameStatsRepository: GameStatsRepository = mockk()
    private val teamRepository: TeamRepository = mockk()

    @BeforeEach
    fun setup() {
        seasonStatsService = SeasonStatsService(
            seasonStatsRepository,
            gameStatsRepository,
            teamRepository,
        )
    }

    @Test
    fun `test updateSeasonStatsAfterGame creates new season stats`() {
        val game = Game().apply {
            id = 1
            season = 1
            subdivision = Subdivision.NFCAAB
        }

        val gameStats = GameStats().apply {
            gameId = 1
            team = "Team A"
            score = 5
            atBats = 30
            hits = 10
            runs = 5
        }

        every { seasonStatsRepository.findByTeamAndSeasonNumber("Team A", 1) } returns null
        every { teamRepository.getTeamByName("Team A") } returns Team().apply { name = "Team A" }
        every { gameStatsRepository.findByTeamAndSeason("Team A", 1) } returns listOf(gameStats)
        every { gameStatsRepository.findByGameId(1) } returns listOf(gameStats)
        every { seasonStatsRepository.save(any()) } returns mockk()

        seasonStatsService.updateSeasonStatsAfterGame(game, gameStats)

        verify { seasonStatsRepository.save(any()) }
    }

    @Test
    fun `test updateSeasonStatsAfterGame updates existing season stats`() {
        val game = Game().apply {
            id = 1
            season = 1
            subdivision = Subdivision.NFCAAB
        }

        val gameStats = GameStats().apply {
            gameId = 1
            team = "Team A"
            score = 5
            atBats = 30
            hits = 10
            runs = 5
        }

        val existingSeasonStats = SeasonStats().apply {
            team = "Team A"
            seasonNumber = 1
        }

        every { seasonStatsRepository.findByTeamAndSeasonNumber("Team A", 1) } returns existingSeasonStats
        every { gameStatsRepository.findByTeamAndSeason("Team A", 1) } returns listOf(gameStats)
        every { gameStatsRepository.findByGameId(1) } returns listOf(gameStats)
        every { seasonStatsRepository.save(any()) } returns mockk()

        seasonStatsService.updateSeasonStatsAfterGame(game, gameStats)

        verify { seasonStatsRepository.save(any()) }
    }
}

