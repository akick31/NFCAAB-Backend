package com.nfcaab.porygon.controllers

import com.nfcaab.porygon.service.discord.DiscordService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/discord")
class DiscordController(
    private var discordService: DiscordService,
)
