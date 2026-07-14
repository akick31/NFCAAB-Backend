package com.nfcaab.backend.service.atbat

import com.nfcaab.backend.dto.AtBatOutcome
import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.model.Game.BaseCondition
import com.nfcaab.backend.model.Game.HitDirection
import com.nfcaab.backend.model.Game.InningHalf
import com.nfcaab.backend.model.Game.InningHalf.TOP
import com.nfcaab.backend.model.Game.Scenario
import com.nfcaab.backend.model.Game.Scenario.DOUBLE
import com.nfcaab.backend.model.Game.Scenario.FLYOUT
import com.nfcaab.backend.model.Game.Scenario.HOME_RUN
import com.nfcaab.backend.model.Game.Scenario.LEFT_GROUNDOUT
import com.nfcaab.backend.model.Game.Scenario.RIGHT_GROUNDOUT
import com.nfcaab.backend.model.Game.Scenario.SINGLE
import com.nfcaab.backend.model.Game.Scenario.STRIKEOUT
import com.nfcaab.backend.model.Game.Scenario.TRIPLE
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.model.Player.BatterArchetype
import com.nfcaab.backend.util.InvalidScenarioException
import com.nfcaab.backend.util.ResultNotFoundException
import org.springframework.stereotype.Service

@Service
class BaseRunningService(
    private val rangesService: RangesService,
) {
    fun resolveOutcome(
        result: Scenario,
        outs: Int,
        inningHalf: InningHalf,
        baseConditionBefore: BaseCondition,
        runnerOnFirst: Player?,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
        homeScore: Int,
        awayScore: Int,
        hitDirection: HitDirection?,
    ): AtBatOutcome =
        when (result) {
            STRIKEOUT ->
                handleStrikeout(outs, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore)
            Scenario.WALK ->
                handleWalk(outs, inningHalf, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore)
            FLYOUT ->
                handleFlyout(outs, inningHalf, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore)
            LEFT_GROUNDOUT ->
                handleLeftGroundout(outs, inningHalf, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore)
            RIGHT_GROUNDOUT ->
                handleRightGroundout(outs, inningHalf, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore)
            SINGLE ->
                handleSingle(outs, inningHalf, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore, hitDirection)
            DOUBLE ->
                handleDouble(outs, inningHalf, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore, hitDirection)
            TRIPLE ->
                handleTriple(outs, inningHalf, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore)
            HOME_RUN ->
                handleHomeRun(outs, inningHalf, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore)
            else -> throw InvalidScenarioException()
        }

    fun resolveSteal(
        runnerArchetype: BatterArchetype,
        pitcherArchetype: Player.PitcherArchetype,
        difference: Int,
    ): Scenario =
        rangesService.getStealResult(runnerArchetype, pitcherArchetype, difference).result
            ?: throw ResultNotFoundException()

    private fun runnerTakesExtraBase(
        direction: HitDirection?,
        runner: Player?,
        outs: Int,
    ): Boolean {
        if (direction == null) return false
        val aggressive = runner?.batterArchetype == BatterArchetype.SPEEDY || outs == 2
        return when (direction) {
            HitDirection.LEFT -> false
            HitDirection.LEFT_CENTER, HitDirection.CENTER, HitDirection.RIGHT_CENTER -> aggressive
            HitDirection.RIGHT -> true
        }
    }

    private fun handleStrikeout(
        outs: Int,
        baseConditionBefore: BaseCondition,
        runnerOnFirst: Player?,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
        homeScore: Int,
        awayScore: Int,
    ): AtBatOutcome {
        val actualResult = ActualResult.STRIKEOUT
        val updatedOuts = outs + 1
        val runsScored = 0
        val runnerOnFirstAfter: Player?
        val runnerOnSecondAfter: Player?
        val runnerOnThirdAfter: Player?
        var baseConditionAfter = baseConditionBefore
        if (updatedOuts < 3) {
            runnerOnFirstAfter = runnerOnFirst
            runnerOnSecondAfter = runnerOnSecond
            runnerOnThirdAfter = runnerOnThird
        } else {
            runnerOnFirstAfter = null
            runnerOnSecondAfter = null
            runnerOnThirdAfter = null
            baseConditionAfter = BaseCondition.EMPTY
        }
        return AtBatOutcome(
            actualResult = actualResult,
            outs = updatedOuts,
            runsScored = runsScored,
            homeScore = homeScore,
            awayScore = awayScore,
            runnerOnFirstAfter = runnerOnFirstAfter,
            runnerOnSecondAfter = runnerOnSecondAfter,
            runnerOnThirdAfter = runnerOnThirdAfter,
            baseConditionAfter = baseConditionAfter,
        )
    }

    private fun handleFlyout(
        outs: Int,
        inningHalf: InningHalf,
        baseConditionBefore: BaseCondition,
        runnerOnFirst: Player?,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
        homeScore: Int,
        awayScore: Int,
    ): AtBatOutcome {
        var actualResult = ActualResult.FLYOUT
        val updatedOuts = outs + 1
        var runsScored = 0
        var updatedHomeScore = homeScore
        var updatedAwayScore = awayScore
        var runnerOnFirstAfter = runnerOnFirst
        var runnerOnSecondAfter = runnerOnSecond
        var runnerOnThirdAfter = runnerOnThird
        var baseConditionAfter = baseConditionBefore
        if (updatedOuts < 3) {
            when (baseConditionBefore) {
                BaseCondition.FIRST -> {
                    runnerOnFirstAfter = runnerOnFirst
                }
                BaseCondition.SECOND -> {
                    if (runnerOnSecond?.batterArchetype == BatterArchetype.SPEEDY) {
                        runnerOnSecondAfter = null
                        runnerOnThirdAfter = runnerOnSecond
                        baseConditionAfter = BaseCondition.THIRD
                    } else {
                        runnerOnSecondAfter = runnerOnSecond
                    }
                }
                BaseCondition.THIRD -> {
                    actualResult = ActualResult.SACRIFICE_FLY
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.EMPTY
                    runsScored = 1
                    if (inningHalf == TOP) {
                        updatedAwayScore += 1
                    } else {
                        updatedHomeScore += 1
                    }
                }
                BaseCondition.FIRST_SECOND -> {
                    if (runnerOnSecond?.batterArchetype == BatterArchetype.SPEEDY) {
                        runnerOnFirstAfter = runnerOnFirst
                        runnerOnSecondAfter = null
                        runnerOnThirdAfter = runnerOnSecond
                        baseConditionAfter = BaseCondition.FIRST_THIRD
                    } else {
                        runnerOnFirstAfter = runnerOnFirst
                        runnerOnSecondAfter = runnerOnSecond
                        baseConditionAfter = BaseCondition.FIRST_SECOND
                    }
                }
                BaseCondition.FIRST_THIRD -> {
                    actualResult = ActualResult.SACRIFICE_FLY
                    runnerOnFirstAfter = runnerOnFirst
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.FIRST
                    runsScored = 1
                    if (inningHalf == TOP) {
                        updatedAwayScore += 1
                    } else {
                        updatedHomeScore += 1
                    }
                }
                BaseCondition.SECOND_THIRD -> {
                    actualResult = ActualResult.SACRIFICE_FLY
                    if (runnerOnSecond?.batterArchetype == BatterArchetype.SPEEDY) {
                        runnerOnSecondAfter = null
                        runnerOnThirdAfter = runnerOnSecond
                        baseConditionAfter = BaseCondition.THIRD
                    } else {
                        runnerOnSecondAfter = runnerOnSecond
                        runnerOnThirdAfter = null
                        baseConditionAfter = BaseCondition.SECOND
                    }
                    runsScored = 1
                    if (inningHalf == TOP) {
                        updatedAwayScore += 1
                    } else {
                        updatedHomeScore += 1
                    }
                }
                BaseCondition.BASED_LOADED -> {
                    actualResult = ActualResult.SACRIFICE_FLY
                    if (runnerOnSecond?.batterArchetype == BatterArchetype.SPEEDY) {
                        runnerOnFirstAfter = runnerOnFirst
                        runnerOnSecondAfter = null
                        runnerOnThirdAfter = runnerOnSecond
                        baseConditionAfter = BaseCondition.FIRST_THIRD
                    } else {
                        runnerOnFirstAfter = runnerOnFirst
                        runnerOnSecondAfter = runnerOnSecond
                        runnerOnThirdAfter = null
                        baseConditionAfter = BaseCondition.FIRST_SECOND
                    }
                    runsScored = 1
                    if (inningHalf == TOP) {
                        updatedAwayScore += 1
                    } else {
                        updatedHomeScore += 1
                    }
                }
                else -> {}
            }
        } else {
            runnerOnFirstAfter = null
            runnerOnSecondAfter = null
            runnerOnThirdAfter = null
            baseConditionAfter = BaseCondition.EMPTY
        }
        return AtBatOutcome(
            actualResult = actualResult,
            outs = updatedOuts,
            runsScored = runsScored,
            homeScore = updatedHomeScore,
            awayScore = updatedAwayScore,
            runnerOnFirstAfter = runnerOnFirstAfter,
            runnerOnSecondAfter = runnerOnSecondAfter,
            runnerOnThirdAfter = runnerOnThirdAfter,
            baseConditionAfter = baseConditionAfter,
        )
    }

    private fun handleWalk(
        outs: Int,
        inningHalf: InningHalf,
        baseConditionBefore: BaseCondition,
        runnerOnFirst: Player?,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
        homeScore: Int,
        awayScore: Int,
    ): AtBatOutcome {
        val actualResult = ActualResult.WALK
        val runsScored = 0
        var runnerOnFirstAfter: Player?
        var runnerOnSecondAfter: Player?
        var runnerOnThirdAfter: Player?
        var baseConditionAfter = baseConditionBefore

        when (baseConditionBefore) {
            BaseCondition.EMPTY -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = null
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST
            }
            BaseCondition.FIRST -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = runnerOnFirst
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST_SECOND
            }
            BaseCondition.SECOND -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = runnerOnSecond
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST_SECOND
            }
            BaseCondition.THIRD -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = null
                runnerOnThirdAfter = runnerOnThird
                baseConditionAfter = BaseCondition.FIRST_THIRD
            }
            BaseCondition.FIRST_SECOND -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = runnerOnFirst
                runnerOnThirdAfter = runnerOnSecond
                baseConditionAfter = BaseCondition.FIRST_THIRD
            }
            BaseCondition.FIRST_THIRD -> {
                var updatedHomeScore = homeScore
                var updatedAwayScore = awayScore
                if (inningHalf == TOP) {
                    updatedAwayScore += 1
                } else {
                    updatedHomeScore += 1
                }
                return AtBatOutcome(
                    actualResult = actualResult,
                    outs = outs,
                    runsScored = 1,
                    homeScore = updatedHomeScore,
                    awayScore = updatedAwayScore,
                    runnerOnFirstAfter = null,
                    runnerOnSecondAfter = runnerOnFirst,
                    runnerOnThirdAfter = null,
                    baseConditionAfter = BaseCondition.FIRST_SECOND,
                )
            }
            BaseCondition.SECOND_THIRD -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = runnerOnSecond
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.BASED_LOADED
            }
            BaseCondition.BASED_LOADED -> {
                var updatedHomeScore = homeScore
                var updatedAwayScore = awayScore
                if (inningHalf == TOP) {
                    updatedAwayScore += 1
                } else {
                    updatedHomeScore += 1
                }
                return AtBatOutcome(
                    actualResult = actualResult,
                    outs = outs,
                    runsScored = 1,
                    homeScore = updatedHomeScore,
                    awayScore = updatedAwayScore,
                    runnerOnFirstAfter = null,
                    runnerOnSecondAfter = runnerOnFirst,
                    runnerOnThirdAfter = runnerOnSecond,
                    baseConditionAfter = BaseCondition.BASED_LOADED,
                )
            }
        }

        return AtBatOutcome(
            actualResult = actualResult,
            outs = outs,
            runsScored = runsScored,
            homeScore = homeScore,
            awayScore = awayScore,
            runnerOnFirstAfter = runnerOnFirstAfter,
            runnerOnSecondAfter = runnerOnSecondAfter,
            runnerOnThirdAfter = runnerOnThirdAfter,
            baseConditionAfter = baseConditionAfter,
        )
    }

    private fun handleLeftGroundout(
        outs: Int,
        inningHalf: InningHalf,
        baseConditionBefore: BaseCondition,
        runnerOnFirst: Player?,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
        homeScore: Int,
        awayScore: Int,
    ): AtBatOutcome {
        val actualResult = ActualResult.GROUNDOUT
        val updatedOuts = outs + 1
        val runsScored = 0
        val updatedHomeScore = homeScore
        val updatedAwayScore = awayScore
        val runnerOnFirstAfter: Player?
        val runnerOnSecondAfter: Player?
        val runnerOnThirdAfter: Player?
        var baseConditionAfter = baseConditionBefore

        if (updatedOuts >= 3) {
            runnerOnFirstAfter = null
            runnerOnSecondAfter = null
            runnerOnThirdAfter = null
            baseConditionAfter = BaseCondition.EMPTY
        } else {
            when (baseConditionBefore) {
                BaseCondition.FIRST -> {
                    runnerOnFirstAfter = null
                    runnerOnSecondAfter = null
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.EMPTY
                }
                BaseCondition.FIRST_SECOND -> {
                    runnerOnFirstAfter = null
                    runnerOnSecondAfter = null
                    runnerOnThirdAfter = runnerOnSecond
                    baseConditionAfter = BaseCondition.THIRD
                }
                else -> {
                    runnerOnFirstAfter = runnerOnFirst
                    runnerOnSecondAfter = runnerOnSecond
                    runnerOnThirdAfter = runnerOnThird
                }
            }
        }

        return AtBatOutcome(
            actualResult = actualResult,
            outs = updatedOuts,
            runsScored = runsScored,
            homeScore = updatedHomeScore,
            awayScore = updatedAwayScore,
            runnerOnFirstAfter = runnerOnFirstAfter,
            runnerOnSecondAfter = runnerOnSecondAfter,
            runnerOnThirdAfter = runnerOnThirdAfter,
            baseConditionAfter = baseConditionAfter,
        )
    }

    private fun handleRightGroundout(
        outs: Int,
        inningHalf: InningHalf,
        baseConditionBefore: BaseCondition,
        runnerOnFirst: Player?,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
        homeScore: Int,
        awayScore: Int,
    ): AtBatOutcome {
        val actualResult = ActualResult.GROUNDOUT
        val updatedOuts = outs + 1
        var runsScored = 0
        var updatedHomeScore = homeScore
        var updatedAwayScore = awayScore
        val runnerOnFirstAfter: Player?
        val runnerOnSecondAfter: Player?
        val runnerOnThirdAfter: Player?
        var baseConditionAfter = baseConditionBefore

        if (updatedOuts >= 3) {
            runnerOnFirstAfter = null
            runnerOnSecondAfter = null
            runnerOnThirdAfter = null
            baseConditionAfter = BaseCondition.EMPTY
        } else {
            when (baseConditionBefore) {
                BaseCondition.FIRST -> {
                    runnerOnFirstAfter = null
                    runnerOnSecondAfter = runnerOnFirst
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.SECOND
                }
                BaseCondition.SECOND -> {
                    runnerOnFirstAfter = null
                    runnerOnSecondAfter = null
                    runnerOnThirdAfter = runnerOnSecond
                    baseConditionAfter = BaseCondition.THIRD
                }
                BaseCondition.THIRD -> {
                    runnerOnFirstAfter = null
                    runnerOnSecondAfter = null
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.EMPTY
                    runsScored = 1
                    if (inningHalf == TOP) {
                        updatedAwayScore += 1
                    } else {
                        updatedHomeScore += 1
                    }
                }
                BaseCondition.FIRST_SECOND -> {
                    runnerOnFirstAfter = null
                    runnerOnSecondAfter = runnerOnFirst
                    runnerOnThirdAfter = runnerOnSecond
                    baseConditionAfter = BaseCondition.SECOND_THIRD
                }
                else -> {
                    runnerOnFirstAfter = runnerOnFirst
                    runnerOnSecondAfter = runnerOnSecond
                    runnerOnThirdAfter = runnerOnThird
                }
            }
        }

        return AtBatOutcome(
            actualResult = actualResult,
            outs = updatedOuts,
            runsScored = runsScored,
            homeScore = updatedHomeScore,
            awayScore = updatedAwayScore,
            runnerOnFirstAfter = runnerOnFirstAfter,
            runnerOnSecondAfter = runnerOnSecondAfter,
            runnerOnThirdAfter = runnerOnThirdAfter,
            baseConditionAfter = baseConditionAfter,
        )
    }

    private fun handleSingle(
        outs: Int,
        inningHalf: InningHalf,
        baseConditionBefore: BaseCondition,
        runnerOnFirst: Player?,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
        homeScore: Int,
        awayScore: Int,
        direction: HitDirection?,
    ): AtBatOutcome {
        val actualResult = ActualResult.SINGLE
        var runsScored = 0
        var updatedHomeScore = homeScore
        var updatedAwayScore = awayScore
        val runnerOnFirstAfter: Player?
        val runnerOnSecondAfter: Player?
        val runnerOnThirdAfter: Player?
        val baseConditionAfter: BaseCondition

        when (baseConditionBefore) {
            BaseCondition.EMPTY -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = null
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST
            }
            BaseCondition.FIRST -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = runnerOnFirst
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST_SECOND
            }
            BaseCondition.SECOND -> {
                runnerOnFirstAfter = null
                if (runnerTakesExtraBase(direction, runnerOnSecond, outs)) {
                    runnerOnSecondAfter = null
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.FIRST
                    runsScored = 1
                    if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                } else {
                    runnerOnSecondAfter = null
                    runnerOnThirdAfter = runnerOnSecond
                    baseConditionAfter = BaseCondition.FIRST_THIRD
                }
            }
            BaseCondition.THIRD -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = null
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST
                runsScored = 1
                if (inningHalf == TOP) {
                    updatedAwayScore += 1
                } else {
                    updatedHomeScore += 1
                }
            }
            BaseCondition.FIRST_SECOND -> {
                runnerOnFirstAfter = null
                if (runnerTakesExtraBase(direction, runnerOnSecond, outs)) {
                    runnerOnSecondAfter = runnerOnFirst
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.FIRST_SECOND
                    runsScored = 1
                    if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                } else {
                    runnerOnSecondAfter = runnerOnFirst
                    runnerOnThirdAfter = runnerOnSecond
                    baseConditionAfter = BaseCondition.BASED_LOADED
                }
            }
            BaseCondition.FIRST_THIRD -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = runnerOnFirst
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST_SECOND
                runsScored = 1
                if (inningHalf == TOP) {
                    updatedAwayScore += 1
                } else {
                    updatedHomeScore += 1
                }
            }
            BaseCondition.SECOND_THIRD -> {
                runnerOnFirstAfter = null
                val extra = runnerTakesExtraBase(direction, runnerOnSecond, outs)
                runsScored = if (extra) 2 else 1
                if (inningHalf == TOP) updatedAwayScore += runsScored else updatedHomeScore += runsScored
                if (extra) {
                    runnerOnSecondAfter = null
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.FIRST
                } else {
                    runnerOnSecondAfter = null
                    runnerOnThirdAfter = runnerOnSecond
                    baseConditionAfter = BaseCondition.FIRST_THIRD
                }
            }
            BaseCondition.BASED_LOADED -> {
                runnerOnFirstAfter = null
                val extra = runnerTakesExtraBase(direction, runnerOnSecond, outs)
                runsScored = if (extra) 2 else 1
                if (inningHalf == TOP) updatedAwayScore += runsScored else updatedHomeScore += runsScored
                if (extra) {
                    runnerOnSecondAfter = runnerOnFirst
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.FIRST_SECOND
                } else {
                    runnerOnSecondAfter = runnerOnFirst
                    runnerOnThirdAfter = runnerOnSecond
                    baseConditionAfter = BaseCondition.BASED_LOADED
                }
            }
        }

        return AtBatOutcome(
            actualResult = actualResult,
            outs = outs,
            runsScored = runsScored,
            homeScore = updatedHomeScore,
            awayScore = updatedAwayScore,
            runnerOnFirstAfter = runnerOnFirstAfter,
            runnerOnSecondAfter = runnerOnSecondAfter,
            runnerOnThirdAfter = runnerOnThirdAfter,
            baseConditionAfter = baseConditionAfter,
        )
    }

    private fun handleDouble(
        outs: Int,
        inningHalf: InningHalf,
        baseConditionBefore: BaseCondition,
        runnerOnFirst: Player?,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
        homeScore: Int,
        awayScore: Int,
        direction: HitDirection?,
    ): AtBatOutcome {
        val actualResult = ActualResult.DOUBLE
        var runsScored = 0
        var updatedHomeScore = homeScore
        var updatedAwayScore = awayScore
        val runnerOnFirstAfter: Player?
        val runnerOnSecondAfter: Player?
        val runnerOnThirdAfter: Player?
        val baseConditionAfter: BaseCondition

        when (baseConditionBefore) {
            BaseCondition.EMPTY -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = null
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.SECOND
            }
            BaseCondition.FIRST -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = null
                if (runnerTakesExtraBase(direction, runnerOnFirst, outs)) {
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.SECOND
                    runsScored = 1
                    if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                } else {
                    runnerOnThirdAfter = runnerOnFirst
                    baseConditionAfter = BaseCondition.SECOND_THIRD
                }
            }
            BaseCondition.SECOND -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = null
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.SECOND
                runsScored = 1
                if (inningHalf == TOP) {
                    updatedAwayScore += 1
                } else {
                    updatedHomeScore += 1
                }
            }
            BaseCondition.THIRD -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = null
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.SECOND
                runsScored = 1
                if (inningHalf == TOP) {
                    updatedAwayScore += 1
                } else {
                    updatedHomeScore += 1
                }
            }
            BaseCondition.FIRST_SECOND -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = null
                runsScored = 1
                if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                if (runnerTakesExtraBase(direction, runnerOnFirst, outs)) {
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.SECOND
                    runsScored = 2
                    if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                } else {
                    runnerOnThirdAfter = runnerOnFirst
                    baseConditionAfter = BaseCondition.SECOND_THIRD
                }
            }
            BaseCondition.FIRST_THIRD -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = null
                runsScored = 1
                if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                if (runnerTakesExtraBase(direction, runnerOnFirst, outs)) {
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.SECOND
                    runsScored = 2
                    if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                } else {
                    runnerOnThirdAfter = runnerOnFirst
                    baseConditionAfter = BaseCondition.SECOND_THIRD
                }
            }
            BaseCondition.SECOND_THIRD -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = null
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.SECOND
                runsScored = 2
                if (inningHalf == TOP) {
                    updatedAwayScore += 2
                } else {
                    updatedHomeScore += 2
                }
            }
            BaseCondition.BASED_LOADED -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = null
                runsScored = 2
                if (inningHalf == TOP) updatedAwayScore += 2 else updatedHomeScore += 2
                if (runnerTakesExtraBase(direction, runnerOnFirst, outs)) {
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.SECOND
                    runsScored = 3
                    if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                } else {
                    runnerOnThirdAfter = runnerOnFirst
                    baseConditionAfter = BaseCondition.SECOND_THIRD
                }
            }
        }

        return AtBatOutcome(
            actualResult = actualResult,
            outs = outs,
            runsScored = runsScored,
            homeScore = updatedHomeScore,
            awayScore = updatedAwayScore,
            runnerOnFirstAfter = runnerOnFirstAfter,
            runnerOnSecondAfter = runnerOnSecondAfter,
            runnerOnThirdAfter = runnerOnThirdAfter,
            baseConditionAfter = baseConditionAfter,
        )
    }

    private fun handleTriple(
        outs: Int,
        inningHalf: InningHalf,
        baseConditionBefore: BaseCondition,
        runnerOnFirst: Player?,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
        homeScore: Int,
        awayScore: Int,
    ): AtBatOutcome {
        val actualResult = ActualResult.TRIPLE
        var updatedHomeScore = homeScore
        var updatedAwayScore = awayScore

        val runsScored =
            when (baseConditionBefore) {
                BaseCondition.EMPTY -> 0
                BaseCondition.FIRST -> 1
                BaseCondition.SECOND -> 1
                BaseCondition.THIRD -> 1
                BaseCondition.FIRST_SECOND -> 2
                BaseCondition.FIRST_THIRD -> 2
                BaseCondition.SECOND_THIRD -> 2
                BaseCondition.BASED_LOADED -> 3
            }

        if (inningHalf == TOP) {
            updatedAwayScore += runsScored
        } else {
            updatedHomeScore += runsScored
        }

        return AtBatOutcome(
            actualResult = actualResult,
            outs = outs,
            runsScored = runsScored,
            homeScore = updatedHomeScore,
            awayScore = updatedAwayScore,
            runnerOnFirstAfter = null,
            runnerOnSecondAfter = null,
            runnerOnThirdAfter = null,
            baseConditionAfter = BaseCondition.THIRD,
        )
    }

    private fun handleHomeRun(
        outs: Int,
        inningHalf: InningHalf,
        baseConditionBefore: BaseCondition,
        runnerOnFirst: Player?,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
        homeScore: Int,
        awayScore: Int,
    ): AtBatOutcome {
        val actualResult = ActualResult.HOME_RUN
        val runsScored =
            when (baseConditionBefore) {
                BaseCondition.EMPTY -> 1
                BaseCondition.FIRST -> 2
                BaseCondition.SECOND -> 2
                BaseCondition.THIRD -> 2
                BaseCondition.FIRST_SECOND -> 3
                BaseCondition.FIRST_THIRD -> 3
                BaseCondition.SECOND_THIRD -> 3
                BaseCondition.BASED_LOADED -> 4
            }
        var updatedHomeScore = homeScore
        var updatedAwayScore = awayScore

        if (inningHalf == TOP) {
            updatedAwayScore += runsScored
        } else {
            updatedHomeScore += runsScored
        }

        return AtBatOutcome(
            actualResult = actualResult,
            outs = outs,
            runsScored = runsScored,
            homeScore = updatedHomeScore,
            awayScore = updatedAwayScore,
            runnerOnFirstAfter = null,
            runnerOnSecondAfter = null,
            runnerOnThirdAfter = null,
            baseConditionAfter = BaseCondition.EMPTY,
        )
    }
}
