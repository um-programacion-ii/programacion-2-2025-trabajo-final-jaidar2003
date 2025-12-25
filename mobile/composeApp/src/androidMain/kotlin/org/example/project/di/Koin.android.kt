package org.example.project.di

import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single<Settings> { 
        SharedPreferencesSettings(
            androidContext().getSharedPreferences("app_preferences", 0)
        )
    }
}
