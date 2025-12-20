package com.nfcaab.backend.controllers

import com.nfcaab.backend.model.Team
import com.nfcaab.backend.service.nfcaab.TeamService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TeamControllerTest {
    private lateinit var teamService: TeamService
    private lateinit var teamController: TeamController

    @BeforeEach
    fun setUp() {
        teamService = mockk()
        teamController = TeamController(teamService)
    }

    @Test
    fun `getTeamById should return team`() {
        val id = 1
        val team = Team().apply {
            this.id = id
            name = "Team A"
        }

        every { teamService.getTeamById(id) } returns team

        val result = teamController.getTeamById(id)

        assertEquals(team, result)
        verify { teamService.getTeamById(id) }
    }

    @Test
    fun `getAllTeams should return list of teams`() {
        val teams = listOf(
            Team().apply { name = "Team A" },
            Team().apply { name = "Team B" },
        )

        every { teamService.getAllTeams() } returns teams

        val result = teamController.getAllTeams()

        assertEquals(teams, result)
        verify { teamService.getAllTeams() }
    }

    @Test
    fun `getTeamByName should return team`() {
        val name = "Team A"
        val team = Team().apply {
            this.name = name
        }

        every { teamService.getTeamByName(name) } returns team

        val result = teamController.getTeamByName(name)

        assertEquals(team, result)
        verify { teamService.getTeamByName(name) }
    }

    @Test
    fun `createTeam should return created team`() {
        val team = Team().apply {
            name = "Team A"
        }

        every { teamService.createTeam(team) } returns team

        val result = teamController.createTeam(team)

        assertEquals(team, result)
        verify { teamService.createTeam(team) }
    }

    @Test
    fun `updateTeam should return updated team`() {
        val team = Team().apply {
            id = 1
            name = "Team A"
        }

        every { teamService.updateTeam(team) } returns team

        val result = teamController.updateTeam(team)

        assertEquals(team, result)
        verify { teamService.updateTeam(team) }
    }

    @Test
    fun `hireCoach should return team`() = runBlocking {
        val team = "Team A"
        val discordId = "discord123"
        val updatedTeam = Team().apply {
            name = team
            coachDiscordId = discordId
        }

        coEvery { teamService.hireCoach(team, discordId) } returns updatedTeam

        val result = teamController.hireCoach(team, discordId)

        assertEquals(updatedTeam, result)
        coVerify { teamService.hireCoach(team, discordId) }
    }

    @Test
    fun `hireInterimCoach should return team`() = runBlocking {
        val team = "Team A"
        val discordId = "discord123"
        val processedBy = "admin"
        val updatedTeam = Team().apply {
            name = team
            coachDiscordId = discordId
        }

        coEvery { teamService.hireInterimCoach(team, discordId, processedBy) } returns updatedTeam

        val result = teamController.hireInterimCoach(team, discordId, processedBy)

        assertEquals(updatedTeam, result)
        coVerify { teamService.hireInterimCoach(team, discordId, processedBy) }
    }

    @Test
    fun `fireCoach should return team`() {
        val team = "Team A"
        val updatedTeam = Team().apply {
            name = team
            coachDiscordId = null
        }

        every { teamService.fireCoach(team) } returns updatedTeam

        val result = teamController.fireCoach(team)

        assertEquals(updatedTeam, result)
        verify { teamService.fireCoach(team) }
    }

    @Test
    fun `getOpenTeams should return list of team names`() {
        val teamNames = listOf("Team A", "Team B")

        every { teamService.getOpenTeams() } returns teamNames

        val result = teamController.getOpenTeams()

        assertEquals(teamNames, result)
        verify { teamService.getOpenTeams() }
    }

    @Test
    fun `deleteTeam should return HttpStatus`() {
        val id = 1
        every { teamService.deleteTeam(id) } returns org.springframework.http.HttpStatus.OK

        val result = teamController.deleteTeam(id)

        assertEquals(org.springframework.http.HttpStatus.OK, result)
        verify { teamService.deleteTeam(id) }
    }
}

