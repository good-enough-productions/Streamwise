package com.example.data.model

data class PodcastInfo(
    val id: String,
    val name: String,
    val hosts: String,
    val description: String,
    val emoji: String
)

data class PodcastMention(
    val podcastId: String,
    val movieTitle: String,
    val isMainSubject: Boolean,
    val episodeNote: String = ""
)

object PodcastEpisodeCatalog {
    val AVAILABLE_PODCASTS = listOf(
        PodcastInfo(
            id = "what_went_wrong",
            name = "What Went Wrong",
            hosts = "Lizzie & Chris",
            description = "The movie podcast about the glorious disasters behind movie history.",
            emoji = "💥"
        ),
        PodcastInfo(
            id = "the_rewatchables",
            name = "The Rewatchables",
            hosts = "Bill Simmons & The Ringer",
            description = "Movies that cannot be turned off when you stumble across them on cable.",
            emoji = "🍿"
        ),
        PodcastInfo(
            id = "the_big_picture",
            name = "The Big Picture",
            hosts = "Sean Fennessey & Amanda Dobbins",
            description = "Deep dives into the latest cinema releases, Oscars, and director retrospectives.",
            emoji = "🎬"
        ),
        PodcastInfo(
            id = "blank_check",
            name = "Blank Check",
            hosts = "Griffin Newman & David Sims",
            description = "Directors who have massive success and are given a blank check to make passion projects.",
            emoji = "🎟️"
        ),
        PodcastInfo(
            id = "unspooled",
            name = "Unspooled",
            hosts = "Paul Scheer & Amy Nicholson",
            description = "Journey through the greatest movies ever made with Paul & Amy.",
            emoji = "📽️"
        ),
        PodcastInfo(
            id = "hdtgm",
            name = "How Did This Get Made?",
            hosts = "Paul Scheer, June Diane Raphael & Jason Mantzoukas",
            description = "Hilarious breakdowns of the world's most baffling and enjoyable bad movies.",
            emoji = "🎙️"
        )
    )

    private val dynamicTitlesMap = mutableMapOf<String, Set<String>>()
    private val allDynamicTitlesSet = mutableSetOf<String>()

    fun initialize(context: android.content.Context) {
        if (dynamicTitlesMap.isNotEmpty()) return
        try {
            val jsonString = context.assets.open("podcast_titles.json").bufferedReader().use { it.readText() }
            initializeWithJson(jsonString)
        } catch (e: Exception) {
            android.util.Log.e("PodcastEpisodeCatalog", "Error loading podcast titles asset: ${e.message}")
        }
    }

    fun initializeWithJson(jsonString: String) {
        try {
            val jsonObject = org.json.JSONObject(jsonString)
            val keys = jsonObject.keys()
            val parsedMap = mutableMapOf<String, Set<String>>()
            while (keys.hasNext()) {
                val key = keys.next()
                val array = jsonObject.getJSONArray(key)
                val set = HashSet<String>(array.length())
                for (i in 0 until array.length()) {
                    set.add(array.getString(i))
                }
                parsedMap[key] = set
            }
            loadCatalog(parsedMap)
        } catch (e: Exception) {
            // Graceful fallback for non-Android environments
        }
    }

    fun loadCatalog(map: Map<String, Set<String>>) {
        dynamicTitlesMap.putAll(map)
        allDynamicTitlesSet.clear()
        for (set in dynamicTitlesMap.values) {
            allDynamicTitlesSet.addAll(set)
        }
    }

    fun clearForTesting() {
        dynamicTitlesMap.clear()
        allDynamicTitlesSet.clear()
    }

    // Catalog mapping lowercase normalized movie titles
    private val MENTIONS = listOf(
        // What Went Wrong
        PodcastMention("what_went_wrong", "waterworld", true, "Ep 1: The Floating Money Pit"),
        PodcastMention("what_went_wrong", "the island of dr moreau", true, "Ep 14: Marlon Brando on Set"),
        PodcastMention("what_went_wrong", "titanic", true, "Ep 25: The 200 Million Dollar Bet"),
        PodcastMention("what_went_wrong", "mad max fury road", true, "Ep 40: Chaos in the Namibian Desert"),
        PodcastMention("what_went_wrong", "alien 3", true, "Ep 52: Fincher's Lost Vision"),
        PodcastMention("what_went_wrong", "the abyss", true, "Ep 67: Deep Water Dangers"),
        PodcastMention("what_went_wrong", "jaws", true, "Ep 78: Bruce the Mechanical Shark"),
        PodcastMention("what_went_wrong", "twilight", true, "Ep 89: Behind the Teen Phenomenon"),
        PodcastMention("what_went_wrong", "blade runner", true, "Ep 102: The Cursed Classic"),
        PodcastMention("what_went_wrong", "the shining", true, "Ep 110: Kubrick's Mental Toll"),
        PodcastMention("what_went_wrong", "speed 2 cruise control", true, "Ep 121: The Boat Runaway"),
        PodcastMention("what_went_wrong", "batman and robin", true, "Ep 134: Bat-Nipples & Ice Puns"),
        PodcastMention("what_went_wrong", "the room", true, "Ep 145: Tommy Wiseau's Masterpiece"),
        PodcastMention("what_went_wrong", "the matrix", true, "Ep 150: Groundbreaking Sci-Fi Struggles"),
        PodcastMention("what_went_wrong", "spider-man 3", true, "Ep 160: Studio Interference"),
        PodcastMention("what_went_wrong", "the crow", true, "Ep 175: Tragedy on Set"),
        PodcastMention("what_went_wrong", "babylon", true, "Ep 188: Box Office Carnage"),
        PodcastMention("what_went_wrong", "fight club", false, "Mentioned during Fincher retrospective"),
        PodcastMention("what_went_wrong", "oppenheimer", false, "Mentioned during Nolan budget discussion"),
        PodcastMention("what_went_wrong", "dune part two", false, "Discussed during sci-fi scale comparison"),
        PodcastMention("what_went_wrong", "whiplash", false, "Discussed in indie budget deep dive"),

        // The Rewatchables
        PodcastMention("the_rewatchables", "heat", true, "Category Originator: Pacino & De Niro"),
        PodcastMention("the_rewatchables", "the godfather", true, "The 50th Anniversary Rewatch"),
        PodcastMention("the_rewatchables", "the godfather part ii", true, "The Sequel Standard"),
        PodcastMention("the_rewatchables", "the departed", true, "Boston Accents & Cranberry Juice"),
        PodcastMention("the_rewatchables", "goodfellas", true, "Scorsese Peak Form"),
        PodcastMention("the_rewatchables", "die hard", true, "Nakatomi Plaza Christmas"),
        PodcastMention("the_rewatchables", "the dark knight", true, "Ledger's Iconic Performance"),
        PodcastMention("the_rewatchables", "pulp fiction", true, "Tarantino's Game Changer"),
        PodcastMention("the_rewatchables", "jurassic park", true, "Spielberg Dinosaurs"),
        PodcastMention("the_rewatchables", "back to the future", true, "80s Perfect Script"),
        PodcastMention("the_rewatchables", "fight club", true, "David Fincher & Brad Pitt"),
        PodcastMention("the_rewatchables", "se7en", true, "What's in the Box?"),
        PodcastMention("the_rewatchables", "inception", true, "Spinning Tops & Nolan"),
        PodcastMention("the_rewatchables", "the big lebowski", true, "The Dude Abides"),
        PodcastMention("the_rewatchables", "the matrix", true, "Bullet Time & Leather Trenchcoats"),
        PodcastMention("the_rewatchables", "zodiac", true, "Fincher's Obsession"),
        PodcastMention("the_rewatchables", "no country for old men", true, "Anton Chigurh"),
        PodcastMention("the_rewatchables", "there will be blood", true, "I Drink Your Milkshake"),
        PodcastMention("the_rewatchables", "the social network", true, "Sorkin & Fincher"),
        PodcastMention("the_rewatchables", "mad max fury road", true, "Witness Me"),
        PodcastMention("the_rewatchables", "inglourious basterds", true, "Tarantino Alternate History"),
        PodcastMention("the_rewatchables", "top gun maverick", true, "Cruise Rescues Cinema"),
        PodcastMention("the_rewatchables", "oppenheimer", true, "Summer of Barbenheimer"),
        PodcastMention("the_rewatchables", "whiplash", true, "Not Quite My Tempo"),
        PodcastMention("the_rewatchables", "dune part two", false, "Discussed during Villeneuve retrospective"),
        PodcastMention("the_rewatchables", "titanic", false, "Mentioned in Cameron box office discussions"),

        // The Big Picture
        PodcastMention("the_big_picture", "oppenheimer", true, "The 2023 Film of the Year"),
        PodcastMention("the_big_picture", "barbie", true, "Greta Gerwig Cultural Phenomenon"),
        PodcastMention("the_big_picture", "dune part two", true, "Villeneuve's Sci-Fi Triumph"),
        PodcastMention("the_big_picture", "killers of the flower moon", true, "Scorsese's American Epic"),
        PodcastMention("the_big_picture", "past lives", true, "Celine Song's Heartbreaker"),
        PodcastMention("the_big_picture", "poor things", true, "Yorgos Lanthimos Fantasy"),
        PodcastMention("the_big_picture", "the holdovers", true, "Alexander Payne Holiday Classic"),
        PodcastMention("the_big_picture", "tar", true, "Todd Field & Cate Blanchett"),
        PodcastMention("the_big_picture", "everything everywhere all at once", true, "Multiverse Oscar Winner"),
        PodcastMention("the_big_picture", "the batman", true, "Matt Reeves Neo-Noir"),
        PodcastMention("the_big_picture", "licorice pizza", true, "PTA 70s Valley Nostalgia"),
        PodcastMention("the_big_picture", "babylon", true, "Chazelle's Hollywood Excess"),
        PodcastMention("the_big_picture", "challengers", true, "Zendaya & Luca Guadagnino"),
        PodcastMention("the_big_picture", "furiosa", true, "George Miller Wasteland Prequel"),
        PodcastMention("the_big_picture", "whiplash", false, "Chazelle career ranking"),
        PodcastMention("the_big_picture", "the matrix", false, "Action cinema benchmark mention"),

        // Blank Check
        PodcastMention("blank_check", "tenet", true, "The Christopher Nolan Miniseries"),
        PodcastMention("blank_check", "interstellar", true, "Love Transcending Dimensions"),
        PodcastMention("blank_check", "the prestige", true, "Are You Watching Closely?"),
        PodcastMention("blank_check", "dunkirk", true, "Three Timelines of Survival"),
        PodcastMention("blank_check", "avatar", true, "James Cameron Pod-Cast"),
        PodcastMention("blank_check", "titanic", true, "King of the World"),
        PodcastMention("blank_check", "spirited away", true, "Hayao Miyazaki Series"),
        PodcastMention("blank_check", "princess mononoke", true, "Miyazaki Epic"),
        PodcastMention("blank_check", "the matrix", true, "The Wachowskis Miniseries"),
        PodcastMention("blank_check", "speed racer", true, "Wachowskis Underrated Pop Art"),
        PodcastMention("blank_check", "cloud atlas", true, "Six Stories in One"),
        PodcastMention("blank_check", "babe pig in the city", true, "George Miller Miniseries"),
        PodcastMention("blank_check", "mad max fury road", true, "Miller Wasteland Masterwork"),
        PodcastMention("blank_check", "oppenheimer", true, "Nolan's Magum Opus"),
        PodcastMention("blank_check", "whiplash", false, "Discussed during contemporary directors round-up"),

        // How Did This Get Made?
        PodcastMention("hdtgm", "the room", true, "Live from Largo: Oh Hi Mark"),
        PodcastMention("hdtgm", "face off", true, "Cage & Travolta Swap Faces"),
        PodcastMention("hdtgm", "deep blue sea", true, "Super-Intelligent Sharks"),
        PodcastMention("hdtgm", "speed 2 cruise control", true, "Dafoe & Leeches"),
        PodcastMention("hdtgm", "battlefield earth", true, "Travolta Psychlo Mayhem"),
        PodcastMention("hdtgm", "geostorm", true, "Gerard Butler Controls the Weather"),
        PodcastMention("hdtgm", "cats", true, "Jellicle Songs for Jellicle Cats"),
        PodcastMention("hdtgm", "fast and furious 6", true, "Runway Length Physics"),
        PodcastMention("hdtgm", "twister", false, "Discussed during disaster movie comparisons"),
        PodcastMention("hdtgm", "waterworld", false, "Mentioned in biggest budget flops discussion")
    )

    internal fun normalize(str: String): String {
        var s = str.lowercase().trim()
        s = s.replace(Regex("""\s*\(\d{4}\)$"""), "").trim()
        s = s.replace(Regex("[^a-z0-9 ]"), "")
        s = s.replace(Regex("\\s+"), " ").trim()
        return s
    }

    fun isCoveredOnPodcast(
        movieTitle: String,
        podcastId: String?,
        mainSubjectOnly: Boolean = false,
        importSource: String? = null,
        notes: String? = null
    ): Boolean {
        if (podcastId.isNullOrBlank()) return true

        // 1. Tag and notes matching
        val podInfo = AVAILABLE_PODCASTS.find { it.id == podcastId }
        if (podInfo != null) {
            val name = podInfo.name
            val shortName = name.removePrefix("The ").trim()
            if (importSource?.contains(name, ignoreCase = true) == true ||
                importSource?.contains(shortName, ignoreCase = true) == true ||
                notes?.contains(name, ignoreCase = true) == true ||
                notes?.contains(shortName, ignoreCase = true) == true
            ) {
                return true
            }
            if (podcastId == "hdtgm") {
                if (importSource?.contains("How Did This Get Made", ignoreCase = true) == true ||
                    notes?.contains("How Did This Get Made", ignoreCase = true) == true ||
                    notes?.contains("HDTGM", ignoreCase = true) == true
                ) {
                    return true
                }
            }
        }

        val norm = normalize(movieTitle)
        if (norm.isBlank()) return false
        val normNoThe = if (norm.startsWith("the ")) norm.removePrefix("the ").trim() else norm

        // 2. Comprehensive asset catalog lookup (2,300+ scraped titles)
        val catalogSet = dynamicTitlesMap[podcastId]
        if (catalogSet != null) {
            if (catalogSet.contains(norm) || catalogSet.contains(normNoThe)) return true
            if (!mainSubjectOnly && catalogSet.any { 
                it == norm || it == normNoThe || it.contains(norm) || norm.contains(it) ||
                (normNoThe.length >= 4 && it.contains(normNoThe))
            }) {
                return true
            }
        }

        // 3. Fallback static MENTIONS
        return MENTIONS.any { mention ->
            if (mention.podcastId != podcastId) return@any false
            if (mainSubjectOnly && !mention.isMainSubject) return@any false

            val mentionNorm = normalize(mention.movieTitle)
            norm == mentionNorm || norm.contains(mentionNorm) || mentionNorm.contains(norm)
        }
    }

    fun getPodcastMentionsForMovie(movieTitle: String): List<PodcastMention> {
        val norm = normalize(movieTitle)
        if (norm.isBlank()) return emptyList()

        return MENTIONS.filter { mention ->
            val mentionNorm = normalize(mention.movieTitle)
            norm == mentionNorm || norm.contains(mentionNorm) || mentionNorm.contains(norm)
        }
    }

    private val staticMentionTitles by lazy {
        MENTIONS.map { normalize(it.movieTitle) }.toSet()
    }

    fun isCoveredOnAnyPodcast(movieTitle: String, importSource: String? = null, notes: String? = null): Boolean {
        if (!importSource.isNullOrBlank() || !notes.isNullOrBlank()) {
            for (pod in AVAILABLE_PODCASTS) {
                val name = pod.name
                val shortName = name.removePrefix("The ").trim()
                if (importSource?.contains(name, ignoreCase = true) == true ||
                    importSource?.contains(shortName, ignoreCase = true) == true ||
                    notes?.contains(name, ignoreCase = true) == true ||
                    notes?.contains(shortName, ignoreCase = true) == true
                ) return true
            }
            if (importSource?.contains("HDTGM", ignoreCase = true) == true ||
                notes?.contains("HDTGM", ignoreCase = true) == true
            ) return true
        }

        val norm = normalize(movieTitle)
        if (norm.isBlank()) return false
        val normNoThe = if (norm.startsWith("the ")) norm.removePrefix("the ").trim() else norm

        if (allDynamicTitlesSet.contains(norm) || allDynamicTitlesSet.contains(normNoThe)) {
            return true
        }

        return staticMentionTitles.contains(norm) || staticMentionTitles.contains(normNoThe)
    }
}
