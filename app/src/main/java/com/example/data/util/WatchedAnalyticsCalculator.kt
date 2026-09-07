package com.example.data.util

import com.example.data.model.MediaItem
import com.example.data.model.StreamingProvider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class EraStat(
    val eraLabel: String,
    val count: Int,
    val percentage: Float
)

data class GenreStat(
    val genre: String,
    val count: Int,
    val percentage: Float,
    val avgRating: Double?
)

data class ServiceStat(
    val providerId: String,
    val displayName: String,
    val count: Int,
    val percentage: Float
)

data class RatingBinStat(
    val binLabel: String,
    val count: Int,
    val percentage: Float
)

data class DirectorStat(
    val name: String,
    val count: Int,
    val percentage: Float,
    val avgRating: Double?,
    val sampleTitles: List<String>
)

data class ActorStat(
    val name: String,
    val count: Int,
    val percentage: Float,
    val sampleTitles: List<String>
)

data class BlindSpotStat(
    val category: String,          // "Cinema Era", "Genre Gap"
    val title: String,             // "Golden Age Cinema (Pre-1970s)", "Westerns"
    val description: String,       // "Only 1.2% of your logged vault."
    val representationPercentage: Float,
    val matchingWatchlistCount: Int,
    val sampleWatchlistTitles: List<String>
)

data class WatchedAnalytics(
    val totalFilms: Int,
    val totalHours: Int,
    val avgRating: Double,
    val ratedCount: Int,
    val topEra: String?,
    val topGenre: String?,
    val eraBreakdown: List<EraStat>,
    val genreBreakdown: List<GenreStat>,
    val serviceBreakdown: List<ServiceStat>,
    val ratingBins: List<RatingBinStat>,
    val mostActiveMonth: String?,
    val mostActiveMonthCount: Int,
    val watchedThisYear: Int,
    val watchedLastYear: Int,
    val topDirectors: List<DirectorStat> = emptyList(),
    val topActors: List<ActorStat> = emptyList(),
    val blindSpots: List<BlindSpotStat> = emptyList(),
    val monthlyVelocity: Float = 0f
)

object WatchedAnalyticsCalculator {

    private val YEAR_PAREN_REGEX = Regex("\\((19\\d\\d|20\\d\\d)\\)")
    private val YEAR_LETTERBOXD_REGEX = Regex("Letterboxd diary: .*?\\b(19\\d\\d|20\\d\\d)\\b", RegexOption.IGNORE_CASE)
    private val YEAR_WORD_REGEX = Regex("\\b(19\\d\\d|20\\d\\d)\\b")
    private val YEAR_URL_REGEX = Regex("-([12]\\d{3})/?$")
    private val YEAR_DATE_REGEX = Regex("^\\s*([12]\\d{3})")

    private val STAR_RATING_REGEX = Regex("([★½]+)")
    private val RATING_SLASH_10_REGEX = Regex("(?i)rating[:\\s]+(\\d+(?:\\.\\d+)?)\\s*/\\s*10")
    private val RATING_SLASH_5_REGEX = Regex("(?i)rating[:\\s]+(\\d+(?:\\.\\d+)?)\\s*/\\s*5")
    private val SCORE_REGEX = Regex("(?i)(?:score|rating)[:\\s]+(\\d+(?:\\.\\d+)?)")

    // Curated Acclaimed Director Filmographies for Pattern Matching
    private val DIRECTOR_FILMOGRAPHIES: Map<String, List<String>> = mapOf(
        "Coen Brothers" to listOf("The Ballad of Buster Scruggs", "Hail Caesar", "Inside Llewyn Davis", "True Grit", "A Serious Man", "Burn After Reading", "No Country for Old Men", "The Ladykillers", "O Brother Where Art Thou", "The Big Lebowski", "Fargo", "Miller's Crossing", "Raising Arizona", "Blood Simple"),
        "Christopher Nolan" to listOf("Oppenheimer", "Tenet", "Dunkirk", "Interstellar", "The Dark Knight Rises", "Inception", "The Dark Knight", "The Prestige", "Batman Begins", "Memento", "Following", "Insomnia"),
        "Martin Scorsese" to listOf("Killers of the Flower Moon", "The Irishman", "Silence", "The Wolf of Wall Street", "Hugo", "Shutter Island", "The Departed", "Gangs of New York", "Casino", "Goodfellas", "Taxi Driver", "Raging Bull", "The King of Comedy", "Cape Fear", "After Hours", "Mean Streets"),
        "David Fincher" to listOf("The Killer", "Mank", "Gone Girl", "The Girl with the Dragon Tattoo", "The Social Network", "The Curious Case of Benjamin Button", "Zodiac", "Panic Room", "Fight Club", "The Game", "Se7en", "Alien 3"),
        "Quentin Tarantino" to listOf("Once Upon a Time in Hollywood", "The Hateful Eight", "Django Unchained", "Inglourious Basterds", "Death Proof", "Kill Bill Vol 1", "Kill Bill Vol 2", "Jackie Brown", "Pulp Fiction", "Reservoir Dogs"),
        "Ridley Scott" to listOf("Gladiator II", "Napoleon", "House of Gucci", "The Last Duel", "All the Money in the World", "Alien Covenant", "The Martian", "Prometheus", "American Gangster", "Kingdom of Heaven", "Black Hawk Down", "Gladiator", "Thelma and Louise", "Blade Runner", "Alien"),
        "James Cameron" to listOf("Avatar The Way of Water", "Avatar", "Titanic", "True Lies", "Terminator 2 Judgment Day", "Aliens", "The Abyss", "The Terminator"),
        "Steven Spielberg" to listOf("The Fabelmans", "West Side Story", "Ready Player One", "The Post", "Bridge of Spies", "Lincoln", "War Horse", "Munich", "War of the Worlds", "Catch Me If You Can", "Minority Report", "Saving Private Ryan", "Schindler's List", "Jurassic Park", "Raiders of the Lost Ark", "Jaws", "ET"),
        "Paul Thomas Anderson" to listOf("Licorice Pizza", "Phantom Thread", "Inherent Vice", "The Master", "There Will Be Blood", "Punch-Drunk Love", "Magnolia", "Boogie Nights", "Hard Eight"),
        "Richard Linklater" to listOf("Hit Man", "Boyhood", "Before Midnight", "Bernie", "Before Sunset", "School of Rock", "Waking Life", "Before Sunrise", "Dazed and Confused"),
        "Denis Villeneuve" to listOf("Dune Part Two", "Dune", "Blade Runner 2049", "Arrival", "Sicario", "Enemy", "Prisoners", "Incendies"),
        "Stanley Kubrick" to listOf("Eyes Wide Shut", "Full Metal Jacket", "The Shining", "Barry Lyndon", "A Clockwork Orange", "2001 A Space Odyssey", "Dr Strangelove", "Spartacus", "Paths of Glory"),
        "Wes Anderson" to listOf("Asteroid City", "The French Dispatch", "Isle of Dogs", "The Grand Budapest Hotel", "Moonrise Kingdom", "Fantastic Mr Fox", "The Darjeeling Limited", "The Life Aquatic with Steve Zissou", "The Royal Tenenbaums", "Rushmore", "Bottle Rocket"),
        "Greta Gerwig" to listOf("Barbie", "Little Women", "Lady Bird"),
        "Jordan Peele" to listOf("Nope", "Us", "Get Out"),
        "Rian Johnson" to listOf("Glass Onion", "Knives Out", "Star Wars The Last Jedi", "Looper", "Brick"),
        "Yorgos Lanthimos" to listOf("Kinds of Kindness", "Poor Things", "The Favourite", "The Killing of a Sacred Deer", "The Lobster", "Dogtooth"),
        "Bong Joon-ho" to listOf("Parasite", "Okja", "Snowpiercer", "Mother", "The Host", "Memories of Murder"),
        "Guillermo del Toro" to listOf("Nightmare Alley", "The Shape of Water", "Crimson Peak", "Pacific Rim", "Pans Labyrinth", "Hellboy", "Blade II", "Cronos"),
        "Edgar Wright" to listOf("Last Night in Soho", "Baby Driver", "The World's End", "Scott Pilgrim vs the World", "Hot Fuzz", "Shaun of the Dead")
    )

    // Curated Star Filmographies for Performer Tracking
    private val ACTOR_FILMOGRAPHIES: Map<String, List<String>> = mapOf(
        "Tom Cruise" to listOf("Mission Impossible", "Top Gun Maverick", "American Made", "Jack Reacher", "Edge of Tomorrow", "Oblivion", "Knight and Day", "Valkyrie", "Tropic Thunder", "War of the Worlds", "Collateral", "The Last Samurai", "Minority Report", "Vanilla Sky", "Magnolia", "Eyes Wide Shut", "Jerry Maguire", "A Few Good Men", "Rain Man", "Top Gun"),
        "Matt Damon" to listOf("Oppenheimer", "Air", "The Last Duel", "Stillwater", "Ford v Ferrari", "Suburbicon", "Downsizing", "The Great Wall", "Jason Bourne", "The Martian", "Interstellar", "The Monuments Men", "Elysium", "Contagion", "True Grit", "The Informant", "The Bourne Ultimatum", "The Departed", "Syriana", "The Bourne Supremacy", "Oceans Eleven", "The Bourne Identity", "The Talented Mr Ripley", "Saving Private Ryan", "Good Will Hunting"),
        "Leonardo DiCaprio" to listOf("Killers of the Flower Moon", "Dont Look Up", "Once Upon a Time in Hollywood", "The Revenant", "The Wolf of Wall Street", "Django Unchained", "J Edgar", "Inception", "Shutter Island", "Revolutionary Road", "Blood Diamond", "The Departed", "The Aviator", "Catch Me If You Can", "Gangs of New York", "Titanic"),
        "Brad Pitt" to listOf("Babylon", "Bullet Train", "Once Upon a Time in Hollywood", "Ad Astra", "The Big Short", "Fury", "12 Years a Slave", "World War Z", "Moneyball", "The Tree of Life", "Inglourious Basterds", "Burn After Reading", "The Curious Case of Benjamin Button", "The Assassination of Jesse James", "Babel", "Mr and Mrs Smith", "Troy", "Oceans Eleven", "Snatch", "Fight Club", "Seven", "Se7en", "12 Monkeys"),
        "Robert De Niro" to listOf("Killers of the Flower Moon", "The Irishman", "Joker", "Joy", "The Intern", "Silver Linings Playbook", "Meet the Parents", "Ronin", "Heat", "Casino", "A Bronx Tale", "Cape Fear", "Goodfellas", "The Untouchables", "The Mission", "Once Upon a Time in America", "The King of Comedy", "Raging Bull", "The Deer Hunter", "Taxi Driver", "The Godfather Part II"),
        "Christian Bale" to listOf("Thor Love and Thunder", "Ford v Ferrari", "Vice", "Hostiles", "The Big Short", "American Hustle", "The Dark Knight Rises", "The Fighter", "Public Enemies", "Terminator Salvation", "The Dark Knight", "The Prestige", "The New World", "Batman Begins", "The Machinist", "American Psycho"),
        "Margot Robbie" to listOf("Barbie", "Asteroid City", "Babylon", "Amsterdam", "The Suicide Squad", "Birds of Prey", "Bombshell", "Once Upon a Time in Hollywood", "Mary Queen of Scots", "I Tonya", "Suicide Squad", "The Big Short", "Focus", "The Wolf of Wall Street"),
        "Al Pacino" to listOf("House of Gucci", "The Irishman", "Once Upon a Time in Hollywood", "Oceans Thirteen", "Insomnia", "Any Given Sunday", "The Insider", "Donnie Brasco", "Heat", "Carlito's Way", "Scent of a Woman", "The Godfather Part III", "Dick Tracy", "Scarface", "Dog Day Afternoon", "Serpico", "The Godfather Part II", "The Godfather"),
        "Emma Stone" to listOf("Kinds of Kindness", "Poor Things", "Cruella", "The Favourite", "Battle of the Sexes", "La La Land", "Birdman", "Magic in the Moonlight", "The Amazing Spider-Man", "Crazy Stupid Love", "The Help", "Easy A", "Zombieland", "Superbad"),
        "Ryan Gosling" to listOf("The Fall Guy", "Barbie", "The Gray Man", "First Man", "Blade Runner 2049", "Song to Song", "La La Land", "The Nice Guys", "The Big Short", "Only God Forgives", "The Place Beyond the Pines", "Drive", "Crazy Stupid Love", "Blue Valentine", "The Notebook", "Half Nelson")
    )

    fun normalizeTitle(raw: String): String {
        val withoutParens = raw.replace(Regex("\\(.*?\\)"), "")
        val alphaNumericOnly = withoutParens.replace(Regex("[^a-zA-Z0-9\\s]"), "")
        return alphaNumericOnly.lowercase().split("\\s+".toRegex()).joinToString(" ").trim()
    }

    /**
     * Extracts the 4-digit release year from official release date, overview, notes, shared url, or title.
     */
    fun extractReleaseYear(item: MediaItem): Int? {
        // 0. Direct official release date/year from TMDB if available
        item.releaseYear?.let { return it }
        if (!item.releaseDate.isNullOrBlank()) {
            YEAR_DATE_REGEX.find(item.releaseDate)?.let { match ->
                match.groupValues.getOrNull(1)?.toIntOrNull()?.let { return it }
            }
        }

        // 1. Look for (YYYY) in title
        YEAR_PAREN_REGEX.find(item.title)?.let { match ->
            match.groupValues.getOrNull(1)?.toIntOrNull()?.let { return it }
        }

        // 2. Look for Letterboxd diary pattern in overview: "Movie (YYYY)" or "diary: ... (YYYY)"
        if (!item.overview.isNullOrBlank()) {
            YEAR_PAREN_REGEX.find(item.overview)?.let { match ->
                match.groupValues.getOrNull(1)?.toIntOrNull()?.let { return it }
            }
            YEAR_LETTERBOXD_REGEX.find(item.overview)?.let { match ->
                match.groupValues.getOrNull(1)?.toIntOrNull()?.let { return it }
            }
        }

        // 3. Look for year at end of Letterboxd slug URL: e.g. /film/ready-or-not-2019/ or /the-babysitter-2017/
        if (!item.sharedUrl.isNullOrBlank()) {
            YEAR_URL_REGEX.find(item.sharedUrl)?.let { match ->
                match.groupValues.getOrNull(1)?.toIntOrNull()?.let { return it }
            }
        }

        // 4. Look for (YYYY) in user notes
        if (!item.userNotes.isNullOrBlank()) {
            YEAR_PAREN_REGEX.find(item.userNotes)?.let { match ->
                match.groupValues.getOrNull(1)?.toIntOrNull()?.let { return it }
            }
        }

        // 5. Look for standalone 4-digit year in title
        YEAR_WORD_REGEX.find(item.title)?.let { match ->
            match.groupValues.getOrNull(1)?.toIntOrNull()?.let { return it }
        }

        return null
    }

    /**
     * Extracts rating on a 10.0 scale from item.rating, userNotes, or overview.
     */
    fun extractRating(item: MediaItem): Double? {
        val direct = item.rating
        if (direct != null && direct > 0.0) return direct

        val combinedText = "${item.userNotes ?: ""} ${item.overview ?: ""}"
        if (combinedText.isBlank()) return null

        // Star glyph parser (e.g. ★★★★½ -> 9.0, ★★★ -> 6.0)
        STAR_RATING_REGEX.find(combinedText)?.let { match ->
            val starsStr = match.groupValues.getOrNull(1) ?: ""
            var score = 0.0
            for (ch in starsStr) {
                if (ch == '★') score += 2.0
                else if (ch == '½') score += 1.0
            }
            if (score > 0.0 && score <= 10.0) return score
        }

        // "Rating: 8.5/10" or "8/10"
        RATING_SLASH_10_REGEX.find(combinedText)?.let { match ->
            match.groupValues.getOrNull(1)?.toDoubleOrNull()?.let { return it.coerceIn(0.0, 10.0) }
        }

        // "Rating: 4.5/5" -> 9.0
        RATING_SLASH_5_REGEX.find(combinedText)?.let { match ->
            match.groupValues.getOrNull(1)?.toDoubleOrNull()?.let { return (it * 2.0).coerceIn(0.0, 10.0) }
        }

        // "Rating: 8.5" or "Score: 9.0"
        SCORE_REGEX.find(combinedText)?.let { match ->
            match.groupValues.getOrNull(1)?.toDoubleOrNull()?.let {
                return if (it <= 5.0) (it * 2.0).coerceIn(0.0, 10.0) else it.coerceIn(0.0, 10.0)
            }
        }

        return null
    }

    /**
     * Maps a release year to a human-readable cinema era.
     */
    fun determineEra(year: Int?): String {
        if (year == null) return "Unknown"
        return when {
            year >= 2020 -> "2020s"
            year >= 2010 -> "2010s"
            year >= 2000 -> "2000s"
            year >= 1990 -> "1990s"
            year >= 1980 -> "1980s"
            year >= 1970 -> "1970s"
            else -> "Pre-1970s"
        }
    }

    /**
     * Generates complete cinephile analytics from a list of watched media items.
     */
    fun calculateAnalytics(
        items: List<MediaItem>,
        allProviders: List<StreamingProvider> = emptyList(),
        watchlistItems: List<MediaItem> = emptyList()
    ): WatchedAnalytics {
        val total = items.size
        if (total == 0) {
            return WatchedAnalytics(
                totalFilms = 0,
                totalHours = 0,
                avgRating = 0.0,
                ratedCount = 0,
                topEra = null,
                topGenre = null,
                eraBreakdown = emptyList(),
                genreBreakdown = emptyList(),
                serviceBreakdown = emptyList(),
                ratingBins = emptyList(),
                mostActiveMonth = null,
                mostActiveMonthCount = 0,
                watchedThisYear = 0,
                watchedLastYear = 0,
                topDirectors = emptyList(),
                topActors = emptyList(),
                blindSpots = emptyList(),
                monthlyVelocity = 0f
            )
        }

        val totalHours = (total * 110) / 60
        val ratedItemsWithScore = items.mapNotNull { item ->
            extractRating(item)?.let { score -> item to score }
        }
        val avgRating = if (ratedItemsWithScore.isNotEmpty()) {
            ratedItemsWithScore.map { it.second }.average()
        } else {
            0.0
        }
        val ratedCount = ratedItemsWithScore.size

        // --- 1. Era Breakdown ---
        val eraOrder = listOf("2020s", "2010s", "2000s", "1990s", "1980s", "1970s", "Pre-1970s", "Unknown")
        val eraCounts = items.groupBy { determineEra(extractReleaseYear(it)) }
            .mapValues { it.value.size }
        val eraBreakdown = eraOrder.mapNotNull { era ->
            val count = eraCounts[era] ?: 0
            if (count > 0) {
                EraStat(eraLabel = era, count = count, percentage = (count.toFloat() / total) * 100f)
            } else null
        }
        val topEra = eraBreakdown.filter { it.eraLabel != "Unknown" }.maxByOrNull { it.count }?.eraLabel

        // --- 2. Genre Breakdown & Avg Rating per Genre ---
        val genreItemsMap = mutableMapOf<String, MutableList<MediaItem>>()
        for (item in items) {
            val genres = item.genres?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            for (g in genres) {
                genreItemsMap.getOrPut(g) { mutableListOf() }.add(item)
            }
        }
        val genreBreakdown = genreItemsMap.map { (genre, gItems) ->
            val count = gItems.size
            val gRated = gItems.mapNotNull { extractRating(it) }
            val gAvg = if (gRated.isNotEmpty()) gRated.average() else null
            GenreStat(
                genre = genre,
                count = count,
                percentage = (count.toFloat() / total) * 100f,
                avgRating = gAvg
            )
        }.sortedByDescending { it.count }
        val topGenre = genreBreakdown.firstOrNull()?.genre

        // --- 3. Streaming Service / Provider Breakdown ---
        val providerLookup = allProviders.associateBy { it.id.lowercase().trim() }
        val serviceCounts = mutableMapOf<String, Int>()
        var unassignedServiceCount = 0

        for (item in items) {
            val pList = item.providersList
            if (pList.isEmpty()) {
                unassignedServiceCount++
            } else {
                for (pId in pList) {
                    val cleanId = pId.lowercase().trim()
                    serviceCounts[cleanId] = (serviceCounts[cleanId] ?: 0) + 1
                }
            }
        }

        val serviceBreakdown = serviceCounts.map { (pId, count) ->
            val displayName = providerLookup[pId]?.name ?: pId.replace('_', ' ').replaceFirstChar { it.uppercase() }
            ServiceStat(
                providerId = pId,
                displayName = displayName,
                count = count,
                percentage = (count.toFloat() / total) * 100f
            )
        }.sortedByDescending { it.count }.toMutableList()

        if (unassignedServiceCount > 0) {
            serviceBreakdown.add(
                ServiceStat(
                    providerId = "other",
                    displayName = "Other / Theatrical / Physical",
                    count = unassignedServiceCount,
                    percentage = (unassignedServiceCount.toFloat() / total) * 100f
                )
            )
        }

        // --- 4. Rating Bins ---
        val binDefinitions = listOf(
            "9.0 - 10 ★" to (9.0..10.0),
            "7.0 - 8.9 ★" to (7.0..8.99),
            "5.0 - 6.9 ★" to (5.0..6.99),
            "3.0 - 4.9 ★" to (3.0..4.99),
            "0.5 - 2.9 ★" to (0.5..2.99)
        )
        val ratingBins = binDefinitions.map { (label, range) ->
            val count = ratedItemsWithScore.count { it.second in range }
            RatingBinStat(
                binLabel = label,
                count = count,
                percentage = if (ratedItemsWithScore.isNotEmpty()) (count.toFloat() / ratedItemsWithScore.size) * 100f else 0f
            )
        }

        // --- 5. Viewing Rhythm, Calendar & Monthly Velocity ---
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val lastYear = currentYear - 1
        val monthSdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val displayMonthSdf = SimpleDateFormat("MMMM yyyy", Locale.US)
        val monthCounts = mutableMapOf<String, Int>()
        var thisYearCount = 0
        var lastYearCount = 0

        for (item in items) {
            val ts = item.watchedAt ?: item.addedAt
            if (ts > 0L) {
                cal.timeInMillis = ts
                val y = cal.get(Calendar.YEAR)
                if (y == currentYear) thisYearCount++
                if (y == lastYear) lastYearCount++

                val monthKey = monthSdf.format(Date(ts))
                monthCounts[monthKey] = (monthCounts[monthKey] ?: 0) + 1
            }
        }

        // Calculate average monthly watch velocity over recent active months
        val sortedMonths = monthCounts.keys.sorted()
        val recentMonths = sortedMonths.takeLast(6)
        val monthlyVelocity = if (recentMonths.isNotEmpty()) {
            recentMonths.map { monthCounts[it] ?: 0 }.average().toFloat()
        } else if (total > 0) {
            (total / 12f).coerceAtLeast(1f)
        } else {
            0f
        }

        val mostActive = monthCounts.maxByOrNull { it.value }
        val mostActiveDisplay = if (mostActive != null) {
            try {
                val d = monthSdf.parse(mostActive.key)
                if (d != null) displayMonthSdf.format(d) else mostActive.key
            } catch (e: Exception) {
                mostActive.key
            }
        } else null

        // --- 6. Top Directors & Auteurs (Issue #17) ---
        val dirLookup = mutableMapOf<String, String>()
        for ((director, dTitles) in DIRECTOR_FILMOGRAPHIES) {
            for (title in dTitles) {
                dirLookup[normalizeTitle(title)] = director
            }
        }

        val directorCountMap = mutableMapOf<String, Int>()
        val directorTitlesMap = mutableMapOf<String, MutableList<String>>()
        val directorRatingsMap = mutableMapOf<String, MutableList<Double>>()

        for (item in items) {
            val norm = normalizeTitle(item.title)
            val matchedDirector = dirLookup[norm]
            if (matchedDirector != null) {
                directorCountMap[matchedDirector] = (directorCountMap[matchedDirector] ?: 0) + 1
                directorTitlesMap.getOrPut(matchedDirector) { mutableListOf() }.add(item.title.trim())
                val score = extractRating(item)
                if (score != null && score > 0.0) {
                    directorRatingsMap.getOrPut(matchedDirector) { mutableListOf() }.add(score)
                }
            }
        }

        val topDirectors = directorCountMap.map { (director, count) ->
            val rList = directorRatingsMap[director] ?: emptyList()
            val dAvg = if (rList.isNotEmpty()) rList.average() else null
            DirectorStat(
                name = director,
                count = count,
                percentage = (count.toFloat() / total) * 100f,
                avgRating = dAvg,
                sampleTitles = directorTitlesMap[director]?.distinct()?.take(4) ?: emptyList()
            )
        }.sortedByDescending { it.count }.take(8)

        // --- 7. Top Actors & Screen Presence (Issue #17) ---
        val actorLookup = mutableMapOf<String, MutableList<String>>()
        for ((actor, aTitles) in ACTOR_FILMOGRAPHIES) {
            for (title in aTitles) {
                val n = normalizeTitle(title)
                actorLookup.getOrPut(n) { mutableListOf() }.add(actor)
            }
        }

        val actorCountMap = mutableMapOf<String, Int>()
        val actorTitlesMap = mutableMapOf<String, MutableList<String>>()

        for (item in items) {
            val norm = normalizeTitle(item.title)
            val matchedActors = actorLookup[norm]
            if (!matchedActors.isNullOrEmpty()) {
                for (actor in matchedActors) {
                    actorCountMap[actor] = (actorCountMap[actor] ?: 0) + 1
                    actorTitlesMap.getOrPut(actor) { mutableListOf() }.add(item.title.trim())
                }
            }
        }

        val topActors = actorCountMap.map { (actor, count) ->
            ActorStat(
                name = actor,
                count = count,
                percentage = (count.toFloat() / total) * 100f,
                sampleTitles = actorTitlesMap[actor]?.distinct()?.take(4) ?: emptyList()
            )
        }.sortedByDescending { it.count }.take(8)

        // --- 8. Cinema Blind Spots & Gaps (Issue #17) ---
        val blindSpots = mutableListOf<BlindSpotStat>()

        // 8a. Era Gap: Pre-1970s Golden Age Cinema
        val pre1970Count = eraCounts["Pre-1970s"] ?: 0
        val pre1970Pct = (pre1970Count.toFloat() / total) * 100f
        if (pre1970Pct < 5f) {
            val matchingWatchlist = watchlistItems.filter { extractReleaseYear(it)?.let { y -> y < 1970 } == true }
            blindSpots.add(
                BlindSpotStat(
                    category = "Cinema Era",
                    title = "Pre-1970s Golden Age Cinema",
                    description = "Only ${String.format(Locale.US, "%.1f", pre1970Pct)}% of your logged vault spans Hollywood's golden age, classic noir, and mid-century cinema.",
                    representationPercentage = pre1970Pct,
                    matchingWatchlistCount = matchingWatchlist.size,
                    sampleWatchlistTitles = matchingWatchlist.map { it.title }.take(3)
                )
            )
        }

        // 8b. Era Gap: 1970s New Hollywood
        val seventiesCount = eraCounts["1970s"] ?: 0
        val seventiesPct = (seventiesCount.toFloat() / total) * 100f
        if (seventiesPct < 6f) {
            val matchingWatchlist = watchlistItems.filter { extractReleaseYear(it)?.let { y -> y in 1970..1979 } == true }
            blindSpots.add(
                BlindSpotStat(
                    category = "Cinema Era",
                    title = "1970s New Hollywood Auteurs",
                    description = "Only ${String.format(Locale.US, "%.1f", seventiesPct)}% of your films represent the gritty 1970s auteur wave (Scorsese, Coppola, Lumet, Friedkin).",
                    representationPercentage = seventiesPct,
                    matchingWatchlistCount = matchingWatchlist.size,
                    sampleWatchlistTitles = matchingWatchlist.map { it.title }.take(3)
                )
            )
        }

        // 8c. Genre Gap: Westerns
        val westernCount = genreItemsMap["Western"]?.size ?: 0
        val westernPct = (westernCount.toFloat() / total) * 100f
        if (westernPct < 4f) {
            val matchingWatchlist = watchlistItems.filter { it.genres?.contains("Western", ignoreCase = true) == true }
            blindSpots.add(
                BlindSpotStat(
                    category = "Genre Gap",
                    title = "Westerns & Frontier Sagas",
                    description = "Westerns represent only ${String.format(Locale.US, "%.1f", westernPct)}% of your logged titles.",
                    representationPercentage = westernPct,
                    matchingWatchlistCount = matchingWatchlist.size,
                    sampleWatchlistTitles = matchingWatchlist.map { it.title }.take(3)
                )
            )
        }

        // 8d. Genre Gap: Documentaries
        val docCount = genreItemsMap["Documentary"]?.size ?: 0
        val docPct = (docCount.toFloat() / total) * 100f
        if (docPct < 4f) {
            val matchingWatchlist = watchlistItems.filter { it.genres?.contains("Documentary", ignoreCase = true) == true }
            blindSpots.add(
                BlindSpotStat(
                    category = "Genre Gap",
                    title = "Feature Documentaries",
                    description = "Documentaries account for just ${String.format(Locale.US, "%.1f", docPct)}% of your vault.",
                    representationPercentage = docPct,
                    matchingWatchlistCount = matchingWatchlist.size,
                    sampleWatchlistTitles = matchingWatchlist.map { it.title }.take(3)
                )
            )
        }

        return WatchedAnalytics(
            totalFilms = total,
            totalHours = totalHours,
            avgRating = avgRating,
            ratedCount = ratedCount,
            topEra = topEra,
            topGenre = topGenre,
            eraBreakdown = eraBreakdown,
            genreBreakdown = genreBreakdown,
            serviceBreakdown = serviceBreakdown,
            ratingBins = ratingBins,
            mostActiveMonth = mostActiveDisplay,
            mostActiveMonthCount = mostActive?.value ?: 0,
            watchedThisYear = thisYearCount,
            watchedLastYear = lastYearCount,
            topDirectors = topDirectors,
            topActors = topActors,
            blindSpots = blindSpots,
            monthlyVelocity = monthlyVelocity
        )
    }
}
