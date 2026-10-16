package com.lexidaily.app.data

import androidx.room.*

@Entity data class LexemeEntity(
    @PrimaryKey val id: String, val lemma: String, val defaultDisplay: String = lemma,
    val frequencyRank: Int = 0, val academicScore: Float = 0f,
    val ieltsRelevanceScore: Float = 0f, val productivePriority: Float = 0f,
    val difficultyScore: Float = .5f, val lexicalDataSource: String = "lexidaily_demo",
    val cefrSource: String = "Editorial estimate", val frequencySource: String = "Not ranked"
)
@Entity(indices = [Index("lexemeId")], foreignKeys = [ForeignKey(
    entity = LexemeEntity::class, parentColumns = ["id"], childColumns = ["lexemeId"]
)]) data class SenseEntity(
    @PrimaryKey val id: String, val lexemeId: String, val partOfSpeech: String,
    val definitionEn: String, val meaningVi: String, val cefrLevel: String,
    val register: String = "neutral", val exampleEn: String, val exampleVi: String = "",
    val commonError: String = ""
)
@Entity data class PronunciationEntity(@PrimaryKey val senseId: String, val ipaBr: String, val ipaAm: String = "")
@Entity(primaryKeys = ["rootLexemeId", "relatedLexemeId"])
data class WordFamilyEntity(val rootLexemeId: String, val relatedLexemeId: String, val relation: String)
@Entity data class CollocationEntity(@PrimaryKey val id: String, val senseId: String, val text: String, val strength: Float = .8f, val register: String = "neutral")
@Entity(primaryKeys = ["senseId", "text"])
data class ParaphraseEntity(val senseId: String, val text: String, val relationType: String = "NEAR_SYNONYM", val targetLexemeId: String? = null)
@Entity data class TopicEntity(@PrimaryKey val id: String, val name: String, val parentId: String? = null)
@Entity(primaryKeys = ["lexemeId", "topicId"])
data class LexemeTopicEntity(val lexemeId: String, val topicId: String)
@Entity data class SourcePackEntity(
    @PrimaryKey val id: String, val name: String, val sourceType: String,
    val publisher: String? = null, val series: String? = null, val volume: Int? = null,
    val year: Int? = null, val licenseType: String, val isBundled: Boolean = false,
    val priority: Float = 1f, val description: String = ""
)
@Entity(indices = [Index("lexemeId"), Index("sourcePackId")]) data class SourceOccurrenceEntity(
    @PrimaryKey val id: String, val lexemeId: String, val sourcePackId: String,
    val senseId: String? = null, val testNumber: Int? = null, val skill: String? = null,
    val section: String? = null, val topic: String? = null,
    val occurrenceCount: Int = 1, val sourceLocator: String? = null
)
@Entity data class CuratedPackEntity(@PrimaryKey val id: String, val name: String, val description: String)
@Entity(primaryKeys = ["packId", "lexemeId"])
data class PackWordEntity(val packId: String, val lexemeId: String)
@Entity data class UserWordProgress(
    @PrimaryKey val senseId: String, val seen: Boolean = false, val stage: Int = 0,
    val recognitionStrength: Float = 0f, val productionStrength: Float = 0f,
    val contextStrength: Float = 0f, val formStrength: Float = 0f,
    val productionSuccesses: Int = 0, val lapseCount: Int = 0,
    val recentResults: String = "", val intervalMinutes: Long = 0,
    val dueAt: Long = 0, val lastReviewedAt: Long = 0,
    @ColumnInfo(defaultValue = "0.5") val userDifficultyScore: Float = .5f, val bookmarked: Boolean = false,
    val recoveredAt: Long = 0
)
@Entity data class UserProfile(
    @PrimaryKey val id: Int = 1, val onboarded: Boolean = false,
    val mode: String = "GENERAL", val dailyGoal: Int = 20, val minCefr: String = "B1",
    val targetBand: String = "7.0", val priority: String = "Balanced",
    val topicMode: String = "DAILY", val preferredTopic: String = "environment",
    val theme: String = "SYSTEM", val haptics: Boolean = true,
    val reduceMotion: Boolean = false, val reminders: Boolean = false,
    val lifetimeLearningXp: Int = 0, val streak: Int = 0, val longestStreak: Int = 0,
    val lastStreakDay: String = "", val graceTokens: Int = 0, val perfectDays: Int = 0
)
@Entity data class DailyPlanEntity(
    @PrimaryKey val day: String, val topicId: String, val goal: Int,
    val reviewIds: String, val newIds: String, val reviewDone: String = "",
    val newDone: String = "", val challengeComplete: Boolean = false,
    val challengeCorrect: Int = 0, val challengeTotal: Int = 0,
    val reviewCorrect: Int = 0, val reviewAttempts: Int = 0,
    val productiveCorrect: Int = 0, val productiveAttempts: Int = 0,
    val recovered: String = "", val xp: Int = 0, val perfect: Boolean = false,
    val streakEarned: Boolean = false, val journeyRewarded: Boolean = false,
    val graceUsed: Boolean = false
)
@Entity data class QuestEntity(
    @PrimaryKey val id: String, val day: String, val kind: String,
    val title: String, val target: Int, val progress: Int = 0, val rewarded: Boolean = false
)
@Entity data class ReviewHistoryEntity(
    @PrimaryKey val id: String, val senseId: String, val day: String,
    val answeredAt: Long, val kind: String, val rating: String,
    val correct: Boolean, val scheduled: Boolean, val responseMs: Long,
    val xp: Int
)
@Entity data class RewardEntity(@PrimaryKey val key: String, val day: String, val xp: Int)
@Entity data class SessionEntity(
    @PrimaryKey val id: String, val day: String, val kind: String,
    val cursor: Int = 0, val complete: Boolean = false, val scope: String = "",
    val startedAt: Long
)
@Entity(indices = [Index("sessionId")]) data class SessionQuestionEntity(
    @PrimaryKey val id: String, val sessionId: String, val position: Int,
    val senseId: String, val kind: String, val options: String = "",
    val revealed: Boolean = false, val draft: String = "",
    val answered: Boolean = false, val correct: Boolean = false,
    val submitted: String = ""
)
@Entity data class AchievementEntity(@PrimaryKey val id: String, val title: String, val earnedAt: Long)
@Entity data class CheckpointEntity(
    @PrimaryKey val id: String, val topicId: String, val threshold: Int,
    val passed: Boolean = false, val bestAccuracy: Float = 0f, val seal: String = ""
)
@Entity data class ContentVersionEntity(@PrimaryKey val id: Int = 1, val version: Int)

fun String.ids(): List<String> = split('|').filter { it.isNotBlank() }
fun Iterable<String>.encoded(): String = distinct().joinToString("|")
fun String.plusId(id: String): String = (ids() + id).encoded()

data class Word(
    val lexeme: LexemeEntity, val sense: SenseEntity,
    val pronunciation: PronunciationEntity? = null,
    val progress: UserWordProgress = UserWordProgress(sense.id),
    val topics: List<String> = emptyList(), val collocations: List<String> = emptyList(),
    val family: List<String> = emptyList(), val paraphrases: List<String> = emptyList(),
    val sources: List<SourceOccurrenceEntity> = emptyList()
)
