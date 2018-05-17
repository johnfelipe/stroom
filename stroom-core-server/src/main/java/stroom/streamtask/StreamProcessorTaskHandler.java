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

package stroom.streamtask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import stroom.guice.StroomBeanStore;
import stroom.node.NodeCache;
import stroom.pipeline.shared.PipelineEntity;
import stroom.security.Security;
import stroom.streamstore.StreamSource;
import stroom.streamstore.StreamStore;
import stroom.streamstore.shared.Stream;
import stroom.streamtask.shared.StreamProcessor;
import stroom.streamtask.shared.StreamProcessorFilter;
import stroom.streamtask.shared.StreamTask;
import stroom.streamtask.shared.TaskStatus;
import stroom.task.AbstractTaskHandler;
import stroom.task.TaskContext;
import stroom.task.TaskHandlerBean;
import stroom.util.date.DateUtil;
import stroom.util.shared.VoidResult;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@TaskHandlerBean(task = StreamProcessorTask.class)
class StreamProcessorTaskHandler extends AbstractTaskHandler<StreamProcessorTask, VoidResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamProcessorTaskHandler.class);
    private static final Set<String> FETCH_SET = new HashSet<>(
            Arrays.asList(StreamProcessor.ENTITY_TYPE, StreamProcessorFilter.ENTITY_TYPE, PipelineEntity.ENTITY_TYPE));
    private final StroomBeanStore beanStore;
    private final StreamProcessorService streamProcessorService;
    private final StreamProcessorFilterService streamProcessorFilterService;
    private final StreamTaskHelper streamTaskHelper;
    private final StreamStore streamStore;
    private final NodeCache nodeCache;
    private final TaskContext taskContext;
    private final Security security;

    @Inject
    StreamProcessorTaskHandler(final StroomBeanStore beanStore,
                               @Named("cachedStreamProcessorService") final StreamProcessorService streamProcessorService,
                               @Named("cachedStreamProcessorFilterService") final StreamProcessorFilterService streamProcessorFilterService,
                               final StreamTaskHelper streamTaskHelper,
                               final StreamStore streamStore,
                               final NodeCache nodeCache,
                               final TaskContext taskContext,
                               final Security security) {
        this.beanStore = beanStore;
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
            StreamTask streamTask = task.getStreamTask();
            LOGGER.trace("Executing stream task: {}", streamTask.getId());

            StreamSource streamSource = null;
            try {
                // Open the stream source.
                streamSource = streamStore.openStreamSource(streamTask.getStream().getId());
                if (streamSource != null) {
                    final Stream stream = streamSource.getStream();

                    // Load lazy stuff
                    // stream.setStreamType(streamTypeService.load(stream.getStreamType()));
                    final StreamProcessor sourceStreamProcessor = streamProcessorService.load(stream.getStreamProcessor());

                    StreamProcessor destStreamProcessor = null;
                    StreamProcessorFilter destStreamProcessorFilter = null;
                    if (streamTask.getStreamProcessorFilter() != null) {
                        destStreamProcessorFilter = streamProcessorFilterService.load(streamTask.getStreamProcessorFilter(),
                                FETCH_SET);
                        if (destStreamProcessorFilter != null) {
                            destStreamProcessor = streamProcessorService
                                    .load(destStreamProcessorFilter.getStreamProcessor(), FETCH_SET);
                        }
                    }
                    if (destStreamProcessorFilter == null || destStreamProcessor == null) {
                        throw new RuntimeException("No dest processor has been loaded.");
                    }

                    if (destStreamProcessor.getPipeline() != null) {
                        taskContext.info("Stream {} {} {} {}", stream.getId(),
                                DateUtil.createNormalDateTimeString(stream.getCreateMs()),
                                destStreamProcessor.getTaskType(), destStreamProcessor.getPipeline().getName());
                    } else {
                        taskContext.info("Stream {} {} {}", stream.getId(),
                                DateUtil.createNormalDateTimeString(stream.getCreateMs()),
                                destStreamProcessor.getTaskType());
                    }

                    // Don't process any streams that we have already created
                    if (sourceStreamProcessor != null && sourceStreamProcessor.equals(destStreamProcessor)) {
                        complete = true;
                        LOGGER.warn("Skipping stream that we seem to have created (avoid processing forever) {} {}", stream,
                                sourceStreamProcessor);

                    } else {
                        // Change the task status.... and save
                        streamTask = streamTaskHelper.changeTaskStatus(streamTask, nodeCache.getDefaultNode(),
                                TaskStatus.PROCESSING, startTime, null);
                        // Avoid having to do another fetch
                        streamTask.setStreamProcessorFilter(destStreamProcessorFilter);

                        final String taskType = destStreamProcessor.getTaskType();

                        final StreamProcessorTaskExecutor streamProcessorTaskExecutor = (StreamProcessorTaskExecutor) beanStore
                                .getInstance(taskType);

                        // Used as a hook for the test code
                        task.setStreamProcessorTaskExecutor(streamProcessorTaskExecutor);

                        try {
                            streamProcessorTaskExecutor.exec(destStreamProcessor, destStreamProcessorFilter, streamTask,
                                    streamSource);
                            // Only record completion for this task if it was not
                            // terminated.
                            if (!taskContext.isTerminated()) {
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