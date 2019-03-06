
/*
 *
 *  * Copyright 2018 Crown Copyright
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package stroom.processor.impl.db.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import stroom.pipeline.shared.PipelineDoc;
import stroom.security.SecurityContext;
import stroom.processor.StreamProcessorFilterService;
import stroom.processor.shared.Processor;
import stroom.processor.shared.ProcessorFilter;
import stroom.util.logging.LambdaLogger;
import stroom.util.RestResource;
import stroom.util.shared.BaseResultList;
import stroom.util.shared.Sort;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static java.util.Comparator.comparingInt;
import static stroom.entity.shared.Sort.Direction.ASCENDING;
import static stroom.processor.impl.db.resource.SearchKeywords.SORT_NEXT;
import static stroom.processor.impl.db.resource.SearchKeywords.addFiltering;
import static stroom.streamtask.resource.SearchKeywords.SORT_NEXT;
import static stroom.streamtask.resource.SearchKeywords.addFiltering;
import static stroom.util.shared.Sort.Direction.ASCENDING;

@Api(value = "stream task - /v1")
@Path("/streamtasks/v1")
@Produces(MediaType.APPLICATION_JSON)
public class StreamTaskResource implements RestResource {
    private static final String FIELD_PROGRESS = "progress";

    private final StreamProcessorFilterService streamProcessorFilterService;
    private final SecurityContext securityContext;

    @Inject
    public StreamTaskResource(
            StreamProcessorFilterService streamProcessorFilterService,
            SecurityContext securityContext) {
        this.streamProcessorFilterService = streamProcessorFilterService;
        this.securityContext = securityContext;
    }

    @PATCH
    @Path("/{filterId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response enable(
            @PathParam("filterId") int filterId,
            StreamTaskPatch patch) {

        return streamProcessorFilterService.fetch(filterId)
                .map(processorFilter -> {
                    boolean patchApplied = false;
                    if (patch.getOp().equalsIgnoreCase("replace")) {
                        if (patch.getPath().equalsIgnoreCase("enabled")) {
                            processorFilter.setEnabled(Boolean.parseBoolean(patch.getValue()));
                            patchApplied = true;
                        }
                    }

                    if (patchApplied) {
                        streamProcessorFilterService.update(processorFilter);
                        return Response
                                .ok()
                                .build();
                    } else {
                        return Response
                                .status(Response.Status.BAD_REQUEST)
                                .entity("Unable to apply the requested patch. See server logs for details.")
                                .build();
                    }
                })
                .orElseGet(() ->
                        Response
                                .status(Response.Status.NOT_FOUND)
                                .entity(LambdaLogger.buildMessage("Filter with ID {} could not be found", filterId))
                                .build());
    }

    @GET
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetch(
            @NotNull @QueryParam("offset") Integer offset,
            @QueryParam("pageSize") Integer pageSize,
            @QueryParam("sortBy") String sortBy,
            @QueryParam("sortDirection") String sortDirection,
            @QueryParam("filter") String filter) {
        // TODO: Authorisation

        final FindStreamProcessorFilterCriteria criteria = new FindStreamProcessorFilterCriteria();

        // SORTING
        Sort.Direction direction = ASCENDING;
        if (sortBy != null) {
            try {
                direction = Sort.Direction.valueOf(sortDirection.toUpperCase());
            } catch (IllegalArgumentException exception) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid sortDirection field").build();
            }
            if (sortBy.equalsIgnoreCase(FindStreamTaskCriteria.FIELD_PIPELINE_UUID)
                    || sortBy.equalsIgnoreCase(FindStreamTaskCriteria.FIELD_PRIORITY)) {
                criteria.setSort(sortBy, direction, false);
            } else if (sortBy.equalsIgnoreCase(FIELD_PROGRESS)) {
                // Sorting progress is done below -- this is here for completeness.
                // Percentage is a calculated variable so it has to be done after retrieval.
                // This poses a problem for paging and at the moment sorting by tracker % won't work correctly when paging.
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid sortBy field").build();
            }
        }

        // PAGING
        if (offset < 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Page offset must be greater than 0").build();
        }
        if (pageSize != null && pageSize < 1) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Page size, if used, must be greater than 1").build();
        }

        addFiltering(filter, criteria);

        if (!securityContext.isAdmin()) {
            criteria.setCreateUser(securityContext.getUserId());
        }

        criteria.getFetchSet().add(Processor.ENTITY_TYPE);
        criteria.getFetchSet().add(PipelineDoc.DOCUMENT_TYPE);

        // We have to load everything because we need to sort by progress, and we can't do that on the database.
        final List<StreamTask> values = find(criteria);

        if (sortBy != null) {
            // If the user is requesting a sort:next then we don't want to apply any other sorting.
            if (sortBy.equalsIgnoreCase(FIELD_PROGRESS) && !filter.contains(SORT_NEXT)) {
                if (direction == ASCENDING) {
                    values.sort(comparingInt(StreamTask::getTrackerPercent));
                } else {
                    values.sort(comparingInt(StreamTask::getTrackerPercent).reversed());
                }
            }
        }

        int from = offset * pageSize;
        int to = (offset * pageSize) + pageSize;
        if (values.size() <= to) {
            to = values.size();
        }
        // PAGING
        List<StreamTask> pageToReturn = values.subList(from, to);

        final StreamTasks response = new StreamTasks(pageToReturn, values.size());

        return Response.ok(response).build();
    }

    private List<StreamTask> find(final FindStreamProcessorFilterCriteria criteria) {

        final BaseResultList<ProcessorFilter> streamProcessorFilters = streamProcessorFilterService
                .find(criteria);


        List<StreamTask> streamTasks = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (ProcessorFilter filter : streamProcessorFilters.getValues()) {
            StreamTask.StreamTaskBuilder builder = StreamTask.StreamTaskBuilder.aStreamTask();

            // Indented to make the source easier to read
            builder
                    .withPipelineName(filter.getStreamProcessor().getPipelineName())
                    //.withPipelineId(     filter.getStreamProcessor().getPipeline().getId())
                    .withPriority(filter.getPriority())
                    .withEnabled(filter.isEnabled())
                    .withFilterId(filter.getId())
                    .withCreateUser(filter.getCreateUser())
                    .withCreatedOn(filter.getCreateTime())
                    .withUpdateUser(filter.getUpdateUser())
                    .withUpdatedOn(filter.getUpdateTime())
                    .withFilter(filter.getQueryData());

            if (filter.getStreamProcessorFilterTracker() != null) {
                Integer trackerPercent = filter.getStreamProcessorFilterTracker().getTrackerStreamCreatePercentage();
                if (trackerPercent == null) {
                    trackerPercent = 0;
                }
                builder.withTrackerMs(filter.getStreamProcessorFilterTracker().getStreamCreateMs())
                        .withTrackerPercent(trackerPercent)
                        .withLastPollAge(filter.getStreamProcessorFilterTracker().getLastPollAge())
                        .withTaskCount(filter.getStreamProcessorFilterTracker().getLastPollTaskCount())
                        .withMinStreamId(filter.getStreamProcessorFilterTracker().getMinStreamId())
                        .withMinEventId(filter.getStreamProcessorFilterTracker().getMinEventId())
                        .withStatus((filter.getStreamProcessorFilterTracker().getStatus()));
            }

            StreamTask streamTask = builder.build();
            streamTasks.add(streamTask);
        }

        return streamTasks;
    }

}