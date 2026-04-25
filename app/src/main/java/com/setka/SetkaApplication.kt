package com.setka

import android.app.Application
import com.setka.util.CrashHandler

class SetkaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.init(this)
    }
}
