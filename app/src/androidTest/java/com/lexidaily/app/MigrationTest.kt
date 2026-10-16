package com.lexidaily.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lexidaily.app.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @Test fun migrationAddsDifficultyWithoutLosingLearningHistory() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val original = Room.databaseBuilder(context, LexiDatabase::class.java, name).build()
        original.dao().put(UserProfile(onboarded = true, lifetimeLearningXp = 420, streak = 7))
        original.dao().put(UserWordProgress("sustainable_adjective_01", seen = true, recognitionStrength = .7f, lapseCount = 3))
        original.dao().insert(RewardEntity("unique-reward", "2026-10-16", 20))
        original.close()
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use { old ->
            old.execSQL("ALTER TABLE UserWordProgress DROP COLUMN userDifficultyScore")
            old.version = 1
        }
        val migrated = Room.databaseBuilder(context, LexiDatabase::class.java, name).addMigrations(LexiDatabase.MIGRATION_1_2).build()
        try {
            assertEquals(420, migrated.dao().profile()!!.lifetimeLearningXp)
            assertEquals(7, migrated.dao().profile()!!.streak)
            val p = migrated.dao().progresses().single()
            assertTrue(p.seen); assertEquals(3, p.lapseCount)
            assertEquals(.5f, p.userDifficultyScore, 0f)
            assertNotNull(migrated.dao().reward("unique-reward"))
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
}
