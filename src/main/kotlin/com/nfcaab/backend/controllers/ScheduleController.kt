package com.nfcaab.backend.controllers

import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import com.nfcaab.backend.service.schedule.ScheduleService

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/schedule")
class ScheduleController(
    private var scheduleService: ScheduleService,
) {
    /**
     * Get an opponent for a given week and team
     * @param team Team name
     * @return Opponent team name
     */
    @GetMapping("/opponent")
    fun getTeamOpponent(
        @RequestParam("team") team: String,
    ) = scheduleService.getTeamOpponent(team)

    /**
     * Get the schedule for a given season for a team
     * @param season Season number
     * @param team Team name
     * @return Schedule
     */
    @GetMapping("/season")
    fun getScheduleBySeasonAndTeam(
        @RequestParam("season") season: Int,
        @RequestParam("team") team: String,
    ) = scheduleService.getScheduleBySeasonAndTeam(season, team)
}
