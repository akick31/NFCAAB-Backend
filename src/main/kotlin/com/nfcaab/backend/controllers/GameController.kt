package com.nfcaab.backend.controllers

import com.nfcaab.backend.dto.requests.StartRequest
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.service.nfcaab.GameService
import com.nfcaab.backend.service.nfcaab.GameSpecificationService.GameCategory
import com.nfcaab.backend.service.nfcaab.GameSpecificationService.GameFilter
import com.nfcaab.backend.service.nfcaab.GameSpecificationService.GameSort
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

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/game")
class GameController(
    private val gameService: GameService,
) {
    /**
     * Start a new game
     * @param startRequest Start request containing subdivision, teams, game type, and series game number
     * @param week Optional week number
     * @return Game
     */
    @PostMapping("")
    suspend fun startGame(
        @RequestBody startRequest: StartRequest,
        @RequestParam(value = "week", required = false) week: Int?,
    ): ResponseEntity<Game> {
        return ResponseEntity.status(201).body(gameService.startSingleGame(startRequest, week))
    }

    /**
     * Start all games for a week
     * @param season Season number
     * @param week Week number
     * @return List of started games
     */
    @PostMapping("/week")
    suspend fun startWeek(
        @RequestParam("season") season: Int,
        @RequestParam("week") week: Int,
    ): ResponseEntity<List<Game>> {
        return ResponseEntity.status(201).body(gameService.startWeek(season, week))
    }

    /**
     * Get game by request message id
     * @param requestMessageId Discord message ID
     * @return Game
     */
    @GetMapping("/request-message")
    fun getGameByRequestMessageId(
        @RequestParam("requestMessageId") requestMessageId: String,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameService.getGameByRequestMessageId(requestMessageId) ?: throw com.nfcaab.backend.util.GameNotFoundException("Game not found"))
    }

    /**
     * Get game by game id
     * @param gameId Game ID
     * @return Game
     */
    @GetMapping("/{gameId}")
    fun getGameByGameId(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameService.getGameById(gameId))
    }

    /**
     * Get all ongoing games
     * @return List of ongoing games
     */
    @GetMapping("")
    fun getAllOngoingGames(): ResponseEntity<List<Game>> {
        return ResponseEntity.ok(gameService.getAllOngoingGames())
    }

    /**
     * Get filtered games
     */
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

    /**
     * Get game by platform id
     * @param platformId Platform ID (channel ID)
     * @return Game
     */
    @GetMapping("/platform")
    fun getGameByPlatformId(
        @RequestParam("platformId") platformId: ULong,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameService.getGameByPlatformId(platformId))
    }

    /**
     * End a game by channel ID
     * @param channelId Discord channel ID
     * @return Game
     */
    @PostMapping("/end")
    fun endGameByChannelId(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameService.endSingleGame(channelId))
    }

    /**
     * End a game by game ID
     * @param gameId Game ID
     * @return Game
     */
    @PostMapping("/{gameId}/end")
    fun endGameByGameId(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameService.endSingleGameByGameId(gameId))
    }

    /**
     * End all ongoing games
     * @return List of ended games
     */
    @PostMapping("/end-all")
    fun endAllGames(): ResponseEntity<List<Game>> {
        return ResponseEntity.ok(gameService.endAllGames())
    }

    /**
     * Restart a game
     * @param channelId Discord channel ID
     * @return Game
     */
    @PostMapping("/restart")
    suspend fun restartGame(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameService.restartGame(channelId))
    }

    /**
     * Delete a game
     * @param channelId Discord channel ID
     * @return Boolean indicating success
     */
    @DeleteMapping("")
    fun deleteGame(
        @RequestParam("channelId") channelId: ULong,
    ): ResponseEntity<Boolean> {
        return ResponseEntity.ok(gameService.deleteOngoingGame(channelId))
    }

    /**
     * Update request message ID
     * @param gameId Game ID
     * @param requestMessageId Request message ID
     * @return Game
     */
    @PutMapping("/{gameId}/request-message")
    fun updateRequestMessageId(
        @PathVariable("gameId") gameId: Int,
        @RequestParam("requestMessageId") requestMessageId: String,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameService.updateRequestMessageId(gameId, requestMessageId))
    }

    /**
     * Update last message timestamp
     * @param gameId Game ID
     * @return Game
     */
    @PutMapping("/{gameId}/last-message-timestamp")
    fun updateLastMessageTimestamp(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameService.updateLastMessageTimestamp(gameId))
    }

    /**
     * Mark close game pinged
     * @param gameId Game ID
     * @return Void
     */
    @PutMapping("/{gameId}/close-game-pinged")
    fun markCloseGamePinged(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Void> {
        gameService.markCloseGamePinged(gameId)
        return ResponseEntity.noContent().build()
    }

    /**
     * Mark upset alert pinged
     * @param gameId Game ID
     * @return Void
     */
    @PutMapping("/{gameId}/upset-alert-pinged")
    fun markUpsetAlertPinged(
        @PathVariable("gameId") gameId: Int,
    ): ResponseEntity<Void> {
        gameService.markUpsetAlertPinged(gameId)
        return ResponseEntity.noContent().build()
    }

    /**
     * Update a game
     * @param game Game object to update
     * @return Updated game
     */
    @PutMapping("")
    fun updateGame(
        @RequestBody game: Game,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameService.updateGame(game))
    }

    /**
     * Sub a coach into a game
     * @param gameId Game ID
     * @param team Team name
     * @param discordId Discord ID of the new coach
     * @return Updated game
     */
    @PutMapping("/{gameId}/sub")
    fun subCoachIntoGame(
        @PathVariable("gameId") gameId: Int,
        @RequestParam("team") team: String,
        @RequestParam("discordId") discordId: String,
    ): ResponseEntity<Game> {
        return ResponseEntity.ok(gameService.subCoachIntoGame(gameId, team, discordId))
    }
}

