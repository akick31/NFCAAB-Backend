package com.nfcaab.backend.service.scorebug

import com.nfcaab.backend.dto.response.ScorebugResponse
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.InningHalf
import com.nfcaab.backend.model.Team
import com.nfcaab.backend.enums.team.Conference
import com.nfcaab.backend.service.game.GameSpecificationService.GameCategory
import com.nfcaab.backend.service.game.GameSpecificationService.GameFilter
import com.nfcaab.backend.service.game.GameSpecificationService.GameSort
import com.nfcaab.backend.util.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import com.nfcaab.backend.service.team.TeamService
import com.nfcaab.backend.service.game.GameSpecificationService
import com.nfcaab.backend.service.game.GameService

@Service
class ScorebugService(
    private val teamService: com.nfcaab.backend.service.team.TeamService,
    private val gameService: GameService,
) {
    @Value("\${images.path}")
    private val imagePath: String? = null

    /**
     * Get the scorebug for a game filtered
     */
    fun getFilteredScorebugs(
        filters: List<GameFilter>?,
        category: GameCategory?,
        sort: GameSort,
        conference: String?,
        season: Int?,
        week: Int?,
        pageable: Pageable,
    ): ResponseEntity<PageImpl<ScorebugResponse>> {
        val filteredGames =
            gameService.getFilteredGames(
                filters = filters ?: emptyList(),
                category = category,
                conference = conference,
                season = season,
                week = week,
                sort = sort,
                pageable = pageable,
            )

        val scorebugResponses =
            filteredGames.content.map { game ->
                var scorebug = getScorebugBytes(game.id)
                if (scorebug == null) {
                    generateScorebug(game)
                    scorebug = getScorebugBytes(game.id)
                }
                ScorebugResponse(
                    gameId = game.id,
                    scorebug = scorebug,
                    homeTeam = game.homeTeam,
                    awayTeam = game.awayTeam,
                    status = game.gameStatus,
                )
            }

        val pageResponse =
            PageImpl(
                scorebugResponses,
                filteredGames.pageable,
                filteredGames.totalElements,
            )

        return ResponseEntity.ok(pageResponse)
    }

    /**
     * Generate all scorebugs
     */
    fun generateAllScorebugs() {
        val games = gameService.getAllGames()
        for (game in games) {
            generateScorebug(game)
        }
    }

    /**
     * Get the scorebug byte array for a game
     */
    private fun getScorebugBytes(gameId: Int): ByteArray? {
        return try {
            File("$imagePath/scorebugs/${gameId}_scorebug.png").readBytes()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get the scorebug image for a game
     */
    fun getScorebugByGameId(gameId: Int): ResponseEntity<ByteArray> {
        val game = gameService.getGameById(gameId)
        generateScorebug(game)

        try {
            val scorebug = File("$imagePath/scorebugs/${game.id}_scorebug.png").readBytes()

            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.IMAGE_PNG
                    contentLength = scorebug.size.toLong()
                }

            return ResponseEntity(scorebug, headers, HttpStatus.OK)
        } catch (e: Exception) {
            Logger.error("Error fetching scorebug image: ${e.message}")
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    /**
     * Get the latest scorebug image for a game without generating
     */
    fun getLatestScorebugByGameId(gameId: Int): ResponseEntity<ByteArray> {
        val bytes = getScorebugBytes(gameId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .contentLength(bytes.size.toLong())
            .body(bytes)
    }

    /**
     * Get the scorebug images for a conference
     */
    fun getScorebugsForConference(
        season: Int,
        week: Int,
        conference: Conference,
    ): ResponseEntity<List<Map<String, Any>>> {
        try {
            val teams = teamService.getTeamsInConference(conference.name) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
            val games = gameService.getGamesWithTeams(teams, season, week)
            if (games.isEmpty()) {
                return ResponseEntity(HttpStatus.NOT_FOUND)
            }

            val scorebugs = mutableListOf<Map<String, Any>>()

            for (game in games) {
                generateScorebug(game)
                val fileBytes = File("$imagePath/scorebugs/${game.id}_scorebug.png").readBytes()
                val base64Image = Base64.getEncoder().encodeToString(fileBytes)

                scorebugs.add(
                    mapOf(
                        "gameId" to game.id.toString(),
                        "image" to base64Image,
                    ),
                )
            }

            return ResponseEntity(scorebugs, HttpStatus.OK)
        } catch (e: Exception) {
            Logger.error("Error fetching scorebug images: ${e.message}")
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    /**
     * Generates a baseball scorebug image for the game
     * Based on the provided image description:
     * - Top: Pitcher name, pitch count, batter name, batting stats
     * - Middle: Team scores (left), bases (right - three diamonds)
     * - Bottom: Inning (▼ 9 for bottom 9th), outs, count (2-2)
     */
    fun generateScorebug(game: Game): BufferedImage {
        val homeTeam = teamService.getTeamByName(game.homeTeam)
        val awayTeam = teamService.getTeamByName(game.awayTeam)

        // Baseball scorebug dimensions - wider for better layout
        val width = 500
        val height = 180
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = image.createGraphics()

        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // Dark background (like the image)
        g.color = Color(20, 20, 20, 240) // Dark with slight transparency
        g.fillRect(0, 0, width, height)

        // Draw border
        g.color = Color.WHITE
        g.stroke = BasicStroke(2f)
        g.drawRect(0, 0, width - 1, height - 1)

        // Top section: Pitcher and Batter info
        drawTopSection(g, game, homeTeam, awayTeam, width, height)

        // Middle section: Scores and Bases
        drawMiddleSection(g, game, homeTeam, awayTeam, width, height)

        // Bottom section: Inning, Outs, Count
        drawBottomSection(g, game, width, height)

        g.dispose()

        // Save image to file
        val outputfile = File("$imagePath/scorebugs/${game.id}_scorebug.png")
        val directory = File("$imagePath/scorebugs")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        ImageIO.write(image, "png", outputfile)
        return image
    }

    /**
     * Draw top section: Pitcher name, pitch count, batter name, batting stats
     */
    private fun drawTopSection(
        g: Graphics2D,
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
        width: Int,
        height: Int,
    ) {
        val topY = 10
        val lineHeight = 25

        // Get pitch count from game (number of at-bats)
        val pitchCount = game.numAtBat

        // Pitcher info (top left)
        val pitcherName = game.pitcherName ?: "TBD"
        val pitcherNumber = game.pitcherUniformNumber?.toString() ?: ""
        g.color = Color.WHITE
        g.font = Font("Arial", Font.BOLD, 18)
        g.drawString(pitcherName, 10, topY + lineHeight)

        // Pitch count (top right of pitcher section)
        g.font = Font("Arial", Font.PLAIN, 16)
        val pitchCountText = "P: $pitchCount"
        val pitchCountWidth = g.fontMetrics.stringWidth(pitchCountText)
        g.drawString(pitchCountText, 200 - pitchCountWidth, topY + lineHeight)

        // Batter info (below pitcher)
        val batterName = game.batterName ?: "TBD"
        val batterNumber = game.batterUniformNumber?.toString() ?: ""
        val batterLineupSpot = if (game.inningHalf == InningHalf.TOP) game.awayBatterLineupSpot else game.homeBatterLineupSpot
        g.font = Font("Arial", Font.BOLD, 16)
        g.drawString("$batterLineupSpot. $batterName", 10, topY + lineHeight * 2)

        // Batter stats (0 FOR 1 format - simplified, would need actual stats)
        g.font = Font("Arial", Font.PLAIN, 14)
        val batterStats = "0 FOR 1" // TODO: Get actual batter stats
        g.drawString(batterStats, 10, topY + lineHeight * 3)
    }

    /**
     * Draw middle section: Team scores (left) and bases (right)
     */
    private fun drawMiddleSection(
        g: Graphics2D,
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
        width: Int,
        height: Int,
    ) {
        val middleY = 80
        val scoreBoxWidth = 80
        val scoreBoxHeight = 50

        // Away team score (top left)
        g.color = Color.decode(homeTeam.primaryColor).darker()
        g.fillRect(10, middleY, scoreBoxWidth, scoreBoxHeight / 2)
        g.color = Color.WHITE
        g.font = Font("Arial", Font.BOLD, 24)
        val awayAbbr = awayTeam.abbreviation
        val awayScore = game.awayScore.toString()
        g.drawString(awayAbbr, 15, middleY + 20)
        g.drawString(awayScore, 15, middleY + 40)

        // Home team score (below away)
        g.color = Color.decode(homeTeam.primaryColor).darker()
        g.fillRect(10, middleY + scoreBoxHeight / 2, scoreBoxWidth, scoreBoxHeight / 2)
        g.color = Color.WHITE
        val homeAbbr = homeTeam.abbreviation
        val homeScore = game.homeScore.toString()
        g.drawString(homeAbbr, 15, middleY + 60)
        g.drawString(homeScore, 15, middleY + 80)

        // Bases (right side) - three diamond shapes
        val basesX = width - 120
        val basesY = middleY + 10
        val diamondSize = 25
        val spacing = 35

        // Third base (top)
        drawBaseDiamond(g, basesX, basesY, diamondSize, game.runnerOnThird != null)
        // Second base (middle)
        drawBaseDiamond(g, basesX + spacing, basesY + spacing, diamondSize, game.runnerOnSecond != null)
        // First base (bottom)
        drawBaseDiamond(g, basesX, basesY + spacing * 2, diamondSize, game.runnerOnFirst != null)
    }

    /**
     * Draw a base diamond
     */
    private fun drawBaseDiamond(
        g: Graphics2D,
        x: Int,
        y: Int,
        size: Int,
        hasRunner: Boolean,
    ) {
        val diamondX = intArrayOf(x, x + size / 2, x + size, x + size / 2)
        val diamondY = intArrayOf(y + size / 2, y, y + size / 2, y + size)

        if (hasRunner) {
            g.color = Color(255, 200, 0) // Yellow/gold for runner
            g.fillPolygon(diamondX, diamondY, 4)
        } else {
            g.color = Color(100, 100, 100) // Gray for empty
            g.fillPolygon(diamondX, diamondY, 4)
        }

        g.color = Color.WHITE
        g.stroke = BasicStroke(1.5f)
        g.drawPolygon(diamondX, diamondY, 4)
    }

    /**
     * Draw bottom section: Inning, Outs
     */
    private fun drawBottomSection(
        g: Graphics2D,
        game: Game,
        width: Int,
        height: Int,
    ) {
        val bottomY = height - 40

        // Inning (bottom left) - ▼ 9 for bottom of 9th
        val inningSymbol = if (game.inningHalf == InningHalf.BOTTOM) "▼" else "▲"
        val inningText = "$inningSymbol ${game.inning}"
        g.color = Color.WHITE
        g.font = Font("Arial", Font.BOLD, 18)
        g.drawString(inningText, 10, bottomY)

        // Outs (middle)
        val outsText = "${game.outs} Out${if (game.outs != 1) "s" else ""}"
        g.font = Font("Arial", Font.PLAIN, 16)
        val outsWidth = g.fontMetrics.stringWidth(outsText)
        g.drawString(outsText, (width - outsWidth) / 2, bottomY)
    }
}
