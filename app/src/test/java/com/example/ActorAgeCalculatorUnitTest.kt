package com.example

import com.example.data.util.ActorAgeCalculator
import com.example.data.util.CastMemberWithAge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActorAgeCalculatorUnitTest {

    @Test
    fun calculateAgeAtRelease_topGunTomCruise_returns23() {
        // Tom Cruise: born July 3, 1962. Top Gun released: May 16, 1986.
        val age = ActorAgeCalculator.calculateAgeAtRelease("1962-07-03", "1986-05-16")
        assertEquals(23, age)
    }

    @Test
    fun calculateAgeAtRelease_aliensSigourneyWeaver_returns36() {
        // Sigourney Weaver: born October 8, 1949. Aliens released: July 18, 1986.
        val age = ActorAgeCalculator.calculateAgeAtRelease("1949-10-08", "1986-07-18")
        assertEquals(36, age)
    }

    @Test
    fun calculateAgeAtRelease_matrixKeanuReeves_returns34() {
        // Keanu Reeves: born September 2, 1964. The Matrix released: March 31, 1999.
        val age = ActorAgeCalculator.calculateAgeAtRelease("1964-09-02", "1999-03-31")
        assertEquals(34, age)
    }

    @Test
    fun calculateAgeAtRelease_birthdayBoundary_evaluatesCorrectly() {
        // Birthday on July 10, 2000
        val birth = "2000-07-10"
        // Day before birthday in 2020 -> 19
        assertEquals(19, ActorAgeCalculator.calculateAgeAtRelease(birth, "2020-07-09"))
        // On birthday in 2020 -> 20
        assertEquals(20, ActorAgeCalculator.calculateAgeAtRelease(birth, "2020-07-10"))
        // Day after birthday in 2020 -> 20
        assertEquals(20, ActorAgeCalculator.calculateAgeAtRelease(birth, "2020-07-11"))
    }

    @Test
    fun calculateAgeAtRelease_yearOnlyFallback_calculatesApproximateAge() {
        val age = ActorAgeCalculator.calculateAgeAtRelease("1980-01-01", "2000")
        assertEquals(20, age)
    }

    @Test
    fun calculateAgeAtRelease_nullOrEmpty_returnsNull() {
        assertNull(ActorAgeCalculator.calculateAgeAtRelease(null, "1986-05-16"))
        assertNull(ActorAgeCalculator.calculateAgeAtRelease("1962-07-03", null))
        assertNull(ActorAgeCalculator.calculateAgeAtRelease("", ""))
    }

    @Test
    fun calculateDeathAge_calculatesCorrectAge() {
        // Sean Connery: born Aug 25, 1930, died Oct 31, 2020 -> age 90
        val deathAge = ActorAgeCalculator.calculateDeathAge("1930-08-25", "2020-10-31")
        assertEquals(90, deathAge)
    }

    @Test
    fun castMemberWithAge_labelsFormatCleanly() {
        val member = CastMemberWithAge(
            id = 500,
            name = "Tom Cruise",
            character = "Pete 'Maverick' Mitchell",
            ageAtRelease = 23
        )
        assertEquals("Age 23 at release", member.ageLabel)
        assertNull(member.deceasedLabel)

        val deceasedMember = CastMemberWithAge(
            id = 738,
            name = "Sean Connery",
            character = "James Bond",
            ageAtRelease = 32,
            birthday = "1930-08-25",
            deathday = "2020-10-31",
            deathAge = 90,
            isDeceased = true
        )
        assertEquals("Age 32 at release", deceasedMember.ageLabel)
        assertTrue(deceasedMember.deceasedLabel?.contains("Passed away in 2020 (age 90)") == true)
    }
}
