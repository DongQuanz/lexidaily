package com.lexidaily.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lexidaily.app.data.*
import com.lexidaily.app.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: LexiDatabase
    private lateinit var repo: LexiRepository
    private var day = LocalDate.parse("2026-10-16")
    private var clock = 1792108800000L
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(context, LexiDatabase::class.java).build()
        SeedImporter(context, db).importIfNeeded()
        repo = LexiRepository(db, { day }, { clock })
        repo.initialize()
        repo.savePreferences(repo.snapshot().profile.copy(onboarded = true, mode = "IELTS", dailyGoal = 10,
            priority = "Writing", topicMode = "FIXED", preferredTopic = "environment"))
    }
    @After fun close() { db.close() }
    @Test fun ieltsSeedImportsAllEntriesAndCatalogSources() = runBlocking {
        assertEquals(3000, db.dao().lexemes().size)
        assertEquals(3000, db.dao().senses().size)
        assertEquals(3000, db.dao().packLinks().count { it.packId == "ielts_core" })
        assertEquals(2, db.dao().contentVersion()!!.version)
        assertTrue(db.dao().sources().any { it.id == "thichhoc_dictionary" })
        val before = db.dao().lexemes().size
        SeedImporter(context, db).importIfNeeded()
        assertEquals(before, db.dao().lexemes().size)
    }
    private suspend fun complete(kind: String) {
        var s = repo.start(kind)
        while (!s.session.complete) {
            val q = s.current!!
            val w = repo.snapshot().words.first { it.sense.id == q.senseId }
            if (!q.answered) {
                if (q.kind == "EXPOSURE") { repo.reveal(s.session.id); repo.answer(s.session.id, "", Rating.GOOD, 3000) }
                else repo.answer(s.session.id, LexiRepository.expected(w, q.kind), null, 3000)
            }
            s = repo.next(s.session.id)
        }
    }
    @Test fun dailyJourneyPersistsEarnsStreakOnceAndBalancesXp() = runBlocking {
        complete("NEW")
        val before = repo.snapshot()
        assertTrue(GameRules.challengeUnlocked(before.plan))
        assertFalse(before.plan.journeyRewarded)
        assertEquals(1, before.profile.streak)
        assertEquals(before.profile.lifetimeLearningXp, before.plan.xp)
        complete("CHALLENGE")
        val after = repo.snapshot()
        assertTrue(after.plan.perfect)
        assertTrue(after.plan.journeyRewarded)
        assertTrue(after.quests.all { it.rewarded })
        assertEquals(1, after.profile.streak)
        assertEquals(1, after.profile.perfectDays)
        assertEquals(after.profile.lifetimeLearningXp, after.plan.xp)
        val xp = after.profile.lifetimeLearningXp
        complete("NEW"); complete("CHALLENGE"); repo.initialize()
        assertEquals(xp, repo.snapshot().profile.lifetimeLearningXp)
        assertEquals(1, repo.snapshot().profile.streak)
        assertEquals(10, repo.snapshot().words.count { it.progress.seen })
    }
    @Test fun processResumePreservesRevealDraftAndSubmittedAnswer() = runBlocking {
        val s = repo.start("NEW")
        repo.reveal(s.session.id)
        repo.draft(s.session.id, "saved input")
        val reopened = LexiRepository(db, { day }, { clock })
        val resumed = reopened.resume()!!
        assertTrue(resumed.current!!.revealed)
        assertEquals("saved input", resumed.current!!.draft)
        reopened.answer(s.session.id, "", Rating.GOOD, 5000)
        assertTrue(reopened.resume()!!.current!!.answered)
        val xp = reopened.snapshot().profile.lifetimeLearningXp
        reopened.answer(s.session.id, "", Rating.EASY, 1000)
        assertEquals(xp, reopened.snapshot().profile.lifetimeLearningXp)
    }
    @Test fun manualReviewDoesNotFarmXpAndContentUpdatesPreserveProgress() = runBlocking {
        complete("NEW")
        val w = repo.snapshot().words.first { it.progress.seen }
        val xp = repo.snapshot().profile.lifetimeLearningXp
        val practice = repo.start("PRACTICE", w.sense.id)
        repo.reveal(practice.session.id)
        repo.answer(practice.session.id, "", Rating.EASY, 2000)
        repo.next(practice.session.id)
        assertEquals(xp, repo.snapshot().profile.lifetimeLearningXp)
        val progress = db.dao().progresses()
        db.dao().put(ContentVersionEntity(version = 0))
        SeedImporter(context, db).importIfNeeded()
        assertEquals(progress.toSet(), db.dao().progresses().toSet())
        assertEquals(xp, repo.snapshot().profile.lifetimeLearningXp)
    }
    @Test fun rolloverCreatesFreshPlanUsesDueReviewsAndGraceAutomatically() = runBlocking {
        complete("NEW")
        db.dao().put(repo.snapshot().profile.copy(graceTokens = 1))
        val old = repo.snapshot().plan
        day = day.plusDays(2); clock += 2 * 86400000L
        repo.initialize()
        val fresh = repo.snapshot()
        assertNotEquals(old.day, fresh.plan.day)
        assertEquals(0, fresh.profile.graceTokens)
        assertEquals(1, fresh.profile.streak)
        assertTrue(fresh.words.filter { it.sense.id in fresh.plan.newIds.ids() }.none { it.progress.seen })
        assertTrue(db.dao().plan(day.minusDays(1).toString())!!.graceUsed)
        day = day.plusDays(1); clock += 86400000L
        repo.initialize()
        assertEquals(0, repo.snapshot().profile.streak)
        assertTrue(repo.snapshot().plan.reviewIds.ids().isNotEmpty())
    }
    @Test fun challengeCannotStartBelowThreshold() = runBlocking {
        try { repo.start("CHALLENGE"); fail("Challenge must be locked") }
        catch (e: IllegalStateException) { assertTrue(e.message!!.contains("70%")) }
    }
    @Test fun reviewOnlyDayCompletesAndChallengeStillHasFiveQuestions() = runBlocking {
        repo.savePreferences(repo.snapshot().profile.copy(mode = "GENERAL", minCefr = "B1"))
        val topicWords = repo.snapshot().words.filter { "environment" in it.topics }
        // Exhaust the selected topic instead of assuming that the seed has no C2 words.
        topicWords.forEach { db.dao().put(it.progress.copy(seen = true, stage = 1, dueAt = clock + 10 * 86400000L)) }
        topicWords.take(3).forEach { db.dao().put(it.progress.copy(seen = true, stage = 1, dueAt = clock - 1)) }
        day = day.plusDays(1); clock += 86400000L
        repo.initialize()
        assertTrue(repo.snapshot().plan.newIds.isEmpty())
        complete("REVIEW")
        assertEquals(1, repo.snapshot().profile.streak)
        assertEquals(5, repo.start("CHALLENGE").questions.size)
        complete("CHALLENGE")
        assertTrue(repo.snapshot().plan.perfect)
    }
    @Test fun onboardingSmallTopicCreatesThreeAchievableQuests() = runBlocking {
        repo.savePreferences(repo.snapshot().profile.copy(topicMode = "FIXED", preferredTopic = "ageing"))
        assertEquals(3, repo.snapshot().plan.newIds.ids().size)
        assertEquals(3, repo.snapshot().quests.size)
        complete("NEW")
        complete("CHALLENGE")
        assertTrue(repo.snapshot().quests.all { it.rewarded })
        assertEquals(5, repo.start("CHALLENGE").questions.size)
    }
}
