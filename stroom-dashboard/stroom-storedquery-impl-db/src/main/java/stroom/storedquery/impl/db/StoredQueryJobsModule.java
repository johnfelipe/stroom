package stroom.storedquery.impl.db;

import stroom.job.api.ScheduledJobsModule;
import stroom.job.api.TaskConsumer;

import javax.inject.Inject;

import static stroom.job.api.Schedule.ScheduleType.CRON;

public class StoredQueryJobsModule extends ScheduledJobsModule {
    @Override
    protected void configure() {
        super.configure();
        bindJob()
                .name("Query History Clean")
                .description("Job to clean up old query history items")
                .schedule(CRON, "0 0 *")
                .advanced(false)
                .to(QueryHistoryClean.class);
    }

    private static class QueryHistoryClean extends TaskConsumer {
        @Inject
        QueryHistoryClean(final StoredQueryHistoryCleanExecutor queryHistoryCleanExecutor) {
            super(task -> queryHistoryCleanExecutor.exec());
        }
    }
}
