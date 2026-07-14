package com.nfcaab.backend.controllers

import com.nfcaab.backend.dto.requests.LineupSubmissionRequest
import com.nfcaab.backend.model.GameLineup
import com.nfcaab.backend.model.LineupToken
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import com.nfcaab.backend.service.lineup.LineupService
import com.nfcaab.backend.service.lineup.LineupTokenService

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/lineup")
class LineupController(
    private val lineupService: LineupService,
    private val lineupTokenService: LineupTokenService,
) {
    @PostMapping("/token")
    fun generateToken(
        @RequestParam("gameId") gameId: Int,
        @RequestParam("team") team: String,
    ): LineupToken {
        return lineupTokenService.generateToken(gameId, team)
    }

    @GetMapping("/token/{token}")
    fun resolveToken(
        @PathVariable("token") token: String,
    ): LineupToken {
        return lineupTokenService.validateToken(token)
    }

    @PostMapping("/submit")
    fun submitLineup(
        @RequestBody request: LineupSubmissionRequest,
    ): List<GameLineup> {
        return lineupService.saveLineup(request)
    }
}
