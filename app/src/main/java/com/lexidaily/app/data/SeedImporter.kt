package com.lexidaily.app.data

import android.content.Context
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

class SeedImporter(private val context: Context, private val db: LexiDatabase) {
    private fun array(path: String): List<JSONObject> {
        val data = JSONArray(context.assets.open(path).bufferedReader().use { it.readText() })
        return (0 until data.length()).map { data.getJSONObject(it) }
    }
    suspend fun importIfNeeded() {
        val version = JSONObject(context.assets.open("manifest.json").bufferedReader().use { it.readText() }).getInt("version")
        val dao = db.dao()
        if ((dao.contentVersion()?.version ?: 0) >= version) return
        db.withTransaction {
            dao.lexemes(array("vocabulary/lexemes.json").map {
                LexemeEntity(it.getString("id"), it.getString("lemma"), it.optString("defaultDisplay", it.getString("lemma")),
                    it.optInt("frequencyRank"), it.float("academicScore"), it.float("ieltsRelevanceScore"),
                    it.float("productivePriority"), it.float("difficultyScore"), it.getString("lexicalDataSource"),
                    it.getString("cefrSource"), it.getString("frequencySource"))
            })
            dao.senses(array("vocabulary/senses.json").map {
                SenseEntity(it.getString("id"), it.getString("lexemeId"), it.getString("partOfSpeech"),
                    it.getString("definitionEn"), it.getString("meaningVi"), it.getString("cefrLevel"),
                    it.getString("register"), it.getString("exampleEn"), it.optString("exampleVi"), it.optString("commonError"))
            })
            dao.pronunciations(array("vocabulary/pronunciations.json").map { PronunciationEntity(it.getString("senseId"), it.getString("ipaBr"), it.optString("ipaAm")) })
            dao.topics(array("vocabulary/topics.json").map { TopicEntity(it.getString("id"), it.getString("name"), it.nullString("parentId")) })
            dao.topicLinks(array("vocabulary/topic_links.json").map { LexemeTopicEntity(it.getString("lexemeId"), it.getString("topicId")) })
            dao.collocations(array("vocabulary/collocations.json").map { CollocationEntity(it.getString("id"), it.getString("senseId"), it.getString("text")) })
            dao.families(array("vocabulary/word_families.json").map { WordFamilyEntity(it.getString("rootLexemeId"), it.getString("relatedLexemeId"), it.getString("relation")) })
            dao.paraphrases(array("vocabulary/paraphrases.json").map { ParaphraseEntity(it.getString("senseId"), it.getString("text"), it.getString("relationType"), it.nullString("targetLexemeId")) })
            dao.sources(listOf("demo", "ngsl", "nawl").flatMap { array("sources/$it.json") }.map {
                SourcePackEntity(it.getString("id"), it.getString("name"), it.getString("sourceType"),
                    it.nullString("publisher"), it.nullString("series"), it.nullInt("volume"), it.nullInt("year"),
                    it.getString("licenseType"), it.optBoolean("isBundled"), it.optDouble("priority", 1.0).toFloat(), it.optString("description"))
            })
            dao.occurrences(array("sources/occurrences.json").map {
                SourceOccurrenceEntity(it.getString("id"), it.getString("lexemeId"), it.getString("sourcePackId"),
                    it.nullString("senseId"), it.nullInt("testNumber"), it.nullString("skill"), it.nullString("section"),
                    it.nullString("topic"), it.optInt("occurrenceCount", 1), it.nullString("sourceLocator"))
            })
            dao.packs(listOf("general", "academic", "ielts_topics").flatMap { array("packs/$it.json") }.map {
                CuratedPackEntity(it.getString("id"), it.getString("name"), it.getString("description"))
            })
            dao.packLinks(array("packs/memberships.json").map { PackWordEntity(it.getString("packId"), it.getString("lexemeId")) })
            dao.put(ContentVersionEntity(version = version))
        }
    }
    private fun JSONObject.float(key: String) = optDouble(key, 0.0).toFloat()
    private fun JSONObject.nullString(key: String): String? = if (has(key) && !isNull(key)) getString(key) else null
    private fun JSONObject.nullInt(key: String): Int? = if (has(key) && !isNull(key)) getInt(key) else null
}
