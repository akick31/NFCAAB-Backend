package com.nfcaab.backend.dto

import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.model.Game.BaseCondition
import com.nfcaab.backend.model.Player

data class AtBatOutcome(
    val actualResult: ActualResult,
    val outs: Int,
    val runsScored: Int,
    val homeScore: Int,
    val awayScore: Int,
    val runnerOnFirstAfter: Player?,
    val runnerOnSecondAfter: Player?,
    val runnerOnThirdAfter: Player?,
    val baseConditionAfter: BaseCondition,
    val scoringRunners: List<Player> = emptyList(),
    val runnerOnFirstPitcherAfter: Int? = null,
    val runnerOnSecondPitcherAfter: Int? = null,
    val runnerOnThirdPitcherAfter: Int? = null,
)

