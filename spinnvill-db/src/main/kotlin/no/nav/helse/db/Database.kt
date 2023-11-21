package no.nav.helse.db

import com.zaxxer.hikari.HikariDataSource

class Database(env: Map<String, String>) {
    private val dataSourceBuilder = DataSourceBuilder(env)
    fun migrate() {
        dataSourceBuilder.migrate()
    }

    internal fun datasource(): HikariDataSource = dataSourceBuilder.getDataSource()
}