package stroom.meta.impl.db;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.zaxxer.hikari.HikariConfig;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.config.common.ConnectionConfig;
import stroom.config.common.ConnectionPoolConfig;
import stroom.db.util.HikariUtil;
import stroom.event.logging.api.ObjectInfoProviderBinder;
import stroom.meta.shared.Meta;
import stroom.meta.shared.MetaSecurityFilter;
import stroom.meta.shared.MetaService;
import stroom.util.guice.GuiceUtil;
import stroom.util.shared.Clearable;

import javax.inject.Provider;
import javax.inject.Singleton;
import javax.sql.DataSource;

public class MetaDbModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetaDbModule.class);
    private static final String MODULE = "stroom-meta";
    private static final String FLYWAY_LOCATIONS = "stroom/meta/impl/db";
    private static final String FLYWAY_TABLE = "meta_schema_history";

    @Override
    protected void configure() {
        bind(MetaFeedService.class).to(MetaFeedServiceImpl.class);
        bind(MetaTypeService.class).to(MetaTypeServiceImpl.class);
        bind(MetaProcessorService.class).to(MetaProcessorServiceImpl.class);
        bind(MetaKeyService.class).to(MetaKeyServiceImpl.class);
        bind(MetaValueService.class).to(MetaValueServiceImpl.class);
        bind(MetaService.class).to(MetaServiceImpl.class);
        bind(MetaSecurityFilter.class).to(MetaSecurityFilterImpl.class);

        GuiceUtil.buildMultiBinder(binder(), Clearable.class).addBinding(Cleanup.class);

        // Provide object info to the logging service.
        ObjectInfoProviderBinder.create(binder())
                .bind(Meta.class, MetaObjectInfoProvider.class);

        // MultiBind the connection provider so we can see status for all databases.
        GuiceUtil.buildMultiBinder(binder(), DataSource.class)
                .addBinding(ConnectionProvider.class);
    }

    @Provides
    @Singleton
    ConnectionProvider getConnectionProvider(final Provider<MetaServiceConfig> configProvider) {
        final ConnectionConfig connectionConfig = configProvider.get().getConnectionConfig();
        final ConnectionPoolConfig connectionPoolConfig = configProvider.get().getConnectionPoolConfig();
        final HikariConfig config = HikariUtil.createConfig(connectionConfig, connectionPoolConfig);
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
