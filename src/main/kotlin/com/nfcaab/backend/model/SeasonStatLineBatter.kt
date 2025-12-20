package com.nfcaab.backend.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "season_stat_line_batter", schema = "porygon")
open class SeasonStatLineBatter {
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

    @Column(name = "at_bats", nullable = false)
    open var atBats: Int? = null

    @Column(name = "runs", nullable = false)
    open var runs: Int? = null

    @Column(name = "hits", nullable = false)
    open var hits: Int? = null

    @Column(name = "runs_batted_in", nullable = false)
    open var runsBattedIn: Int? = null

    @Column(name = "home_runs", nullable = false)
    open var homeRuns: Int? = null

    @Column(name = "triples", nullable = false)
    open var triples: Int? = null

    @Column(name = "doubles", nullable = false)
    open var doubles: Int? = null

    @Column(name = "singles", nullable = false)
    open var singles: Int? = null

    @Column(name = "walks", nullable = false)
    open var walks: Int? = null

    @Column(name = "strikeouts", nullable = false)
    open var strikeouts: Int? = null

    @Column(name = "steals", nullable = false)
    open var steals: Int? = null

    @Column(name = "double_plays", nullable = false)
    open var doublePlays: Int? = null

    @Column(name = "batting_average")
    open var battingAverage: Float? = null

    @Column(name = "on_base_percentage")
    open var onBasePercentage: Float? = null

    @Column(name = "slugging")
    open var slugging: Float? = null

    @Column(name = "weighted_on_base")
    open var weightedOnBase: Float? = null

    @Column(name = "babip")
    open var babip: Float? = null

    @Column(name = "isolated_power")
    open var isolatedPower: Float? = null

    @Column(name = "strikeout_percentage")
    open var strikeoutPercentage: Float? = null

    @Column(name = "walk_percentage")
    open var walkPercentage: Float? = null

    @Column(name = "wins_above_replacement")
    open var winsAboveReplacement: Float? = null
}
