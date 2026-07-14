package com.nfcaab.backend.controllers

import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import com.nfcaab.backend.service.user.NewSignupService

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/new_signups")
class NewSignupController(
    private var newSignupService: NewSignupService,
) {
    @GetMapping("")
    fun getNewSignups() = newSignupService.getNewSignups()
}
