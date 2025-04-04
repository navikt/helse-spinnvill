package no.nav.helse.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.flywaydb.core.Flyway
import java.time.Duration
import javax.sql.DataSource

internal class DataSourceBuilder(env: Map<String, String>) {
    private val databaseHost: String = requireNotNull(env["DATABASE_HOST"]) { "host må settes" }
    private val databasePort: String = requireNotNull(env["DATABASE_PORT"]) { "port må settes" }
    private val databaseName: String = requireNotNull(env["DATABASE_DATABASE"]) { "databasenavn må settes" }
    private val databaseUsername: String = requireNotNull(env["DATABASE_USERNAME"]) { "brukernavn må settes" }
    private val databasePassword: String = requireNotNull(env["DATABASE_PASSWORD"]) { "passord må settes" }

    private val dbUrl = String.format("jdbc:postgresql://%s:%s/%s", databaseHost, databasePort, databaseName)

    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = dbUrl
        username = databaseUsername
        password = databasePassword
        maximumPoolSize = 5
        minimumIdle = 2
        idleTimeout = Duration.ofMinutes(1).toMillis()
        maxLifetime = idleTimeout * 5
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
        connectionTimeout = Duration.ofSeconds(30).toMillis()
        metricRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    private val hikariMigrationConfig = HikariConfig().apply {
        jdbcUrl = dbUrl
        username = databaseUsername
        password = databasePassword
        initializationFailTimeout = Duration.ofMinutes(1).toMillis()
        connectionTimeout = Duration.ofMinutes(1).toMillis()
        maximumPoolSize = 2
    }

    private fun runMigration(dataSource: DataSource) =
        Flyway.configure()
            .dataSource(dataSource)
            .lockRetryCount(-1)
            .load()
            .migrate()

    internal fun getDataSource(): HikariDataSource = HikariDataSource(hikariConfig)

    internal fun migrate() {
        HikariDataSource(hikariMigrationConfig).use { runMigration(it) }
    }
}
