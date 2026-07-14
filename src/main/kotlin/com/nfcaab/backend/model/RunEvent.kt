package com.nfcaab.backend.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "run_event", schema = "porygon")
class RunEvent {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Int? = null

    @Column(name = "game_id", nullable = false)
    var gameId: Int = 0

    @Column(name = "at_bat_id", nullable = false)
    var atBatId: Int = 0

    @Column(name = "inning", nullable = false)
    var inning: Int = 1

    @Column(name = "scoring_team", nullable = false)
    var scoringTeam: String = ""

    @Column(name = "scoring_player_uniform_number")
    var scoringPlayerUniformNumber: Int? = null

    @Column(name = "charged_pitcher_uniform_number")
    var chargedPitcherUniformNumber: Int? = null

    @Column(name = "charged_pitcher_team", nullable = false)
    var chargedPitcherTeam: String = ""
}
