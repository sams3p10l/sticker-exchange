package exchange.usecase

import exchange.domain.AppError
import exchange.domain.isCountry

internal const val EMAIL_MAX = 254
internal const val PASSWORD_MIN = 8
internal const val PASSWORD_MAX = 72
internal const val NAME_MAX = 80
internal const val ABOUT_MAX = 2000

private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
private val usernamePattern = Regex("^[A-Za-z0-9_]{3,30}$")

internal fun emailError(email: String): AppError? =
    if (email.length in 1..EMAIL_MAX && emailPattern.matches(email)) null else AppError.InvalidEmail

internal fun passwordError(password: String): AppError? = when {
    password.length < PASSWORD_MIN -> AppError.PasswordTooShort
    password.length > PASSWORD_MAX -> AppError.PasswordTooLong
    else -> null
}

internal fun usernameError(username: String): AppError? =
    if (usernamePattern.matches(username)) null else AppError.InvalidUsername

internal fun personError(firstName: String, lastName: String, city: String, country: String): AppError? = when {
    firstName.isBlank() -> AppError.FirstNameMissing
    firstName.length > NAME_MAX -> AppError.FirstNameTooLong
    lastName.isBlank() -> AppError.LastNameMissing
    lastName.length > NAME_MAX -> AppError.LastNameTooLong
    city.isBlank() -> AppError.CityMissing
    city.length > NAME_MAX -> AppError.CityTooLong
    !isCountry(country) -> AppError.CountryInvalid
    else -> null
}
