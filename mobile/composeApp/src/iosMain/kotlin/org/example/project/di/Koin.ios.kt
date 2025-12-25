package org.example.project.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val platformModule = module {
    single<Settings> { 
        NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    }
}
