package com.nfcaab.porygon.dto

import com.nfcaab.porygon.model.Game.ActualResult
import com.nfcaab.porygon.model.Game.InningHalf

data class GameScenario(
    val result: ActualResult,
    val batterOnFirst: Boolean,
    val batterOnSecond: Boolean,
    val batterOnThird: Boolean,
    val runsScored: Int,
    val inning: Int,
    val inningHalf: InningHalf,
    val outs: Int,
)
