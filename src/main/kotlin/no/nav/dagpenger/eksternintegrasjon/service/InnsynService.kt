package no.nav.dagpenger.eksternintegrasjon.service

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.dagpenger.eksternintegrasjon.db.InnsynDAO
import java.util.*

class InnsynService (private val dao :InnsynDAO){

    suspend fun createInnsyn (lagetAv :String) :UUID {
        val uuid = dao.ny(lagetAv)
        fetchData(uuid)
        return uuid
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun fetchData(uuid: UUID) {

        GlobalScope.launch {
            val data = getVedtak("")
            dao.saveData(uuid, data)
        }
    }

    fun getVedtak(fnr: String) :String {
        return """
            {
                "person_id": "12345678901",
                "virkning_fom": "2012-04-23"
            }
        """.trimIndent()
    }

    fun getInnsyn(uuid: UUID) :String? {
        return dao.hent(uuid)
    }
}