package no.nav.dagpenger.eksternintegrasjon.db

import io.ktor.util.logging.*
import kotliquery.queryOf
import kotliquery.sessionOf
import java.util.*
import javax.sql.DataSource

class InnsynDAO(private val dataSource: DataSource, private val log: Logger) {

    fun ny(lagetAv: String): UUID {
        val uuid = UUID.randomUUID()
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    "INSERT INTO innsyn (innsyn_ref, created_by, status) values (:uuid, :lagetAv, 'OPPRETTET')",
                    mapOf(
                        "uuid" to uuid,
                        "lagetAv" to lagetAv,
                    )
                ).asUpdate
            )
        }
        return uuid
    }

    fun saveData(uuid: UUID, data: String) {
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    """
                        UPDATE innsyn set 
                            data = :data::jsonb,
                            status = 'FERDIG'
                        where innsyn_ref = :uuid
                    """.trimIndent(),
                    mapOf(
                        "uuid" to uuid,
                        "data" to data
                    )
                ).asUpdate
            )
        }
    }

    fun saveError(uuid: UUID, error: String) {
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    """
                        UPDATE innsyn set 
                            data = :data::jsonb,
                            status = 'FEIL'
                        where innsyn_ref = :uuid
                    """.trimIndent(),
                    mapOf(
                        "uuid" to uuid,
                        "data" to error
                    )
                ).asUpdate
            )
        }

    }

    fun hent(uuid: UUID): String? {
        val query = queryOf(
            """
                SELECT data FROM innsyn
                where innsyn_ref = ?
            """.trimIndent(),
            uuid
        ).map { row ->
            row.let {
                row.string("data")
            }
        }

        val session = sessionOf(dataSource)

        try {
            return session.run(query.asSingle)
        } catch (e: Exception) {
            log.error(e)
        }
        return null
    }
}
