package com.lexidaily.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val freshDatabase = object : ExternalResource() {
        override fun before() {
            runBlocking(Dispatchers.IO) {
                ApplicationProvider.getApplicationContext<LexiApplication>().database.clearAllTables()
            }
        }
    }
    @get:Rule val rules = RuleChain.outerRule(freshDatabase).around(compose)
    private fun awaitText(text: String) {
        compose.waitUntil(60000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun awaitEnabled(text: String) {
        compose.waitUntil(60000) {
            try { compose.onNodeWithText(text).assertIsEnabled(); true }
            catch (_: AssertionError) { false }
        }
    }
    private fun shot(name: String) {
        compose.waitForIdle()
        val file = File(compose.activity.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun onboardingLearningResumeLibraryAndDarkMode() {
        awaitText("General English")
        shot("onboarding")
        compose.onNodeWithText("Tiếp tục", useUnmergedTree = true).performClick()
        compose.onNodeWithText("10", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Bắt đầu hành trình").performScrollTo().performClick()
        awaitText("Học một chút,\nđi thật xa.")
        shot("home-light")
        compose.onNodeWithText("Tiếp tục học").performScrollTo().performClick()
        awaitText("Xem nghĩa")
        shot("flashcard-front")
        awaitEnabled("Xem nghĩa")
        compose.onNodeWithText("Xem nghĩa").performClick()
        awaitText("Good")
        shot("flashcard-back")
        awaitEnabled("Good")
        compose.onNodeWithText("Good").performClick()
        awaitText("Từ tiếp theo")
        awaitEnabled("Từ tiếp theo")
        compose.onNodeWithText("Từ tiếp theo").performClick()
        awaitText("2 / 10")
        compose.activityRule.scenario.recreate()
        awaitText("2 / 10")
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithText("Library").performClick()
        awaitText("Thư viện của bạn")
        shot("library")
        compose.onNodeWithText("Progress").performClick()
        awaitText("Nhìn lại để tiến xa")
        shot("progress")
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithContentDescription("Cài đặt").performClick()
        compose.onNodeWithText("DARK").performScrollTo().performClick()
        compose.onNodeWithText("Lưu cài đặt").performScrollTo().performClick()
        awaitText("Học một chút,\nđi thật xa.")
        lateinit var vm: LexiViewModel
        compose.runOnIdle { vm = ViewModelProvider(compose.activity)[LexiViewModel::class.java] }
        compose.waitUntil(60000) { vm.state.value.data?.profile?.theme == "DARK" && !vm.state.value.busy }
        compose.waitForIdle()
        shot("home-dark")
        compose.onNodeWithText("Library").performClick()
        awaitText("Thư viện của bạn")
        shot("library-dark")
        compose.onNodeWithText("Progress").performClick()
        awaitText("Nhìn lại để tiến xa")
        shot("progress-dark")
        compose.onNodeWithText("Learn").performClick()
        awaitText("Một hành trình,\nba bước tiến.")
        shot("learn-dark")
        compose.onNodeWithText("02 · Từ mới").performScrollTo().performClick()
        awaitText("Xem nghĩa")
        shot("flashcard-front-dark")
        compose.onNodeWithText("Xem nghĩa").performClick()
        awaitText("Good")
        shot("flashcard-back-dark")
    }
}
