package no.nav.dagpenger.eksternintegrasjon.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.net.URL
import java.util.concurrent.TimeUnit


fun Application.installAuth(realm: String, scope: String) {
    val wellKnownUrl = System.getenv().get("MASKINPORTEN_WELL_KNOWN_URL")
    val metadataJson = Json.parseToJsonElement(URL(wellKnownUrl).readText())
    val jwks_uri = metadataJson.jsonObject["jwks_uri"].toString().trim('"')
    val issuer = metadataJson.jsonObject["issuer"].toString().trim('"')

    install(Authentication) {
        jwt(realm) {
            verifier(jwks_uri, issuer)
            validate { credential ->
                if (!"\"$scope\"".equals(credential.payload.claims["scope"].toString())) {
                    return@validate null;
                }
                JWTPrincipal(credential.payload)
            }
        }
    }
}

private fun JWTAuthenticationProvider.Config.verifier(jwks_uri: String, issuer: String) {
    verifier(
        JwkProviderBuilder(URL(jwks_uri))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build(), issuer
    )
}
