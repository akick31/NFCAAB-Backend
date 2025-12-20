package com.nfcaab.backend.repositories

import com.nfcaab.backend.model.GameWriteup
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GameWriteupRepository : CrudRepository<GameWriteup?, Int?> {
    @Query(
        value = """
            SELECT * FROM game_writeup g 
            WHERE g.result = :result 
              AND g.batter_on_first = :batterOnFirst 
              AND g.batter_on_second = :batterOnSecond 
              AND g.batter_on_third = :batterOnThird 
              AND g.runs_scored = :runsScored
        """,
        nativeQuery = true,
    )
    fun findByScenario(
        result: String,
        batterOnFirst: Boolean,
        batterOnSecond: Boolean,
        batterOnThird: Boolean,
        runsScored: Int,
    ): List<GameWriteup>
}
