package com.nfcaab.backend.service.game

import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.BaseCondition
import com.nfcaab.backend.model.Player
import com.nfcaab.backend.model.Team
import com.nfcaab.backend.repositories.GameRepository
import com.nfcaab.backend.service.game.GameSpecificationService.GameCategory
import com.nfcaab.backend.service.game.GameSpecificationService.GameFilter
import com.nfcaab.backend.service.game.GameSpecificationService.GameSort
import com.nfcaab.backend.util.GameNotFoundException
import com.nfcaab.backend.util.Logger
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val gameSpecificationService: GameSpecificationService,
) {
    fun getGameByRequestMessageId(requestMessageId: String) =
        gameRepository.getGameByRequestMessageId(requestMessageId)
            ?: throw GameNotFoundException("Game not found for Request Message ID: $requestMessageId")

    fun getGameByPlatformId(platformId: ULong) =
        gameRepository.getGameByPlatformId(platformId.toString())
            ?: throw GameNotFoundException("Game not found for Platform ID: $platformId")

    fun getGameById(id: Int) = gameRepository.getGameById(id) ?: throw GameNotFoundException("No game found with ID: $id")

    fun getFilteredGames(
        filters: List<GameFilter>,
        category: GameCategory?,
        conference: String?,
        season: Int?,
        week: Int?,
        sort: GameSort,
        pageable: Pageable,
    ): Page<Game> {
        val filterSpec = gameSpecificationService.createSpecification(filters, category, conference, season, week)
        val sortOrders = gameSpecificationService.createSort(sort)
        val sortedPageable =
            PageRequest.of(
                pageable.pageNumber,
                pageable.pageSize,
                Sort.by(sortOrders),
            )

        return gameRepository.findAll(filterSpec, sortedPageable)
            ?: throw GameNotFoundException(
                "No games found for the following filters: " +
                    "filters = $filters, " +
                    "category = $category, " +
                    "conference = $conference, " +
                    "season = $season, " +
                    "week = $week",
            )
    }

    fun findExpiredTimers() =
        gameRepository.findExpiredTimers().ifEmpty {
            Logger.info("No games found with expired timers")
            emptyList()
        }

    fun findGamesToWarn() =
        gameRepository.findGamesToWarn().ifEmpty {
            Logger.info("No games found to warn")
            emptyList()
        }

    fun updateGameAsWarned(id: Int) = gameRepository.updateGameAsWarned(id)

    fun markCloseGamePinged(id: Int) = gameRepository.markCloseGamePinged(id)

    fun markUpsetAlertPinged(id: Int) = gameRepository.markUpsetAlertPinged(id)

    fun getAllGames() =
        gameRepository.getAllGames().ifEmpty {
            throw GameNotFoundException("No games found when getting all games")
        }

    fun getAllOngoingGames() =
        gameRepository.getAllOngoingGames().ifEmpty {
            emptyList()
        }

    fun getGamesWithTeams(
        teams: List<Team>,
        season: Int,
        week: Int,
    ): List<Game> {
        val games = mutableListOf<Game>()
        for (team in teams) {
            val game = gameRepository.getGamesByTeamSeasonAndWeek(team.name ?: "", season, week)
            if (game != null) {
                games.add(game)
            } else {
                throw GameNotFoundException("No games found for ${team.name} in season $season week $week")
            }
        }
        return games
    }

    fun getDifference(
        batterNumber: Int,
        pitcherNumber: Int,
    ): Int {
        var difference = abs(pitcherNumber - batterNumber)
        if (difference > 500) {
            difference = 1000 - difference
        }
        return difference
    }

    fun getBaseCondition(
        runnerOnFirst: Player?,
        runnerOnSecond: Player?,
        runnerOnThird: Player?,
    ): BaseCondition {
        return when {
            runnerOnFirst != null && runnerOnSecond != null && runnerOnThird != null -> BaseCondition.BASED_LOADED
            runnerOnFirst != null && runnerOnSecond != null -> BaseCondition.FIRST_SECOND
            runnerOnFirst != null && runnerOnThird != null -> BaseCondition.FIRST_THIRD
            runnerOnSecond != null && runnerOnThird != null -> BaseCondition.SECOND_THIRD
            runnerOnFirst != null -> BaseCondition.FIRST
            runnerOnSecond != null -> BaseCondition.SECOND
            runnerOnThird != null -> BaseCondition.THIRD
            else -> BaseCondition.EMPTY
        }
    }

    fun saveGame(game: Game) =
        gameRepository.save(game)
            ?: throw GameNotFoundException("Unable to save game: ${game.id} - ${game.homeTeam} vs ${game.awayTeam}")

    fun updateGame(game: Game): Game = saveGame(game)

    fun updateRequestMessageId(
        id: Int,
        requestMessageId: String,
    ): Game {
        val game = getGameById(id)
        game.requestMessageId = requestMessageId
        return saveGame(game)
    }

    fun updateLastMessageTimestamp(id: Int): Game {
        val game = getGameById(id)
        game.lastMessageTimestamp = java.time.Instant.now()
        return saveGame(game)
    }

    fun calculateDelayOfGameTimer(): String? {
        val now = ZonedDateTime.now(ZoneId.of("America/New_York"))
        val futureTime = now.plusHours(14)
        val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss")
        return futureTime.format(formatter)
    }
}
