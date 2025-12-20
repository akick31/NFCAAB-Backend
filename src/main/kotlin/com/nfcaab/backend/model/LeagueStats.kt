package com.nfcaab.backend.model

import com.nfcaab.backend.enums.team.Subdivision
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "league_stats", schema = "porygon")
class LeagueStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int = 0,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "subdivision", nullable = false)
    var subdivision: Subdivision,
    @Basic
    @Column(name = "season_number", nullable = false)
    var seasonNumber: Int,
    @Basic
    @Column(name = "total_teams")
    var totalTeams: Int = 0,
    @Basic
    @Column(name = "total_games")
    var totalGames: Int = 0,
    // Batting Stats (League Totals)
    @Basic
    @Column(name = "at_bats")
    var atBats: Int = 0,
    @Basic
    @Column(name = "runs")
    var runs: Int = 0,
    @Basic
    @Column(name = "hits")
    var hits: Int = 0,
    @Basic
    @Column(name = "runs_batted_in")
    var runsBattedIn: Int = 0,
    @Basic
    @Column(name = "home_runs")
    var homeRuns: Int = 0,
    @Basic
    @Column(name = "triples")
    var triples: Int = 0,
    @Basic
    @Column(name = "doubles")
    var doubles: Int = 0,
    @Basic
    @Column(name = "singles")
    var singles: Int = 0,
    @Basic
    @Column(name = "walks")
    var walks: Int = 0,
    @Basic
    @Column(name = "strikeouts")
    var strikeouts: Int = 0,
    @Basic
    @Column(name = "steals")
    var steals: Int = 0,
    @Basic
    @Column(name = "double_plays")
    var doublePlays: Int = 0,
    @Basic
    @Column(name = "batting_average")
    var battingAverage: Double = 0.0,
    @Basic
    @Column(name = "on_base_percentage")
    var onBasePercentage: Double = 0.0,
    @Basic
    @Column(name = "slugging_percentage")
    var sluggingPercentage: Double = 0.0,
    // Pitching Stats (League Totals)
    @Basic
    @Column(name = "innings_pitched")
    var inningsPitched: String? = null,
    @Basic
    @Column(name = "hits_allowed")
    var hitsAllowed: Int = 0,
    @Basic
    @Column(name = "runs_allowed")
    var runsAllowed: Int = 0,
    @Basic
    @Column(name = "earned_runs_allowed")
    var earnedRunsAllowed: Int = 0,
    @Basic
    @Column(name = "walks_allowed")
    var walksAllowed: Int = 0,
    @Basic
    @Column(name = "strikeouts_thrown")
    var strikeoutsThrown: Int = 0,
    @Basic
    @Column(name = "home_runs_allowed")
    var homeRunsAllowed: Int = 0,
    @Basic
    @Column(name = "triples_allowed")
    var triplesAllowed: Int = 0,
    @Basic
    @Column(name = "doubles_allowed")
    var doublesAllowed: Int = 0,
    @Basic
    @Column(name = "singles_allowed")
    var singlesAllowed: Int = 0,
    @Basic
    @Column(name = "steals_allowed")
    var stealsAllowed: Int = 0,
    @Basic
    @Column(name = "double_plays_forced")
    var doublePlaysForced: Int = 0,
    @Basic
    @Column(name = "era")
    var era: Double = 0.0,
    @Basic
    @Column(name = "whip")
    var whip: Double = 0.0,
    // Performance Metrics (League Averages)
    @Basic
    @Column(name = "average_response_speed")
    var averageResponseSpeed: Double = 0.0,
    // Metadata
    @Basic
    @Column(name = "last_modified_ts")
    var lastModifiedTs: String? = null,
) {
    constructor() : this(
        subdivision = Subdivision.NFCAAB,
        seasonNumber = 0,
    )
}

