package stroom.job.impl.db;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

class JobDaoImpl implements JobDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobDaoImpl.class);

    private final ConnectionProvider connectionProvider;

    @Inject
    JobDaoImpl(final ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public Job create(final Job job) {
        try (final Connection connection = connectionProvider.getConnection()) {
            final DSLContext create = DSL.using(connection, SQLDialect.MYSQL);
            //TODO
        } catch (final SQLException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        return job;
    }

    @Override
    public Job create() {
        return null;
    }

    @Override
    public Job update(final Job job) {
        try (final Connection connection = connectionProvider.getConnection()) {
            final DSLContext create = DSL.using(connection, SQLDialect.MYSQL);
            //TODO
        } catch (final SQLException e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
        return job;
    }

    @Override
    public int delete(int id) {
        //TODO
        return 0;
    }

    @Override
    public Optional<Job> fetch(int id) {
        //TODO
        return Optional.empty();
    }

}