package com.nfcaab.porygon.enums.records

/**
 * Enum representing all possible stat types that can be tracked as records
 * Based on all fields from the GameStats model for baseball
 */
enum class Stats {
    // Basic Game Info
    SCORE,

    // Batting Stats
    AT_BATS,
    RUNS,
    HITS,
    RUNS_BATTED_IN,
    HOME_RUNS,
    TRIPLES,
    DOUBLES,
    SINGLES,
    WALKS,
    STRIKEOUTS,
    STEALS,
    DOUBLE_PLAYS,

    // Batting Averages
    BATTING_AVERAGE,
    ON_BASE_PERCENTAGE,
    SLUGGING_PERCENTAGE,

    // Pitching Stats
    INNINGS_PITCHED,
    HITS_ALLOWED,
    RUNS_ALLOWED,
    EARNED_RUNS_ALLOWED,
    WALKS_ALLOWED,
    STRIKEOUTS_THROWN,
    HOME_RUNS_ALLOWED,
    TRIPLES_ALLOWED,
    DOUBLES_ALLOWED,
    SINGLES_ALLOWED,
    STEALS_ALLOWED,
    DOUBLE_PLAYS_FORCED,

    // Pitching Averages
    ERA,
    WHIP,

    // Game Flow
    LARGEST_LEAD,
    LARGEST_DEFICIT,
    AVERAGE_RESPONSE_SPEED,

    // Inning Scores
    I1_SCORE,
    I2_SCORE,
    I3_SCORE,
    I4_SCORE,
    I5_SCORE,
    I6_SCORE,
    I7_SCORE,
    I8_SCORE,
    I9_SCORE,
    EXTRA_INNINGS_SCORE,
}

