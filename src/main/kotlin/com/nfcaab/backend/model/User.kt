package com.nfcaab.backend.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "users", schema = "porygon")
class User {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long = 0

    @Column(name = "username", nullable = false)
    lateinit var username: String

    @Column(name = "coach_name", nullable = false)
    lateinit var coachName: String

    @Column(name = "discord_tag")
    lateinit var discordTag: String

    @Column(name = "discord_id")
    lateinit var discordId: String

    @Column(name = "email", nullable = false)
    lateinit var email: String

    @Column(name = "hashed_email")
    var hashedEmail: String? = null

    @Column(name = "password", nullable = false)
    lateinit var password: String

    @Column(name = "position", nullable = false)
    lateinit var position: String

    @Column(name = "role", nullable = false)
    lateinit var role: Role

    @Column(name = "team")
    var team: String? = null

    @Column(name = "delay_of_game_instances")
    var delayOfGameInstances: Int = 0

    @Column(name = "wins")
    var wins: Int = 0

    @Column(name = "losses")
    var losses: Int = 0

    @Column(name = "win_percentage")
    var winPercentage: Float = 0.0F

    @Column(name = "conference_wins")
    var conferenceWins: Int = 0

    @Column(name = "conference_losses")
    var conferenceLosses: Int = 0

    @Column(name = "conference_win_percentage")
    var conferenceWinPercentage: Float = 0.0F

    @Column(name = "series_wins")
    var seriesWins: Int = 0

    @Column(name = "series_losses")
    var seriesLosses: Int = 0

    @Column(name = "series_pushes")
    var seriesPushes: Int = 0

    @Column(name = "conference_championships")
    var conferenceChampionships: Int = 0

    @Column(name = "tournament_appearances")
    var tournamentAppearances: Int = 0

    @Column(name = "super_regional_appearances")
    var superRegionalAppearances: Int = 0

    @Column(name = "college_world_series_appearances")
    var collegeWorldSeriesAppearances: Int = 0

    @Column(name = "championships")
    var championships: Int = 0

    @Column(name = "average_response_time")
    var averageResponseTime: Double = 0.0

    @Column(name = "reset_token")
    var resetToken: String? = null

    @Column(name = "reset_token_expiration")
    var resetTokenExpiration: String? = null

    // Default constructor
    constructor()

    // Constructor with parameters
    constructor(
        username: String,
        coachName: String,
        discordTag: String,
        discordId: String,
        email: String,
        hashedEmail: String?,
        password: String,
        role: Role,
        team: String?,
        delayOfGameInstances: Int,
        wins: Int,
        losses: Int,
        winPercentage: Float,
        conferenceWins: Int,
        conferenceLosses: Int,
        conferenceWinPercentage: Float,
        seriesWins: Int,
        seriesLosses: Int,
        seriesPushes: Int,
        conferenceChampionships: Int,
        tournamentAppearances: Int,
        superRegionalAppearances: Int,
        collegeWorldSeriesAppearances: Int,
        championships: Int,
        averageResponseTime: Double,
        resetToken: String?,
        resetTokenExpiration: String?,
    ) {
        this.username = username
        this.coachName = coachName
        this.discordTag = discordTag
        this.discordId = discordId
        this.email = email
        this.hashedEmail = hashedEmail
        this.password = password
        this.position = position
        this.role = role
        this.team = team
        this.delayOfGameInstances = delayOfGameInstances
        this.wins = wins
        this.losses = losses
        this.winPercentage = winPercentage
        this.conferenceWins = conferenceWins
        this.conferenceLosses = conferenceLosses
        this.conferenceWinPercentage = conferenceWinPercentage
        this.seriesWins = seriesWins
        this.seriesLosses = seriesLosses
        this.seriesPushes = seriesPushes
        this.conferenceChampionships = conferenceChampionships
        this.tournamentAppearances = tournamentAppearances
        this.superRegionalAppearances = superRegionalAppearances
        this.collegeWorldSeriesAppearances = collegeWorldSeriesAppearances
        this.championships = championships
        this.averageResponseTime = averageResponseTime
        this.resetToken = resetToken
        this.resetTokenExpiration = resetTokenExpiration
    }

    enum class Role(val description: String) {
        USER("User"),
        CONFERENCE_COMMISSIONER("Conference Commissioner"),
        ADMIN("Admin"),
        ;

        companion object {
            fun fromString(description: String): Role? {
                return entries.find { it.description == description }
            }
        }
    }
}
