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
 *
 */

package stroom.processor.impl.db.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.data.meta.api.Data;
import stroom.data.store.api.StreamSource;
import stroom.data.store.api.StreamStore;
import stroom.node.NodeCache;
import stroom.processor.StreamProcessorFilterService;
import stroom.processor.StreamProcessorService;
import stroom.processor.StreamProcessorTaskExecutor;
import stroom.processor.impl.db.CachedStreamProcessorFilterService;
import stroom.processor.impl.db.CachedStreamProcessorService;
import stroom.processor.impl.db.StreamProcessorTask;
import stroom.processor.impl.db.StreamTaskHelper;
import stroom.processor.impl.db.TaskType;
import stroom.processor.shared.Processor;
import stroom.processor.shared.ProcessorFilter;
import stroom.processor.shared.ProcessorFilterTask;
import stroom.processor.shared.TaskStatus;
import stroom.security.Security;
import stroom.task.api.AbstractTaskHandler;
import stroom.task.api.TaskContext;
import stroom.util.date.DateUtil;
import stroom.util.shared.VoidResult;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StreamProcessorTaskHandler extends AbstractTaskHandler<StreamProcessorTask, VoidResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamProcessorTaskHandler.class);
    private static final Set<String> FETCH_SET = new HashSet<>(
            Arrays.asList(Processor.ENTITY_TYPE, ProcessorFilter.ENTITY_TYPE));
    private final Map<TaskType, Provider<StreamProcessorTaskExecutor>> executorProviders;
    private final StreamProcessorService streamProcessorService;
    private final StreamProcessorFilterService streamProcessorFilterService;
    private final StreamTaskHelper streamTaskHelper;
    private final StreamStore streamStore;
    private final NodeCache nodeCache;
    private final TaskContext taskContext;
    private final Security security;

    @Inject
    StreamProcessorTaskHandler(final Map<TaskType, Provider<StreamProcessorTaskExecutor>> executorProviders,
                               final CachedStreamProcessorService streamProcessorService,
                               final CachedStreamProcessorFilterService streamProcessorFilterService,
                               final StreamTaskHelper streamTaskHelper,
                               final StreamStore streamStore,
                               final NodeCache nodeCache,
                               final TaskContext taskContext,
                               final Security security) {
        this.executorProviders = executorProviders;
        this.streamProcessorService = streamProcessorService;
        this.streamProcessorFilterService = streamProcessorFilterService;
        this.streamTaskHelper = streamTaskHelper;
        this.streamStore = streamStore;
        this.nodeCache = nodeCache;
        this.taskContext = taskContext;
        this.security = security;
    }

    @Override
    public VoidResult exec(final StreamProcessorTask task) {
        return security.secureResult(() -> {
            boolean complete = false;
            final long startTime = System.currentTimeMillis();
            ProcessorFilterTask streamTask = task.getStreamTask();
            LOGGER.trace("Executing stream task: {}", streamTask.getId());

            StreamSource streamSource = null;
            try {
                // Open the stream source.
                streamSource = streamStore.openStreamSource(streamTask.getStreamId());
                if (streamSource != null) {
                    final Data stream = streamSource.getStream();

                    Processor destStreamProcessor = null;
                    ProcessorFilter destStreamProcessorFilter = null;
                    if (streamTask.getStreamProcessorFilter() != null) {
                        destStreamProcessorFilter = streamProcessorFilterService.load(
                                streamTask.getStreamProcessorFilter(),
                                FETCH_SET);
                        if (destStreamProcessorFilter != null) {
                            destStreamProcessor = streamProcessorService
                                    .load(destStreamProcessorFilter.getStreamProcessor(), FETCH_SET);
                        }
                    }
                    if (destStreamProcessorFilter == null || destStreamProcessor == null) {
                        throw new RuntimeException("No dest processor has been loaded.");
                    }

                    if (destStreamProcessor.getPipelineUuid() != null) {
                        taskContext.info("Stream {} {} {} {}", stream.getId(),
                                DateUtil.createNormalDateTimeString(stream.getCreateMs()),
                                destStreamProcessor.getTaskType(), destStreamProcessor.getPipelineUuid());
                    } else {
                        taskContext.info("Stream {} {} {}", stream.getId(),
                                DateUtil.createNormalDateTimeString(stream.getCreateMs()),
                                destStreamProcessor.getTaskType());
                    }

                    // Don't process any streams that we have already created
                    if (stream.getProcessorId() != null && stream.getProcessorId() == destStreamProcessor.getId()) {
                        complete = true;
                        LOGGER.warn("Skipping stream that we seem to have created (avoid processing forever) {} {}", stream,
                                destStreamProcessor);

                    } else {
                        // Change the task status.... and save
                        streamTask = streamTaskHelper.changeTaskStatus(streamTask, nodeCache.getDefaultNode(),
                                TaskStatus.PROCESSING, startTime, null);
                        // Avoid having to do another fetch
                        streamTask.setStreamProcessorFilter(destStreamProcessorFilter);

                        final Provider<StreamProcessorTaskExecutor> executorProvider = executorProviders.get(new TaskType(destStreamProcessor.getTaskType()));
                        final StreamProcessorTaskExecutor streamProcessorTaskExecutor = executorProvider.get();

                        // Used as a hook for the test code
                        task.setStreamProcessorTaskExecutor(streamProcessorTaskExecutor);

                        try {
                            streamProcessorTaskExecutor.exec(destStreamProcessor, destStreamProcessorFilter, streamTask,
                                    streamSource);
                            // Only record completion for this task if it was not
                            // terminated.
                            if (!Thread.currentThread().isInterrupted()) {
                                complete = true;
                            }
                        } catch (final RuntimeException e) {
                            LOGGER.error("Task failed {} {}", new Object[]{destStreamProcessor, stream}, e);
                        }
                    }
                }
            } catch (final RuntimeException e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                // Close the stream source.
                if (streamSource != null) {
                    try {
                        streamStore.closeStreamSource(streamSource);
                    } catch (final RuntimeException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }

                if (complete) {
                    streamTaskHelper.changeTaskStatus(streamTask, nodeCache.getDefaultNode(), TaskStatus.COMPLETE,
                            startTime, System.currentTimeMillis());
                } else {
                    streamTaskHelper.changeTaskStatus(streamTask, nodeCache.getDefaultNode(), TaskStatus.FAILED, startTime,
                            System.currentTimeMillis());
                }
            }

            return new VoidResult();
        });
    }
}