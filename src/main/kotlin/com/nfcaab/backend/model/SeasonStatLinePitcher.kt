package com.nfcaab.backend.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "season_stat_line_pitcher", schema = "porygon")
open class SeasonStatLinePitcher {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Int? = null

    @Column(name = "season", nullable = false)
    open var season: Int? = null

    @Column(name = "player_id", nullable = false)
    open var playerId: String? = null

    @Column(name = "team", nullable = false)
    open var team: String? = null

    @Column(name = "first_name", nullable = false)
    open var firstName: String? = null

    @Column(name = "last_name", nullable = false)
    open var lastName: String? = null

    @Column(name = "uniform_number", nullable = false)
    open var uniformNumber: Int? = null

    @Column(name = "position", nullable = false)
    open var position: String? = null

    @Column(name = "archetype", nullable = false)
    open var archetype: String? = null

    @Column(name = "wins")
    open var wins: Int? = null

    @Column(name = "losses")
    open var losses: Int? = null

    @Column(name = "saves")
    open var saves: Int? = null

    @Column(name = "games")
    open var games: Int? = null

    @Column(name = "games_started")
    open var gamesStarted: Int? = null

    @Column(name = "innings_pitched", nullable = false)
    open var inningsPitched: String? = null

    @Column(name = "pitches_thrown", nullable = false)
    open var pitchesThrown: Int? = null

    @Column(name = "hits", nullable = false)
    open var hits: Int? = null

    @Column(name = "runs", nullable = false)
    open var runs: Int? = null

    @Column(name = "earned_runs", nullable = false)
    open var earnedRuns: Int? = null

    @Column(name = "walks", nullable = false)
    open var walks: Int? = null

    @Column(name = "strikeouts", nullable = false)
    open var strikeouts: Int? = null

    @Column(name = "home_runs", nullable = false)
    open var homeRuns: Int? = null

    @Column(name = "triples", nullable = false)
    open var triples: Int? = null

    @Column(name = "doubles", nullable = false)
    open var doubles: Int? = null

    @Column(name = "singles", nullable = false)
    open var singles: Int? = null

    @Column(name = "steals_allowed", nullable = false)
    open var stealsAllowed: Int? = null

    @Column(name = "double_plays_forced", nullable = false)
    open var doublePlaysForced: Int? = null

    @Column(name = "strikeout_per_nine")
    open var strikeoutPerNine: Float? = null

    @Column(name = "walks_per_nine")
    open var walksPerNine: Float? = null

    @Column(name = "home_runs_per_nine")
    open var homeRunsPerNine: Float? = null

    @Column(name = "babip")
    open var babip: Float? = null

    @Column(name = "left_on_base_percentage")
    open var leftOnBasePercentage: Float? = null

    @Column(name = "earned_run_average")
    open var earnedRunAverage: Float? = null

    @Column(name = "fielder_independent_pitching")
    open var fielderIndependentPitching: Float? = null

    @Column(name = "wins_above_replacement")
    open var winsAboveReplacement: Float? = null
}
