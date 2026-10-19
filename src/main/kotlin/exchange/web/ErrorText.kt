package exchange.web

import exchange.domain.AppError

fun AppError.message(): String = when (this) {
    AppError.InvalidEmail -> "Unesi ispravan email."
    AppError.PasswordTooShort -> "Lozinka mora da ima najmanje 8 znakova."
    AppError.PasswordTooLong -> "Lozinka može da ima najviše 72 znaka."
    AppError.InvalidUsername -> "Korisničko ime ima 3 do 30 znakova: latinica, brojevi i donja crta."
    AppError.FirstNameMissing -> "Unesi ime."
    AppError.FirstNameTooLong -> "Ime može da ima najviše 80 znakova."
    AppError.LastNameMissing -> "Unesi prezime."
    AppError.LastNameTooLong -> "Prezime može da ima najviše 80 znakova."
    AppError.CityMissing -> "Unesi grad."
    AppError.CityTooLong -> "Grad može da ima najviše 80 znakova."
    AppError.CountryInvalid -> "Izaberi državu."
    AppError.AboutTooLong -> "O meni može da ima najviše 2000 znakova."
    AppError.InactivityInvalid -> "Prag neaktivnosti je ceo broj, nula ili veći."
    AppError.EmailTaken -> "Ovaj email je zauzet."
    AppError.UsernameTaken -> "Ovo korisničko ime je zauzeto."
    AppError.InvalidCredentials -> "Pogrešan email ili lozinka."
    AppError.WrongPassword -> "Trenutna lozinka nije tačna."
    AppError.NotFound -> "Nije pronađeno."
    AppError.Rejected -> "Nije sačuvano."
}
