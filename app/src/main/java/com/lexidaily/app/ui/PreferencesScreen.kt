package com.lexidaily.app.ui

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

@Composable fun PreferencesScreen(data: AppSnapshot, onboarding: Boolean, busy: Boolean, onSave: (UserProfile) -> Unit) {
    val p = data.profile
    var step by rememberSaveable { mutableIntStateOf(if (onboarding) 0 else 1) }
    var mode by rememberSaveable { mutableStateOf(p.mode) }
    var goal by rememberSaveable { mutableIntStateOf(p.dailyGoal) }
    var cefr by rememberSaveable { mutableStateOf(p.minCefr) }
    var band by rememberSaveable { mutableStateOf(p.targetBand) }
    var priority by rememberSaveable { mutableStateOf(p.priority) }
    var topicMode by rememberSaveable { mutableStateOf(p.topicMode) }
    var topic by rememberSaveable { mutableStateOf(p.preferredTopic) }
    var theme by rememberSaveable { mutableStateOf(p.theme) }
    var haptics by rememberSaveable { mutableStateOf(p.haptics) }
    var motion by rememberSaveable { mutableStateOf(p.reduceMotion) }
    var reminders by rememberSaveable { mutableStateOf(p.reminders) }
    Page {
        if (onboarding) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("LexiDaily ✦", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.primary)
                Text("0${step + 1} / 02", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(20.dp))
        }
        Text(if (step == 0) "Bạn đang học\nđể làm gì?" else if (onboarding) "Một nhịp học\nphù hợp với bạn." else "Theo cách của bạn", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text(if (step == 0) "Một mục tiêu rõ ràng. Một hành trình dành riêng cho bạn." else "Bạn có thể điều chỉnh các lựa chọn này bất cứ lúc nào.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (step == 0) {
            listOf("GENERAL" to "General English", "IELTS" to "IELTS").forEach { (key, title) ->
                OutlinedCard(onClick = { mode = key }, colors = CardDefaults.outlinedCardColors(containerColor = if (mode == key) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface), shape = MaterialTheme.shapes.large) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(if (key == "GENERAL") Icons.Outlined.Public else Icons.Outlined.School, null, tint = AppColors.primary)
                        Column(Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleLarge)
                            Text(if (key == "GENERAL") "Tự tin với tiếng Anh mỗi ngày" else "Từ vựng để hiểu sâu và dùng đúng", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RadioButton(mode == key, { mode = key })
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Tiếp tục") { step = 1 }
        } else {
            if (!onboarding) { Eyebrow("Mục tiêu học"); ChoiceStrip(listOf("GENERAL", "IELTS"), mode) { mode = it } }
            if (mode == "IELTS") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Target Band")
                    ChoiceStrip(listOf("6.0", "6.5", "7.0", "7.5", "8.0+"), band) { band = it }
                    Text("Mục tiêu điều chỉnh độ sâu, ngữ cảnh và cách luyện nhớ. Từ vựng không được gắn điểm band chính thức.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column { SectionTitle("Kỹ năng ưu tiên"); ChoiceStrip(listOf("Balanced", "Academic Reading", "Writing", "Speaking", "Listening"), priority) { priority = it } }
            } else {
                Column { SectionTitle("CEFR tối thiểu"); ChoiceStrip(listOf("B1", "B2", "C1", "C2"), cefr) { cefr = it } }
            }
            Column { SectionTitle("Từ mới mỗi ngày"); ChoiceStrip(listOf("10", "15", "20", "30"), goal.toString()) { goal = it.toInt() } }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Nhịp chủ đề")
                ChoiceStrip(listOf("Mỗi ngày một chủ đề", "Chọn một chủ đề"), if (topicMode == "DAILY") "Mỗi ngày một chủ đề" else "Chọn một chủ đề") { topicMode = if (it == "Chọn một chủ đề") "FIXED" else "DAILY" }
                if (topicMode == "FIXED") ChoiceStrip(data.topics.filter { t -> data.words.any { t.id in it.topics } }.map { it.name }, data.topics.find { it.id == topic }?.name.orEmpty()) { name -> topic = data.topics.first { it.name == name }.id }
                Text("Khi bộ từ mẫu không đủ, hành trình dùng số từ thực tế còn lại.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!onboarding) {
                HorizontalDivider()
                Column { SectionTitle("Giao diện"); ChoiceStrip(listOf("SYSTEM", "LIGHT", "DARK"), theme) { theme = it } }
                SettingSwitch("Phản hồi rung", "Xác nhận nhẹ khi học đúng và hoàn thành mục tiêu", haptics) { haptics = it }
                SettingSwitch("Giảm chuyển động", "Rút gọn hiệu ứng khi học", motion) { motion = it }
                SettingSwitch("Nhắc học hằng ngày", "Nhắc theo ngữ cảnh trong khung 08:00–20:00", reminders) { reminders = it }
            }
            PrimaryButton(if (onboarding) "Bắt đầu hành trình" else "Lưu cài đặt", enabled = !busy) {
                onSave(p.copy(onboarded = true, mode = mode, dailyGoal = goal, minCefr = cefr, targetBand = band,
                    priority = priority, topicMode = topicMode, preferredTopic = topic, theme = theme,
                    haptics = haptics, reduceMotion = motion, reminders = reminders))
            }
            if (onboarding) TextButton({ step = 0 }) { Text("‹ Thay đổi mục tiêu") }
        }
    }
}
@Composable private fun SettingSwitch(title: String, description: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(value, onChange)
    }
}
