package no.nav.helse.db

import com.zaxxer.hikari.HikariDataSource

class Database private constructor(env: Map<String, String>) {
    private val dataSourceBuilder = DataSourceBuilder(env)

    init {
        org.jetbrains.exposed.sql.Database.connect(datasource())
    }

    fun migrate() {
        dataSourceBuilder.migrate()
    }

    internal fun datasource(): HikariDataSource = dataSourceBuilder.getDataSource()

    companion object {
        private var instance: Database? = null
        fun instance(env: Map<String, String>): Database {
            return instance ?: synchronized(this) {
                instance ?: Database(env).also { instance = it }
            }
        }
    }
}