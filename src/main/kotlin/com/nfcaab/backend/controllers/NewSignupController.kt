package com.nfcaab.backend.controllers

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import com.nfcaab.backend.service.user.NewSignupService

@RestController
@RequestMapping("/new_signups")
@PreAuthorize("hasRole('ADMIN')")
class NewSignupController(
    private var newSignupService: NewSignupService,
) {
    @GetMapping("")
    fun getNewSignups() = newSignupService.getNewSignups()

    @PostMapping("/{id}/approve")
    fun approveNewSignup(
        @PathVariable id: Long,
    ) = newSignupService.approveNewSignup(newSignupService.getNewSignupById(id))

    @DeleteMapping("/{id}")
    fun rejectNewSignup(
        @PathVariable id: Long,
    ) = newSignupService.deleteNewSignup(newSignupService.getNewSignupById(id))
}
