package stroom.job.impl.db;

import com.google.inject.Guice;
import com.google.inject.Inject;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.DataChangedException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import stroom.job.shared.FindJobCriteria;
import stroom.job.shared.Job;
import stroom.security.impl.mock.MockSecurityContextModule;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestJobDaoImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestJobDaoImpl.class);

    private static MySQLContainer dbContainer = new MySQLContainer()
            .withDatabaseName("stroom");

    @Inject
    private JobDaoImpl dao;

    @BeforeAll
    static void beforeAll() {
        LOGGER.info(() -> "Before All - Start Database");
        Optional.ofNullable(dbContainer).ifPresent(MySQLContainer::start);
    }

    @BeforeEach
    void beforeEach() {
        Guice.createInjector(
                new JobDbModule(),
                new MySQLContainerModule(dbContainer),
                new MockSecurityContextModule())
                .injectMembers(this);
        cleanup();
    }

    @Test
    void basicCreation() {
        // Given/when
        Job job = createStandardJob();

        // Then
        assertThat(job.getId()).isNotNull();
        assertThat(job.getVersion()).isNotNull();
        assertThat(job.getName()).isEqualTo("Some name");
        assertThat(job.isEnabled()).isTrue();

        Job loadedJob = dao.fetch(job.getId()).get();
        assertThat(loadedJob.getId()).isEqualTo(job.getId());
        assertThat(loadedJob.getVersion()).isNotNull();
        assertThat(loadedJob.getName()).isEqualTo("Some name");
        assertThat(loadedJob.isEnabled()).isTrue();
    }

    @Test
    void descriptionTooLong() {
        // Given
        Job job = new Job();
        job.setEnabled(true);
        job.setName(RandomStringUtils.randomAlphabetic(256));

        // When/then
        assertThrows(DataAccessException.class, () -> dao.create(job));
    }

    @Test
    void badFetch(){
        // Given/when
        Optional<Job> job = dao.fetch(11111);
        // Then
        assertThat(job.isPresent()).isFalse();
    }

    @Test
    void update(){
        // Given
        Job job = createStandardJob();
        int version = job.getVersion();
        job.setName("Different name");
        job.setEnabled(false);

        // When
        Job updatedJob = dao.update(job);

        // Then
        assertThat(updatedJob.getId()).isEqualTo(job.getId());
        assertThat(updatedJob.getVersion()).isEqualTo(version + 1);
        assertThat(updatedJob.getName()).isEqualTo("Different name");
        assertThat(updatedJob.isEnabled()).isFalse();

        // Then
        Job fetchedUpdatedJob = dao.fetch(updatedJob.getId()).get();
        assertThat(fetchedUpdatedJob.getId()).isEqualTo(job.getId());
        assertThat(fetchedUpdatedJob.getVersion()).isEqualTo(version + 1);
        assertThat(fetchedUpdatedJob.getName()).isEqualTo("Different name");
        assertThat(fetchedUpdatedJob.isEnabled()).isFalse();
    }

    @Test
    void delete() {
        // Given
        Job job = createStandardJob();

        // When
        boolean didDeleteSucceed = dao.delete(job.getId());

        // Then
        assertThat(didDeleteSucceed).isTrue();
        Optional<Job> optionalJob = dao.fetch(job.getId());
        assertThat(optionalJob.isPresent()).isFalse();
    }

    @Test
    void badDelete(){
        // Given/when
        boolean didDeleteSucceed = dao.delete(111111);
        // Then
        assertThat(didDeleteSucceed).isFalse();
    }

    @Test
    void checkOcc(){
        // Given
        Job job = createStandardJob();
        Job copy1 = dao.fetch(job.getId()).get();
        Job copy2 = dao.fetch(job.getId()).get();

        copy1.setName("change 1");
        dao.update(copy1);

        copy2.setName("change 2");

        // When/then
        assertThrows(DataChangedException.class, () -> dao.update(copy2));
    }

    @AfterAll
    static void afterAll() {
        LOGGER.info(() -> "After All - Stop Database");
        Optional.ofNullable(dbContainer).ifPresent(MySQLContainer::stop);
    }

    private Job createStandardJob(){
        Job job = new Job();
        job.setEnabled(true);
        job.setName("Some name");
        Job createdJob = dao.create(job);
        return createdJob;
    }

    private void cleanup() {
        // Cleanup
        final List<Job> jobs = dao.find(new FindJobCriteria());
        jobs.forEach(job -> dao.delete(job.getId()));
    }
}
