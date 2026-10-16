package com.lexidaily.app

import com.lexidaily.app.data.*
import com.lexidaily.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class LearningEngineTest {
    private val fresh = UserWordProgress("inevitable_adjective_01")
    @Test fun srsSchedulesAllFourRatingsAndCapsIntervals() {
        assertEquals(listOf(10L, 1440L, 4320L, 10080L), Rating.entries.map { LearningEngine.interval(fresh, it) })
        val later = LearningEngine.review(fresh, "EXPOSURE", Rating.EASY, 1000, 3000)
        assertEquals(1000 + 10080 * 60000L, later.dueAt)
        assertEquals(525600L, LearningEngine.interval(fresh.copy(intervalMinutes = 500000), Rating.EASY))
    }
    @Test fun easyFlashcardsCannotCreateIeltsMastery() {
        var p = fresh
        repeat(30) { p = LearningEngine.review(p, "FLASHCARD", Rating.EASY, it * 1000000L, 3000) }
        assertEquals(0f, p.productionStrength, 0f)
        assertEquals(0f, p.contextStrength, 0f)
        assertNotEquals(MemoryState.MASTERED, LearningEngine.state(p, true))
        repeat(5) { p = LearningEngine.review(p, "CLOZE", Rating.GOOD, 900000000, 5000) }
        assertEquals(MemoryState.MASTERED, LearningEngine.state(p, true))
    }
    @Test fun questionProgressionMovesTowardsProductionAndBackOnErrors() {
        var p = LearningEngine.review(fresh, "EXPOSURE", Rating.GOOD, 0, 1000)
        assertEquals("MEANING", LearningEngine.questionKind(p))
        p = LearningEngine.review(p, "MEANING", Rating.GOOD, 1000, 2000)
        assertEquals("DEFINITION", LearningEngine.questionKind(p))
        p = LearningEngine.review(p, "DEFINITION", Rating.GOOD, 2000, 2000)
        assertEquals("TYPING", LearningEngine.questionKind(p))
        p = LearningEngine.review(p, "TYPING", Rating.AGAIN, 3000, 2000)
        assertTrue(p.stage <= 2)
        assertEquals(1, p.lapseCount)
    }
    @Test fun troubleDeckUsesRecentErrorsAndProductionGap() {
        assertFalse(LearningEngine.trouble(fresh))
        assertTrue(LearningEngine.trouble(fresh.copy(seen = true, lapseCount = 3)))
        assertTrue(LearningEngine.trouble(fresh.copy(seen = true, recentResults = "001")))
        assertTrue(LearningEngine.trouble(fresh.copy(seen = true, recognitionStrength = .9f, productionStrength = .1f)))
    }
    @Test fun graceBridgesMultipleMissedDaysAndResetsWhenInsufficient() {
        val p = UserProfile(streak = 12, lastStreakDay = "2026-10-12", graceTokens = 2)
        val bridged = GameRules.bridge(p, LocalDate.parse("2026-10-15"))
        assertEquals(12, bridged.streak); assertEquals(0, bridged.graceTokens)
        assertEquals("2026-10-14", bridged.lastStreakDay)
        assertEquals(0, GameRules.bridge(p, LocalDate.parse("2026-10-16")).streak)
        assertEquals(p, GameRules.bridge(p, LocalDate.parse("2026-10-13")))
    }
    @Test fun challengeUnlocksAtSeventyPercentAndMinimumAtHalf() {
        val ids = (1..10).map { "word$it" }
        val p = DailyPlanEntity("2026-10-16", "technology", 10, "review", ids.encoded(), reviewDone = "review", newDone = ids.take(6).encoded())
        assertFalse(GameRules.challengeUnlocked(p)); assertTrue(GameRules.minimum(p))
        assertTrue(GameRules.challengeUnlocked(p.copy(newDone = ids.take(7).encoded())))
        assertFalse(GameRules.minimum(p.copy(reviewDone = "")))
    }
    @Test fun selectorHonoursModeTopicSkillAndExcludesKnownWords() {
        fun word(id: String, topic: String, level: String, academic: Float, seen: Boolean = false) = Word(
            LexemeEntity(id, id, academicScore = academic, ieltsRelevanceScore = academic, productivePriority = academic),
            SenseEntity("${id}_01", id, "noun", "A $id", id, level, exampleEn = "An $id is useful."),
            progress = UserWordProgress("${id}_01", seen = seen), topics = listOf(topic),
            collocations = listOf("test $id"), sources = listOf(SourceOccurrenceEntity("o$id", id, "demo", skill = "Writing")))
        val p = UserProfile(mode = "IELTS", targetBand = "7.0", priority = "Writing", topicMode = "FIXED", preferredTopic = "environment")
        val words = listOf(word("sustainable", "environment", "B2", .9f), word("obscure", "environment", "C2", .2f),
            word("learned", "environment", "B2", 1f, true), word("computer", "technology", "B2", 1f))
        val selected = DailySelector.select(words, p, "environment", emptyList(), 10)
        assertEquals(listOf("sustainable", "obscure"), selected.map { it.lexeme.id })
    }
    @Test fun reviewOnlyDayCanKeepHabitWithoutRewardingAnEmptyDay() {
        val empty = DailyPlanEntity("2026-10-16", "technology", 20, "", "")
        assertFalse(GameRules.minimum(empty))
        assertFalse(GameRules.challengeUnlocked(empty))
        val reviews = empty.copy(reviewIds = "word", reviewDone = "word")
        assertTrue(GameRules.minimum(reviews))
        assertTrue(GameRules.challengeUnlocked(reviews))
    }
    @Test fun positionsAreStableBoundedAndAnswersAreNormalized() {
        assertEquals(LearningEngine.position("mitigate"), LearningEngine.position("mitigate"))
        (1..1000).forEach { val (x, y) = LearningEngine.position("word$it"); assertTrue(x in 0.08f..0.92f && y in 0.08f..0.92f) }
        assertTrue(LearningEngine.answerMatches("  SUSTAINABLE  ", "sustainable"))
        assertFalse(LearningEngine.answerMatches("sustain", "sustainable"))
    }
    @Test fun incompleteEnrichmentNeverProducesAnEmptyOrUnmaskedCloze() {
        val word = Word(LexemeEntity("resilience", "resilience"),
            SenseEntity("resilience_noun_01", "resilience", "noun", "Ability to recover.", "khả năng phục hồi", "C1", exampleEn = ""))
        assertEquals("TYPING", LearningEngine.supportedQuestionKind(word, "CLOZE"))
        assertEquals("TYPING", LearningEngine.supportedQuestionKind(word, "COLLOCATION"))
        assertEquals("TYPING", LearningEngine.supportedQuestionKind(word.copy(sense = word.sense.copy(exampleEn = "The city is resilient.")), "CLOZE"))
        val contextual = word.copy(sense = word.sense.copy(exampleEn = "The city needs greater resilience."))
        assertEquals("CLOZE", LearningEngine.supportedQuestionKind(contextual, "CLOZE"))
        assertEquals("CLOZE", LearningEngine.supportedQuestionKind(contextual, "COLLOCATION"))
        assertEquals("COLLOCATION", LearningEngine.supportedQuestionKind(word.copy(collocations = listOf("build resilience")), "COLLOCATION"))
    }
}
