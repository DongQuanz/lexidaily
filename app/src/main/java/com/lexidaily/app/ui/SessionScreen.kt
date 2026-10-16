package com.lexidaily.app.ui

import android.os.SystemClock
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.lexidaily.app.data.*
import com.lexidaily.app.domain.*
import kotlinx.coroutines.delay

@Composable fun SessionScreen(data: AppSnapshot, learning: LearningSession, busy: Boolean,
    onReveal: () -> Unit, onAnswer: (String, Rating?, Long) -> Unit, onDraft: (String) -> Unit,
    onNext: () -> Unit, onDone: () -> Unit, onSpeak: (String) -> Unit, onBookmark: (String) -> Unit) {
    if (learning.session.complete) { CompletionScreen(data, learning, onDone); return }
    val q = learning.current ?: return
    val word = data.words.find { it.sense.id == q.senseId } ?: return
    var typed by rememberSaveable(q.id) { mutableStateOf(q.draft) }
    val started = remember(q.id) { SystemClock.elapsedRealtime() }
    val haptic = LocalHapticFeedback.current
    val shake = remember(q.id) { Animatable(0f) }
    val scale = remember(q.id) { Animatable(1f) }
    LaunchedEffect(q.answered) {
        if (q.answered) {
            if (q.correct && data.profile.haptics) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            if (!data.profile.reduceMotion) {
                if (q.correct) { scale.animateTo(1.015f, tween(100)); scale.animateTo(1f, tween(150)) }
                else { shake.animateTo(-5f, tween(60)); shake.animateTo(5f, tween(80)); shake.animateTo(0f, tween(60)) }
            }
        }
    }
    val flash = q.kind in listOf("EXPOSURE", "FLASHCARD")
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = Spacing.screen), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Eyebrow(when (learning.session.kind) { "NEW" -> "Khám phá từ mới"; "REVIEW" -> "Ôn tập đúng lúc"; "REPAIR" -> "Củng cố từ khó"; "CHECKPOINT" -> "Checkpoint"; "CHALLENGE" -> "Thử thách"; else -> "Luyện tập" })
            Text("${learning.session.cursor + 1} / ${learning.questions.size}", style = MaterialTheme.typography.labelLarge)
        }
        ProgressBar(learning.session.cursor.toFloat() / learning.questions.size.coerceAtLeast(1))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).graphicsLayer { translationX = shake.value; scaleX = scale.value; scaleY = scale.value }, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (flash) FlashCard(word, q.revealed, data.profile.mode == "IELTS", data.profile.reduceMotion, onReveal, { onSpeak(word.lexeme.lemma) }, { onBookmark(word.sense.id) })
            else {
                Spacer(Modifier.height(12.dp))
                Eyebrow(when (q.kind) { "MEANING" -> "Chọn nghĩa đúng"; "DEFINITION" -> "Từ nào khớp với định nghĩa?"; "CLOZE" -> "Điền từ còn thiếu"; "COLLOCATION" -> "Chọn cụm từ phù hợp"; else -> "Tự nhớ từ tiếng Anh" })
                Text(when (q.kind) {
                    "MEANING" -> word.lexeme.lemma
                    "CLOZE" -> word.sense.exampleEn.replace(Regex("\\b${Regex.escape(word.lexeme.lemma)}\\b", RegexOption.IGNORE_CASE), "________")
                    "COLLOCATION" -> "Cụm nào thường đi với “${word.lexeme.lemma}”?"
                    else -> word.sense.definitionEn
                }, style = if (q.kind == "MEANING") MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                if (q.kind in listOf("TYPING", "CLOZE")) {
                    OutlinedTextField(typed, { typed = it.take(100); onDraft(typed) }, Modifier.fillMaxWidth(),
                        label = { Text("Nhập từ tiếng Anh") }, singleLine = true, enabled = !q.answered && !busy,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                        shape = MaterialTheme.shapes.medium)
                } else q.options.ids().forEach { option ->
                    OutlinedButton({ onAnswer(option, null, SystemClock.elapsedRealtime() - started) }, Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        enabled = !q.answered && !busy, shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(16.dp)) {
                        Text(option, Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            if (q.answered) {
                Card(colors = CardDefaults.cardColors(containerColor = if (q.correct) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (q.correct) "✓ Đã củng cố thêm một chút" else "Thử lại sẽ giúp nhớ lâu hơn", style = MaterialTheme.typography.titleMedium)
                        if (!flash) { Text("Đáp án: ${LexiRepository.expected(word, q.kind)}", fontWeight = FontWeight.SemiBold); Text(word.sense.meaningVi) }
                        if (LearningEngine.state(word.progress, data.profile.mode == "IELTS") == MemoryState.MASTERED) Text("★ Từ vựng đã thành thạo", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Column(Modifier.padding(bottom = 16.dp)) {
            when {
                q.answered -> PrimaryButton(if (learning.session.cursor == learning.questions.lastIndex) "Xem kết quả" else "Từ tiếp theo", enabled = !busy, onClick = onNext)
                flash && !q.revealed -> PrimaryButton("Xem nghĩa", enabled = !busy, onClick = onReveal)
                flash -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Rating.entries.forEach { rating -> RatingButton(rating, LearningEngine.interval(word.progress, rating), Modifier.weight(1f), !busy) { onAnswer("", rating, SystemClock.elapsedRealtime() - started) } }
                }
                q.kind in listOf("TYPING", "CLOZE") -> PrimaryButton("Kiểm tra", enabled = !busy && typed.isNotBlank()) { onAnswer(typed, null, SystemClock.elapsedRealtime() - started) }
                else -> Text("Chọn câu trả lời phù hợp nhất.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
            }
        }
    }
}

@Composable fun CompletionScreen(data: AppSnapshot, learning: LearningSession?, onDone: () -> Unit) {
    val p = data.plan
    val questions = learning?.questions.orEmpty()
    val recall = if (p.reviewAttempts == 0) "—" else "${p.reviewCorrect * 100 / p.reviewAttempts}%"
    val production = if (p.productiveAttempts == 0) "—" else "${p.productiveCorrect * 100 / p.productiveAttempts}%"
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(Unit) { if (data.profile.haptics) haptic.performHapticFeedback(HapticFeedbackType.Confirm) }
    Page {
        Spacer(Modifier.height(8.dp))
        Icon(if (p.perfect) Icons.Outlined.WorkspacePremium else Icons.Outlined.AutoAwesome, null, Modifier.size(56.dp), tint = AppColors.mastered)
        Eyebrow(if (p.perfect) "Perfect Day" else "Từng bước đều có ý nghĩa")
        Text(if (p.perfect) "Một ngày học\nthật trọn vẹn." else if (questions.isEmpty()) "Bạn đã bắt kịp\nnhịp ôn tập." else "Thêm một bước\nđến trí nhớ bền lâu.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        if (questions.isEmpty()) {
            val next = data.words.filter { it.progress.seen && it.progress.dueAt > System.currentTimeMillis() }.minOfOrNull { it.progress.dueAt }
            Text(if (next == null) "Chưa có từ đến hạn. Khám phá từ mới để bắt đầu lịch ôn của bạn." else "Không có từ nào đang phai mờ. Lần ôn tiếp theo sau ${LearningEngine.intervalLabel(((next - System.currentTimeMillis()) / 60000).coerceAtLeast(1))}.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else Text("Phiên này: ${questions.count { it.correct }} / ${questions.size} câu đã nhớ được.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth()) { StatBlock("${p.newDone.ids().size}", "Từ mới hôm nay", Modifier.weight(1f)); StatBlock("${p.reviewDone.ids().size}", "Từ đã ôn", Modifier.weight(1f)) }
        Row(Modifier.fillMaxWidth()) { StatBlock(recall, "Độ chính xác ôn tập", Modifier.weight(1f)); StatBlock(production, "Nhớ chủ động", Modifier.weight(1f)) }
        Text("${p.recovered.ids().size} từ khó được khôi phục · ${data.words.count { it.progress.recognitionStrength >= .65f }} từ đang vững", style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider()
        Row(Modifier.fillMaxWidth()) { StatBlock("+${p.xp} XP", "Học tập hôm nay", Modifier.weight(1f)); StatBlock("🔥 ${data.profile.streak}", "Ngày liên tiếp", Modifier.weight(1f)) }
        if (!p.perfect && p.newIds.isNotEmpty()) Text("Tiếp tục Ôn tập → Từ mới → Thử thách để hoàn thành Perfect Day.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        PrimaryButton("Về hành trình", onClick = onDone)
    }
}
