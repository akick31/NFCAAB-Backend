package com.nfcaab.backend.service.atbat

import com.nfcaab.backend.dto.AtBatOutcome
import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.Scenario
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.model.RunEvent
import com.nfcaab.backend.repositories.AtBatRepository
import com.nfcaab.backend.repositories.RunEventRepository
import com.nfcaab.backend.service.game.GameService
import com.nfcaab.backend.service.game.GameLifecycleService
import com.nfcaab.backend.service.player.PlayerService
import com.nfcaab.backend.service.scorebug.ScorebugService
import com.nfcaab.backend.service.stats.GameStatsService
import com.nfcaab.backend.service.stats.PlayerGameStatsService
import com.nfcaab.backend.util.EncryptionUtils
import com.nfcaab.backend.util.ResultNotFoundException
import org.springframework.stereotype.Service

@Service
class AtBatResolutionService(
    private val atBatRepository: AtBatRepository,
    private val runEventRepository: RunEventRepository,
    private val encryptionUtils: EncryptionUtils,
    private val gameService: GameService,
    private val gameLifecycleService: GameLifecycleService,
    private val gameStatsService: GameStatsService,
    private val playerGameStatsService: PlayerGameStatsService,
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
                batter,
                submissionType,
            )

        val enrichedOutcome =
            enrichOutcomeWithResponsiblePitchers(
                outcome,
                runnerOnFirst,
                game.runnerOnFirstPitcher,
                runnerOnSecond,
                game.runnerOnSecondPitcher,
                runnerOnThird,
                game.runnerOnThirdPitcher,
                pitcher.uniformNumber,
            )

        gameLifecycleService.updateGameValues(game, enrichedOutcome)

        recordRunEvents(
            game,
            atBat,
            enrichedOutcome.scoringRunners,
            atBat.battingTeam ?: "",
            atBat.pitchingTeam ?: "",
            runnerOnFirst,
            game.runnerOnFirstPitcher,
            runnerOnSecond,
            game.runnerOnSecondPitcher,
            runnerOnThird,
            game.runnerOnThirdPitcher,
            pitcher.uniformNumber,
        )

        val allAtBats = atBatRepository.getAllAtBatsByGameId(game.id)
        gameStatsService.updateGameStats(game, allAtBats)
        playerGameStatsService.updatePlayerGameStats(game, allAtBats)
        scorebugService.generateScorebug(game)

        val resolvedHitLocation =
            hitLocation.copy(
                fieldingNotation =
                    hitLocationService.buildFieldingNotation(
                        enrichedOutcome.actualResult,
                        hitLocation.fielderPosition,
                        hitLocation.battedBallType,
                    ),
            )

        return updateAtBatValues(
            atBat,
            submissionType,
            result,
            enrichedOutcome,
            decryptedPitcherNumber.toInt(),
            batterNumberSubmission,
            difference,
            resolvedHitLocation,
            runnerOnFirst,
            game.runnerOnFirstPitcher,
            runnerOnSecond,
            game.runnerOnSecondPitcher,
            runnerOnThird,
            game.runnerOnThirdPitcher,
        )
    }

    fun resolveSteal(
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
        val runner = baseRunningService.leadRunner(runnerOnFirst, runnerOnSecond, runnerOnThird)
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

        val outcome =
            baseRunningService.resolveStealOutcome(
                result,
                game.outs,
                game.inningHalf,
                runnerOnFirst,
                runnerOnSecond,
                runnerOnThird,
                game.homeScore,
                game.awayScore,
            )

        val enrichedOutcome =
            enrichOutcomeWithResponsiblePitchers(
                outcome,
                runnerOnFirst,
                game.runnerOnFirstPitcher,
                runnerOnSecond,
                game.runnerOnSecondPitcher,
                runnerOnThird,
                game.runnerOnThirdPitcher,
                pitcher.uniformNumber,
            )

        gameLifecycleService.updateGameValues(game, enrichedOutcome, false)

        recordRunEvents(
            game,
            atBat,
            enrichedOutcome.scoringRunners,
            atBat.battingTeam ?: "",
            atBat.pitchingTeam ?: "",
            runnerOnFirst,
            game.runnerOnFirstPitcher,
            runnerOnSecond,
            game.runnerOnSecondPitcher,
            runnerOnThird,
            game.runnerOnThirdPitcher,
            pitcher.uniformNumber,
        )

        val allAtBats = atBatRepository.getAllAtBatsByGameId(game.id)
        gameStatsService.updateGameStats(game, allAtBats)
        playerGameStatsService.updatePlayerGameStats(game, allAtBats)
        scorebugService.generateScorebug(game)

        return updateAtBatValues(
            atBat,
            submissionType,
            result,
            enrichedOutcome,
            decryptedPitcherNumber.toInt(),
            batterNumberSubmission,
            difference,
            HitLocation(null, null, null),
            runnerOnFirst,
            game.runnerOnFirstPitcher,
            runnerOnSecond,
            game.runnerOnSecondPitcher,
            runnerOnThird,
            game.runnerOnThirdPitcher,
        )
    }

    fun resolveIntentionalWalk(
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
                batter,
            )

        val currentPitcherUniformNumber = decryptedPitcherNumber.toIntOrNull()
        val enrichedOutcome =
            enrichOutcomeWithResponsiblePitchers(
                outcome,
                runnerOnFirst,
                game.runnerOnFirstPitcher,
                runnerOnSecond,
                game.runnerOnSecondPitcher,
                runnerOnThird,
                game.runnerOnThirdPitcher,
                currentPitcherUniformNumber,
            )

        gameLifecycleService.updateGameValues(game, enrichedOutcome)

        recordRunEvents(
            game,
            atBat,
            enrichedOutcome.scoringRunners,
            atBat.battingTeam ?: "",
            atBat.pitchingTeam ?: "",
            runnerOnFirst,
            game.runnerOnFirstPitcher,
            runnerOnSecond,
            game.runnerOnSecondPitcher,
            runnerOnThird,
            game.runnerOnThirdPitcher,
            currentPitcherUniformNumber,
        )

        val allAtBats = atBatRepository.getAllAtBatsByGameId(game.id)
        gameStatsService.updateGameStats(game, allAtBats)
        playerGameStatsService.updatePlayerGameStats(game, allAtBats)
        scorebugService.generateScorebug(game)

        return updateAtBatValues(
            atBat,
            submissionType,
            Scenario.WALK,
            enrichedOutcome,
            decryptedPitcherNumber.toInt(),
            batterNumberSubmission,
            difference,
            HitLocation(null, null, null),
            runnerOnFirst,
            game.runnerOnFirstPitcher,
            runnerOnSecond,
            game.runnerOnSecondPitcher,
            runnerOnThird,
            game.runnerOnThirdPitcher,
        )
    }

    private fun enrichOutcomeWithResponsiblePitchers(
        outcome: AtBatOutcome,
        runnerOnFirst: Player?,
        runnerOnFirstPitcher: Int?,
        runnerOnSecond: Player?,
        runnerOnSecondPitcher: Int?,
        runnerOnThird: Player?,
        runnerOnThirdPitcher: Int?,
        currentPitcherUniformNumber: Int?,
    ): AtBatOutcome =
        outcome.copy(
            runnerOnFirstPitcherAfter =
                resolveResponsiblePitcher(
                    outcome.runnerOnFirstAfter,
                    runnerOnFirst,
                    runnerOnFirstPitcher,
                    runnerOnSecond,
                    runnerOnSecondPitcher,
                    runnerOnThird,
                    runnerOnThirdPitcher,
                    currentPitcherUniformNumber,
                ),
            runnerOnSecondPitcherAfter =
                resolveResponsiblePitcher(
                    outcome.runnerOnSecondAfter,
                    runnerOnFirst,
                    runnerOnFirstPitcher,
                    runnerOnSecond,
                    runnerOnSecondPitcher,
                    runnerOnThird,
                    runnerOnThirdPitcher,
                    currentPitcherUniformNumber,
                ),
            runnerOnThirdPitcherAfter =
                resolveResponsiblePitcher(
                    outcome.runnerOnThirdAfter,
                    runnerOnFirst,
                    runnerOnFirstPitcher,
                    runnerOnSecond,
                    runnerOnSecondPitcher,
                    runnerOnThird,
                    runnerOnThirdPitcher,
                    currentPitcherUniformNumber,
                ),
        )

    private fun resolveResponsiblePitcher(
        afterRunner: Player?,
        runnerOnFirst: Player?,
        runnerOnFirstPitcher: Int?,
        runnerOnSecond: Player?,
        runnerOnSecondPitcher: Int?,
        runnerOnThird: Player?,
        runnerOnThirdPitcher: Int?,
        currentPitcherUniformNumber: Int?,
    ): Int? {
        if (afterRunner == null) return null
        return when {
            runnerOnFirst != null && afterRunner.uniformNumber == runnerOnFirst.uniformNumber -> runnerOnFirstPitcher
            runnerOnSecond != null && afterRunner.uniformNumber == runnerOnSecond.uniformNumber -> runnerOnSecondPitcher
            runnerOnThird != null && afterRunner.uniformNumber == runnerOnThird.uniformNumber -> runnerOnThirdPitcher
            else -> currentPitcherUniformNumber
        }
    }

    private fun recordRunEvents(
        game: Game,
        atBat: AtBat,
        scoringRunners: List<Player>,
        battingTeam: String,
        pitchingTeam: String,
        runnerOnFirst: Player?,
        runnerOnFirstPitcher: Int?,
        runnerOnSecond: Player?,
        runnerOnSecondPitcher: Int?,
        runnerOnThird: Player?,
        runnerOnThirdPitcher: Int?,
        currentPitcherUniformNumber: Int?,
    ) {
        scoringRunners.forEach { scorer ->
            val chargedPitcher =
                resolveResponsiblePitcher(
                    scorer,
                    runnerOnFirst,
                    runnerOnFirstPitcher,
                    runnerOnSecond,
                    runnerOnSecondPitcher,
                    runnerOnThird,
                    runnerOnThirdPitcher,
                    currentPitcherUniformNumber,
                )
            runEventRepository.save(
                RunEvent().apply {
                    gameId = game.id
                    atBatId = atBat.id
                    inning = game.inning
                    scoringTeam = battingTeam
                    scoringPlayerUniformNumber = scorer.uniformNumber
                    chargedPitcherUniformNumber = chargedPitcher
                    chargedPitcherTeam = pitchingTeam
                },
            )
        }
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
        runnerOnFirst: Player?,
        runnerOnFirstPitcher: Int?,
        runnerOnSecond: Player?,
        runnerOnSecondPitcher: Int?,
        runnerOnThird: Player?,
        runnerOnThirdPitcher: Int?,
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
        atBat.runnerOnFirstPitcher = runnerOnFirstPitcher.takeIf { runnerOnFirst != null }
        atBat.runnerOnSecondPitcher = runnerOnSecondPitcher.takeIf { runnerOnSecond != null }
        atBat.runnerOnThirdPitcher = runnerOnThirdPitcher.takeIf { runnerOnThird != null }
        atBat.runnerOnFirstPitcherAfter = outcome.runnerOnFirstPitcherAfter
        atBat.runnerOnSecondPitcherAfter = outcome.runnerOnSecondPitcherAfter
        atBat.runnerOnThirdPitcherAfter = outcome.runnerOnThirdPitcherAfter
        atBat.hitDirection = hitLocation.direction
        atBat.battedBallType = hitLocation.battedBallType
        atBat.fielderPosition = hitLocation.fielderPosition
        atBat.fieldingNotation = hitLocation.fieldingNotation
        atBat.atBatFinished = true

        return saveAtBat(atBat)
    }

    private fun saveAtBat(atBat: AtBat) = atBatRepository.save(atBat)
}
