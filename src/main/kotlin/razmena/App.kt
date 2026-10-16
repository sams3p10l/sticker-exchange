package razmena

import io.ktor.http.ContentType
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.lang
import kotlinx.html.main
import kotlinx.html.meta
import kotlinx.html.p
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe

private const val PORT = 43123

fun main() {
    val config = DbConfig.fromEnv()
    val started = connectAndMigrate(config)
    println("Migracija: ${if (started.ran) "prošla" else "nije prošla"} (verzija ${started.version ?: "nema"}).")
    embeddedServer(Netty, host = "0.0.0.0", port = PORT) {
        routing {
            get("/health") {
                val status = readMigrationStatus()
                call.respondText(healthText(status), ContentType.parse("text/plain; charset=UTF-8"))
            }
            get("/") {
                call.respondHtml {
                    lang = "sr"
                    head {
                        meta(charset = "utf-8")
                        meta(name = "viewport", content = "width=device-width, initial-scale=1")
                        title("Razmena sličica")
                        style {
                            unsafe {
                                +"""
                                body { margin: 0; background: #f4f1ea; color: #1c1915; font-family: Georgia, "Times New Roman", serif; }
                                main { max-width: 36rem; margin: 0 auto; padding: 3rem 1.25rem; }
                                h1 { font-size: 1.8rem; font-weight: normal; margin: 0 0 0.75rem; }
                                p { font-size: 1.05rem; line-height: 1.5; margin: 0; }
                                """.trimIndent()
                            }
                        }
                    }
                    body {
                        main {
                            h1 { +"Razmena sličica" }
                            p { +"Aplikacija radi. Ovo je radni skelet, bez naloga i albuma." }
                        }
                    }
                }
            }
        }
    }.start(wait = true)
}

private fun healthText(status: MigrationStatus): String {
    val migration = if (status.ran) "prošla" else "nije prošla"
    val version = status.version ?: "nema"
    return "status: ok\nmigracija: $migration\nverzija: $version\n"
}
