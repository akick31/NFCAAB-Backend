package com.nfcaab.backend.service.scorebug

import com.nfcaab.backend.dto.response.ScorebugResponse
import com.nfcaab.backend.model.Game
import com.nfcaab.backend.model.Game.InningHalf
import com.nfcaab.backend.model.Team
import com.nfcaab.backend.enums.team.Conference
import com.nfcaab.backend.service.game.GameSpecificationService.GameCategory
import com.nfcaab.backend.service.game.GameSpecificationService.GameFilter
import com.nfcaab.backend.service.game.GameSpecificationService.GameSort
import com.nfcaab.backend.service.stats.PlayerStatLineService
import com.nfcaab.backend.util.Logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.MultipleGradientPaint
import java.awt.Rectangle
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import com.nfcaab.backend.service.team.TeamService
import com.nfcaab.backend.service.game.GameSpecificationService
import com.nfcaab.backend.service.game.GameService

@Service
class ScorebugService(
    private val teamService: TeamService,
    private val gameService: GameService,
    private val playerStatLineService: PlayerStatLineService,
) {
    @Value("\${images.path}")
    private val imagePath: String? = null

    private val logoCache = ConcurrentHashMap<String, BufferedImage?>()
    private val logoBoundsCache = ConcurrentHashMap<String, Rectangle?>()

    companion object {
        private const val WIDTH = 620
        private const val CORNER_RADIUS = 16.0
        private const val TOP_PADDING_TOP = 20
        private const val TOP_PADDING_SIDE = 18
        private const val TOP_PADDING_BOTTOM = 12
        private const val LOGO_SIZE = 98
        private const val DIAMOND_SIZE = 52
        private const val BASES_VERTICAL_OFFSET = 10
        private val SCORE_ROW_HEIGHT = maxOf(LOGO_SIZE, DIAMOND_SIZE)
        private const val META_GAP_TOP = 9
        private const val META_ROW_HEIGHT = 40
        private const val PEOPLE_PADDING_TOP = 12
        private const val PEOPLE_PADDING_SIDE = 28
        private const val PEOPLE_PADDING_BOTTOM = 14
        private const val PEOPLE_ROW_GAP = 10
        private const val PERSON_ROW_HEIGHT = 28
        private const val BAR_WIDTH = 6
        private const val BAR_HEIGHT = 26
        private const val BASE_SQUARE = 30
        private const val SQRT_2 = 1.4142135
        private const val SCORE_TRACKING = -10
        private const val CORNER_GLOW_RADIUS = 280f
        private const val CORNER_GLOW_VERTICAL_SQUEEZE = 0.679
        private const val DIVIDER_INSET = 18
        private const val DIVIDER_HEIGHT = 1

        private val PEOPLE_PANEL = Color(18, 20, 23)
        private val BASE_UNLIT_FILL = Color(35, 38, 43)
        private val BASE_UNLIT_BORDER = Color(56, 61, 68)
        private val FOUL_YELLOW = Color(244, 196, 48)
        private val OUT_DOT_UNLIT = Color(58, 63, 70)
        private val OUT_DOT_LIT = Color(230, 230, 223)
        private val SCORE_WHITE = Color(242, 242, 240)
        private val PITCHER_NAME_COLOR = Color(207, 211, 216)
        private val BATTER_NAME_COLOR = Color(255, 255, 255)
        private val PITCHER_STAT_COLOR = Color(154, 160, 168)
        private val BATTER_STAT_COLOR = Color(216, 218, 221)
        private val DIVIDER_COLOR = Color(110, 114, 120, 90)

        private val SANS_BOLD = Font("SansSerif", Font.BOLD, 1)
        private val SANS_PLAIN = Font("SansSerif", Font.PLAIN, 1)
    }

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
        } catch (e: IOException) {
            Logger.warn("Could not read scorebug file for game $gameId", e)
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
        } catch (e: IOException) {
            Logger.error("Error fetching scorebug image for game ${game.id}", e)
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
        } catch (e: IOException) {
            Logger.error("Error fetching scorebug images for conference ${conference.name}", e)
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    /**
     * Generates the locked NFCAAB scorebug design: a dark card with a team-color
     * gradient header (logos, score, baserunner diamond, inning/outs) and a
     * darker panel below showing the current pitcher and batter with a stat line.
     */
    fun generateScorebug(game: Game): BufferedImage {
        val homeTeam = teamService.getTeamByName(game.homeTeam)
        val awayTeam = teamService.getTeamByName(game.awayTeam)

        val height = TOP_PADDING_TOP + SCORE_ROW_HEIGHT + META_GAP_TOP + META_ROW_HEIGHT + TOP_PADDING_BOTTOM +
            PEOPLE_PADDING_TOP + PERSON_ROW_HEIGHT + PEOPLE_ROW_GAP + PERSON_ROW_HEIGHT + PEOPLE_PADDING_BOTTOM

        val image = BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val cardShape = RoundRectangle2D.Double(0.0, 0.0, WIDTH.toDouble(), height.toDouble(), CORNER_RADIUS, CORNER_RADIUS)
        g.clip = cardShape

        val topPanelHeight = TOP_PADDING_TOP + SCORE_ROW_HEIGHT + META_GAP_TOP + META_ROW_HEIGHT + TOP_PADDING_BOTTOM
        drawTopPanel(g, game, homeTeam, awayTeam, topPanelHeight)
        drawPeoplePanel(g, game, homeTeam, awayTeam, topPanelHeight, height - topPanelHeight)
        drawSectionDivider(g, topPanelHeight)

        g.dispose()

        val outputfile = File("$imagePath/scorebugs/${game.id}_scorebug.png")
        val directory = File("$imagePath/scorebugs")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        ImageIO.write(image, "png", outputfile)
        return image
    }

    private fun drawTopPanel(
        g: Graphics2D,
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
        panelHeight: Int,
    ) {
        val awayDark = darken(Color.decode(awayTeam.primaryColor))
        val homeDark = darken(Color.decode(homeTeam.primaryColor))

        g.color = PEOPLE_PANEL
        g.fillRect(0, 0, WIDTH, panelHeight)

        val glowTransform = AffineTransform.getScaleInstance(1.0, CORNER_GLOW_VERTICAL_SQUEEZE)

        g.paint =
            RadialGradientPaint(
                Point2D.Float(0f, 0f),
                CORNER_GLOW_RADIUS,
                Point2D.Float(0f, 0f),
                floatArrayOf(0f, 1f),
                arrayOf(
                    Color(awayDark.red, awayDark.green, awayDark.blue, 255),
                    Color(awayDark.red, awayDark.green, awayDark.blue, 0),
                ),
                MultipleGradientPaint.CycleMethod.NO_CYCLE,
                MultipleGradientPaint.ColorSpaceType.SRGB,
                glowTransform,
            )
        g.fillRect(0, 0, WIDTH, panelHeight)

        g.paint =
            RadialGradientPaint(
                Point2D.Float(WIDTH.toFloat(), 0f),
                CORNER_GLOW_RADIUS,
                Point2D.Float(WIDTH.toFloat(), 0f),
                floatArrayOf(0f, 1f),
                arrayOf(
                    Color(homeDark.red, homeDark.green, homeDark.blue, 255),
                    Color(homeDark.red, homeDark.green, homeDark.blue, 0),
                ),
                MultipleGradientPaint.CycleMethod.NO_CYCLE,
                MultipleGradientPaint.ColorSpaceType.SRGB,
                glowTransform,
            )
        g.fillRect(0, 0, WIDTH, panelHeight)

        val awayLogo = loadLogo(awayTeam)
        val homeLogo = loadLogo(homeTeam)
        val awayLogoBounds = logoBounds(awayTeam, awayLogo)
        val homeLogoBounds = logoBounds(homeTeam, homeLogo)

        val scoreRowCenterY = TOP_PADDING_TOP + SCORE_ROW_HEIGHT / 2
        val diamondCenterX = WIDTH / 2
        val diamondCenterY = scoreRowCenterY + BASES_VERTICAL_OFFSET
        val baseHalfDiagonal = (BASE_SQUARE * SQRT_2 / 2).toInt()
        val diamondLeftEdge = diamondCenterX - DIAMOND_SIZE / 2 - baseHalfDiagonal
        val diamondRightEdge = diamondCenterX + DIAMOND_SIZE / 2 + baseHalfDiagonal
        val homeLogoLeftEdge = WIDTH - TOP_PADDING_SIDE - LOGO_SIZE
        val awayVisibleRightEdge = TOP_PADDING_SIDE + visibleRightInset(awayLogo, awayLogoBounds)
        val homeVisibleLeftEdge = homeLogoLeftEdge + visibleLeftInset(homeLogo, homeLogoBounds)

        drawTeamBlock(
            g,
            awayTeam,
            awayLogo,
            game.awayScore,
            logoX = TOP_PADDING_SIDE,
            scoreCenterX = (awayVisibleRightEdge + diamondLeftEdge) / 2,
            rowCenterY = scoreRowCenterY,
        )
        drawTeamBlock(
            g,
            homeTeam,
            homeLogo,
            game.homeScore,
            logoX = homeLogoLeftEdge,
            scoreCenterX = (diamondRightEdge + homeVisibleLeftEdge) / 2,
            rowCenterY = scoreRowCenterY,
        )

        drawDiamond(g, diamondCenterX, diamondCenterY, game)

        val metaRowCenterY = TOP_PADDING_TOP + SCORE_ROW_HEIGHT + META_GAP_TOP + META_ROW_HEIGHT / 2
        drawInningAndOuts(g, game, metaRowCenterY)
    }

    private fun drawTeamBlock(
        g: Graphics2D,
        team: Team,
        logo: BufferedImage?,
        score: Int,
        logoX: Int,
        scoreCenterX: Int,
        rowCenterY: Int,
    ) {
        val logoY = rowCenterY - LOGO_SIZE / 2
        if (logo != null) {
            g.drawImage(logo, logoX, logoY, LOGO_SIZE, LOGO_SIZE, null)
        } else {
            drawLogoFallback(g, team, logoX, logoY)
        }

        g.font = SANS_BOLD.deriveFont(86f)
        g.color = SCORE_WHITE
        val scoreText = score.toString()
        val metrics = g.fontMetrics
        val baselineY = rowCenterY + (metrics.ascent - metrics.descent) / 2
        val scoreWidth = trackedStringWidth(metrics, scoreText, SCORE_TRACKING)
        val scoreX = scoreCenterX - scoreWidth / 2
        drawTrackedString(g, scoreText, scoreX, baselineY, SCORE_TRACKING)
    }

    private fun trackedStringWidth(
        metrics: FontMetrics,
        text: String,
        tracking: Int,
    ): Int {
        if (text.isEmpty()) return 0
        val glyphWidths = text.sumOf { metrics.charWidth(it) }
        return glyphWidths + tracking * (text.length - 1)
    }

    private fun drawTrackedString(
        g: Graphics2D,
        text: String,
        x: Int,
        baselineY: Int,
        tracking: Int,
    ) {
        val metrics = g.fontMetrics
        var cursorX = x
        for (ch in text) {
            g.drawString(ch.toString(), cursorX, baselineY)
            cursorX += metrics.charWidth(ch) + tracking
        }
    }

    private fun drawLogoFallback(
        g: Graphics2D,
        team: Team,
        x: Int,
        y: Int,
    ) {
        g.color = Color.decode(team.primaryColor)
        g.fillOval(x, y, LOGO_SIZE, LOGO_SIZE)
        g.font = SANS_BOLD.deriveFont(28f)
        g.color = Color.WHITE
        val metrics = g.fontMetrics
        val initials = team.abbreviation.take(2).uppercase()
        val textX = x + (LOGO_SIZE - metrics.stringWidth(initials)) / 2
        val textY = y + LOGO_SIZE / 2 + (metrics.ascent - metrics.descent) / 2
        g.drawString(initials, textX, textY)
    }

    private fun drawDiamond(
        g: Graphics2D,
        centerX: Int,
        centerY: Int,
        game: Game,
    ) {
        drawBase(g, centerX, centerY - DIAMOND_SIZE / 2, game.runnerOnSecond != null)
        drawBase(g, centerX + DIAMOND_SIZE / 2, centerY, game.runnerOnFirst != null)
        drawBase(g, centerX - DIAMOND_SIZE / 2, centerY, game.runnerOnThird != null)
    }

    private fun drawBase(
        g: Graphics2D,
        centerX: Int,
        centerY: Int,
        lit: Boolean,
    ) {
        val half = BASE_SQUARE / 2.0
        val square = RoundRectangle2D.Double(-half, -half, BASE_SQUARE.toDouble(), BASE_SQUARE.toDouble(), 4.0, 4.0)
        val transform = AffineTransform.getTranslateInstance(centerX.toDouble(), centerY.toDouble())
        transform.rotate(Math.toRadians(45.0))
        val rotated = transform.createTransformedShape(square)

        g.color = if (lit) FOUL_YELLOW else BASE_UNLIT_FILL
        g.fill(rotated)
        if (!lit) {
            g.color = BASE_UNLIT_BORDER
            g.draw(rotated)
        }
    }

    private fun drawInningAndOuts(
        g: Graphics2D,
        game: Game,
        centerY: Int,
    ) {
        val inningText = "${game.inning}${ordinalSuffix(game.inning)}"
        g.font = SANS_BOLD.deriveFont(27f)
        val metrics = g.fontMetrics
        val arrowWidth = 16
        val arrowGap = 5
        val outsGap = 20
        val outsDotSize = 15
        val outsDotGap = 7

        val inningTextWidth = metrics.stringWidth(inningText)
        val outsWidth = outsDotSize * 2 + outsDotGap
        val totalWidth = arrowWidth + arrowGap + inningTextWidth + outsGap + outsWidth
        var x = WIDTH / 2 - totalWidth / 2

        drawInningArrow(g, x, centerY, arrowWidth, game.inningHalf == InningHalf.TOP)
        x += arrowWidth + arrowGap

        g.color = SCORE_WHITE
        val baselineY = centerY + (metrics.ascent - metrics.descent) / 2
        g.drawString(inningText, x, baselineY)
        x += inningTextWidth + outsGap

        val litOuts = minOf(game.outs, 2)
        for (i in 0 until 2) {
            g.color = if (i < litOuts) OUT_DOT_LIT else OUT_DOT_UNLIT
            g.fillOval(x, centerY - outsDotSize / 2, outsDotSize, outsDotSize)
            x += outsDotSize + outsDotGap
        }
    }

    private fun drawInningArrow(
        g: Graphics2D,
        x: Int,
        centerY: Int,
        size: Int,
        pointingUp: Boolean,
    ) {
        val path = Path2D.Double()
        if (pointingUp) {
            path.moveTo(x.toDouble(), (centerY + size / 2).toDouble())
            path.lineTo((x + size).toDouble(), (centerY + size / 2).toDouble())
            path.lineTo((x + size / 2).toDouble(), (centerY - size / 2).toDouble())
        } else {
            path.moveTo(x.toDouble(), (centerY - size / 2).toDouble())
            path.lineTo((x + size).toDouble(), (centerY - size / 2).toDouble())
            path.lineTo((x + size / 2).toDouble(), (centerY + size / 2).toDouble())
        }
        path.closePath()
        g.color = FOUL_YELLOW
        g.fill(path)
    }

    private fun drawSectionDivider(
        g: Graphics2D,
        topPanelHeight: Int,
    ) {
        g.color = DIVIDER_COLOR
        g.fillRect(DIVIDER_INSET, topPanelHeight - DIVIDER_HEIGHT, WIDTH - 2 * DIVIDER_INSET, DIVIDER_HEIGHT)
    }

    private fun drawPeoplePanel(
        g: Graphics2D,
        game: Game,
        homeTeam: Team,
        awayTeam: Team,
        panelTop: Int,
        panelHeight: Int,
    ) {
        g.color = PEOPLE_PANEL
        g.fillRect(0, panelTop, WIDTH, panelHeight)

        val battingTeam = if (game.inningHalf == InningHalf.TOP) awayTeam else homeTeam
        val pitchingTeam = if (game.inningHalf == InningHalf.TOP) homeTeam else awayTeam

        val lineupSpot = if (game.inningHalf == InningHalf.TOP) game.awayBatterLineupSpot else game.homeBatterLineupSpot
        val pitcherLabel = game.pitcherName ?: "TBD"
        val batterLabel = game.batterName?.let { "$lineupSpot. $it" } ?: "TBD"

        val pitcherStat = playerStatLineService.pitcherGameLine(pitchingTeam.name, game.pitcherUniformNumber, game.id)
        val batterStat = playerStatLineService.batterGameLine(battingTeam.name, game.batterUniformNumber, game.id)

        var rowCenterY = panelTop + PEOPLE_PADDING_TOP + PERSON_ROW_HEIGHT / 2
        drawPersonRow(g, Color.decode(pitchingTeam.primaryColor), pitcherLabel, pitcherStat, PITCHER_NAME_COLOR, PITCHER_STAT_COLOR, rowCenterY)

        rowCenterY += PERSON_ROW_HEIGHT / 2 + PEOPLE_ROW_GAP + PERSON_ROW_HEIGHT / 2
        drawPersonRow(g, Color.decode(battingTeam.primaryColor), batterLabel, batterStat, BATTER_NAME_COLOR, BATTER_STAT_COLOR, rowCenterY)
    }

    private fun drawPersonRow(
        g: Graphics2D,
        barColor: Color,
        name: String,
        stat: String?,
        nameColor: Color,
        statColor: Color,
        centerY: Int,
    ) {
        val barX = PEOPLE_PADDING_SIDE
        g.color = barColor
        g.fillRoundRect(barX, centerY - BAR_HEIGHT / 2, BAR_WIDTH, BAR_HEIGHT, 3, 3)

        g.font = SANS_BOLD.deriveFont(22f)
        g.color = nameColor
        val nameMetrics: FontMetrics = g.fontMetrics
        val nameX = barX + BAR_WIDTH + 12
        val baselineY = centerY + (nameMetrics.ascent - nameMetrics.descent) / 2
        g.drawString(name, nameX, baselineY)

        if (stat != null) {
            g.font = SANS_PLAIN.deriveFont(22f)
            g.color = statColor
            val statMetrics = g.fontMetrics
            val statX = WIDTH - PEOPLE_PADDING_SIDE - statMetrics.stringWidth(stat)
            val statBaselineY = centerY + (statMetrics.ascent - statMetrics.descent) / 2
            g.drawString(stat, statX, statBaselineY)
        }
    }

    private fun loadLogo(team: Team): BufferedImage? {
        val path = team.scorebugLogo ?: team.logo ?: return null
        return logoCache.getOrPut(path) {
            try {
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    ImageIO.read(URL(path))
                } else {
                    ImageIO.read(File(path))
                }
            } catch (e: IOException) {
                Logger.warn("Failed to load logo for team ${team.name} from $path", e)
                null
            }
        }
    }

    private fun logoBounds(
        team: Team,
        image: BufferedImage?,
    ): Rectangle? {
        if (image == null) return null
        val key = team.scorebugLogo ?: team.logo ?: return null
        return logoBoundsCache.getOrPut(key) { opaqueBoundingBox(image) }
    }

    private fun opaqueBoundingBox(image: BufferedImage): Rectangle? {
        var minX = image.width
        var maxX = -1
        var minY = image.height
        var maxY = -1
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val alpha = (image.getRGB(x, y) ushr 24) and 0xFF
                if (alpha > 16) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < minX || maxY < minY) return null
        return Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1)
    }

    private fun visibleRightInset(
        image: BufferedImage?,
        bounds: Rectangle?,
    ): Int {
        if (image == null || bounds == null) return LOGO_SIZE
        val fraction = (bounds.x + bounds.width).toDouble() / image.width
        return (fraction * LOGO_SIZE).toInt()
    }

    private fun visibleLeftInset(
        image: BufferedImage?,
        bounds: Rectangle?,
    ): Int {
        if (image == null || bounds == null) return 0
        val fraction = bounds.x.toDouble() / image.width
        return (fraction * LOGO_SIZE).toInt()
    }

    private fun darken(color: Color): Color {
        val factor = 0.32
        return Color(
            (color.red * factor).toInt().coerceIn(0, 255),
            (color.green * factor).toInt().coerceIn(0, 255),
            (color.blue * factor).toInt().coerceIn(0, 255),
        )
    }

    private fun ordinalSuffix(n: Int): String {
        val remainder100 = n % 100
        if (remainder100 in 11..13) return "th"
        return when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
}
