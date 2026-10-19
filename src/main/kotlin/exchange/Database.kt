package exchange

import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.net.ConnectException
import java.sql.SQLException
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("exchange.database")

data class DbConfig(
    val host: String,
    val port: String,
    val name: String,
    val user: String,
    val password: String,
) {
    val jdbcUrl: String
        get() = "jdbc:postgresql://$host:$port/$name?connectTimeout=5"

    companion object {
        fun fromEnv(): DbConfig = DbConfig(
            host = System.getenv("DB_HOST") ?: "127.0.0.1",
            port = System.getenv("DB_PORT") ?: "5432",
            name = System.getenv("DB_NAME") ?: "razmena",
            user = System.getenv("DB_USER") ?: "razmena",
            password = System.getenv("DB_PASSWORD") ?: "razmena",
        )
    }
}

data class MigrationStatus(
    val ran: Boolean,
    val version: String?,
)

private object SchemaMarker : Table("schema_marker") {
    val id = integer("id")
    val description = text("description")
    override val primaryKey = PrimaryKey(id)
}

private object FlywaySchemaHistory : Table("flyway_schema_history") {
    val installedRank = integer("installed_rank")
    val version = varchar("version", 50).nullable()
    val success = bool("success")
    override val primaryKey = PrimaryKey(installedRank)
}

fun connectAndMigrate(config: DbConfig): MigrationStatus {
    val deadline = System.nanoTime() + STARTUP_WAIT_NANOS
    while (true) {
        try {
            Flyway.configure()
                .dataSource(config.jdbcUrl, config.user, config.password)
                .locations("classpath:db/migration")
                .load()
                .migrate()
            Database.connect(
                url = config.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = config.user,
                password = config.password,
            )
            return readMigrationStatus()
        } catch (error: Exception) {
            if (isDatabaseStarting(error) && System.nanoTime() < deadline) {
                Thread.sleep(500)
                continue
            }
            if (isDatabaseUnavailable(error)) {
                failDatabaseDown(error)
            }
            log.error("Database migration failed", error)
            exitProcess(1)
        }
    }
}

fun readMigrationStatus(): MigrationStatus = transaction {
    val applied = FlywaySchemaHistory
        .selectAll()
        .where { FlywaySchemaHistory.success eq true }
        .orderBy(FlywaySchemaHistory.installedRank to SortOrder.DESC)
        .limit(1)
        .firstOrNull()
    val marker = SchemaMarker.selectAll().count()
    val version = applied?.get(FlywaySchemaHistory.version)
    MigrationStatus(ran = applied != null && marker > 0 && version != null, version = version)
}

private const val STARTUP_WAIT_NANOS = 30_000_000_000L

private fun failDatabaseDown(error: Throwable): Nothing {
    val detail = generateSequence(error) { it.cause }.last().message ?: error.message
    log.error(
        """
        Postgres is not available${detail?.let { " ($it)" } ?: ""}.

        Start the database, then the app:

            docker compose up -d
            ./gradlew run
        """.trimIndent(),
    )
    exitProcess(1)
}

private fun isDatabaseStarting(error: Throwable): Boolean =
    causeChain(error).any { throwable ->
        (throwable as? SQLException)?.sqlState == "57P03" ||
            throwable.message.orEmpty().contains("starting up", ignoreCase = true)
    }

private fun isDatabaseUnavailable(error: Throwable): Boolean =
    causeChain(error).any { throwable ->
        throwable is ConnectException ||
            throwable is java.net.SocketTimeoutException ||
            throwable is java.net.UnknownHostException ||
            (throwable as? SQLException)?.sqlState?.let { state ->
                state.startsWith("08") || state == "3D000" || state == "28P01" || state == "57P03"
            } == true ||
            throwable.message.orEmpty().let { message ->
                message.contains("Connection refused", ignoreCase = true) ||
                    message.contains("connection attempt failed", ignoreCase = true) ||
                    message.contains("password authentication failed", ignoreCase = true) ||
                    message.contains("does not exist", ignoreCase = true)
            }
    }

private fun causeChain(error: Throwable): Sequence<Throwable> =
    generateSequence(error) { it.cause }
