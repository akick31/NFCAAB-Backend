package com.nfcaab.backend.service.atbat

import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.InningHalf.TOP
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.service.game.GameService
import com.nfcaab.backend.service.game.GameLifecycleService
import com.nfcaab.backend.util.AtBatNotFoundException
import com.nfcaab.backend.util.EncryptionUtils
import com.nfcaab.backend.util.Logger
import com.nfcaab.backend.util.PitcherNumberSubmissionNotFound
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class AtBatService(
    private val atBatRepository: AtBatRepository,
    private val encryptionUtils: EncryptionUtils,
    private val gameService: GameService,
    private val gameLifecycleService: GameLifecycleService,
    private val atBatResolutionService: AtBatResolutionService,
    private val buntService: BuntService,
) {
    fun pitchingNumberSubmitted(
        gameId: Int,
        pitcherSubmitter: String,
        pitcherNumberSubmission: Int,
        submissionType: SubmissionType?,
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
                        atBatFinished = false,
                    ),
                )

            gameLifecycleService.updateWithPitcherNumberSubmission(game, atBat)
            return atBat
        } catch (e: Exception) {
            Logger.error("There was an error submitting the pitching number for game $gameId", e)
            throw e
        }
    }

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

            val decryptedPitcherNumber =
                encryptionUtils.decrypt(
                    atBat.pitcherNumberSubmission ?: throw PitcherNumberSubmissionNotFound(),
                )

            if (pitcherSubmissionType != null) {
                when (pitcherSubmissionType) {
                    SubmissionType.INTENTIONAL_WALK ->
                        atBat =
                            atBatResolutionService.resolveIntentionalWalk(
                                atBat,
                                game,
                                submissionType,
                                batterNumberSubmission,
                                decryptedPitcherNumber,
                            )
                    else -> {}
                }
            } else {
                when (submissionType) {
                    SubmissionType.SWING ->
                        atBat =
                            atBatResolutionService.resolveSwing(
                                atBat,
                                game,
                                submissionType,
                                batterNumberSubmission,
                                decryptedPitcherNumber,
                            )
                    SubmissionType.BUNT ->
                        atBat =
                            buntService.resolveBunt(
                                atBat,
                                game,
                                submissionType,
                                batterNumberSubmission,
                                decryptedPitcherNumber,
                            )
                    SubmissionType.STEAL ->
                        atBat =
                            atBatResolutionService.resolveSteal(
                                atBat,
                                game,
                                submissionType,
                                batterNumberSubmission,
                                decryptedPitcherNumber,
                            )
                    else -> {}
                }
            }

            return atBat
        } catch (e: Exception) {
            Logger.error("There was an error submitting the batting number for game $gameId", e)
            throw e
        }
    }

    fun rollbackAtBat(gameId: Int): AtBat {
        try {
            val game = gameService.getGameById(gameId)
            val previousAtBat = getPreviousAtBat(gameId)
            val atBat = getAtBatById(game.currentAtBatId!!)
            gameLifecycleService.rollbackAtBat(game, previousAtBat, atBat)
            atBatRepository.deleteById(atBat.id)
            return previousAtBat
        } catch (e: Exception) {
            Logger.error("There was an error rolling back the play for game $gameId", e)
            throw e
        }
    }

    fun getPreviousAtBat(gameId: Int) =
        atBatRepository.getPreviousAtBat(gameId)
            ?: throw AtBatNotFoundException("No previous play found for game $gameId")

    fun getCurrentAtBat(gameId: Int) =
        atBatRepository.getCurrentAtBat(gameId)
            ?: throw AtBatNotFoundException("No current play found for game $gameId")

    fun getCurrentAtBatOrNull(gameId: Int) = atBatRepository.getCurrentAtBat(gameId)

    fun getAllAtBatsByGameId(gameId: Int) =
        atBatRepository.getAllAtBatsByGameId(gameId).ifEmpty {
            throw AtBatNotFoundException("No plate appearances found for game $gameId")
        }

    fun getAllAtBatsByDiscordTag(discordTag: String) =
        atBatRepository.getAllAtBatsByDiscordTag(discordTag).ifEmpty {
            throw AtBatNotFoundException("No plate appearances found for user $discordTag")
        }

    fun getHomeDelayOfGameInstances(gameId: Int) =
        atBatRepository.getHomeDelayOfGameInstances(gameId)
            ?: throw AtBatNotFoundException("No delay of game instances found for game $gameId")

    fun getAwayDelayOfGameInstances(gameId: Int) =
        atBatRepository.getAwayDelayOfGameInstances(gameId)
            ?: throw AtBatNotFoundException("No delay of game instances found for game $gameId")

    fun getUserAverageResponseTime(
        discordTag: String,
        season: Int,
    ) = atBatRepository.getUserAverageResponseTime(discordTag, season)
        ?: throw Exception("Could not get average response time for user $discordTag")

    private fun getResponseSpeed(game: Game): Long {
        val lastTimestamp = game.lastMessageTimestamp ?: Instant.now()
        val currentTimestamp = Instant.now()
        return Duration.between(lastTimestamp, currentTimestamp).seconds
    }

    private fun getAtBatById(playId: Int) =
        atBatRepository.getAtBatById(playId)
            ?: throw AtBatNotFoundException("AtBatRepository with id $playId not found")

    private fun saveAtBat(atBat: AtBat) = atBatRepository.save(atBat)
}
