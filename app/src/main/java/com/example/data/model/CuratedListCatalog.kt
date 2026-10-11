package com.example.data.model

import com.example.data.remote.TmdbMatchingHelper
import java.util.Locale

/**
 * Curated catalog providing foundational 3rd-party cinema lists:
 * - Oscar Best Picture Winners (1927–2024)
 * - AFI's 100 Greatest American Films
 * - Sight & Sound Greatest of All Time
 * - The Rewatchables Hall of Fame
 * - The Big Picture Essentials
 * - Blank Check Miniseries Highlights
 * - Criterion Channel Essentials
 * - IMDb Top 50 Classics
 *
 * Plus dynamic ad-hoc list parser for Letterboxd links and user text pastes.
 */
object CuratedListCatalog {

    fun getAllLists(): List<CuratedList> {
        return listOf(
            OSCAR_BEST_PICTURE,
            AFI_TOP_100,
            THE_REWATCHABLES,
            THE_BIG_PICTURE,
            SIGHT_AND_SOUND_50,
            CRITERION_ESSENTIALS,
            BLANK_CHECK,
            IMDB_TOP_50
        )
    }

    fun getListById(id: String): CuratedList? {
        return getAllLists().firstOrNull { it.id.equals(id, ignoreCase = true) }
    }

    /**
     * Dynamically enriches each film in a 3rd-party list against the user's
     * local personal database (watchlist, watched vault, and active streaming subscriptions).
     */
    fun crossReference(
        list: CuratedList,
        allItems: List<MediaItem>,
        activeProviders: List<StreamingProvider>
    ): List<EnrichedCuratedItem> {
        val activeIds = activeProviders.filter { it.isActive || it.costPerMonth == 0.0 }.map { it.id }.toSet()
        val watchedMap = HashMap<String, MediaItem>()
        val watchlistMap = HashMap<String, MediaItem>()

        for (item in allItems) {
            val key = TmdbMatchingHelper.normalizeTitle(item.title)
            if (key.isBlank()) continue
            if (item.status == MediaStatus.WATCHED.name) {
                // If duplicates exist, pick the one with rating or latest watch
                val existing = watchedMap[key]
                if (existing == null || (item.rating ?: 0.0) > (existing.rating ?: 0.0)) {
                    watchedMap[key] = item
                }
            } else {
                watchlistMap[key] = item
            }
        }

        return list.items.map { curItem ->
            val normTitle = TmdbMatchingHelper.normalizeTitle(curItem.title)

            // 1. Check watched history
            val watchedMatch = watchedMap[normTitle] ?: allItems.firstOrNull {
                it.status == MediaStatus.WATCHED.name &&
                isTitleAndYearMatch(normTitle, curItem.year, it)
            }

            // 2. Check personal watchlist
            val watchlistMatch = watchlistMap[normTitle] ?: allItems.firstOrNull {
                it.status != MediaStatus.WATCHED.name &&
                isTitleAndYearMatch(normTitle, curItem.year, it)
            }

            val matchedMedia = watchlistMatch ?: watchedMatch

            // 3. Resolve streaming providers
            val rawProviders = matchedMedia?.providersList
                ?: curItem.defaultProviders
            val cleanedProviders = rawProviders.filter { it.isNotBlank() && it != "none" }

            val availableOnMySubs = cleanedProviders.any { activeIds.contains(it) }
            val matchingActiveProviders = activeProviders.filter { cleanedProviders.contains(it.id) }

            EnrichedCuratedItem(
                item = curItem,
                isWatched = watchedMatch != null,
                userRating = watchedMatch?.rating,
                watchedAt = watchedMatch?.watchedAt,
                isInWatchlist = watchlistMatch != null,
                isStarred = watchlistMatch?.isStarred == true,
                matchingMediaItem = matchedMedia,
                streamingProviders = cleanedProviders,
                activeProviders = matchingActiveProviders,
                isAvailableOnMyServices = availableOnMySubs
            )
        }
    }

    private fun isTitleAndYearMatch(normCurTitle: String, curYear: Int?, media: MediaItem): Boolean {
        val normMedia = TmdbMatchingHelper.normalizeTitle(media.title)
        if (normMedia != normCurTitle) {
            // Check contains for subtitles (e.g. "The Lord of the Rings: The Return of the King")
            if (!normMedia.contains(normCurTitle) && !normCurTitle.contains(normMedia)) {
                return false
            }
        }
        if (curYear != null && media.releaseYear != null) {
            return kotlin.math.abs(curYear - media.releaseYear!!) <= 1
        }
        return true
    }

    /**
     * Computes real-time progress statistics for an enriched list.
     */
    fun computeStats(enrichedItems: List<EnrichedCuratedItem>): CuratedListStats {
        val total = enrichedItems.size
        val watched = enrichedItems.count { it.isWatched }
        val inWatchlist = enrichedItems.count { it.isInWatchlist }
        val streaming = enrichedItems.count { it.isAvailableOnMyServices }
        return CuratedListStats(
            totalCount = total,
            watchedCount = watched,
            watchlistCount = inWatchlist,
            streamingNowCount = streaming
        )
    }

    /**
     * Parses an ad-hoc custom list from user text, CSV paste, or Letterboxd export.
     * Supports formats like:
     * - "The Godfather (1972)"
     * - "1972, The Godfather"
     * - "1. Citizen Kane - Orson Welles"
     */
    fun parseAdHocList(rawText: String, title: String = "Custom Ad-Hoc List"): CuratedList {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("//") }

        val items = mutableListOf<CuratedListItem>()
        val yearRegex = Regex("""\b(19\d{2}|20\d{2})\b""")
        val numberedPrefixRegex = Regex("""^\d+[\.\)]\s*""")

        for ((index, line) in lines.withIndex()) {
            var clean = line.replace(numberedPrefixRegex, "").trim()
            val yearMatch = yearRegex.find(clean)
            val year = yearMatch?.value?.toIntOrNull()

            // Remove (1972) or [1972] from title
            if (yearMatch != null) {
                clean = clean.replace(Regex("""[\(\[]?\b${yearMatch.value}\b[\)\]]?"""), "").trim()
            }
            // Strip trailing/leading punctuation
            clean = clean.trim('-', ',', ':', ';', '•').trim()

            if (clean.isNotBlank()) {
                items.add(
                    CuratedListItem(
                        title = clean,
                        year = year,
                        badge = "#${index + 1}",
                        note = "Imported into ad-hoc viewer"
                    )
                )
            }
        }

        return CuratedList(
            id = "custom_adhoc_${System.currentTimeMillis()}",
            title = title,
            subtitle = "${items.size} Titles parsed on-the-fly",
            category = CuratedListCategory.CUSTOM,
            emoji = "🔗",
            description = "Ad-hoc cinema list loaded without cluttering your personal watchlist queue.",
            items = items
        )
    }

    // ==========================================
    // 1. OSCAR BEST PICTURE WINNERS (1927–2023)
    // ==========================================
    val OSCAR_BEST_PICTURE = CuratedList(
        id = "oscar_best_picture",
        title = "Oscar Best Picture Winners",
        subtitle = "96 Films (1927–2024)",
        category = CuratedListCategory.AWARDS,
        emoji = "🏆",
        description = "Every Academy Award Best Picture winner throughout cinema history, from Wings to Oppenheimer.",
        items = listOf(
            CuratedListItem("Oppenheimer", 2023, "2023 Winner", "Christopher Nolan", "7 Oscars including Best Director"),
            CuratedListItem("Everything Everywhere All at Once", 2022, "2022 Winner", "Daniels", "7 Oscars including Best Actress"),
            CuratedListItem("CODA", 2021, "2021 Winner", "Sian Heder", "First streaming-service Best Picture winner"),
            CuratedListItem("Nomadland", 2020, "2020 Winner", "Chloé Zhao", "Best Picture, Director & Actress"),
            CuratedListItem("Parasite", 2019, "2019 Winner", "Bong Joon-ho", "First non-English language Best Picture winner"),
            CuratedListItem("Green Book", 2018, "2018 Winner", "Peter Farrelly", "Best Picture & Original Screenplay"),
            CuratedListItem("The Shape of Water", 2017, "2017 Winner", "Guillermo del Toro", "4 Oscars including Best Director"),
            CuratedListItem("Moonlight", 2016, "2016 Winner", "Barry Jenkins", "3 Oscars including Adapted Screenplay"),
            CuratedListItem("Spotlight", 2015, "2015 Winner", "Tom McCarthy", "Best Picture & Original Screenplay"),
            CuratedListItem("Birdman", 2014, "2014 Winner", "Alejandro G. Iñárritu", "4 Oscars including Best Director"),
            CuratedListItem("12 Years a Slave", 2013, "2013 Winner", "Steve McQueen", "3 Oscars including Best Picture"),
            CuratedListItem("Argo", 2012, "2012 Winner", "Ben Affleck", "3 Oscars including Best Picture"),
            CuratedListItem("The Artist", 2011, "2011 Winner", "Michel Hazanavicius", "5 Oscars including Best Actor"),
            CuratedListItem("The King's Speech", 2010, "2010 Winner", "Tom Hooper", "4 Oscars including Best Actor"),
            CuratedListItem("The Hurt Locker", 2009, "2009 Winner", "Kathryn Bigelow", "6 Oscars including Best Director"),
            CuratedListItem("Slumdog Millionaire", 2008, "2008 Winner", "Danny Boyle", "8 Oscars including Best Picture"),
            CuratedListItem("No Country for Old Men", 2007, "2007 Winner", "Joel & Ethan Coen", "4 Oscars including Best Supporting Actor"),
            CuratedListItem("The Departed", 2006, "2006 Winner", "Martin Scorsese", "4 Oscars including Best Director"),
            CuratedListItem("Crash", 2005, "2005 Winner", "Paul Haggis", "3 Oscars including Best Picture"),
            CuratedListItem("Million Dollar Baby", 2004, "2004 Winner", "Clint Eastwood", "4 Oscars including Best Actress"),
            CuratedListItem("The Lord of the Rings: The Return of the King", 2003, "2003 Winner", "Peter Jackson", "Swept all 11 nominations"),
            CuratedListItem("Chicago", 2002, "2002 Winner", "Rob Marshall", "6 Oscars including Best Supporting Actress"),
            CuratedListItem("A Beautiful Mind", 2001, "2001 Winner", "Ron Howard", "4 Oscars including Best Director"),
            CuratedListItem("Gladiator", 2000, "2000 Winner", "Ridley Scott", "5 Oscars including Best Actor"),
            CuratedListItem("American Beauty", 1999, "1999 Winner", "Sam Mendes", "5 Oscars including Best Director"),
            CuratedListItem("Shakespeare in Love", 1998, "1998 Winner", "John Madden", "7 Oscars including Best Actress"),
            CuratedListItem("Titanic", 1997, "1997 Winner", "James Cameron", "Tied record with 11 Oscars"),
            CuratedListItem("The English Patient", 1996, "1996 Winner", "Anthony Minghella", "9 Oscars including Best Director"),
            CuratedListItem("Braveheart", 1995, "1995 Winner", "Mel Gibson", "5 Oscars including Best Director"),
            CuratedListItem("Forrest Gump", 1994, "1994 Winner", "Robert Zemeckis", "6 Oscars including Best Actor"),
            CuratedListItem("Schindler's List", 1993, "1993 Winner", "Steven Spielberg", "7 Oscars including Best Director"),
            CuratedListItem("Unforgiven", 1992, "1992 Winner", "Clint Eastwood", "4 Oscars including Best Director"),
            CuratedListItem("The Silence of the Lambs", 1991, "1991 Winner", "Jonathan Demme", "Big Five Academy Award sweep"),
            CuratedListItem("Dances with Wolves", 1990, "1990 Winner", "Kevin Costner", "7 Oscars including Best Director"),
            CuratedListItem("Driving Miss Daisy", 1989, "1989 Winner", "Bruce Beresford", "4 Oscars including Best Actress"),
            CuratedListItem("Rain Man", 1988, "1988 Winner", "Barry Levinson", "4 Oscars including Best Actor"),
            CuratedListItem("The Last Emperor", 1987, "1987 Winner", "Bernardo Bertolucci", "Swept all 9 nominations"),
            CuratedListItem("Platoon", 1986, "1986 Winner", "Oliver Stone", "4 Oscars including Best Director"),
            CuratedListItem("Out of Africa", 1985, "1985 Winner", "Sydney Pollack", "7 Oscars including Best Director"),
            CuratedListItem("Amadeus", 1984, "1984 Winner", "Miloš Forman", "8 Oscars including Best Actor"),
            CuratedListItem("Terms of Endearment", 1983, "1983 Winner", "James L. Brooks", "5 Oscars including Best Actress"),
            CuratedListItem("Gandhi", 1982, "1982 Winner", "Richard Attenborough", "8 Oscars including Best Actor"),
            CuratedListItem("Chariots of Fire", 1981, "1981 Winner", "Hugh Hudson", "4 Oscars including Best Original Score"),
            CuratedListItem("Ordinary People", 1980, "1980 Winner", "Robert Redford", "4 Oscars including Best Director"),
            CuratedListItem("Kramer vs. Kramer", 1979, "1979 Winner", "Robert Benton", "5 Oscars including Best Actor"),
            CuratedListItem("The Deer Hunter", 1978, "1978 Winner", "Michael Cimino", "5 Oscars including Best Director"),
            CuratedListItem("Annie Hall", 1977, "1977 Winner", "Woody Allen", "4 Oscars including Best Actress"),
            CuratedListItem("Rocky", 1976, "1976 Winner", "John G. Avildsen", "3 Oscars including Best Director"),
            CuratedListItem("One Flew Over the Cuckoo's Nest", 1975, "1975 Winner", "Miloš Forman", "Big Five Academy Award sweep"),
            CuratedListItem("The Godfather Part II", 1974, "1974 Winner", "Francis Ford Coppola", "First sequel to win Best Picture"),
            CuratedListItem("The Sting", 1973, "1973 Winner", "George Roy Hill", "7 Oscars including Best Director"),
            CuratedListItem("The Godfather", 1972, "1972 Winner", "Francis Ford Coppola", "3 Oscars including Best Actor"),
            CuratedListItem("The French Connection", 1971, "1971 Winner", "William Friedkin", "5 Oscars including Best Actor"),
            CuratedListItem("Patton", 1970, "1970 Winner", "Franklin J. Schaffner", "7 Oscars including Best Actor"),
            CuratedListItem("Midnight Cowboy", 1969, "1969 Winner", "John Schlesinger", "Only X-rated film to win Best Picture"),
            CuratedListItem("Oliver!", 1968, "1968 Winner", "Carol Reed", "5 Oscars including Best Director"),
            CuratedListItem("In the Heat of the Night", 1967, "1967 Winner", "Norman Jewison", "5 Oscars including Best Actor"),
            CuratedListItem("A Man for All Seasons", 1966, "1966 Winner", "Fred Zinnemann", "6 Oscars including Best Actor"),
            CuratedListItem("The Sound of Music", 1965, "1965 Winner", "Robert Wise", "5 Oscars including Best Director"),
            CuratedListItem("My Fair Lady", 1964, "1964 Winner", "George Cukor", "8 Oscars including Best Actor"),
            CuratedListItem("Tom Jones", 1963, "1963 Winner", "Tony Richardson", "4 Oscars including Best Director"),
            CuratedListItem("Lawrence of Arabia", 1962, "1962 Winner", "David Lean", "7 Oscars including Best Director"),
            CuratedListItem("West Side Story", 1961, "1961 Winner", "Robert Wise & Jerome Robbins", "10 Oscars including Best Director"),
            CuratedListItem("The Apartment", 1960, "1960 Winner", "Billy Wilder", "5 Oscars including Best Director"),
            CuratedListItem("Ben-Hur", 1959, "1959 Winner", "William Wyler", "Record 11 Oscars"),
            CuratedListItem("Gigi", 1958, "1958 Winner", "Vincente Minnelli", "Swept all 9 nominations"),
            CuratedListItem("The Bridge on the River Kwai", 1957, "1957 Winner", "David Lean", "7 Oscars including Best Actor"),
            CuratedListItem("Around the World in 80 Days", 1956, "1956 Winner", "Michael Anderson", "5 Oscars including Best Picture"),
            CuratedListItem("Marty", 1955, "1955 Winner", "Delbert Mann", "Shortest film to win Best Picture"),
            CuratedListItem("On the Waterfront", 1954, "1954 Winner", "Elia Kazan", "8 Oscars including Best Actor"),
            CuratedListItem("From Here to Eternity", 1953, "1953 Winner", "Fred Zinnemann", "8 Oscars including Best Director"),
            CuratedListItem("The Greatest Show on Earth", 1952, "1952 Winner", "Cecil B. DeMille", "2 Oscars including Best Picture"),
            CuratedListItem("An American in Paris", 1951, "1951 Winner", "Vincente Minnelli", "6 Oscars including Best Picture"),
            CuratedListItem("All About Eve", 1950, "1950 Winner", "Joseph L. Mankiewicz", "6 Oscars from 14 nominations"),
            CuratedListItem("All the King's Men", 1949, "1949 Winner", "Robert Rossen", "3 Oscars including Best Actor"),
            CuratedListItem("Hamlet", 1948, "1948 Winner", "Laurence Olivier", "First British production to win"),
            CuratedListItem("Gentleman's Agreement", 1947, "1947 Winner", "Elia Kazan", "3 Oscars including Best Director"),
            CuratedListItem("The Best Years of Our Lives", 1946, "1946 Winner", "William Wyler", "7 Oscars including Best Director"),
            CuratedListItem("The Lost Weekend", 1945, "1945 Winner", "Billy Wilder", "4 Oscars including Best Actor"),
            CuratedListItem("Going My Way", 1944, "1944 Winner", "Leo McCarey", "7 Oscars including Best Actor"),
            CuratedListItem("Casablanca", 1943, "1943 Winner", "Michael Curtiz", "3 Oscars including Best Director"),
            CuratedListItem("Mrs. Miniver", 1942, "1942 Winner", "William Wyler", "6 Oscars including Best Actress"),
            CuratedListItem("How Green Was My Valley", 1941, "1941 Winner", "John Ford", "5 Oscars including Best Director"),
            CuratedListItem("Rebecca", 1940, "1940 Winner", "Alfred Hitchcock", "Hitchcock's only Best Picture winner"),
            CuratedListItem("Gone with the Wind", 1939, "1939 Winner", "Victor Fleming", "8 Oscars plus 2 honorary"),
            CuratedListItem("You Can't Take It with You", 1938, "1938 Winner", "Frank Capra", "Best Picture & Best Director"),
            CuratedListItem("The Life of Emile Zola", 1937, "1937 Winner", "William Dieterle", "3 Oscars including Best Picture"),
            CuratedListItem("The Great Ziegfeld", 1936, "1936 Winner", "Robert Z. Leonard", "3 Oscars including Best Actress"),
            CuratedListItem("Mutiny on the Bounty", 1935, "1935 Winner", "Frank Lloyd", "Best Picture"),
            CuratedListItem("It Happened One Night", 1934, "1934 Winner", "Frank Capra", "First to sweep the Big Five"),
            CuratedListItem("Cavalcade", 1933, "1933 Winner", "Frank Lloyd", "3 Oscars including Best Director"),
            CuratedListItem("Grand Hotel", 1932, "1932 Winner", "Edmund Goulding", "Only film to win Best Picture with zero other noms"),
            CuratedListItem("Cimarron", 1931, "1931 Winner", "Wesley Ruggles", "3 Oscars including Best Picture"),
            CuratedListItem("All Quiet on the Western Front", 1930, "1930 Winner", "Lewis Milestone", "First anti-war Best Picture"),
            CuratedListItem("The Broadway Melody", 1929, "1929 Winner", "Harry Beaumont", "First sound film to win Best Picture"),
            CuratedListItem("Wings", 1927, "1927 Winner", "William A. Wellman", "First ever Academy Award Best Picture winner")
        )
    )

    // ==========================================
    // 2. AFI'S 100 YEARS... 100 MOVIES (10th Anniv)
    // ==========================================
    val AFI_TOP_100 = CuratedList(
        id = "afi_top_100",
        title = "AFI's 100 Greatest American Films",
        subtitle = "The 10th Anniversary Definitive Canon",
        category = CuratedListCategory.CANON,
        emoji = "🏛️",
        description = "The American Film Institute's definitive ranking of the 100 greatest films of all time.",
        items = listOf(
            CuratedListItem("Citizen Kane", 1941, "#1 AFI", "Orson Welles"),
            CuratedListItem("The Godfather", 1972, "#2 AFI", "Francis Ford Coppola"),
            CuratedListItem("Casablanca", 1942, "#3 AFI", "Michael Curtiz"),
            CuratedListItem("Raging Bull", 1980, "#4 AFI", "Martin Scorsese"),
            CuratedListItem("Singin' in the Rain", 1952, "#5 AFI", "Gene Kelly & Stanley Donen"),
            CuratedListItem("Gone with the Wind", 1939, "#6 AFI", "Victor Fleming"),
            CuratedListItem("Lawrence of Arabia", 1962, "#7 AFI", "David Lean"),
            CuratedListItem("Schindler's List", 1993, "#8 AFI", "Steven Spielberg"),
            CuratedListItem("Vertigo", 1958, "#9 AFI", "Alfred Hitchcock"),
            CuratedListItem("The Wizard of Oz", 1939, "#10 AFI", "Victor Fleming"),
            CuratedListItem("City Lights", 1931, "#11 AFI", "Charlie Chaplin"),
            CuratedListItem("The Searchers", 1956, "#12 AFI", "John Ford"),
            CuratedListItem("Star Wars", 1977, "#13 AFI", "George Lucas"),
            CuratedListItem("Psycho", 1960, "#14 AFI", "Alfred Hitchcock"),
            CuratedListItem("2001: A Space Odyssey", 1968, "#15 AFI", "Stanley Kubrick"),
            CuratedListItem("Sunset Boulevard", 1950, "#16 AFI", "Billy Wilder"),
            CuratedListItem("The Graduate", 1967, "#17 AFI", "Mike Nichols"),
            CuratedListItem("The General", 1926, "#18 AFI", "Buster Keaton"),
            CuratedListItem("On the Waterfront", 1954, "#19 AFI", "Elia Kazan"),
            CuratedListItem("It's a Wonderful Life", 1946, "#20 AFI", "Frank Capra"),
            CuratedListItem("Chinatown", 1974, "#21 AFI", "Roman Polanski"),
            CuratedListItem("Some Like It Hot", 1959, "#22 AFI", "Billy Wilder"),
            CuratedListItem("The Grapes of Wrath", 1940, "#23 AFI", "John Ford"),
            CuratedListItem("E.T. the Extra-Terrestrial", 1982, "#24 AFI", "Steven Spielberg"),
            CuratedListItem("To Kill a Mockingbird", 1962, "#25 AFI", "Robert Mulligan"),
            CuratedListItem("Mr. Smith Goes to Washington", 1939, "#26 AFI", "Frank Capra"),
            CuratedListItem("High Noon", 1952, "#27 AFI", "Fred Zinnemann"),
            CuratedListItem("All About Eve", 1950, "#28 AFI", "Joseph L. Mankiewicz"),
            CuratedListItem("Double Indemnity", 1944, "#29 AFI", "Billy Wilder"),
            CuratedListItem("Apocalypse Now", 1979, "#30 AFI", "Francis Ford Coppola"),
            CuratedListItem("The Maltese Falcon", 1941, "#31 AFI", "John Huston"),
            CuratedListItem("The Godfather Part II", 1974, "#32 AFI", "Francis Ford Coppola"),
            CuratedListItem("One Flew Over the Cuckoo's Nest", 1975, "#33 AFI", "Miloš Forman"),
            CuratedListItem("Snow White and the Seven Dwarfs", 1937, "#34 AFI", "David Hand"),
            CuratedListItem("Annie Hall", 1977, "#35 AFI", "Woody Allen"),
            CuratedListItem("The Bridge on the River Kwai", 1957, "#36 AFI", "David Lean"),
            CuratedListItem("The Best Years of Our Lives", 1946, "#37 AFI", "William Wyler"),
            CuratedListItem("The Treasure of the Sierra Madre", 1948, "#38 AFI", "John Huston"),
            CuratedListItem("Dr. Strangelove", 1964, "#39 AFI", "Stanley Kubrick"),
            CuratedListItem("The Sound of Music", 1965, "#40 AFI", "Robert Wise"),
            CuratedListItem("King Kong", 1933, "#41 AFI", "Merian C. Cooper"),
            CuratedListItem("Bonnie and Clyde", 1967, "#42 AFI", "Arthur Penn"),
            CuratedListItem("Midnight Cowboy", 1969, "#43 AFI", "John Schlesinger"),
            CuratedListItem("The Philadelphia Story", 1940, "#44 AFI", "George Cukor"),
            CuratedListItem("Shane", 1953, "#45 AFI", "George Stevens"),
            CuratedListItem("It Happened One Night", 1934, "#46 AFI", "Frank Capra"),
            CuratedListItem("A Streetcar Named Desire", 1951, "#47 AFI", "Elia Kazan"),
            CuratedListItem("Rear Window", 1954, "#48 AFI", "Alfred Hitchcock"),
            CuratedListItem("Intolerance", 1916, "#49 AFI", "D.W. Griffith"),
            CuratedListItem("The Lord of the Rings: The Fellowship of the Ring", 2001, "#50 AFI", "Peter Jackson"),
            CuratedListItem("West Side Story", 1961, "#51 AFI", "Robert Wise"),
            CuratedListItem("Taxi Driver", 1976, "#52 AFI", "Martin Scorsese"),
            CuratedListItem("The Deer Hunter", 1978, "#53 AFI", "Michael Cimino"),
            CuratedListItem("MASH", 1970, "#54 AFI", "Robert Altman"),
            CuratedListItem("North by Northwest", 1959, "#55 AFI", "Alfred Hitchcock"),
            CuratedListItem("Jaws", 1975, "#56 AFI", "Steven Spielberg"),
            CuratedListItem("Rocky", 1976, "#57 AFI", "John G. Avildsen"),
            CuratedListItem("The Gold Rush", 1925, "#58 AFI", "Charlie Chaplin"),
            CuratedListItem("Nashville", 1975, "#59 AFI", "Robert Altman"),
            CuratedListItem("Duck Soup", 1933, "#60 AFI", "Leo McCarey"),
            CuratedListItem("Sullivan's Travels", 1941, "#61 AFI", "Preston Sturges"),
            CuratedListItem("American Graffiti", 1973, "#62 AFI", "George Lucas"),
            CuratedListItem("Cabaret", 1972, "#63 AFI", "Bob Fosse"),
            CuratedListItem("Network", 1976, "#64 AFI", "Sidney Lumet"),
            CuratedListItem("The African Queen", 1951, "#65 AFI", "John Huston"),
            CuratedListItem("Raiders of the Lost Ark", 1981, "#66 AFI", "Steven Spielberg"),
            CuratedListItem("Who's Afraid of Virginia Woolf?", 1966, "#67 AFI", "Mike Nichols"),
            CuratedListItem("Unforgiven", 1992, "#68 AFI", "Clint Eastwood"),
            CuratedListItem("Tootsie", 1982, "#69 AFI", "Sydney Pollack"),
            CuratedListItem("A Clockwork Orange", 1971, "#70 AFI", "Stanley Kubrick"),
            CuratedListItem("Saving Private Ryan", 1998, "#71 AFI", "Steven Spielberg"),
            CuratedListItem("The Shawshank Redemption", 1994, "#72 AFI", "Frank Darabont"),
            CuratedListItem("Butch Cassidy and the Sundance Kid", 1969, "#73 AFI", "George Roy Hill"),
            CuratedListItem("The Silence of the Lambs", 1991, "#74 AFI", "Jonathan Demme"),
            CuratedListItem("In the Heat of the Night", 1967, "#75 AFI", "Norman Jewison"),
            CuratedListItem("Forrest Gump", 1994, "#76 AFI", "Robert Zemeckis"),
            CuratedListItem("All the President's Men", 1976, "#77 AFI", "Alan J. Pakula"),
            CuratedListItem("Modern Times", 1936, "#78 AFI", "Charlie Chaplin"),
            CuratedListItem("The Wild Bunch", 1969, "#79 AFI", "Sam Peckinpah"),
            CuratedListItem("The Apartment", 1960, "#80 AFI", "Billy Wilder"),
            CuratedListItem("Spartacus", 1960, "#81 AFI", "Stanley Kubrick"),
            CuratedListItem("Sunrise", 1927, "#82 AFI", "F.W. Murnau"),
            CuratedListItem("Titanic", 1997, "#83 AFI", "James Cameron"),
            CuratedListItem("Easy Rider", 1969, "#84 AFI", "Dennis Hopper"),
            CuratedListItem("A Night at the Opera", 1935, "#85 AFI", "Sam Wood"),
            CuratedListItem("Platoon", 1986, "#86 AFI", "Oliver Stone"),
            CuratedListItem("12 Angry Men", 1957, "#87 AFI", "Sidney Lumet"),
            CuratedListItem("Bringing Up Baby", 1938, "#88 AFI", "Howard Hawks"),
            CuratedListItem("The Sixth Sense", 1999, "#89 AFI", "M. Night Shyamalan"),
            CuratedListItem("Swing Time", 1936, "#90 AFI", "George Stevens"),
            CuratedListItem("Sophie's Choice", 1982, "#91 AFI", "Alan J. Pakula"),
            CuratedListItem("Goodfellas", 1990, "#92 AFI", "Martin Scorsese"),
            CuratedListItem("The French Connection", 1971, "#93 AFI", "William Friedkin"),
            CuratedListItem("Pulp Fiction", 1994, "#94 AFI", "Quentin Tarantino"),
            CuratedListItem("The Last Picture Show", 1971, "#95 AFI", "Peter Bogdanovich"),
            CuratedListItem("Do the Right Thing", 1989, "#96 AFI", "Spike Lee"),
            CuratedListItem("Blade Runner", 1982, "#97 AFI", "Ridley Scott"),
            CuratedListItem("Yankee Doodle Dandy", 1942, "#98 AFI", "Michael Curtiz"),
            CuratedListItem("Toy Story", 1995, "#99 AFI", "John Lasseter"),
            CuratedListItem("Ben-Hur", 1959, "#100 AFI", "William Wyler")
        )
    )

    // ==========================================
    // 3. THE REWATCHABLES (The Ringer)
    // ==========================================
    val THE_REWATCHABLES = CuratedList(
        id = "the_rewatchables",
        title = "The Rewatchables Hall of Fame",
        subtitle = "Bill Simmons & The Ringer Canon",
        category = CuratedListCategory.PODCASTS,
        emoji = "🍿",
        description = "Movies that cannot be turned off when you stumble across them on cable, broken down by Bill Simmons, Chris Ryan, Sean Fennessey, and guests.",
        items = listOf(
            CuratedListItem("Heat", 1995, "Apex Mountain", "Michael Mann", "Category Originator: Pacino & De Niro diner scene"),
            CuratedListItem("The Godfather", 1972, "50th Anniv", "Francis Ford Coppola", "The all-time movie masterpiece"),
            CuratedListItem("The Godfather Part II", 1974, "Classic", "Francis Ford Coppola", "The sequel standard"),
            CuratedListItem("The Departed", 2006, "Boston Classic", "Martin Scorsese", "Leo, Matt, Jack, Mark Wahlberg cranberry juice"),
            CuratedListItem("Goodfellas", 1990, "Scorsese Peak", "Martin Scorsese", "As far back as I can remember I always wanted to be a gangster"),
            CuratedListItem("Die Hard", 1988, "Christmas Pick", "John McTiernan", "Nakatomi Plaza, John McClane & Hans Gruber"),
            CuratedListItem("The Dark Knight", 2008, "Superhero Peak", "Christopher Nolan", "Heath Ledger's Joker benchmark"),
            CuratedListItem("Pulp Fiction", 1994, "90s Revolution", "Quentin Tarantino", "Tarantino's pop-culture explosion"),
            CuratedListItem("Jurassic Park", 1993, "Summer Blockbuster", "Steven Spielberg", "Dinosaurs that still look breathtaking"),
            CuratedListItem("Back to the Future", 1985, "Perfect Script", "Robert Zemeckis", "Marty McFly & Doc Brown in 1955"),
            CuratedListItem("Fight Club", 1999, "Fincher Masterpiece", "David Fincher", "Brad Pitt, Edward Norton and the soap"),
            CuratedListItem("Se7en", 1995, "Crime Thriller", "David Fincher", "What's in the box?!"),
            CuratedListItem("Inception", 2010, "Heist In A Dream", "Christopher Nolan", "Spinning tops, hallway fights, Zimmer horns"),
            CuratedListItem("The Big Lebowski", 1998, "Cult Classic", "Joel & Ethan Coen", "The Dude abides, Walter Sobchak bowling"),
            CuratedListItem("The Matrix", 1999, "Sci-Fi Gamechanger", "Lana & Lilly Wachowski", "Bullet time, red pill, leather trenchcoats"),
            CuratedListItem("Zodiac", 2007, "Obsession Cinema", "David Fincher", "Mark Ruffalo animal crackers, Gyllenhaal files"),
            CuratedListItem("No Country for Old Men", 2007, "Modern Western", "Joel & Ethan Coen", "Anton Chigurh coin flips"),
            CuratedListItem("There Will Be Blood", 2007, "American Epic", "Paul Thomas Anderson", "I drink your milkshake!"),
            CuratedListItem("The Social Network", 2010, "Sorkin & Fincher", "David Fincher", "A million dollars isn't cool. You know what is?"),
            CuratedListItem("Mad Max: Fury Road", 2015, "Action Benchmark", "George Miller", "Witness me! Two-hour car chase masterpiece"),
            CuratedListItem("Inglourious Basterds", 2009, "Alternate History", "Quentin Tarantino", "Christoph Waltz opening farmhouse scene"),
            CuratedListItem("Top Gun: Maverick", 2022, "Cinema Rescuer", "Joseph Kosinski", "Tom Cruise in real cockpits"),
            CuratedListItem("Oppenheimer", 2023, "Barbenheimer", "Christopher Nolan", "The summer 2023 phenomenon"),
            CuratedListItem("Whiplash", 2014, "Intensity", "Damien Chazelle", "Not quite my tempo"),
            CuratedListItem("Speed", 1994, "Action Perfection", "Jan de Bont", "Keep the bus above 50 mph"),
            CuratedListItem("Point Break", 1991, "Pure Rewatchable", "Kathryn Bigelow", "Keanu & Swayze skydiving and surfing"),
            CuratedListItem("Crimson Tide", 1995, "Tony Scott Submarine", "Tony Scott", "Denzel vs Hackman Silver Surfer debate"),
            CuratedListItem("A Few Good Men", 1992, "Courtroom Drama", "Rob Reiner", "You can't handle the truth!"),
            CuratedListItem("Tombstone", 1993, "Western Essential", "George P. Cosmatos", "I'm your huckleberry"),
            CuratedListItem("The Fugitive", 1993, "Harrison Ford Peak", "Andrew Davis", "I didn't kill my wife! I don't care!"),
            CuratedListItem("Predator", 1987, "Arnold Classic", "John McTiernan", "Get to the chopper!"),
            CuratedListItem("Jaws", 1975, "First Summer Blockbuster", "Steven Spielberg", "You're gonna need a bigger boat"),
            CuratedListItem("Ferris Bueller's Day Off", 1986, "Hughes Classic", "John Hughes", "Life moves pretty fast")
        )
    )

    // ==========================================
    // 4. THE BIG PICTURE (Sean & Amanda)
    // ==========================================
    val THE_BIG_PICTURE = CuratedList(
        id = "the_big_picture",
        title = "The Big Picture Essentials",
        subtitle = "Sean Fennessey & Amanda Dobbins Picks",
        category = CuratedListCategory.PODCASTS,
        emoji = "🎬",
        description = "Modern cinema essentials, Movie Draft favorites, and Oscar contenders celebrated on The Ringer's flagship film show.",
        items = listOf(
            CuratedListItem("Oppenheimer", 2023, "2023 Film of Year", "Christopher Nolan", "Summer titan and Oscar champion"),
            CuratedListItem("Barbie", 2023, "Cultural Event", "Greta Gerwig", "Barbenheimer phenomenon"),
            CuratedListItem("Dune: Part Two", 2024, "Sci-Fi Triumph", "Denis Villeneuve", "Sandworms, Arrakis, IMAX grandeur"),
            CuratedListItem("Killers of the Flower Moon", 2023, "Scorsese Epic", "Martin Scorsese", "Lily Gladstone & DiCaprio tragedy"),
            CuratedListItem("Past Lives", 2023, "Indie Heartbreaker", "Celine Song", "In-Yun, childhood love, NYC reunion"),
            CuratedListItem("Poor Things", 2023, "Lanthimos Fantasy", "Yorgos Lanthimos", "Emma Stone tour de force"),
            CuratedListItem("The Holdovers", 2023, "Holiday Classic", "Alexander Payne", "Paul Giamatti & Da'Vine Joy Randolph"),
            CuratedListItem("Tár", 2022, "Field & Blanchett", "Todd Field", "Lydia Tár's rise and spiral"),
            CuratedListItem("Everything Everywhere All at Once", 2022, "Multiverse Marvel", "Daniels", "Bagel of nihilism to family love"),
            CuratedListItem("The Batman", 2022, "Neo-Noir", "Matt Reeves", "Pattinson Nirvana detective story"),
            CuratedListItem("Licorice Pizza", 2021, "PTA 70s Valley", "Paul Thomas Anderson", "Haim & Cooper running through the Valley"),
            CuratedListItem("Babylon", 2022, "Hollywood Excess", "Damien Chazelle", "Wild love letter to early cinema"),
            CuratedListItem("Challengers", 2024, "Horny Tennis Drama", "Luca Guadagnino", "Zendaya & Reznor/Ross pulsating score"),
            CuratedListItem("Furiosa: A Mad Max Saga", 2024, "Miller Wasteland", "George Miller", "Anya Taylor-Joy and Chris Hemsworth"),
            CuratedListItem("Anatomy of a Fall", 2023, "Palme d'Or Winner", "Justine Triet", "Sandra Hüller courtroom mystery"),
            CuratedListItem("The Zone of Interest", 2023, "Glazer Masterwork", "Jonathan Glazer", "Auschwitz commandant villa soundscape"),
            CuratedListItem("Civil War", 2024, "Garland Road Movie", "Alex Garland", "Photojournalists in near-future war"),
            CuratedListItem("Monkey Man", 2024, "Action Debut", "Dev Patel", "Dev Patel revenge thriller")
        )
    )

    // ==========================================
    // 5. SIGHT & SOUND TOP 50 (BFI 2022)
    // ==========================================
    val SIGHT_AND_SOUND_50 = CuratedList(
        id = "sight_sound_50",
        title = "Sight & Sound: Greatest of All Time",
        subtitle = "BFI International Critics Poll",
        category = CuratedListCategory.CANON,
        emoji = "📽️",
        description = "The world's most prestigious international film canon, voted by over 1,600 film critics, scholars, and curators.",
        items = listOf(
            CuratedListItem("Jeanne Dielman, 23, quai du Commerce, 1080 Bruxelles", 1975, "#1 S&S", "Chantal Akerman"),
            CuratedListItem("Vertigo", 1958, "#2 S&S", "Alfred Hitchcock"),
            CuratedListItem("Citizen Kane", 1941, "#3 S&S", "Orson Welles"),
            CuratedListItem("Tokyo Story", 1953, "#4 S&S", "Yasujirô Ozu"),
            CuratedListItem("In the Mood for Love", 2000, "#5 S&S", "Wong Kar-wai"),
            CuratedListItem("2001: A Space Odyssey", 1968, "#6 S&S", "Stanley Kubrick"),
            CuratedListItem("Beau Travail", 1999, "#7 S&S", "Claire Denis"),
            CuratedListItem("Mulholland Drive", 2001, "#8 S&S", "David Lynch"),
            CuratedListItem("Man with a Movie Camera", 1929, "#9 S&S", "Dziga Vertov"),
            CuratedListItem("Singin' in the Rain", 1952, "#10 S&S", "Stanley Donen & Gene Kelly"),
            CuratedListItem("Sunrise: A Song of Two Humans", 1927, "#11 S&S", "F.W. Murnau"),
            CuratedListItem("The Godfather", 1972, "#12 S&S", "Francis Ford Coppola"),
            CuratedListItem("The Rules of the Game", 1939, "#13 S&S", "Jean Renoir"),
            CuratedListItem("Cléo from 5 to 7", 1962, "#14 S&S", "Agnès Varda"),
            CuratedListItem("The Searchers", 1956, "#15 S&S", "John Ford"),
            CuratedListItem("Meshes of the Afternoon", 1943, "#16 S&S", "Maya Deren"),
            CuratedListItem("Close-Up", 1990, "#17 S&S", "Abbas Kiarostami"),
            CuratedListItem("Persona", 1966, "#18 S&S", "Ingmar Bergman"),
            CuratedListItem("Apocalypse Now", 1979, "#19 S&S", "Francis Ford Coppola"),
            CuratedListItem("Seven Samurai", 1954, "#20 S&S", "Akira Kurosawa"),
            CuratedListItem("The Passion of Joan of Arc", 1928, "#21 S&S", "Carl Theodor Dreyer"),
            CuratedListItem("Late Spring", 1949, "#22 S&S", "Yasujirô Ozu"),
            CuratedListItem("Playtime", 1967, "#23 S&S", "Jacques Tati"),
            CuratedListItem("Do the Right Thing", 1989, "#24 S&S", "Spike Lee"),
            CuratedListItem("The Night of the Hunter", 1955, "#25 S&S", "Charles Laughton"),
            CuratedListItem("Rashomon", 1950, "#26 S&S", "Akira Kurosawa"),
            CuratedListItem("Stalker", 1979, "#27 S&S", "Andrei Tarkovsky"),
            CuratedListItem("Taxi Driver", 1976, "#28 S&S", "Martin Scorsese"),
            CuratedListItem("Bicycle Thieves", 1948, "#29 S&S", "Vittorio De Sica"),
            CuratedListItem("Breathless", 1960, "#30 S&S", "Jean-Luc Godard"),
            CuratedListItem("Mirror", 1975, "#31 S&S", "Andrei Tarkovsky"),
            CuratedListItem("Psycho", 1960, "#32 S&S", "Alfred Hitchcock"),
            CuratedListItem("8½", 1963, "#33 S&S", "Federico Fellini"),
            CuratedListItem("Portrait of a Lady on Fire", 2019, "#34 S&S", "Céline Sciamma"),
            CuratedListItem("Pather Panchali", 1955, "#35 S&S", "Satyajit Ray"),
            CuratedListItem("City Lights", 1931, "#36 S&S", "Charlie Chaplin"),
            CuratedListItem("M", 1931, "#37 S&S", "Fritz Lang"),
            CuratedListItem("Some Like It Hot", 1959, "#38 S&S", "Billy Wilder"),
            CuratedListItem("Rear Window", 1954, "#39 S&S", "Alfred Hitchcock"),
            CuratedListItem("Sunset Boulevard", 1950, "#40 S&S", "Billy Wilder"),
            CuratedListItem("Spirited Away", 2001, "#41 S&S", "Hayao Miyazaki"),
            CuratedListItem("Goodfellas", 1990, "#42 S&S", "Martin Scorsese"),
            CuratedListItem("Barry Lyndon", 1975, "#43 S&S", "Stanley Kubrick"),
            CuratedListItem("Paris, Texas", 1984, "#44 S&S", "Wim Wenders"),
            CuratedListItem("Blade Runner", 1982, "#45 S&S", "Ridley Scott")
        )
    )

    // ==========================================
    // 6. CRITERION CHANNEL ESSENTIALS
    // ==========================================
    val CRITERION_ESSENTIALS = CuratedList(
        id = "criterion_essentials",
        title = "Criterion Channel Essentials",
        subtitle = "World Cinema & Auteur Discoveries",
        category = CuratedListCategory.CANON,
        emoji = "📀",
        description = "Landmarks of international art cinema and Criterion restorations ready to stream or discover.",
        items = listOf(
            CuratedListItem("Seven Samurai", 1954, "Kurosawa", "Akira Kurosawa"),
            CuratedListItem("The 400 Blows", 1959, "French New Wave", "François Truffaut"),
            CuratedListItem("8½", 1963, "Fellini Classic", "Federico Fellini"),
            CuratedListItem("Breathless", 1960, "Godard", "Jean-Luc Godard"),
            CuratedListItem("Tokyo Story", 1953, "Ozu Masterwork", "Yasujirô Ozu"),
            CuratedListItem("Persona", 1966, "Bergman", "Ingmar Bergman"),
            CuratedListItem("Bicycle Thieves", 1948, "Neorealism", "Vittorio De Sica"),
            CuratedListItem("In the Mood for Love", 2000, "Wong Kar-wai", "Wong Kar-wai"),
            CuratedListItem("Rashomon", 1950, "Kurosawa", "Akira Kurosawa"),
            CuratedListItem("Cléo from 5 to 7", 1962, "Agnès Varda", "Agnès Varda"),
            CuratedListItem("Jeanne Dielman, 23, quai du Commerce, 1080 Bruxelles", 1975, "Akerman", "Chantal Akerman"),
            CuratedListItem("Playtime", 1967, "Jacques Tati", "Jacques Tati"),
            CuratedListItem("Stalker", 1979, "Tarkovsky", "Andrei Tarkovsky"),
            CuratedListItem("The Night of the Hunter", 1955, "Laughton", "Charles Laughton"),
            CuratedListItem("Paris, Texas", 1984, "Wim Wenders", "Wim Wenders"),
            CuratedListItem("La Haine", 1995, "Kassovitz", "Mathieu Kassovitz"),
            CuratedListItem("Chungking Express", 1994, "Wong Kar-wai", "Wong Kar-wai"),
            CuratedListItem("Yi Yi", 2000, "Edward Yang", "Edward Yang"),
            CuratedListItem("A Brighter Summer Day", 1991, "Edward Yang", "Edward Yang")
        )
    )

    // ==========================================
    // 7. BLANK CHECK (Griffin & David)
    // ==========================================
    val BLANK_CHECK = CuratedList(
        id = "blank_check",
        title = "Blank Check Director Highlights",
        subtitle = "Directors Given a Blank Check",
        category = CuratedListCategory.PODCASTS,
        emoji = "🎟️",
        description = "Auteur passion projects, huge budgets, and fascinating creative swings broken down miniseries by miniseries.",
        items = listOf(
            CuratedListItem("Tenet", 2020, "Nolan Series", "Christopher Nolan"),
            CuratedListItem("Interstellar", 2014, "Nolan Series", "Christopher Nolan"),
            CuratedListItem("The Prestige", 2006, "Nolan Series", "Christopher Nolan"),
            CuratedListItem("Dunkirk", 2017, "Nolan Series", "Christopher Nolan"),
            CuratedListItem("Avatar", 2009, "Cameron Series", "James Cameron"),
            CuratedListItem("Titanic", 1997, "Cameron Series", "James Cameron"),
            CuratedListItem("Spirited Away", 2001, "Miyazaki Series", "Hayao Miyazaki"),
            CuratedListItem("Princess Mononoke", 1997, "Miyazaki Series", "Hayao Miyazaki"),
            CuratedListItem("The Matrix", 1999, "Wachowskis Series", "Lana & Lilly Wachowski"),
            CuratedListItem("Speed Racer", 2008, "Wachowskis Series", "Lana & Lilly Wachowski"),
            CuratedListItem("Cloud Atlas", 2012, "Wachowskis Series", "Lana & Lilly Wachowski"),
            CuratedListItem("Babe: Pig in the City", 1998, "Miller Series", "George Miller"),
            CuratedListItem("Mad Max: Fury Road", 2015, "Miller Series", "George Miller"),
            CuratedListItem("Oppenheimer", 2023, "Nolan Series", "Christopher Nolan")
        )
    )

    // ==========================================
    // 8. IMDB TOP 50 CLASSICS
    // ==========================================
    val IMDB_TOP_50 = CuratedList(
        id = "imdb_top_50",
        title = "IMDb Top 50 Classics",
        subtitle = "All-Time Highest User Ratings",
        category = CuratedListCategory.CANON,
        emoji = "⭐",
        description = "The highest-rated films in cinema history according to millions of moviegoers worldwide.",
        items = listOf(
            CuratedListItem("The Shawshank Redemption", 1994, "#1 IMDb", "Frank Darabont"),
            CuratedListItem("The Godfather", 1972, "#2 IMDb", "Francis Ford Coppola"),
            CuratedListItem("The Dark Knight", 2008, "#3 IMDb", "Christopher Nolan"),
            CuratedListItem("The Godfather Part II", 1974, "#4 IMDb", "Francis Ford Coppola"),
            CuratedListItem("12 Angry Men", 1957, "#5 IMDb", "Sidney Lumet"),
            CuratedListItem("Schindler's List", 1993, "#6 IMDb", "Steven Spielberg"),
            CuratedListItem("The Lord of the Rings: The Return of the King", 2003, "#7 IMDb", "Peter Jackson"),
            CuratedListItem("Pulp Fiction", 1994, "#8 IMDb", "Quentin Tarantino"),
            CuratedListItem("The Good, the Bad and the Ugly", 1966, "#9 IMDb", "Sergio Leone"),
            CuratedListItem("Fight Club", 1999, "#10 IMDb", "David Fincher"),
            CuratedListItem("Forrest Gump", 1994, "#11 IMDb", "Robert Zemeckis"),
            CuratedListItem("Inception", 2010, "#12 IMDb", "Christopher Nolan"),
            CuratedListItem("The Lord of the Rings: The Fellowship of the Ring", 2001, "#13 IMDb", "Peter Jackson"),
            CuratedListItem("Star Wars: Episode V - The Empire Strikes Back", 1980, "#14 IMDb", "Irvin Kershner"),
            CuratedListItem("The Matrix", 1999, "#15 IMDb", "Lana & Lilly Wachowski"),
            CuratedListItem("Goodfellas", 1990, "#16 IMDb", "Martin Scorsese"),
            CuratedListItem("One Flew Over the Cuckoo's Nest", 1975, "#17 IMDb", "Miloš Forman"),
            CuratedListItem("Seven Samurai", 1954, "#18 IMDb", "Akira Kurosawa"),
            CuratedListItem("Se7en", 1995, "#19 IMDb", "David Fincher"),
            CuratedListItem("The Silence of the Lambs", 1991, "#20 IMDb", "Jonathan Demme"),
            CuratedListItem("It's a Wonderful Life", 1946, "#21 IMDb", "Frank Capra"),
            CuratedListItem("Life Is Beautiful", 1997, "#22 IMDb", "Roberto Benigni"),
            CuratedListItem("Saving Private Ryan", 1998, "#23 IMDb", "Steven Spielberg"),
            CuratedListItem("City of God", 2002, "#24 IMDb", "Fernando Meirelles"),
            CuratedListItem("Interstellar", 2014, "#25 IMDb", "Christopher Nolan"),
            CuratedListItem("Spirited Away", 2001, "#26 IMDb", "Hayao Miyazaki"),
            CuratedListItem("The Green Mile", 1999, "#27 IMDb", "Frank Darabont"),
            CuratedListItem("Star Wars", 1977, "#28 IMDb", "George Lucas"),
            CuratedListItem("Terminator 2: Judgment Day", 1991, "#29 IMDb", "James Cameron"),
            CuratedListItem("Back to the Future", 1985, "#30 IMDb", "Robert Zemeckis"),
            CuratedListItem("The Pianist", 2002, "#31 IMDb", "Roman Polanski"),
            CuratedListItem("Psycho", 1960, "#32 IMDb", "Alfred Hitchcock"),
            CuratedListItem("Parasite", 2019, "#33 IMDb", "Bong Joon-ho"),
            CuratedListItem("Gladiator", 2000, "#34 IMDb", "Ridley Scott"),
            CuratedListItem("The Lion King", 1994, "#35 IMDb", "Roger Allers & Rob Minkoff"),
            CuratedListItem("Léon: The Professional", 1994, "#36 IMDb", "Luc Besson"),
            CuratedListItem("The Departed", 2006, "#37 IMDb", "Martin Scorsese"),
            CuratedListItem("Whiplash", 2014, "#38 IMDb", "Damien Chazelle"),
            CuratedListItem("The Prestige", 2006, "#39 IMDb", "Christopher Nolan"),
            CuratedListItem("Alien", 1979, "#40 IMDb", "Ridley Scott")
        )
    )
}
