package com.lexidaily.app.data

import androidx.room.withTransaction
import com.lexidaily.app.domain.*
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

data class AppSnapshot(
    val profile: UserProfile, val plan: DailyPlanEntity, val words: List<Word>,
    val topics: List<TopicEntity>, val sources: List<SourcePackEntity>,
    val quests: List<QuestEntity>, val plans: List<DailyPlanEntity>,
    val history: List<ReviewHistoryEntity>, val achievements: List<AchievementEntity>,
    val checkpoints: List<CheckpointEntity>, val packs: List<CuratedPackEntity>, val packLinks: List<PackWordEntity>
)
data class LearningSession(val session: SessionEntity, val questions: List<SessionQuestionEntity>) {
    val current get() = questions.getOrNull(session.cursor)
}

class LexiRepository(val db: LexiDatabase, private val today: () -> LocalDate = { LocalDate.now() },
    private val now: () -> Long = { System.currentTimeMillis() }) {
    private val dao = db.dao()
    suspend fun initialize() {
        db.withTransaction {
            if (dao.profile() == null) dao.put(UserProfile())
            val p = dao.profile()!!
            val bridged = GameRules.bridge(p, today())
            if (bridged != p && p.lastStreakDay.isNotEmpty()) {
                val used = p.graceTokens - bridged.graceTokens
                repeat(used) { i ->
                    val day = LocalDate.parse(p.lastStreakDay).plusDays(i + 1L).toString()
                    val missedPlan = dao.plan(day)
                    dao.put(missedPlan?.copy(graceUsed = true) ?: DailyPlanEntity(day, "", 0, "", "", graceUsed = true))
                }
                dao.put(bridged)
            }
            ensurePlan()
        }
    }
    private suspend fun words(): List<Word> {
        val lexemes = dao.lexemes().associateBy { it.id }
        val progress = dao.progresses().associateBy { it.senseId }
        val pronunciations = dao.pronunciations().associateBy { it.senseId }
        val topics = dao.topicLinks().groupBy { it.lexemeId }
        val collocations = dao.collocations().groupBy { it.senseId }
        val families = dao.families().groupBy { it.rootLexemeId }
        val paraphrases = dao.paraphrases().groupBy { it.senseId }
        val occurrences = dao.occurrences().groupBy { it.lexemeId }
        return dao.senses().mapNotNull { sense -> lexemes[sense.lexemeId]?.let { lexeme ->
            Word(lexeme, sense, pronunciations[sense.id], progress[sense.id] ?: UserWordProgress(sense.id),
                topics[lexeme.id].orEmpty().map { it.topicId }, collocations[sense.id].orEmpty().map { it.text },
                families[lexeme.id].orEmpty().mapNotNull { lexemes[it.relatedLexemeId]?.lemma },
                paraphrases[sense.id].orEmpty().map { it.text }, occurrences[lexeme.id].orEmpty())
        } }
    }
    private suspend fun ensurePlan(): DailyPlanEntity {
        val day = today().toString()
        dao.plan(day)?.let { return it }
        val p = dao.profile() ?: UserProfile()
        val all = words()
        val availableTopics = dao.topics().filter { t -> all.any { t.id in it.topics && !it.progress.seen } }.sortedBy { it.id }
        val topic = if (p.topicMode == "FIXED") p.preferredTopic else availableTopics.getOrNull(Math.floorMod(today().toEpochDay().toInt(), availableTopics.size.coerceAtLeast(1)))?.id ?: p.preferredTopic
        val due = all.filter { it.progress.seen && it.progress.dueAt <= now() }.sortedBy { it.progress.dueAt }
        val selected = DailySelector.select(all, p, topic, dao.sources(), p.dailyGoal)
        val plan = DailyPlanEntity(day, topic, p.dailyGoal, due.map { it.sense.id }.encoded(), selected.map { it.sense.id }.encoded())
        dao.put(plan)
        createQuests(day, selected, due, all)
        return plan
    }
    private suspend fun createQuests(day: String, selected: List<Word>, due: List<Word>, all: List<Word>) {
        dao.deleteQuests(day)
        val options = mutableListOf<QuestEntity>()
        if (selected.isNotEmpty()) options += QuestEntity("$day:explorer", day, "EXPLORER", "Học ${selected.size} từ mới", selected.size)
        if (due.isNotEmpty()) options += QuestEntity("$day:recall", day, "RECALL", "Ôn hết ${due.size} từ đến hạn", due.size)
        val troubled = all.filter { LearningEngine.trouble(it.progress) }
        val pool = (selected + due + troubled).distinctBy { it.sense.id }
        val extras = mutableListOf<QuestEntity>()
        if (pool.isNotEmpty()) extras += QuestEntity("$day:precision", day, "PRECISION", "Đạt 80% trong thử thách", 80)
        val recognition = pool.count { LearningEngine.questionKind(it.progress) in listOf("EXPOSURE", "MEANING", "DEFINITION") }.coerceAtMost(5)
        if (recognition > 0) extras += QuestEntity("$day:recognition", day, "RECOGNITION", "Nhận biết đúng $recognition từ", recognition)
        val production = pool.count { LearningEngine.questionKind(it.progress) in listOf("TYPING", "CLOZE") || LearningEngine.trouble(it.progress) }.coerceAtMost(5)
        if (production > 0) extras += QuestEntity("$day:production", day, "PRODUCTION", "Tự nhớ đúng $production từ", production)
        if (troubled.isNotEmpty()) extras += QuestEntity("$day:comeback", day, "COMEBACK", "Khôi phục ${troubled.size.coerceAtMost(3)} từ khó", troubled.size.coerceAtMost(3))
        val academic = selected.count { it.lexeme.academicScore >= .6f }.coerceAtMost(5)
        if (academic > 0) extras += QuestEntity("$day:academic", day, "ACADEMIC", "Học $academic từ học thuật", academic)
        if (extras.isNotEmpty()) {
            val offset = Math.floorMod(today().toEpochDay().toInt(), extras.size)
            options += (extras.drop(offset) + extras.take(offset)).take(3 - options.size)
        }
        options.forEach { dao.put(it) }
    }
    suspend fun snapshot(): AppSnapshot {
        val plan = ensurePlan()
        return AppSnapshot(dao.profile()!!, plan, words(), dao.topics(), dao.sources(), dao.quests(plan.day),
            dao.plans(), dao.history(), dao.achievements(), dao.checkpoints(), dao.packs(), dao.packLinks())
    }
    suspend fun savePreferences(profile: UserProfile) = db.withTransaction {
        val old = dao.profile()!!
        dao.put(profile.copy(lifetimeLearningXp = old.lifetimeLearningXp, streak = old.streak,
            longestStreak = old.longestStreak, lastStreakDay = old.lastStreakDay,
            graceTokens = old.graceTokens, perfectDays = old.perfectDays))
        val plan = dao.plan(today().toString())
        // An untouched plan may be rebuilt after onboarding or a preference change.
        if (plan != null && plan.newDone.isEmpty() && plan.reviewDone.isEmpty() && dao.activeSession(plan.day) == null) {
            val all = words()
            val topic = if (profile.topicMode == "FIXED") profile.preferredTopic else plan.topicId
            val selected = DailySelector.select(all, profile, topic, dao.sources(), profile.dailyGoal)
            dao.put(plan.copy(topicId = topic, goal = profile.dailyGoal, newIds = selected.map { it.sense.id }.encoded()))
            createQuests(plan.day, selected, all.filter { it.sense.id in plan.reviewIds.ids() }, all)
        }
    }
    suspend fun bookmark(id: String) = db.withTransaction {
        val old = dao.progresses().find { it.senseId == id } ?: UserWordProgress(id)
        dao.put(old.copy(bookmarked = !old.bookmarked))
    }
    suspend fun resume(): LearningSession? = dao.activeSession(today().toString())?.let { loadSession(it.id) }
    suspend fun loadSession(id: String): LearningSession? = dao.session(id)?.let { LearningSession(it, dao.questions(id)) }

    suspend fun start(kind: String, scope: String = ""): LearningSession = db.withTransaction {
        val day = today().toString()
        val plan = ensurePlan()
        val id = "$day:$kind:$scope"
        loadSession(id)?.let { return@withTransaction it }
        val all = words()
        val byId = all.associateBy { it.sense.id }
        val pool: List<Pair<Word, String>> = when (kind) {
            "REVIEW" -> plan.reviewIds.ids().filter { it !in plan.reviewDone.ids() }.mapNotNull { byId[it] }.map { it to LearningEngine.questionKind(it.progress) }
            "NEW" -> plan.newIds.ids().filter { it !in plan.newDone.ids() }.mapNotNull { byId[it] }.map { it to "EXPOSURE" }
            "CHALLENGE" -> {
                check(GameRules.challengeUnlocked(plan)) { "Học ít nhất 70% từ mới để mở thử thách." }
                val ids = (plan.newDone.ids() + all.filter { LearningEngine.trouble(it.progress) }.map { it.sense.id } + plan.reviewIds.ids()).distinct()
                val chosen = ids.mapNotNull { byId[it] }.sortedByDescending { it.progress.userDifficultyScore }.take(10)
                (0 until if (chosen.isEmpty()) 0 else chosen.size.coerceAtLeast(5)).map { i ->
                    val w = chosen[i % chosen.size]
                    w to when {
                    i >= chosen.size -> if (i % 2 == 0) "CLOZE" else "DEFINITION"
                    w.progress.recognitionStrength - w.progress.productionStrength >= .3f -> if (i % 2 == 0) "TYPING" else "CLOZE"
                    w.progress.stage <= 1 -> "MEANING"
                    else -> LearningEngine.questionKind(w.progress)
                } }
            }
            "REPAIR" -> all.filter { LearningEngine.trouble(it.progress) }.sortedByDescending { it.progress.userDifficultyScore }.take(10)
                .flatMap { w -> listOf(w to "MEANING", w to "CLOZE", w to "TYPING") }
            "CHECKPOINT" -> {
                val cp = dao.checkpoints().find { it.id == scope } ?: error("Checkpoint chưa mở.")
                val available = all.filter { (cp.topicId.isEmpty() || cp.topicId in it.topics) && it.progress.seen }
                if (available.isEmpty()) emptyList() else (0 until maxOf(15, available.size.coerceAtMost(20))).map { i ->
                    available[i % available.size] to listOf("MEANING", "DEFINITION", "CLOZE", "COLLOCATION", "TYPING")[i % 5] }
            }
            else -> all.filter { it.sense.id == scope }.map { it to "FLASHCARD" }
        }
        val session = SessionEntity(id, day, kind, complete = pool.isEmpty(), scope = scope, startedAt = now())
        dao.put(session)
        pool.forEachIndexed { index, (word, qkind) ->
            val type = LearningEngine.supportedQuestionKind(word, qkind)
            val correct = expected(word, type)
            val distractors = all.filter { it.sense.id != word.sense.id }.map { expected(it, type) }.distinct().filter { it != correct }.take(3)
            val options = (distractors + correct).sortedBy { (it + session.id + index).hashCode() }.encoded()
            dao.put(SessionQuestionEntity("$id:$index", id, index, word.sense.id, type, options))
        }
        loadSession(id)!!
    }
    suspend fun draft(sessionId: String, value: String) = db.withTransaction {
        val q = loadSession(sessionId)?.current ?: return@withTransaction
        if (!q.answered) dao.put(q.copy(draft = value))
    }
    suspend fun reveal(sessionId: String) = db.withTransaction {
        val q = loadSession(sessionId)?.current ?: return@withTransaction
        dao.put(q.copy(revealed = true))
    }
    suspend fun answer(sessionId: String, submitted: String, rating: Rating?, responseMs: Long): LearningSession = db.withTransaction {
        val learning = loadSession(sessionId) ?: error("Phiên học không tồn tại.")
        check(learning.session.day == today().toString()) { "Ngày mới đã bắt đầu. Hãy mở hành trình hôm nay." }
        val q = learning.current ?: return@withTransaction learning
        if (q.answered) return@withTransaction learning
        val word = words().first { it.sense.id == q.senseId }
        if (q.kind in listOf("EXPOSURE", "FLASHCARD")) check(q.revealed) { "Hãy xem nghĩa trước khi đánh giá." }
        val correct = if (rating != null) rating != Rating.AGAIN else LearningEngine.answerMatches(submitted, expected(word, q.kind))
        val effectiveRating = rating ?: if (correct) Rating.GOOD else Rating.AGAIN
        var updated = LearningEngine.review(word.progress, q.kind, effectiveRating, now(), responseMs)
        var plan = ensurePlan()
        val planned = learning.session.kind in listOf("NEW", "REVIEW", "CHALLENGE", "REPAIR", "CHECKPOINT")
        val due = word.progress.seen && word.progress.dueAt <= now()
        var earned = 0
        if (!word.progress.seen && q.kind == "EXPOSURE") earned += award("first:${q.senseId}", plan.day, 2)
        if (correct && due) earned += award("due:${q.senseId}:${word.progress.dueAt}", plan.day, 3)
        if (correct && q.kind in listOf("TYPING", "CLOZE") && (planned || due))
            earned += award("productive:${plan.day}:${q.senseId}", plan.day, 5)
        val recovered = correct && q.kind == "TYPING" && LearningEngine.trouble(word.progress) && !LearningEngine.trouble(updated)
        if (recovered) {
            updated = updated.copy(recoveredAt = now())
            earned += award("recover:${plan.day}:${q.senseId}", plan.day, 5)
            plan = plan.copy(recovered = plan.recovered.plusId(q.senseId))
        } else if (correct && q.kind == "TYPING" && updated.recentResults.takeLast(3) == "111" && updated.recognitionStrength >= .5f) {
            // Historical lapse count remains an audit trail; recovery closes that trouble episode.
            if (LearningEngine.trouble(word.progress) && updated.recognitionStrength - updated.productionStrength < .45f) {
                updated = updated.copy(recoveredAt = now())
                earned += award("recover:${plan.day}:${q.senseId}", plan.day, 5)
                plan = plan.copy(recovered = plan.recovered.plusId(q.senseId))
            }
        }
        dao.put(updated)
        if (learning.session.kind == "NEW") plan = plan.copy(newDone = plan.newDone.plusId(q.senseId))
        if (learning.session.kind == "REVIEW") plan = plan.copy(reviewDone = plan.reviewDone.plusId(q.senseId),
            reviewAttempts = plan.reviewAttempts + 1, reviewCorrect = plan.reviewCorrect + if (correct) 1 else 0)
        if (q.kind in listOf("TYPING", "CLOZE")) plan = plan.copy(productiveAttempts = plan.productiveAttempts + 1,
            productiveCorrect = plan.productiveCorrect + if (correct) 1 else 0)
        dao.put(plan.copy(xp = dao.plan(plan.day)!!.xp))
        dao.put(q.copy(answered = true, correct = correct, submitted = submitted.ifBlank { effectiveRating.name }, revealed = true))
        dao.insert(ReviewHistoryEntity(q.id, q.senseId, plan.day, now(), q.kind, effectiveRating.name, correct, planned || due, responseMs, earned))
        updateGame()
        loadSession(sessionId)!!
    }
    suspend fun next(sessionId: String): LearningSession = db.withTransaction {
        val learning = loadSession(sessionId)!!
        check(learning.session.day == today().toString()) { "Ngày mới đã bắt đầu. Hãy mở hành trình hôm nay." }
        if (learning.session.complete || learning.current?.answered != true) return@withTransaction learning
        val next = learning.session.cursor + 1
        val complete = next >= learning.questions.size
        dao.put(learning.session.copy(cursor = next, complete = complete))
        if (complete) {
            var plan = ensurePlan()
            if (learning.session.kind == "CHALLENGE") {
                plan = plan.copy(challengeComplete = true, challengeCorrect = learning.questions.count { it.correct }, challengeTotal = learning.questions.size)
                dao.put(plan)
            }
            if (learning.session.kind == "CHECKPOINT") {
                val cp = dao.checkpoints().first { it.id == learning.session.scope }
                val accuracy = learning.questions.count { it.correct }.toFloat() / learning.questions.size.coerceAtLeast(1)
                val retention = dao.history().filter { it.scheduled && it.kind !in listOf("EXPOSURE", "FLASHCARD") && it.senseId in learning.questions.map { q -> q.senseId } }
                    .let { h -> h.count { it.correct }.toFloat() / h.size.coerceAtLeast(1) }
                val passed = accuracy >= .8f && retention >= .8f
                val seal = if (!passed) "" else when { cp.threshold >= 90 -> "Platinum"; cp.threshold >= 75 -> "Gold"; cp.threshold >= 50 -> "Silver"; else -> "Bronze" }
                dao.put(cp.copy(passed = cp.passed || passed, bestAccuracy = maxOf(cp.bestAccuracy, accuracy), seal = seal.ifEmpty { cp.seal }))
            }
            updateGame()
        }
        loadSession(sessionId)!!
    }
    private suspend fun award(key: String, day: String, xp: Int): Int {
        if (dao.reward(key) != null) return 0
        dao.insert(RewardEntity(key, day, xp))
        val p = dao.profile()!!
        dao.put(p.copy(lifetimeLearningXp = p.lifetimeLearningXp + xp))
        val plan = dao.plan(day)
        if (plan != null) dao.put(plan.copy(xp = plan.xp + xp))
        return xp
    }
    private suspend fun updateGame() {
        var plan = ensurePlan()
        val all = words()
        dao.quests(plan.day).forEach { quest ->
            val progress = when (quest.kind) {
                "RECALL" -> plan.reviewDone.ids().size
                "EXPLORER" -> plan.newDone.ids().size
                "PRECISION" -> if (plan.challengeComplete) plan.challengeCorrect * 100 / plan.challengeTotal.coerceAtLeast(1) else 0
                "PRODUCTION" -> plan.productiveCorrect
                "RECOGNITION" -> dao.history().filter { it.day == plan.day && it.scheduled && it.correct && it.kind in listOf("MEANING", "DEFINITION") }.map { it.senseId }.distinct().size
                "COMEBACK" -> plan.recovered.ids().size
                "ACADEMIC" -> all.count { it.sense.id in plan.newDone.ids() && it.lexeme.academicScore >= .6f }
                else -> 0
            }
            if (progress >= quest.target && !quest.rewarded) award("quest:${quest.id}", plan.day, 10)
            dao.put(quest.copy(progress = progress, rewarded = quest.rewarded || progress >= quest.target))
        }
        plan = dao.plan(plan.day)!!
        if (GameRules.minimum(plan) && !plan.streakEarned) {
            val p = dao.profile()!!
            val streak = if (p.lastStreakDay == today().minusDays(1).toString()) p.streak + 1 else if (p.lastStreakDay == plan.day) p.streak else 1
            dao.put(p.copy(streak = streak, longestStreak = maxOf(streak, p.longestStreak), lastStreakDay = plan.day))
            plan = plan.copy(streakEarned = true)
            dao.put(plan)
        }
        if (GameRules.perfect(plan) && !plan.journeyRewarded) {
            award("journey:${plan.day}", plan.day, 20)
            plan = dao.plan(plan.day)!!.copy(journeyRewarded = true)
            dao.put(plan)
        }
        if (GameRules.perfect(plan) && !plan.perfect) {
            award("perfect:${plan.day}", plan.day, 15)
            val p = dao.profile()!!
            val count = p.perfectDays + 1
            dao.put(p.copy(perfectDays = count, graceTokens = minOf(2, p.graceTokens + if (count % 7 == 0) 1 else 0)))
            plan = dao.plan(plan.day)!!.copy(perfect = true)
            dao.put(plan)
        }
        val ielts = dao.profile()!!.mode == "IELTS"
        val totalSeen = all.count { it.progress.seen }
        for (threshold in 50..totalSeen step 50) {
            val id = "all:$threshold"
            if (dao.checkpoints().none { it.id == id }) dao.put(CheckpointEntity(id, "", threshold))
        }
        val mastered = all.filter { LearningEngine.state(it.progress, ielts) == MemoryState.MASTERED }
        val achievements = mutableListOf<Pair<String, String>>()
        if (mastered.isNotEmpty()) achievements += "first_light" to "First Light · Thành thạo từ đầu tiên"
        if (all.flatMap { it.topics }.distinct().any { t -> mastered.count { t in it.topics } >= 25 }) achievements += "constellation" to "Constellation · 25 ngôi sao thành thạo"
        if (dao.plans().sumOf { it.recovered.ids().size } >= 10) achievements += "comeback" to "Comeback · Khôi phục 10 từ khó"
        if (all.count { it.progress.intervalMinutes > 43200 } >= 50) achievements += "deep_memory" to "Deep Memory · 50 từ nhớ lâu"
        if (mastered.count { it.lexeme.academicScore >= .6f } >= 100) achievements += "academic_core" to "Academic Core · 100 từ học thuật"
        val encounteredSources = all.filter { it.progress.seen }.flatMap { it.sources }.map { it.sourcePackId }.distinct()
            .count { id -> dao.sources().any { it.id == id && it.sourceType.startsWith("IELTS_") } }
        if (encounteredSources >= 10) achievements += "ielts_explorer" to "IELTS Explorer · 10 nguồn luyện tập"
        if ((0L..6L).all { offset -> dao.plan(today().minusDays(offset).toString())?.perfect == true }) achievements += "perfect_week" to "Perfect Week · Trọn vẹn 7 ngày"
        achievements.forEach { (id, title) -> dao.insert(AchievementEntity(id, title, now())) }
        dao.topics().forEach { topic ->
            val topicWords = all.filter { topic.id in it.topics }
            if (topicWords.isNotEmpty()) {
                val coverage = topicWords.count { it.progress.seen } * 100 / topicWords.size
                listOf(25, 50, 75, 90).filter { coverage >= it }.forEach { threshold ->
                    val id = "${topic.id}:$threshold"
                    if (dao.checkpoints().none { it.id == id }) dao.put(CheckpointEntity(id, topic.id, threshold))
                }
            }
        }
        val weekStart = today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
        val weekPlans = dao.plans().filter { it.day >= weekStart }
        if (weekPlans.sumOf { it.newDone.ids().size } >= plan.goal * 5 && weekPlans.count { it.journeyRewarded } >= 5 &&
            weekPlans.sumOf { it.reviewCorrect }.toFloat() / weekPlans.sumOf { it.reviewAttempts }.coerceAtLeast(1) >= .8f)
            award("weekly:$weekStart", plan.day, 30)
    }
    companion object {
        fun expected(word: Word, kind: String) = when (kind) {
            "MEANING" -> word.sense.meaningVi
            "COLLOCATION" -> word.collocations.firstOrNull() ?: word.lexeme.lemma
            else -> word.lexeme.lemma
        }
    }
}
