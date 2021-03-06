package stroom.app.guice;

import com.google.inject.AbstractModule;
import stroom.core.benchmark.BenchmarkJobsModule;
import stroom.cache.impl.CacheJobsModule;
import stroom.cluster.lock.impl.db.ClusterLockJobsModule;
import stroom.config.global.impl.db.GlobalConfigJobsModule;
import stroom.core.receive.ProxyAggregationJobsModule;
import stroom.data.retention.impl.DataRetentionJobsModule;
import stroom.data.store.impl.DataRetentionJobModule;
import stroom.data.store.impl.fs.FsDataStoreJobsModule;
import stroom.data.store.impl.fs.FsVolumeJobsModule;
import stroom.index.IndexJobsModule;
import stroom.index.selection.VolumeJobsModule;
import stroom.pipeline.PipelineJobsModule;
import stroom.pipeline.refdata.store.RefDataStoreJobsModule;
import stroom.processor.impl.ProcessorTaskJobsModule;
import stroom.resource.impl.ResourceJobsModule;
import stroom.search.SearchJobsModule;
import stroom.search.shard.ShardJobsModule;
import stroom.statistics.impl.sql.SQLStatisticsJobsModule;
import stroom.statistics.impl.sql.search.SQLStatisticSearchJobsModule;
import stroom.storedquery.impl.db.StoredQueryJobsModule;

public class JobsModule extends AbstractModule {
    @Override
    protected void configure(){
        install(new BenchmarkJobsModule());
        install(new CacheJobsModule());
        install(new ClusterLockJobsModule());
        install(new DataRetentionJobModule());
        install(new DataRetentionJobsModule());
        install(new FsDataStoreJobsModule());
        install(new FsVolumeJobsModule());
        install(new GlobalConfigJobsModule());
        install(new IndexJobsModule());
        install(new PipelineJobsModule());
        install(new RefDataStoreJobsModule());
        install(new ResourceJobsModule());
        install(new SearchJobsModule());
        install(new ShardJobsModule());
        install(new SQLStatisticSearchJobsModule());
        install(new SQLStatisticsJobsModule());
        install(new StoredQueryJobsModule());
        install(new stroom.job.impl.JobSystemJobsModule());
        install(new stroom.meta.impl.db.MetaDbJobsModule());
        install(new stroom.node.impl.NodeJobsModule());
        install(new ProcessorTaskJobsModule());
        install(new ProxyAggregationJobsModule());
        install(new VolumeJobsModule());
    }
}
