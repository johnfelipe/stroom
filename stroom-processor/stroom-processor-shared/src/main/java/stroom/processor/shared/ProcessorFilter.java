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
import stroom.streamstore.shared.QueryData;
import stroom.util.shared.HasAuditInfo;

import javax.xml.bind.annotation.XmlTransient;
import java.util.Comparator;
import java.util.Objects;

public class ProcessorFilter implements HasAuditInfo, SharedObject {

    public static final String ENTITY_TYPE = "StreamProcessorFilter";

    public static final Comparator<ProcessorFilter> HIGHEST_PRIORITY_FIRST_COMPARATOR = Comparator
            .comparingInt(ProcessorFilter::getPriority)
            .thenComparingLong(processorFilter ->
                    processorFilter.streamProcessorFilterTracker.getMinStreamId())
            .thenComparingLong(processorFilter ->
                    processorFilter.streamProcessorFilterTracker.getMinEventId());

    private static final long serialVersionUID = -2478788451478923825L;

    // standard id, OCC and audit fields
    private Integer id;
    private Integer version;
    private Long createTimeMs;
    private String createUser;
    private Long updateTimeMs;
    private String updateUser;

    private String data;
    private QueryData queryData;

    private Processor streamProcessor;
    private ProcessorFilterTracker streamProcessorFilterTracker;

    /**
     * The higher the number the higher the priority. So 1 is LOW, 10 is medium,
     * 20 is high.
     */
    private int priority = 10;
    private boolean enabled;

    public ProcessorFilter() {
        // Default constructor necessary for GWT serialisation.
    }

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

    @Override
    public Long getCreateTimeMs() {
        return createTimeMs;
    }

    public void setCreateTimeMs(final Long createTimeMs) {
        this.createTimeMs = createTimeMs;
    }

    @Override
    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(final String createUser) {
        this.createUser = createUser;
    }

    @Override
    public Long getUpdateTimeMs() {
        return updateTimeMs;
    }

    public void setUpdateTimeMs(final Long updateTimeMs) {
        this.updateTimeMs = updateTimeMs;
    }

    @Override
    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(final String updateUser) {
        this.updateUser = updateUser;
    }

    public String getData() {
        return data;
    }

    public void setData(final String data) {
        this.data = data;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(final int priority) {
        this.priority = priority;
    }

    public boolean isHigherPriority(final ProcessorFilter other) {
        return HIGHEST_PRIORITY_FIRST_COMPARATOR.compare(this, other) < 0;
    }

    public Processor getStreamProcessor() {
        return streamProcessor;
    }

    public void setStreamProcessor(final Processor streamProcessor) {
        this.streamProcessor = streamProcessor;
    }

    public ProcessorFilterTracker getStreamProcessorFilterTracker() {
        return streamProcessorFilterTracker;
    }

    public void setStreamProcessorFilterTracker(final ProcessorFilterTracker streamProcessorFilterTracker) {
        this.streamProcessorFilterTracker = streamProcessorFilterTracker;
    }

    @XmlTransient
    public QueryData getQueryData() {
        return queryData;
    }

    public void setQueryData(final QueryData queryData) {
        this.queryData = queryData;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "[" + priority + "] - " + queryData.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ProcessorFilter that = (ProcessorFilter) o;
        return priority == that.priority &&
                enabled == that.enabled &&
                Objects.equals(id, that.id) &&
                Objects.equals(version, that.version) &&
                Objects.equals(createTimeMs, that.createTimeMs) &&
                Objects.equals(createUser, that.createUser) &&
                Objects.equals(updateTimeMs, that.updateTimeMs) &&
                Objects.equals(updateUser, that.updateUser) &&
                Objects.equals(data, that.data) &&
                Objects.equals(queryData, that.queryData) &&
                Objects.equals(streamProcessor, that.streamProcessor) &&
                Objects.equals(streamProcessorFilterTracker, that.streamProcessorFilterTracker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version, createTimeMs, createUser, updateTimeMs, updateUser, data, queryData, streamProcessor, streamProcessorFilterTracker, priority, enabled);
    }
}