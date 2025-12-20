package com.nfcaab.backend.controllers

import com.nfcaab.backend.dto.GameScenario
import com.nfcaab.backend.service.nfcaab.GameWriteupService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
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
