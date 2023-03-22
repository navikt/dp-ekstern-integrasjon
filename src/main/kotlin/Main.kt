import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.slf4j.event.Level
import java.net.URL
import java.util.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {

    install(CallLogging) {
        level = Level.INFO
    }


    val wellKnownUrl = System.getenv().get("MASKINPORTEN_WELL_KNOWN_URL")

    val metadataJson = Json.parseToJsonElement(URL(wellKnownUrl).readText())
    val jwks_uri = metadataJson.jsonObject["jwks_uri"].toString().trim('"')
    val issuer = metadataJson.jsonObject["issuer"].toString().trim('"')

    install(Authentication) {
        jwt("maskinporten") {
            verifier(JwkProviderBuilder(URL(jwks_uri)).build(), issuer)
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
//        authenticate("maskinporten") {
        route("/api/dagpenger/v1/vedtak/innsyn") {
            put {
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
//        }
    }
}
