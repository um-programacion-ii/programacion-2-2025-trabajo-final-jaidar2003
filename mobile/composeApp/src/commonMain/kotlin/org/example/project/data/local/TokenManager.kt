package org.example.project.data.local

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.russhwolf.settings.get

class TokenManager(private val settings: Settings) {
    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_EMAIL = "user_email"
    }

    var token: String?
        get() = settings.getStringOrNull(KEY_TOKEN)
        set(value) {
            if (value != null) {
                settings[KEY_TOKEN] = value
            } else {
                settings.remove(KEY_TOKEN)
            }
        }

    var email: String?
        get() = settings.getStringOrNull(KEY_EMAIL)
        set(value) {
            if (value != null) {
                settings[KEY_EMAIL] = value
            } else {
                settings.remove(KEY_EMAIL)
            }
        }
}
