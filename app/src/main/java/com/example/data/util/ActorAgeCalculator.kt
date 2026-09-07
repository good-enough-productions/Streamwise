package com.example.data.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CastMemberWithAge(
    val id: Int,
    val name: String,
    val character: String,
    val profilePath: String? = null,
    val birthday: String? = null,
    val deathday: String? = null,
    val ageAtRelease: Int? = null,
    val isDeceased: Boolean = false,
    val deathAge: Int? = null
) {
    val ageLabel: String
        get() = when {
            ageAtRelease != null -> "Age $ageAtRelease at release"
            else -> "Age unknown"
        }

    val deceasedLabel: String?
        get() = if (isDeceased && deathday != null) {
            val year = deathday.take(4)
            if (deathAge != null) "Passed away in $year (age $deathAge)" else "Passed away in $year"
        } else null
}

object ActorAgeCalculator {
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Calculates the actor's age at the time of a movie's release.
     * Takes ISO YYYY-MM-DD or year-only format.
     */
    fun calculateAgeAtRelease(birthday: String?, releaseDate: String?): Int? {
        if (birthday.isNullOrBlank() || releaseDate.isNullOrBlank()) return null
        return calculateAge(birthday.trim(), releaseDate.trim())
    }

    /**
     * Calculates how old the actor was when they passed away.
     */
    fun calculateDeathAge(birthday: String?, deathday: String?): Int? {
        if (birthday.isNullOrBlank() || deathday.isNullOrBlank()) return null
        return calculateAge(birthday.trim(), deathday.trim())
    }

    private fun calculateAge(birthDateStr: String, targetDateStr: String): Int? {
        try {
            val birthCal = parseDateToCalendar(birthDateStr) ?: return null
            val targetCal = parseDateToCalendar(targetDateStr) ?: return null

            var age = targetCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
            val monthDiff = targetCal.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH)
            val dayDiff = targetCal.get(Calendar.DAY_OF_MONTH) - birthCal.get(Calendar.DAY_OF_MONTH)

            if (monthDiff < 0 || (monthDiff == 0 && dayDiff < 0)) {
                age--
            }

            return if (age in 0..130) age else null
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseDateToCalendar(dateStr: String): Calendar? {
        val clean = dateStr.trim()
        if (clean.length >= 10 && clean[4] == '-' && clean[7] == '-') {
            val parsed = DATE_FORMAT.parse(clean.substring(0, 10)) ?: return null
            val cal = Calendar.getInstance()
            cal.time = parsed
            return cal
        }
        // Fallback if only 4-digit year is provided (e.g. "1986")
        if (clean.length == 4 && clean.all { it.isDigit() }) {
            val cal = Calendar.getInstance()
            cal.set(clean.toInt(), Calendar.JULY, 1) // mid-year estimate
            return cal
        }
        return null
    }
}
