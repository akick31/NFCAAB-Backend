package com.nfcaab.porygon.repositories

import com.nfcaab.porygon.enums.records.RecordType
import com.nfcaab.porygon.enums.records.Stats
import com.nfcaab.porygon.model.Record
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RecordRepository : JpaRepository<Record, Long>, JpaSpecificationExecutor<Record> {
    fun findAllByOrderBySeasonNumberDescWeekDesc(): List<Record>

    fun findByRecordNameAndRecordType(
        recordName: Stats,
        recordType: RecordType,
    ): Record?

    fun findBySeasonNumberOrderByWeekDesc(seasonNumber: Int): List<Record>

    fun findByRecordTeamOrderBySeasonNumberDescWeekDesc(recordTeam: String): List<Record>

    fun findByRecordNameOrderByRecordValueDesc(recordName: Stats): List<Record>

    fun findByGameId(gameId: Int): List<Record>

    @Query("SELECT r.recordValue FROM Record r WHERE r.recordName = :recordName AND r.recordType = :recordType")
    fun findCurrentRecordValue(
        @Param("recordName") recordName: Stats,
        @Param("recordType") recordType: RecordType,
    ): Double?

    fun findTopByRecordNameAndRecordTypeOrderByRecordValueDesc(
        recordName: Stats,
        recordType: RecordType,
    ): Record?

    fun deleteBySeasonNumber(seasonNumber: Int)

    fun deleteByGameId(gameId: Int)

    @Query("SELECT r FROM Record r WHERE r.gameId = :gameId AND r.recordValue > COALESCE(r.previousRecordValue, 0)")
    fun findRecordsBrokenInGame(
        @Param("gameId") gameId: Int,
    ): List<Record>
}

