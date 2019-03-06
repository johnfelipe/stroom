/*
 * Copyright 2016 Crown Copyright
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

package stroom.processor.shared;

import stroom.docref.SharedObject;

import java.util.Objects;

/**
 * Class used to represent processing a stream.
 */
public class ProcessorFilterTask implements SharedObject {
//    public static final String ENTITY_TYPE = "StreamTask";
    private static final long serialVersionUID = 3926403008832938745L;

    // standard id and OCC fields
    private Integer id;
    private Integer version;

    private Long streamId;
    private String data;
    private String nodeName;
    private Long createMs;
    private Long statusMs;
    private Long startTimeMs;
    private Long endTimeMs;
    private TaskStatus status = TaskStatus.UNPROCESSED;

    // parent filter
    private ProcessorFilter streamProcessorFilter;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public Long getStreamId() {
        return streamId;
    }

    public void setStreamId(final Long streamId) {
        this.streamId = streamId;
    }

    public String getData() {
        return data;
    }

    public void setData(final String data) {
        this.data = data;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(final String nodeName) {
        this.nodeName = nodeName;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(final TaskStatus status) {
        this.status = status;
    }

    public Long getStartTimeMs() {
        return startTimeMs;
    }

    public void setStartTimeMs(final Long startTimeMs) {
        this.startTimeMs = startTimeMs;
    }

    public Long getCreateMs() {
        return createMs;
    }

    public void setCreateMs(final Long createMs) {
        this.createMs = createMs;
    }

    public Long getStatusMs() {
        return statusMs;
    }

    public void setStatusMs(final Long statusMs) {
        this.statusMs = statusMs;
    }

    public Long getEndTimeMs() {
        return endTimeMs;
    }

    public void setEndTimeMs(final Long endTimeMs) {
        this.endTimeMs = endTimeMs;
    }

    public ProcessorFilter getStreamProcessorFilter() {
        return streamProcessorFilter;
    }

    public void setStreamProcessorFilter(final ProcessorFilter streamProcessorFilter) {
        this.streamProcessorFilter = streamProcessorFilter;
    }

    @Override
    public String toString() {
        return "ProcessorFilterTask{" +
                "id=" + id +
                ", version=" + version +
                ", streamId=" + streamId +
                ", data='" + data + '\'' +
                ", nodeName='" + nodeName + '\'' +
                ", createMs=" + createMs +
                ", statusMs=" + statusMs +
                ", startTimeMs=" + startTimeMs +
                ", endTimeMs=" + endTimeMs +
                ", status=" + status +
                ", streamProcessorFilter=" + streamProcessorFilter +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ProcessorFilterTask that = (ProcessorFilterTask) o;
        return Objects.equals(status, that.status) &&
                Objects.equals(id, that.id) &&
                Objects.equals(version, that.version) &&
                Objects.equals(streamId, that.streamId) &&
                Objects.equals(data, that.data) &&
                Objects.equals(nodeName, that.nodeName) &&
                Objects.equals(createMs, that.createMs) &&
                Objects.equals(statusMs, that.statusMs) &&
                Objects.equals(startTimeMs, that.startTimeMs) &&
                Objects.equals(endTimeMs, that.endTimeMs) &&
                Objects.equals(streamProcessorFilter, that.streamProcessorFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version, streamId, data, nodeName, createMs, statusMs, startTimeMs, endTimeMs, status, streamProcessorFilter);
    }
}