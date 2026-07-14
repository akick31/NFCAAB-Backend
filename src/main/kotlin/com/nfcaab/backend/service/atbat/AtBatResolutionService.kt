package com.nfcaab.backend.service.atbat

import com.nfcaab.backend.dto.AtBatOutcome
import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.model.Game.Scenario
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.service.game.GameService
import com.nfcaab.backend.service.game.GameLifecycleService
import com.nfcaab.backend.service.player.PlayerService
import com.nfcaab.backend.service.scorebug.ScorebugService
import com.nfcaab.backend.service.stats.GameStatsService
import com.nfcaab.backend.util.EncryptionUtils
import com.nfcaab.backend.util.ResultNotFoundException
import org.springframework.stereotype.Service

@Service
class AtBatResolutionService(
    private val atBatRepository: AtBatRepository,
    private val encryptionUtils: EncryptionUtils,
    private val gameService: GameService,
    private val gameLifecycleService: GameLifecycleService,
    private val gameStatsService: GameStatsService,
    private val rangesService: RangesService,
    private val scorebugService: ScorebugService,
    private val playerService: PlayerService,
    private val baseRunningService: BaseRunningService,
    private val hitLocationService: HitLocationService,
) {
    fun resolveSwing(
        atBat: AtBat,
        game: Game,
        submissionType: SubmissionType,
        batterNumberSubmission: Int,
        decryptedPitcherNumber: String,
    ): AtBat {
        val difference = gameService.getDifference(batterNumberSubmission, decryptedPitcherNumber.toInt())
        val batter =
            playerService.getPlayerByNumberAndTeam(
                atBat.battingTeam ?: "",
                atBat.batterUniformNumber,
            )
        val pitcher =
            playerService.getPlayerByNumberAndTeam(
                atBat.pitchingTeam ?: "",
                decryptedPitcherNumber.toIntOrNull(),
            )
        val resultInformation =
            rangesService.getResult(
                submissionType,
                batter.batterArchetype ?: Player.BatterArchetype.NEUTRAL,
                pitcher.pitcherArchetype ?: Player.PitcherArchetype.NEUTRAL,
                difference,
            )
        val result = resultInformation.result ?: throw ResultNotFoundException()

        val hitLocation =
            hitLocationService.determine(
                result,
                batter.batterArchetype ?: Player.BatterArchetype.NEUTRAL,
                batterNumberSubmission,
                difference,
            )

        val runnerOnFirst = atBat.runnerOnFirst?.let { playerService.getPlayerByNumberAndTeam(atBat.battingTeam ?: "", it) }
        val runnerOnSecond = atBat.runnerOnSecond?.let { playerService.getPlayerByNumberAndTeam(atBat.battingTeam ?: "", it) }
        val runnerOnThird = atBat.runnerOnThird?.let { playerService.getPlayerByNumberAndTeam(atBat.battingTeam ?: "", it) }
        val baseConditionBefore = gameService.getBaseCondition(runnerOnFirst, runnerOnSecond, runnerOnThird)

        val outcome =
            baseRunningService.resolveOutcome(
                result,
                game.outs,
                game.inningHalf,
                baseConditionBefore,
                runnerOnFirst,
                runnerOnSecond,
                runnerOnThird,
                game.homeScore,
                game.awayScore,
                hitLocation.direction,
            )

        gameLifecycleService.updateGameValues(game, outcome)
        scorebugService.generateScorebug(game)

        val allAtBats = atBatRepository.getAllAtBatsByGameId(game.id)
        gameStatsService.updateGameStats(game, allAtBats)

        return updateAtBatValues(
            atBat,
            submissionType,
            result,
            outcome,
            decryptedPitcherNumber.toInt(),
            batterNumberSubmission,
            difference,
            hitLocation,
        )
    }

    fun resolveSteal(
        atBat: AtBat,
        submissionType: SubmissionType,
        batterNumberSubmission: Int,
        decryptedPitcherNumber: String,
    ): AtBat {
        val difference = gameService.getDifference(batterNumberSubmission, decryptedPitcherNumber.toInt())
        val runner =
            playerService.getPlayerByNumberAndTeam(
                atBat.battingTeam ?: "",
                atBat.batterUniformNumber,
            )
        val pitcher =
            playerService.getPlayerByNumberAndTeam(
                atBat.pitchingTeam ?: "",
                decryptedPitcherNumber.toIntOrNull(),
            )
        val result =
            baseRunningService.resolveSteal(
                runner.batterArchetype ?: Player.BatterArchetype.NEUTRAL,
                pitcher.pitcherArchetype ?: Player.PitcherArchetype.NEUTRAL,
                difference,
            )

        atBat.result = result
        atBat.actualResult = if (result == Scenario.STEAL_SUCCESS) ActualResult.SINGLE else ActualResult.STRIKEOUT
        atBat.difference = difference
        atBat.submissionType = submissionType
        atBat.batterNumberSubmission = encryptionUtils.encrypt(batterNumberSubmission.toString())
        atBat.pitcherNumberSubmission = encryptionUtils.encrypt(decryptedPitcherNumber)
        atBat.atBatFinished = true

        return saveAtBat(atBat)
    }

    fun resolveIntentionalWalk(
        atBat: AtBat,
        game: Game,
        submissionType: SubmissionType,
        batterNumberSubmission: Int,
        decryptedPitcherNumber: String,
    ): AtBat {
        val difference = gameService.getDifference(batterNumberSubmission, decryptedPitcherNumber.toInt())
        val runnerOnFirst = atBat.runnerOnFirst?.let { playerService.getPlayerByNumberAndTeam(atBat.battingTeam ?: "", it) }
        val runnerOnSecond = atBat.runnerOnSecond?.let { playerService.getPlayerByNumberAndTeam(atBat.battingTeam ?: "", it) }
        val runnerOnThird = atBat.runnerOnThird?.let { playerService.getPlayerByNumberAndTeam(atBat.battingTeam ?: "", it) }
        val baseConditionBefore = gameService.getBaseCondition(runnerOnFirst, runnerOnSecond, runnerOnThird)

        val outcome =
            baseRunningService.resolveOutcome(
                Scenario.WALK,
                game.outs,
                game.inningHalf,
                baseConditionBefore,
                runnerOnFirst,
                runnerOnSecond,
                runnerOnThird,
                game.homeScore,
                game.awayScore,
                null,
            )

        gameLifecycleService.updateGameValues(game, outcome)
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
            difference,
            HitLocation(null, null, null),
        )
    }

    private fun updateAtBatValues(
        atBat: AtBat,
        submissionType: SubmissionType,
        result: Scenario,
        outcome: AtBatOutcome,
        decryptedPitcherNumber: Int,
        batterNumberSubmission: Int,
        difference: Int,
        hitLocation: HitLocation,
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
        atBat.hitDirection = hitLocation.direction
        atBat.battedBallType = hitLocation.battedBallType
        atBat.fielderPosition = hitLocation.fielderPosition
        atBat.assistSequence = hitLocation.assistSequence
        atBat.atBatFinished = true

        return saveAtBat(atBat)
    }

    private fun saveAtBat(atBat: AtBat) = atBatRepository.save(atBat)
}
