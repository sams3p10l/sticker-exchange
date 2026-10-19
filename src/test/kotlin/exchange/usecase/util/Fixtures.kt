package exchange.usecase.util

import exchange.domain.AlbumProgress
import exchange.domain.Role
import exchange.domain.User
import exchange.usecase.RegisterUser
import java.time.OffsetDateTime
import java.util.UUID

fun user(id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")) = User(
    id = id,
    email = "ana@example.com",
    username = "ana",
    firstName = "Ana",
    lastName = "Anic",
    city = "Beograd",
    country = "RS",
    about = null,
    joinedAt = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
    byMail = true,
    inPerson = true,
    emailNotifications = false,
    lastSeenAt = OffsetDateTime.parse("2026-01-01T00:00:00Z"),
    inactivityDays = 7,
    role = Role.USER,
)

fun registration(password: String = "lozinka1") = RegisterUser(
    email = "Ana@Example.com",
    password = password,
    username = "ana",
    firstName = "Ana",
    lastName = "Anic",
    city = "Beograd",
    country = "RS",
    byMail = true,
    inPerson = false,
)

fun progress() = AlbumProgress(
    total = 3,
    owned = 1,
    missing = 2,
    mineWanted = 0,
    collectors = 1,
    offered = 0,
    wanted = 0,
)
