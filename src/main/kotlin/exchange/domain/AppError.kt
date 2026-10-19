package exchange.domain

sealed interface AppError {
    data object InvalidEmail : AppError
    data object PasswordTooShort : AppError
    data object PasswordTooLong : AppError
    data object InvalidUsername : AppError
    data object FirstNameMissing : AppError
    data object FirstNameTooLong : AppError
    data object LastNameMissing : AppError
    data object LastNameTooLong : AppError
    data object CityMissing : AppError
    data object CityTooLong : AppError
    data object CountryInvalid : AppError
    data object AboutTooLong : AppError
    data object InactivityInvalid : AppError
    data object EmailTaken : AppError
    data object UsernameTaken : AppError
    data object InvalidCredentials : AppError
    data object WrongPassword : AppError
    data object NotFound : AppError
    data object Rejected : AppError
}
