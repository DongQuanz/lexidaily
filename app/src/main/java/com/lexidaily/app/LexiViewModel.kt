package com.lexidaily.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lexidaily.app.data.*
import com.lexidaily.app.domain.Rating
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class UiState(val data: AppSnapshot? = null, val session: LearningSession? = null,
    val loading: Boolean = true, val busy: Boolean = false, val error: String? = null)
class LexiViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LexiApplication
    private val repo = app.repository
    private val mutex = Mutex()
    private val mutable = MutableStateFlow(UiState())
    val state = mutable.asStateFlow()
    init { refresh(true) }
    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            mutex.withLock {
                mutable.value = mutable.value.copy(busy = true)
                try {
                    withContext(Dispatchers.IO) { block() }
                    val refreshed = withContext(Dispatchers.IO) { repo.snapshot() }
                    mutable.value = mutable.value.copy(data = refreshed, loading = false, error = null)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    mutable.value = mutable.value.copy(loading = false, error = e.message ?: "Không thể hoàn thành thao tác.")
                } finally { mutable.value = mutable.value.copy(busy = false) }
            }
        }
    }
    fun refresh(resume: Boolean = false) = action {
        SeedImporter(app, app.database).importIfNeeded()
        repo.initialize()
        if (resume) mutable.value = mutable.value.copy(session = repo.resume())
        else if (mutable.value.session?.session?.day != java.time.LocalDate.now().toString()) mutable.value = mutable.value.copy(session = null)
    }
    fun save(profile: UserProfile) = action { repo.savePreferences(profile); ReminderWorker.schedule(app, profile.reminders) }
    fun start(kind: String, scope: String = "") = action {
        mutable.value = mutable.value.copy(session = repo.start(kind, scope))
    }
    fun reveal() = action {
        mutable.value.session?.session?.id?.let { repo.reveal(it); mutable.value = mutable.value.copy(session = repo.loadSession(it)) }
    }
    fun draft(value: String) {
        val id = mutable.value.session?.session?.id ?: return
        viewModelScope.launch(Dispatchers.IO) { mutex.withLock { repo.draft(id, value) } }
    }
    fun answer(value: String = "", rating: Rating? = null, responseMs: Long = 0) = action {
        mutable.value.session?.session?.id?.let { mutable.value = mutable.value.copy(session = repo.answer(it, value, rating, responseMs)) }
    }
    fun next() = action {
        mutable.value.session?.session?.id?.let { mutable.value = mutable.value.copy(session = repo.next(it)) }
    }
    fun closeSession() { mutable.value = mutable.value.copy(session = null) }
    fun bookmark(id: String) = action { repo.bookmark(id) }
    fun clearError() { mutable.value = mutable.value.copy(error = null) }
}
