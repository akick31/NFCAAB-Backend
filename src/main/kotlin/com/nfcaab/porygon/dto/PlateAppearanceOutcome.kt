package com.nfcaab.porygon.dto

import com.nfcaab.porygon.model.Game.ActualResult
import com.nfcaab.porygon.model.Game.BaseCondition
import com.nfcaab.porygon.model.Player

data class PlateAppearanceOutcome(
    val actualResult: ActualResult,
    val outs: Int,
    val runsScored: Int,
    val homeScore: Int,
    val awayScore: Int,
    val runnerOnFirstAfter: Player?,
    val runnerOnSecondAfter: Player?,
    val runnerOnThirdAfter: Player?,
    val baseConditionAfter: BaseCondition,
)
