package com.nfcaab.backend.model

import com.nfcaab.backend.model.Game.GameStatus
import com.nfcaab.backend.model.Game.GameType
import com.nfcaab.backend.enums.team.Subdivision
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "game_stats")
class GameStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int = 0,
    @Basic
    @Column(name = "game_id")
    var gameId: Int = 0,
    @Basic
    @Column(name = "team")
    var team: String? = null,
    @Basic
    @Column(name = "season")
    var season: Int? = null,
    @Basic
    @Column(name = "week")
    var week: Int? = null,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "subdivision")
    var subdivision: Subdivision? = null,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "game_status")
    var gameStatus: GameStatus? = null,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "game_type")
    var gameType: GameType? = null,
    @Basic
    @Column(name = "score")
    var score: Int = 0,
    // Batting stats
    @Basic
    @Column(name = "at_bats")
    var atBats: Int = 0,
    @Basic
    @Column(name = "runs")
    var runs: Int = 0,
    @Basic
    @Column(name = "hits")
    var hits: Int = 0,
    @Basic
    @Column(name = "runs_batted_in")
    var runsBattedIn: Int = 0,
    @Basic
    @Column(name = "home_runs")
    var homeRuns: Int = 0,
    @Basic
    @Column(name = "triples")
    var triples: Int = 0,
    @Basic
    @Column(name = "doubles")
    var doubles: Int = 0,
    @Basic
    @Column(name = "singles")
    var singles: Int = 0,
    @Basic
    @Column(name = "walks")
    var walks: Int = 0,
    @Basic
    @Column(name = "strikeouts")
    var strikeouts: Int = 0,
    @Basic
    @Column(name = "steals")
    var steals: Int = 0,
    @Basic
    @Column(name = "double_plays")
    var doublePlays: Int = 0,
    // Pitching stats
    @Basic
    @Column(name = "innings_pitched")
    var inningsPitched: Double = 0.0,
    @Basic
    @Column(name = "hits_allowed")
    var hitsAllowed: Int = 0,
    @Basic
    @Column(name = "runs_allowed")
    var runsAllowed: Int = 0,
    @Basic
    @Column(name = "earned_runs_allowed")
    var earnedRunsAllowed: Int = 0,
    @Basic
    @Column(name = "walks_allowed")
    var walksAllowed: Int = 0,
    @Basic
    @Column(name = "strikeouts_thrown")
    var strikeoutsThrown: Int = 0,
    @Basic
    @Column(name = "home_runs_allowed")
    var homeRunsAllowed: Int = 0,
    @Basic
    @Column(name = "triples_allowed")
    var triplesAllowed: Int = 0,
    @Basic
    @Column(name = "doubles_allowed")
    var doublesAllowed: Int = 0,
    @Basic
    @Column(name = "singles_allowed")
    var singlesAllowed: Int = 0,
    @Basic
    @Column(name = "steals_allowed")
    var stealsAllowed: Int = 0,
    @Basic
    @Column(name = "double_plays_forced")
    var doublePlaysForced: Int = 0,
    // Calculated stats
    @Basic
    @Column(name = "batting_average")
    var battingAverage: Double = 0.0,
    @Basic
    @Column(name = "on_base_percentage")
    var onBasePercentage: Double = 0.0,
    @Basic
    @Column(name = "slugging_percentage")
    var sluggingPercentage: Double = 0.0,
    @Basic
    @Column(name = "era")
    var era: Double = 0.0,
    @Basic
    @Column(name = "whip")
    var whip: Double = 0.0,
    // Inning scores
    @Basic
    @Column(name = "i1_score")
    var i1Score: Int = 0,
    @Basic
    @Column(name = "i2_score")
    var i2Score: Int = 0,
    @Basic
    @Column(name = "i3_score")
    var i3Score: Int = 0,
    @Basic
    @Column(name = "i4_score")
    var i4Score: Int = 0,
    @Basic
    @Column(name = "i5_score")
    var i5Score: Int = 0,
    @Basic
    @Column(name = "i6_score")
    var i6Score: Int = 0,
    @Basic
    @Column(name = "i7_score")
    var i7Score: Int = 0,
    @Basic
    @Column(name = "i8_score")
    var i8Score: Int = 0,
    @Basic
    @Column(name = "i9_score")
    var i9Score: Int = 0,
    @Basic
    @Column(name = "i10_score")
    var i10Score: Int = 0,
    @Basic
    @Column(name = "i11_score")
    var i11Score: Int = 0,
    @Basic
    @Column(name = "i12_score")
    var i12Score: Int = 0,
    @Basic
    @Column(name = "i13_score")
    var i13Score: Int = 0,
    @Basic
    @Column(name = "i14_score")
    var i14Score: Int = 0,
    @Basic
    @Column(name = "i15_score")
    var i15Score: Int = 0,
    @Basic
    @Column(name = "extra_innings_score")
    var extraInningsScore: Int = 0,
    @Basic
    @Column(name = "largest_lead")
    var largestLead: Int = 0,
    @Basic
    @Column(name = "largest_deficit")
    var largestDeficit: Int = 0,
    @Basic
    @Column(name = "average_response_speed")
    var averageResponseSpeed: Double = 0.0,
    @Basic
    @Column(name = "last_modified_ts")
    var lastModifiedTs: String? = null,
) {
    // Default constructor
    constructor() : this(
        id = 0,
        gameId = 0,
        team = null,
        season = null,
        week = null,
        subdivision = null,
        gameStatus = null,
        gameType = null,
        score = 0,
        atBats = 0,
        runs = 0,
        hits = 0,
        runsBattedIn = 0,
        homeRuns = 0,
        triples = 0,
        doubles = 0,
        singles = 0,
        walks = 0,
        strikeouts = 0,
        steals = 0,
        doublePlays = 0,
        inningsPitched = 0.0,
        hitsAllowed = 0,
        runsAllowed = 0,
        earnedRunsAllowed = 0,
        walksAllowed = 0,
        strikeoutsThrown = 0,
        homeRunsAllowed = 0,
        triplesAllowed = 0,
        doublesAllowed = 0,
        singlesAllowed = 0,
        stealsAllowed = 0,
        doublePlaysForced = 0,
        battingAverage = 0.0,
        onBasePercentage = 0.0,
        sluggingPercentage = 0.0,
        era = 0.0,
        whip = 0.0,
        i1Score = 0,
        i2Score = 0,
        i3Score = 0,
        i4Score = 0,
        i5Score = 0,
        i6Score = 0,
        i7Score = 0,
        i8Score = 0,
        i9Score = 0,
        i10Score = 0,
        i11Score = 0,
        i12Score = 0,
        i13Score = 0,
        i14Score = 0,
        i15Score = 0,
        extraInningsScore = 0,
        largestLead = 0,
        largestDeficit = 0,
        averageResponseSpeed = 0.0,
        lastModifiedTs = null,
    )
}

