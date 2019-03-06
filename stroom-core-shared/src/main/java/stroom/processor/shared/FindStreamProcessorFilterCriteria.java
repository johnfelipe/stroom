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

import stroom.docref.DocRef;
import stroom.util.shared.BaseCriteria;
import stroom.util.shared.CriteriaSet;
import stroom.util.shared.Period;
import stroom.util.shared.Range;

public class FindStreamProcessorFilterCriteria extends BaseCriteria {
    private static final long serialVersionUID = 1L;

    /**
     * The range you care about
     */
    private Range<Integer> priorityRange = null;
    private Period lastPollPeriod = null;
    private CriteriaSet<Integer> processorIdSet = null;
    private CriteriaSet<DocRef> pipelineSet = null;
    private Boolean streamProcessorEnabled = null;
    private Boolean streamProcessorFilterEnabled = null;
    private String createUser;
    private String pipelineNameFilter;
    private String status;

    public FindStreamProcessorFilterCriteria() {
        // Default constructor necessary for GWT serialisation.
    }

    public FindStreamProcessorFilterCriteria(final DocRef pipeline) {
        obtainPipelineSet().add(pipeline);
    }

    public FindStreamProcessorFilterCriteria(final Processor processor) {
        obtainStreamProcessorIdSet().add(processor.getId());
    }

    public Range<Integer> getPriorityRange() {
        return priorityRange;
    }

    public void setPriorityRange(Range<Integer> priorityRange) {
        this.priorityRange = priorityRange;
    }

    public Range<Integer> obtainPriorityRange() {
        if (priorityRange == null) {
            priorityRange = new Range<>();
        }
        return priorityRange;
    }

    public Period getLastPollPeriod() {
        return lastPollPeriod;
    }

    public void setLastPollPeriod(Period lastPollPeriod) {
        this.lastPollPeriod = lastPollPeriod;
    }

    public Period obtainLastPollPeriod() {
        if (lastPollPeriod == null) {
            lastPollPeriod = new Period();
        }
        return lastPollPeriod;
    }

    public CriteriaSet<DocRef> getPipelineSet() {
        return pipelineSet;
    }

    public void setPipelineSet(final CriteriaSet<DocRef> pipelineSet) {
        this.pipelineSet = pipelineSet;
    }

    public CriteriaSet<DocRef> obtainPipelineSet() {
        if (pipelineSet == null) {
            pipelineSet = new CriteriaSet<>();
        }
        return pipelineSet;
    }

    public CriteriaSet<Integer> getProcessorIdSet() {
        return processorIdSet;
    }

    public void setProcessorIdSet(CriteriaSet<Integer> processorIdSet) {
        this.processorIdSet = processorIdSet;
    }

    public CriteriaSet<Integer> obtainStreamProcessorIdSet() {
        if (processorIdSet == null) {
            processorIdSet = new CriteriaSet<>();
        }
        return processorIdSet;
    }

    public Boolean getStreamProcessorEnabled() {
        return streamProcessorEnabled;
    }

    public void setStreamProcessorEnabled(Boolean streamProcessorEnabled) {
        this.streamProcessorEnabled = streamProcessorEnabled;
    }

    public Boolean getStreamProcessorFilterEnabled() {
        return streamProcessorFilterEnabled;
    }

    public void setStreamProcessorFilterEnabled(Boolean streamProcessorFilterEnabled) {
        this.streamProcessorFilterEnabled = streamProcessorFilterEnabled;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getPipelineNameFilter() {
        return pipelineNameFilter;
    }

    public void setPipelineNameFilter(String pipelineNameFilter) {
        this.pipelineNameFilter = pipelineNameFilter;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

}