package com.nfcaab.backend.model

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "lineup_tokens", schema = "porygon")
open class LineupToken {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Int? = null

    @Column(name = "token", nullable = false, unique = true)
    open var token: String? = null

    @Column(name = "game_id", nullable = false)
    open var gameId: Int? = null

    @Column(name = "team", nullable = false)
    open var team: String? = null

    @Column(name = "created_at", nullable = false)
    open var createdAt: LocalDateTime? = null

    @Column(name = "expires_at", nullable = false)
    open var expiresAt: LocalDateTime? = null

    @Column(name = "used", nullable = false)
    open var used: Boolean = false
}
