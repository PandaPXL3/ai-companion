package ai.localcompanion.app.profile

import android.content.Context

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * SharedPreferences-backed storage for the onboarding profile and the few
 * app settings that must survive restarts. Everything stays on-device;
 * nothing here is ever synced anywhere.
 */
class ProfileStore(context: Context) {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): UserProfile? {
        val userName = prefs.getString(KEY_USER_NAME, null) ?: return null
        return UserProfile(
            userName = userName,
            userAge = prefs.getInt(KEY_USER_AGE, 0),
            companionName = prefs.getString(KEY_COMPANION_NAME, "") ?: "",
            personalityShort = prefs.getString(KEY_PERSONALITY, "") ?: "",
            personaPrompt = prefs.getString(KEY_PERSONA_PROMPT, "") ?: ""
        )
    }

    fun save(profile: UserProfile) {
        prefs.edit()
            .putString(KEY_USER_NAME, profile.userName)
            .putInt(KEY_USER_AGE, profile.userAge)
            .putString(KEY_COMPANION_NAME, profile.companionName)
            .putString(KEY_PERSONALITY, profile.personalityShort)
            .putString(KEY_PERSONA_PROMPT, profile.personaPrompt)
            .apply()
    }

    /** Wipes the profile (Data → Reset). Settings like theme are kept. */
    fun clearProfile() {
        prefs.edit()
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_AGE)
            .remove(KEY_COMPANION_NAME)
            .remove(KEY_PERSONALITY)
            .remove(KEY_PERSONA_PROMPT)
            .apply()
    }

    fun themeMode(): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    private companion object {
        const val FILE = "profile"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_AGE = "user_age"
        const val KEY_COMPANION_NAME = "companion_name"
        const val KEY_PERSONALITY = "personality_short"
        const val KEY_PERSONA_PROMPT = "persona_prompt"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
