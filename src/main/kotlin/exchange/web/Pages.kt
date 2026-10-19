package exchange.web

import exchange.domain.User
import exchange.domain.countries
import exchange.domain.countryName
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.html.respondHtml
import io.ktor.server.response.header
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.MAIN
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.dd
import kotlinx.html.div
import kotlinx.html.dl
import kotlinx.html.dt
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.head
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.lang
import kotlinx.html.link
import kotlinx.html.main
import kotlinx.html.meta
import kotlinx.html.option
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.select
import kotlinx.html.textArea
import kotlinx.html.title
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val zone = ZoneId.of("Europe/Belgrade")
private val dateOnly = DateTimeFormatter.ofPattern("d. M. yyyy.", Locale.forLanguageTag("sr-Latn"))
private val dateTime = DateTimeFormatter.ofPattern("d. M. yyyy. HH:mm", Locale.forLanguageTag("sr-Latn"))

fun noticeText(key: String?): String? = when (key) {
    "login_required" -> "Prijavi se da bi video ovu stranu."
    "saved" -> "Sačuvano."
    "password_changed" -> "Lozinka je promenjena."
    else -> null
}

suspend fun ApplicationCall.respondEntry(
    user: User?,
    registerForm: RegisterForm,
    registerError: String?,
    loginEmail: String,
    loginError: String?,
    notice: String?,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    respondLayout(status, "Razmena sličica", user != null) {
        h1 { +"Razmena sličica" }
        message(notice, "notice")
        if (user != null) {
            p { +"Prijavljen si kao ${user.username}." }
            p { a(href = "/collections") { +"Moje kolekcije" } }
        } else {
            h2 { +"Registracija" }
            message(registerError, "error")
            registrationForm(registerForm)
            h2 { +"Prijava" }
            message(loginError, "error")
            loginForm(loginEmail)
        }
    }
}

suspend fun ApplicationCall.respondProfile(
    user: User,
    form: ProfileForm,
    profileError: String?,
    passwordError: String?,
    notice: String?,
    status: HttpStatusCode = HttpStatusCode.OK,
) {
    respondLayout(status, "Moj profil", loggedIn = true) {
        h1 { +"Moj profil" }
        message(notice, "notice")
        message(profileError, "error")
        p(classes = "muted") { +"Email: ${user.email}" }
        p(classes = "muted") { +"Korisničko ime: ${user.username}" }
        profileForm(form)
        h2 { +"Lozinka" }
        message(passwordError, "error")
        passwordForm()
    }
}

suspend fun ApplicationCall.respondPublicProfile(viewerLoggedIn: Boolean, user: User) {
    respondLayout(HttpStatusCode.OK, user.username, viewerLoggedIn) {
        h1 { +user.username }
        dl {
            row("Korisničko ime", user.username)
            row("Ime", user.firstName)
            row("Grad", user.city)
            row("Država", countryName(user.country) ?: user.country)
            row("O meni", user.about ?: "Nije uneto.")
            row("Član od", user.joinedAt.atZoneSameInstant(zone).format(dateOnly))
            row("Poslednji ulazak", user.lastSeenAt?.let { formatStamp(it) } ?: "Još nema ulaska.")
            row("Prosek ocena", "Nema ocena")
        }
    }
}

suspend fun ApplicationCall.respondMissingUser(viewerLoggedIn: Boolean) {
    respondLayout(HttpStatusCode.NotFound, "Korisnik nije pronađen", viewerLoggedIn) {
        h1 { +"Korisnik nije pronađen" }
        p { +"Nema naloga sa tim korisničkim imenom." }
    }
}

internal suspend fun ApplicationCall.respondLayout(
    status: HttpStatusCode,
    pageTitle: String,
    loggedIn: Boolean,
    wide: Boolean = false,
    script: String? = null,
    block: MAIN.() -> Unit,
) {
    response.header("Cache-Control", "no-store")
    respondHtml(status) {
        lang = "sr"
        head {
            meta(charset = "utf-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1")
            title(pageTitle)
            link(href = "/static/style.css", rel = "stylesheet")
        }
        body {
            val fill: MAIN.() -> Unit = {
                div(classes = "nav") {
                    a(href = "/") { +"Razmena sličica" }
                    if (loggedIn) {
                        a(href = "/collections") { +"Moje kolekcije" }
                        a(href = "/profile") { +"Moj profil" }
                        form(action = "/logout", method = FormMethod.post, classes = "inline") {
                            button(type = ButtonType.submit, classes = "link") { +"Odjava" }
                        }
                    }
                }
                block()
            }
            if (wide) {
                main(classes = "wide") { fill() }
            } else {
                main { fill() }
            }
            if (script != null) {
                script(src = script) {}
            }
        }
    }
}

private fun FlowContent.message(text: String?, kind: String) {
    if (text != null) {
        p(classes = kind) { +text }
    }
}

private fun FlowContent.registrationForm(form: RegisterForm) {
    form(action = "/register", method = FormMethod.post) {
        textField("Email", "email", form.email, InputType.email, "email", maxLength = "254")
        passwordField("Lozinka", "password", "new-password")
        textField("Korisničko ime", "username", form.username, InputType.text, "username", maxLength = "30")
        textField("Ime", "firstName", form.firstName, InputType.text, "given-name")
        textField("Prezime", "lastName", form.lastName, InputType.text, "family-name")
        textField("Grad", "city", form.city, InputType.text, "address-level2")
        countryField(form.country)
        checkField("byMail", "Poštom", form.byMail)
        checkField("inPerson", "Uživo", form.inPerson)
        button(type = ButtonType.submit) { +"Registruj se" }
    }
}

private fun FlowContent.loginForm(email: String) {
    form(action = "/login", method = FormMethod.post) {
        textField("Email", "email", email, InputType.email, "email", maxLength = "254")
        passwordField("Lozinka", "password", "current-password")
        button(type = ButtonType.submit) { +"Prijavi se" }
    }
}

private fun FlowContent.profileForm(form: ProfileForm) {
    form(action = "/profile", method = FormMethod.post) {
        textField("Ime", "firstName", form.firstName, InputType.text, "given-name")
        textField("Prezime", "lastName", form.lastName, InputType.text, "family-name")
        textField("Grad", "city", form.city, InputType.text, "address-level2")
        countryField(form.country)
        label {
            +"O meni"
            textArea {
                name = "about"
                rows = "5"
                maxLength = "2000"
                +form.about
            }
        }
        checkField("byMail", "Poštom", form.byMail)
        checkField("inPerson", "Uživo", form.inPerson)
        checkField("emailNotifications", "Obavesti me emailom", form.emailNotifications)
        label {
            +"Prag neaktivnosti (dana)"
            input(type = InputType.number, name = "inactivityDays") {
                value = form.inactivityDays
                min = "0"
                step = "1"
                required = true
            }
        }
        p(classes = "hint") { +"Nula gasi filter. Podrazumevano je 7 dana." }
        button(type = ButtonType.submit) { +"Sačuvaj" }
    }
}

private fun FlowContent.passwordForm() {
    form(action = "/profile/password", method = FormMethod.post) {
        passwordField("Trenutna lozinka", "currentPassword", "current-password")
        passwordField("Nova lozinka", "newPassword", "new-password")
        button(type = ButtonType.submit) { +"Promeni lozinku" }
    }
}

private fun FlowContent.textField(
    label: String,
    name: String,
    value: String,
    type: InputType,
    autoComplete: String,
    maxLength: String = "80",
) {
    label {
        +label
        input(type = type, name = name) {
            this.value = value
            required = true
            this.maxLength = maxLength
            attributes["autocomplete"] = autoComplete
        }
    }
}

private fun FlowContent.passwordField(label: String, name: String, autoComplete: String) {
    label {
        +label
        input(type = InputType.password, name = name) {
            required = true
            maxLength = "72"
            attributes["autocomplete"] = autoComplete
        }
    }
}

private fun FlowContent.countryField(selected: String) {
    label {
        +"Država"
        select {
            name = "country"
            required = true
            countries.forEach { country ->
                option {
                    value = country.code
                    if (country.code == selected) this.selected = true
                    +country.name
                }
            }
        }
    }
}

private fun FlowContent.checkField(name: String, label: String, checked: Boolean) {
    label(classes = "check") {
        input(type = InputType.checkBox, name = name) {
            value = "true"
            if (checked) this.checked = true
        }
        +label
    }
}

private fun kotlinx.html.DL.row(label: String, value: String) {
    dt { +label }
    dd { +value }
}

private fun formatStamp(value: OffsetDateTime): String =
    value.atZoneSameInstant(zone).format(dateTime)
