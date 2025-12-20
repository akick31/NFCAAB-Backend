package com.nfcaab.backend.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "game_writeups", schema = "porygon")
class GameWriteup {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Int = 0

    @Column(name = "result")
    lateinit var result: String

    @Column(name = "batter_on_first")
    var batterOnFirst: Boolean = false

    @Column(name = "batter_on_second")
    var batterOnSecond: Boolean = false

    @Column(name = "batter_on_third")
    var batterOnThird: Boolean = false

    @Column(name = "runs_scored")
    var runsScored: Int = 0

    @Column(name = "game_writeup")
    lateinit var gameWriteup: String
}
