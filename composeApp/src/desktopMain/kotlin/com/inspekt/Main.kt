package com.inspekt

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.inspekt.di.appModule
import org.koin.compose.KoinApplication
import java.io.File

fun main() {
    val storageDir = File(System.getProperty("user.home"), ".inspekt/collections").also {
        it.mkdirs()
    }.absolutePath

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "InspeKt",
            icon = painterResource("icon.png"),
        ) {
            KoinApplication(application = {
                modules(appModule(storageDir))
            }) {
                App(isDesktop = true)
            }
        }
    }
}
