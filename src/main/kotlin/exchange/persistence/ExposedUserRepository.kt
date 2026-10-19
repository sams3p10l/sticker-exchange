package exchange.persistence

import exchange.domain.InsertedUser
import exchange.domain.NewUser
import exchange.domain.ProfileUpdate
import exchange.domain.Role
import exchange.domain.User
import exchange.domain.UserRepository
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.sql.SQLException
import java.time.OffsetDateTime
import java.util.UUID

internal object Users : Table("users") {
    val id = javaUUID("id")
    val email = varchar("email", 254)
    val passwordHash = varchar("password_hash", 100)
    val username = varchar("username", 30)
    val firstName = varchar("first_name", 80)
    val lastName = varchar("last_name", 80)
    val city = varchar("city", 80)
    val country = varchar("country", 2)
    val about = text("about").nullable()
    val joinedAt = timestampWithTimeZone("joined_at")
    val byMail = bool("by_mail")
    val inPerson = bool("in_person")
    val emailNotifications = bool("email_notifications")
    val lastSeenAt = timestampWithTimeZone("last_seen_at").nullable()
    val inactivityDays = integer("inactivity_days")
    val role = varchar("role", 16)
    override val primaryKey = PrimaryKey(id)
}

class ExposedUserRepository : UserRepository {
    private val mapper = UserMapper()
    override fun findById(id: UUID): User? = transaction {
        Users.selectAll().where { Users.id eq id }.limit(1).firstOrNull()?.toUser()
    }

    override fun findByEmail(email: String): User? = transaction {
        Users.selectAll()
            .where { Users.email eq email.lowercase() }
            .limit(1)
            .firstOrNull()
            ?.toUser()
    }

    override fun findByUsername(username: String): User? = transaction {
        Users.selectAll()
            .where { Users.username.lowerCase() eq username.lowercase() }
            .limit(1)
            .firstOrNull()
            ?.toUser()
    }

    override fun passwordHash(userId: UUID): String? = transaction {
        Users.selectAll().where { Users.id eq userId }.limit(1).firstOrNull()?.get(Users.passwordHash)
    }

    override fun insert(user: NewUser): InsertedUser = try {
        transaction {
            if (emailExists(user.email)) return@transaction InsertedUser.EmailTaken
            if (usernameExists(user.username)) return@transaction InsertedUser.UsernameTaken
            val userId = UUID.randomUUID()
            val now = OffsetDateTime.now()
            Users.insert {
                it[id] = userId
                it[email] = user.email
                it[passwordHash] = user.passwordHash
                it[username] = user.username
                it[firstName] = user.firstName
                it[lastName] = user.lastName
                it[city] = user.city
                it[country] = user.country
                it[about] = null
                it[joinedAt] = now
                it[byMail] = user.byMail
                it[inPerson] = user.inPerson
                it[emailNotifications] = user.emailNotifications
                it[lastSeenAt] = now
                it[inactivityDays] = user.inactivityDays
                it[role] = user.role.stored
            }
            InsertedUser.Created(
                User(
                    id = userId,
                    email = user.email,
                    username = user.username,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    city = user.city,
                    country = user.country,
                    about = null,
                    joinedAt = now,
                    byMail = user.byMail,
                    inPerson = user.inPerson,
                    emailNotifications = user.emailNotifications,
                    lastSeenAt = now,
                    inactivityDays = user.inactivityDays,
                    role = user.role,
                ),
            )
        }
    } catch (error: ExposedSQLException) {
        when (uniqueIndex(error)) {
            "users_email_lower_idx" -> InsertedUser.EmailTaken
            "users_username_lower_idx" -> InsertedUser.UsernameTaken
            else -> throw error
        }
    }

    override fun updateProfile(id: UUID, update: ProfileUpdate): User? = transaction {
        val updated = Users.update({ Users.id eq id }) {
            it[firstName] = update.firstName
            it[lastName] = update.lastName
            it[city] = update.city
            it[country] = update.country
            it[about] = update.about
            it[byMail] = update.byMail
            it[inPerson] = update.inPerson
            it[emailNotifications] = update.emailNotifications
            it[inactivityDays] = update.inactivityDays
        }
        if (updated == 0) null else Users.selectAll().where { Users.id eq id }.limit(1).firstOrNull()?.toUser()
    }

    override fun updatePassword(id: UUID, passwordHash: String): Boolean = transaction {
        Users.update({ Users.id eq id }) {
            it[Users.passwordHash] = passwordHash
        } > 0
    }

    override fun recordLogin(id: UUID) {
        val now = OffsetDateTime.now()
        transaction {
            Users.update({ Users.id eq id }) {
                it[lastSeenAt] = now
            }
        }
    }

    private fun ResultRow.toUser(): User = mapper.map(
        UserEntity(
            id = this[Users.id],
            email = this[Users.email],
            passwordHash = this[Users.passwordHash],
            username = this[Users.username],
            firstName = this[Users.firstName],
            lastName = this[Users.lastName],
            city = this[Users.city],
            country = this[Users.country],
            about = this[Users.about],
            joinedAt = this[Users.joinedAt],
            byMail = this[Users.byMail],
            inPerson = this[Users.inPerson],
            emailNotifications = this[Users.emailNotifications],
            lastSeenAt = this[Users.lastSeenAt],
            inactivityDays = this[Users.inactivityDays],
            role = Role.fromStored(this[Users.role]),
        ),
    )
}

private fun emailExists(email: String): Boolean =
    Users.selectAll().where { Users.email eq email.lowercase() }.limit(1).firstOrNull() != null

private fun usernameExists(username: String): Boolean =
    Users.selectAll()
        .where { Users.username.lowerCase() eq username.lowercase() }
        .limit(1)
        .firstOrNull() != null

private fun uniqueIndex(error: ExposedSQLException): String? {
    val sql = error.cause as? SQLException ?: return null
    if (sql.sqlState != "23505") return null
    val message = sql.message.orEmpty()
    return when {
        "users_email_lower_idx" in message -> "users_email_lower_idx"
        "users_username_lower_idx" in message -> "users_username_lower_idx"
        else -> null
    }
}
