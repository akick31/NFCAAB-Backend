package com.nfcaab.backend.controllers

import com.nfcaab.backend.dto.GameScenario
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import com.nfcaab.backend.service.game.GameWriteupService

@RestController
@RequestMapping("/game_writeup")
class GameWriteupController(
    private var gameWriteupService: GameWriteupService,
) {
    @GetMapping("")
    fun getGameMessageByScenario(
        @RequestBody gameScenario: GameScenario,
    ) = gameWriteupService.getGameMessageByScenario(gameScenario)
}
