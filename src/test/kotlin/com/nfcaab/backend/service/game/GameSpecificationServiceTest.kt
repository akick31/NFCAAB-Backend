package com.nfcaab.backend.service.game

import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.GameStatus
import com.nfcaab.backend.model.Game.GameType
import com.nfcaab.backend.repositories.GameRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.jpa.domain.Specification
import com.nfcaab.backend.service.team.TeamService

class GameSpecificationServiceTest {
    private lateinit var teamService: TeamService
    private lateinit var gameRepository: GameRepository
    private lateinit var gameSpecificationService: GameSpecificationService

    @BeforeEach
    fun setUp() {
        teamService = mockk()
        gameRepository = mockk()
        gameSpecificationService = GameSpecificationService(teamService, gameRepository)
    }

    @Test
    fun `createSpecification should create spec for ONGOING category`() {
        val filters = emptyList<GameSpecificationService.GameFilter>()
        val category = GameSpecificationService.GameCategory.ONGOING
        val conference: String? = null
        val season: Int? = null
        val week: Int? = null

        val spec = gameSpecificationService.createSpecification(filters, category, conference, season, week)

        assertNotNull(spec)
    }

    @Test
    fun `createSpecification should create spec for PAST category`() {
        val filters = emptyList<GameSpecificationService.GameFilter>()
        val category = GameSpecificationService.GameCategory.PAST
        val conference: String? = null
        val season: Int? = null
        val week: Int? = null

        val spec = gameSpecificationService.createSpecification(filters, category, conference, season, week)

        assertNotNull(spec)
    }

    @Test
    fun `createSpecification should create spec for SCRIMMAGE category`() {
        val filters = emptyList<GameSpecificationService.GameFilter>()
        val category = GameSpecificationService.GameCategory.SCRIMMAGE
        val conference: String? = null
        val season: Int? = null
        val week: Int? = null

        val spec = gameSpecificationService.createSpecification(filters, category, conference, season, week)

        assertNotNull(spec)
    }

    @Test
    fun `createSpecification should create spec with conference filter`() {
        val filters = emptyList<GameSpecificationService.GameFilter>()
        val category: GameSpecificationService.GameCategory? = null
        val conference = "SEC"
        val season: Int? = null
        val week: Int? = null

        val spec = gameSpecificationService.createSpecification(filters, category, conference, season, week)

        assertNotNull(spec)
    }

    @Test
    fun `createSpecification should create spec with season and week`() {
        val filters = emptyList<GameSpecificationService.GameFilter>()
        val category: GameSpecificationService.GameCategory? = null
        val conference: String? = null
        val season = 2024
        val week = 5

        val spec = gameSpecificationService.createSpecification(filters, category, conference, season, week)

        assertNotNull(spec)
    }

    @Test
    fun `createSpecification should create spec with game type filter`() {
        val filters = listOf(GameSpecificationService.GameFilter.CONFERENCE_GAME)
        val category: GameSpecificationService.GameCategory? = null
        val conference: String? = null
        val season: Int? = null
        val week: Int? = null

        val spec = gameSpecificationService.createSpecification(filters, category, conference, season, week)

        assertNotNull(spec)
    }
}

