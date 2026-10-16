package com.lexidaily.app.domain

import com.lexidaily.app.data.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.*

enum class Rating(val label: String) { AGAIN("Again"), HARD("Hard"), GOOD("Good"), EASY("Easy") }
enum class MemoryState(val label: String, val symbol: String) {
    UNSEEN("Chưa học", "○"), SEEN("Đã gặp", "·"), LEARNING("Đang học", "✧"),
    STRONG("Vững", "✦"), MASTERED("Thành thạo", "★")
}
object LearningEngine {
    fun interval(p: UserWordProgress, rating: Rating): Long = when (rating) {
        Rating.AGAIN -> 10
        Rating.HARD -> max(1440, (p.intervalMinutes * 1.2).toLong())
        Rating.GOOD -> max(4320, (p.intervalMinutes * 2.4).toLong())
        Rating.EASY -> max(10080, (p.intervalMinutes * 3.2).toLong())
    }.coerceAtMost(525600)

    fun review(p: UserWordProgress, kind: String, rating: Rating, now: Long, responseMs: Long): UserWordProgress {
        val correct = rating != Rating.AGAIN
        val productive = kind == "TYPING" || kind == "CLOZE"
        val context = kind == "CLOZE" || kind == "COLLOCATION"
        val gain = when (rating) { Rating.AGAIN -> -.22f; Rating.HARD -> .04f; Rating.GOOD -> .13f; Rating.EASY -> .18f }
        val recognition = (p.recognitionStrength + gain).coerceIn(0f, 1f)
        val production = if (productive) (p.productionStrength + if (correct) .24f else -.28f).coerceIn(0f, 1f) else p.productionStrength
        val contextStrength = if (context) (p.contextStrength + if (correct) .25f else -.2f).coerceIn(0f, 1f) else p.contextStrength
        val nextStage = if (!correct) max(1, p.stage - 1) else when {
            kind == "EXPOSURE" -> max(1, p.stage)
            productive -> 4
            kind == "DEFINITION" -> 3
            kind == "MEANING" -> max(2, p.stage)
            else -> max(p.stage, if (recognition >= .5f) 3 else 1)
        }
        val minutes = interval(p, rating)
        val difficulty = (p.userDifficultyScore * .8f +
            (if (correct) .15f else .95f) * .15f + (responseMs / 60000f).coerceIn(0f, 1f) * .05f).coerceIn(0f, 1f)
        return p.copy(seen = true, stage = nextStage, recognitionStrength = recognition,
            productionStrength = production, contextStrength = contextStrength,
            formStrength = (p.formStrength + if (correct) .15f else -.1f).coerceIn(0f, 1f),
            productionSuccesses = p.productionSuccesses + if (productive && correct) 1 else 0,
            lapseCount = p.lapseCount + if (correct) 0 else 1,
            recentResults = (p.recentResults + if (correct) "1" else "0").takeLast(10),
            intervalMinutes = minutes, dueAt = now + minutes * 60000L, lastReviewedAt = now,
            userDifficultyScore = difficulty, recoveredAt = if (correct) p.recoveredAt else 0)
    }
    fun trouble(p: UserWordProgress): Boolean = p.seen && (
        (p.lapseCount >= 3 && p.recoveredAt == 0L) ||
        (p.recentResults.length >= 3 && p.recentResults.count { it == '1' }.toFloat() / p.recentResults.length < .6f) ||
        (p.recognitionStrength >= .6f && p.recognitionStrength - p.productionStrength >= .45f))

    fun state(p: UserWordProgress, ielts: Boolean): MemoryState = when {
        !p.seen -> MemoryState.UNSEEN
        p.intervalMinutes >= 43200 && p.recognitionStrength >= .8f &&
            (!ielts || (p.contextStrength >= .7f && p.productionSuccesses > 0 && p.productionStrength >= .5f)) -> MemoryState.MASTERED
        p.recognitionStrength >= .65f -> MemoryState.STRONG
        p.stage >= 1 -> MemoryState.LEARNING
        else -> MemoryState.SEEN
    }
    fun questionKind(p: UserWordProgress): String = when {
        !p.seen -> "EXPOSURE"
        trouble(p) && p.recognitionStrength < .5f -> "MEANING"
        p.recognitionStrength - p.productionStrength >= .3f && p.stage >= 3 -> "TYPING"
        p.stage <= 1 -> "MEANING"
        p.stage == 2 -> "DEFINITION"
        p.stage == 3 || p.contextStrength < .7f -> "CLOZE"
        else -> "TYPING"
    }
    fun answerMatches(answer: String, expected: String): Boolean =
        answer.trim().lowercase().replace(Regex("\\s+"), " ") == expected.trim().lowercase().replace(Regex("\\s+"), " ")
    fun supportedQuestionKind(word: Word, requested: String): String {
        val candidate = if (requested == "COLLOCATION" && word.collocations.isEmpty()) "CLOZE" else requested
        val hasClozeTarget = word.sense.exampleEn.isNotBlank() &&
            Regex("\\b${Regex.escape(word.lexeme.lemma)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(word.sense.exampleEn)
        return if (candidate == "CLOZE" && !hasClozeTarget) "TYPING" else candidate
    }
    fun intervalLabel(minutes: Long): String = when {
        minutes < 60 -> "$minutes phút"
        minutes < 1440 -> "${minutes / 60} giờ"
        else -> "${minutes / 1440} ngày"
    }
    fun position(id: String): Pair<Float, Float> {
        val a = id.hashCode().toLong() and 0xffffffffL
        val b = (a * 1664525 + 1013904223) and 0xffffffffL
        return (.08f + (a % 10000) / 10000f * .84f) to (.08f + (b % 10000) / 10000f * .84f)
    }
}

data class SelectionWeights(val need: Float = 1.3f, val relevance: Float = 1.2f,
    val frequency: Float = .3f, val skill: Float = 1f, val topic: Float = 1.4f, val academic: Float = .7f)
object DailySelector {
    private val levels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
    fun select(words: List<Word>, profile: UserProfile, topicId: String, sources: List<SourcePackEntity>, count: Int): List<Word> {
        val eligible = words.filter { !it.progress.seen && it.progress.lapseCount == 0 }
        val min = levels.indexOf(profile.minCefr)
        val filtered = if (profile.mode == "GENERAL") eligible.filter { levels.indexOf(it.sense.cefrLevel) >= min } else eligible
        val topicWords = filtered.filter { topicId in it.topics }
        val pool = if (profile.topicMode == "FIXED") topicWords else filtered
        return pool.sortedWith(compareByDescending<Word> { score(it, profile, topicId, sources) }.thenBy { it.sense.id }).take(count)
    }
    fun score(w: Word, p: UserProfile, topic: String, sources: List<SourcePackEntity>, weights: SelectionWeights = SelectionWeights()): Float {
        val band = (p.targetBand.removeSuffix("+").toFloatOrNull() ?: 7f).coerceIn(6f, 8f)
        val depth = ((band - 6f) / 2f).coerceIn(0f, 1f)
        val target = if (p.mode == "IELTS") 2.7f + depth * 1.2f else levels.indexOf(p.minCefr).toFloat()
        val fit = (1 - abs(levels.indexOf(w.sense.cefrLevel) - target) / 4).coerceIn(0f, 1f)
        val matched = w.sources.filter { it.skill?.equals(p.priority.removePrefix("Academic "), true) == true }
        val sourceScore = w.sources.sumOf { occurrence ->
            val source = sources.find { it.id == occurrence.sourcePackId }
            val recency = source?.year?.let { 1.0 / (1 + max(0, LocalDate.now().year - it) * .08) } ?: .5
            (source?.priority ?: .5f) * recency
        }.toFloat().coerceAtMost(5f) / 5
        val collocationUtility = (w.collocations.size / 3f).coerceAtMost(1f)
        return weights.need * fit + weights.topic * ifTopic(topic in w.topics) +
            (if (p.mode == "IELTS") weights.relevance * w.lexeme.ieltsRelevanceScore +
                weights.academic * (1f + depth * .3f) * w.lexeme.academicScore + weights.skill *
                (if (p.priority == "Writing" || p.priority == "Speaking") (w.lexeme.productivePriority + collocationUtility) / 2 else ifTopic(matched.isNotEmpty())) +
                depth * .3f * (collocationUtility + w.lexeme.productivePriority) else 0f) +
            weights.frequency * sourceScore - if (w.sense.cefrLevel == "C2") .4f else 0f
    }
    private fun ifTopic(value: Boolean) = if (value) 1f else 0f
}

object GameRules {
    fun reviewComplete(plan: DailyPlanEntity) = plan.reviewDone.ids().containsAll(plan.reviewIds.ids())
    fun newComplete(plan: DailyPlanEntity) = plan.newDone.ids().size >= plan.newIds.ids().size
    fun challengeUnlocked(plan: DailyPlanEntity) = if (plan.newIds.isEmpty()) plan.reviewIds.isNotEmpty()
        else plan.newDone.ids().size >= ceil(plan.newIds.ids().size * .7).toInt()
    fun minimum(plan: DailyPlanEntity) = reviewComplete(plan) && if (plan.newIds.isEmpty()) plan.reviewIds.isNotEmpty()
        else plan.newDone.ids().size >= ceil(plan.newIds.ids().size * .5).toInt() && plan.newDone.ids().isNotEmpty()
    fun perfect(plan: DailyPlanEntity) = reviewComplete(plan) && newComplete(plan) && plan.challengeComplete
    fun level(xp: Int) = 1 + sqrt(xp.coerceAtLeast(0) / 100.0).toInt()
    fun title(level: Int) = when { level >= 30 -> "Lexical Scholar"; level >= 20 -> "Word Strategist"; level >= 10 -> "Lexical Explorer"; level >= 5 -> "Vocabulary Scout"; else -> "Word Starter" }
    fun bridge(profile: UserProfile, today: LocalDate): UserProfile {
        if (profile.lastStreakDay.isEmpty()) return profile
        val missed = ChronoUnit.DAYS.between(LocalDate.parse(profile.lastStreakDay), today) - 1
        if (missed <= 0) return profile
        return if (missed <= profile.graceTokens) profile.copy(graceTokens = profile.graceTokens - missed.toInt(), lastStreakDay = today.minusDays(1).toString())
        else profile.copy(streak = 0, lastStreakDay = "")
    }
}
