package no.nav.dagpenger.eksternintegrasjon

import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import no.nav.dagpenger.eksternintegrasjon.auth.installAuth
import no.nav.dagpenger.eksternintegrasjon.db.InnsynDAO
import no.nav.dagpenger.eksternintegrasjon.db.PostgresDataSource
import no.nav.dagpenger.eksternintegrasjon.service.InnsynService
import org.flywaydb.core.Flyway
import org.slf4j.event.Level
import java.util.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {

    Flyway.configure().dataSource(PostgresDataSource.dataSource).load().migrate()
    val innsynDAO = InnsynDAO(PostgresDataSource.dataSource, log)
    val httpClient = HttpClient(io.ktor.client.engine.java.Java)
    val innsynService = InnsynService(innsynDAO, environment)

    install(CallLogging) {
        level = Level.INFO
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }

    installAuth("vedtak.nivå.1", "nav:dagpenger:vedtak.read")

    routing {
        // Internal API
        route("/internal/liveness") {
            get {
                call.respond("Alive")
            }
        }
        route("/internal/readyness") {
            get {
                call.respond("Ready")
            }
        }
        authenticate("azureAD", "maskinporten") {
            route("/api/dagpenger/v1/vedtak/innsyn") {
                put {
                    val body = call.receiveText()
                    Json.parseToJsonElement(body).jsonObject.get("fnr")?.let {
                        val uuid = innsynService.createInnsyn("test", it.toString().trim('"'))
                        call.respondText(uuid.toString(), status = HttpStatusCode.Accepted)
                    } ?: call.respond(HttpStatusCode.BadRequest)
                }
                get("/{uuid}") {

                    val parameter = call.parameters["uuid"]
                    val uuid: UUID
                    try {
                        uuid = UUID.fromString(parameter)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@get
                    }

                    val innsyn = innsynService.getInnsyn(uuid)
                    if (innsyn == null) {
                        call.respond(HttpStatusCode.NotFound)
                    } else {
                        call.respondText(innsyn, ContentType.Application.Json)
                    }
                }
            }
        }
    }
}
