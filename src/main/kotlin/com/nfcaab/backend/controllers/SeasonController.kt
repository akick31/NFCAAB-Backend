package com.nfcaab.backend.controllers

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import com.nfcaab.backend.service.schedule.SeasonService

@RestController
@RequestMapping("/season")
class SeasonController(
    private var seasonService: SeasonService,
) {
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/start")
    fun startSeason() = seasonService.startSeason()

    @GetMapping("/current")
    fun getCurrentSeason() = seasonService.getCurrentSeason()

    @GetMapping("/week")
    fun getCurrentWeek() = seasonService.getCurrentWeek()
}
