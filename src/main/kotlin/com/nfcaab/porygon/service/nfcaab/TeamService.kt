package com.nfcaab.porygon.service.nfcaab

import com.nfcaab.porygon.model.Game
import com.nfcaab.porygon.model.Game.GameType.COLLEGE_WORLD_SERIES
import com.nfcaab.porygon.model.Game.GameType.CONFERENCE_CHAMPIONSHIP
import com.nfcaab.porygon.model.Game.GameType.CONFERENCE_GAME
import com.nfcaab.porygon.model.Game.GameType.REGIONAL
import com.nfcaab.porygon.model.Game.GameType.SUPER_REGIONAL
import com.nfcaab.porygon.model.Team
import com.nfcaab.porygon.repositories.TeamRepository
import com.nfcaab.porygon.service.nfcaab.NewSignupService
import com.nfcaab.porygon.util.NoCoachDiscordIdFoundException
import com.nfcaab.porygon.util.TeamNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class TeamService(
    private val teamRepository: TeamRepository,
    private val userService: UserService,
    private val newSignupService: NewSignupService,
) {
    /**
     * After a game ends, update the team's wins and losses
     * @param game
     */
    fun updateTeamWinsAndLosses(game: Game) {
        val homeTeam = getTeamByName(game.homeTeam)
        val awayTeam = getTeamByName(game.awayTeam)

        if (game.homeScore > game.awayScore) {
            homeTeam.currentWins += 1
            awayTeam.currentLosses += 1
            homeTeam.totalWins += 1
            awayTeam.totalLosses += 1
            if (game.gameType == CONFERENCE_GAME) {
                homeTeam.currentConferenceWins += 1
                awayTeam.currentConferenceLosses += 1
                homeTeam.totalConferenceWins += 1
                awayTeam.totalConferenceLosses += 1
            } else if (game.gameType == CONFERENCE_CHAMPIONSHIP) {
                homeTeam.totalConferenceChampionships += 1
            } else if (game.gameType == REGIONAL && game.seriesGameNumber == 1) {
                homeTeam.totalTournamentAppearances += 1
            } else if (game.gameType == SUPER_REGIONAL && game.seriesGameNumber == 1) {
                homeTeam.totalSuperRegionalAppearances += 1
            } else if (game.gameType == COLLEGE_WORLD_SERIES && game.seriesGameNumber == 1) {
                homeTeam.totalCollegeWorldSeriesAppearances += 1
            }
        } else {
            homeTeam.currentLosses += 1
            awayTeam.currentWins += 1
            homeTeam.totalLosses += 1
            awayTeam.totalWins += 1
            if (game.gameType == CONFERENCE_GAME) {
                homeTeam.currentConferenceLosses += 1
                awayTeam.currentConferenceWins += 1
                homeTeam.totalConferenceLosses += 1
                awayTeam.totalConferenceWins += 1
            } else if (game.gameType == CONFERENCE_CHAMPIONSHIP) {
                awayTeam.totalConferenceChampionships += 1
            } else if (game.gameType == REGIONAL && game.seriesGameNumber == 1) {
                awayTeam.totalTournamentAppearances += 1
            } else if (game.gameType == SUPER_REGIONAL && game.seriesGameNumber == 1) {
                awayTeam.totalSuperRegionalAppearances += 1
            } else if (game.gameType == COLLEGE_WORLD_SERIES && game.seriesGameNumber == 1) {
                awayTeam.totalCollegeWorldSeriesAppearances += 1
            }
        }
        updateTeam(homeTeam)
        updateTeam(awayTeam)
    }

    /**
     * Get a team by its ID
     * @param id
     */
    fun getTeamById(id: Int) =
        teamRepository.findById(id)
            ?: throw TeamNotFoundException("Team not found with ID: $id")

    /**
     * Get all teams
     */
    fun getAllTeams() =
        teamRepository.getAllActiveTeams().ifEmpty {
            throw TeamNotFoundException("No active teams found")
        }

    /**
     * Get a team by its name
     * @param name
     */
    fun getTeamByName(name: String?) =
        teamRepository.getTeamByName(name)
            ?: throw TeamNotFoundException("Team not found with name: $name")

    /**
     * Create a new team
     * @param team
     */
    fun createTeam(team: Team): Team {
        try {
            val newTeam =
                teamRepository.save(
                    Team(
                        name = team.name,
                        shortName = team.shortName,
                        abbreviation = team.abbreviation,
                        subdivision = team.subdivision,
                        logo = team.logo,
                        scorebugLogo = team.scorebugLogo,
                        primaryColor = team.primaryColor,
                        secondaryColor = team.secondaryColor,
                        coachUsername = team.coachUsername,
                        coachName = team.coachName,
                        coachDiscordTag = team.coachDiscordTag,
                        coachDiscordId = team.coachDiscordId,
                        conference = team.conference,
                        ranking = team.ranking,
                        currentWins = team.currentWins,
                        currentLosses = team.currentLosses,
                        currentWinPercentage = team.currentWinPercentage,
                        currentConferenceWins = team.currentConferenceWins,
                        currentConferenceLosses = team.currentConferenceLosses,
                        currentConferenceWinPercentage = team.currentConferenceWinPercentage,
                        currentSeriesWins = team.currentSeriesWins,
                        currentSeriesLosses = team.currentSeriesLosses,
                        currentSeriesPushes = team.currentSeriesPushes,
                        numSeasons = team.numSeasons,
                        totalWins = team.totalWins,
                        totalLosses = team.totalLosses,
                        totalWinPercentage = team.totalWinPercentage,
                        totalConferenceWins = team.totalConferenceWins,
                        totalConferenceLosses = team.totalConferenceLosses,
                        totalConferenceWinPercentage = team.totalConferenceWinPercentage,
                        totalSeriesWins = team.totalSeriesWins,
                        totalSeriesLosses = team.totalSeriesLosses,
                        totalSeriesPushes = team.totalSeriesPushes,
                        totalConferenceChampionships = team.totalConferenceChampionships,
                        totalTournamentAppearances = team.totalTournamentAppearances,
                        totalSuperRegionalAppearances = team.totalSuperRegionalAppearances,
                        totalCollegeWorldSeriesAppearances = team.totalCollegeWorldSeriesAppearances,
                        totalChampionships = team.totalChampionships,
                        lastAcceptedLineup = team.lastAcceptedLineup,
                        taken = team.taken,
                        active = team.active,
                    ),
                )
            return newTeam
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Update a team
     * @param team
     */
    fun updateTeam(team: Team): Team {
        val existingTeam = getTeamByName(team.name)

        existingTeam.apply {
            name = team.name
            shortName = team.shortName
            abbreviation = team.abbreviation
            subdivision = team.subdivision
            logo = team.logo
            scorebugLogo = team.scorebugLogo
            primaryColor = team.primaryColor
            secondaryColor = team.secondaryColor
            coachUsername = team.coachUsername
            coachName = team.coachName
            coachDiscordTag = team.coachDiscordTag
            coachDiscordId = team.coachDiscordId
            conference = team.conference
            ranking = team.ranking
            currentWins = team.currentWins
            currentLosses = team.currentLosses
            currentWinPercentage = team.currentWinPercentage
            currentConferenceWins = team.currentConferenceWins
            currentConferenceLosses = team.currentConferenceLosses
            currentConferenceWinPercentage = team.currentConferenceWinPercentage
            currentSeriesWins = team.currentSeriesWins
            currentSeriesLosses = team.currentSeriesLosses
            currentSeriesPushes = team.currentSeriesPushes
            numSeasons = team.numSeasons
            totalWins = team.totalWins
            totalLosses = team.totalLosses
            totalWinPercentage = team.totalWinPercentage
            totalConferenceWins = team.totalConferenceWins
            totalConferenceLosses = team.totalConferenceLosses
            totalConferenceWinPercentage = team.totalConferenceWinPercentage
            totalSeriesWins = team.totalSeriesWins
            totalSeriesLosses = team.totalSeriesLosses
            totalSeriesPushes = team.totalSeriesPushes
            totalConferenceChampionships = team.totalConferenceChampionships
            totalTournamentAppearances = team.totalTournamentAppearances
            totalSuperRegionalAppearances = team.totalSuperRegionalAppearances
            totalCollegeWorldSeriesAppearances = team.totalCollegeWorldSeriesAppearances
            totalChampionships = team.totalChampionships
            lastAcceptedLineup = team.lastAcceptedLineup
            taken = team.taken
            active = team.active
        }
        teamRepository.save(existingTeam)
        return existingTeam
    }

    /**
     * Update team color
     */
    fun updateTeamColor(
        team: String,
        color: String,
    ): Team {
        val existingTeam = getTeamByName(team)
        val hexRegex = Regex("^#([a-fA-F0-9]{3}|[a-fA-F0-9]{4}|[a-fA-F0-9]{6}|[a-fA-F0-9]{8})$")
        if (!hexRegex.matches(color)) {
            throw IllegalArgumentException("Invalid color")
        }

        existingTeam.apply {
            primaryColor = color
        }
        teamRepository.save(existingTeam)
        return existingTeam
    }

    /**
     * Hire a coach for a team
     * @param team
     * @param discordId
     */
    suspend fun hireCoach(
        team: String?,
        discordId: String,
    ): Team {
        val existingTeam = getTeamByName(team)
        val user = userService.getUserDTOByDiscordId(discordId)
        user.team = existingTeam.name
        if (existingTeam.coachUsername != null) {
            fireCoach(existingTeam.name)
        }
        existingTeam.coachUsername = user.username
        existingTeam.coachName = user.coachName
        existingTeam.coachDiscordTag = user.discordTag
        existingTeam.coachDiscordId = discordId

        withContext(Dispatchers.IO) {
            existingTeam.taken = true
            saveTeam(existingTeam)
            userService.updateUser(user)
            val signupObject = newSignupService.getNewSignupByDiscordId(discordId)
            if (signupObject != null) {
                newSignupService.deleteNewSignup(signupObject)
            }
        }
        return existingTeam
    }

    /**
     * Hire an interim coach for a team
     * @param team
     * @param discordId
     * @param processedBy
     */
    suspend fun hireInterimCoach(
        team: String?,
        discordId: String,
        processedBy: String,
    ): Team {
        val existingTeam = getTeamByName(team)
        val user = userService.getUserDTOByDiscordId(discordId)

        existingTeam.coachUsername = user.username
        existingTeam.coachName = user.coachName
        existingTeam.coachDiscordTag = user.discordTag
        existingTeam.coachDiscordId = discordId

        withContext(Dispatchers.IO) {
            saveTeam(existingTeam)
            val signupObject = newSignupService.getNewSignupByDiscordId(discordId)
            if (signupObject != null) {
                newSignupService.deleteNewSignup(signupObject)
            }
        }
        return existingTeam
    }

    /**
     * Fire all coaches for a team
     * @param name
     */
    fun fireCoach(name: String?): Team {
        val existingTeam = getTeamByName(name)
        val coachDiscordId = existingTeam.coachDiscordId ?: throw NoCoachDiscordIdFoundException()
        val user = userService.getUserDTOByDiscordId(coachDiscordId)
        if (user.team == existingTeam.name) {
            user.team = null
            userService.updateUser(user)
        }

        existingTeam.coachUsername = null
        existingTeam.coachName = null
        existingTeam.coachDiscordTag = null
        existingTeam.coachDiscordId = null
        existingTeam.taken = false
        saveTeam(existingTeam)
        return existingTeam
    }

    /**
     * Get open teams
     */
    fun getOpenTeams() = teamRepository.getOpenTeams()

    /**
     * Get all teams in a conference
     */
    fun getTeamsInConference(conference: String) = teamRepository.getTeamsInConference(conference)

    /**
     * Save a team
     * @param team
     */
    private fun saveTeam(team: Team) = teamRepository.save(team)

    /**
     * Delete a team
     * @param id
     */
    fun deleteTeam(id: Int): HttpStatus {
        teamRepository.findById(id) ?: return HttpStatus.NOT_FOUND
        if (!teamRepository.findById(id).isPresent) {
            return HttpStatus.NOT_FOUND
        }
        teamRepository.deleteById(id)
        return HttpStatus.OK
    }
}
