package com.nfcaab.backend.controllers

import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.service.discord.DiscordService
import com.nfcaab.backend.service.nfcaab.AtBatService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/discord")
class DiscordController(
    private var discordService: DiscordService,
    private val atBatService: AtBatService,
) {
    /**
     * Submit a pitcher number for an at-bat
     * @param gameId Game ID
     * @param pitcherSubmitter Discord username of the pitcher submitter
     * @param pitcherNumberSubmission The pitcher's number (1-100)
     * @param submissionType Optional submission type (e.g., INTENTIONAL_WALK)
     * @return AtBat
     */
    @PostMapping("/at-bat/pitcher")
    fun submitPitcherNumber(
        @RequestParam("gameId") gameId: Int,
        @RequestParam("pitcherSubmitter") pitcherSubmitter: String,
        @RequestParam("pitcherNumberSubmission") pitcherNumberSubmission: Int,
        @RequestParam(value = "submissionType", required = false) submissionType: SubmissionType?,
    ): AtBat {
        return atBatService.pitchingNumberSubmitted(
            gameId,
            pitcherSubmitter,
            pitcherNumberSubmission,
            submissionType,
        )
    }

    /**
     * Submit a batter number (swing/bunt/steal) for an at-bat
     * @param gameId Game ID
     * @param batterSubmitter Discord username of the batter submitter
     * @param batterNumberSubmission The batter's number (1-100)
     * @param submissionType Submission type (SWING, BUNT, or STEAL)
     * @return AtBat
     */
    @PutMapping("/at-bat/batter")
    fun submitBatterNumber(
        @RequestParam("gameId") gameId: Int,
        @RequestParam("batterSubmitter") batterSubmitter: String,
        @RequestParam("batterNumberSubmission") batterNumberSubmission: Int,
        @RequestParam("submissionType") submissionType: SubmissionType,
    ): AtBat {
        return atBatService.batterNumberSubmitted(
            gameId,
            batterSubmitter,
            batterNumberSubmission,
            submissionType,
        )
    }
}
