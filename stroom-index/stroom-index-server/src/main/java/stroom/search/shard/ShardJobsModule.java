package stroom.search.shard;

import stroom.job.api.ScheduledJobsModule;
import stroom.job.api.TaskConsumer;

import javax.inject.Inject;

import static stroom.job.api.Schedule.ScheduleType.PERIODIC;

public class ShardJobsModule extends ScheduledJobsModule {
    @Override
    protected void configure() {
        super.configure();
        bindJob()
                .name("Index Searcher Cache Refresh")
                .description("Job to refresh index shard searchers in the cache")
                .schedule(PERIODIC, "10m")
                .to(IndexSearcherCacheRefresh.class);
    }

    private static class IndexSearcherCacheRefresh extends TaskConsumer {
        @Inject
        IndexSearcherCacheRefresh(final IndexShardSearcherCache indexShardSearcherCache) {
            super(task -> indexShardSearcherCache.refresh());
        }
    }
}
