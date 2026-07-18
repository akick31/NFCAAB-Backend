package com.nfcaab.backend.controllers

import com.nfcaab.backend.model.Player
import com.nfcaab.backend.service.player.PlayerService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/player")
class PlayerController(
    private val playerService: PlayerService,
) {
    /**
     * Get the active roster for a team
     * @param team
     */
    @GetMapping("/team")
    fun getPlayersByTeam(
        @RequestParam("team") team: String,
    ): List<Player> = playerService.getPlayersByTeam(team)
}
