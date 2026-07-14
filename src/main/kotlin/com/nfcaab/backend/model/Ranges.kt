package com.nfcaab.backend.model

import com.nfcaab.backend.model.Game.Scenario
import com.nfcaab.backend.model.AtBat.SubmissionType
import com.nfcaab.backend.model.Player.BatterArchetype
import com.nfcaab.backend.model.Player.PitcherArchetype
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
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

    @Enumerated(EnumType.STRING)
    @Column(name = "batter_archetype")
    open var batterArchetype: BatterArchetype? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "pitcher_archetype")
    open var pitcherArchetype: PitcherArchetype? = null

    @Column(name = "submission_type")
    open var submissionType: SubmissionType? = null

    @Column(name = "result")
    open var result: Scenario? = null

    @Column(name = "low_range")
    open var lowRange: Int? = null

    @Column(name = "high_range")
    open var highRange: Int? = null
}
