package com.nfcaab.backend.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "season", schema = "porygon")
class Season {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "season_number", nullable = false)
    var seasonNumber: Int = 1

    @Column(name = "start_date")
    lateinit var startDate: String

    @Column(name = "end_date")
    var endDate: String? = null

    @Column(name = "national_championship_winning_team")
    var nationalChampionshipWinningTeam: String? = null

    @Column(name = "national_championship_losing_team")
    var nationalChampionshipLosingTeam: String? = null

    @Column(name = "national_championship_winning_coach")
    var nationalChampionshipWinningCoach: String? = null

    @Column(name = "national_championship_losing_coach")
    var nationalChampionshipLosingCoach: String? = null

    @Column(name = "current_week")
    var currentWeek: Int = 1

    @Column(name = "current_season")
    var currentSeason: Boolean = false

    // Default constructor
    constructor()

    // Constructor with parameters
    constructor(
        seasonNumber: Int,
        startDate: String,
        endDate: String?,
        nationalChampionshipWinningTeam: String?,
        nationalChampionshipLosingTeam: String?,
        nationalChampionshipWinningCoach: String?,
        nationalChampionshipLosingCoach: String?,
        currentWeek: Int,
        currentSeason: Boolean,
    ) {
        this.seasonNumber = seasonNumber
        this.startDate = startDate
        this.endDate = endDate
        this.nationalChampionshipWinningTeam = nationalChampionshipWinningTeam
        this.nationalChampionshipLosingTeam = nationalChampionshipLosingTeam
        this.nationalChampionshipWinningCoach = nationalChampionshipWinningCoach
        this.nationalChampionshipLosingCoach = nationalChampionshipLosingCoach
        this.currentWeek = currentWeek
        this.currentSeason = currentSeason
    }
}
