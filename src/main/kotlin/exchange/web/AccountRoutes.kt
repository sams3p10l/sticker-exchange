package exchange.web

import exchange.domain.AppError
import exchange.domain.Result
import exchange.usecase.ChangePassword
import exchange.usecase.ChangePasswordUseCase
import exchange.usecase.FindUserByUsernameUseCase
import exchange.usecase.LogIn
import exchange.usecase.LogInUseCase
import exchange.usecase.RegisterUserUseCase
import exchange.usecase.UpdateProfileUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("exchange.account")

fun Route.accountRoutes(
    registerUser: RegisterUserUseCase,
    logInUser: LogInUseCase,
    updateProfile: UpdateProfileUseCase,
    changePassword: ChangePasswordUseCase,
    findByUsername: FindUserByUsernameUseCase,
) {
    get("/") {
        val user = call.currentUser()
        call.respondEntry(
            user = user,
            registerForm = RegisterForm(),
            registerError = null,
            loginEmail = "",
            loginError = null,
            notice = noticeText(call.request.queryParameters["notice"]),
        )
    }

    post("/register") {
        if (call.currentUser() != null) {
            call.respondRedirect("/profile")
            return@post
        }
        val params = call.receiveParameters()
        val form = parseRegistration(params)
        when (val created = registerUser.execute(form.toRegisterUser(params["password"].orEmpty()))) {
            is Result.Success -> {
                call.signIn(created.data)
                log.info("Registered user {}", created.data.id)
                call.respondRedirect("/profile")
            }
            is Result.Failure -> call.respondEntry(
                user = null,
                registerForm = form,
                registerError = created.error.message(),
                loginEmail = "",
                loginError = null,
                notice = null,
                status = HttpStatusCode.BadRequest,
            )
        }
    }

    post("/login") {
        if (call.currentUser() != null) {
            call.respondRedirect("/profile")
            return@post
        }
        val params = call.receiveParameters()
        val email = params["email"]?.trim().orEmpty()
        val password = params["password"].orEmpty()
        when (val loggedIn = logInUser.execute(LogIn(email, password))) {
            is Result.Success -> {
                call.signIn(loggedIn.data)
                log.info("Login succeeded for user {}", loggedIn.data.id)
                call.respondRedirect("/profile")
            }
            is Result.Failure -> {
                log.info("Login failed")
                call.respondEntry(
                    user = null,
                    registerForm = RegisterForm(),
                    registerError = null,
                    loginEmail = email,
                    loginError = loggedIn.error.message(),
                    notice = null,
                    status = HttpStatusCode.BadRequest,
                )
            }
        }
    }

    post("/logout") {
        val user = call.currentUser()
        call.signOut()
        if (user != null) {
            log.info("Logged out user {}", user.id)
        }
        call.respondRedirect("/")
    }

    get("/profile") {
        val user = call.currentUser()
        if (user == null) {
            call.respondRedirect("/?notice=login_required")
            return@get
        }
        call.respondProfile(
            user = user,
            form = user.toProfileForm(),
            profileError = null,
            passwordError = null,
            notice = noticeText(call.request.queryParameters["notice"]),
        )
    }

    post("/profile") {
        val user = call.currentUser()
        if (user == null) {
            call.respondRedirect("/?notice=login_required")
            return@post
        }
        val params = call.receiveParameters()
        when (val saved = updateProfile.execute(parseProfile(user.id, params))) {
            is Result.Success -> {
                log.info("Profile updated for user {}", user.id)
                call.respondRedirect("/profile?notice=saved")
            }
            is Result.Failure -> if (saved.error == AppError.NotFound) {
                call.signOut()
                call.respondRedirect("/?notice=login_required")
            } else {
                call.respondProfile(
                    user = user,
                    form = profileForm(params),
                    profileError = saved.error.message(),
                    passwordError = null,
                    notice = null,
                    status = HttpStatusCode.BadRequest,
                )
            }
        }
    }

    post("/profile/password") {
        val user = call.currentUser()
        if (user == null) {
            call.respondRedirect("/?notice=login_required")
            return@post
        }
        val params = call.receiveParameters()
        val changed = changePassword.execute(
            ChangePassword(
                userId = user.id,
                currentPassword = params["currentPassword"].orEmpty(),
                newPassword = params["newPassword"].orEmpty(),
            ),
        )
        when (changed) {
            is Result.Success -> {
                log.info("Password changed for user {}", user.id)
                call.respondRedirect("/profile?notice=password_changed")
            }
            is Result.Failure -> if (changed.error == AppError.NotFound) {
                call.signOut()
                call.respondRedirect("/?notice=login_required")
            } else {
                log.info("Password change rejected for user {}", user.id)
                call.respondProfile(
                    user = user,
                    form = user.toProfileForm(),
                    profileError = null,
                    passwordError = changed.error.message(),
                    notice = null,
                    status = HttpStatusCode.BadRequest,
                )
            }
        }
    }

    route("/users/{username}") {
        get {
            val viewer = call.currentUser()
            if (viewer == null) {
                call.respondRedirect("/?notice=login_required")
                return@get
            }
            val username = call.parameters["username"].orEmpty()
            when (val found = findByUsername.execute(username)) {
                is Result.Success -> call.respondPublicProfile(viewerLoggedIn = true, user = found.data)
                is Result.Failure -> call.respondMissingUser(viewerLoggedIn = true)
            }
        }
    }
}
