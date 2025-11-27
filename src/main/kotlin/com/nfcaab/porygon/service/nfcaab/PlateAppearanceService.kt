package com.nfcaab.porygon.service.nfcaab

import com.nfcaab.porygon.model.Game
import com.nfcaab.porygon.model.Game.ActualResult
import com.nfcaab.porygon.model.Game.BaseCondition
import com.nfcaab.porygon.model.Game.InningHalf
import com.nfcaab.porygon.model.Game.InningHalf.TOP
import com.nfcaab.porygon.model.Game.Scenario
import com.nfcaab.porygon.model.Game.Scenario.DOUBLE
import com.nfcaab.porygon.model.Game.Scenario.FLYOUT
import com.nfcaab.porygon.model.Game.Scenario.HOME_RUN
import com.nfcaab.porygon.model.Game.Scenario.LEFT_GROUNDOUT
import com.nfcaab.porygon.model.Game.Scenario.RIGHT_GROUNDOUT
import com.nfcaab.porygon.model.Game.Scenario.SINGLE
import com.nfcaab.porygon.model.Game.Scenario.STRIKEOUT
import com.nfcaab.porygon.model.Game.Scenario.TRIPLE
import com.nfcaab.porygon.model.PlateAppearance
import com.nfcaab.porygon.model.PlateAppearance.SubmissionType
import com.nfcaab.porygon.model.Player
import com.nfcaab.porygon.model.Player.Archetype
import com.nfcaab.porygon.dto.PlateAppearanceOutcome
import com.nfcaab.porygon.repositories.PlateAppearanceRepository
import com.nfcaab.porygon.service.nfcaab.RangesService
import com.nfcaab.porygon.service.nfcaab.ScorebugService
import com.nfcaab.porygon.util.EncryptionUtils
import com.nfcaab.porygon.util.InvalidScenarioException
import com.nfcaab.porygon.util.Logger
import com.nfcaab.porygon.util.PitcherNumberSubmissionNotFound
import com.nfcaab.porygon.util.PlateAppearanceNotFoundException
import com.nfcaab.porygon.util.ResultNotFoundException
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class PlateAppearanceService(
    private val plateAppearanceRepository: PlateAppearanceRepository,
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
    ): PlateAppearance {
        try {
            val game = gameService.getGameById(gameId)
            val responseSpeed =
                if (game.gameStatus != Game.GameStatus.PREGAME) {
                    getResponseSpeed(game)
                } else {
                    null
                }

            val encryptedPitchNumber = encryptionUtils.encrypt(pitcherNumberSubmission.toString())

            val plateAppearance =
                savePlateAppearance(
                    PlateAppearance(
                        gameId = game.id,
                        pitchNumber = game.numPlateAppearance.plus(1),
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
                        plateAppearanceFinished = false
                    ),
                )

            gameService.updateWithPitcherNumberSubmission(game, plateAppearance)
            return plateAppearance
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
    ): PlateAppearance {
        try {
            val game = gameService.getGameById(gameId)
            var plateAppearance = getPlateAppearanceById(game.currentPlateAppearanceId!!)
            val responseSpeed = getResponseSpeed(game)
            val pitcherSubmissionType = plateAppearance.submissionType

            plateAppearance.batterResponseSpeed = responseSpeed
            plateAppearance.batterSubmitter = batterSubmitter

            val descryptedPitcherNumber = encryptionUtils.decrypt(plateAppearance.pitcherNumberSubmission
                ?: throw PitcherNumberSubmissionNotFound()
            )

            if (pitcherSubmissionType != null) {
                when (pitcherSubmissionType) {
                    SubmissionType.INTENTIONAL_WALK -> plateAppearance =
                        runIntentionalWalk(
                            plateAppearance,
                            game,
                            submissionType,
                            batterNumberSubmission,
                            descryptedPitcherNumber
                        )
                    else -> {}
                }
            } else {
                when (submissionType) {
                    SubmissionType.SWING -> plateAppearance =
                        swing(
                            plateAppearance,
                            game,
                            submissionType,
                            batterNumberSubmission,
                            descryptedPitcherNumber
                        )
                    SubmissionType.BUNT -> plateAppearance =
                        bunt(
                            plateAppearance,
                            game,
                            submissionType,
                            batterNumberSubmission,
                            descryptedPitcherNumber
                        )
                    SubmissionType.STEAL -> plateAppearance =
                        steal(
                            plateAppearance,
                            game,
                            submissionType,
                            batterNumberSubmission,
                            descryptedPitcherNumber
                        )
                    else -> {}
                }
            }

            return plateAppearance
        } catch (e: Exception) {
            Logger.error("There was an error submitting the batting number for game $gameId: " + e.message)
            throw e
        }
    }

    /**
     * Rollback the plate appearance to the previous plate appearance
     * @param gameId
     */
    fun rollbackPlateAppearance(gameId: Int): PlateAppearance {
        try {
            val game = gameService.getGameById(gameId)
            val previousPlateAppearance = getPreviousPlateAppearance(gameId)
            val plateAppearance = getPlateAppearanceById(game.currentPlateAppearanceId!!)
            gameService.rollbackPlateAppearance(game, previousPlateAppearance, plateAppearance)
            plateAppearanceRepository.deleteById(plateAppearance.id)
            return previousPlateAppearance
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
    private fun getPlateAppearanceById(playId: Int) =
        plateAppearanceRepository.getPlateAppearanceById(playId)
            ?: throw PlateAppearanceNotFoundException("PlateAppearanceRepository with id $playId not found")

    /**
     * Get the previous plate appearances of a game
     * @param gameId
     * @return
     */
    fun getPreviousPlateAppearance(gameId: Int) =
        plateAppearanceRepository.getPreviousPlateAppearance(gameId)
            ?: throw PlateAppearanceNotFoundException("No previous play found for game $gameId")

    /**
     * Get the current plate appearances of a game
     * @param gameId
     */
    fun getCurrentPlateAppearance(gameId: Int) =
        plateAppearanceRepository.getCurrentPlateAppearance(gameId)
            ?: throw PlateAppearanceNotFoundException("No current play found for game $gameId")

    /**
     * Get the current plate appearances of a game or null
     * @param gameId
     */
    fun getCurrentPlateAppearanceOrNull(gameId: Int) = plateAppearanceRepository.getCurrentPlateAppearance(gameId)

    /**
     * Get all plate appearances for a game
     * @param gameId
     */
    fun getAllPlateAppearancesByGameId(gameId: Int) =
        plateAppearanceRepository.getAllPlateAppearancesByGameId(gameId).ifEmpty {
            throw PlateAppearanceNotFoundException("No plate appearances found for game $gameId")
        }

    /**
     * Get all plate appearances with a user
     * @param discordTag
     */
    fun getAllPlateAppearancesByDiscordTag(discordTag: String) =
        plateAppearanceRepository.getAllPlateAppearancesByDiscordTag(discordTag).ifEmpty {
            throw PlateAppearanceNotFoundException("No plate appearances found for user $discordTag")
        }

    /**
     * Get the number of delay of game instances for a home team
     * @param gameId
     */
    fun getHomeDelayOfGameInstances(gameId: Int) =
        plateAppearanceRepository.getHomeDelayOfGameInstances(gameId)
            ?: throw PlateAppearanceNotFoundException("No delay of game instances found for game $gameId")

    /**
     * Get the number of delay of game instances for an away team
     * @param gameId
     */
    fun getAwayDelayOfGameInstances(gameId: Int) =
        plateAppearanceRepository.getAwayDelayOfGameInstances(gameId)
            ?: throw PlateAppearanceNotFoundException("No delay of game instances found for game $gameId")

    /**
     * Get the average response time for a user
     * @param discordTag
     * @param season
     */
    fun getUserAverageResponseTime(
        discordTag: String,
        season: Int,
    ) = plateAppearanceRepository.getUserAverageResponseTime(discordTag, season)
        ?: throw Exception("Could not get average response time for user $discordTag")

    /**
     * Runs the play, returns the updated gamePlay
     * @param plateAppearance
     * @param game
     * @param submissionType
     * @param batterNumberSubmission
     * @param decryptedPitcherNumber
     * @return
     */
    private fun swing(
        plateAppearance: PlateAppearance,
        game: Game,
        submissionType: SubmissionType,
        batterNumberSubmission: Int,
        decryptedPitcherNumber: String,
    ): PlateAppearance {
        val difference = gameService.getDifference(batterNumberSubmission, decryptedPitcherNumber.toInt())
        val batter = playerService.getPlayerByNumberAndTeam(
            plateAppearance.battingTeam ?: "",
            plateAppearance.batterUniformNumber,
        )
        val pitcher = playerService.getPlayerByNumberAndTeam(
            plateAppearance.pitchingTeam ?: "",
            decryptedPitcherNumber.toIntOrNull(),
        )
        val resultInformation = rangesService.getResult(
            submissionType,
            batter.archetype,
            pitcher.archetype,
            difference
        )
        var result = resultInformation.result ?: throw ResultNotFoundException()
        var homeScore = game.homeScore
        var awayScore = game.awayScore
        var inning = game.inning
        var inningHalf = game.inningHalf
        var outs = game.outs
        var runsScored = 0
        var runnerOnFirst = plateAppearance.runnerOnFirst?.let {
            playerService.getPlayerByNumberAndTeam(plateAppearance.battingTeam ?: "", it)
        }
        var runnerOnSecond = plateAppearance.runnerOnSecond?.let {
            playerService.getPlayerByNumberAndTeam(plateAppearance.battingTeam ?: "", it)
        }
        var runnerOnThird = plateAppearance.runnerOnThird?.let {
            playerService.getPlayerByNumberAndTeam(plateAppearance.battingTeam ?: "", it)
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

        val allPlateAppearances = plateAppearanceRepository.getAllPlateAppearancesByGameId(game.id)
        gameStatsService.updateGameStats(
            game,
            allPlateAppearances,
        )

        return updatePlateAppearanceValues(
            plateAppearance,
            submissionType,
            result,
            outcome,
            decryptedPitcherNumber,
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
    ): PlateAppearanceOutcome {
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
        return PlateAppearanceOutcome(
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
    ): PlateAppearanceOutcome {
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
                    if (runnerOnSecond?.archetype == Archetype.SPEEDY) {
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
                    if (runnerOnSecond?.archetype == Archetype.SPEEDY) {
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
                    if (runnerOnSecond?.archetype == Archetype.SPEEDY) {
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
                    if (runnerOnSecond?.archetype == Archetype.SPEEDY) {
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
        return PlateAppearanceOutcome(
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
    ): PlateAppearanceOutcome {
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
                return PlateAppearanceOutcome(
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
                baseConditionAfter = BaseCondition.FIRST_SECOND_THIRD
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
                return PlateAppearanceOutcome(
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

        return PlateAppearanceOutcome(
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
    ): PlateAppearanceOutcome {
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

        return PlateAppearanceOutcome(
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
    ): PlateAppearanceOutcome {
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

        return PlateAppearanceOutcome(
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
    ): PlateAppearanceOutcome {
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

        return PlateAppearanceOutcome(
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
    ): PlateAppearanceOutcome {
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

        return PlateAppearanceOutcome(
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
    ): PlateAppearanceOutcome {
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

        return PlateAppearanceOutcome(
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
    ): PlateAppearanceOutcome {
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

        return PlateAppearanceOutcome(
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
        plateAppearance: PlateAppearance,
        game: Game,
        submissionType: SubmissionType,
        batterNumberSubmission: Int,
        decryptedPitcherNumber: String,
    ): PlateAppearance {
        val difference = gameService.getDifference(batterNumberSubmission, decryptedPitcherNumber.toInt())
        val resultInformation = rangesService.getBuntResult(difference)
        var result = resultInformation.result ?: throw ResultNotFoundException()

        // Similar to swing but with different outcomes
        return swing(plateAppearance, game, submissionType, batterNumberSubmission, decryptedPitcherNumber)
    }

    /**
     * Handle steal submission
     */
    private fun steal(
        plateAppearance: PlateAppearance,
        game: Game,
        submissionType: SubmissionType,
        batterNumberSubmission: Int,
        decryptedPitcherNumber: String,
    ): PlateAppearance {
        val difference = gameService.getDifference(batterNumberSubmission, decryptedPitcherNumber.toInt())
        val resultInformation = rangesService.getStealResult(difference)
        var result = resultInformation.result ?: throw ResultNotFoundException()

        // Handle steal attempt logic
        plateAppearance.result = result
        plateAppearance.actualResult = if (result == Scenario.STEAL_SUCCESS) {
            ActualResult.SINGLE // Steal success represented as single
        } else {
            ActualResult.STRIKEOUT // Steal failure
        }
        plateAppearance.difference = difference
        plateAppearance.submissionType = submissionType
        plateAppearance.batterNumberSubmission = batterNumberSubmission.toString()
        plateAppearance.pitcherNumberSubmission = decryptedPitcherNumber
        plateAppearance.plateAppearanceFinished = true

        return savePlateAppearance(plateAppearance)
    }

    /**
     * Handle intentional walk
     */
    private fun runIntentionalWalk(
        plateAppearance: PlateAppearance,
        game: Game,
        submissionType: SubmissionType,
        batterNumberSubmission: Int,
        decryptedPitcherNumber: String,
    ): PlateAppearance {
        val difference = gameService.getDifference(batterNumberSubmission, decryptedPitcherNumber.toInt())
        val outcome = handleWalk(
            game.outs,
            game.inningHalf,
            gameService.getBaseCondition(
                plateAppearance.runnerOnFirst?.let { playerService.getPlayerByNumberAndTeam(plateAppearance.battingTeam ?: "", it) },
                plateAppearance.runnerOnSecond?.let { playerService.getPlayerByNumberAndTeam(plateAppearance.battingTeam ?: "", it) },
                plateAppearance.runnerOnThird?.let { playerService.getPlayerByNumberAndTeam(plateAppearance.battingTeam ?: "", it) },
            ),
            plateAppearance.runnerOnFirst?.let { playerService.getPlayerByNumberAndTeam(plateAppearance.battingTeam ?: "", it) },
            plateAppearance.runnerOnSecond?.let { playerService.getPlayerByNumberAndTeam(plateAppearance.battingTeam ?: "", it) },
            plateAppearance.runnerOnThird?.let { playerService.getPlayerByNumberAndTeam(plateAppearance.battingTeam ?: "", it) },
            game.homeScore,
            game.awayScore,
        )

        gameService.updateGameValues(game, outcome)
        scorebugService.generateScorebug(game)

        val allPlateAppearances = plateAppearanceRepository.getAllPlateAppearancesByGameId(game.id)
        gameStatsService.updateGameStats(game, allPlateAppearances)

        return updatePlateAppearanceValues(
            plateAppearance,
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
     * @param plateAppearance
     * @param submissionType
     * @param result
     * @param outcome
     * @param decryptedPitcherNumber
     * @param batterNumberSubmission
     * @param difference
     * @return
     */
    private fun updatePlateAppearanceValues(
        plateAppearance: PlateAppearance,
        submissionType: SubmissionType,
        result: Scenario,
        outcome: PlateAppearanceOutcome,
        decryptedPitcherNumber: Int,
        batterNumberSubmission: Int,
        difference: Int
    ): PlateAppearance {
        plateAppearance.homeScore = outcome.homeScore
        plateAppearance.awayScore = outcome.awayScore
        plateAppearance.batterNumberSubmission = batterNumberSubmission.toString()
        plateAppearance.pitcherNumberSubmission = decryptedPitcherNumber.toString()
        plateAppearance.difference = difference
        plateAppearance.submissionType = submissionType
        plateAppearance.result = result
        plateAppearance.actualResult = outcome.actualResult
        plateAppearance.runsScored = outcome.runsScored
        plateAppearance.runnerOnFirstAfter = outcome.runnerOnFirstAfter?.uniformNumber
        plateAppearance.runnerOnSecondAfter = outcome.runnerOnSecondAfter?.uniformNumber
        plateAppearance.runnerOnThirdAfter = outcome.runnerOnThirdAfter?.uniformNumber
        plateAppearance.plateAppearanceFinished = true

        return savePlateAppearance(plateAppearance)
    }

    /**
     * Update the batter and pitcher information for the game
     * @param plateAppearance
     */
    private fun updateBatterAndPitcher(
        plateAppearance: PlateAppearance,
        gameId: Int,
    ) {
        val batter = lineupService.getBatterByLineupSpotAndTeam(
            gameId,
            plateAppearance.battingTeam ?: "",
            plateAppearance.lineupSpot ?: 1,
        )
        val pitcher = lineupService.getPitcherByTeam(
            gameId,
            plateAppearance.pitchingTeam ?: "",
        )
        plateAppearance.batterName = "${batter.firstName} ${batter.lastName}"
        plateAppearance.batterUniformNumber = batter.uniformNumber
        plateAppearance.pitcherName = "${pitcher.firstName} ${pitcher.lastName}"
        plateAppearance.pitcherUniformNumber = pitcher.uniformNumber
    }

    /**
     * Save the plate appearance
     */
    private fun savePlateAppearance(plateAppearance: PlateAppearance) =
        plateAppearanceRepository.save(plateAppearance)
}
