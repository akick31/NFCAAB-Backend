package com.nfcaab.backend.model

import com.nfcaab.backend.model.Game.Scenario
import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Player.Archetype
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "ranges", schema = "porygon")
open class Ranges {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false)
    open var id: Int? = null

    @Column(name = "batter_archetype")
    open var batterArchetype: Archetype? = null

    @Column(name = "pitcher_archetype")
    open var pitcherArchetype: Archetype? = null

    @Column(name = "submission_type")
    open var submissionType: SubmissionType? = null

    @Column(name = "result")
    open var result: Scenario? = null

    @Column(name = "low_range")
    open var lowRange: Int? = null

    @Column(name = "high_range")
    open var highRange: Int? = null
}
