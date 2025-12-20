package com.nfcaab.backend.model

import com.nfcaab.backend.enums.team.Conference
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
@Table(name = "season_stats", schema = "porygon")
class SeasonStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int = 0,
    @Basic
    @Column(name = "team", nullable = false)
    var team: String,
    @Basic
    @Column(name = "season_number", nullable = false)
    var seasonNumber: Int,
    @Basic
    @Column(name = "wins")
    var wins: Int = 0,
    @Basic
    @Column(name = "losses")
    var losses: Int = 0,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "subdivision")
    var subdivision: Subdivision? = null,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "conference")
    var conference: Conference? = null,
    // Batting Stats (Season Totals)
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
    @Basic
    @Column(name = "batting_average")
    var battingAverage: Double? = null,
    @Basic
    @Column(name = "on_base_percentage")
    var onBasePercentage: Double? = null,
    @Basic
    @Column(name = "slugging_percentage")
    var sluggingPercentage: Double? = null,
    // Pitching Stats (Season Totals)
    @Basic
    @Column(name = "innings_pitched")
    var inningsPitched: String? = null,
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
    @Basic
    @Column(name = "era")
    var era: Double? = null,
    @Basic
    @Column(name = "whip")
    var whip: Double? = null,
    // Game Control (Season Totals)
    @Basic
    @Column(name = "largest_lead")
    var largestLead: Int = 0,
    @Basic
    @Column(name = "largest_deficit")
    var largestDeficit: Int = 0,
    // Performance Metrics (Season Averages)
    @Basic
    @Column(name = "average_response_speed")
    var averageResponseSpeed: Double? = null,
    // Opponent Stats (what the team allowed opponents to do)
    // Opponent Batting Stats (Season Totals)
    @Basic
    @Column(name = "opponent_at_bats")
    var opponentAtBats: Int = 0,
    @Basic
    @Column(name = "opponent_runs")
    var opponentRuns: Int = 0,
    @Basic
    @Column(name = "opponent_hits")
    var opponentHits: Int = 0,
    @Basic
    @Column(name = "opponent_runs_batted_in")
    var opponentRunsBattedIn: Int = 0,
    @Basic
    @Column(name = "opponent_home_runs")
    var opponentHomeRuns: Int = 0,
    @Basic
    @Column(name = "opponent_triples")
    var opponentTriples: Int = 0,
    @Basic
    @Column(name = "opponent_doubles")
    var opponentDoubles: Int = 0,
    @Basic
    @Column(name = "opponent_singles")
    var opponentSingles: Int = 0,
    @Basic
    @Column(name = "opponent_walks")
    var opponentWalks: Int = 0,
    @Basic
    @Column(name = "opponent_strikeouts")
    var opponentStrikeouts: Int = 0,
    @Basic
    @Column(name = "opponent_steals")
    var opponentSteals: Int = 0,
    @Basic
    @Column(name = "opponent_double_plays")
    var opponentDoublePlays: Int = 0,
    @Basic
    @Column(name = "opponent_batting_average")
    var opponentBattingAverage: Double? = null,
    @Basic
    @Column(name = "opponent_on_base_percentage")
    var opponentOnBasePercentage: Double? = null,
    @Basic
    @Column(name = "opponent_slugging_percentage")
    var opponentSluggingPercentage: Double? = null,
    // Opponent Pitching Stats (Season Totals)
    @Basic
    @Column(name = "opponent_innings_pitched")
    var opponentInningsPitched: String? = null,
    @Basic
    @Column(name = "opponent_hits_allowed")
    var opponentHitsAllowed: Int = 0,
    @Basic
    @Column(name = "opponent_runs_allowed")
    var opponentRunsAllowed: Int = 0,
    @Basic
    @Column(name = "opponent_earned_runs_allowed")
    var opponentEarnedRunsAllowed: Int = 0,
    @Basic
    @Column(name = "opponent_walks_allowed")
    var opponentWalksAllowed: Int = 0,
    @Basic
    @Column(name = "opponent_strikeouts_thrown")
    var opponentStrikeoutsThrown: Int = 0,
    @Basic
    @Column(name = "opponent_home_runs_allowed")
    var opponentHomeRunsAllowed: Int = 0,
    @Basic
    @Column(name = "opponent_triples_allowed")
    var opponentTriplesAllowed: Int = 0,
    @Basic
    @Column(name = "opponent_doubles_allowed")
    var opponentDoublesAllowed: Int = 0,
    @Basic
    @Column(name = "opponent_singles_allowed")
    var opponentSinglesAllowed: Int = 0,
    @Basic
    @Column(name = "opponent_steals_allowed")
    var opponentStealsAllowed: Int = 0,
    @Basic
    @Column(name = "opponent_double_plays_forced")
    var opponentDoublePlaysForced: Int = 0,
    @Basic
    @Column(name = "opponent_era")
    var opponentEra: Double? = null,
    @Basic
    @Column(name = "opponent_whip")
    var opponentWhip: Double? = null,
    // Additional Season Info
    @Basic
    @Column(name = "last_modified_ts")
    var lastModifiedTs: String? = null,
) {
    constructor() : this(
        team = "",
        seasonNumber = 0,
    )
}

