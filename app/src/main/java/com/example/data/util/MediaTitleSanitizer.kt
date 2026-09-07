package com.example.data.util

object MediaTitleSanitizer {

    private val NON_MOVIE_PATTERNS = listOf(
        Regex("""\bmailbag\b""", RegexOption.IGNORE_CASE),
        Regex("""\bdraft\b""", RegexOption.IGNORE_CASE),
        Regex("""\bauction\b""", RegexOption.IGNORE_CASE),
        Regex("""\bhall of fame\b""", RegexOption.IGNORE_CASE),
        Regex("""\bpredictions\b""", RegexOption.IGNORE_CASE),
        Regex("""\brankings\b""", RegexOption.IGNORE_CASE),
        Regex("""\bselection show\b""", RegexOption.IGNORE_CASE),
        Regex("""\bvoicemailbag\b""", RegexOption.IGNORE_CASE),
        Regex("""\bcongratulations\b""", RegexOption.IGNORE_CASE),
        Regex("""\bwelcome to the rewatchables\b""", RegexOption.IGNORE_CASE),
        Regex("""\bask sean anything\b""", RegexOption.IGNORE_CASE),
        Regex("""\bphysical media high council\b""", RegexOption.IGNORE_CASE),
        Regex("""\bwe went to cannes\b""", RegexOption.IGNORE_CASE),
        Regex("""\bholiday special\b""", RegexOption.IGNORE_CASE),
        Regex("""\bcannes\b""", RegexOption.IGNORE_CASE),
        Regex("""\btelluride\b""", RegexOption.IGNORE_CASE),
        Regex("""\bsundance\b""", RegexOption.IGNORE_CASE),
        Regex("""\boscars?\b""", RegexOption.IGNORE_CASE),
        Regex("""\bgolden globes?\b""", RegexOption.IGNORE_CASE),
        Regex("""\bunder 35\b""", RegexOption.IGNORE_CASE),
        Regex("""\bover 35\b""", RegexOption.IGNORE_CASE),
        Regex("""\bmegadraft\b""", RegexOption.IGNORE_CASE),
        Regex("""\bmega-movie re-draft\b""", RegexOption.IGNORE_CASE),
        Regex("""\bmega-draft\b""", RegexOption.IGNORE_CASE),
        Regex("""\bre-draft\b""", RegexOption.IGNORE_CASE),
        Regex("""\bmount rushmore\b""", RegexOption.IGNORE_CASE),
        Regex("""\badvice hour\b""", RegexOption.IGNORE_CASE),
        Regex("""\bannouncement\b""", RegexOption.IGNORE_CASE),
        Regex("""\bbonus episode\b""", RegexOption.IGNORE_CASE),
        Regex("""\blive from\b""", RegexOption.IGNORE_CASE),
        Regex("""\bminisode\b""", RegexOption.IGNORE_CASE),
        Regex("""\blast looks\b""", RegexOption.IGNORE_CASE),
        Regex("""\bmatinee monday\b""", RegexOption.IGNORE_CASE),
        Regex("""\bselection show special\b""", RegexOption.IGNORE_CASE),
        Regex("""\bthe loss of david lynch\b""", RegexOption.IGNORE_CASE),
        Regex("""\bshould i see it in a movie theater\b""", RegexOption.IGNORE_CASE),
        Regex("""\bthe gen z movie focus group\b""", RegexOption.IGNORE_CASE),
        Regex("""\bthe king of physical media\b""", RegexOption.IGNORE_CASE),
        Regex("""\ban everything-but-movies mailbag\b""", RegexOption.IGNORE_CASE),
        Regex("""\bthe hollywood hack\b""", RegexOption.IGNORE_CASE),
        Regex("""^(?:the\s+)?rewatchables$""", RegexOption.IGNORE_CASE),
        Regex("""^(?:the\s+)?big picture$""", RegexOption.IGNORE_CASE),
        Regex("""^unspooled$""", RegexOption.IGNORE_CASE),
        Regex("""^blank check$""", RegexOption.IGNORE_CASE),
        Regex("""^how did this get made\??$""", RegexOption.IGNORE_CASE),
        Regex("""^what went wrong\??$""", RegexOption.IGNORE_CASE)
    )

    fun isNonMovieEpisode(title: String): Boolean {
        if (title.isBlank()) return true
        return NON_MOVIE_PATTERNS.any { it.containsMatchIn(title) }
    }

    fun cleanCandidateTitle(raw: String): String {
        var t = raw.trim()

        // Strip leading prefixes e.g. "Episode 123 - ", "Ep. 45: "
        t = t.replace(Regex("""^(?:Rerun|Re-Release|Bonus|REVIEW|EP\.?\s*\d+|Episode\s*\d+|Last Looks|Featurette|Mini-Episode|Minisode|Deep Dive|Matinee Monday)\s*[-:]\s*""", RegexOption.IGNORE_CASE), "")

        // Strip "The Re-" prefix e.g. "The Re-Heat" -> "Heat", "The Re-Departed" -> "Departed"
        if (t.startsWith("The Re-", ignoreCase = true)) {
            t = t.substring(7).trim()
        }

        // Strip "(Re)" e.g. "A Few Good (Re)Men" -> "A Few Good Men"
        t = t.replace(Regex("""\(Re\)""", RegexOption.IGNORE_CASE), "")

        // Extract quoted title if present: "..." or '...' or “...” or ‘...’
        val quoteMatch = Regex("""[“"\'‘]([^"”\'’]+)[”"\'’]""").find(t)
        if (quoteMatch != null) {
            val cand = quoteMatch.groupValues[1].trim()
            if (cand.length >= 2 && !isNonMovieEpisode(cand)) {
                return cand
            }
        }

        // Strip Part indicators in parens: "(Part 1 + 2)", "(Part 1)"
        t = t.replace(Regex("""\s*\(Part\s*\d+.*?\)|:\s*Part\s*\d+.*$""", RegexOption.IGNORE_CASE), "")

        // Strip guest attributions in parens: "(with ...)"
        t = t.replace(Regex("""\s*\((?:with|w\/|featuring|feat\.)\s+[^)]+\)""", RegexOption.IGNORE_CASE), "")

        // Strip everything after "Plus:", "w/", or "With [Guest Name]"
        val splitParts = t.split(Regex("""\s+(?:Plus:|w\/|With\s+[A-Z])""", RegexOption.IGNORE_CASE))
        if (splitParts.isNotEmpty() && splitParts[0].isNotBlank()) {
            t = splitParts[0]
        }

        // Strip trailing punctuation
        return t.trim().trimEnd('-', ':', ',', '.', '|').trim()
    }
}
