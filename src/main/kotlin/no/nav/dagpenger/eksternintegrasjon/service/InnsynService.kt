package no.nav.dagpenger.eksternintegrasjon.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.server.application.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.dagpenger.eksternintegrasjon.db.InnsynDAO
import java.net.URL
import java.util.*

class InnsynService(private val dao: InnsynDAO, private val environment :ApplicationEnvironment) {

    fun createInnsyn (lagetAv: String, personId: String) :UUID {
        val uuid = dao.ny(lagetAv)
        fetchData(uuid, personId)
        return uuid
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun fetchData(uuid: UUID, personId: String) {
        GlobalScope.launch {
            try {
                val data = getVedtak(personId)
                dao.saveData(uuid, data)
            } catch (e :Exception) {
                dao.saveError(uuid, "${e.message}")
            }
        }
    }

    suspend fun getVedtak(fnr: String) :String {
        return HttpClient(io.ktor.client.engine.java.Java).use { client ->
            client.get(URL("${environment.config.property("iverksett.url").getString()}/api/vedtakstatus/$fnr"))
        }.body()
    }

    fun getInnsyn(uuid: UUID) :String? {
        return dao.hent(uuid)
    }
}