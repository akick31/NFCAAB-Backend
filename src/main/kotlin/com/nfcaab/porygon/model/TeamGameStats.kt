package com.nfcaab.porygon.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "game_stat_line_team", schema = "porygon")
open class TeamGameStats {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Int? = null

    @Column(name = "game_id", nullable = false)
    open var gameId: String? = null

    @Column(name = "game_number", nullable = false)
    open var gameNumber: Int? = null

    @Column(name = "team", nullable = false)
    open var team: String? = null

    @Column(name = "opponent_team", nullable = false)
    open var opponentTeam: String? = null

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

    @Column(name = "innings_pitched", nullable = false)
    open var inningsPitched: String? = null

    @Column(name = "hits_allowed", nullable = false)
    open var hitsAllowed: Int? = null

    @Column(name = "runs_allowed", nullable = false)
    open var runsAllowed: Int? = null

    @Column(name = "earned_runs_allowed", nullable = false)
    open var earnedRunsAllowed: Int? = null

    @Column(name = "walks_allowed", nullable = false)
    open var walksAllowed: Int? = null

    @Column(name = "strikeouts_thrown", nullable = false)
    open var strikeoutsThrown: Int? = null

    @Column(name = "home_runs_allowed", nullable = false)
    open var homeRunsAllowed: Int? = null

    @Column(name = "triples_allowed", nullable = false)
    open var triplesAllowed: Int? = null

    @Column(name = "doubles_allowed", nullable = false)
    open var doublesAllowed: Int? = null

    @Column(name = "singles_allowed", nullable = false)
    open var singlesAllowed: Int? = null

    @Column(name = "steals_allowed", nullable = false)
    open var stealsAllowed: Int? = null

    @Column(name = "double_plays_forced", nullable = false)
    open var doublePlaysForced: Int? = null
}
