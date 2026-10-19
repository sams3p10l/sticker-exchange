package exchange

import exchange.domain.DEFAULT_INACTIVITY_DAYS
import exchange.domain.InsertedUser
import exchange.domain.NewUser
import exchange.domain.PasswordHasher
import exchange.domain.Role
import exchange.domain.UserRepository
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("exchange.dev-user")

private const val DEV_EMAIL = "admin@localhost"
private const val DEV_USERNAME = "admin"
private const val DEV_PASSWORD = "admin"

fun seedDevUser(users: UserRepository, passwords: PasswordHasher) {
    if (users.findByUsername(DEV_USERNAME) != null || users.findByEmail(DEV_EMAIL) != null) {
        log.info("Dev user already present")
        return
    }
    val inserted = users.insert(
        NewUser(
            email = DEV_EMAIL,
            passwordHash = passwords.hash(DEV_PASSWORD),
            username = DEV_USERNAME,
            firstName = "Admin",
            lastName = "Admin",
            city = "Beograd",
            country = "RS",
            byMail = true,
            inPerson = true,
            emailNotifications = false,
            inactivityDays = DEFAULT_INACTIVITY_DAYS,
            role = Role.ADMIN,
        ),
    )
    when (inserted) {
        is InsertedUser.Created -> log.info("Dev user ready {}", inserted.user.id)
        InsertedUser.EmailTaken -> log.info("Dev user email already present")
        InsertedUser.UsernameTaken -> log.info("Dev user name already present")
    }
}
