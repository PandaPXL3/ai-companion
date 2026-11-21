package ai.localcompanion.app.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingValidatorTest {

    private fun valid() = UserProfile(
        userName = "Manoj",
        userAge = 24,
        companionName = "Aira",
        personalityShort = "Cheerful, teasing, loves books"
    )

    @Test
    fun `valid profile returns no errors`() {
        assertTrue(OnboardingValidator.validate(valid()).isEmpty())
        assertTrue(OnboardingValidator.isValid(valid()))
    }

    @Test
    fun `age 17 is rejected with the 18 plus error`() {
        val errors = OnboardingValidator.validate(valid().copy(userAge = 17))
        assertEquals(listOf("You must be 18 or older to use this app."), errors)
    }

    @Test
    fun `age exactly 18 is accepted`() {
        assertTrue(OnboardingValidator.validate(valid().copy(userAge = 18)).isEmpty())
    }

    @Test
    fun `age over 120 is invalid`() {
        val errors = OnboardingValidator.validate(valid().copy(userAge = 121))
        assertEquals(listOf("Please enter a valid age."), errors)
    }

    @Test
    fun `blank user name is rejected`() {
        val errors = OnboardingValidator.validate(valid().copy(userName = "  "))
        assertEquals(listOf("Your name is required."), errors)
    }

    @Test
    fun `blank companion name is rejected`() {
        val errors = OnboardingValidator.validate(valid().copy(companionName = ""))
        assertEquals(listOf("Give your companion a name."), errors)
    }

    @Test
    fun `blank personality is rejected`() {
        val errors = OnboardingValidator.validate(valid().copy(personalityShort = ""))
        assertEquals(listOf("Describe her personality in a sentence or two."), errors)
    }

    @Test
    fun `personality of 281 chars is rejected, 280 is accepted`() {
        val tooLong = OnboardingValidator.validate(valid().copy(personalityShort = "x".repeat(281)))
        assertEquals(listOf("Keep the personality description under 280 characters."), tooLong)
        assertTrue(OnboardingValidator.validate(valid().copy(personalityShort = "x".repeat(280))).isEmpty())
    }

    @Test
    fun `multiple problems return multiple errors in field order`() {
        val errors = OnboardingValidator.validate(
            UserProfile(userName = "", userAge = 15, companionName = "", personalityShort = "")
        )
        assertEquals(
            listOf(
                "Your name is required.",
                "You must be 18 or older to use this app.",
                "Give your companion a name.",
                "Describe her personality in a sentence or two."
            ),
            errors
        )
    }
}
