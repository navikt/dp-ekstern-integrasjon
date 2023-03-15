import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.Identity.decode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.net.URL

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {

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
        route("internal/readiness") {
            get {
                call.respond("Ready")
            }
        }
        authenticate("maskinporten") {
            route("/api/dagpenger/v1/vedtak") {
                get {
                    call.respond("Dette er et vedtak")
                }
            }
        }
    }
}
