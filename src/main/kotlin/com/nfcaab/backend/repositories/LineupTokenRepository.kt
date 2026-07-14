package com.nfcaab.backend.repositories

import com.nfcaab.backend.model.LineupToken
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface LineupTokenRepository : CrudRepository<LineupToken, Int> {
    @Query(value = "SELECT * FROM lineup_tokens WHERE token = ?", nativeQuery = true)
    fun getByToken(token: String): LineupToken?
}
