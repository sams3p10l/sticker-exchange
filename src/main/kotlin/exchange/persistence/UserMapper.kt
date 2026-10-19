package exchange.persistence

import exchange.domain.User
import java.time.OffsetDateTime
import java.util.UUID

internal data class UserEntity(
    val id: UUID,
    val email: String,
    val passwordHash: String,
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

internal class UserMapper : Mapper<UserEntity, User> {
    override fun map(input: UserEntity) = User(
        id = input.id,
        email = input.email,
        username = input.username,
        firstName = input.firstName,
        lastName = input.lastName,
        city = input.city,
        country = input.country,
        about = input.about,
        joinedAt = input.joinedAt,
        byMail = input.byMail,
        inPerson = input.inPerson,
        emailNotifications = input.emailNotifications,
        lastSeenAt = input.lastSeenAt,
        inactivityDays = input.inactivityDays,
    )
}
