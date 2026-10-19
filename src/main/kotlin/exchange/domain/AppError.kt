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
    data object Forbidden : AppError
    data object PublisherMissing : AppError
    data object PublisherTooLong : AppError
    data object TitleMissing : AppError
    data object TitleTooLong : AppError
    data object SlugInvalid : AppError
    data object SlugTaken : AppError
    data object StickersMissing : AppError
    data object StickerCodeInvalid : AppError
    data object StickerCodeDuplicate : AppError
    data object StickerNameTooLong : AppError
    data object TooManyStickers : AppError
    data object ImageTooLarge : AppError
    data object ImageUnsupported : AppError
}
