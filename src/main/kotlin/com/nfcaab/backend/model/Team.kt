package com.nfcaab.backend.model

import com.nfcaab.backend.enums.team.Conference
import com.nfcaab.backend.enums.team.Subdivision
import org.hibernate.annotations.Type
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "teams", schema = "porygon")
class Team {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Int? = null

    @Column(name = "name")
    lateinit var name: String

    @Column(name = "short_name")
    lateinit var shortName: String

    @Column(name = "abbreviation")
    lateinit var abbreviation: String

    @Column(name = "subdivision")
    var subdivision: Subdivision = Subdivision.NFCAAB

    @Column(name = "logo")
    var logo: String? = null

    @Column(name = "scorebug_logo")
    var scorebugLogo: String? = null

    @Column(name = "primary_color")
    var primaryColor: String = "#000000"

    @Column(name = "secondary_color")
    var secondaryColor: String = "#FFFFFF"

    @Column(name = "coach_username")
    var coachUsername: String? = null

    @Column(name = "coach_name")
    var coachName: String? = null

    @Column(name = "coach_discord_tag")
    var coachDiscordTag: String? = null

    @Column(name = "coach_discord_id")
    var coachDiscordId: String? = null

    @Column(name = "conference")
    var conference: Conference? = null

    @Column(name = "ranking")
    var ranking: Int? = null

    @Column(name = "current_wins")
    var currentWins: Int = 0

    @Column(name = "current_losses")
    var currentLosses: Int = 0

    @Column(name = "current_win_percentage")
    var currentWinPercentage: Float = 0.0F

    @Column(name = "current_conference_wins")
    var currentConferenceWins: Int = 0

    @Column(name = "current_conference_losses")
    var currentConferenceLosses: Int = 0

    @Column(name = "current_conference_win_percentage")
    var currentConferenceWinPercentage: Float = 0.0F

    @Column(name = "current_series_wins")
    var currentSeriesWins: Int = 0

    @Column(name = "current_series_losses")
    var currentSeriesLosses: Int = 0

    @Column(name = "current_series_pushes")
    var currentSeriesPushes: Int = 0

    @Column(name = "num_seasons")
    var numSeasons: Int = 0

    @Column(name = "total_wins")
    var totalWins: Int = 0

    @Column(name = "total_losses")
    var totalLosses: Int = 0

    @Column(name = "total_win_percentage")
    var totalWinPercentage: Float = 0.0F

    @Column(name = "total_conference_wins")
    var totalConferenceWins: Int = 0

    @Column(name = "total_conference_losses")
    var totalConferenceLosses: Int = 0

    @Column(name = "total_conference_win_percentage")
    var totalConferenceWinPercentage: Float = 0.0F

    @Column(name = "total_series_wins")
    var totalSeriesWins: Int = 0

    @Column(name = "total_series_losses")
    var totalSeriesLosses: Int = 0

    @Column(name = "total_series_pushes")
    var totalSeriesPushes: Int = 0

    @Column(name = "total_conference_championships")
    var totalConferenceChampionships: Int = 0

    @Column(name = "total_tournament_appearances")
    var totalTournamentAppearances: Int = 0

    @Column(name = "total_super_regional_appearances")
    var totalSuperRegionalAppearances: Int = 0

    @Column(name = "total_college_world_series_appearances")
    var totalCollegeWorldSeriesAppearances: Int = 0

    @Column(name = "total_championships")
    var totalChampionships: Int = 0

    @Column(name = "last_accepted_lineup")
    @Type(type = "json")
    var lastAcceptedLineup: List<Int> = emptyList()

    @Column(name = "taken")
    var taken: Boolean = false

    @Column(name = "active")
    var active: Boolean = false

    @Column(name = "current_elo")
    var currentElo: Double = 1500.0

    @Column(name = "overall_elo")
    var overallElo: Double = 1500.0

    // Default constructor
    constructor()

    // Constructor with parameters
    constructor(
        name: String,
        shortName: String,
        abbreviation: String,
        subdivision: Subdivision,
        logo: String?,
        scorebugLogo: String?,
        primaryColor: String,
        secondaryColor: String,
        coachUsername: String?,
        coachName: String?,
        coachDiscordTag: String?,
        coachDiscordId: String?,
        conference: Conference?,
        ranking: Int?,
        currentWins: Int,
        currentLosses: Int,
        currentWinPercentage: Float,
        currentConferenceWins: Int,
        currentConferenceLosses: Int,
        currentConferenceWinPercentage: Float,
        currentSeriesWins: Int,
        currentSeriesLosses: Int,
        currentSeriesPushes: Int,
        numSeasons: Int,
        totalWins: Int,
        totalLosses: Int,
        totalWinPercentage: Float,
        totalConferenceWins: Int,
        totalConferenceLosses: Int,
        totalConferenceWinPercentage: Float,
        totalSeriesWins: Int,
        totalSeriesLosses: Int,
        totalSeriesPushes: Int,
        totalConferenceChampionships: Int,
        totalTournamentAppearances: Int,
        totalSuperRegionalAppearances: Int,
        totalCollegeWorldSeriesAppearances: Int,
        totalChampionships: Int,
        lastAcceptedLineup: List<Int>,
        taken: Boolean,
        active: Boolean,
    ) {
        this.name = name
        this.shortName = shortName
        this.abbreviation = abbreviation
        this.subdivision = subdivision
        this.logo = logo
        this.scorebugLogo = scorebugLogo
        this.primaryColor = primaryColor
        this.secondaryColor = secondaryColor
        this.coachUsername = coachUsername
        this.coachName = coachName
        this.coachDiscordTag = coachDiscordTag
        this.coachDiscordId = coachDiscordId
        this.conference = conference
        this.ranking = ranking
        this.currentWins = currentWins
        this.currentLosses = currentLosses
        this.currentWinPercentage = currentWinPercentage
        this.currentConferenceWins = currentConferenceWins
        this.currentConferenceLosses = currentConferenceLosses
        this.currentConferenceWinPercentage = currentConferenceWinPercentage
        this.currentSeriesWins = currentSeriesWins
        this.currentSeriesLosses = currentSeriesLosses
        this.currentSeriesPushes = currentSeriesPushes
        this.numSeasons = numSeasons
        this.totalWins = totalWins
        this.totalLosses = totalLosses
        this.totalWinPercentage = totalWinPercentage
        this.totalConferenceWins = totalConferenceWins
        this.totalConferenceLosses = totalConferenceLosses
        this.totalConferenceWinPercentage = totalConferenceWinPercentage
        this.totalSeriesWins = totalSeriesWins
        this.totalSeriesLosses = totalSeriesLosses
        this.totalSeriesPushes = totalSeriesPushes
        this.totalConferenceChampionships = totalConferenceChampionships
        this.totalTournamentAppearances = totalTournamentAppearances
        this.totalSuperRegionalAppearances = totalSuperRegionalAppearances
        this.totalCollegeWorldSeriesAppearances = totalCollegeWorldSeriesAppearances
        this.totalChampionships = totalChampionships
        this.lastAcceptedLineup = lastAcceptedLineup
        this.taken = taken
        this.active = active
    }

}
