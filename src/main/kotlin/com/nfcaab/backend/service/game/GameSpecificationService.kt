package com.nfcaab.backend.service.game

import com.nfcaab.backend.model.Game
import com.nfcaab.backend.repositories.GameRepository
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root
import com.nfcaab.backend.service.team.TeamService

@Service
class GameSpecificationService(
    private val teamService: TeamService,
    private val gameRepository: GameRepository,
) {
    enum class GameFilter {
        RANKED_GAME,
        CONFERENCE_GAME,
        OUT_OF_CONFERENCE,
        CONFERENCE_TOURNAMENT,
        CONFERENCE_CHAMPIONSHIP,
        REGIONAL,
        REGIONAL_ELIMINATION,
        SUPER_REGIONAL,
        COLLEGE_WORLD_SERIES,
        COLLEGE_WORLD_SERIES_ELIMINATION,
        COLLEGE_WORLD_SERIES_CHAMPIONSHIP,
        PREGAME,
        IN_PROGRESS,
        EXTRA_INNINGS,
    }

    enum class GameCategory {
        ONGOING,
        PAST,
        SCRIMMAGE,
        PAST_SCRIMMAGE,
    }

    enum class GameSort {
        CLOSEST_TO_END,
        MOST_TIME_REMAINING,
    }

    /**
     * Create the spec for a game
     * @param filters
     * @param conference
     * @param season
     * @param week
     */
    fun createSpecification(
        filters: List<GameFilter>,
        category: GameCategory?,
        conference: String?,
        season: Int?,
        week: Int?,
    ): Specification<Game> {
        return Specification { root: Root<Game>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            // Handle category filter
            category?.let {
                when (it) {
                    GameCategory.ONGOING -> {
                        predicates.add(cb.notEqual(root.get<Game.GameStatus>("gameStatus"), Game.GameStatus.FINAL))
                        predicates.add(cb.notEqual(root.get<Game.GameType>("gameType"), Game.GameType.SCRIMMAGE))
                    }
                    GameCategory.PAST -> {
                        predicates.add(cb.equal(root.get<Game.GameStatus>("gameStatus"), Game.GameStatus.FINAL))
                        predicates.add(cb.notEqual(root.get<Game.GameType>("gameType"), Game.GameType.SCRIMMAGE))
                    }
                    GameCategory.SCRIMMAGE -> {
                        predicates.add(cb.notEqual(root.get<Game.GameStatus>("gameStatus"), Game.GameStatus.FINAL))
                        predicates.add(cb.equal(root.get<Game.GameType>("gameType"), Game.GameType.SCRIMMAGE))
                    }
                    GameCategory.PAST_SCRIMMAGE -> {
                        predicates.add(cb.equal(root.get<Game.GameStatus>("gameStatus"), Game.GameStatus.FINAL))
                        predicates.add(cb.equal(root.get<Game.GameType>("gameType"), Game.GameType.SCRIMMAGE))
                    }
                }
            }

            // Handle other filters
            filters.forEach { filter ->
                when (filter) {
                    GameFilter.RANKED_GAME -> {
                        val rankedGames = gameRepository.getRankedGames().map { game -> game.id }
                        if (rankedGames.isNotEmpty()) {
                            predicates.add(root.get<Int>("gameId").`in`(rankedGames))
                        }
                    }
                    GameFilter.CONFERENCE_GAME -> {
                        predicates.add(cb.equal(root.get<Game.GameType>("gameType"), Game.GameType.CONFERENCE_GAME))
                    }
                    GameFilter.OUT_OF_CONFERENCE -> {
                        predicates.add(cb.equal(root.get<Game.GameType>("gameType"), Game.GameType.OUT_OF_CONFERENCE))
                    }
                    GameFilter.CONFERENCE_TOURNAMENT -> {
                        predicates.add(cb.equal(root.get<Game.GameType>("gameType"), Game.GameType.CONFERENCE_TOURNAMENT))
                    }
                    GameFilter.CONFERENCE_CHAMPIONSHIP -> {
                        predicates.add(cb.equal(root.get<Game.GameType>("gameType"), Game.GameType.CONFERENCE_CHAMPIONSHIP))
                    }
                    GameFilter.REGIONAL -> {
                        predicates.add(cb.equal(root.get<Game.GameType>("gameType"), Game.GameType.REGIONAL))
                    }
                    GameFilter.REGIONAL_ELIMINATION -> {
                        predicates.add(cb.equal(root.get<Game.GameType>("gameType"), Game.GameType.REGIONAL_ELIMINATION))
                    }
                    GameFilter.SUPER_REGIONAL -> {
                        predicates.add(cb.equal(root.get<Game.GameType>("gameType"), Game.GameType.SUPER_REGIONAL))
                    }
                    GameFilter.COLLEGE_WORLD_SERIES -> {
                        predicates.add(cb.equal(root.get<Game.GameType>("gameType"), Game.GameType.COLLEGE_WORLD_SERIES))
                    }
                    GameFilter.COLLEGE_WORLD_SERIES_ELIMINATION -> {
                        predicates.add(
                            cb.equal(root.get<Game.GameType>("gameType"), Game.GameType.COLLEGE_WORLD_SERIES_ELIMINATION),
                        )
                    }
                    GameFilter.COLLEGE_WORLD_SERIES_CHAMPIONSHIP -> {
                        predicates.add(
                            cb.equal(root.get<Game.GameType>("gameType"), Game.GameType.COLLEGE_WORLD_SERIES_CHAMPIONSHIP),
                        )
                    }
                    GameFilter.PREGAME -> {
                        predicates.add(cb.equal(root.get<Game.GameStatus>("gameStatus"), Game.GameStatus.PREGAME))
                    }
                    GameFilter.IN_PROGRESS -> {
                        predicates.add(cb.equal(root.get<Game.GameStatus>("gameStatus"), Game.GameStatus.IN_PROGRESS))
                    }
                    GameFilter.EXTRA_INNINGS -> {
                        predicates.add(cb.equal(root.get<Game.GameStatus>("gameStatus"), Game.GameStatus.EXTRA_INNINGS))
                    }
                }
            }

            // Conference filter
            conference?.let {
                val conferenceTeams = teamService.getTeamsInConference(it)?.map { team -> team.name }
                predicates.add(
                    cb.or(
                        root.get<String>("homeTeam").`in`(conferenceTeams),
                        root.get<String>("awayTeam").`in`(conferenceTeams),
                    ),
                )
            }

            // Season/week filters
            season?.let { predicates.add(cb.equal(root.get<Int>("season"), it)) }
            week?.let { predicates.add(cb.equal(root.get<Int>("week"), it)) }

            cb.and(*predicates.toTypedArray())
        }
    }

    /**
     * Sort games by how far they are from being done
     * @param sort
     */
    fun createSort(sort: GameSort): List<org.springframework.data.domain.Sort.Order> {
        return when (sort) {
            GameSort.CLOSEST_TO_END ->
                listOf(
                    org.springframework.data.domain.Sort.Order.desc("quarter"),
                    org.springframework.data.domain.Sort.Order.asc("clock"),
                )
            GameSort.MOST_TIME_REMAINING ->
                listOf(
                    org.springframework.data.domain.Sort.Order.asc("quarter"),
                    org.springframework.data.domain.Sort.Order.desc("clock"),
                )
        }
    }
}
