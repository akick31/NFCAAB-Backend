package com.nfcaab.porygon.model

import com.nfcaab.porygon.model.Game.GameType
import com.nfcaab.porygon.model.Game.Subdivision
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "schedule", schema = "porygon")
class Schedule {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Int? = null

    @Column(name = "season", nullable = false)
    var season: Int? = 0

    @Column(name = "week", nullable = false)
    var week: Int? = 0

    @Column(name = "game_number", nullable = false)
    var gameNumber: Int? = 1

    @Column(name = "subdivision", nullable = false)
    lateinit var subdivision: Subdivision

    @Column(name = "home_team", nullable = false)
    lateinit var homeTeam: String

    @Column(name = "away_team", nullable = false)
    lateinit var awayTeam: String

    @Column(name = "game_type", nullable = false)
    lateinit var gameType: GameType

    @Column(name = "started", nullable = false)
    var started: Boolean? = false

    @Column(name = "finished")
    var finished: Boolean? = false
}
