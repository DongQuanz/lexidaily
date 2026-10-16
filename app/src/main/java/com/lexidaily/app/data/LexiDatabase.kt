package com.lexidaily.app.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao interface LexiDao {
    @Query("SELECT * FROM LexemeEntity ORDER BY lemma") suspend fun lexemes(): List<LexemeEntity>
    @Query("SELECT * FROM SenseEntity") suspend fun senses(): List<SenseEntity>
    @Query("SELECT * FROM PronunciationEntity") suspend fun pronunciations(): List<PronunciationEntity>
    @Query("SELECT * FROM TopicEntity") suspend fun topics(): List<TopicEntity>
    @Query("SELECT * FROM LexemeTopicEntity") suspend fun topicLinks(): List<LexemeTopicEntity>
    @Query("SELECT * FROM SourcePackEntity") suspend fun sources(): List<SourcePackEntity>
    @Query("SELECT * FROM SourceOccurrenceEntity") suspend fun occurrences(): List<SourceOccurrenceEntity>
    @Query("SELECT * FROM CollocationEntity") suspend fun collocations(): List<CollocationEntity>
    @Query("SELECT * FROM WordFamilyEntity") suspend fun families(): List<WordFamilyEntity>
    @Query("SELECT * FROM ParaphraseEntity") suspend fun paraphrases(): List<ParaphraseEntity>
    @Query("SELECT * FROM CuratedPackEntity") suspend fun packs(): List<CuratedPackEntity>
    @Query("SELECT * FROM PackWordEntity") suspend fun packLinks(): List<PackWordEntity>
    @Query("SELECT * FROM ContentVersionEntity WHERE id=1") suspend fun contentVersion(): ContentVersionEntity?
    @Query("SELECT * FROM UserProfile WHERE id=1") suspend fun profile(): UserProfile?
    @Query("SELECT * FROM UserWordProgress") suspend fun progresses(): List<UserWordProgress>
    @Query("SELECT * FROM DailyPlanEntity WHERE day=:day") suspend fun plan(day: String): DailyPlanEntity?
    @Query("SELECT * FROM DailyPlanEntity ORDER BY day") suspend fun plans(): List<DailyPlanEntity>
    @Query("SELECT * FROM QuestEntity WHERE day=:day") suspend fun quests(day: String): List<QuestEntity>
    @Query("DELETE FROM QuestEntity WHERE day=:day") suspend fun deleteQuests(day: String)
    @Query("SELECT * FROM ReviewHistoryEntity ORDER BY answeredAt") suspend fun history(): List<ReviewHistoryEntity>
    @Query("SELECT * FROM SessionEntity WHERE day=:day AND complete=0 ORDER BY startedAt DESC LIMIT 1") suspend fun activeSession(day: String): SessionEntity?
    @Query("SELECT * FROM SessionEntity WHERE id=:id") suspend fun session(id: String): SessionEntity?
    @Query("SELECT * FROM SessionQuestionEntity WHERE sessionId=:session ORDER BY position") suspend fun questions(session: String): List<SessionQuestionEntity>
    @Query("SELECT * FROM RewardEntity WHERE `key`=:key") suspend fun reward(key: String): RewardEntity?
    @Query("SELECT * FROM AchievementEntity") suspend fun achievements(): List<AchievementEntity>
    @Query("SELECT * FROM CheckpointEntity") suspend fun checkpoints(): List<CheckpointEntity>
    @Query("SELECT * FROM UserProfile WHERE id=1") fun observeProfile(): Flow<UserProfile?>
    @Query("SELECT * FROM DailyPlanEntity ORDER BY day DESC") fun observePlans(): Flow<List<DailyPlanEntity>>
    @Query("SELECT * FROM UserWordProgress") fun observeProgress(): Flow<List<UserWordProgress>>
    @Upsert suspend fun put(value: UserProfile)
    @Upsert suspend fun put(value: UserWordProgress)
    @Upsert suspend fun put(value: DailyPlanEntity)
    @Upsert suspend fun put(value: QuestEntity)
    @Upsert suspend fun put(value: SessionEntity)
    @Upsert suspend fun put(value: SessionQuestionEntity)
    @Upsert suspend fun put(value: CheckpointEntity)
    @Upsert suspend fun put(value: ContentVersionEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(value: RewardEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(value: ReviewHistoryEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(value: AchievementEntity)
    @Upsert suspend fun lexemes(values: List<LexemeEntity>)
    @Upsert suspend fun senses(values: List<SenseEntity>)
    @Upsert suspend fun pronunciations(values: List<PronunciationEntity>)
    @Upsert suspend fun topics(values: List<TopicEntity>)
    @Upsert suspend fun topicLinks(values: List<LexemeTopicEntity>)
    @Upsert suspend fun sources(values: List<SourcePackEntity>)
    @Upsert suspend fun occurrences(values: List<SourceOccurrenceEntity>)
    @Upsert suspend fun collocations(values: List<CollocationEntity>)
    @Upsert suspend fun families(values: List<WordFamilyEntity>)
    @Upsert suspend fun paraphrases(values: List<ParaphraseEntity>)
    @Upsert suspend fun packs(values: List<CuratedPackEntity>)
    @Upsert suspend fun packLinks(values: List<PackWordEntity>)
}

@Database(entities = [LexemeEntity::class, SenseEntity::class, PronunciationEntity::class,
    WordFamilyEntity::class, CollocationEntity::class, ParaphraseEntity::class,
    TopicEntity::class, LexemeTopicEntity::class, SourcePackEntity::class,
    SourceOccurrenceEntity::class, CuratedPackEntity::class, PackWordEntity::class,
    UserWordProgress::class, UserProfile::class, DailyPlanEntity::class, QuestEntity::class,
    ReviewHistoryEntity::class, RewardEntity::class, SessionEntity::class,
    SessionQuestionEntity::class, AchievementEntity::class, CheckpointEntity::class,
    ContentVersionEntity::class], version = 2, exportSchema = true)
abstract class LexiDatabase : RoomDatabase() {
    abstract fun dao(): LexiDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE UserWordProgress ADD COLUMN userDifficultyScore REAL NOT NULL DEFAULT 0.5")
            }
        }
        fun open(context: Context) = Room.databaseBuilder(context, LexiDatabase::class.java, "lexidaily.db")
            .addMigrations(MIGRATION_1_2).build()
    }
}
