package com.lexidaily.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lexidaily.app.data.*
import com.lexidaily.app.domain.*
import kotlin.math.*

@Composable fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen).padding(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.section), content = content)
}
@Composable fun Eyebrow(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
}
@Composable fun SectionTitle(title: String, trailing: String? = null, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (trailing != null) {
            if (onClick != null) TextButton(onClick) { Text(trailing) }
            else Text(trailing, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable fun PrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = enabled, shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)) {
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, Modifier.size(20.dp))
    }
}
@Composable fun TopicChip(text: String, selected: Boolean = false, onClick: () -> Unit = {}) {
    FilterChip(selected, onClick, { Text(text) }, modifier = Modifier.heightIn(min = 48.dp))
}
@Composable fun CefrBadge(level: String) = BadgeLabel(level)
@Composable fun SourceBadge(text: String) = BadgeLabel(text, MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.colorScheme.onSurfaceVariant)
@Composable fun BadgeLabel(text: String, background: Color = MaterialTheme.colorScheme.primaryContainer,
    foreground: Color = MaterialTheme.colorScheme.onPrimaryContainer) {
    Text(text, Modifier.clip(RoundedCornerShape(8.dp)).background(background).padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelMedium, color = foreground)
}
@Composable fun ProgressBar(value: Float, modifier: Modifier = Modifier, color: Color = AppColors.primary) {
    LinearProgressIndicator(progress = { value.coerceIn(0f, 1f) }, modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)),
        color = color, trackColor = color.copy(alpha = .12f), drawStopIndicator = {})
}
@Composable fun MasteryRing(value: Float, modifier: Modifier = Modifier) {
    Box(modifier.size(60.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(progress = { value.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxSize(), color = AppColors.mastered,
            trackColor = AppColors.mastered.copy(alpha = .12f), strokeWidth = 5.dp)
        Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
    }
}
@Composable fun JourneyStep(number: Int, title: String, detail: String, complete: Boolean, active: Boolean, locked: Boolean = false, modifier: Modifier = Modifier) {
    val onHero = AppColors.onHero
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(14.dp))
            .background(if (complete) AppColors.mastered else if (active) onHero else onHero.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
            if (complete) Icon(Icons.Outlined.Check, null, tint = MaterialTheme.colorScheme.onPrimary)
            else if (locked) Icon(Icons.Outlined.Lock, null, Modifier.size(16.dp), tint = onHero.copy(alpha = .7f))
            else Text("0$number", color = if (active) AppColors.hero else onHero, style = MaterialTheme.typography.labelLarge)
        }
        Text(title, color = onHero, style = MaterialTheme.typography.labelLarge)
        Text(detail, color = onHero.copy(alpha = .75f), style = MaterialTheme.typography.labelMedium)
    }
}
@Composable fun DailyJourneyCard(plan: DailyPlanEntity, busy: Boolean, onContinue: () -> Unit) {
    val reviewDone = GameRules.reviewComplete(plan)
    val newDone = GameRules.newComplete(plan)
    Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = AppColors.hero)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Eyebrow("Hành trình hôm nay", AppColors.onHero.copy(alpha = .7f))
                Text(if (plan.perfect) "✦ PERFECT DAY" else "MỖI NGÀY, MỘT BƯỚC", color = AppColors.onHero.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall)
            }
            Text(if (plan.perfect) "Một ngày thật trọn vẹn." else if (plan.newDone.isNotEmpty()) "Tiếp tục thắp sáng\ntri thức của bạn." else "Một chút mỗi ngày.\nNhớ lâu hơn mỗi lần.",
                color = AppColors.onHero, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                JourneyStep(1, "Ôn tập", if (plan.reviewIds.isEmpty()) "Đã sẵn sàng" else "${plan.reviewDone.ids().size}/${plan.reviewIds.ids().size}", reviewDone, !reviewDone, modifier = Modifier.weight(1f))
                Text("—", color = AppColors.onHero.copy(alpha = .3f), modifier = Modifier.padding(top = 8.dp))
                JourneyStep(2, "Từ mới", "${plan.newDone.ids().size}/${plan.newIds.ids().size}", newDone && plan.newIds.isNotEmpty(), reviewDone && !newDone, modifier = Modifier.weight(1f))
                Text("—", color = AppColors.onHero.copy(alpha = .3f), modifier = Modifier.padding(top = 8.dp))
                JourneyStep(3, "Thử thách", if (plan.challengeComplete) "Hoàn thành" else if (GameRules.challengeUnlocked(plan)) "Đã mở" else "Học đủ 70%", plan.challengeComplete, newDone && !plan.challengeComplete, !GameRules.challengeUnlocked(plan), Modifier.weight(1f))
            }
            Button(onContinue, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.onHero, contentColor = AppColors.hero), shape = MaterialTheme.shapes.medium) {
                Text(if (plan.perfect) "Xem thành quả" else if (plan.newIds.isEmpty() && reviewDone && !GameRules.challengeUnlocked(plan)) "Khám phá thư viện" else "Tiếp tục học", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(20.dp))
            }
        }
    }
}
@Composable fun QuestRow(quest: QuestEntity) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(if (quest.rewarded) MaterialTheme.colorScheme.secondaryContainer else AppColors.surfaceElevated), contentAlignment = Alignment.Center) {
            Icon(if (quest.rewarded) Icons.Outlined.Check else Icons.Outlined.RadioButtonUnchecked, null, tint = if (quest.rewarded) AppColors.mastered else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(quest.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (!quest.rewarded) ProgressBar(quest.progress.toFloat() / quest.target)
            Text(if (quest.rewarded) "Hoàn thành" else "${quest.progress.coerceAtMost(quest.target)} / ${quest.target}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("+10 XP", color = AppColors.onReward, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(AppColors.reward.copy(alpha = .3f)).padding(7.dp), style = MaterialTheme.typography.labelSmall)
    }
}
@Composable fun VocabularyCard(word: Word, ielts: Boolean, onClick: () -> Unit) {
    val state = LearningEngine.state(word.progress, ielts)
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(state.symbol, style = MaterialTheme.typography.headlineSmall, color = if (state >= MemoryState.STRONG) AppColors.mastered else AppColors.primary, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(word.lexeme.defaultDisplay, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(word.sense.meaningVi, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${word.sense.partOfSpeech} · ${state.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CefrBadge(word.sense.cefrLevel)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
}
@Composable fun StatBlock(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun EmptyState(title: String, body: String, icon: ImageVector = Icons.Outlined.AutoAwesome) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(40.dp), tint = AppColors.primary)
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun ActionRow(title: String, subtitle: String, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(enabled = enabled, onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, Modifier.size(28.dp), tint = if (enabled) AppColors.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(if (enabled) Icons.AutoMirrored.Outlined.ArrowForward else Icons.Outlined.Lock, null, Modifier.size(20.dp))
    }
}
@Composable fun ConstellationView(words: List<Word>, ielts: Boolean, modifier: Modifier = Modifier, onWord: (String) -> Unit = {}) {
    val onHero = AppColors.onHero
    val teal = AppColors.mastered
    val amber = AppColors.reward
    val hero = AppColors.hero
    val stars = words.sortedBy { it.sense.id }.take(120)
    Canvas(modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.extraLarge).background(hero)
        .semantics { contentDescription = "Chòm sao: ${stars.size} từ, ${stars.count { it.progress.seen }} đã học. Danh sách từ ở bên dưới." }
        .pointerInput(stars) { detectTapGestures { tap ->
            stars.minByOrNull { word -> val p = LearningEngine.position(word.lexeme.id); hypot(tap.x - p.first * size.width, tap.y - p.second * size.height) }
                ?.let { onWord(it.sense.id) }
        } }) {
        val points = stars.map { w -> LearningEngine.position(w.lexeme.id).let { Offset(it.first * size.width, it.second * size.height) } }
        points.zipWithNext().forEach { (a, b) -> if ((a - b).getDistance() < size.width * .45f) drawLine(onHero.copy(alpha = .09f), a, b, 1.dp.toPx()) }
        stars.forEachIndexed { index, word ->
            val state = LearningEngine.state(word.progress, ielts)
            val point = points[index]
            val color = when (state) { MemoryState.MASTERED -> amber; MemoryState.STRONG -> teal; else -> onHero }
            val radius = when (state) { MemoryState.UNSEEN -> 4f; MemoryState.SEEN -> 2f; MemoryState.LEARNING -> 5f; MemoryState.STRONG -> 6f; MemoryState.MASTERED -> 8f }.dp.toPx()
            if (state >= MemoryState.STRONG) drawCircle(color.copy(alpha = .12f), radius * 2.5f, point)
            if (state == MemoryState.UNSEEN) drawCircle(color.copy(alpha = .35f), radius, point, style = Stroke(1.3.dp.toPx()))
            else if (state == MemoryState.SEEN) drawCircle(color.copy(alpha = .5f), radius, point)
            else {
                val path = Path()
                (0..7).forEach { i ->
                    val angle = i * PI / 4 - PI / 2
                    val r = if (i % 2 == 0) radius else radius * .34f
                    val x = point.x + (cos(angle) * r).toFloat(); val y = point.y + (sin(angle) * r).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                if (state == MemoryState.LEARNING) drawPath(path, color.copy(alpha = .75f), style = Stroke(1.3.dp.toPx())) else drawPath(path, color)
            }
        }
    }
}
@Composable fun RatingButton(rating: Rating, interval: Long, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val color = when (rating) { Rating.AGAIN -> AppColors.danger; Rating.HARD -> MaterialTheme.colorScheme.onSurfaceVariant; Rating.GOOD -> AppColors.primary; Rating.EASY -> AppColors.mastered }
    OutlinedButton(onClick, modifier.heightIn(min = 68.dp), enabled = enabled, shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp), border = BorderStroke(1.dp, color.copy(alpha = .4f))) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(rating.label, style = MaterialTheme.typography.labelLarge, color = color)
            Text(LearningEngine.intervalLabel(interval), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
@Composable fun WordMeaning(word: Word, ielts: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(word.sense.definitionEn, style = MaterialTheme.typography.bodyLarge)
        Text(word.sense.meaningVi, style = MaterialTheme.typography.bodyLarge, color = AppColors.primary)
        HorizontalDivider()
        Text(word.sense.exampleEn, style = MaterialTheme.typography.bodyLarge)
        if (word.sense.exampleVi.isNotBlank()) Text(word.sense.exampleVi, style = MaterialTheme.typography.bodyMedium)
        if (word.collocations.isNotEmpty()) { Eyebrow("Collocations"); Text(word.collocations.joinToString("\n"), style = MaterialTheme.typography.bodyMedium) }
        if (word.family.isNotEmpty()) { Eyebrow("Word family"); Text(word.family.joinToString(" · ")) }
        if (word.paraphrases.isNotEmpty()) { Eyebrow("Từ gần nghĩa"); Text(word.paraphrases.joinToString(" · ")); Text("Cách dùng có thể khác nhau theo ngữ cảnh.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text("${word.sense.cefrLevel} · ${word.sense.register}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (ielts) Text("IELTS relevance: ${if (word.lexeme.ieltsRelevanceScore >= .7f) "High" else "Medium"} · Ước tính biên tập\n${word.sources.mapNotNull { it.skill }.distinct().joinToString(" · ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun FlashCard(word: Word, revealed: Boolean, ielts: Boolean, reduceMotion: Boolean, onReveal: () -> Unit, onSpeak: () -> Unit, onBookmark: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                CefrBadge(word.sense.cefrLevel)
                IconToggleButton(word.progress.bookmarked, onCheckedChange = { onBookmark() }) { Icon(if (word.progress.bookmarked) Icons.Outlined.Star else Icons.Outlined.StarBorder, "Lưu từ") }
            }
            Text(word.lexeme.defaultDisplay, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp), textAlign = TextAlign.Center)
            Text(listOfNotNull(
                word.pronunciation?.ipaBr?.takeIf { it.isNotBlank() }?.let { "UK $it" },
                word.pronunciation?.ipaAm?.takeIf { it.isNotBlank() }?.let { "US $it" }
            ).joinToString(" · "), Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(word.sense.partOfSpeech, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(onSpeak, Modifier.align(Alignment.CenterHorizontally).size(52.dp)) { Icon(Icons.AutoMirrored.Outlined.VolumeUp, "Nghe phát âm tiếng Anh", tint = AppColors.primary) }
            AnimatedContent(revealed, transitionSpec = { fadeIn(tween(if (reduceMotion) 0 else 250)) togetherWith fadeOut(tween(if (reduceMotion) 0 else 220)) }, label = "reveal") { shown ->
                if (shown) WordMeaning(word, ielts)
                else Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Bạn còn nhớ nghĩa của từ này?", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onReveal, modifier = Modifier.heightIn(min = 48.dp)) { Text("Chạm để xem nghĩa") }
                }
            }
        }
    }
}
