package com.nfcaab.backend.model

import com.nfcaab.backend.model.Game.ActualResult
import com.nfcaab.backend.model.Game.Scenario
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "at_bats", schema = "porygon")
class AtBat {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "at_bat_id", nullable = false)
    var id: Int = 0

    @Column(name = "game_id")
    var gameId: Int = 0

    @Column(name = "pitch_number")
    var pitchNumber: Int = 1

    @Column(name = "home_team")
    lateinit var homeTeam: String

    @Column(name = "away_team")
    lateinit var awayTeam: String

    @Column(name = "home_score")
    var homeScore: Int = 0

    @Column(name = "away_score")
    var awayScore: Int = 0

    @Column(name = "inning_half")
    var inningHalf: Game.InningHalf = Game.InningHalf.TOP

    @Column(name = "inning")
    var inning: Int = 1

    @Column(name = "outs")
    var outs: Int = 0

    @Column(name = "pitching_team")
    var pitchingTeam: String? = null

    @Column(name = "batting_team")
    var battingTeam: String? = null

    @Column(name = "pitcher_submitter")
    var pitcherSubmitter: String? = null

    @Column(name = "batter_submitter")
    var batterSubmitter: String? = null

    @Column(name = "batter_number_submission")
    var batterNumberSubmission: String? = null

    @Column(name = "pitcher_number_submission")
    var pitcherNumberSubmission: String? = null

    @Column(name = "difference")
    var difference: Int? = null

    @Column(name = "submission_type")
    var submissionType: SubmissionType? = null

    @Column(name = "result")
    var result: Scenario? = null

    @Column(name = "actual_result")
    var actualResult: ActualResult? = null

    @Column(name = "runner_on_first")
    var runnerOnFirst: Int? = null

    @Column(name = "runner_on_second")
    var runnerOnSecond: Int? = null

    @Column(name = "runner_on_third")
    var runnerOnThird: Int? = null

    @Column(name = "runs_scored")
    var runsScored: Int = 0

    @Column(name = "runner_on_first_after")
    var runnerOnFirstAfter: Int? = null

    @Column(name = "runner_on_second_after")
    var runnerOnSecondAfter: Int? = null

    @Column(name = "runner_on_third_after")
    var runnerOnThirdAfter: Int? = null

    @Column(name = "lineup_spot")
    var lineupSpot: Int = 1

    @Column(name = "batter_name")
    var batterName: String? = null

    @Column(name = "batter_uniform_number")
    var batterUniformNumber: Int? = null

    @Column(name = "pitcher_name")
    var pitcherName: String? = null

    @Column(name = "pitcher_uniform_number")
    var pitcherUniformNumber: Int? = null

    @Column(name = "win_probability")
    var winProbability: Float = 0.0F

    @Column(name = "win_probability_added")
    var winProbabilityAdded: Float = 0.0F

    @Column(name = "batter_response_speed")
    var batterResponseSpeed: Long? = null

    @Column(name = "pitcher_response_speed")
    var pitcherResponseSpeed: Long? = null

    @Column(name = "at_bat_finished")
    var atBatFinished: Boolean = false

    @Enumerated(EnumType.STRING)
    @Column(name = "hit_direction")
    var hitDirection: Game.HitDirection? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "batted_ball_type")
    var battedBallType: Game.BattedBallType? = null

    @Column(name = "fielder_position")
    var fielderPosition: Int? = null

    @Column(name = "assist_sequence")
    var assistSequence: String? = null

    constructor()

    // Constructor with parameters
    constructor(
        gameId: Int,
        pitchNumber: Int,
        homeTeam: String,
        awayTeam: String,
        homeScore: Int,
        awayScore: Int,
        inningHalf: Game.InningHalf,
        inning: Int,
        outs: Int,
        pitchingTeam: String?,
        battingTeam: String?,
        pitcherSubmitter: String?,
        batterSubmitter: String?,
        batterNumberSubmission: String?,
        pitcherNumberSubmission: String?,
        difference: Int?,
        submissionType: SubmissionType?,
        result: Scenario?,
        actualResult: ActualResult?,
        runnerOnFirst: Int?,
        runnerOnSecond: Int?,
        runnerOnThird: Int?,
        runsScored: Int,
        runnerOnFirstAfter: Int?,
        runnerOnSecondAfter: Int?,
        runnerOnThirdAfter: Int?,
        lineupSpot: Int,
        batterName: String?,
        batterUniformNumber: Int?,
        pitcherName: String?,
        pitcherUniformNumber: Int?,
        winProbability: Float,
        winProbabilityAdded: Float,
        batterResponseSpeed: Long?,
        pitcherResponseSpeed: Long?,
        atBatFinished: Boolean,
    ) {
        this.gameId = gameId
        this.pitchNumber = pitchNumber
        this.homeTeam = homeTeam
        this.awayTeam = awayTeam
        this.homeScore = homeScore
        this.awayScore = awayScore
        this.inningHalf = inningHalf
        this.inning = inning
        this.outs = outs
        this.pitchingTeam = pitchingTeam
        this.battingTeam = battingTeam
        this.pitcherSubmitter = pitcherSubmitter
        this.batterSubmitter = batterSubmitter
        this.batterNumberSubmission = batterNumberSubmission
        this.pitcherNumberSubmission = pitcherNumberSubmission
        this.difference = difference
        this.submissionType = submissionType
        this.result = result
        this.actualResult = actualResult
        this.runnerOnFirst = runnerOnFirst
        this.runnerOnSecond = runnerOnSecond
        this.runnerOnThird = runnerOnThird
        this.runsScored = runsScored
        this.runnerOnFirstAfter = runnerOnFirstAfter
        this.runnerOnSecondAfter = runnerOnSecondAfter
        this.runnerOnThirdAfter = runnerOnThirdAfter
        this.lineupSpot = lineupSpot
        this.batterName = batterName
        this.batterUniformNumber = batterUniformNumber
        this.pitcherName = pitcherName
        this.pitcherUniformNumber = pitcherUniformNumber
        this.winProbability = winProbability
        this.winProbabilityAdded = winProbabilityAdded
        this.batterResponseSpeed = batterResponseSpeed
        this.pitcherResponseSpeed = pitcherResponseSpeed
        this.atBatFinished = atBatFinished
    }

    enum class SubmissionType(val description: String) {
        SWING("Swing"),
        BUNT("Bunt"),
        STEAL("Steal"),
        INTENTIONAL_WALK("Intentional Walk"),
        PINCH_HIT("Pinch Hit"),
        PINCH_RUN("Pinch Run"),
        PITCHER_CHANGE("Pitcher Change"),
    }
}

