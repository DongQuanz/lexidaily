package com.lexidaily.app

import android.app.Application
import com.lexidaily.app.data.LexiDatabase
import com.lexidaily.app.data.LexiRepository

class LexiApplication : Application() {
    val database by lazy { LexiDatabase.open(this) }
    val repository by lazy { LexiRepository(database) }
}
