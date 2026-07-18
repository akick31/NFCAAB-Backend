package com.nfcaab.backend.service.game

import com.nfcaab.backend.dto.requests.StartRequest
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.service.schedule.ScheduleService
import com.nfcaab.backend.util.Logger
import com.nfcaab.backend.util.NoGameFoundException
import kotlinx.coroutines.delay
import org.springframework.stereotype.Service

@Service
class GameWeekService(
    private val scheduleService: ScheduleService,
    private val gameLifecycleService: GameLifecycleService,
) {
    companion object {
        private const val GAMES_PER_BATCH = 25
        private const val BATCH_DELAY_MS = 300000L
    }

    suspend fun startWeek(
        season: Int,
        week: Int,
    ): List<Game> {
        val gamesToStart =
            scheduleService.getGamesToStartBySeasonAndWeek(season, week) ?: run {
                Logger.error("No games found for season $season week $week")
                throw NoGameFoundException()
            }
        val startedGames = mutableListOf<Game>()
        var count = 0
        for (game in gamesToStart) {
            try {
                if (count >= GAMES_PER_BATCH) {
                    delay(BATCH_DELAY_MS)
                    count = 0
                    Logger.info("Block of $GAMES_PER_BATCH games started, delaying for 5 minutes")
                }
                val startedGame =
                    gameLifecycleService.startGame(
                        StartRequest(
                            game.subdivision,
                            game.homeTeam,
                            game.awayTeam,
                            game.gameType,
                            1,
                        ),
                        week,
                    )
                startedGames.add(startedGame)
                scheduleService.markGameAsStarted(game)
                count += 1
            } catch (e: Exception) {
                Logger.error("Error starting ${game.homeTeam} vs ${game.awayTeam}", e)
                continue
            }
        }
        return startedGames
    }
}
