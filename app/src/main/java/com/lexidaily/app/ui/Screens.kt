package com.lexidaily.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lexidaily.app.data.*
import com.lexidaily.app.domain.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

@Composable fun HomeScreen(data: AppSnapshot, busy: Boolean, onContinue: () -> Unit, onTopic: (String) -> Unit, onStart: (String) -> Unit) {
    val plan = data.plan
    val topic = data.topics.find { it.id == plan.topicId }?.name ?: "Vocabulary"
    val topicWords = data.words.filter { plan.topicId in it.topics }
    val trouble = data.words.count { LearningEngine.trouble(it.progress) }
    Page {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(when (LocalTime.now().hour) { in 5..11 -> "Chào buổi sáng"; in 12..17 -> "Chào buổi chiều"; else -> "Chào buổi tối" }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Học một chút,\nđi thật xa.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                BadgeLabel("🔥 ${data.profile.streak} ngày", AppColors.reward.copy(alpha = .2f), AppColors.onReward)
            }
        }
        DailyJourneyCard(plan, busy, onContinue)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Eyebrow("Chủ đề hôm nay")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(topic, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("${plan.newIds.ids().size} từ · ${if (data.profile.mode == "IELTS") "IELTS · ${data.profile.priority}" else "General English · ${data.profile.minCefr}+"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Outlined.Spa, null, Modifier.size(36.dp), tint = AppColors.mastered)
            }
            Text("Mỗi từ là một kết nối mới với thế giới.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
        Column {
            SectionTitle("Nhiệm vụ hôm nay", if (data.quests.isEmpty()) null else "${data.quests.count { it.rewarded }}/${data.quests.size}")
            if (data.quests.isEmpty()) Text("Nhiệm vụ sẽ xuất hiện khi có từ phù hợp trong hành trình.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            data.quests.forEach { QuestRow(it) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Chòm sao từ vựng", "Khám phá") { onTopic(plan.topicId) }
            ConstellationView(topicWords, data.profile.mode == "IELTS", onWord = { onTopic(plan.topicId) })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(topic, fontWeight = FontWeight.Medium)
                Text("${topicWords.count { it.progress.seen }} / ${topicWords.size} đã khám phá", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        HorizontalDivider()
        Column {
            ActionRow("Từ cần củng cố", "$trouble từ cần bạn quan tâm", Icons.Outlined.Build, trouble > 0) { onStart("REPAIR") }
            ActionRow("Sẵn sàng ôn tập", "${plan.reviewIds.ids().size - plan.reviewDone.ids().size} từ đến hạn", Icons.Outlined.History) { onStart("REVIEW") }
        }
        Text("BỘ TỪ MẪU · Nội dung biên soạn riêng", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun LearnScreen(data: AppSnapshot, onStart: (String, String) -> Unit) {
    val plan = data.plan
    Page {
        Text("Một hành trình,\nba bước tiến.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text("Ôn đúng lúc. Khám phá từ mới. Tự nhớ để dùng được.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column {
            ActionRow("01 · Ôn tập", "${plan.reviewDone.ids().size} / ${plan.reviewIds.ids().size} từ đến hạn", Icons.Outlined.History) { onStart("REVIEW", "") }
            HorizontalDivider()
            ActionRow("02 · Từ mới", "${plan.newDone.ids().size} / ${plan.newIds.ids().size} từ hôm nay", Icons.Outlined.AutoStories, plan.newIds.isNotEmpty()) { onStart("NEW", "") }
            HorizontalDivider()
            ActionRow("03 · Thử thách", if (GameRules.challengeUnlocked(plan)) "5–10 câu hỏi theo điểm yếu của bạn" else "Mở sau khi học đủ 70% từ mới", Icons.Outlined.Bolt, GameRules.challengeUnlocked(plan)) { onStart("CHALLENGE", "") }
        }
        SectionTitle("Củng cố trí nhớ")
        ActionRow("Repair session", "Nhận biết → ngữ cảnh → tự nhớ", Icons.Outlined.Build,
            data.words.any { LearningEngine.trouble(it.progress) }) { onStart("REPAIR", "") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Checkpoint")
            if (data.checkpoints.isEmpty()) Text("Khám phá 25% một chủ đề hoặc 50 từ để mở bài kiểm tra kiến thức.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            data.checkpoints.forEach { cp ->
                val topic = data.topics.find { it.id == cp.topicId }?.name ?: "Tổng hợp"
                ActionRow("$topic · ${cp.threshold}${if (cp.topicId.isEmpty()) " từ" else "%"}",
                    if (cp.passed) "${cp.seal} Seal · ${(cp.bestAccuracy * 100).toInt()}%" else "Bài kiểm tra kiến thức · 15–20 câu khi đủ từ", Icons.Outlined.WorkspacePremium) { onStart("CHECKPOINT", cp.id) }
            }
        }
    }
}

@Composable fun LibraryScreen(data: AppSnapshot, onWord: (String) -> Unit, onTopic: (String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var stateFilter by rememberSaveable { mutableStateOf("Tất cả") }
    var cefrFilter by rememberSaveable { mutableStateOf("Tất cả CEFR") }
    var skillFilter by rememberSaveable { mutableStateOf("Mọi kỹ năng") }
    var topicFilter by rememberSaveable { mutableStateOf("Mọi chủ đề") }
    var packFilter by rememberSaveable { mutableStateOf("") }
    Page {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Thư viện của bạn", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("Từ đơn lẻ thành cả một hệ tri thức.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PrimaryTabRow(selectedTabIndex = tab) {
            listOf("Từ vựng", "Chủ đề", "IELTS Packs").forEachIndexed { index, title -> Tab(tab == index, { tab = index; packFilter = "" }, text = { Text(title) }) }
        }
        if (tab == 1) {
            data.topics.forEach { topic ->
                val words = data.words.filter { topic.id in it.topics }
                Row(Modifier.fillMaxWidth().clickable { onTopic(topic.id) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MasteryRing(words.count { it.progress.seen }.toFloat() / words.size.coerceAtLeast(1))
                    Column(Modifier.weight(1f)) {
                        Text(topic.name, style = MaterialTheme.typography.titleMedium)
                        Text("${words.count { it.progress.seen }} / ${words.size} từ đã khám phá", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.ChevronRight, "Mở chòm sao ${topic.name}")
                }
            }
        } else if (tab == 2 && packFilter.isEmpty()) {
            data.packs.filter { it.id != "general" }.forEach { pack ->
                val ids = data.packLinks.filter { it.packId == pack.id }.map { it.lexemeId }
                val members = data.words.filter { it.lexeme.id in ids }
                Card(onClick = { packFilter = pack.id }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(pack.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(pack.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ProgressBar(members.count { it.progress.seen }.toFloat() / members.size.coerceAtLeast(1))
                        Text("${members.count { it.progress.seen }} / ${members.size} từ", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            SectionTitle("Nguồn dữ liệu")
            data.sources.forEach { source ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(source.name, fontWeight = FontWeight.Medium)
                    SourceBadge(if (source.isBundled) "Có trong ứng dụng" else "Chưa kèm bộ dữ liệu")
                    Text(source.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            if (packFilter.isNotEmpty()) TextButton({ packFilter = "" }) { Text("‹ Tất cả bộ từ") }
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Tìm từ hoặc nghĩa tiếng Việt") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = MaterialTheme.shapes.medium)
            ChoiceStrip(listOf("Tất cả", "Đã học", "Đã lưu", "Từ khó", "Thành thạo"), stateFilter) { stateFilter = it }
            ChoiceStrip(listOf("Tất cả CEFR", "B1", "B2", "C1", "C2"), cefrFilter) { cefrFilter = it }
            ChoiceStrip(listOf("Mọi kỹ năng", "Reading", "Writing", "Speaking", "Listening"), skillFilter) { skillFilter = it }
            ChoiceStrip(listOf("Mọi chủ đề") + data.topics.filter { t -> data.words.any { t.id in it.topics } }.map { it.name }, topicFilter) { topicFilter = it }
            val ids = data.packLinks.filter { it.packId == packFilter }.map { it.lexemeId }
            val filtered = data.words.filter { w ->
                (packFilter.isEmpty() || w.lexeme.id in ids) &&
                (query.isBlank() || w.lexeme.lemma.contains(query, true) || w.sense.meaningVi.contains(query, true)) &&
                (cefrFilter == "Tất cả CEFR" || w.sense.cefrLevel == cefrFilter) &&
                (skillFilter == "Mọi kỹ năng" || w.sources.any { it.skill == skillFilter }) &&
                (topicFilter == "Mọi chủ đề" || data.topics.any { it.name == topicFilter && it.id in w.topics }) &&
                when (stateFilter) { "Đã học" -> w.progress.seen; "Đã lưu" -> w.progress.bookmarked; "Từ khó" -> LearningEngine.trouble(w.progress); "Thành thạo" -> LearningEngine.state(w.progress, data.profile.mode == "IELTS") == MemoryState.MASTERED; else -> true }
            }
            if (filtered.isEmpty()) EmptyState("Chưa có từ phù hợp", "Thử một bộ lọc khác hoặc bắt đầu hành trình hôm nay.")
            else Column {
                Eyebrow("${filtered.size} từ")
                filtered.forEach { word -> VocabularyCard(word, data.profile.mode == "IELTS") { onWord(word.sense.id) } }
            }
        }
    }
}

@Composable fun ProgressScreen(data: AppSnapshot, onTopic: (String) -> Unit) {
    val p = data.profile
    val weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
    val week = data.plans.filter { it.day >= weekStart }
    val reviews = data.history.filter { it.kind !in listOf("EXPOSURE", "FLASHCARD") && it.scheduled }
    val productive = reviews.filter { it.kind in listOf("TYPING", "CLOZE") }
    fun percent(h: List<ReviewHistoryEntity>) = if (h.isEmpty()) "—" else "${h.count { it.correct } * 100 / h.size}%"
    val level = GameRules.level(p.lifetimeLearningXp)
    Page {
        Text("Nhìn lại để tiến xa", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Eyebrow("Sự bền bỉ của bạn", MaterialTheme.colorScheme.onPrimaryContainer)
                Text("🔥 ${p.streak} ngày", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                Text("Level $level · ${GameRules.title(level)}", color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("${p.lifetimeLearningXp} XP học tập · ${p.graceTokens}/2 Grace Days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                ProgressBar((p.lifetimeLearningXp - (level - 1) * (level - 1) * 100).toFloat() / ((level * level - (level - 1) * (level - 1)) * 100))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionTitle("Tuần này")
            val days = week.count { it.newDone.isNotEmpty() || it.reviewDone.isNotEmpty() }
            Text("$days / 7 ngày có hoạt động học")
            ProgressBar(days / 7f)
            Row(Modifier.fillMaxWidth()) { StatBlock(percent(reviews), "Ghi nhớ khi ôn", Modifier.weight(1f)); StatBlock(percent(productive), "Tự nhớ từ", Modifier.weight(1f)) }
            Text("Chỉ tính câu hỏi có đáp án, không tính tự đánh giá flashcard.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle("Vốn từ đang lớn lên")
            MemoryState.entries.forEach { state ->
                val count = data.words.count { LearningEngine.state(it.progress, p.mode == "IELTS") == state }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${state.symbol}  ${state.label}"); Text("$count", fontWeight = FontWeight.SemiBold) }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Mục tiêu tuần")
            val count = week.sumOf { it.newDone.ids().size }
            Text("Từ mới · $count / ${p.dailyGoal * 5}")
            ProgressBar(count.toFloat() / (p.dailyGoal * 5))
            Text("Hành trình · ${week.count { it.journeyRewarded }} / 5")
            ProgressBar(week.count { it.journeyRewarded } / 5f)
            Text("Mục tiêu độ chính xác ôn tập: 80% · +30 XP khi đạt cả ba.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column {
            SectionTitle("Chòm sao của bạn")
            data.topics.filter { t -> data.words.any { t.id in it.topics } }.forEach { topic ->
                val words = data.words.filter { topic.id in it.topics }
                val strong = words.count { LearningEngine.state(it.progress, p.mode == "IELTS") >= MemoryState.STRONG }
                ActionRow(topic.name, "$strong / ${words.size} từ vững hoặc thành thạo", Icons.Outlined.AutoAwesome) { onTopic(topic.id) }
            }
        }
        if (p.mode == "IELTS") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("IELTS · Target ${p.targetBand}")
                listOf("B2", "C1").forEach { levelName ->
                    val words = data.words.filter { it.sense.cefrLevel == levelName }
                    Text("$levelName · ${words.count { it.progress.seen }} / ${words.size} đã khám phá")
                    ProgressBar(words.count { it.progress.seen }.toFloat() / words.size.coerceAtLeast(1))
                }
                Text("CEFR và mức độ liên quan là ước tính biên tập; không phải điểm IELTS chính thức.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SectionTitle("Dấu mốc đáng nhớ")
        if (data.achievements.isEmpty()) Text("Ngôi sao thành thạo đầu tiên đang chờ bạn.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        data.achievements.forEach { Text("✦ ${it.title}") }
        data.checkpoints.filter { it.passed }.forEach { Text("✦ ${it.topicId} · ${it.seal} Seal") }
    }
}

@Composable fun ConstellationScreen(data: AppSnapshot, topicId: String, onWord: (String) -> Unit, onContinue: () -> Unit) {
    val words = data.words.filter { topicId in it.topics }
    val seen = words.count { it.progress.seen }
    val mastered = words.count { LearningEngine.state(it.progress, data.profile.mode == "IELTS") == MemoryState.MASTERED }
    Page {
        Text(data.topics.find { it.id == topicId }?.name ?: "Constellation", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        if (words.isEmpty()) EmptyState("Chòm sao đang chờ được khám phá", "Chủ đề này chưa có từ trong bộ dữ liệu mẫu.")
        else {
            ConstellationView(words, data.profile.mode == "IELTS", onWord = onWord)
            Row(Modifier.fillMaxWidth()) { StatBlock("$seen / ${words.size}", "Đã khám phá", Modifier.weight(1f)); StatBlock("$mastered", "Thành thạo", Modifier.weight(1f)) }
            ProgressBar(mastered.toFloat() / words.size)
            Text(MemoryState.entries.joinToString("  ") { "${it.symbol} ${it.label}" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryButton("Tiếp tục hành trình", onClick = onContinue)
            Column { words.forEach { w -> VocabularyCard(w, data.profile.mode == "IELTS") { onWord(w.sense.id) } } }
        }
    }
}

@Composable fun WordDetailScreen(data: AppSnapshot, id: String, onBookmark: () -> Unit, onSpeak: (String) -> Unit, onPractice: () -> Unit) {
    val word = data.words.find { it.sense.id == id } ?: return
    var sourceDetails by rememberSaveable(id) { mutableStateOf(false) }
    Page {
        FlashCard(word, true, data.profile.mode == "IELTS", data.profile.reduceMotion, {}, { onSpeak(word.lexeme.lemma) }, onBookmark)
        OutlinedButton(onPractice, Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = MaterialTheme.shapes.medium) { Text("Ôn từ này") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Bạn hiểu từ này đến đâu?")
            listOf("FORM" to word.progress.formStrength, "MEANING" to word.progress.recognitionStrength,
                "CONTEXT" to word.progress.contextStrength, "PRODUCTION" to word.progress.productionStrength).forEach { (name, strength) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Eyebrow(name); Text("${(strength * 100).toInt()}%", style = MaterialTheme.typography.labelMedium) }
                ProgressBar(strength)
            }
        }
        Text("Register: ${word.sense.register}\n${word.sense.commonError}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Nguồn & xuất xứ")
            Text(data.sources.find { it.id == word.lexeme.lexicalDataSource }?.name ?: word.lexeme.lexicalDataSource)
            Text("CEFR: ${word.lexeme.cefrSource}\nFrequency: ${word.lexeme.frequencySource}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton({ sourceDetails = !sourceDetails }) { Text(if (sourceDetails) "Ẩn chi tiết nguồn" else "Xem chi tiết nguồn") }
            if (sourceDetails) word.sources.forEach { occurrence ->
                val source = data.sources.find { it.id == occurrence.sourcePackId }
                Text(listOfNotNull(source?.name, source?.volume?.let { "Volume $it" }, occurrence.testNumber?.let { "Test $it" }, occurrence.skill, occurrence.section).joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                Text(occurrence.sourceLocator.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable fun ChoiceStrip(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { TopicChip(it, selected == it) { onSelect(it) } }
    }
}
