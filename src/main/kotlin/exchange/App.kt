package exchange

import exchange.persistence.ExposedAlbumRepository
import exchange.persistence.ExposedUserRepository
import exchange.security.BcryptPasswordHasher
import exchange.usecase.AddAlbumUseCase
import exchange.usecase.ChangePasswordUseCase
import exchange.usecase.FindUserByUsernameUseCase
import exchange.usecase.FindUserUseCase
import exchange.usecase.GetAlbumBoardUseCase
import exchange.usecase.ListCollectionsUseCase
import exchange.usecase.LogInUseCase
import exchange.usecase.RegisterUserUseCase
import exchange.usecase.RemoveAlbumUseCase
import exchange.usecase.UpdateProfileUseCase
import exchange.usecase.UpdateStickerUseCase
import exchange.web.accountRoutes
import exchange.web.albumRoutes
import exchange.web.installAccountSessions
import io.ktor.http.ContentType
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

private const val PORT = 43123
private val log = LoggerFactory.getLogger("exchange.app")

fun main() {
    val config = DbConfig.fromEnv()
    val started = connectAndMigrate(config)
    if (started.ran) {
        log.info("Migration applied (version {})", started.version)
    } else {
        log.info("Migration did not apply (version {})", started.version ?: "none")
    }
    val users = ExposedUserRepository()
    val albums = ExposedAlbumRepository()
    val passwords = BcryptPasswordHasher()
    seedTrialAlbum(albums)
    embeddedServer(Netty, host = "0.0.0.0", port = PORT) {
        installAccountSessions(FindUserUseCase(users))
        routing {
            staticResources("/static", "static")
            get("/health") {
                val status = readMigrationStatus()
                call.respondText(healthText(status), ContentType.parse("text/plain; charset=UTF-8"))
            }
            accountRoutes(
                registerUser = RegisterUserUseCase(users, passwords),
                logInUser = LogInUseCase(users, passwords),
                updateProfile = UpdateProfileUseCase(users),
                changePassword = ChangePasswordUseCase(users, passwords),
                findByUsername = FindUserByUsernameUseCase(users),
            )
            albumRoutes(
                listCollections = ListCollectionsUseCase(albums),
                getAlbum = GetAlbumBoardUseCase(albums),
                addAlbum = AddAlbumUseCase(albums),
                removeAlbum = RemoveAlbumUseCase(albums),
                updateSticker = UpdateStickerUseCase(albums),
            )
        }
    }.start(wait = true)
}

private fun healthText(status: MigrationStatus): String {
    val migration = if (status.ran) "applied" else "not applied"
    val version = status.version ?: "none"
    return "status: ok\nmigration: $migration\nversion: $version\n"
}
