package com.nfcaab.porygon.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "game_stat_line_pitcher", schema = "porygon")
open class PitcherGameStats {
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

    @Column(name = "lineup_spot", nullable = false)
    open var lineupSpot: Int? = null

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

    @Column(name = "pitches_thrown", nullable = false)
    open var pitchesThrown: Int? = null

    @Column(name = "innings_pitched", nullable = false)
    open var inningsPitched: String? = null

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

    @Column(name = "left_on_base")
    open var leftOnBase: Int? = null

    @Column(name = "steals_allowed", nullable = false)
    open var stealsAllowed: Int? = null

    @Column(name = "double_plays_forced", nullable = false)
    open var doublePlaysForced: Int? = null
}
