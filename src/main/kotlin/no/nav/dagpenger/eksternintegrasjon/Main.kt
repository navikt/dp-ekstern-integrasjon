package no.nav.dagpenger.eksternintegrasjon

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.slf4j.event.Level
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {

    install(CallLogging) {
        level = Level.INFO
    }


    val wellKnownUrl = System.getenv().get("MASKINPORTEN_WELL_KNOWN_URL")

    val metadataJson = Json.parseToJsonElement(URL(wellKnownUrl).readText())
    val jwks_uri = metadataJson.jsonObject["jwks_uri"].toString().trim('"')
    val issuer = metadataJson.jsonObject["issuer"].toString().trim('"')

    install(Authentication) {
        jwt {
            verifier(JwkProviderBuilder(URL(jwks_uri))
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build(), issuer)
            validate { credential ->
                JWTPrincipal(credential.payload)
            }
        }
    }

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
        authenticate {
            route("/api/dagpenger/v1/vedtak/innsyn") {
                put {
                    call.authentication.principal<JWTPrincipal>()?.let { principal -> {
                        val scope = principal.payload.claims["scope"] ?: error("No scope provided")
                        if (scope.equals("nav:dagpenger:vedtak.read")) {
                            error("nav:dagpenger:vedtak.read not in scope")
                        }
                    }}
                    call.respond(HttpStatusCode.Accepted, UUID.randomUUID().toString())
                }
                get("/{uuid}") {
                    call.respond(
                        """
                        {
                            "person_id": "12345678901",
                            "virkning_fom": "2012-04-23",
                            "virkning_tom: null
                        }
                    """.trimIndent()
                    )
                }
            }
        }
    }
}
