package com.nfcaab.backend.service.atbat

import com.nfcaab.backend.model.AtBat
import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Game
import org.springframework.stereotype.Service

@Service
class BuntService(
    private val atBatResolutionService: AtBatResolutionService,
) {
    fun resolveBunt(
        atBat: AtBat,
        game: Game,
        submissionType: SubmissionType,
        batterNumberSubmission: Int,
        decryptedPitcherNumber: String,
    ): AtBat = atBatResolutionService.resolveSwing(atBat, game, submissionType, batterNumberSubmission, decryptedPitcherNumber)
}
