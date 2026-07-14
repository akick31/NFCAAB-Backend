package com.nfcaab.backend.service.nfcaab

import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.model.Game.BaseCondition
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
import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.model.Player.BatterArchetype
import com.nfcaab.backend.dto.AtBatOutcome
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.util.EncryptionUtils
import com.nfcaab.backend.util.InvalidScenarioException
import com.nfcaab.backend.util.Logger
import com.nfcaab.backend.util.PitcherNumberSubmissionNotFound
import com.nfcaab.backend.util.AtBatNotFoundException
import com.nfcaab.backend.util.ResultNotFoundException
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class AtBatService(
    private val atBatRepository: AtBatRepository,
    private val encryptionUtils: EncryptionUtils,
    private val gameService: GameService,
    private val gameStatsService: GameStatsService,
    private val rangesService: RangesService,
    private val scorebugService: ScorebugService,
    private val playerService: PlayerService,
    private val lineupService: LineupService,
) {
    /**
     * Start a new plate appearance, the pitching number was submitted. The pitching number is encrypted
     * @param gameId
     * @param pitcherSubmitter
     * @param pitcherNumberSubmission
     * @return
     */
    fun pitchingNumberSubmitted(
        gameId: Int,
        pitcherSubmitter: String,
        pitcherNumberSubmission: Int,
        submissionType: SubmissionType?
    ): AtBat {
        try {
            val game = gameService.getGameById(gameId)
            val responseSpeed =
                if (game.gameStatus != Game.GameStatus.PREGAME) {
                    getResponseSpeed(game)
                } else {
                    null
                }

            val encryptedPitchNumber = encryptionUtils.encrypt(pitcherNumberSubmission.toString())

            val atBat =
                saveAtBat(
                    AtBat(
                        gameId = game.id,
                        pitchNumber = game.numAtBat.plus(1),
                        homeTeam = game.homeTeam,
                        awayTeam = game.awayTeam,
                        homeScore = game.homeScore,
                        awayScore = game.awayScore,
                        inningHalf = game.inningHalf,
                        inning = game.inning,
                        outs = game.outs,
                        pitchingTeam = if (game.inningHalf == TOP) game.homeTeam else game.awayTeam,
                        battingTeam = if (game.inningHalf == TOP) game.awayTeam else game.homeTeam,
                        pitcherSubmitter = pitcherSubmitter,
                        batterSubmitter = null,
                        batterNumberSubmission = null,
                        pitcherNumberSubmission = encryptedPitchNumber,
                        difference = null,
                        submissionType = submissionType,
                        result = null,
                        actualResult = null,
                        runnerOnFirst = game.runnerOnFirst,
                        runnerOnSecond = game.runnerOnSecond,
                        runnerOnThird = game.runnerOnThird,
                        runsScored = 0,
                        runnerOnFirstAfter = null,
                        runnerOnSecondAfter = null,
                        runnerOnThirdAfter = null,
                        lineupSpot = if (game.inningHalf == TOP) game.awayBatterLineupSpot else game.homeBatterLineupSpot,
                        batterName = game.batterName,
                        batterUniformNumber = game.batterUniformNumber,
                        pitcherName = game.pitcherName,
                        pitcherUniformNumber = game.pitcherUniformNumber,
                        winProbability = 0.0F,
                        winProbabilityAdded = 0.0F,
                        batterResponseSpeed = null,
                        pitcherResponseSpeed = responseSpeed,
                        atBatFinished = false
                    ),
                )

            gameService.updateWithPitcherNumberSubmission(game, atBat)
            return atBat
        } catch (e: Exception) {
            Logger.error("There was an error submitting the pitching number for game $gameId: " + e.message)
            throw e
        }
    }

    /**
     * The batting number was submitted, run the plate appearance
     * @param gameId
     * @param batterSubmitter
     * @param batterNumberSubmission
     * @param submissionType
     * @return
     */
    fun batterNumberSubmitted(
        gameId: Int,
        batterSubmitter: String,
        batterNumberSubmission: Int,
        submissionType: SubmissionType,
    ): AtBat {
        try {
            val game = gameService.getGameById(gameId)
            var atBat = getAtBatById(game.currentAtBatId!!)
            val responseSpeed = getResponseSpeed(game)
            val pitcherSubmissionType = atBat.submissionType

            atBat.batterResponseSpeed = responseSpeed
            atBat.batterSubmitter = batterSubmitter

            val descryptedPitcherNumber = encryptionUtils.decrypt(atBat.pitcherNumberSubmission
                ?: throw PitcherNumberSubmissionNotFound()
            )

            if (pitcherSubmissionType != null) {
                when (pitcherSubmissionType) {
                    SubmissionType.INTENTIONAL_WALK -> atBat =
                        runIntentionalWalk(
                            atBat,
                            game,
                            submissionType,
                            batterNumberSubmission,
                            descryptedPitcherNumber
                        )
                    else -> {}
                }
            } else {
                when (submissionType) {
                    SubmissionType.SWING -> atBat =
                        swing(
                            atBat,
                            game,
                            submissionType,
                            batterNumberSubmission,
                            descryptedPitcherNumber
                        )
                    SubmissionType.BUNT -> atBat =
                        bunt(
                            atBat,
                            game,
                            submissionType,
                            batterNumberSubmission,
                            descryptedPitcherNumber
                        )
                    SubmissionType.STEAL -> atBat =
                        steal(
                            atBat,
                            game,
                            submissionType,
                            batterNumberSubmission,
                            descryptedPitcherNumber
                        )
                    else -> {}
                }
            }

            return atBat
        } catch (e: Exception) {
            Logger.error("There was an error submitting the batting number for game $gameId: " + e.message)
            throw e
        }
    }

    /**
     * Rollback the plate appearance to the previous plate appearance
     * @param gameId
     */
    fun rollbackAtBat(gameId: Int): AtBat {
        try {
            val game = gameService.getGameById(gameId)
            val previousAtBat = getPreviousAtBat(gameId)
            val atBat = getAtBatById(game.currentAtBatId!!)
            gameService.rollbackAtBat(game, previousAtBat, atBat)
            atBatRepository.deleteById(atBat.id)
            return previousAtBat
        } catch (e: Exception) {
            Logger.error("There was an error rolling back the play for game $gameId: " + e.message)
            throw e
        }
    }

    /**
     * Get the response speed
     * @param game
     */
    private fun getResponseSpeed(game: Game): Long {
        val lastTimestamp = game.lastMessageTimestamp ?: Instant.now()
        val currentTimestamp = Instant.now()
        return Duration.between(lastTimestamp, currentTimestamp).seconds
    }

    /**
     * Get a plate appearances by its id
     * @param playId
     * @return
     */
    private fun getAtBatById(playId: Int) =
        atBatRepository.getAtBatById(playId)
            ?: throw AtBatNotFoundException("AtBatRepository with id $playId not found")

    /**
     * Get the previous plate appearances of a game
     * @param gameId
     * @return
     */
    fun getPreviousAtBat(gameId: Int) =
        atBatRepository.getPreviousAtBat(gameId)
            ?: throw AtBatNotFoundException("No previous play found for game $gameId")

    /**
     * Get the current plate appearances of a game
     * @param gameId
     */
    fun getCurrentAtBat(gameId: Int) =
        atBatRepository.getCurrentAtBat(gameId)
            ?: throw AtBatNotFoundException("No current play found for game $gameId")

    /**
     * Get the current plate appearances of a game or null
     * @param gameId
     */
    fun getCurrentAtBatOrNull(gameId: Int) = atBatRepository.getCurrentAtBat(gameId)

    /**
     * Get all plate appearances for a game
     * @param gameId
     */
    fun getAllAtBatsByGameId(gameId: Int) =
        atBatRepository.getAllAtBatsByGameId(gameId).ifEmpty {
            throw AtBatNotFoundException("No plate appearances found for game $gameId")
        }

    /**
     * Get all plate appearances with a user
     * @param discordTag
     */
    fun getAllAtBatsByDiscordTag(discordTag: String) =
        atBatRepository.getAllAtBatsByDiscordTag(discordTag).ifEmpty {
            throw AtBatNotFoundException("No plate appearances found for user $discordTag")
        }

    /**
     * Get the number of delay of game instances for a home team
     * @param gameId
     */
    fun getHomeDelayOfGameInstances(gameId: Int) =
        atBatRepository.getHomeDelayOfGameInstances(gameId)
            ?: throw AtBatNotFoundException("No delay of game instances found for game $gameId")

    /**
     * Get the number of delay of game instances for an away team
     * @param gameId
     */
    fun getAwayDelayOfGameInstances(gameId: Int) =
        atBatRepository.getAwayDelayOfGameInstances(gameId)
            ?: throw AtBatNotFoundException("No delay of game instances found for game $gameId")

    /**
     * Get the average response time for a user
     * @param discordTag
     * @param season
     */
    fun getUserAverageResponseTime(
        discordTag: String,
        season: Int,
    ) = atBatRepository.getUserAverageResponseTime(discordTag, season)
        ?: throw Exception("Could not get average response time for user $discordTag")

    /**
     * Runs the play, returns the updated gamePlay
     * @param atBat
     * @param game
     * @param submissionType
     * @param batterNumberSubmission
     * @param decryptedPitcherNumber
     * @return
     */
    private fun swing(
        atBat: AtBat,
        game: Game,
        submissionType: SubmissionType,
        batterNumberSubmission: Int,
        decryptedPitcherNumber: String,
    ): AtBat {
        val difference = gameService.getDifference(batterNumberSubmission, decryptedPitcherNumber.toInt())
        val batter = playerService.getPlayerByNumberAndTeam(
            atBat.battingTeam ?: "",
            atBat.batterUniformNumber,
        )
        val pitcher = playerService.getPlayerByNumberAndTeam(
            atBat.pitchingTeam ?: "",
            decryptedPitcherNumber.toIntOrNull(),
        )
        val resultInformation = rangesService.getResult(
            submissionType,
            batter.batterArchetype ?: Player.BatterArchetype.NEUTRAL,
            pitcher.pitcherArchetype ?: Player.PitcherArchetype.NEUTRAL,
            difference
        )
        var result = resultInformation.result ?: throw ResultNotFoundException()
        var homeScore = game.homeScore
        var awayScore = game.awayScore
        var inning = game.inning
        var inningHalf = game.inningHalf
        var outs = game.outs
        var runsScored = 0
        var runnerOnFirst = atBat.runnerOnFirst?.let {
            playerService.getPlayerByNumberAndTeam(atBat.battingTeam ?: "", it)
        }
        var runnerOnSecond = atBat.runnerOnSecond?.let {
            playerService.getPlayerByNumberAndTeam(atBat.battingTeam ?: "", it)
        }
        var runnerOnThird = atBat.runnerOnThird?.let {
            playerService.getPlayerByNumberAndTeam(atBat.battingTeam ?: "", it)
        }
        var baseConditionBefore = gameService.getBaseCondition(
            runnerOnFirst,
            runnerOnSecond,
            runnerOnThird,
        )
        var runnerOnFirstAfter = runnerOnFirst
        var runnerOnSecondAfter = runnerOnSecond
        var runnerOnThirdAfter = runnerOnThird
        var baseConditionAfter = baseConditionBefore
        var actualResult: ActualResult
        val outcome = when (result) {
            STRIKEOUT -> {
                handleStrikeout(
                    outs,
                    baseConditionBefore,
                    runnerOnFirst,
                    runnerOnSecond,
                    runnerOnThird,
                    homeScore,
                    awayScore,
                )
            }
            Scenario.WALK -> {
                handleWalk(
                    outs,
                    inningHalf,
                    baseConditionBefore,
                    runnerOnFirst,
                    runnerOnSecond,
                    runnerOnThird,
                    homeScore,
                    awayScore,
                )
            }
            FLYOUT -> {
                handleFlyout(
                    outs,
                    inningHalf,
                    baseConditionBefore,
                    runnerOnFirst,
                    runnerOnSecond,
                    runnerOnThird,
                    homeScore,
                    awayScore,
                )
            }
            LEFT_GROUNDOUT -> {
                handleLeftGroundout(
                    outs,
                    inningHalf,
                    baseConditionBefore,
                    runnerOnFirst,
                    runnerOnSecond,
                    runnerOnThird,
                    homeScore,
                    awayScore,
                )
            }
            RIGHT_GROUNDOUT -> {
                handleRightGroundout(
                    outs,
                    inningHalf,
                    baseConditionBefore,
                    runnerOnFirst,
                    runnerOnSecond,
                    runnerOnThird,
                    homeScore,
                    awayScore,
                )
            }
            SINGLE -> {
                handleSingle(
                    outs,
                    inningHalf,
                    baseConditionBefore,
                    runnerOnFirst,
                    runnerOnSecond,
                    runnerOnThird,
                    homeScore,
                    awayScore,
                )
            }
            DOUBLE -> {
                handleDouble(
                    outs,
                    inningHalf,
                    baseConditionBefore,
                    runnerOnFirst,
                    runnerOnSecond,
                    runnerOnThird,
                    homeScore,
                    awayScore,
                )
            }
            TRIPLE -> {
                handleTriple(
                    outs,
                    inningHalf,
                    baseConditionBefore,
                    runnerOnFirst,
                    runnerOnSecond,
                    runnerOnThird,
                    homeScore,
                    awayScore,
                )
            }
            HOME_RUN -> {
                handleHomeRun(
                    outs,
                    inningHalf,
                    baseConditionBefore,
                    runnerOnFirst,
                    runnerOnSecond,
                    runnerOnThird,
                    homeScore,
                    awayScore,
                )
            }
            else -> throw InvalidScenarioException()
        }

        gameService.updateGameValues(
            game,
            outcome
        )

        scorebugService.generateScorebug(game)

        val allAtBats = atBatRepository.getAllAtBatsByGameId(game.id)
        gameStatsService.updateGameStats(
            game,
            allAtBats,
        )

        return updateAtBatValues(
            atBat,
            submissionType,
            result,
            outcome,
            decryptedPitcherNumber.toInt(),
            batterNumberSubmission,
            difference
        )
    }

    /**
     * Handle the strikeout scenario
     * @param outs
     * @param baseConditionBefore
     * @param runnerOnFirst
     * @param runnerOnSecond
     * @param runnerOnThird
     * @param homeScore
     * @param awayScore
     */
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

    /**
     * Handle the flyout scenario
     * @param outs
     * @param inningHalf
     * @param baseConditionBefore
     * @param runnerOnFirst
     * @param runnerOnSecond
     * @param runnerOnThird
     * @param homeScore
     * @param awayScore
     */
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

    /**
     * Handle the walk scenario
     */
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

        // Base advancement logic for walk
        when (baseConditionBefore) {
            BaseCondition.EMPTY -> {
                runnerOnFirstAfter = null // Batter becomes runner on first
                runnerOnSecondAfter = null
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST
            }
            BaseCondition.FIRST -> {
                runnerOnFirstAfter = null // Batter becomes runner on first
                runnerOnSecondAfter = runnerOnFirst // Runner moves to second
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.FIRST_SECOND
            }
            BaseCondition.SECOND -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = runnerOnSecond
                runnerOnThirdAfter = null // Batter becomes runner on first
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
                runnerOnThirdAfter = runnerOnSecond // Runner moves to third
                baseConditionAfter = BaseCondition.FIRST_THIRD
            }
            BaseCondition.FIRST_THIRD -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = runnerOnFirst
                runnerOnThirdAfter = runnerOnThird
                // Runner on third scores
                var updatedHomeScore = homeScore
                var updatedAwayScore = awayScore
                if (inningHalf == TOP) {
                    updatedAwayScore += 1
                } else {
                    updatedHomeScore += 1
                }
                baseConditionAfter = BaseCondition.FIRST_SECOND
                return AtBatOutcome(
                    actualResult = actualResult,
                    outs = outs,
                    runsScored = 1,
                    homeScore = updatedHomeScore,
                    awayScore = updatedAwayScore,
                    runnerOnFirstAfter = null,
                    runnerOnSecondAfter = runnerOnFirst,
                    runnerOnThirdAfter = null,
                    baseConditionAfter = baseConditionAfter,
                )
            }
            BaseCondition.SECOND_THIRD -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = runnerOnSecond
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.BASED_LOADED
            }
            BaseCondition.BASED_LOADED -> {
                // Runner on third scores
                runnerOnFirstAfter = null
                runnerOnSecondAfter = runnerOnFirst
                runnerOnThirdAfter = runnerOnSecond
                baseConditionAfter = BaseCondition.BASED_LOADED
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
                    runnerOnFirstAfter = runnerOnFirstAfter,
                    runnerOnSecondAfter = runnerOnSecondAfter,
                    runnerOnThirdAfter = runnerOnThirdAfter,
                    baseConditionAfter = baseConditionAfter,
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

    /**
     * Handle the left groundout scenario
     */
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
        var runsScored = 0
        var updatedHomeScore = homeScore
        var updatedAwayScore = awayScore
        var runnerOnFirstAfter: Player?
        var runnerOnSecondAfter: Player?
        var runnerOnThirdAfter: Player?
        var baseConditionAfter = baseConditionBefore

        if (updatedOuts >= 3) {
            runnerOnFirstAfter = null
            runnerOnSecondAfter = null
            runnerOnThirdAfter = null
            baseConditionAfter = BaseCondition.EMPTY
        } else {
            // Left side groundout - typically double play opportunity
            when (baseConditionBefore) {
                BaseCondition.FIRST -> {
                    // Double play opportunity
                    runnerOnFirstAfter = null
                    runnerOnSecondAfter = null
                    runnerOnThirdAfter = null
                    baseConditionAfter = BaseCondition.EMPTY
                }
                BaseCondition.FIRST_SECOND -> {
                    runnerOnFirstAfter = null
                    runnerOnSecondAfter = null
                    runnerOnThirdAfter = runnerOnSecond // Runner advances to third
                    baseConditionAfter = BaseCondition.THIRD
                }
                else -> {
                    // Other scenarios - runner may advance or stay
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

    /**
     * Handle the right groundout scenario
     */
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
        var runnerOnFirstAfter: Player?
        var runnerOnSecondAfter: Player?
        var runnerOnThirdAfter: Player?
        var baseConditionAfter = baseConditionBefore

        if (updatedOuts >= 3) {
            runnerOnFirstAfter = null
            runnerOnSecondAfter = null
            runnerOnThirdAfter = null
            baseConditionAfter = BaseCondition.EMPTY
        } else {
            // Right side groundout - runner typically advances
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

    /**
     * Handle the single scenario
     */
    private fun handleSingle(
        outs: Int,
        inningHalf: InningHalf,
        baseConditionBefore: BaseCondition,
        runnerOnFirst: Player?,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
        homeScore: Int,
        awayScore: Int,
    ): AtBatOutcome {
        val actualResult = ActualResult.SINGLE
        var runsScored = 0
        var updatedHomeScore = homeScore
        var updatedAwayScore = awayScore
        var runnerOnFirstAfter: Player?
        var runnerOnSecondAfter: Player?
        var runnerOnThirdAfter: Player?
        var baseConditionAfter: BaseCondition

        when (baseConditionBefore) {
            BaseCondition.EMPTY -> {
                runnerOnFirstAfter = null // Batter becomes runner on first
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
                runnerOnSecondAfter = null
                runnerOnThirdAfter = runnerOnSecond
                baseConditionAfter = BaseCondition.FIRST_THIRD
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
                runnerOnSecondAfter = runnerOnFirst
                runnerOnThirdAfter = runnerOnSecond
                baseConditionAfter = BaseCondition.BASED_LOADED
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
                runnerOnSecondAfter = null
                runnerOnThirdAfter = runnerOnSecond
                baseConditionAfter = BaseCondition.FIRST_THIRD
                runsScored = 1
                if (inningHalf == TOP) {
                    updatedAwayScore += 1
                } else {
                    updatedHomeScore += 1
                }
            }
            BaseCondition.BASED_LOADED -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = runnerOnFirst
                runnerOnThirdAfter = runnerOnSecond
                baseConditionAfter = BaseCondition.BASED_LOADED
                runsScored = 1
                if (inningHalf == TOP) {
                    updatedAwayScore += 1
                } else {
                    updatedHomeScore += 1
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

    /**
     * Handle the double scenario
     */
    private fun handleDouble(
        outs: Int,
        inningHalf: InningHalf,
        baseConditionBefore: BaseCondition,
        runnerOnFirst: Player?,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
        homeScore: Int,
        awayScore: Int,
    ): AtBatOutcome {
        val actualResult = ActualResult.DOUBLE
        var runsScored = 0
        var updatedHomeScore = homeScore
        var updatedAwayScore = awayScore
        var runnerOnFirstAfter: Player?
        var runnerOnSecondAfter: Player?
        var runnerOnThirdAfter: Player?
        var baseConditionAfter: BaseCondition

        when (baseConditionBefore) {
            BaseCondition.EMPTY -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = null // Batter becomes runner on second
                runnerOnThirdAfter = null
                baseConditionAfter = BaseCondition.SECOND
            }
            BaseCondition.FIRST -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = null
                runnerOnThirdAfter = runnerOnFirst
                baseConditionAfter = BaseCondition.SECOND_THIRD
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
                runnerOnThirdAfter = runnerOnFirst
                baseConditionAfter = BaseCondition.SECOND_THIRD
                runsScored = 1
                if (inningHalf == TOP) {
                    updatedAwayScore += 1
                } else {
                    updatedHomeScore += 1
                }
            }
            BaseCondition.FIRST_THIRD -> {
                runnerOnFirstAfter = null
                runnerOnSecondAfter = null
                runnerOnThirdAfter = runnerOnFirst
                baseConditionAfter = BaseCondition.SECOND_THIRD
                runsScored = 2
                if (inningHalf == TOP) {
                    updatedAwayScore += 2
                } else {
                    updatedHomeScore += 2
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
                runnerOnThirdAfter = runnerOnFirst
                baseConditionAfter = BaseCondition.SECOND_THIRD
                runsScored = 2
                if (inningHalf == TOP) {
                    updatedAwayScore += 2
                } else {
                    updatedHomeScore += 2
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

    /**
     * Handle the triple scenario
     */
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
        var runsScored = 0
        var updatedHomeScore = homeScore
        var updatedAwayScore = awayScore
        var runnerOnFirstAfter: Player?
        var runnerOnSecondAfter: Player?
        var runnerOnThirdAfter: Player?
        var baseConditionAfter: BaseCondition

        // All runners score on a triple
        runsScored = when (baseConditionBefore) {
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

        runnerOnFirstAfter = null
        runnerOnSecondAfter = null
        runnerOnThirdAfter = null // Batter becomes runner on third
        baseConditionAfter = BaseCondition.THIRD

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

    /**
     * Handle the home run scenario
     */
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
        val runsScored = when (baseConditionBefore) {
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

    /**
     * Handle bunt submission
     */
    private fun bunt(
        atBat: AtBat,
        game: Game,
        submissionType: SubmissionType,
        batterNumberSubmission: Int,
        decryptedPitcherNumber: String,
    ): AtBat {
        return swing(atBat, game, submissionType, batterNumberSubmission, decryptedPitcherNumber)
    }

    /**
     * Handle steal submission
     */
    private fun steal(
        atBat: AtBat,
        game: Game,
        submissionType: SubmissionType,
        batterNumberSubmission: Int,
        decryptedPitcherNumber: String,
    ): AtBat {
        val difference = gameService.getDifference(batterNumberSubmission, decryptedPitcherNumber.toInt())
        val runner = playerService.getPlayerByNumberAndTeam(
            atBat.battingTeam ?: "",
            atBat.batterUniformNumber,
        )
        val pitcher = playerService.getPlayerByNumberAndTeam(
            atBat.pitchingTeam ?: "",
            decryptedPitcherNumber.toIntOrNull(),
        )
        val resultInformation = rangesService.getStealResult(
            runner.batterArchetype ?: Player.BatterArchetype.NEUTRAL,
            pitcher.pitcherArchetype ?: Player.PitcherArchetype.NEUTRAL,
            difference,
        )
        var result = resultInformation.result ?: throw ResultNotFoundException()

        // Handle steal attempt logic
        atBat.result = result
        atBat.actualResult = if (result == Scenario.STEAL_SUCCESS) {
            ActualResult.SINGLE // Steal success represented as single
        } else {
            ActualResult.STRIKEOUT // Steal failure
        }
        atBat.difference = difference
        atBat.submissionType = submissionType
        atBat.batterNumberSubmission = encryptionUtils.encrypt(batterNumberSubmission.toString())
        atBat.pitcherNumberSubmission = encryptionUtils.encrypt(decryptedPitcherNumber)
        atBat.atBatFinished = true

        return saveAtBat(atBat)
    }

    /**
     * Handle intentional walk
     */
    private fun runIntentionalWalk(
        atBat: AtBat,
        game: Game,
        submissionType: SubmissionType,
        batterNumberSubmission: Int,
        decryptedPitcherNumber: String,
    ): AtBat {
        val difference = gameService.getDifference(batterNumberSubmission, decryptedPitcherNumber.toInt())
        val outcome = handleWalk(
            game.outs,
            game.inningHalf,
            gameService.getBaseCondition(
                atBat.runnerOnFirst?.let { playerService.getPlayerByNumberAndTeam(atBat.battingTeam ?: "", it) },
                atBat.runnerOnSecond?.let { playerService.getPlayerByNumberAndTeam(atBat.battingTeam ?: "", it) },
                atBat.runnerOnThird?.let { playerService.getPlayerByNumberAndTeam(atBat.battingTeam ?: "", it) },
            ),
            atBat.runnerOnFirst?.let { playerService.getPlayerByNumberAndTeam(atBat.battingTeam ?: "", it) },
            atBat.runnerOnSecond?.let { playerService.getPlayerByNumberAndTeam(atBat.battingTeam ?: "", it) },
            atBat.runnerOnThird?.let { playerService.getPlayerByNumberAndTeam(atBat.battingTeam ?: "", it) },
            game.homeScore,
            game.awayScore,
        )

        gameService.updateGameValues(game, outcome)
        scorebugService.generateScorebug(game)

        val allAtBats = atBatRepository.getAllAtBatsByGameId(game.id)
        gameStatsService.updateGameStats(game, allAtBats)

        return updateAtBatValues(
            atBat,
            submissionType,
            Scenario.WALK,
            outcome,
            decryptedPitcherNumber.toInt(),
            batterNumberSubmission,
            difference
        )
    }

    /**
     * Update the plate appearance values
     * @param atBat
     * @param submissionType
     * @param result
     * @param outcome
     * @param decryptedPitcherNumber
     * @param batterNumberSubmission
     * @param difference
     * @return
     */
    private fun updateAtBatValues(
        atBat: AtBat,
        submissionType: SubmissionType,
        result: Scenario,
        outcome: AtBatOutcome,
        decryptedPitcherNumber: Int,
        batterNumberSubmission: Int,
        difference: Int
    ): AtBat {
        atBat.homeScore = outcome.homeScore
        atBat.awayScore = outcome.awayScore
        atBat.batterNumberSubmission = encryptionUtils.encrypt(batterNumberSubmission.toString())
        atBat.pitcherNumberSubmission = encryptionUtils.encrypt(decryptedPitcherNumber.toString())
        atBat.difference = difference
        atBat.submissionType = submissionType
        atBat.result = result
        atBat.actualResult = outcome.actualResult
        atBat.runsScored = outcome.runsScored
        atBat.runnerOnFirstAfter = outcome.runnerOnFirstAfter?.uniformNumber
        atBat.runnerOnSecondAfter = outcome.runnerOnSecondAfter?.uniformNumber
        atBat.runnerOnThirdAfter = outcome.runnerOnThirdAfter?.uniformNumber
        atBat.atBatFinished = true

        return saveAtBat(atBat)
    }

    /**
     * Update the batter and pitcher information for the game
     * @param atBat
     */
    private fun updateBatterAndPitcher(
        atBat: AtBat,
        gameId: Int,
    ) {
        val batter = lineupService.getBatterByLineupSpotAndTeam(
            gameId,
            atBat.battingTeam ?: "",
            atBat.lineupSpot ?: 1,
        )
        val pitcher = lineupService.getPitcherByTeam(
            gameId,
            atBat.pitchingTeam ?: "",
        )
        atBat.batterName = "${batter.firstName} ${batter.lastName}"
        atBat.batterUniformNumber = batter.uniformNumber
        atBat.pitcherName = "${pitcher.firstName} ${pitcher.lastName}"
        atBat.pitcherUniformNumber = pitcher.uniformNumber
    }

    /**
     * Save the plate appearance
     */
    private fun saveAtBat(atBat: AtBat) =
        atBatRepository.save(atBat)
}
