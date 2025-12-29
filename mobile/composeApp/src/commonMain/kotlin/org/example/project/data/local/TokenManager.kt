package org.example.project.data.local

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.russhwolf.settings.get

class TokenManager(private val settings: Settings) {
    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_FIRST_NAME = "user_first_name"
        private const val KEY_LAST_NAME = "user_last_name"
        private const val KEY_NOMBRE_ALUMNO = "user_nombre_alumno"
        private const val KEY_PROYECTO = "user_proyecto"
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

    var firstName: String?
        get() = settings.getStringOrNull(KEY_FIRST_NAME)
        set(value) { if (value != null) settings[KEY_FIRST_NAME] = value else settings.remove(KEY_FIRST_NAME) }

    var lastName: String?
        get() = settings.getStringOrNull(KEY_LAST_NAME)
        set(value) { if (value != null) settings[KEY_LAST_NAME] = value else settings.remove(KEY_LAST_NAME) }

    var nombreAlumno: String?
        get() = settings.getStringOrNull(KEY_NOMBRE_ALUMNO)
        set(value) { if (value != null) settings[KEY_NOMBRE_ALUMNO] = value else settings.remove(KEY_NOMBRE_ALUMNO) }

    var proyecto: String?
        get() = settings.getStringOrNull(KEY_PROYECTO)
        set(value) { if (value != null) settings[KEY_PROYECTO] = value else settings.remove(KEY_PROYECTO) }

}
