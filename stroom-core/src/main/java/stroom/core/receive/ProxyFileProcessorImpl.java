/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.core.receive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.data.store.api.Store;
import stroom.data.zip.StreamProgressMonitor;
import stroom.feed.api.FeedProperties;
import stroom.meta.shared.AttributeMap;
import stroom.meta.shared.StandardHeaderArguments;
import stroom.meta.statistics.api.MetaStatistics;
import stroom.proxy.repo.ProxyFileHandler;
import stroom.proxy.repo.ProxyFileProcessor;
import stroom.proxy.repo.StroomZipRepository;
import stroom.receive.common.StreamTargetStroomStreamHandler;
import stroom.util.io.BufferFactory;
import stroom.util.logging.LogExecutionTime;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that reads a nested directory tree of stroom zip files.
 * <p>
 * <p>
 * TODO - This class is extended in ProxyAggregationExecutor in Stroom
 * so changes to the way files are stored in the zip repository
 * may have an impact on Stroom while it is using stroom.util.zip as opposed
 * to stroom-proxy-zip.  Need to pull all the zip repository stuff out
 * into its own repo with its own lifecycle and a clearly defined API,
 * then both stroom-proxy and stroom can use it.
 */
public final class ProxyFileProcessorImpl implements ProxyFileProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyFileProcessorImpl.class);

    private final ProxyFileHandler feedFileProcessorHelper;

    private final Store streamStore;
    private final FeedProperties feedProperties;
    private final MetaStatistics metaDataStatistic;
    private final int maxAggregation;
    private final long maxStreamSize;
    private final boolean aggregate = true;
    private final ProxyFileHandler proxyFileHandler;
    private volatile boolean stop = false;

    @Inject
    ProxyFileProcessorImpl(final Store streamStore,
                           final FeedProperties feedProperties,
                           final MetaStatistics metaDataStatistic,
                           final ProxyAggregationConfig proxyAggregationConfig,
                           final BufferFactory bufferFactory) {
        this(
                streamStore,
                feedProperties,
                metaDataStatistic,
                proxyAggregationConfig.getMaxAggregation(),
                proxyAggregationConfig.getMaxStreamSizeBytes(),
                bufferFactory
        );
    }

    public ProxyFileProcessorImpl(final Store streamStore,
                           final FeedProperties feedProperties,
                           final MetaStatistics metaDataStatistic,
                           final int maxAggregation,
                           final long maxStreamSize,
                           final BufferFactory bufferFactory) {
        this.streamStore = streamStore;
        this.feedProperties = feedProperties;
        this.metaDataStatistic = metaDataStatistic;
        this.maxAggregation = maxAggregation;
        this.maxStreamSize = maxStreamSize;

        feedFileProcessorHelper = new ProxyFileHandler(bufferFactory);
        proxyFileHandler = new ProxyFileHandler(bufferFactory);
    }

    @Override
    public void processFeedFiles(final StroomZipRepository stroomZipRepository, final String feedName, final List<Path> fileList) {
        final LogExecutionTime logExecutionTime = new LogExecutionTime();
        LOGGER.info("processFeedFiles() - Started {} ({} Files)", feedName, fileList.size());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("processFeedFiles() - " + feedName + " " + fileList);
        }

        // We don't want to aggregate reference feeds.
        final boolean isReference = feedProperties.isReference(feedName);
        final String streamType = feedProperties.getStreamTypeName(feedName);
        final boolean oneByOne = isReference || !aggregate;

        List<StreamTargetStroomStreamHandler> handlers = openStreamHandlers(feedName, streamType, isReference);
        List<Path> deleteFileList = new ArrayList<>();

        long sequence = 1;
        long maxAggregation = this.maxAggregation;
        if (oneByOne) {
            maxAggregation = 1;
        }

        Long nextBatchBreak = this.maxStreamSize;

        final StreamProgressMonitor streamProgressMonitor = new StreamProgressMonitor("ProxyAggregationTask");

        for (final Path file : fileList) {
            if (stop) {
                break;
            }
            try {
                if (sequence > maxAggregation
                        || (streamProgressMonitor.getTotalBytes() > nextBatchBreak)) {
                    LOGGER.info("processFeedFiles() - Breaking Batch {} as limit is ({} > {}) or ({} > {})",
                            feedName,
                            sequence,
                            maxAggregation,
                            streamProgressMonitor.getTotalBytes(),
                            nextBatchBreak
                    );

                    // Recalculate the next batch break
                    nextBatchBreak = streamProgressMonitor.getTotalBytes() + maxStreamSize;

                    // Close off this unit
                    handlers = closeStreamHandlers(handlers);

                    // Delete the done files
                    proxyFileHandler.deleteFiles(stroomZipRepository, deleteFileList);

                    // Start new batch
                    deleteFileList = new ArrayList<>();
                    handlers = openStreamHandlers(feedName, streamType, isReference);
                    sequence = 1;
                }
                sequence = feedFileProcessorHelper.processFeedFile(handlers, stroomZipRepository, file, streamProgressMonitor, sequence);
                deleteFileList.add(file);

            } catch (final IOException | RuntimeException e) {
                handlers = closeDeleteStreamHandlers(handlers);
            }
        }
        closeStreamHandlers(handlers);
        proxyFileHandler.deleteFiles(stroomZipRepository, deleteFileList);
        LOGGER.info("processFeedFiles() - Completed {} in {}", feedName, logExecutionTime);
    }

    private List<StreamTargetStroomStreamHandler> openStreamHandlers(final String feedName, final String streamType, final boolean isReference) {
        // We don't want to aggregate reference feeds.
        final boolean oneByOne = isReference || !aggregate;

        final StreamTargetStroomStreamHandler streamTargetStroomStreamHandler = new StreamTargetStroomStreamHandler(streamStore,
                feedProperties, metaDataStatistic, feedName, streamType);

        streamTargetStroomStreamHandler.setOneByOne(oneByOne);

        final AttributeMap globalAttributeMap = new AttributeMap();
        globalAttributeMap.put(StandardHeaderArguments.FEED, feedName);

//        try {
        streamTargetStroomStreamHandler.handleHeader(globalAttributeMap);
//        } catch (final IOException ioEx) {
//            streamTargetStroomStreamHandler.close();
//            throw new RuntimeException(ioEx);
//        }

        final List<StreamTargetStroomStreamHandler> list = new ArrayList<>();
        list.add(streamTargetStroomStreamHandler);

        return list;
    }

    private List<StreamTargetStroomStreamHandler> closeStreamHandlers(final List<StreamTargetStroomStreamHandler> handlers) {
        if (handlers != null) {
            handlers.forEach(StreamTargetStroomStreamHandler::close);
        }
        return null;
    }

    private List<StreamTargetStroomStreamHandler> closeDeleteStreamHandlers(
            final List<StreamTargetStroomStreamHandler> handlers) {
        if (handlers != null) {
            handlers.forEach(StreamTargetStroomStreamHandler::closeDelete);
        }
        return null;
    }

//    static long getByteSize(final String propertyValue, final long defaultValue) {
//        Long value = null;
//        try {
//            value = ModelStringUtil.parseIECByteSizeString(propertyValue);
//        } catch (final RuntimeException e) {
//            LOGGER.error(e.getMessage(), e);
//        }
//
//        if (value == null) {
//            value = defaultValue;
//        }
//
//        return value;
//    }

    /**
     * Stops the task as soon as possible.
     */
    public void stop() {
        stop = true;
    }
}
