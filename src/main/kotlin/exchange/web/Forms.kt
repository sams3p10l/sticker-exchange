package exchange.web

import exchange.domain.DEFAULT_COUNTRY_CODE
import exchange.domain.User
import exchange.usecase.RegisterUser
import exchange.usecase.UpdateProfile
import io.ktor.http.Parameters
import java.util.UUID

data class RegisterForm(
    val email: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val city: String = "",
    val country: String = DEFAULT_COUNTRY_CODE,
    val byMail: Boolean = true,
    val inPerson: Boolean = true,
)

data class ProfileForm(
    val firstName: String,
    val lastName: String,
    val city: String,
    val country: String,
    val about: String,
    val byMail: Boolean,
    val inPerson: Boolean,
    val emailNotifications: Boolean,
    val inactivityDays: String,
)

fun User.toProfileForm(): ProfileForm = ProfileForm(
    firstName = firstName,
    lastName = lastName,
    city = city,
    country = country,
    about = about.orEmpty(),
    byMail = byMail,
    inPerson = inPerson,
    emailNotifications = emailNotifications,
    inactivityDays = inactivityDays.toString(),
)

fun parseRegistration(params: Parameters): RegisterForm {
    val country = params["country"]?.trim()?.uppercase().orEmpty().ifBlank { DEFAULT_COUNTRY_CODE }
    return RegisterForm(
        email = params.text("email"),
        username = params.text("username"),
        firstName = params.text("firstName"),
        lastName = params.text("lastName"),
        city = params.text("city"),
        country = country,
        byMail = params.flag("byMail"),
        inPerson = params.flag("inPerson"),
    )
}

fun RegisterForm.toRegisterUser(password: String) = RegisterUser(
    email = email,
    password = password,
    username = username,
    firstName = firstName,
    lastName = lastName,
    city = city,
    country = country,
    byMail = byMail,
    inPerson = inPerson,
)

fun parseProfile(userId: UUID, params: Parameters): UpdateProfile {
    val country = params["country"]?.trim()?.uppercase().orEmpty().ifBlank { DEFAULT_COUNTRY_CODE }
    return UpdateProfile(
        userId = userId,
        firstName = params.text("firstName"),
        lastName = params.text("lastName"),
        city = params.text("city"),
        country = country,
        about = params.text("about"),
        byMail = params.flag("byMail"),
        inPerson = params.flag("inPerson"),
        emailNotifications = params.flag("emailNotifications"),
        inactivityDays = params["inactivityDays"]?.trim().orEmpty(),
    )
}

fun profileForm(params: Parameters): ProfileForm {
    val country = params["country"]?.trim()?.uppercase().orEmpty().ifBlank { DEFAULT_COUNTRY_CODE }
    return ProfileForm(
        firstName = params.text("firstName"),
        lastName = params.text("lastName"),
        city = params.text("city"),
        country = country,
        about = params.text("about"),
        byMail = params.flag("byMail"),
        inPerson = params.flag("inPerson"),
        emailNotifications = params.flag("emailNotifications"),
        inactivityDays = params["inactivityDays"]?.trim().orEmpty(),
    )
}

private fun Parameters.text(name: String): String = this[name]?.trim().orEmpty()

private fun Parameters.flag(name: String): Boolean = this[name] == "true"
