package stroom.index.impl.db;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.zaxxer.hikari.HikariConfig;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.config.common.ConnectionConfig;
import stroom.config.common.ConnectionPoolConfig;
import stroom.db.util.DbUtil;
import stroom.index.dao.IndexShardDao;
import stroom.index.dao.IndexVolumeDao;
import stroom.index.dao.IndexVolumeGroupDao;
import stroom.util.guice.GuiceUtil;

import javax.inject.Provider;
import javax.inject.Singleton;
import javax.sql.DataSource;

public class IndexDbModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexDbModule.class);
    private static final String MODULE = "stroom-index";
    private static final String FLYWAY_LOCATIONS = "stroom/index/impl/db";
    private static final String FLYWAY_TABLE = "index_schema_history";

    @Override
    protected void configure() {
        bind(IndexShardDao.class).to(IndexShardDaoImpl.class);
        bind(IndexVolumeDao.class).to(IndexVolumeDaoImpl.class);
        bind(IndexVolumeGroupDao.class).to(IndexVolumeGroupDaoImpl.class);

        // MultiBind the connection provider so we can see status for all databases.
        GuiceUtil.buildMultiBinder(binder(), DataSource.class)
                .addBinding(ConnectionProvider.class);
    }

    @Provides
    @Singleton
    ConnectionProvider getConnectionProvider(final Provider<IndexDbConfig> configProvider) {
        final ConnectionConfig connectionConfig = configProvider.get().getConnectionConfig();

        // Keep waiting until we can establish a DB connection to allow for the DB to start after the app
        DbUtil.waitForConnection(
                connectionConfig.getJdbcDriverClassName(),
                connectionConfig.getJdbcDriverUrl(),
                connectionConfig.getJdbcDriverUsername(),
                connectionConfig.getJdbcDriverPassword());

        final ConnectionPoolConfig connectionPoolConfig = configProvider.get().getConnectionPoolConfig();

        connectionConfig.validate();

        final HikariConfig config = new HikariConfig();
        config.setJdbcUrl(connectionConfig.getJdbcDriverUrl());
        config.setUsername(connectionConfig.getJdbcDriverUsername());
        config.setPassword(connectionConfig.getJdbcDriverPassword());
        config.addDataSourceProperty("cachePrepStmts",
                String.valueOf(connectionPoolConfig.isCachePrepStmts()));
        config.addDataSourceProperty("prepStmtCacheSize",
                String.valueOf(connectionPoolConfig.getPrepStmtCacheSize()));
        config.addDataSourceProperty("prepStmtCacheSqlLimit",
                String.valueOf(connectionPoolConfig.getPrepStmtCacheSqlLimit()));
        final ConnectionProvider connectionProvider = new ConnectionProvider(config);
        flyway(connectionProvider);
        return connectionProvider;
    }

    private Flyway flyway(final DataSource dataSource) {
        final Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(FLYWAY_LOCATIONS)
                .table(FLYWAY_TABLE)
                .baselineOnMigrate(true)
                .load();
        LOGGER.info("Applying Flyway migrations to {} in {} from {}", MODULE, FLYWAY_TABLE, FLYWAY_LOCATIONS);
        try {
            flyway.migrate();
        } catch (FlywayException e) {
            LOGGER.error("Error migrating {} database", MODULE, e);
            throw e;
        }
        LOGGER.info("Completed Flyway migrations for {} in {}", MODULE, FLYWAY_TABLE);
        return flyway;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
