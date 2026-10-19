package exchange.domain

import java.time.OffsetDateTime
import java.util.UUID

const val DEFAULT_INACTIVITY_DAYS = 7

data class User(
    val id: UUID,
    val email: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val city: String,
    val country: String,
    val about: String?,
    val joinedAt: OffsetDateTime,
    val byMail: Boolean,
    val inPerson: Boolean,
    val emailNotifications: Boolean,
    val lastSeenAt: OffsetDateTime?,
    val inactivityDays: Int,
)

data class NewUser(
    val email: String,
    val passwordHash: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val city: String,
    val country: String,
    val byMail: Boolean,
    val inPerson: Boolean,
    val emailNotifications: Boolean,
    val inactivityDays: Int,
)

data class ProfileUpdate(
    val firstName: String,
    val lastName: String,
    val city: String,
    val country: String,
    val about: String?,
    val byMail: Boolean,
    val inPerson: Boolean,
    val emailNotifications: Boolean,
    val inactivityDays: Int,
)

sealed interface InsertedUser {
    data class Created(val user: User) : InsertedUser
    data object EmailTaken : InsertedUser
    data object UsernameTaken : InsertedUser
}

interface UserRepository {
    fun findById(id: UUID): User?
    fun findByEmail(email: String): User?
    fun findByUsername(username: String): User?
    fun passwordHash(userId: UUID): String?
    fun insert(user: NewUser): InsertedUser
    fun updateProfile(id: UUID, update: ProfileUpdate): User?
    fun updatePassword(id: UUID, passwordHash: String): Boolean
    fun recordLogin(id: UUID)
}

interface PasswordHasher {
    fun hash(password: String): String
    fun verify(password: String, hash: String): Boolean
}
