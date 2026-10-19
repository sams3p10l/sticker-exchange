package exchange.web

import exchange.domain.Result
import exchange.domain.User
import exchange.usecase.FindUserUseCase
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.sessions.SessionSerializer
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.util.AttributeKey
import org.slf4j.LoggerFactory
import java.util.UUID

private val log = LoggerFactory.getLogger("exchange.session")

private val sessionSigningKey =
    "local-dev-session-key-not-for-production-use".toByteArray(Charsets.UTF_8)

private val findUserKey = AttributeKey<FindUserUseCase>("findUser")

data class UserSession(val userId: String)

private object UserSessionSerializer : SessionSerializer<UserSession> {
    override fun serialize(session: UserSession): String = session.userId

    override fun deserialize(text: String): UserSession = UserSession(text)
}

fun Application.installAccountSessions(findUser: FindUserUseCase) {
    attributes.put(findUserKey, findUser)
    install(Sessions) {
        cookie<UserSession>("session") {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.secure = false
            cookie.extensions["SameSite"] = "Lax"
            serializer = UserSessionSerializer
            transform(SessionTransportTransformerMessageAuthentication(sessionSigningKey))
        }
    }
}

suspend fun ApplicationCall.currentUser(): User? {
    val session = try {
        sessions.get<UserSession>()
    } catch (error: Exception) {
        log.warn("Ignored unreadable session", error)
        sessions.clear<UserSession>()
        return null
    } ?: return null

    val id = runCatching { UUID.fromString(session.userId) }.getOrNull()
    if (id == null) {
        sessions.clear<UserSession>()
        return null
    }
    val found = application.attributes[findUserKey].execute(id)
    if (found is Result.Failure) {
        sessions.clear<UserSession>()
        return null
    }
    return found.data
}

fun ApplicationCall.signIn(user: User) {
    sessions.set(UserSession(user.id.toString()))
}

fun ApplicationCall.signOut() {
    sessions.clear<UserSession>()
}
