package com.nfcaab.backend.controllers

import com.nfcaab.backend.model.Team
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import com.nfcaab.backend.service.team.TeamService

@RestController
@RequestMapping("/team")
class TeamController(
    private var teamService: TeamService,
) {
    /**
     * Get a team by id
     * @param id
     */
    @GetMapping("/id")
    fun getTeamById(
        @RequestParam id: Int,
    ) = teamService.getTeamById(id)

    /**
     * Get all teams
     * @return
     */
    @GetMapping("")
    fun getAllTeams() = teamService.getAllTeams()

    /**
     * Get a team by name
     * @param name
     * @return
     */
    @GetMapping("/name")
    fun getTeamByName(
        @RequestParam name: String?,
    ) = teamService.getTeamByName(name)

    /**
     * Create a team
     * @param team
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("")
    fun createTeam(
        @RequestBody team: Team,
    ) = teamService.createTeam(team)

    /**
     * Update a team
     * @param team
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("")
    fun updateTeam(
        @RequestBody team: Team,
    ) = teamService.updateTeam(team)

    /**
     * Hire a coach for a team
     * @param team
     * @param discordId
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/hire")
    suspend fun hireCoach(
        @RequestParam team: String?,
        @RequestParam discordId: String,
    ) = teamService.hireCoach(team, discordId)

    /**
     * Hire an interim coach for a team
     * @param team
     * @param discordId
     * @param processedBy
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/hire/interim")
    suspend fun hireInterimCoach(
        @RequestParam team: String,
        @RequestParam discordId: String,
        @RequestParam processedBy: String,
    ) = teamService.hireInterimCoach(team, discordId, processedBy)

    /**
     * Fire all coaches for a team
     * @param team
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/fire")
    fun fireCoach(
        @RequestParam team: String,
    ) = teamService.fireCoach(team)

    /**
     * Get open teams
     */
    @GetMapping("/open")
    fun getOpenTeams() = teamService.getOpenTeams()

    /**
     * Delete a team
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("")
    fun deleteTeam(
        @RequestParam id: Int,
    ) = teamService.deleteTeam(id)
}
