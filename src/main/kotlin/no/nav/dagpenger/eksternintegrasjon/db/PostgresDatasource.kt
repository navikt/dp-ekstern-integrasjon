package no.nav.dagpenger.eksternintegrasjon.db

import com.zaxxer.hikari.HikariDataSource
import ch.qos.logback.core.util.OptionHelper


internal object PostgresDataSource {

    const val DB_USERNAME_KEY = "DB_USERNAME"
    const val DB_PASSWORD_KEY = "DB_PASSWORD"
    const val DB_DATABASE_KEY = "DB_DATABASE"
    const val DB_HOST_KEY = "DB_HOST"
    const val DB_PORT_KEY = "DB_PORT"

    private fun getOrThrow(key: String): String = OptionHelper.getEnv(key) ?: OptionHelper.getSystemProperty(key)


    val dataSource =
        HikariDataSource().apply {
            dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
            addDataSourceProperty("serverName", getOrThrow(DB_HOST_KEY))
            addDataSourceProperty("portNumber", getOrThrow(DB_PORT_KEY))
            addDataSourceProperty("databaseName", getOrThrow(DB_DATABASE_KEY))
            addDataSourceProperty("user", getOrThrow(DB_USERNAME_KEY))
            addDataSourceProperty("password", getOrThrow(DB_PASSWORD_KEY))
            maximumPoolSize = 10
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001

    }
}