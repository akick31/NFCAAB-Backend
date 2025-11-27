package com.nfcaab.porygon.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "game_lineups", schema = "porygon")
open class GameLineup {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Int? = null

    @Column(name = "game_id", nullable = false)
    open var gameId: String? = null

    @Column(name = "team", nullable = false)
    open var team: String? = null

    @Column(name = "lineup_spot", nullable = false)
    open var lineupSpot: String? = null

    @Column(name = "name", nullable = false)
    open var name: String? = null

    @Column(name = "uniform_number", nullable = false)
    open var uniformNumber: Int? = null

    @Column(name = "position", nullable = false)
    open var position: String? = null

    @Column(name = "archetype", nullable = false)
    open var archetype: String? = null

    @Column(name = "currently_playing", nullable = false)
    open var currentlyPlaying: Boolean? = false

    @Column(name = "has_appeared", nullable = false)
    open var hasAppeared: Boolean? = false
}
