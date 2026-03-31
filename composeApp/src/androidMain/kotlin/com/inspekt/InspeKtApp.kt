package com.inspekt

import android.app.Application
import com.inspekt.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.io.File

class InspeKtApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val storageDir = File(filesDir, "collections").also { it.mkdirs() }.absolutePath
        startKoin {
            androidContext(this@InspeKtApp)
            modules(appModule(storageDir))
        }
    }
}
