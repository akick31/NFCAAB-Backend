package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.model.Team
import com.nfcaab.backend.repositories.TeamRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TeamServiceTest {
    private lateinit var teamRepository: TeamRepository
    private lateinit var userService: UserService
    private lateinit var newSignupService: NewSignupService
    private lateinit var teamService: TeamService

    @BeforeEach
    fun setUp() {
        teamRepository = mockk()
        userService = mockk()
        newSignupService = mockk()
        teamService = TeamService(teamRepository, userService, newSignupService)
    }

    @Test
    fun `getTeamById should return team`() {
        val id = 1
        val team = Team().apply {
            this.id = id
            name = "Team A"
        }

        @Suppress("UNCHECKED_CAST")
        val optionalTeam = java.util.Optional.of(team) as java.util.Optional<Team?>
        every { teamRepository.findById(id) } returns optionalTeam

        val result = teamService.getTeamById(id)

        assertEquals(team, result)
        verify { teamRepository.findById(id) }
    }

    @Test
    fun `getTeamByName should return team`() {
        val name = "Team A"
        val team = Team().apply {
            this.name = name
        }

        every { teamRepository.getTeamByName(name) } returns team

        val result = teamService.getTeamByName(name)

        assertEquals(team, result)
        verify { teamRepository.getTeamByName(name) }
    }

    @Test
    fun `getAllTeams should return list of teams`() {
        val teams = listOf(
            Team().apply { name = "Team A" },
            Team().apply { name = "Team B" },
        )

        every { teamRepository.findAll() } returns teams

        val result = teamService.getAllTeams()

        assertEquals(teams, result)
        verify { teamRepository.findAll() }
    }

    @Test
    fun `createTeam should save and return team`() {
        val team = Team().apply {
            name = "Team A"
        }

        every { teamRepository.save(team) } returns team

        val result = teamService.createTeam(team)

        assertEquals(team, result)
        verify { teamRepository.save(team) }
    }

    @Test
    fun `updateTeam should save and return updated team`() {
        val team = Team().apply {
            id = 1
            name = "Team A"
        }

        every { teamRepository.save(team) } returns team

        val result = teamService.updateTeam(team)

        assertEquals(team, result)
        verify { teamRepository.save(team) }
    }

    @Test
    fun `getOpenTeams should return team names without coaches`() {
        val teamNames = listOf("Team A", "Team B")

        every { teamRepository.getOpenTeams() } returns teamNames

        val result = teamService.getOpenTeams()

        assertEquals(teamNames, result)
        verify { teamRepository.getOpenTeams() }
    }
}

