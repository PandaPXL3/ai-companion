package ai.localcompanion.app.profile

/**
 * Validation for the onboarding form. The age rule is a hard gate, not a
 * warning: this app is 18+ and [validate] is what the UI checks before the
 * companion is created.
 */
object OnboardingValidator {

    const val MIN_AGE = 18
    const val MAX_AGE = 120
    const val MAX_PERSONALITY_CHARS = 280

    /** All applicable error messages, in field order. Empty list = valid. */
    fun validate(profile: UserProfile): List<String> {
        val errors = mutableListOf<String>()
        if (profile.userName.isBlank()) {
            errors += "Your name is required."
        }
        if (profile.userAge < MIN_AGE) {
            errors += "You must be 18 or older to use this app."
        } else if (profile.userAge > MAX_AGE) {
            errors += "Please enter a valid age."
        }
        if (profile.companionName.isBlank()) {
            errors += "Give your companion a name."
        }
        if (profile.personalityShort.isBlank()) {
            errors += "Describe her personality in a sentence or two."
        } else if (profile.personalityShort.length > MAX_PERSONALITY_CHARS) {
            errors += "Keep the personality description under 280 characters."
        }
        return errors
    }

    fun isValid(profile: UserProfile): Boolean = validate(profile).isEmpty()
}
