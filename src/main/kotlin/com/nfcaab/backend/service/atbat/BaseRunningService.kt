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
import com.nfcaab.backend.model.AtBat.SubmissionType
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
        batter: Player,
        submissionType: SubmissionType = SubmissionType.SWING,
    ): AtBatOutcome =
        when (result) {
            STRIKEOUT ->
                handleStrikeout(outs, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore)
            Scenario.WALK ->
                handleWalk(outs, inningHalf, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore, batter)
            FLYOUT ->
                handleFlyout(outs, inningHalf, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore)
            LEFT_GROUNDOUT, RIGHT_GROUNDOUT ->
                if (submissionType == SubmissionType.BUNT && baseConditionBefore != BaseCondition.EMPTY) {
                    handleSacrificeBunt(outs, inningHalf, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore, batter, result == RIGHT_GROUNDOUT)
                } else if (result == LEFT_GROUNDOUT) {
                    handleLeftGroundout(outs, inningHalf, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore, batter)
                } else {
                    handleRightGroundout(outs, inningHalf, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore, batter)
                }
            SINGLE ->
                handleSingle(outs, inningHalf, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore, hitDirection, batter)
            DOUBLE ->
                handleDouble(outs, inningHalf, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore, hitDirection, batter)
            TRIPLE ->
                handleTriple(outs, inningHalf, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore, batter)
            HOME_RUN ->
                handleHomeRun(outs, inningHalf, baseConditionBefore, runnerOnFirst, runnerOnSecond, runnerOnThird, homeScore, awayScore, batter)
            else -> throw InvalidScenarioException()
        }

    fun resolveSteal(
        runnerArchetype: BatterArchetype,
        pitcherArchetype: Player.PitcherArchetype,
        difference: Int,
    ): Scenario =
        rangesService.getStealResult(runnerArchetype, pitcherArchetype, difference).result
            ?: throw ResultNotFoundException()

    fun leadRunner(
        runnerOnFirst: Player?,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
    ): Player =
        runnerOnThird ?: runnerOnSecond ?: runnerOnFirst ?: throw InvalidScenarioException()

    fun resolveStealOutcome(
        result: Scenario,
        outs: Int,
        inningHalf: InningHalf,
        runnerOnFirst: Player?,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
        homeScore: Int,
        awayScore: Int,
    ): AtBatOutcome {
        val success = result == Scenario.STEAL_SUCCESS
        val actualResult = if (success) ActualResult.STOLEN_BASE else ActualResult.CAUGHT_STEALING
        var updatedOuts = outs
        var runsScored = 0
        var updatedHomeScore = homeScore
        var updatedAwayScore = awayScore
        var runnerOnFirstAfter = runnerOnFirst
        var runnerOnSecondAfter = runnerOnSecond
        var runnerOnThirdAfter = runnerOnThird
        val scoringRunners = mutableListOf<Player>()

        fun scoreRun(scorer: Player) {
            runsScored += 1
            if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
            scoringRunners.add(scorer)
        }

        when {
            runnerOnFirst != null && runnerOnSecond != null && runnerOnThird == null -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = runnerOnFirst
                runnerOnThirdAfter = if (success) runnerOnSecond else null
                if (!success) updatedOuts = outs + 1
            }
            runnerOnFirst != null && runnerOnThird != null && runnerOnSecond == null -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = runnerOnFirst
                runnerOnThirdAfter = null
                if (success) scoreRun(runnerOnThird) else updatedOuts = outs + 1
            }
            runnerOnSecond != null && runnerOnThird != null && runnerOnFirst == null -> {
                runnerOnSecondAfter = null
                runnerOnThirdAfter = runnerOnSecond
                if (success) scoreRun(runnerOnThird) else updatedOuts = outs + 1
            }
            runnerOnThird != null -> {
                runnerOnThirdAfter = null
                if (success) scoreRun(runnerOnThird) else updatedOuts = outs + 1
            }
            runnerOnSecond != null -> {
                runnerOnSecondAfter = null
                runnerOnThirdAfter = if (success) runnerOnSecond else null
                if (!success) updatedOuts = outs + 1
            }
            runnerOnFirst != null -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = if (success) runnerOnFirst else null
                if (!success) updatedOuts = outs + 1
            }
            else -> throw InvalidScenarioException()
        }

        if (updatedOuts >= 3) {
            runnerOnFirstAfter = null
            runnerOnSecondAfter = null
            runnerOnThirdAfter = null
        }

        val baseConditionAfter =
            when {
                runnerOnFirstAfter != null && runnerOnSecondAfter != null && runnerOnThirdAfter != null -> BaseCondition.BASED_LOADED
                runnerOnFirstAfter != null && runnerOnSecondAfter != null -> BaseCondition.FIRST_SECOND
                runnerOnFirstAfter != null && runnerOnThirdAfter != null -> BaseCondition.FIRST_THIRD
                runnerOnSecondAfter != null && runnerOnThirdAfter != null -> BaseCondition.SECOND_THIRD
                runnerOnFirstAfter != null -> BaseCondition.FIRST
                runnerOnSecondAfter != null -> BaseCondition.SECOND
                runnerOnThirdAfter != null -> BaseCondition.THIRD
                else -> BaseCondition.EMPTY
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
            scoringRunners = scoringRunners,
        )
    }

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

    private fun handleSacrificeBunt(
        outs: Int,
        inningHalf: InningHalf,
        baseConditionBefore: BaseCondition,
        runnerOnFirst: Player?,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
        homeScore: Int,
        awayScore: Int,
        batter: Player,
        bunterSucceeds: Boolean,
    ): AtBatOutcome {
        val updatedOuts = outs + 1

        if (updatedOuts >= 3) {
            return AtBatOutcome(
                actualResult = ActualResult.SACRIFICE_BUNT,
                outs = updatedOuts,
                runsScored = 0,
                homeScore = homeScore,
                awayScore = awayScore,
                runnerOnFirstAfter = null,
                runnerOnSecondAfter = null,
                runnerOnThirdAfter = null,
                baseConditionAfter = BaseCondition.EMPTY,
            )
        }

        if (bunterSucceeds) {
            var updatedHomeScore = homeScore
            var updatedAwayScore = awayScore
            var runsScored = 0
            var scoringRunners: List<Player> = emptyList()
            val runnerOnFirstAfter: Player?
            val runnerOnSecondAfter: Player?
            val runnerOnThirdAfter: Player?
            val baseConditionAfter: BaseCondition
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
                    scoringRunners = listOfNotNull(runnerOnThird)
                    if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                }
                BaseCondition.FIRST_SECOND -> {
                    runnerOnFirstAfter = null
                    runnerOnSecondAfter = runnerOnFirst
                    runnerOnThirdAfter = runnerOnSecond
                    baseConditionAfter = BaseCondition.SECOND_THIRD
                }
                BaseCondition.FIRST_THIRD -> {
                    runnerOnFirstAfter = null
                    runnerOnSecondAfter = runnerOnFirst
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.SECOND
                    runsScored = 1
                    scoringRunners = listOfNotNull(runnerOnThird)
                    if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                }
                BaseCondition.SECOND_THIRD -> {
                    runnerOnFirstAfter = null
                    runnerOnSecondAfter = null
                    runnerOnThirdAfter = runnerOnSecond
                    baseConditionAfter = BaseCondition.THIRD
                    runsScored = 1
                    scoringRunners = listOfNotNull(runnerOnThird)
                    if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                }
                BaseCondition.BASED_LOADED -> {
                    runnerOnFirstAfter = null
                    runnerOnSecondAfter = runnerOnFirst
                    runnerOnThirdAfter = runnerOnSecond
                    baseConditionAfter = BaseCondition.SECOND_THIRD
                    runsScored = 1
                    scoringRunners = listOfNotNull(runnerOnThird)
                    if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                }
                else -> throw InvalidScenarioException()
            }
            return AtBatOutcome(
                actualResult = ActualResult.SACRIFICE_BUNT,
                outs = updatedOuts,
                runsScored = runsScored,
                homeScore = updatedHomeScore,
                awayScore = updatedAwayScore,
                runnerOnFirstAfter = runnerOnFirstAfter,
                runnerOnSecondAfter = runnerOnSecondAfter,
                runnerOnThirdAfter = runnerOnThirdAfter,
                baseConditionAfter = baseConditionAfter,
                scoringRunners = scoringRunners,
            )
        }

        if (baseConditionBefore == BaseCondition.SECOND) {
            return AtBatOutcome(
                actualResult = ActualResult.SACRIFICE_BUNT,
                outs = updatedOuts,
                runsScored = 0,
                homeScore = homeScore,
                awayScore = awayScore,
                runnerOnFirstAfter = null,
                runnerOnSecondAfter = runnerOnSecond,
                runnerOnThirdAfter = null,
                baseConditionAfter = BaseCondition.SECOND,
            )
        }

        val runnerOnSecondAfter: Player?
        val runnerOnThirdAfter: Player?
        val baseConditionAfter: BaseCondition
        when (baseConditionBefore) {
            BaseCondition.FIRST -> {
                runnerOnSecondAfter = null
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST
            }
            BaseCondition.THIRD -> {
                runnerOnSecondAfter = null
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST
            }
            BaseCondition.FIRST_SECOND -> {
                runnerOnSecondAfter = null
                runnerOnThirdAfter = runnerOnSecond
                baseConditionAfter = BaseCondition.FIRST_THIRD
            }
            BaseCondition.FIRST_THIRD -> {
                runnerOnSecondAfter = runnerOnFirst
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST_SECOND
            }
            BaseCondition.SECOND_THIRD -> {
                runnerOnSecondAfter = null
                runnerOnThirdAfter = runnerOnSecond
                baseConditionAfter = BaseCondition.FIRST_THIRD
            }
            BaseCondition.BASED_LOADED -> {
                runnerOnSecondAfter = runnerOnFirst
                runnerOnThirdAfter = runnerOnSecond
                baseConditionAfter = BaseCondition.BASED_LOADED
            }
            else -> throw InvalidScenarioException()
        }
        return AtBatOutcome(
            actualResult = ActualResult.FIELDERS_CHOICE,
            outs = updatedOuts,
            runsScored = 0,
            homeScore = homeScore,
            awayScore = awayScore,
            runnerOnFirstAfter = batter,
            runnerOnSecondAfter = runnerOnSecondAfter,
            runnerOnThirdAfter = runnerOnThirdAfter,
            baseConditionAfter = baseConditionAfter,
        )
    }

    private fun resolveForceOut(
        outs: Int,
        inningHalf: InningHalf,
        baseConditionBefore: BaseCondition,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
        homeScore: Int,
        awayScore: Int,
        batter: Player,
        turnsTwo: Boolean,
    ): AtBatOutcome {
        val actualResult = if (turnsTwo) ActualResult.DOUBLE_PLAY else ActualResult.FIELDERS_CHOICE
        val updatedOuts = outs + if (turnsTwo) 2 else 1

        var runsScored = 0
        var updatedHomeScore = homeScore
        var updatedAwayScore = awayScore
        var scoringRunners: List<Player> = emptyList()

        val runnerOnFirstAfter: Player?
        val runnerOnSecondAfter: Player?
        val runnerOnThirdAfter: Player?
        val baseConditionAfter: BaseCondition

        if (updatedOuts >= 3) {
            runnerOnFirstAfter = null
            runnerOnSecondAfter = null
            runnerOnThirdAfter = null
            baseConditionAfter = BaseCondition.EMPTY
        } else {
            runnerOnFirstAfter = if (turnsTwo) null else batter
            runnerOnSecondAfter = null
            when (baseConditionBefore) {
                BaseCondition.FIRST -> {
                    runnerOnThirdAfter = null
                    baseConditionAfter = if (turnsTwo) BaseCondition.EMPTY else BaseCondition.FIRST
                }
                BaseCondition.FIRST_SECOND -> {
                    runnerOnThirdAfter = runnerOnSecond
                    baseConditionAfter = if (turnsTwo) BaseCondition.THIRD else BaseCondition.FIRST_THIRD
                }
                BaseCondition.FIRST_THIRD -> {
                    runnerOnThirdAfter = runnerOnThird
                    baseConditionAfter = if (turnsTwo) BaseCondition.THIRD else BaseCondition.FIRST_THIRD
                }
                BaseCondition.BASED_LOADED -> {
                    runnerOnThirdAfter = runnerOnSecond
                    baseConditionAfter = if (turnsTwo) BaseCondition.THIRD else BaseCondition.FIRST_THIRD
                    runsScored = 1
                    scoringRunners = listOfNotNull(runnerOnThird)
                    if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                }
                else -> throw InvalidScenarioException()
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
            scoringRunners = scoringRunners,
        )
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
        var scoringRunners: List<Player> = emptyList()
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
                    scoringRunners = listOfNotNull(runnerOnThird)
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
                    scoringRunners = listOfNotNull(runnerOnThird)
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
                    scoringRunners = listOfNotNull(runnerOnThird)
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
                    scoringRunners = listOfNotNull(runnerOnThird)
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
            scoringRunners = scoringRunners,
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
        batter: Player,
    ): AtBatOutcome {
        val actualResult = ActualResult.WALK
        val runsScored = 0
        var runnerOnFirstAfter: Player?
        var runnerOnSecondAfter: Player?
        var runnerOnThirdAfter: Player?
        var baseConditionAfter = baseConditionBefore

        when (baseConditionBefore) {
            BaseCondition.EMPTY -> {
                runnerOnFirstAfter = batter
                runnerOnSecondAfter = null
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST
            }
            BaseCondition.FIRST -> {
                runnerOnFirstAfter = batter
                runnerOnSecondAfter = runnerOnFirst
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST_SECOND
            }
            BaseCondition.SECOND -> {
                runnerOnFirstAfter = batter
                runnerOnSecondAfter = runnerOnSecond
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST_SECOND
            }
            BaseCondition.THIRD -> {
                runnerOnFirstAfter = batter
                runnerOnSecondAfter = null
                runnerOnThirdAfter = runnerOnThird
                baseConditionAfter = BaseCondition.FIRST_THIRD
            }
            BaseCondition.FIRST_SECOND -> {
                runnerOnFirstAfter = batter
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
                    runnerOnFirstAfter = batter,
                    runnerOnSecondAfter = runnerOnFirst,
                    runnerOnThirdAfter = null,
                    baseConditionAfter = BaseCondition.FIRST_SECOND,
                    scoringRunners = listOfNotNull(runnerOnThird),
                )
            }
            BaseCondition.SECOND_THIRD -> {
                runnerOnFirstAfter = batter
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
                    runnerOnFirstAfter = batter,
                    runnerOnSecondAfter = runnerOnFirst,
                    runnerOnThirdAfter = runnerOnSecond,
                    baseConditionAfter = BaseCondition.BASED_LOADED,
                    scoringRunners = listOfNotNull(runnerOnThird),
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
        batter: Player,
    ): AtBatOutcome {
        if (baseConditionBefore.hasRunnerOnFirst()) {
            return resolveForceOut(outs, inningHalf, baseConditionBefore, runnerOnSecond, runnerOnThird, homeScore, awayScore, batter, outs < 2)
        }

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
            runnerOnFirstAfter = runnerOnFirst
            runnerOnSecondAfter = runnerOnSecond
            runnerOnThirdAfter = runnerOnThird
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
        batter: Player,
    ): AtBatOutcome {
        if (baseConditionBefore.hasRunnerOnFirst()) {
            return resolveForceOut(outs, inningHalf, baseConditionBefore, runnerOnSecond, runnerOnThird, homeScore, awayScore, batter, false)
        }

        val actualResult = ActualResult.GROUNDOUT
        val updatedOuts = outs + 1
        var runsScored = 0
        var updatedHomeScore = homeScore
        var updatedAwayScore = awayScore
        var scoringRunners: List<Player> = emptyList()
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
                    scoringRunners = listOfNotNull(runnerOnThird)
                    if (inningHalf == TOP) {
                        updatedAwayScore += 1
                    } else {
                        updatedHomeScore += 1
                    }
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
            scoringRunners = scoringRunners,
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
        batter: Player,
    ): AtBatOutcome {
        val actualResult = ActualResult.SINGLE
        var runsScored = 0
        var updatedHomeScore = homeScore
        var updatedAwayScore = awayScore
        var scoringRunners: List<Player> = emptyList()
        val runnerOnFirstAfter: Player?
        val runnerOnSecondAfter: Player?
        val runnerOnThirdAfter: Player?
        val baseConditionAfter: BaseCondition

        when (baseConditionBefore) {
            BaseCondition.EMPTY -> {
                runnerOnFirstAfter = batter
                runnerOnSecondAfter = null
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST
            }
            BaseCondition.FIRST -> {
                runnerOnFirstAfter = batter
                runnerOnSecondAfter = runnerOnFirst
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST_SECOND
            }
            BaseCondition.SECOND -> {
                runnerOnFirstAfter = batter
                if (runnerTakesExtraBase(direction, runnerOnSecond, outs)) {
                    runnerOnSecondAfter = null
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.FIRST
                    runsScored = 1
                    scoringRunners = listOfNotNull(runnerOnSecond)
                    if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                } else {
                    runnerOnSecondAfter = null
                    runnerOnThirdAfter = runnerOnSecond
                    baseConditionAfter = BaseCondition.FIRST_THIRD
                }
            }
            BaseCondition.THIRD -> {
                runnerOnFirstAfter = batter
                runnerOnSecondAfter = null
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST
                runsScored = 1
                scoringRunners = listOfNotNull(runnerOnThird)
                if (inningHalf == TOP) {
                    updatedAwayScore += 1
                } else {
                    updatedHomeScore += 1
                }
            }
            BaseCondition.FIRST_SECOND -> {
                runnerOnFirstAfter = batter
                if (runnerTakesExtraBase(direction, runnerOnSecond, outs)) {
                    runnerOnSecondAfter = runnerOnFirst
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.FIRST_SECOND
                    runsScored = 1
                    scoringRunners = listOfNotNull(runnerOnSecond)
                    if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                } else {
                    runnerOnSecondAfter = runnerOnFirst
                    runnerOnThirdAfter = runnerOnSecond
                    baseConditionAfter = BaseCondition.BASED_LOADED
                }
            }
            BaseCondition.FIRST_THIRD -> {
                runnerOnFirstAfter = batter
                runnerOnSecondAfter = runnerOnFirst
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST_SECOND
                runsScored = 1
                scoringRunners = listOfNotNull(runnerOnThird)
                if (inningHalf == TOP) {
                    updatedAwayScore += 1
                } else {
                    updatedHomeScore += 1
                }
            }
            BaseCondition.SECOND_THIRD -> {
                runnerOnFirstAfter = batter
                val extra = runnerTakesExtraBase(direction, runnerOnSecond, outs)
                runsScored = if (extra) 2 else 1
                scoringRunners = if (extra) listOfNotNull(runnerOnThird, runnerOnSecond) else listOfNotNull(runnerOnThird)
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
                runnerOnFirstAfter = batter
                val extra = runnerTakesExtraBase(direction, runnerOnSecond, outs)
                runsScored = if (extra) 2 else 1
                scoringRunners = if (extra) listOfNotNull(runnerOnThird, runnerOnSecond) else listOfNotNull(runnerOnThird)
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
            scoringRunners = scoringRunners,
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
        batter: Player,
    ): AtBatOutcome {
        val actualResult = ActualResult.DOUBLE
        var runsScored = 0
        var updatedHomeScore = homeScore
        var updatedAwayScore = awayScore
        var scoringRunners: List<Player> = emptyList()
        val runnerOnFirstAfter: Player?
        val runnerOnSecondAfter: Player?
        val runnerOnThirdAfter: Player?
        val baseConditionAfter: BaseCondition

        when (baseConditionBefore) {
            BaseCondition.EMPTY -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = batter
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.SECOND
            }
            BaseCondition.FIRST -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = batter
                if (runnerTakesExtraBase(direction, runnerOnFirst, outs)) {
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.SECOND
                    runsScored = 1
                    scoringRunners = listOfNotNull(runnerOnFirst)
                    if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                } else {
                    runnerOnThirdAfter = runnerOnFirst
                    baseConditionAfter = BaseCondition.SECOND_THIRD
                }
            }
            BaseCondition.SECOND -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = batter
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.SECOND
                runsScored = 1
                scoringRunners = listOfNotNull(runnerOnSecond)
                if (inningHalf == TOP) {
                    updatedAwayScore += 1
                } else {
                    updatedHomeScore += 1
                }
            }
            BaseCondition.THIRD -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = batter
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.SECOND
                runsScored = 1
                scoringRunners = listOfNotNull(runnerOnThird)
                if (inningHalf == TOP) {
                    updatedAwayScore += 1
                } else {
                    updatedHomeScore += 1
                }
            }
            BaseCondition.FIRST_SECOND -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = batter
                runsScored = 1
                if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                if (runnerTakesExtraBase(direction, runnerOnFirst, outs)) {
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.SECOND
                    runsScored = 2
                    scoringRunners = listOfNotNull(runnerOnSecond, runnerOnFirst)
                    if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                } else {
                    runnerOnThirdAfter = runnerOnFirst
                    baseConditionAfter = BaseCondition.SECOND_THIRD
                    scoringRunners = listOfNotNull(runnerOnSecond)
                }
            }
            BaseCondition.FIRST_THIRD -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = batter
                runsScored = 1
                if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                if (runnerTakesExtraBase(direction, runnerOnFirst, outs)) {
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.SECOND
                    runsScored = 2
                    scoringRunners = listOfNotNull(runnerOnThird, runnerOnFirst)
                    if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                } else {
                    runnerOnThirdAfter = runnerOnFirst
                    baseConditionAfter = BaseCondition.SECOND_THIRD
                    scoringRunners = listOfNotNull(runnerOnThird)
                }
            }
            BaseCondition.SECOND_THIRD -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = batter
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.SECOND
                runsScored = 2
                scoringRunners = listOfNotNull(runnerOnSecond, runnerOnThird)
                if (inningHalf == TOP) {
                    updatedAwayScore += 2
                } else {
                    updatedHomeScore += 2
                }
            }
            BaseCondition.BASED_LOADED -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = batter
                runsScored = 2
                if (inningHalf == TOP) updatedAwayScore += 2 else updatedHomeScore += 2
                if (runnerTakesExtraBase(direction, runnerOnFirst, outs)) {
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.SECOND
                    runsScored = 3
                    scoringRunners = listOfNotNull(runnerOnSecond, runnerOnThird, runnerOnFirst)
                    if (inningHalf == TOP) updatedAwayScore += 1 else updatedHomeScore += 1
                } else {
                    runnerOnThirdAfter = runnerOnFirst
                    baseConditionAfter = BaseCondition.SECOND_THIRD
                    scoringRunners = listOfNotNull(runnerOnSecond, runnerOnThird)
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
            scoringRunners = scoringRunners,
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
        batter: Player,
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
            runnerOnThirdAfter = batter,
            baseConditionAfter = BaseCondition.THIRD,
            scoringRunners = listOfNotNull(runnerOnFirst, runnerOnSecond, runnerOnThird),
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
        batter: Player,
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
            scoringRunners = listOfNotNull(runnerOnFirst, runnerOnSecond, runnerOnThird, batter),
        )
    }
}
