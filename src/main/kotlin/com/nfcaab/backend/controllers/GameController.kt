package com.nfcaab.backend.controllers

import com.nfcaab.backend.dto.requests.PinchRunRequest
import com.nfcaab.backend.dto.requests.StartRequest
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.service.game.GameSpecificationService.GameCategory
import com.nfcaab.backend.service.game.GameSpecificationService.GameFilter
import com.nfcaab.backend.service.game.GameSpecificationService.GameSort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import com.nfcaab.backend.service.game.GameSpecificationService
import com.nfcaab.backend.service.game.GameService
import com.nfcaab.backend.service.game.GameLifecycleService
import com.nfcaab.backend.service.game.GameWeekService

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/game")
class GameController(
    private val gameService: GameService,
    private val gameLifecycleService: GameLifecycleService,
    private val gameWeekService: GameWeekService,
) {
    @PostMapping("")
    suspend fun startGame(
        @RequestBody startRequest: StartRequest,
        @RequestParam(value = "week", required = false) week: Int?,
    ): ResponseEntity<Game> {
        return ResponseEntity.status(201).body(gameLifecycleService.startSingleGame(startRequest, week))
    }

    @PostMapping("/week")
    suspend fun startWeek(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
    ): ResponseEntity<List<Game>> {
        return ResponseEntity.status(201).body(gameWeekService.startWeek(season, week))
    }

    @GetMapping("/request-message")
    fun getGameByRequestMessageId(
        @RequestParam("requestMessageId") requestMessageId: String,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameService.getGameByRequestMessageId(requestMessageId) ?: throw com.nfcaab.backend.util.GameNotFoundException("Game not found"))
    }

    @GetMapping("/{gameId}")
    fun getGameByGameId(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameService.getGameById(gameId))
    }

    @GetMapping("")
    fun getAllOngoingGames(): ResponseEntity<List<Game>> {
        return ResponseEntity.ok(gameService.getAllOngoingGames())
    }

    @GetMapping("/filtered")
    fun getFilteredGames(
        @RequestParam(required = false) filters: List<GameFilter>?,
        @RequestParam(required = false) category: GameCategory?,
        @RequestParam(defaultValue = "CLOSEST_TO_END") sort: GameSort,
        @RequestParam(required = false) conference: String?,
        @RequestParam(required = false) season: Int?,
        @RequestParam(required = false) week: Int?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<Page<Game>> {
        return ResponseEntity.ok(
            gameService.getFilteredGames(
                filters = filters ?: emptyList(),
                category = category,
                conference = conference,
                season = season,
                week = week,
                sort = sort,
                pageable = pageable,
            ),
        )
    }

    @GetMapping("/platform")
    fun getGameByPlatformId(
        @RequestParam("platformId") platformId: ULong,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameService.getGameByPlatformId(platformId))
    }

    @PostMapping("/end")
    fun endGameByChannelId(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameLifecycleService.endSingleGame(channelId))
    }

    @PostMapping("/{gameId}/end")
    fun endGameByGameId(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameLifecycleService.endSingleGameByGameId(gameId))
    }

    @PostMapping("/end-all")
    fun endAllGames(): ResponseEntity<List<Game>> {
        return ResponseEntity.ok(gameLifecycleService.endAllGames())
    }

    @PostMapping("/restart")
    suspend fun restartGame(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameLifecycleService.restartGame(channelId))
    }

    @DeleteMapping("")
    fun deleteGame(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Boolean> {
        return ResponseEntity.ok(gameLifecycleService.deleteOngoingGame(channelId))
    }

    @PutMapping("/{gameId}/request-message")
    fun updateRequestMessageId(
        @PathVariable("gameId") gameId: Int,
        @RequestParam("requestMessageId") requestMessageId: String,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameService.updateRequestMessageId(gameId, requestMessageId))
    }

    @PutMapping("/{gameId}/last-message-timestamp")
    fun updateLastMessageTimestamp(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameService.updateLastMessageTimestamp(gameId))
    }

    @PutMapping("/{gameId}/close-game-pinged")
    fun markCloseGamePinged(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Void> {
        gameService.markCloseGamePinged(gameId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{gameId}/upset-alert-pinged")
    fun markUpsetAlertPinged(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Void> {
        gameService.markUpsetAlertPinged(gameId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("")
    fun updateGame(
        @RequestBody game: Game,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameService.updateGame(game))
    }

    @PutMapping("/{gameId}/sub")
    fun subCoachIntoGame(
        @PathVariable("gameId") gameId: Int,
        @RequestParam("team") team: String,
        @RequestParam("discordId") discordId: String,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameLifecycleService.subCoachIntoGame(gameId, team, discordId))
    }

    @PutMapping("/{gameId}/pinch-run")
    fun pinchRun(
        @PathVariable("gameId") gameId: Int,
        @RequestBody request: PinchRunRequest,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameLifecycleService.pinchRun(gameId, request.team, request.base, request.incomingUniformNumber))
    }
}
